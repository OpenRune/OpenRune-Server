package org.rsmod.tools.wiki.dumping

import org.rsmod.tools.wiki.dumping.wiki.HerbDropTableParser
import org.rsmod.tools.wiki.dumping.wiki.HerbRollVariant
import org.rsmod.tools.wiki.dumping.wiki.ParsedWikiDrop
import org.rsmod.tools.wiki.dumping.wiki.WikiCompanionDropParser
import org.rsmod.tools.wiki.dumping.wiki.WikiDropNotes
import org.rsmod.tools.wiki.dumping.wiki.WikiQuestDropMode
import org.rsmod.tools.wiki.dumping.wiki.WikiQuestDropRequirement
import org.rsmod.tools.wiki.dumping.wiki.WikiDropParser
import org.rsmod.tools.wiki.dumping.wiki.WikiDropSection
import org.rsmod.tools.wiki.dumping.wiki.WikiCompanionDropSpec
import dtx.rs.brimstoneRarityDenominator

data class UnknownDropRateEntry(
    val wikiPage: String,
    val tableName: String,
    val itemName: String,
    val section: String,
    val subsection: String,
    val rarity: String,
) {
    fun manifestLine(): String =
        listOf(wikiPage, itemName, section, subsection.ifBlank { "-" }, rarity).joinToString("\t")
}

data class ResolvedDropEntry(
    val obj: String,
    val quantity: String,
    val weight: Int? = null,
    val outOf: Int? = null,
    val rollDenominator: Int? = null,
    val wikiName: String,
    val wikiRarity: String = "",
    val subsection: String = "",
    val wikiNotes: WikiDropNotes = WikiDropNotes(),
    val isNothing: Boolean = false,
    val bonusDrops: List<ResolvedBonusDrop> = emptyList(),
    val brimstoneCombatRoll: Boolean = false,
    val brimstoneKonarBonus: Boolean = false,
)

data class ResolvedBonusDrop(
    val obj: String,
    val count: Int = 1,
    val countMax: Int? = null,
    val bodyTypeRequired: Int? = null,
)

data class SeparateRollSpec(
    val subsection: String,
    val accessNumerator: Int,
    val accessDenominator: Int,
    val entries: List<ResolvedDropEntry>,
)

data class ResolvedSubtableAccess(
    val tableRef: String,
    val numerator: Int,
    val denominator: Int,
    val subsection: String,
    val wikiLabel: String = "",
    val needsHardcodedSharedTable: Boolean = false,
    val herbRollVariants: List<HerbRollVariant>? = null,
)

data class GeneratedDropTableSpec(
    val tableVarName: String,
    val tableIdentifier: String,
    val npcRscmKeys: List<String>,
    val areaRscmKeys: List<String> = emptyList(),
    val guaranteed: List<ResolvedDropEntry>,
    val main: List<ResolvedDropEntry>,
    val mainMaxRoll: Int? = null,
    val subtableAccesses: List<ResolvedSubtableAccess> = emptyList(),
    val separateRolls: List<SeparateRollSpec> = emptyList(),
    val preRoll: List<ResolvedDropEntry> = emptyList(),
    val preRollSeparateRolls: List<SeparateRollSpec> = emptyList(),
    val tertiary: List<ResolvedDropEntry>,
    val unmappedItems: List<String> = emptyList(),
    val unmappedLabel: String? = null,
    val unknownDropRates: List<UnknownDropRateEntry> = emptyList(),
) {
    fun hasDropContent(): Boolean =
        guaranteed.isNotEmpty() ||
            main.isNotEmpty() ||
            subtableAccesses.isNotEmpty() ||
            separateRolls.isNotEmpty() ||
            preRoll.isNotEmpty() ||
            preRollSeparateRolls.isNotEmpty() ||
            tertiary.isNotEmpty()

    fun allResolvedEntries(): List<ResolvedDropEntry> =
        buildList {
            addAll(guaranteed)
            addAll(main)
            addAll(preRoll)
            addAll(tertiary)
            for (roll in separateRolls) {
                addAll(roll.entries)
            }
            for (roll in preRollSeparateRolls) {
                addAll(roll.entries)
            }
        }

    companion object {
        private const val PRE_ROLL_SUBSECTION = "Pre-roll"

        fun isPreRollSubsection(subsection: String): Boolean =
            subsection.equals(PRE_ROLL_SUBSECTION, ignoreCase = true)

        fun scaleDecimalMainWeights(
            main: List<ResolvedDropEntry>,
            mainMaxRoll: Int?,
        ): Pair<List<ResolvedDropEntry>, Int?> {
            val scale = tableWeightScale(main)
            if (scale <= 1 || mainMaxRoll == null) {
                return main to mainMaxRoll
            }
            val scaledMain =
                main.map { entry ->
                    entry.copy(
                        weight = entry.weight?.times(scale),
                        rollDenominator = entry.rollDenominator?.times(scale),
                    )
                }
            return scaledMain to mainMaxRoll * scale
        }

        fun reconcileMainMaxRoll(
            main: List<ResolvedDropEntry>,
            mainMaxRoll: Int?,
            subtableAccesses: List<ResolvedSubtableAccess>,
        ): Int? {
            val pool = mainMaxRoll ?: return null
            val used = main.sumOf { it.weight ?: 0 } + subtableAccesses.sumOf { it.numerator }
            return maxOf(pool, used)
        }

        private fun tableWeightScale(main: List<ResolvedDropEntry>): Int {
            val maxDecimalPlaces =
                main.maxOfOrNull { entry -> decimalPlacesInRarity(entry.wikiRarity) } ?: 0
            if (maxDecimalPlaces == 0) {
                return 1
            }
            var scale = 1
            repeat(maxDecimalPlaces) { scale *= 10 }
            return scale
        }

        private fun decimalPlacesInRarity(rarity: String): Int {
            val numerator = rarity.substringBefore('/').trim()
            if (!numerator.contains('.')) {
                return 0
            }
            return numerator.substringAfter('.').length
        }

        fun finalizeMainRolls(
            main: List<ResolvedDropEntry>,
            subtableAccesses: List<ResolvedSubtableAccess>,
        ): Triple<List<ResolvedDropEntry>, Int?, List<SeparateRollSpec>> {
            if (main.isEmpty()) {
                val accessDenom = subtableAccesses.firstOrNull()?.denominator
                return Triple(emptyList(), accessDenom, emptyList())
            }

            val byDenominator =
                main.groupBy { entry ->
                    entry.rollDenominator ?: subtableAccesses.firstOrNull()?.denominator ?: 128
                }

            val primaryDenominator =
                byDenominator.maxWithOrNull(
                    compareBy<Map.Entry<Int, List<ResolvedDropEntry>>> { it.value.size }
                        .thenBy { entry -> entry.value.sumOf { it.weight ?: 0 } }
                        .thenBy { -it.key },
                )?.key
                    ?: 128

            val mainEntries = byDenominator[primaryDenominator].orEmpty()
            val separateRolls =
                byDenominator
                    .filterKeys { it != primaryDenominator }
                    .flatMap { (denominator, entries) ->
                        buildSeparateRollSpecs(entries, denominator)
                    }

            return Triple(mainEntries, primaryDenominator, separateRolls)
        }

        private fun buildSeparateRollSpecs(
            entries: List<ResolvedDropEntry>,
            denominator: Int,
        ): List<SeparateRollSpec> {
            return entries
                .groupBy { it.subsection.ifBlank { "Other" } }
                .flatMap { (subsection, sectionEntries) ->
                    sectionEntries
                        .groupBy { it.weight ?: 1 }
                        .map { (accessNumerator, weightEntries) ->
                            SeparateRollSpec(
                                subsection = subsection,
                                accessNumerator = accessNumerator,
                                accessDenominator = denominator,
                                entries = weightEntries,
                            )
                        }
                }
        }
    }
}

object DropTableCodeGenerator {
    private const val LOOTING_BAG_OBJ = "obj.looting_bag"
    private const val BRIMSTONE_KEY_OBJ = "obj.konar_key"

    fun generate(spec: GeneratedDropTableSpec): String = generateFile(listOf(spec))

    fun generateFile(specs: List<GeneratedDropTableSpec>, packageName: String = "org.rsmod.content.drops.tables"): String {
        require(specs.isNotEmpty())

        val builder = StringBuilder()
        val needsNothingIfRing =
            specs.any { spec ->
                spec.main.any { it.isNothing } ||
                    spec.separateRolls.any { roll -> roll.entries.any { it.isNothing } }
            }
        val needsNothingPadding = specs.any(::mainMaxRollRequiresNothing)
        val needsNothingImports = needsNothingIfRing || needsNothingPadding
        val needsPreRoll = specs.any { it.preRoll.isNotEmpty() || it.preRollSeparateRolls.isNotEmpty() }
        val needsBodyTypeConstants =
            specs.any { spec ->
                spec.allResolvedEntries().any { entry ->
                    entry.bonusDrops.any { it.bodyTypeRequired != null }
                }
            }
        val needsLootingBagChecks =
            specs.any { spec ->
                spec.allResolvedEntries().any { it.requiresLootingBagCondition() }
            }
        val needsBrimstoneKeyChecks =
            specs.any { spec ->
                spec.allResolvedEntries().any { it.requiresBrimstoneKeyCondition() && !it.brimstoneCombatRoll }
            }
        val needsBrimstoneKeyRoll =
            specs.any { spec ->
                spec.tertiary.any { it.brimstoneCombatRoll }
            }
        val needsQuestChecks =
            specs.any { spec ->
                spec.allResolvedEntries().any { it.requiresQuestCondition() }
            }
        val needsDropRollableImports =
            specs.any { spec ->
                spec.allResolvedEntries().any { entry -> entry.bonusDrops.isNotEmpty() }
            }
        builder.appendFileHeader(
            packageName = packageName,
            needsSharedDropTables =
                specs.any { spec ->
                    spec.subtableAccesses.any { it.herbRollVariants.isNullOrEmpty() }
                },
            needsInlineHerbTables =
                specs.any { spec ->
                    spec.subtableAccesses.any { !it.herbRollVariants.isNullOrEmpty() }
                },
            needsNothingImports = needsNothingImports,
            needsNothingPadding = needsNothingPadding,
            needsNothingIfRing = needsNothingIfRing,
            needsPreRoll = needsPreRoll,
            needsBodyTypeConstants = needsBodyTypeConstants,
            needsLootingBagChecks = needsLootingBagChecks,
            needsBrimstoneKeyChecks = needsBrimstoneKeyChecks,
            needsBrimstoneKeyRoll = needsBrimstoneKeyRoll,
            needsQuestChecks = needsQuestChecks,
            needsDropRollableImports = needsDropRollableImports,
        )
        for ((index, spec) in specs.withIndex()) {
            if (index > 0) {
                builder.appendLine()
            }
            builder.appendTable(spec)
        }

        for (spec in specs) {
            builder.appendUnmappedItems(spec.unmappedItems, spec.unmappedLabel)
            builder.appendUnknownDropRates(spec.unknownDropRates)
        }

        return builder.toString()
    }

    fun tableBodyForDedup(spec: GeneratedDropTableSpec): String {
        val builder = StringBuilder()
        builder.appendTable(
            spec.copy(
                tableVarName = "dropTable",
                tableIdentifier = "Drop Table",
                unmappedLabel = null,
            ),
        )
        return builder.toString().trim()
    }

    private fun StringBuilder.appendFileHeader(
        packageName: String,
        needsSharedDropTables: Boolean,
        needsInlineHerbTables: Boolean,
        needsNothingImports: Boolean,
        needsNothingPadding: Boolean,
        needsNothingIfRing: Boolean,
        needsPreRoll: Boolean,
        needsBodyTypeConstants: Boolean,
        needsLootingBagChecks: Boolean,
        needsBrimstoneKeyChecks: Boolean,
        needsBrimstoneKeyRoll: Boolean,
        needsQuestChecks: Boolean,
        needsDropRollableImports: Boolean,
    ) {
        appendLine("package $packageName")
        appendLine()
        appendLine("import dtx.rs.RSDropTable")
        appendLine("import dtx.rs.npcs")
        appendLine("import dtx.rs.areas")
        appendLine("import org.rsmod.api.droptable.rsPlayerGuaranteedTable")
        appendLine("import org.rsmod.api.droptable.rsPlayerTertiaryTable")
        appendLine("import org.rsmod.api.droptable.rsPlayerWeightedTable")
        if (needsInlineHerbTables) {
            appendLine("import dtx.rs.rsWeightedTable")
        }
        if (needsPreRoll) {
            appendLine("import org.rsmod.api.droptable.rsPlayerPrerollTable")
        }
        if (needsPreRoll || needsDropRollableImports) {
            appendLine("import org.rsmod.api.droptable.dropRollable")
        }
        if (needsSharedDropTables) {
            appendLine("import org.rsmod.content.drops.tables.shared.SharedDropTables")
        }
        if (needsInlineHerbTables) {
            appendLine("import org.rsmod.content.drops.tables.shared.doubleRollHerbDropTable")
            appendLine("import org.rsmod.content.drops.tables.shared.herbDropTable")
            appendLine("import org.rsmod.content.drops.tables.shared.tripleRollHerbDropTable")
        }
        appendLine("import org.rsmod.api.droptable.DropRollItem")
        if (needsBodyTypeConstants) {
            appendLine("import org.rsmod.api.config.constants")
        }
        if (needsLootingBagChecks) {
            appendLine("import org.rsmod.content.drops.shouldDropLootingBag")
        }
        if (needsBrimstoneKeyChecks) {
            appendLine("import org.rsmod.content.drops.shouldDropBrimstoneKey")
        }
        if (needsBrimstoneKeyRoll) {
            appendLine("import org.rsmod.content.drops.brimstoneKeyRoll")
        }
        if (needsQuestChecks) {
            appendLine("import org.rsmod.content.drops.hasCompletedQuest")
            appendLine("import org.rsmod.content.drops.isOnQuest")
        }
        if (needsNothingImports) {
            if (needsNothingPadding) {
                appendLine("import org.rsmod.api.droptable.nothing")
            }
            if (needsNothingIfRing) {
                appendLine("import org.rsmod.api.droptable.ringNothing")
            }
        }
        appendLine("import org.rsmod.api.droptable.RegisterDropTable")
        appendLine("import org.rsmod.game.entity.Player")
        appendLine()
    }

    private fun StringBuilder.appendTable(spec: GeneratedDropTableSpec) {
        appendLine("@field:RegisterDropTable")
        appendLine("@JvmField")
        appendLine("public val ${spec.tableVarName}: RSDropTable<Player, DropRollItem> = RSDropTable(")
        appendLine("    tableIdentifier = ${spec.tableIdentifier.kotlinString()},")
        appendNpcList(spec.npcRscmKeys)
        if (spec.areaRscmKeys.isNotEmpty()) {
            appendAreaList(spec.areaRscmKeys)
        }

        if (spec.guaranteed.isNotEmpty()) {
            appendLine("    guaranteed = rsPlayerGuaranteedTable {")
            for (entry in spec.guaranteed) {
                appendDropLine(entry, indent = "        ")
            }
            appendLine("    },")
        }

        if (spec.preRoll.isNotEmpty() || spec.preRollSeparateRolls.isNotEmpty()) {
            appendPreRollTable(spec.preRoll, spec.preRollSeparateRolls)
        }

        if (spec.main.isNotEmpty() || spec.subtableAccesses.isNotEmpty() || spec.separateRolls.isNotEmpty()) {
            appendMainTable(
                spec.tableIdentifier,
                spec.main,
                spec.mainMaxRoll,
                spec.subtableAccesses,
                spec.separateRolls,
            )
        }

        if (spec.tertiary.isNotEmpty()) {
            appendLine("    tertiaries = rsPlayerTertiaryTable {")
            for (entry in spec.tertiary) {
                appendTertiaryLine(entry, indent = "        ")
            }
            appendLine("    },")
        }

        appendLine(")")
    }

    private fun StringBuilder.appendUnmappedItems(unmappedItems: List<String>, label: String?) {
        if (unmappedItems.isEmpty()) {
            return
        }
        appendLine()
        val heading =
            if (label.isNullOrBlank()) {
                "// Unmapped wiki items:"
            } else {
                "// Unmapped wiki items ($label):"
            }
        appendLine(heading)
        for (item in unmappedItems.distinct().sorted()) {
            appendLine("//   - $item")
        }
    }

    private fun StringBuilder.appendUnknownDropRates(unknownDropRates: List<UnknownDropRateEntry>) {
        if (unknownDropRates.isEmpty()) {
            return
        }
        appendLine()
        appendLine("// Unknown wiki drop rates (text rarity — need data collection):")
        for (entry in unknownDropRates.distinctBy { it.itemName to it.rarity }) {
            val section = entry.section.lowercase()
            appendLine("//   - ${entry.itemName} [$section/${entry.rarity}]")
        }
    }

    suspend fun resolveItemDrop(
        drop: ParsedWikiDrop,
        itemLookup: ItemWikiLookup,
        objLookup: ObjRscmLookup,
    ): ResolvedDropEntry? {
        val obj = objLookup.resolveWikiItem(itemLookup, drop.name, noted = drop.isNoted) ?: return null
        val bonusDrops = resolveBonusDrops(drop.wikiNotes.companionDrops, itemLookup, objLookup, drop.name, drop.quantity)

        return when (drop.section) {
            WikiDropSection.Guaranteed ->
                resolvedEntry(drop, obj, bonusDrops)

            WikiDropSection.Main ->
                parseMainRarity(drop.rarity)?.let { (weight, denominator) ->
                    resolvedEntry(drop, obj, bonusDrops, weight = weight, rollDenominator = denominator)
                }

            WikiDropSection.Tertiary ->
                if (isBrimstoneRarityTemplate(drop.rarity)) {
                    resolvedEntry(
                        drop = drop,
                        obj = obj,
                        bonusDrops = bonusDrops,
                        brimstoneCombatRoll = true,
                        brimstoneKonarBonus = hasBrimstoneKonarBonus(drop.rarity),
                    )
                } else {
                    parseTertiaryRarity(drop.rarity)?.let { (weight, outOf) ->
                        resolvedEntry(drop, obj, bonusDrops, weight = weight, outOf = outOf)
                    }
                }
        }
    }

    private fun resolvedEntry(
        drop: ParsedWikiDrop,
        obj: String,
        bonusDrops: List<ResolvedBonusDrop>,
        weight: Int? = null,
        rollDenominator: Int? = null,
        outOf: Int? = null,
        brimstoneCombatRoll: Boolean = false,
        brimstoneKonarBonus: Boolean = false,
    ): ResolvedDropEntry =
        ResolvedDropEntry(
            obj = obj,
            quantity = drop.quantity,
            weight = weight,
            rollDenominator = rollDenominator,
            outOf = outOf,
            wikiName = drop.name,
            wikiRarity = drop.rarity,
            subsection = drop.subsection,
            wikiNotes = drop.wikiNotes,
            bonusDrops = bonusDrops,
            brimstoneCombatRoll = brimstoneCombatRoll,
            brimstoneKonarBonus = brimstoneKonarBonus,
        )

    private suspend fun resolveBonusDrops(
        specs: List<WikiCompanionDropSpec>,
        itemLookup: ItemWikiLookup,
        objLookup: ObjRscmLookup,
        primaryDropName: String,
        primaryQuantity: String,
    ): List<ResolvedBonusDrop> {
        if (specs.isEmpty()) {
            return emptyList()
        }

        val resolved = mutableListOf<ResolvedBonusDrop>()
        for (spec in specs) {
            if (spec.genderSplit) {
                val legsName =
                    spec.wikiNames.firstOrNull { name ->
                        name.contains("platelegs", ignoreCase = true) ||
                            (name.contains("legs", ignoreCase = true) && !name.contains("skirt", ignoreCase = true))
                    }
                val skirtName = spec.wikiNames.firstOrNull { name -> name.contains("plateskirt", ignoreCase = true) || name.contains("skirt", ignoreCase = true) }
                if (legsName != null) {
                    objLookup.resolveWikiItem(itemLookup, legsName)?.let { obj ->
                        resolved +=
                            ResolvedBonusDrop(
                                obj = obj,
                                count = spec.count,
                                bodyTypeRequired = BODYTYPE_A,
                            )
                    }
                }
                if (skirtName != null) {
                    objLookup.resolveWikiItem(itemLookup, skirtName)?.let { obj ->
                        resolved +=
                            ResolvedBonusDrop(
                                obj = obj,
                                count = spec.count,
                                bodyTypeRequired = BODYTYPE_B,
                            )
                    }
                }
                continue
            }

            for (name in spec.wikiNames) {
                WikiCompanionDropParser
                    .resolveObj(itemLookup, objLookup, name, primaryDropName, spec.droppedTogether)
                    ?.let { obj ->
                        val count =
                            spec.count.takeIf { it > 1 }
                                ?: primaryQuantity.toIntOrNull()
                                ?: spec.count
                        resolved += ResolvedBonusDrop(obj = obj, count = count)
                    }
            }
        }
        return resolved
    }

    private const val BODYTYPE_A = 0
    private const val BODYTYPE_B = 1

    private fun StringBuilder.appendPreRollTable(
        preRoll: List<ResolvedDropEntry>,
        separateRolls: List<SeparateRollSpec>,
    ) {
        appendLine("    preRoll = rsPlayerPrerollTable {")
        for (entry in preRoll) {
            appendPreRollLine(entry, indent = "        ")
        }
        for (roll in separateRolls) {
            appendInlinePreRollSeparate(roll, indent = "        ")
        }
        appendLine("    },")
    }

    private fun StringBuilder.appendPreRollLine(entry: ResolvedDropEntry, indent: String) {
        val weight = entry.weight ?: return
        val outOf = entry.outOf ?: entry.rollDenominator ?: return
        appendRateFirstChanceLine(entry, weight, outOf, indent, fallbackRollKeyword = "rolls")
    }

    private fun ResolvedDropEntry.canUseItemChainSyntax(): Boolean = bonusDrops.isEmpty()

    private fun formatDropCount(count: Int, countMax: Int?, parenthesizeRange: Boolean = false): String =
        if (countMax != null && countMax > count) {
            val range = "$count..$countMax"
            if (parenthesizeRange) "($range)" else range
        } else {
            count.toString()
        }

    private fun ResolvedDropEntry.dropSpecCount(): Int {
        val range = quantityRange()
        return range?.first ?: quantity.toIntOrNull() ?: 1
    }

    private fun ResolvedDropEntry.dropSpecCountMax(): Int? {
        val range = quantityRange()
        val count = dropSpecCount()
        return range?.second?.takeIf { it > count }
    }

    private fun StringBuilder.appendInlinePreRollSeparate(roll: SeparateRollSpec, indent: String) {
        if (roll.entries.size == 1) {
            val entry = roll.entries.first()
            appendRateFirstChanceLine(
                entry,
                roll.accessNumerator,
                roll.accessDenominator,
                indent,
                fallbackRollKeyword = "rolls",
            )
            return
        }

        appendLine("$indent${roll.accessNumerator} outOf ${roll.accessDenominator} rolls rsPlayerWeightedTable {")
        for (entry in roll.entries) {
            appendWeightedLine(entry, indent = "$indent    ")
        }
        appendLine("$indent}")
    }

    private fun StringBuilder.appendMainTable(
        tableIdentifier: String,
        main: List<ResolvedDropEntry>,
        mainMaxRoll: Int?,
        subtableAccesses: List<ResolvedSubtableAccess>,
        separateRolls: List<SeparateRollSpec>,
    ) {
        val poolTotal = mainMaxRoll
        if (poolTotal != null) {
            appendLine("    mainTable = rsPlayerWeightedTable(total = $poolTotal) {")
        } else {
            appendLine("    mainTable = rsPlayerWeightedTable {")
        }
        appendLine("        name(${tableIdentifier.kotlinString()})")

        for (entry in main) {
            appendWeightedLine(entry, indent = "        ")
        }
        for (roll in separateRolls) {
            appendInlineSeparateRoll(roll, indent = "        ")
        }

        if (subtableAccesses.isNotEmpty()) {
            appendLine()
            for (access in subtableAccesses) {
                appendSubtableWeight(access)
            }
        }

        val hasExplicitNothing = main.any { it.isNothing }
        val nothingWeight = computeNothingWeight(main, mainMaxRoll, subtableAccesses)
        if (nothingWeight > 0) {
            if (hasExplicitNothing) {
                appendLine("        // Pool padding (F2P drops removed / subtable access missing from wiki parse)")
            }
            appendLine("        $nothingWeight weight nothing()")
        }

        appendLine("    },")
    }

    private fun StringBuilder.appendSubtableWeight(access: ResolvedSubtableAccess) {
        if (access.needsHardcodedSharedTable) {
            appendLine(
                "        // Needs hardcoded shared table: ${access.wikiLabel} " +
                    "(${access.numerator}/${access.denominator})",
            )
        }

        val variants = access.herbRollVariants
        if (variants.isNullOrEmpty()) {
            appendLine("        ${access.numerator} weight ${access.tableRef}")
            return
        }

        appendInlineHerbRollTable(access.numerator, variants)
    }

    private fun StringBuilder.appendInlineHerbRollTable(
        mainWeight: Int,
        variants: List<HerbRollVariant>,
    ) {
        val poolTotal = inlineHerbPoolTotal(variants)
        appendLine("        $mainWeight weight rsWeightedTable(total = $poolTotal) {")
        if (HerbDropTableParser.isStandardMultiRoll(variants)) {
            appendLine("            name(\"Multi-roll herb drop table\")")
        }
        for (variant in variants) {
            val tableRef =
                when (variant.herbCount) {
                    1 -> "herbDropTable"
                    2 -> "doubleRollHerbDropTable"
                    3 -> "tripleRollHerbDropTable"
                    else -> error("unsupported herbCount ${variant.herbCount}")
                }
            appendLine("            ${variant.numerator} weight $tableRef")
        }
        appendLine("        }")
    }

    internal fun inlineHerbPoolTotal(variants: List<HerbRollVariant>): Int {
        require(variants.isNotEmpty())
        val variantSum = variants.sumOf { it.numerator }
        val sharedDenom = variants.first().denominator
        if (variants.all { it.denominator == sharedDenom } && variantSum < sharedDenom) {
            return variantSum
        }
        return sharedDenom
    }

    private fun computeNothingWeight(
        main: List<ResolvedDropEntry>,
        mainMaxRoll: Int?,
        subtableAccesses: List<ResolvedSubtableAccess>,
    ): Int {
        val maxRoll = mainMaxRoll ?: return 0
        val used =
            main.sumOf { it.weight ?: 0 } +
                subtableAccesses.sumOf { it.numerator }
        return (maxRoll - used).coerceAtLeast(0)
    }

    private fun mainMaxRollRequiresNothing(spec: GeneratedDropTableSpec): Boolean =
        computeNothingWeight(spec.main, spec.mainMaxRoll, spec.subtableAccesses) > 0

    fun parseMainWeight(rarity: String): Int? = parseMainRarity(rarity)?.first

    fun parseMainRarity(rarity: String): Pair<Int, Int>? {
        if (isUnknownWikiDropRate(rarity) || rarity.equals("Always", ignoreCase = true)) {
            return null
        }
        parseDecimalFraction(rarity.trim())?.let { return it }
        val weight = rarity.trim().toIntOrNull() ?: return null
        return weight to weight
    }

    fun parseTertiaryRarity(rarity: String): Pair<Int, Int>? {
        if (rarity.equals("Always", ignoreCase = true)) {
            return 1 to 1
        }
        parseBrimstoneRarity(rarity)?.let { return it }
        if (isUnknownWikiDropRate(rarity)) {
            return null
        }
        return parseDecimalFraction(rarity.trim())
    }

    private fun parseDecimalFraction(trimmed: String): Pair<Int, Int>? {
        val fraction = DECIMAL_FRACTION_RARITY.find(trimmed) ?: return null
        val numerator =
            fraction.groupValues[1]
                .toBigDecimal()
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .toInt()
        val denominator =
            fraction.groupValues[2]
                .toBigDecimal()
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .toInt()
        return numerator to denominator
    }

    fun isUnknownWikiDropRate(rarity: String): Boolean = !hasKnownWikiDropRate(rarity)

    fun hasKnownWikiDropRate(rarity: String): Boolean {
        val trimmed = rarity.trim()
        if (trimmed.isBlank()) {
            return false
        }
        if (trimmed.equals("Always", ignoreCase = true)) {
            return true
        }
        if (parseBrimstoneRarity(trimmed) != null) {
            return true
        }
        if (containsUnresolvedWikiRateMarkup(trimmed)) {
            return false
        }
        if (DECIMAL_FRACTION_RARITY.containsMatchIn(trimmed)) {
            return true
        }
        if (trimmed.toIntOrNull() != null) {
            return true
        }
        return false
    }

    private val DECIMAL_FRACTION_RARITY = Regex("""(\d+(?:\.\d+)?)\s*/\s*(\d+(?:\.\d+)?)""")

    private fun containsUnresolvedWikiRateMarkup(rarity: String): Boolean {
        if (BRIMSTONE_RARITY_TEMPLATE.containsMatchIn(rarity)) {
            return false
        }
        return rarity.contains("{{") ||
            rarity.contains("}}") ||
            rarity.contains("#expr") ||
            rarity.contains("#var")
    }

    fun parseBrimstoneRarity(rarity: String): Pair<Int, Int>? {
        if (!isBrimstoneRarityTemplate(rarity)) {
            return null
        }
        val match = BRIMSTONE_RARITY_TEMPLATE.find(rarity.trim()) ?: return null
        val combatLevel = match.groupValues[1].toIntOrNull() ?: return null
        val bonus = hasBrimstoneKonarBonus(rarity)
        val outOf = brimstoneRarityDenominator(combatLevel, bonus)
        return 1 to outOf
    }

    fun isBrimstoneRarityTemplate(rarity: String): Boolean = BRIMSTONE_RARITY_TEMPLATE.containsMatchIn(rarity.trim())

    fun hasBrimstoneKonarBonus(rarity: String): Boolean = BRIMSTONE_BONUS_FLAG.containsMatchIn(rarity)

    private val BRIMSTONE_RARITY_TEMPLATE =
        Regex("""(?i)\{\{\s*Brimstone\s+rarity\s*\|\s*(\d+)""")

    private val BRIMSTONE_BONUS_FLAG =
        Regex("""(?i)(?:\|\s*bonus\s*=\s*yes|\|\s*yes\s*\}\})""")

    private fun StringBuilder.appendNpcList(npcRscmKeys: List<String>) {
        val npcList = npcRscmKeys.distinct().sorted().joinToString(", ") { it.kotlinString() }
        appendLine("    npcs = npcs($npcList),")
    }

    private fun StringBuilder.appendAreaList(areaRscmKeys: List<String>) {
        val areaList = areaRscmKeys.distinct().sorted().joinToString(", ") { it.kotlinString() }
        appendLine("    areas = areas($areaList),")
    }

    private fun StringBuilder.appendDropLine(entry: ResolvedDropEntry, indent: String) {
        if (entry.canUseItemChainSyntax()) {
            appendObjectDropLine(entry, indent, entry.obj.kotlinString())
            return
        }
        appendDropRollLine("$indent add(", entry, indent, suffix = ")", useRollable = true)
    }

    private fun StringBuilder.appendInlineSeparateRoll(roll: SeparateRollSpec, indent: String) {
        if (roll.entries.size == 1) {
            val entry = roll.entries.first()
            if (entry.isNothing) {
                appendLine("$indent${roll.accessNumerator} outOf ${roll.accessDenominator} separate ringNothing()")
                return
            }
            appendResolvedDropLine(
                entry = entry,
                indent = indent,
                header = "${roll.accessNumerator} outOf ${roll.accessDenominator} separate ${entry.obj.kotlinString()}",
                fallbackPrefix = "$indent${roll.accessNumerator} outOf ${roll.accessDenominator} separate ",
            )
            return
        }

        appendLine("$indent${roll.accessNumerator} outOf ${roll.accessDenominator} separate rsPlayerWeightedTable {")
        for (entry in roll.entries) {
            appendWeightedLine(entry, indent = "$indent    ")
        }
        appendLine("$indent}")
    }

    private fun StringBuilder.appendWeightedLine(entry: ResolvedDropEntry, indent: String) {
        val weight = entry.weight ?: return
        if (entry.isNothing) {
            appendLine("$indent$weight weight ringNothing()")
            return
        }
        appendResolvedDropLine(
            entry = entry,
            indent = indent,
            header = "$weight weight ${entry.obj.kotlinString()}",
            fallbackPrefix = "$indent$weight weight ",
        )
    }

    private fun StringBuilder.appendResolvedDropLine(
        entry: ResolvedDropEntry,
        indent: String,
        header: String,
        fallbackPrefix: String,
        lineSuffix: String = "",
        fallbackSuffix: String = "",
        useRollable: Boolean = true,
    ) {
        if (entry.canUseItemChainSyntax()) {
            appendObjectDropLine(entry, indent, header, lineSuffix)
            return
        }
        appendDropRollLine(
            fallbackPrefix,
            entry,
            indent,
            suffix = fallbackSuffix,
            useRollable = useRollable,
        )
    }

    private fun StringBuilder.appendObjectDropLine(
        entry: ResolvedDropEntry,
        indent: String,
        header: String,
        suffix: String = "",
    ) {
        val count = entry.dropSpecCount()
        val countMax = entry.dropSpecCountMax()
        val conditionBody = entry.conditionBody()
        val killConditionBody = entry.killConditionBody()
        val hasTransform = entry.wikiNotes.hasTransformItem
        val hasManualCondition = entry.wikiNotes.hasCondition
        val hasModifier = conditionBody != null || killConditionBody != null || hasTransform || hasManualCondition
        val countArg = formatDropCount(count, countMax, parenthesizeRange = hasModifier)
        val innerIndent = "$indent    "

        if (hasTransform) {
            appendLine("$indent$header count $countArg transformObj { player ->")
            for (note in entry.wikiNotes.transformItem) {
                appendLine("$innerIndent// Drops Need Manual (item): ${note.kotlinComment()}")
            }
            appendLine("$innerIndent null")
            appendLine("$indent}$suffix")
            return
        }

        if (hasManualCondition) {
            appendLine("$indent$header count $countArg condition { player ->")
            for (note in entry.wikiNotes.condition) {
                appendLine("$innerIndent// Drops Need Manual: ${note.kotlinComment()}")
            }
            appendLine("$innerIndent true")
            appendLine("$indent}$suffix")
            return
        }

        if (conditionBody != null) {
            appendLine("$indent$header count $countArg condition {")
            appendLine("$innerIndent$conditionBody")
            appendLine("$indent}$suffix")
            return
        }

        if (killConditionBody != null) {
            appendLine("$indent$header count $countArg killCondition {")
            appendLine("$innerIndent$killConditionBody")
            appendLine("$indent}$suffix")
            return
        }

        appendLine("$indent$header count $countArg$suffix")
    }

    private fun StringBuilder.appendTertiaryLine(entry: ResolvedDropEntry, indent: String) {
        if (entry.brimstoneCombatRoll) {
            for (note in entry.wikiNotes.transformRate) {
                appendLine("$indent// Drops Need Manual (rate): ${note.kotlinComment()}")
            }
            appendLine("$indent${entry.brimstoneKeyRollLine()}")
            return
        }
        val weight = entry.weight ?: return
        val outOf = entry.outOf ?: return
        for (note in entry.wikiNotes.transformRate) {
            appendLine("$indent// Drops Need Manual (rate): ${note.kotlinComment()}")
        }
        appendRateFirstChanceLine(entry, weight, outOf, indent)
    }

    private fun StringBuilder.appendRateFirstChanceLine(
        entry: ResolvedDropEntry,
        numerator: Int,
        outOf: Int,
        indent: String,
        fallbackRollKeyword: String = "chance",
    ) {
        appendResolvedDropLine(
            entry = entry,
            indent = indent,
            header = "$numerator outOf $outOf weight ${entry.obj.kotlinString()}",
            fallbackPrefix = "$indent$numerator outOf $outOf $fallbackRollKeyword ",
        )
    }

    private fun ResolvedDropEntry.conditionBody(): String? =
        when {
            requiresLootingBagCondition() ->
                "player -> player.shouldDropLootingBag()"
            requiresQuestCondition() -> {
                val questRequirement = wikiNotes.questRequirements.first()
                "player -> ${questRequirement.conditionExpression()}"
            }
            else -> null
        }

    private fun ResolvedDropEntry.killConditionBody(): String? =
        if (requiresBrimstoneKeyCondition()) {
            "player, npc, areaChecker -> player.shouldDropBrimstoneKey(npc, areaChecker)"
        } else {
            null
        }

    private fun StringBuilder.appendDropRollLine(
        prefix: String,
        entry: ResolvedDropEntry,
        indent: String,
        suffix: String = "",
        useRollable: Boolean = false,
    ) {
        val expression = if (useRollable) entry.rollableExpression(indent) else entry.dropRollExpression(indent)
        appendLine("$prefix$expression$suffix")
    }

    private fun ResolvedDropEntry.rollableExpression(indent: String = ""): String {
        val item = dropRollExpression(indent)
        return if (bonusDrops.isNotEmpty()) {
            "dropRollable($item)"
        } else {
            item
        }
    }

    private fun ResolvedDropEntry.dropRollExpression(indent: String = ""): String {
        val count = dropSpecCount()
        val countMax = dropSpecCountMax()
        val needsLootingBagCondition = requiresLootingBagCondition()
        val needsBrimstoneKeyCondition = requiresBrimstoneKeyCondition()
        val questRequirement = wikiNotes.questRequirements.firstOrNull()
        val needsQuestCondition = questRequirement != null

        val innerIndent = indent + "    "
        return buildString {
            append("DropRollItem(${obj.kotlinString()}, ${formatDropCount(count, countMax)}")
            if (needsLootingBagCondition) {
                append(", condition = { player -> player.shouldDropLootingBag() }")
            } else if (needsQuestCondition) {
                append(", condition = { player -> ${questRequirement!!.conditionExpression()} }")
            } else if (wikiNotes.hasCondition) {
                append(", condition = { player ->\n")
                for (note in wikiNotes.condition) {
                    append("$innerIndent// Drops Need Manual: ${note.kotlinComment()}\n")
                }
                append("$innerIndent true\n")
                append("$indent}")
            }
            if (needsBrimstoneKeyCondition) {
                append(
                    ", killCondition = { player, npc, areaChecker -> " +
                        "player.shouldDropBrimstoneKey(npc, areaChecker) }",
                )
            }
            if (wikiNotes.hasTransformItem) {
                append(", transformObj = { player ->\n")
                for (note in wikiNotes.transformItem) {
                    append("$innerIndent// Drops Need Manual (item): ${note.kotlinComment()}\n")
                }
                append("$innerIndent null\n")
                append("$indent}")
            }
            if (bonusDrops.isNotEmpty()) {
                append(", bonusDrops = listOf(\n")
                for (bonus in bonusDrops) {
                    append(bonus.bonusDropExpression(innerIndent))
                    append(",\n")
                }
                append("$indent))")
            } else {
                append(")")
            }
        }
    }

    private fun ResolvedDropEntry.requiresLootingBagCondition(): Boolean =
        obj == LOOTING_BAG_OBJ || wikiNotes.lootingBagWilderness

    private fun ResolvedDropEntry.requiresBrimstoneKeyCondition(): Boolean =
        !brimstoneCombatRoll && (obj == BRIMSTONE_KEY_OBJ || wikiNotes.brimstoneKonarTask)

    private fun ResolvedDropEntry.brimstoneKeyRollLine(): String =
        if (brimstoneKonarBonus) {
            "onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }"
        } else {
            "onBuilder { brimstoneKeyRoll() }"
        }

    private fun ResolvedDropEntry.requiresQuestCondition(): Boolean =
        wikiNotes.hasQuestRequirement

    private fun WikiQuestDropRequirement.conditionExpression(): String {
        val key = questKey.kotlinString()
        return when (mode) {
            WikiQuestDropMode.RequiresCompleted -> "player.hasCompletedQuest($key)"
            WikiQuestDropMode.RequiresDuring -> "player.isOnQuest($key)"
            WikiQuestDropMode.RequiresNotCompleted -> "!player.hasCompletedQuest($key)"
        }
    }

    private fun ResolvedBonusDrop.bonusDropExpression(indent: String): String =
        buildString {
            append("${indent}DropRollItem(${obj.kotlinString()}, ${formatDropCount(count, countMax)}")
            when (bodyTypeRequired) {
                BODYTYPE_A ->
                    append(", condition = { player -> player.appearance.bodyType == constants.bodytype_a }")
                BODYTYPE_B ->
                    append(", condition = { player -> player.appearance.bodyType == constants.bodytype_b }")
            }
            append(")")
        }

    private fun ResolvedDropEntry.quantityRange(): Pair<Int, Int>? {
        val match = Regex("""(\d+)\s*-\s*(\d+)""").find(quantity) ?: return null
        val min = match.groupValues[1].toIntOrNull() ?: return null
        val max = match.groupValues[2].toIntOrNull() ?: return null
        return min to max
    }

    private fun String.kotlinString(): String = "\"" + replace("\"", "\\\"") + "\""

    private fun String.kotlinComment(): String = replace("*/", "* /")
}
