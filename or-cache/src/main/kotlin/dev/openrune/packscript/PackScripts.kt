package dev.openrune.packscript

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.definition.dbtables.DBTable
import dev.openrune.filesystem.Cache
import java.io.File
import kotlin.system.exitProcess

/**
 * Runs plugin RunePack (`.rp`) scripts for a pack phase.
 *
 * Any parse / validation / runtime error fails the cache build.
 *
 * Example (`toolbelt-items.rp`):
 * ```
 * [after_db.toolbelt_items]
 * for obj in db.toolbelt_slots.allowed_items {
 *   val equipable = oc.equipable(obj)
 *   if (equipable) {
 *     println("equipable", obj)
 *   }
 *   oc.addiop(obj, "Add-to-toolbelt")
 *   // or force a slot: oc.addiop(obj, "Add-to-toolbelt", 2)
 * }
 * ```
 */
class PackScripts(
    private val scriptDirectories: List<File>,
    private val phase: PackScriptPhase,
    private val tables: List<DBTable>,
) : CacheTask() {
    private val logger = InlineLogger()

    override fun init(cache: Cache) {
        val files =
            scriptDirectories
                .filter { it.isDirectory }
                .flatMap { dir ->
                    dir.walkTopDown().filter { it.isFile && it.extension == "rp" }.toList()
                }
                .sortedBy { it.absolutePath }

        if (files.isEmpty()) {
            return
        }

        val errors = mutableListOf<String>()
        val scripts = mutableListOf<PackScript>()

        for (file in files) {
            try {
                scripts += PackLangParser.parseFile(file)
            } catch (ex: Exception) {
                errors += "${file.absolutePath}: ${ex.message ?: ex::class.simpleName}"
            }
        }

        val tableNames = tables.mapNotNull { it.rscmName }.toSet()
        val matched = scripts.filter { it.phase == phase }
        for (script in matched) {
            errors += RunePackValidator.validate(script, tableNames)
        }

        if (errors.isNotEmpty()) {
            failBuild(errors)
        }

        if (matched.isEmpty()) {
            logger.info { "RunePack($phase): ${files.size} file(s), none for this phase" }
            return
        }

        val runtime = PackLangRuntime(cache, revision, tables, failOnMissingItem = true)
        logger.info { "RunePack($phase): running ${matched.size} script(s)" }
        try {
            for (script in matched) {
                runtime.run(script)
            }
        } catch (ex: Exception) {
            failBuild(listOf(ex.message ?: ex.toString()))
        }
    }

    private fun failBuild(errors: List<String>): Nothing {
        logger.error { "RunePack failed with ${errors.size} error(s):" }
        for (error in errors) {
            logger.error { "  - $error" }
        }
        // BuildCache used to swallow exceptions; exit so Gradle/JavaExec still fails.
        exitProcess(1)
    }
}
