package dev.openrune.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType

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
