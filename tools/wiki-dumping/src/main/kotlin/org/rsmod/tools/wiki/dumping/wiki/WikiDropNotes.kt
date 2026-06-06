package org.rsmod.tools.wiki.dumping.wiki

import org.rsmod.tools.wiki.dumping.ItemWikiLookup
import org.rsmod.tools.wiki.dumping.ObjRscmLookup

data class WikiDropNotes(
    val condition: List<String> = emptyList(),
    val transformItem: List<String> = emptyList(),
    val transformRate: List<String> = emptyList(),
    val companionDrops: List<WikiCompanionDropSpec> = emptyList(),
    val isCompanionOnly: Boolean = false,
    val lootingBagWilderness: Boolean = false,
    val brimstoneKonarTask: Boolean = false,
    val questRequirements: List<WikiQuestDropRequirement> = emptyList(),
) {
    val hasQuestRequirement: Boolean get() = questRequirements.isNotEmpty()
    val hasCondition: Boolean get() = condition.isNotEmpty()
    val hasTransformItem: Boolean get() = transformItem.isNotEmpty()
    val hasTransformRate: Boolean get() = transformRate.isNotEmpty()
    val hasCompanionDrops: Boolean get() = companionDrops.isNotEmpty()

    fun merge(other: WikiDropNotes): WikiDropNotes =
        WikiDropNotes(
            condition = (condition + other.condition).distinct(),
            transformItem = (transformItem + other.transformItem).distinct(),
            transformRate = (transformRate + other.transformRate).distinct(),
            companionDrops = (companionDrops + other.companionDrops).distinct(),
            isCompanionOnly = isCompanionOnly || other.isCompanionOnly,
            lootingBagWilderness = lootingBagWilderness || other.lootingBagWilderness,
            brimstoneKonarTask = brimstoneKonarTask || other.brimstoneKonarTask,
            questRequirements = (questRequirements + other.questRequirements).distinct(),
        )
}

object WikiDropNoteClassifier {
    var includeF2pWorldNotes: Boolean = false

    private val WIKI_LINK = Regex("""\[\[([^|\]]+)""")
    private val f2pNotePattern = Regex("""free[\s-]*to[\s-]*play""", RegexOption.IGNORE_CASE)
    private val f2pOnlyDropPattern =
        Regex("""only dropped in(?:\s*\[\[)?\s*free[\s-]*to[\s-]*play""", RegexOption.IGNORE_CASE)
    private val transformItemPattern =
        Regex("""scroll\s*box|x\s*marks\s*the\s*spot|replaced\s*by""", RegexOption.IGNORE_CASE)
    private val transformRatePattern =
        Regex("""drop\s*rate|increases\s*to|decreases\s*to|\d+\s*/\s*\d+""", RegexOption.IGNORE_CASE)
    private val lootingBagWildernessPattern =
        Regex("""looting\s*bags?\s*are\s*only\s*dropped.*wilderness""", RegexOption.IGNORE_CASE)
    private val brimstoneKonarTaskPattern =
        Regex(
            """brimstone\s*keys?\s*are\s*only\s*dropped.*(?:konar\s*quo\s*maten|slayer\s*task\s*given\s*by\s*konar)""",
            RegexOption.IGNORE_CASE,
        )
    private val f2pNameNotePattern = Regex("""\(f\)""", RegexOption.IGNORE_CASE)
    private val f2pRefNamePattern = Regex("""name\s*=\s*['"]f2p['"]""", RegexOption.IGNORE_CASE)

    fun classify(note: String, dropName: String = ""): WikiDropNotes {
        val cleaned = note.trim()
        if (cleaned.isBlank() || shouldIgnore(cleaned)) {
            return WikiDropNotes()
        }

        if (lootingBagWildernessPattern.containsMatchIn(cleaned)) {
            return WikiDropNotes(lootingBagWilderness = true)
        }
        if (dropName.contains("looting bag", ignoreCase = true) &&
            Regex("""wilderness""", RegexOption.IGNORE_CASE).containsMatchIn(cleaned)
        ) {
            return WikiDropNotes(lootingBagWilderness = true)
        }
        if (brimstoneKonarTaskPattern.containsMatchIn(cleaned)) {
            return WikiDropNotes(brimstoneKonarTask = true)
        }

        WikiQuestDropParser.parse(cleaned)?.let { requirement ->
            return WikiDropNotes(questRequirements = listOf(requirement))
        }

        if (dropName.isNotBlank()) {
            when (val companion = WikiCompanionDropParser.parse(cleaned, dropName)) {
                is WikiCompanionParseResult.Primary ->
                    return WikiDropNotes(companionDrops = companion.specs)
                WikiCompanionParseResult.CompanionOnlyRow ->
                    return WikiDropNotes(isCompanionOnly = true)
                null -> Unit
            }
        }

        return when {
            transformItemPattern.containsMatchIn(cleaned) ->
                WikiDropNotes(transformItem = listOf(cleaned))
            transformRatePattern.containsMatchIn(cleaned) ->
                WikiDropNotes(transformRate = listOf(cleaned))
            else -> WikiDropNotes(condition = listOf(cleaned))
        }
    }

    fun classifyAll(notes: List<String>, dropName: String): WikiDropNotes =
        notes.filterNot(::shouldIgnore).fold(WikiDropNotes()) { acc, note -> acc.merge(classify(note, dropName)) }

    fun namesMatchForCompanion(dropName: String, candidate: String): Boolean {
        val dropNorm = normalizeCompanionItemName(dropName)
        val candNorm = normalizeCompanionItemName(candidate)
        if (dropNorm.isBlank() || candNorm.isBlank()) {
            return false
        }
        if (dropNorm == candNorm || dropNorm.contains(candNorm) || candNorm.contains(dropNorm)) {
            return true
        }
        val dropTokens = dropNorm.split(" ").filter { it.length > 2 }
        val candTokens = candNorm.split(" ").filter { it.length > 2 }
        if (dropTokens.isEmpty() || candTokens.isEmpty()) {
            return false
        }
        val (shorter, longer) =
            if (dropTokens.size <= candTokens.size) {
                dropTokens to candTokens.joinToString(" ")
            } else {
                candTokens to dropTokens.joinToString(" ")
            }
        return shorter.all { token -> longer.contains(token) }
    }

    fun shouldSkipF2pOnlyDrop(params: Map<String, String>, attachedNotes: List<String>): Boolean {
        if (includeF2pWorldNotes) {
            return false
        }
        if (params["f2p"].equals("yes", ignoreCase = true) || params["leaguef2p"].equals("yes", ignoreCase = true)) {
            return true
        }
        val nameNotes = params["namenotes"].orEmpty()
        val rarityNotes = params["raritynotes"].orEmpty()
        if (f2pNameNotePattern.containsMatchIn(nameNotes)) {
            return true
        }
        if (f2pRefNamePattern.containsMatchIn("$nameNotes $rarityNotes")) {
            return true
        }
        return attachedNotes.any { f2pOnlyDropPattern.containsMatchIn(it) }
    }

    fun relevantToDrop(dropName: String, note: String): Boolean {
        val name = dropName.lowercase()
        val text = note.lowercase()

        if (text.contains("looting bag") || (text.contains("wilderness") && text.contains("bag"))) {
            return name.contains("looting bag")
        }
        if (text.contains("key") && text.contains("medium")) {
            return name.contains("key")
        }
        if (transformItemPattern.containsMatchIn(text) || (text.contains("clue") && text.contains("scroll"))) {
            return name.contains("clue scroll")
        }
        if (transformRatePattern.containsMatchIn(text) && text.contains("easy")) {
            return name.contains("clue scroll") && name.contains("easy")
        }
        if (transformRatePattern.containsMatchIn(text) && text.contains("clue")) {
            return name.contains("clue scroll")
        }
        if (text.contains("free-to-play") || text.contains("free to play")) {
            return false
        }
        if (text.contains("increases to") || text.contains("decreases to")) {
            return name.equals("Nothing", ignoreCase = true)
        }
        return noteMentionsItem(dropName, note)
    }

    fun noteMentionsItem(dropName: String, note: String): Boolean {
        val normalizedDrop = normalizeItemName(dropName)
        if (normalizedDrop.isBlank()) {
            return false
        }
        for (match in WIKI_LINK.findAll(note)) {
            if (namesMatch(normalizedDrop, normalizeItemName(match.groupValues[1]))) {
                return true
            }
        }
        return normalizeItemName(note).contains(normalizedDrop)
    }

    private fun shouldIgnore(note: String): Boolean =
        !includeF2pWorldNotes && f2pNotePattern.containsMatchIn(note)

    private fun normalizeCompanionItemName(name: String): String =
        normalizeItemName(name)
            .replace(Regex("""\(\d+\)"""), "")
            .replace(Regex("""^\d+\s+dose\s+"""), "")
            .replace(Regex("""\s+\d+$"""), "")
            .split(" ")
            .map { it.removeSuffix("s") }
            .joinToString(" ")
            .trim()

    private fun namesMatch(drop: String, candidate: String): Boolean =
        drop == candidate || drop.contains(candidate) || candidate.contains(drop)

    private fun normalizeItemName(name: String): String =
        name.lowercase().replace(Regex("""[^a-z0-9]+"""), " ").trim()
}

// ---------------------------------------------------------------------------
// Companion drops ("together with …", "dropped together")
// ---------------------------------------------------------------------------

data class WikiCompanionDropSpec(
    val wikiNames: List<String>,
    val genderSplit: Boolean = false,
    val count: Int = 1,
    val droppedTogether: Boolean = false,
)

sealed class WikiCompanionParseResult {
    data class Primary(val specs: List<WikiCompanionDropSpec>) : WikiCompanionParseResult()
    data object CompanionOnlyRow : WikiCompanionParseResult()
}

object WikiCompanionDropParser {
    private val togetherWithPattern =
        Regex("""receive\s+(.+?)\s+together with(?:\s+an extra drop of)?\s+(.+)""", RegexOption.IGNORE_CASE)
    private val droppedTogetherPattern =
        Regex("""(?i)(.+?)\s+and\s+(.+?)\s+are\s+(?:always\s+)?dropped\s+together""")
    private val topAndBottomTogetherPattern =
        Regex("""(?i)(.+?)\s+top\s+and\s+bottom\s+are\s+(?:always\s+)?dropped\s+together""")
    private val countPrefixPattern = Regex("""^(one|two|three|four|five|\d+)\s+""", RegexOption.IGNORE_CASE)
    private val topSuffix = Regex("""top$""", RegexOption.IGNORE_CASE)
    private val bottomSuffix = Regex("""bottom$""", RegexOption.IGNORE_CASE)
    private val orSplit = Regex("""\s+or\s+""", RegexOption.IGNORE_CASE)
    private val wikiLinkPattern = Regex("""\[\[([^|\]]+)""")

    fun parse(note: String, dropName: String): WikiCompanionParseResult? {
        val cleaned = note.trim().trimEnd('.')
        parseDroppedTogether(cleaned, dropName)?.let { return it }
        if (!cleaned.contains("together with", ignoreCase = true)) {
            return null
        }

        val match = togetherWithPattern.find(cleaned) ?: return null
        val primaryNames = extractWikiItemNames(match.groupValues[1])
        val companionNames = extractWikiItemNames(match.groupValues[2])
        val isPrimary = primaryNames.any { WikiDropNoteClassifier.namesMatchForCompanion(dropName, it) }
        val isCompanion = companionNames.any { WikiDropNoteClassifier.namesMatchForCompanion(dropName, it) }

        return when {
            !isPrimary && isCompanion -> WikiCompanionParseResult.CompanionOnlyRow
            !isPrimary -> null
            else -> parseCompanionText(match.groupValues[2].trim().trimEnd('.'))
                .takeIf { it.isNotEmpty() }
                ?.let { WikiCompanionParseResult.Primary(it) }
        }
    }

    suspend fun resolveObj(
        itemLookup: ItemWikiLookup,
        objLookup: ObjRscmLookup,
        companionName: String,
        primaryDropName: String,
        droppedTogether: Boolean,
    ): String? {
        for (candidate in resolutionCandidates(companionName, primaryDropName, droppedTogether)) {
            objLookup.resolveWikiItem(itemLookup, candidate)?.let { return it }
        }
        return null
    }

    private fun parseDroppedTogether(note: String, dropName: String): WikiCompanionParseResult? {
        if (topAndBottomTogetherPattern.containsMatchIn(note)) {
            parseTopBottomTogether(dropName)?.let { return it }
            topAndBottomTogetherPattern.find(note)?.let { match ->
                val base = match.groupValues[1].trim()
                return droppedTogetherResult(dropName, "$base top", "$base bottom")
            }
        }

        val match = droppedTogetherPattern.find(note) ?: return null
        var item1 = match.groupValues[1].trim().trimEnd('.')
        var item2 = match.groupValues[2].trim().trimEnd('.')
        if (item2.equals("bottom", ignoreCase = true) && item1.contains("top", ignoreCase = true)) {
            item2 = item1.replace(Regex("top", RegexOption.IGNORE_CASE), "bottom")
        }
        return droppedTogetherResult(dropName, item1, item2)
    }

    private fun droppedTogetherResult(
        dropName: String,
        primaryCandidate: String,
        companionCandidate: String,
    ): WikiCompanionParseResult? {
        val primaryNames = expandItemNames(primaryCandidate)
        val companionNames = expandItemNames(companionCandidate)
        val isPrimary =
            primaryNames.any { WikiDropNoteClassifier.namesMatchForCompanion(dropName, it) } ||
                WikiDropNoteClassifier.namesMatchForCompanion(dropName, primaryCandidate)
        val isCompanion =
            companionNames.any { WikiDropNoteClassifier.namesMatchForCompanion(dropName, it) } ||
                WikiDropNoteClassifier.namesMatchForCompanion(dropName, companionCandidate)

        return when {
            isPrimary && !isCompanion ->
                WikiCompanionParseResult.Primary(
                    companionNames.map { WikiCompanionDropSpec(wikiNames = listOf(it), droppedTogether = true) },
                )
            isCompanion && !isPrimary -> WikiCompanionParseResult.CompanionOnlyRow
            isPrimary && isCompanion -> {
                val companions =
                    companionNames.filterNot { WikiDropNoteClassifier.namesMatchForCompanion(dropName, it) }
                companions
                    .takeIf { it.isNotEmpty() }
                    ?.let { names ->
                        WikiCompanionParseResult.Primary(
                            names.map { WikiCompanionDropSpec(wikiNames = listOf(it), droppedTogether = true) },
                        )
                    }
            }
            else -> null
        }
    }

    private fun parseTopBottomTogether(dropName: String): WikiCompanionParseResult? {
        val trimmed = dropName.trim()
        return when {
            topSuffix.containsMatchIn(trimmed) && !bottomSuffix.containsMatchIn(trimmed) ->
                WikiCompanionParseResult.Primary(
                    listOf(
                        WikiCompanionDropSpec(
                            wikiNames = listOf(topSuffix.replace(trimmed, "bottom")),
                            droppedTogether = true,
                        ),
                    ),
                )
            bottomSuffix.containsMatchIn(trimmed) -> WikiCompanionParseResult.CompanionOnlyRow
            else -> null
        }
    }

    private fun resolutionCandidates(
        companionName: String,
        primaryDropName: String,
        droppedTogether: Boolean,
    ): List<String> {
        if (!droppedTogether) {
            return listOf(companionName.trim())
        }

        val dose =
            Regex("""\((\d+)\)""").find(primaryDropName)?.groupValues?.get(1)
                ?: Regex("""\((\d+)\)""").find(companionName)?.groupValues?.get(1)
                ?: "3"
        val singular = companionName.trim().trimEnd('.').removeSuffix("s").trim()
        val withoutPotions = singular.replace(Regex("""\s+potions?""", RegexOption.IGNORE_CASE), "").trim()

        val candidates = linkedSetOf<String>()
        if (withoutPotions.isNotBlank()) {
            candidates += "${titleCaseWords(withoutPotions)}($dose)"
            candidates += "$withoutPotions($dose)"
        }
        candidates += companionName.trim()
        candidates += singular
        return candidates.toList()
    }

    private fun expandItemNames(text: String): List<String> =
        extractWikiItemNames(text).ifEmpty { listOf(text.trim()).filter { it.isNotBlank() } }

    private fun parseCompanionText(text: String): List<WikiCompanionDropSpec> {
        var remaining = text.trim().trimEnd('.')
        var count = 1

        countPrefixPattern.find(remaining)?.let { match ->
            count = parseCountWord(match.groupValues[1]) ?: 1
            remaining = remaining.substring(match.range.last + 1).trim()
        }

        if (orSplit.containsMatchIn(remaining)) {
            val names = remaining.split(orSplit).flatMap(::extractWikiItemNames)
            if (names.size >= 2 && isGenderLegSkirtPair(names)) {
                return listOf(WikiCompanionDropSpec(wikiNames = names, genderSplit = true, count = count))
            }
        }

        return extractWikiItemNames(remaining).map { WikiCompanionDropSpec(wikiNames = listOf(it), count = count) }
    }

    private fun extractWikiItemNames(text: String): List<String> {
        val fromLinks = wikiLinkPattern.findAll(text).map { it.groupValues[1].trim() }.filter { it.isNotBlank() }
        return if (fromLinks.any()) fromLinks.toList() else listOf(text.trim()).filter { it.isNotBlank() }
    }

    private fun isGenderLegSkirtPair(names: List<String>): Boolean {
        val normalized = names.map { it.lowercase().replace(Regex("""[^a-z0-9]+"""), " ") }
        val hasLegs = normalized.any { it.contains("platelegs") || it.endsWith(" legs") }
        val hasSkirt = normalized.any { it.contains("plateskirt") || it.contains(" skirt") }
        return hasLegs && hasSkirt
    }

    private fun titleCaseWords(text: String): String =
        text.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

    private fun parseCountWord(word: String): Int? =
        when (word.lowercase()) {
            "one" -> 1
            "two" -> 2
            "three" -> 3
            "four" -> 4
            "five" -> 5
            else -> word.toIntOrNull()
        }
}

// ---------------------------------------------------------------------------
// Quest-gated drops
// ---------------------------------------------------------------------------

enum class WikiQuestDropMode {
    RequiresCompleted,
    RequiresDuring,
    RequiresNotCompleted,
}

data class WikiQuestDropRequirement(val questKey: String, val mode: WikiQuestDropMode)

object WikiQuestDropParser {
    private val WIKI_LINK = Regex("""\[\[([^|\]#]+)""")
    private val notCompletedPattern =
        Regex(
            """(?i)(?:only\s+)?(?:dropped\s+)?(?:if\s+)?(?:[\w\s']+\s+)?(?:isn't|is\s+not|aren't|are\s+not|haven't|has\s+not(?:\s+been)?)\s+completed""",
        )
    private val notCompletedQuestPattern =
        Regex("""(?i)if\s+(.+?)\s+(?:isn't|is\s+not|aren't|are\s+not|haven't|has\s+not(?:\s+been)?)\s+completed""")
    private val afterCompletionPattern =
        Regex("""(?i)(?:only\s+)?(?:dropped\s+)?(?:after\s+completion\s+of|after\s+completing|upon\s+completion\s+of)""")
    private val duringPattern =
        Regex("""(?i)(?:only\s+)?(?:dropped\s+)?(?:when\s+fought\s+)?during(?:\s+the)?(?:\s+quest)?\s+""")
    private val onlyDuringPattern = Regex("""(?i)only\s+during\s+""")
    private val nonQuestPattern =
        Regex("""(?i)\bdiary\b|quest variant|unowned during""")

    fun parse(note: String): WikiQuestDropRequirement? {
        val cleaned = note.trim()
        if (cleaned.isBlank() || nonQuestPattern.containsMatchIn(cleaned)) {
            return null
        }

        val mode =
            when {
                notCompletedPattern.containsMatchIn(cleaned) -> WikiQuestDropMode.RequiresNotCompleted
                afterCompletionPattern.containsMatchIn(cleaned) -> WikiQuestDropMode.RequiresCompleted
                duringPattern.containsMatchIn(cleaned) || onlyDuringPattern.containsMatchIn(cleaned) ->
                    WikiQuestDropMode.RequiresDuring
                else -> return null
            }

        val questName = extractQuestName(cleaned, mode) ?: return null
        return WikiQuestDropRequirement(questKey = WikiQuestNameLookup.toQuestKey(questName), mode = mode)
    }

    private fun extractQuestName(note: String, mode: WikiQuestDropMode): String? {
        WIKI_LINK.findAll(note).forEach { match ->
            val title = cleanQuestTitle(match.groupValues[1].trim())
            if (title.length >= 3 && !title.equals("quest", ignoreCase = true)) {
                return title
            }
        }

        val raw =
            when (mode) {
                WikiQuestDropMode.RequiresNotCompleted ->
                    notCompletedQuestPattern.find(note)?.groupValues?.get(1)?.trim()
                WikiQuestDropMode.RequiresCompleted ->
                    afterCompletionPattern.find(note)?.range?.last?.plus(1)?.let { note.substring(it).trim() }
                WikiQuestDropMode.RequiresDuring ->
                    duringPattern.find(note)?.range?.last?.plus(1)?.let { note.substring(it).trim() }
                        ?: onlyDuringPattern.find(note)?.range?.last?.plus(1)?.let { note.substring(it).trim() }
            } ?: return null

        return cleanQuestTitle(raw.substringBefore('.').substringBefore(',').substringBefore(" if ").trim())
            .takeIf { it.length >= 3 && !it.equals("quest", ignoreCase = true) }
    }

    private fun cleanQuestTitle(title: String): String =
        title
            .removeSuffix("(quest)")
            .removeSuffix("(Quest)")
            .replace(Regex("""\s*\(quest\)\s*""", RegexOption.IGNORE_CASE), " ")
            .trim()
}

object WikiQuestNameLookup {
    private val byNormalizedName =
        mapOf(
            normalize("Monkey Madness II") to "quest_monkeymadness2",
            normalize("Rag and Bone Man I") to "quest_ragandboneman1",
            normalize("Rag and Bone Man II") to "quest_ragandboneman2",
            normalize("Dragon Slayer I") to "quest_dragonslayer1",
            normalize("The Fremennik Trials") to "quest_fremenniktrials",
            normalize("Underground Pass") to "quest_undergroundpass",
            normalize("Grim Tales") to "quest_grimtales",
            normalize("Legends' Quest") to "quest_legendsquest",
            normalize("Roving Elves") to "quest_rovingelves",
            normalize("Tree Gnome Village") to "quest_treegnomevillage",
            normalize("Eagles' Peak") to "quest_eaglespeak",
            normalize("Priest in Peril") to "quest_priestinperil",
            normalize("Between a Rock...") to "quest_betweenarock",
            normalize("Rum Deal") to "quest_rumdeal",
            normalize("The Great Brain Robbery") to "quest_greatbrainrobbery",
            normalize("One Small Favour") to "quest_onesmallfavour",
            normalize("Desert Treasure I") to "quest_deserttreasure1",
            normalize("Lunar Diplomacy") to "quest_lunardiplomacy",
            normalize("Troll Stronghold") to "quest_trollstronghold",
            normalize("Observatory Quest") to "quest_observatoryquest",
            normalize("X Marks the Spot") to "quest_xmarksthespot",
        )

    fun toQuestKey(wikiName: String): String {
        val cleaned = wikiName.trim().removeSuffix(".").trim()
        byNormalizedName[normalize(cleaned)]?.let { return it }
        val slug = cleaned.lowercase().replace(Regex("""[^a-z0-9']+"""), "").replace("'", "")
        return "quest_$slug"
    }

    private fun normalize(name: String): String =
        name.lowercase().replace(Regex("""[^a-z0-9']+"""), " ").trim()
}
