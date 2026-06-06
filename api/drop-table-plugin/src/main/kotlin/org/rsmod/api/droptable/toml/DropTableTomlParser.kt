package org.rsmod.api.droptable.toml

import dtx.rs.RSDropTable
import dtx.rs.RSGuaranteedTable
import dtx.rs.RSPreRollTable
import dtx.rs.RSWeightedTable
import org.rsmod.api.droptable.DropChanceTableScope
import org.rsmod.api.droptable.DropWeightedTableScope
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.PendingDropItemConfig
import org.rsmod.api.droptable.dropRollable
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.requiresRollableWrapper
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import dtx.rs.RSPrerollTableBuilder
import org.rsmod.api.droptable.rsPlayerPrerollTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

public object DropTableTomlParser {
    public fun parse(
        def: TomlDropTableDef,
        resolver: DropTableTomlResolver,
        sourcePath: String = def.id,
    ): RSDropTable<Player, DropRollItem> {
        require(def.npcs.isNotEmpty()) {
            "Drop table '${def.id}' in '$sourcePath' must define at least one npc."
        }

        return RSDropTable(
            tableIdentifier = def.id,
            npcs = def.npcs,
            areas = def.areas,
            guaranteed = buildGuaranteed(def.guaranteed, resolver),
            preRoll = buildPreRoll(def.preRoll, def.preRollSeparateRolls, resolver, sourcePath),
            mainTable = buildMain(def.main, resolver, sourcePath),
            tertiaries = buildTertiary(def, resolver),
        )
    }

    private fun buildGuaranteed(
        entries: List<TomlGuaranteedEntry>,
        resolver: DropTableTomlResolver,
    ): RSGuaranteedTable<Player, DropRollItem> {
        if (entries.isEmpty()) {
            return RSGuaranteedTable.Empty()
        }
        return rsPlayerGuaranteedTable {
            for (entry in entries) {
                val item = buildItem(entry.obj, parseCount(entry), entry.toHooks(), resolver)
                if (item.requiresRollableWrapper()) {
                    add(dropRollable(item))
                } else {
                    add(item)
                }
            }
        }
    }

    private fun buildMain(
        section: TomlWeightedSection?,
        resolver: DropTableTomlResolver,
        sourcePath: String,
    ): RSWeightedTable<Player, DropRollItem> {
        if (section == null) {
            return RSWeightedTable.Empty()
        }
        return rsPlayerWeightedTable(total = section.total) {
            section.name?.let { name(it) }
            for (entry in section.entries) {
                appendTomlWeightedEntry(entry, resolver, sourcePath)
            }
            for (roll in section.separateRolls) {
                require(roll.entries.isNotEmpty()) {
                    "Separate roll ${roll.numerator}/${roll.denominator} in '$sourcePath' must define entries."
                }
                roll.numerator outOf roll.denominator separate {
                    for (entry in roll.entries) {
                        appendTomlWeightedEntry(entry, resolver, sourcePath)
                    }
                }
            }
        }
    }

    private fun DropWeightedTableScope.appendTomlWeightedEntry(
        entry: TomlWeightedEntry,
        resolver: DropTableTomlResolver,
        sourcePath: String,
    ) {
        when {
            entry.nothing -> {
                entry.weight weight nothing()
            }
            entry.shared != null -> {
                entry.weight weight resolver.sharedTable(entry.shared)
            }
            entry.obj != null -> {
                val item = buildItem(entry.obj, parseCount(entry), entry.toHooks(), resolver)
                if (item.requiresRollableWrapper()) {
                    entry.weight weight dropRollable(item)
                } else {
                    entry.weight weight item
                }
            }
            else ->
                error(
                    "Main entry in '$sourcePath' with weight ${entry.weight} " +
                        "must define obj, shared, or nothing = true.",
                )
        }
    }

    private fun buildPreRoll(
        entries: List<TomlChanceEntry>,
        separateRolls: List<TomlSeparateRoll>,
        resolver: DropTableTomlResolver,
        sourcePath: String,
    ): RSPreRollTable<Player, DropRollItem> {
        if (entries.isEmpty() && separateRolls.isEmpty()) {
            return RSPreRollTable.Empty()
        }
        return rsPlayerPrerollTable {
            for (entry in entries) {
                addChanceEntry(entry, resolver, useRolls = true)
            }
            for (roll in separateRolls) {
                require(roll.entries.isNotEmpty()) {
                    "Pre-roll separate roll ${roll.numerator}/${roll.denominator} in '$sourcePath' must define entries."
                }
                roll.numerator outOf roll.denominator rolls buildSeparateRollTable(roll, resolver, sourcePath)
            }
        }
    }

    private fun buildSeparateRollTable(
        roll: TomlSeparateRoll,
        resolver: DropTableTomlResolver,
        sourcePath: String,
    ): RSWeightedTable<Player, DropRollItem> =
        rsPlayerWeightedTable {
            for (entry in roll.entries) {
                appendTomlWeightedEntry(entry, resolver, sourcePath)
            }
        }

    private fun buildTertiary(
        def: TomlDropTableDef,
        resolver: DropTableTomlResolver,
    ): RSPreRollTable<Player, DropRollItem> {
        if (def.tertiary.isEmpty() && !def.brimstoneKeyRoll) {
            return RSPreRollTable.Empty()
        }
        return rsPlayerTertiaryTable {
            if (def.brimstoneKeyRoll) {
                onBuilder { resolver.applyBrimstoneKeyRoll(this, def.brimstoneKeyRollKonarBonus) }
            }
            for (entry in def.tertiary) {
                addChanceEntry(entry, resolver, useRolls = false)
            }
        }
    }

    private fun DropChanceTableScope.addChanceEntry(
        entry: TomlChanceEntry,
        resolver: DropTableTomlResolver,
        useRolls: Boolean,
    ) {
        val item = buildItem(entry.obj, parseCount(entry), entry.toHooks(), resolver)
        if (useRolls) {
            if (item.requiresRollableWrapper()) {
                entry.numerator outOf entry.denominator rolls dropRollable(item)
            } else {
                entry.numerator outOf entry.denominator rolls item
            }
        } else {
            if (item.requiresRollableWrapper()) {
                entry.numerator outOf entry.denominator chance dropRollable(item)
            } else {
                entry.numerator outOf entry.denominator chance item
            }
        }
    }

    private fun buildItem(
        obj: String,
        count: ParsedCount,
        hooks: TomlDropHooks,
        resolver: DropTableTomlResolver,
    ): DropRollItem {
        val config = PendingDropItemConfig(obj, count.range)
        config.countChoices = count.choices
        resolver.applyHooks(config, hooks)
        return config.toItem()
    }

    private data class ParsedCount(
        val range: IntRange,
        val choices: List<Int>? = null,
    )

    private fun parseCount(entry: TomlWeightedEntry): ParsedCount = parseCount(entry.count, entry.countMin, entry.countMax)

    private fun parseCount(entry: TomlGuaranteedEntry): ParsedCount = parseCount(entry.count, entry.countMin, entry.countMax)

    private fun parseCount(entry: TomlChanceEntry): ParsedCount = parseCount(entry.count, entry.countMin, entry.countMax)

    private fun parseCount(count: String?, countMin: Int?, countMax: Int?): ParsedCount {
        if (countMin != null && countMax != null) {
            return ParsedCount(countMin..countMax)
        }
        if (countMin != null) {
            return ParsedCount(countMin..countMin)
        }
        val raw = count ?: return ParsedCount(1..1)
        if (';' in raw) {
            val choices =
                raw.split(';').map { part ->
                    part.trim().toIntOrNull()
                        ?: error("Invalid count choice '${part.trim()}' in '$raw'.")
                }
            require(choices.isNotEmpty()) { "Invalid count list '$raw'." }
            val first = choices.first()
            return ParsedCount(first..first, choices)
        }
        if (".." in raw) {
            val parts = raw.split("..", limit = 2)
            require(parts.size == 2) { "Invalid count range '$raw'." }
            val min = parts[0].trim().toInt()
            val max = parts[1].trim().toInt()
            return ParsedCount(min..max)
        }
        val single = raw.trim().toInt()
        return ParsedCount(single..single)
    }

    private fun TomlWeightedEntry.toHooks(): TomlDropHooks =
        TomlDropHooks(
            shouldDropLootingBag = shouldDropLootingBag,
            shouldDropBrimstoneKey = shouldDropBrimstoneKey,
            clueScrollBox = clueScrollBox,
            quest = quest,
            questMode = questMode,
        )

    private fun TomlGuaranteedEntry.toHooks(): TomlDropHooks =
        TomlDropHooks(
            shouldDropLootingBag = shouldDropLootingBag,
            shouldDropBrimstoneKey = shouldDropBrimstoneKey,
            clueScrollBox = clueScrollBox,
            quest = quest,
            questMode = questMode,
        )

    private fun TomlChanceEntry.toHooks(): TomlDropHooks =
        TomlDropHooks(
            shouldDropLootingBag = shouldDropLootingBag,
            shouldDropBrimstoneKey = shouldDropBrimstoneKey,
            clueScrollBox = clueScrollBox,
            requireRingOfWealth = requireRingOfWealth,
            excludeRingOfWealth = excludeRingOfWealth,
            requireWilderness = requireWilderness,
            quest = quest,
            questMode = questMode,
        )

}
