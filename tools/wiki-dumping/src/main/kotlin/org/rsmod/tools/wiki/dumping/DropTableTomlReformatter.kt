package org.rsmod.tools.wiki.dumping

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.walk
import kotlin.io.path.writeText
import org.rsmod.api.droptable.toml.DropTableTomlTextFixer
import org.rsmod.api.droptable.toml.DropTableTomlWriter
import org.rsmod.api.droptable.toml.TomlDropTableDef

object DropTableTomlReformatter {
    private val readMapper: ObjectMapper =
        ObjectMapper(TomlFactory())
            .registerKotlinModule()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)

    fun reformatFile(path: Path) {
        val raw = DropTableTomlTextFixer.hoistTableLevelKeys(path.toFile().readText())
        val def = readMapper.readValue<TomlDropTableDef>(raw)
        path.writeText(DropTableTomlWriter.write(normalize(def)))
    }

    fun reformatTree(tomlRoot: Path): Int {
        var count = 0
        tomlRoot.walk().filter { it.isRegularFile() && it.fileName.toString().endsWith(".toml") }.forEach { file ->
            reformatFile(file)
            count++
        }
        return count
    }

    private fun normalize(def: TomlDropTableDef): TomlDropTableDef =
        def.copy(
            areas = def.areas.filter { it.isNotBlank() },
            guaranteed = def.guaranteed.map { normalizeGuaranteed(it) },
            preRoll = def.preRoll.map { normalizeChance(it) },
            preRollSeparateRolls =
                def.preRollSeparateRolls.map { roll ->
                    roll.copy(entries = roll.entries.map { normalizeWeighted(it) })
                },
            tertiary = def.tertiary.map { normalizeChance(it) },
            main =
                def.main?.let { main ->
                    main.copy(
                        entries = main.entries.map { normalizeWeighted(it) },
                        separateRolls =
                            main.separateRolls.map { roll ->
                                roll.copy(entries = roll.entries.map { normalizeWeighted(it) })
                            },
                    )
                },
            notes = def.notes.map { it.trim() }.filter { it.isNotBlank() },
        )

    private fun normalizeGuaranteed(entry: org.rsmod.api.droptable.toml.TomlGuaranteedEntry) =
        entry.copy(
            obj = entry.obj.trim(),
            count = entry.count?.trim()?.ifBlank { null },
            quest = entry.quest?.trim()?.ifBlank { null },
            questMode = entry.questMode?.trim()?.ifBlank { null },
        )

    private fun normalizeChance(entry: org.rsmod.api.droptable.toml.TomlChanceEntry) =
        entry.copy(
            obj = entry.obj.trim(),
            count = entry.count?.trim()?.ifBlank { null },
            quest = entry.quest?.trim()?.ifBlank { null },
            questMode = entry.questMode?.trim()?.ifBlank { null },
        )

    private fun normalizeWeighted(entry: org.rsmod.api.droptable.toml.TomlWeightedEntry): org.rsmod.api.droptable.toml.TomlWeightedEntry {
        val shared = entry.shared?.trim()?.ifBlank { null }
        val obj = entry.obj?.trim()?.ifBlank { null }
        return entry.copy(
            obj = if (entry.nothing || shared != null) null else obj,
            shared = if (entry.nothing) null else shared,
            count = entry.count?.trim()?.ifBlank { null },
            quest = entry.quest?.trim()?.ifBlank { null },
            questMode = entry.questMode?.trim()?.ifBlank { null },
            shouldDropLootingBag = entry.shouldDropLootingBag,
            shouldDropBrimstoneKey = entry.shouldDropBrimstoneKey,
        )
    }
}

fun main(args: Array<String>) {
    val root =
        args.firstOrNull()?.let { Path.of(it) }
            ?: defaultTomlOutputDir(findRepoRoot())
    val count = DropTableTomlReformatter.reformatTree(root)
    println("Reformatted $count drop table TOML file(s) under $root")
}

private fun findRepoRoot(): Path? {
    var dir = Path.of("").toAbsolutePath()
    repeat(8) {
        if (dir.resolve("settings.gradle.kts").toFile().exists() || dir.resolve("build.gradle.kts").toFile().exists()) {
            return dir
        }
        dir = dir.parent ?: return null
    }
    return null
}
