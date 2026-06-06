package org.rsmod.tools.wiki.dumping

import org.rsmod.api.droptable.toml.DropTableTomlWriter
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import org.rsmod.api.droptable.toml.TomlDropTableDef
import org.rsmod.api.droptable.toml.TomlChanceEntry
import org.rsmod.api.droptable.toml.TomlGuaranteedEntry
import org.rsmod.api.droptable.toml.TomlSeparateRoll
import org.rsmod.api.droptable.toml.TomlWeightedEntry
import org.rsmod.api.droptable.toml.TomlWeightedSection
import org.rsmod.tools.wiki.dumping.wiki.WikiQuestDropMode

private val SHARED_TABLE_NAMES =
    setOf("herb", "usefulHerb", "combatHerb", "gem", "seed", "rareSeed", "megaRare", "rareDrop")

private val CLUE_SCROLL_BOX_TRANSFORM_NOTE =
    Regex("""scroll\s*box|x\s*marks\s*the\s*spot""", RegexOption.IGNORE_CASE)

object DropTableTomlExporter {
    fun isSimpleForTomlExport(spec: GeneratedDropTableSpec): Boolean = tomlExportBlockers(spec).isEmpty()

    fun tomlExportBlockers(spec: GeneratedDropTableSpec): List<String> {
        val blockers = mutableListOf<String>()
        if (!spec.hasDropContent() && !spec.hasBrimstoneKeyRollOnly()) {
            blockers += "no drop content"
        }
        spec.preRollSeparateRolls.forEach { roll ->
            if (roll.entries.isEmpty()) {
                blockers += "pre-roll separate roll ${roll.accessNumerator}/${roll.accessDenominator} has no entries"
            }
            roll.entries.filterNot { it.isSimpleTomlEntry() }.forEach { entry ->
                blockers += "pre-roll separate entry not TOML-simple: ${entry.obj.ifBlank { entry.wikiName }}"
            }
        }
        spec.separateRolls.forEach { roll ->
            if (roll.entries.isEmpty()) {
                blockers += "main separate roll ${roll.accessNumerator}/${roll.accessDenominator} has no entries"
            }
            roll.entries.forEach { entry ->
                entry.tomlEntryBlockers().forEach { reason ->
                    blockers += "main separate entry not TOML-simple (${entry.obj}): $reason"
                }
            }
        }
        if (spec.unmappedItems.isNotEmpty()) {
            blockers += "unmapped items: ${spec.unmappedItems.joinToString()}"
        }
        spec.subtableAccesses.forEach { access ->
            if (!access.herbRollVariants.isNullOrEmpty()) {
                blockers += "herb roll variants on ${access.tableRef}"
            }
        }
        spec.guaranteed.filterNot { it.isSimpleTomlEntry() }.forEach { entry ->
            blockers += "guaranteed entry not TOML-simple: ${entry.obj}"
        }
        spec.preRoll.forEach { entry ->
            if (!entry.isSimpleTomlEntry()) {
                blockers += "pre-roll entry not TOML-simple: ${entry.obj}"
            }
            if (entry.chanceDenominator() == null) {
                blockers += "pre-roll entry missing outOf: ${entry.obj}"
            }
        }
        spec.main.forEach { entry ->
            entry.tomlEntryBlockers().forEach { reason ->
                blockers += "main entry not TOML-simple (${entry.obj.ifBlank { entry.wikiName }}): $reason"
            }
        }
        spec.tertiary.forEach { entry ->
            if (!entry.isSimpleTomlTertiaryEntry()) {
                blockers += "tertiary entry not TOML-simple: ${entry.obj.ifBlank { entry.wikiName }}"
            } else if (!entry.brimstoneCombatRoll && entry.chanceDenominator() == null) {
                blockers += "tertiary entry missing outOf: ${entry.obj.ifBlank { entry.wikiName }}"
            }
        }
        return blockers
    }

    private fun GeneratedDropTableSpec.hasBrimstoneKeyRollOnly(): Boolean =
        tertiary.all { it.brimstoneCombatRoll } &&
            guaranteed.isEmpty() &&
            preRoll.isEmpty() &&
            main.isEmpty() &&
            subtableAccesses.isEmpty() &&
            separateRolls.isEmpty()

    fun exportTable(spec: GeneratedDropTableSpec): TomlDropTableDef {
        require(isSimpleForTomlExport(spec)) {
            "Drop table '${spec.tableIdentifier}' is not simple enough for TOML export."
        }

        val mainEntries = buildList {
            addAll(spec.main.map { it.toTomlWeightedEntry() })
            addAll(
                spec.subtableAccesses.filter { it.isTomlExportableSharedAccess() }.map { access ->
                    TomlWeightedEntry(
                        weight = access.numerator,
                        shared = access.sharedTableName(),
                    )
                },
            )
            val nothingWeight = spec.poolPaddingWeight()
            if (nothingWeight > 0) {
                add(TomlWeightedEntry(weight = nothingWeight, nothing = true))
            }
        }

        val separateRolls = spec.separateRolls.map { it.toTomlSeparateRoll() }
        val preRollSeparateRolls = spec.preRollSeparateRolls.map { it.toTomlSeparateRoll() }
        val tertiaryEntries = spec.tertiary.filter { !it.brimstoneCombatRoll }
        val brimstoneKeyRoll = spec.tertiary.any { it.brimstoneCombatRoll }
        val brimstoneKeyRollKonarBonus = spec.tertiary.any { it.brimstoneKonarBonus }

        return TomlDropTableDef(
            id = spec.tableIdentifier,
            npcs = spec.npcRscmKeys,
            areas = spec.areaRscmKeys.takeIf { it.isNotEmpty() } ?: emptyList(),
            guaranteed = spec.guaranteed.map { it.toTomlGuaranteedEntry() },
            preRoll = spec.preRoll.map { it.toTomlChanceEntry() },
            preRollSeparateRolls = preRollSeparateRolls,
            main =
                if (mainEntries.isEmpty() && separateRolls.isEmpty()) {
                    null
                } else {
                    TomlWeightedSection(
                        total = spec.mainMaxRoll,
                        name = spec.tableIdentifier,
                        entries = mainEntries,
                        separateRolls = separateRolls,
                    )
                },
            tertiary = tertiaryEntries.flatMap { it.toTomlChanceEntries() },
            brimstoneKeyRoll = brimstoneKeyRoll,
            brimstoneKeyRollKonarBonus = brimstoneKeyRollKonarBonus,
            notes = buildTomlExportNotes(spec),
        )
    }

    fun writeTable(tomlRoot: Path, tableVarName: String, table: TomlDropTableDef) {
        val output = DropTableTomlOutputLayout.resolveTableFile(tomlRoot, tableVarName)
        output.parent.createDirectories()
        output.writeText(DropTableTomlWriter.write(table))
    }
}

object DropTableTomlOutputLayout {
    const val MONSTERS_DIR = "monsters"

    fun resolveTableFile(tomlRoot: Path, tableVarName: String): Path {
        val fileName = tableVarName.removeSuffix("DropTable").replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
        return tomlRoot.resolve(MONSTERS_DIR).resolve("$fileName.toml")
    }
}

private fun ResolvedSubtableAccess.sharedTableName(): String? {
    val ref = tableRef.removePrefix("SharedDropTables.")
    return ref.takeIf { it in SHARED_TABLE_NAMES }
}

private fun ResolvedSubtableAccess.isKnownSharedTableRef(): Boolean = sharedTableName() != null

private fun ResolvedDropEntry.isSimpleTomlEntry(): Boolean = tomlEntryBlockers().isEmpty()

private fun ResolvedDropEntry.tomlEntryBlockers(): List<String> {
    val blockers = mutableListOf<String>()
    if (!isNothing && obj.isBlank()) {
        blockers += "missing obj"
    }
    if (bonusDrops.isNotEmpty()) {
        blockers += "bonus drops"
    }
    if (brimstoneCombatRoll || brimstoneKonarBonus) {
        blockers += "brimstone roll marker"
    }
    if (wikiNotes.hasTransformItem && !isClueScrollBoxTransform()) {
        blockers += "transform item note"
    }
    wikiNotes.transformRate.filterNot(::isHandledTransformRateNote).forEach { note ->
        blockers += "transform rate: ${note.take(80)}"
    }
    if (wikiNotes.hasCondition && !hasSupportedTomlCondition()) {
        blockers += "condition: ${wikiNotes.condition.joinToString().take(80)}"
    }
    return blockers
}

private fun ResolvedDropEntry.hasSupportedTomlCondition(): Boolean =
    requiresLootingBagCondition() ||
        requiresBrimstoneKeyCondition() ||
        wikiNotes.lootingBagWilderness ||
        wikiNotes.brimstoneKonarTask ||
        wikiNotes.hasQuestRequirement

private fun SeparateRollSpec.toTomlSeparateRoll(): TomlSeparateRoll =
    TomlSeparateRoll(
        numerator = accessNumerator,
        denominator = accessDenominator,
        entries = entries.map { it.toTomlWeightedEntry() },
    )

private fun ResolvedDropEntry.toTomlWeightedEntry(): TomlWeightedEntry =
    TomlWeightedEntry(
        weight = weight ?: error("Main entry '${obj}' is missing weight."),
        obj = obj,
        count = formatTomlCount(),
        shouldDropLootingBag = requiresLootingBagCondition(),
        shouldDropBrimstoneKey = requiresBrimstoneKeyCondition(),
        clueScrollBox = isClueScrollBoxTransform(),
        quest = wikiNotes.questRequirements.firstOrNull()?.questKey,
        questMode = wikiNotes.questRequirements.firstOrNull()?.toTomlQuestMode(),
        nothing = isNothing,
    )

private fun ResolvedDropEntry.toTomlGuaranteedEntry(): TomlGuaranteedEntry =
    TomlGuaranteedEntry(
        obj = obj,
        count = formatTomlCount(),
        shouldDropLootingBag = requiresLootingBagCondition(),
        shouldDropBrimstoneKey = requiresBrimstoneKeyCondition(),
        clueScrollBox = isClueScrollBoxTransform(),
        quest = wikiNotes.questRequirements.firstOrNull()?.questKey,
        questMode = wikiNotes.questRequirements.firstOrNull()?.toTomlQuestMode(),
    )

private fun ResolvedDropEntry.isSimpleTomlTertiaryEntry(): Boolean =
    brimstoneCombatRoll || isSimpleTomlEntry()

private fun ResolvedDropEntry.chanceDenominator(): Int? = outOf ?: rollDenominator

private fun ResolvedDropEntry.toTomlChanceEntry(): TomlChanceEntry =
    TomlChanceEntry(
        numerator = weight ?: 1,
        denominator =
            exportClueDenominator(
                chanceDenominator() ?: error("Chance entry '${obj}' is missing outOf."),
            ),
        obj = obj,
        count = formatTomlCount(),
        shouldDropLootingBag = requiresLootingBagCondition(),
        shouldDropBrimstoneKey = requiresBrimstoneKeyCondition(),
        clueScrollBox = isClueScrollBoxTransform(),
        quest = wikiNotes.questRequirements.firstOrNull()?.questKey,
        questMode = wikiNotes.questRequirements.firstOrNull()?.toTomlQuestMode(),
    )

private fun ResolvedDropEntry.toTomlChanceEntries(): List<TomlChanceEntry> {
    val base = toTomlChanceEntry()
    val wealth = ringOfWealthClueRate() ?: return listOf(base)
    return listOf(
        base.copy(excludeRingOfWealth = true),
        base.copy(
            denominator = wealth.wealthDenominator,
            requireRingOfWealth = true,
            requireWilderness = wealth.requiresWilderness,
        ),
    )
}

private fun ResolvedDropEntry.isClueScrollBoxTransform(): Boolean =
    wikiNotes.transformItem.any { CLUE_SCROLL_BOX_TRANSFORM_NOTE.containsMatchIn(it) }

private fun ResolvedDropEntry.formatTomlCount(): String {
    val range = quantityRange()
    if (range != null) {
        val (min, max) = range
        return if (min == max) min.toString() else "$min..$max"
    }
    return quantity.ifBlank { "1" }
}

private fun ResolvedDropEntry.quantityRange(): Pair<Int, Int>? {
    val match = Regex("""(\d+)\s*-\s*(\d+)""").find(quantity) ?: return null
    val min = match.groupValues[1].toIntOrNull() ?: return null
    val max = match.groupValues[2].toIntOrNull() ?: return null
    return min to max
}

private fun ResolvedDropEntry.requiresLootingBagCondition(): Boolean =
    obj == LOOTING_BAG_OBJ || wikiNotes.lootingBagWilderness

private fun ResolvedDropEntry.requiresBrimstoneKeyCondition(): Boolean =
    !brimstoneCombatRoll && (obj == BRIMSTONE_KEY_OBJ || wikiNotes.brimstoneKonarTask)

private fun org.rsmod.tools.wiki.dumping.wiki.WikiQuestDropRequirement.toTomlQuestMode(): String =
    when (mode) {
        WikiQuestDropMode.RequiresCompleted -> "completed"
        WikiQuestDropMode.RequiresDuring -> "during"
        WikiQuestDropMode.RequiresNotCompleted -> "not_completed"
    }

private const val LOOTING_BAG_OBJ = "obj.looting_bag"
private const val BRIMSTONE_KEY_OBJ = "obj.konar_key"

data class TomlExportConfig(
    val tomlRoot: Path,
)

fun defaultTomlOutputDir(repoRoot: Path?): Path =
    if (repoRoot != null) {
        repoRoot.resolve("content/drops/src/main/resources/drops/tables")
    } else {
        Path.of("content/drops/src/main/resources/drops/tables")
    }
