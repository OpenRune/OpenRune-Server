package dev.openrune.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.buildCodeBlock
import dev.openrune.definition.type.EnumType
import java.io.File

private const val OUT_CONFIG_KT = "../api/generated/src/main/kotlin"
private const val PKG_ENUMS = "org.rsmod.api.enums"
private const val LEGACY_SINGLE_FILE = "org/rsmod/api/GeneratedEnums.kt"

private val cnameEnumTypeMap = ClassName("dev.openrune.types.enums", "EnumTypeMap")

private const val UNNAMED_CLUSTER_KEY = "__unnamed__"

private const val MAX_ENUM_ACCESSORS_PER_FILE = 240

fun startEnumGeneration(enums: Map<Int, EnumType>) {
    val outDir = File(OUT_CONFIG_KT).canonicalFile
    val enumsDir = File(outDir, PKG_ENUMS.replace('.', '/')).canonicalFile
    clearGeneratedEnumSources(enumsDir)
    File(outDir, LEGACY_SINGLE_FILE).delete()

    val entries =
        enums.toSortedMap().mapNotNull { (id, enum) ->
            if (enum.values.isEmpty()) {
                return@mapNotNull null
            }
            val slug = enumInternalSlug(id)
            Triple(id, enum, slug)
        }
    if (entries.isEmpty()) {
        return
    }

    val allSlugs = entries.map { it.third }.toSet()
    val byCluster =
        entries
            .groupBy { (_, _, slug) ->
                if (isSyntheticEnumSlug(slug)) {
                    UNNAMED_CLUSTER_KEY
                } else {
                    clusterKeyForSlug(slug, allSlugs)
                }
            }
            .toSortedMap()

    val singletonNamed = mutableListOf<Triple<Int, EnumType, String>>()

    for ((clusterKey, group) in byCluster) {
        val sorted = group.sortedBy { it.third }
        when {
            clusterKey == UNNAMED_CLUSTER_KEY -> {
                sorted.chunked(MAX_ENUM_ACCESSORS_PER_FILE).forEachIndexed { chunkIdx, chunk ->
                    val objectName = chunkedObjectName("UnnamedEnums", chunkIdx)
                    val comment =
                        if (chunkIdx == 0) {
                            "synthetic enum_* slugs"
                        } else {
                            "synthetic enum_* slugs (part ${chunkIdx + 1})"
                        }
                    writeEnumAccessorObject(outDir, enums, objectName, comment, chunk)
                }
            }
            sorted.size == 1 -> singletonNamed += sorted.single()
            else -> {
                val baseObjectName = pascalEnumsObjectName(clusterKey)
                sorted.chunked(MAX_ENUM_ACCESSORS_PER_FILE).forEachIndexed { chunkIdx, chunk ->
                    val objectName = chunkedObjectName(baseObjectName, chunkIdx)
                    val comment =
                        when {
                            chunkIdx > 0 -> "$clusterKey (part ${chunkIdx + 1})"
                            else -> clusterKey
                        }
                    writeEnumAccessorObject(outDir, enums, objectName, comment, chunk)
                }
            }
        }
    }

    singletonNamed.sortBy { it.third }
    singletonNamed.chunked(MAX_ENUM_ACCESSORS_PER_FILE).forEachIndexed { chunkIdx, chunk ->
        val objectName = namedSingletonBucketObjectName(chunkIdx)
        val comment =
            if (chunkIdx == 0) {
                "named singleton clusters (one enum per slug group)"
            } else {
                "named singleton clusters (part ${chunkIdx + 1})"
            }
        writeEnumAccessorObject(outDir, enums, objectName, comment, chunk)
    }
}

private fun namedSingletonBucketObjectName(chunkIndex: Int): String =
    when (chunkIndex) {
        0 -> "NamedEnums"
        else -> "Named$chunkIndex"
    }

private fun writeEnumAccessorObject(
    outDir: File,
    enums: Map<Int, EnumType>,
    objectName: String,
    commentSuffix: String,
    chunk: List<Triple<Int, EnumType, String>>,
) {
    val usedNames = mutableSetOf<String>()
    val extraImports = linkedSetOf<ClassName>()
    val props = mutableListOf<PropertySpec>()

    for ((id, enum, slug) in chunk) {
        val baseName = enumPropertyName(id, slug)
        val propName = disambiguatePropertyName(baseName, usedNames)
        usedNames += propName

        val (k, v) = kotlinTypePairForEnumDefinition(enum, enums, cnameEnumTypeMap)
        val mapType = cnameEnumTypeMap.parameterizedBy(k, v)
        referencedOpenRuneClassNames(k).forEach { extraImports += it }
        referencedOpenRuneClassNames(v).forEach { extraImports += it }

        val init = buildCodeBlock { add("enum(%S)", slug) }
        props += PropertySpec.builder(propName, mapType, KModifier.PUBLIC).initializer(init).build()
    }

    val obj =
        TypeSpec.objectBuilder(objectName)
            .addModifiers(KModifier.PUBLIC)
            .addProperties(props)
            .build()

    FileSpec.builder(PKG_ENUMS, objectName)
        .addFileComment(
            "AUTO-GENERATED enum accessors — do not edit. Run cache build to refresh. $commentSuffix"
        )
        .addImport("dev.openrune.types.enums", "enum")
        .addImportClass(cnameEnumTypeMap)
        .apply {
            extraImports
                .sortedBy { cn -> cn.packageName + "." + cn.simpleNames.joinToString(".") }
                .forEach { addImportClass(it) }
        }
        .addType(obj)
        .build()
        .writeTo(outDir)
}

private fun isSyntheticEnumSlug(slug: String): Boolean {
    if (!slug.startsWith("enum_")) {
        return false
    }
    val rest = slug.removePrefix("enum_")
    return rest.isNotEmpty() && rest.all { it.isDigit() }
}

private fun chunkedObjectName(baseObjectName: String, chunkIndex: Int): String {
    if (chunkIndex == 0) {
        return baseObjectName
    }
    val stem = baseObjectName.removeSuffix("Enums")
    return "${stem}Enums${chunkIndex + 1}"
}

private fun clearGeneratedEnumSources(root: File) {
    if (!root.exists()) {
        root.mkdirs()
        return
    }
    check(root.isDirectory) { "Enum output path is not a directory: ${root.absolutePath}" }
    root.walkBottomUp()
        .filter { it.isFile && it.extension.equals("kt", ignoreCase = true) }
        .forEach { it.delete() }
    root.walkBottomUp()
        .filter { it.isDirectory && it != root && it.listFiles().isNullOrEmpty() }
        .forEach { it.delete() }
}

/** Trim leading/trailing `_` so `_tree_axes` participates in `tree_*` prefix clusters. */
private fun normalizedSlugForCluster(slug: String): String = slug.trim { it == '_' }

/**
 * Cluster key: plural stem, then longest shared `_` prefix (on [normalizedSlugForCluster]), then
 * shared **last segment** if ≥2 slugs end with that token, then first two segments if ≥3 parts.
 */
private fun clusterKeyForSlug(slug: String, allSlugs: Set<String>): String {
    val n = normalizedSlugForCluster(slug)
    if (n.length > 2 && n.endsWith('s') && !n.endsWith("ss")) {
        val stem = n.dropLast(1)
        val pref = "${stem}_"
        if (allSlugs.any { other ->
                val o = normalizedSlugForCluster(other)
                o != n && o.startsWith(pref)
            }) {
            return stem
        }
    }
    val parts = n.split('_').filter { it.isNotBlank() }
    val shared = longestSharedPrefixCluster(parts, allSlugs)
    if (shared != null) {
        return shared
    }
    val tailCluster = clusterKeyBySharedLastSegment(parts, allSlugs)
    if (tailCluster != null) {
        return tailCluster
    }
    if (parts.size >= 3) {
        return parts.take(2).joinToString("_")
    }
    return n
}

/** When ≥2 slugs share the same final `_` segment (e.g. `axes`), use that segment as cluster key. */
private fun clusterKeyBySharedLastSegment(parts: List<String>, allSlugs: Set<String>): String? {
    if (parts.isEmpty()) {
        return null
    }
    val tail = parts.last()
    if (tail.length < 3) {
        return null
    }
    val tailCount =
        allSlugs.count { s ->
            val segs = normalizedSlugForCluster(s).split('_').filter { it.isNotBlank() }
            segs.lastOrNull() == tail
        }
    return tail.takeIf { tailCount >= 2 }
}

private fun slugUnderPrefix(normalizedSlug: String, normalizedPrefix: String): Boolean =
    normalizedSlug == normalizedPrefix || normalizedSlug.startsWith("${normalizedPrefix}_")

private fun countSlugsUnderPrefix(prefix: String, allSlugs: Set<String>): Int {
    val p = normalizedSlugForCluster(prefix)
    return allSlugs.count { slugUnderPrefix(normalizedSlugForCluster(it), p) }
}

/** Longest `parts.take(len)` (len < parts.size) with ≥2 slugs under that prefix. */
private fun longestSharedPrefixCluster(parts: List<String>, allSlugs: Set<String>): String? {
    if (parts.size < 2) {
        return null
    }
    var best: String? = null
    var bestLen = 0
    for (len in 1 until parts.size) {
        val prefix = parts.take(len).joinToString("_")
        if (countSlugsUnderPrefix(prefix, allSlugs) >= 2 && len > bestLen) {
            best = prefix
            bestLen = len
        }
    }
    return best
}

private fun pascalEnumsObjectName(clusterKey: String): String {
    val parts = clusterKey.split('_').filter { it.isNotBlank() }
    val pascal = parts.joinToString("") { p -> p.replaceFirstChar(Char::uppercase) }
    return "${pascal}Enums"
}

private fun enumPropertyName(id: Int, slug: String): String {
    val raw = slug.replace('-', '_').replace('.', '_').ifBlank { "enum_$id" }
    if (raw.isEmpty()) return "enum_$id"
    val withPrefixIfDigit =
        if (raw.first().isDigit() || raw.first() == '_') {
            "enum_$id"
        } else {
            raw
        }
    return withPrefixIfDigit
}

private fun disambiguatePropertyName(base: String, used: MutableSet<String>): String {
    var n = base
    var i = 0
    while (n in used) {
        i++
        n = "${base}_$i"
    }
    return n
}
