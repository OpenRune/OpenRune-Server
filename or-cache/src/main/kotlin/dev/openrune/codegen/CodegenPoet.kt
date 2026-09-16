package dev.openrune.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import java.io.File

/**
 * Writes generated `*.kt` files under [root] only when their rendered content changed, so
 * unchanged files keep their old mtime/hash and don't force Gradle/Kotlin to recompile every
 * downstream module on every cache build. Call [pruneStale] once generation finishes to delete
 * files from a prior run that weren't rewritten this time (e.g. a removed dbtable/enum).
 *
 * Pass [force] on a from-scratch cache build (`freshCache`) to wipe [root] up front instead of
 * diffing against whatever is already there.
 */
class GeneratedFileTracker(private val root: File, force: Boolean = false) {
    private val before: Set<File> = if (force) wipe(root) else collectKtFiles(root)
    private val written = mutableSetOf<File>()

    fun write(spec: FileSpec) {
        val relative = spec.packageName.replace('.', '/')
        val dir = if (relative.isEmpty()) root else File(root, relative)
        val file = File(dir, "${spec.name}.kt")
        written += file
        val rendered = spec.toString()
        if (file.exists() && file.readText() == rendered) {
            return
        }
        dir.mkdirs()
        file.writeText(rendered)
    }

    fun pruneStale() {
        (before - written).forEach { it.delete() }
        root
            .walkBottomUp()
            .filter { it.isDirectory && it != root && it.listFiles().isNullOrEmpty() }
            .forEach { it.delete() }
    }

    private fun collectKtFiles(dir: File): Set<File> {
        if (!dir.exists()) {
            dir.mkdirs()
            return emptySet()
        }
        check(dir.isDirectory) { "Generated output path is not a directory: ${dir.absolutePath}" }
        return dir
            .walkBottomUp()
            .filter { it.isFile && it.extension.equals("kt", ignoreCase = true) }
            .toSet()
    }

    /** Deletes every `*.kt` (and now-empty dir) under [dir] up front; always returns empty. */
    private fun wipe(dir: File): Set<File> {
        collectKtFiles(dir).forEach { it.delete() }
        dir
            .walkBottomUp()
            .filter { it.isDirectory && it != dir && it.listFiles().isNullOrEmpty() }
            .forEach { it.delete() }
        return emptySet()
    }
}

/**
 * Slug string for [dev.openrune.types.enums.enum] / `enum.${slug}.asRSCM()`: RSCM reverse map with
 * `enum.` prefix stripped; missing / `-1` → `enum_<id>`.
 */
internal fun enumInternalSlug(id: Int): String {
    val rev =
        try {
            RSCM.getReverseMapping(RSCMType.ENUM, id)
        } catch (_: IllegalStateException) {
            return "enum_$id"
        }
    if (rev == "-1" || rev.isBlank()) {
        return "enum_$id"
    }
    return when {
        rev.startsWith("enum.") -> rev.removePrefix("enum.")
        else -> rev.substringAfterLast('.')
    }
}

/** KotlinPoet `addImport(KClass)` requires non-empty `names`; use package + [ClassName.simpleNames]. */
internal fun FileSpec.Builder.addImportClass(cn: ClassName): FileSpec.Builder =
    addImport(cn.packageName, *cn.simpleNames.toTypedArray())

/** Class names referenced by [root] that live in project / OpenRune packages (skip `kotlin.*`). */
internal fun referencedOpenRuneClassNames(root: TypeName): Set<ClassName> {
    val out = linkedSetOf<ClassName>()
    fun walk(t: TypeName) {
        when (t) {
            is ClassName -> {
                if (t.packageName == "kotlin" || t.packageName.startsWith("java.")) {
                    return
                }
                if (t.packageName.startsWith("dev.openrune") || t.packageName.startsWith("org.rsmod")) {
                    out += t
                }
            }
            is ParameterizedTypeName -> {
                walk(t.rawType)
                t.typeArguments.forEach(::walk)
            }
            else -> Unit
        }
    }
    walk(root)
    return out
}
