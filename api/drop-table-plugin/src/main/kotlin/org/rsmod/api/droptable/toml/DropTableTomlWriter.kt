package org.rsmod.api.droptable.toml

public object DropTableTomlWriter {
    public fun write(def: TomlDropTableDef): String {
        val sb = StringBuilder()
        sb.appendLine("id = ${quote(def.id)}")
        sb.appendLine("npcs = ${quoteList(def.npcs)}")
        if (def.areas.isNotEmpty()) {
            sb.appendLine("areas = ${quoteList(def.areas)}")
        }
        if (def.brimstoneKeyRoll) {
            sb.appendLine("brimstone_key_roll = true")
            if (def.brimstoneKeyRollKonarBonus) {
                sb.appendLine("brimstone_key_roll_konar_bonus = true")
            }
        }
        sb.appendLine()

        for (entry in def.guaranteed) {
            sb.appendLine("[[guaranteed]]")
            appendGuaranteedFields(sb, entry)
            sb.appendLine()
        }

        for (entry in def.preRoll) {
            sb.appendLine("[[pre_roll]]")
            appendChanceFields(sb, entry)
            sb.appendLine()
        }
        for (roll in def.preRollSeparateRolls) {
            sb.appendLine("[[pre_roll_separate_rolls]]")
            sb.appendLine("numerator = ${roll.numerator}")
            sb.appendLine("denominator = ${roll.denominator}")
            sb.appendLine()
            for (entry in roll.entries) {
                sb.appendLine("[[pre_roll_separate_rolls.entries]]")
                appendWeightedFields(sb, entry)
                sb.appendLine()
            }
        }

        def.main?.let { section ->
            sb.appendLine("[main]")
            section.total?.let { sb.appendLine("total = $it") }
            section.name?.let { sb.appendLine("name = ${quote(it)}") }
            sb.appendLine()
            for (entry in section.entries) {
                sb.appendLine("[[main.entries]]")
                appendWeightedFields(sb, entry)
                sb.appendLine()
            }
            for (roll in section.separateRolls) {
                sb.appendLine("[[main.separate_rolls]]")
                sb.appendLine("numerator = ${roll.numerator}")
                sb.appendLine("denominator = ${roll.denominator}")
                sb.appendLine()
                for (entry in roll.entries) {
                    sb.appendLine("[[main.separate_rolls.entries]]")
                    appendWeightedFields(sb, entry)
                    sb.appendLine()
                }
            }
        }

        for (entry in def.tertiary) {
            sb.appendLine("[[tertiary]]")
            appendChanceFields(sb, entry)
            sb.appendLine()
        }

        if (def.notes.isNotEmpty()) {
            sb.appendLine()
            for (note in def.notes) {
                sb.appendLine("# ${note.replace("\n", " ").trim()}")
            }
        }

        return sb.toString().trimEnd() + "\n"
    }

    private fun appendGuaranteedFields(sb: StringBuilder, entry: TomlGuaranteedEntry) {
        sb.appendLine("obj = ${quote(entry.obj)}")
        appendCount(sb, entry.count, entry.countMin, entry.countMax)
        appendHooks(sb, entry.toHooks())
    }

    private fun appendChanceFields(sb: StringBuilder, entry: TomlChanceEntry) {
        sb.appendLine("numerator = ${entry.numerator}")
        sb.appendLine("denominator = ${entry.denominator}")
        sb.appendLine("obj = ${quote(entry.obj)}")
        appendCount(sb, entry.count, entry.countMin, entry.countMax)
        appendHooks(sb, entry.toHooks())
    }

    private fun appendWeightedFields(sb: StringBuilder, entry: TomlWeightedEntry) {
        sb.appendLine("weight = ${entry.weight}")
        when {
            entry.nothing -> sb.appendLine("nothing = true")
            entry.shared != null -> sb.appendLine("shared = ${quote(entry.shared)}")
            entry.obj != null -> {
                sb.appendLine("obj = ${quote(entry.obj)}")
                appendCount(sb, entry.count, entry.countMin, entry.countMax)
            }
        }
        appendHooks(sb, entry.toHooks())
    }

    private fun appendCount(
        sb: StringBuilder,
        count: String?,
        countMin: Int?,
        countMax: Int?,
    ) {
        when {
            countMin != null && countMax != null -> {
                if (countMin == countMax) {
                    sb.appendLine("count = $countMin")
                } else {
                    sb.appendLine("count = ${quote("$countMin..$countMax")}")
                }
            }
            countMin != null -> sb.appendLine("count = $countMin")
            count != null && count.isNotBlank() -> appendCountValue(sb, count)
        }
    }

    private fun appendCountValue(sb: StringBuilder, count: String) {
        if (".." in count) {
            sb.appendLine("count = ${quote(count)}")
        } else {
            count.toIntOrNull()?.let { sb.appendLine("count = $it") }
                ?: sb.appendLine("count = ${quote(count)}")
        }
    }

    private fun appendHooks(sb: StringBuilder, hooks: TomlDropHooks) {
        if (hooks.shouldDropLootingBag) {
            sb.appendLine("should_drop_looting_bag = true")
        }
        if (hooks.shouldDropBrimstoneKey) {
            sb.appendLine("should_drop_brimstone_key = true")
        }
        if (hooks.clueScrollBox) {
            sb.appendLine("clue_scroll_box = true")
        }
        if (hooks.requireRingOfWealth) {
            sb.appendLine("require_ring_of_wealth = true")
        }
        if (hooks.excludeRingOfWealth) {
            sb.appendLine("exclude_ring_of_wealth = true")
        }
        if (hooks.requireWilderness) {
            sb.appendLine("require_wilderness = true")
        }
        hooks.quest?.takeIf { it.isNotBlank() }?.let { quest ->
            sb.appendLine("quest = ${quote(quest)}")
            hooks.questMode?.takeIf { it.isNotBlank() }?.let { mode ->
                sb.appendLine("quest_mode = ${quote(mode)}")
            }
        }
    }

    private fun quote(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private fun quoteList(values: List<String>): String =
        values.joinToString(prefix = "[", postfix = "]") { quote(it) }

    private fun TomlGuaranteedEntry.toHooks(): TomlDropHooks =
        TomlDropHooks(
            shouldDropLootingBag = shouldDropLootingBag,
            shouldDropBrimstoneKey = shouldDropBrimstoneKey,
            clueScrollBox = clueScrollBox,
            quest = quest,
            questMode = questMode,
        )

    private fun TomlWeightedEntry.toHooks(): TomlDropHooks =
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
