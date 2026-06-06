package org.rsmod.tools.wiki.dumping

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.openrune.rscm.RSCM
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

data class JsonItemRef(
    val wikiName: String,
    val itemId: Int?,
    val internalName: String,
)

data class JsonDropEntry(
    val item: JsonItemRef,
    val quantity: String? = null,
    val weight: Int? = null,
    val rollDenominator: Int? = null,
    val outOf: Int? = null,
    val bonusDrops: List<JsonDropEntry> = emptyList(),
)

data class JsonSeparateRoll(
    val subsection: String,
    val accessNumerator: Int,
    val accessDenominator: Int,
    val entries: List<JsonDropEntry>,
)

data class JsonSubtableAccess(
    val tableRef: String,
    val numerator: Int,
    val denominator: Int,
    val subsection: String,
    val wikiLabel: String = "",
)

data class JsonDropTable(
    val tableId: String,
    val tableName: String,
    val wikiPage: String,
    val guaranteed: List<JsonDropEntry>,
    val preRoll: List<JsonDropEntry>,
    val preRollSeparateRolls: List<JsonSeparateRoll>,
    val main: List<JsonDropEntry>,
    val mainMaxRoll: Int? = null,
    val separateRolls: List<JsonSeparateRoll>,
    val subtableAccesses: List<JsonSubtableAccess>,
    val tertiary: List<JsonDropEntry>,
)

data class JsonNpcManifestEntry(
    val npcId: Int,
    val wikiName: String,
    val tableRef: String,
)

object DropTableJsonExporter {
    private val mapper: ObjectMapper =
        ObjectMapper()
            .registerKotlinModule()
            .setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY)
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)

    fun exportTable(
        result: DumpResult,
        wikiPage: String,
    ): JsonDropTable {
        val spec = result.spec
        return JsonDropTable(
            tableId = spec.tableVarName,
            tableName = spec.tableIdentifier,
            wikiPage = wikiPage,
            guaranteed = result.guaranteedIncludingRemains.mapNotNull { toJsonEntry(it) },
            preRoll = spec.preRoll.mapNotNull { toJsonEntry(it) },
            preRollSeparateRolls = spec.preRollSeparateRolls.map { toJsonSeparateRoll(it) },
            main = spec.main.mapNotNull { toJsonEntry(it) },
            mainMaxRoll = spec.mainMaxRoll,
            separateRolls = spec.separateRolls.map { toJsonSeparateRoll(it) },
            subtableAccesses =
                spec.subtableAccesses.map { access ->
                    JsonSubtableAccess(
                        tableRef = access.tableRef,
                        numerator = access.numerator,
                        denominator = access.denominator,
                        subsection = access.subsection,
                        wikiLabel = access.wikiLabel,
                    )
                },
            tertiary = spec.tertiary.mapNotNull { toJsonEntry(it) },
        )
    }

    fun writeTable(jsonRoot: Path, tableVarName: String, table: JsonDropTable) {
        val output = DropTableJsonOutputLayout.resolveTableFile(jsonRoot, tableVarName)
        output.parent.createDirectories()
        output.writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(table))
    }

    fun writeManifest(jsonRoot: Path, manifest: List<JsonNpcManifestEntry>) {
        val output = DropTableJsonOutputLayout.resolveManifestFile(jsonRoot)
        output.parent.createDirectories()
        output.writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifest))
    }

    fun buildManifest(
        jsonRoot: Path,
        canonical: List<CanonicalJsonExport>,
        duplicates: List<DuplicateDropTable>,
    ): List<JsonNpcManifestEntry> {
        val npcs = mutableListOf<JsonNpcManifestEntry>()

        for (entry in canonical) {
            for (result in entry.results) {
                val tableRef = DropTableJsonOutputLayout.relativeTablePath(jsonRoot, result.spec.tableVarName)
                addNpcManifestEntries(
                    npcs = npcs,
                    wikiPage = entry.wikiPage,
                    tableRef = tableRef,
                    npcIds = result.npcIds,
                )
            }
        }

        val canonicalByWikiPage = canonical.associateBy { it.wikiPage }
        for (dup in duplicates) {
            val canonicalEntry = canonicalByWikiPage[dup.canonicalWikiPage] ?: continue
            val matched =
                canonicalEntry.results.firstOrNull { it.spec.tableVarName == dup.tableVarName }
                    ?: canonicalEntry.results.firstOrNull()
                    ?: continue
            val tableRef = DropTableJsonOutputLayout.relativeTablePath(jsonRoot, dup.tableVarName)

            addNpcManifestEntries(
                npcs = npcs,
                wikiPage = dup.wikiPage,
                tableRef = tableRef,
                npcIds = matched.npcIds,
            )
        }

        return npcs.sortedWith(compareBy({ it.wikiName }, { it.npcId }))
    }

    private fun addNpcManifestEntries(
        npcs: MutableList<JsonNpcManifestEntry>,
        wikiPage: String,
        tableRef: String,
        npcIds: List<Int>,
    ) {
        val wikiName = displayWikiPageName(wikiPage)
        for (npcId in npcIds) {
            npcs +=
                JsonNpcManifestEntry(
                    npcId = npcId,
                    wikiName = wikiName,
                    tableRef = tableRef,
                )
        }
    }

    private fun toJsonSeparateRoll(roll: SeparateRollSpec): JsonSeparateRoll =
        JsonSeparateRoll(
            subsection = roll.subsection,
            accessNumerator = roll.accessNumerator,
            accessDenominator = roll.accessDenominator,
            entries = roll.entries.mapNotNull { toJsonEntry(it) },
        )

    private fun toJsonEntry(entry: ResolvedDropEntry): JsonDropEntry? {
        if (entry.isNothing) {
            return null
        }
        if (entry.brimstoneCombatRoll || entry.brimstoneKonarBonus) {
            return null
        }

        val bonusDrops =
            entry.bonusDrops.mapNotNull { bonus ->
                val objKey = bonus.obj
                if (objKey.isBlank()) {
                    return@mapNotNull null
                }
                JsonDropEntry(
                    item = toItemRef(objKey.removePrefix("obj.").replace('_', ' '), objKey),
                    quantity = bonus.countMax?.let { "${bonus.count}-$it" } ?: bonus.count.toString(),
                )
            }

        if (entry.obj.isBlank()) {
            return null
        }

        return JsonDropEntry(
            item = toItemRef(entry.wikiName, entry.obj),
            quantity = entry.quantity.takeIf { it.isNotBlank() },
            weight = entry.weight,
            rollDenominator = entry.rollDenominator,
            outOf = entry.outOf,
            bonusDrops = bonusDrops,
        )
    }

    private fun toItemRef(wikiName: String, objKey: String): JsonItemRef =
        JsonItemRef(
            wikiName = wikiName,
            itemId = resolveItemId(objKey),
            internalName = stripObjPrefix(objKey),
        )

    private fun resolveItemId(objKey: String): Int? {
        val fullKey = if (objKey.startsWith("obj.")) objKey else "obj.$objKey"
        return runCatching { RSCM.getRSCM(fullKey) }.getOrNull()?.takeIf { it >= 0 }
    }

    private fun stripObjPrefix(objKey: String): String = objKey.removePrefix("obj.")

    data class CanonicalJsonExport(
        val wikiPage: String,
        val results: List<DumpResult>,
    )
}

data class JsonExportConfig(
    val jsonRoot: Path,
    val dumpIndex: NpcDumpIndex,
    val npcLookup: NpcRscmLookup,
)

fun defaultJsonOutputDir(repoRoot: Path?): Path =
    if (repoRoot != null) {
        repoRoot.resolve("content/drops/json")
    } else {
        Path("content/drops/json")
    }
