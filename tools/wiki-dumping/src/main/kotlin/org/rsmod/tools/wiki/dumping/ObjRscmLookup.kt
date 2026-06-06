package org.rsmod.tools.wiki.dumping

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType

/**
 * Resolves wiki drop display names to `obj.*` gameval keys.
 *
 * Resolution order in [resolveWikiItem]:
 * 1. Preset item ids (clue scroll tiers, manual overrides) → reverse RSCM
 * 2. Wiki infobox item id via [ItemWikiLookup] → reverse RSCM
 * 3. Local name heuristics in [resolveByDisplayName] (aliases, dose variants, normalized keys)
 *
 * Edit override maps in the companion object when wiki lookup is insufficient.
 */
class ObjRscmLookup {
    private val cache = mutableMapOf<Int, String?>()
    private val displayNameCache = mutableMapOf<String, String?>()

    fun toRscm(itemId: Int): String? =
        cache.getOrPut(itemId) {
            reverseObj(itemId) ?: WIKI_ITEM_ID_OBJ_KEYS[itemId]?.let { lookupObjKey(it) }
        }

    suspend fun resolveWikiItem(
        itemLookup: ItemWikiLookup,
        displayName: String,
        noted: Boolean = false,
    ): String? {
        resolvePresetItemId(displayName)?.let { return it }

        itemLookup.resolveItemId(displayName)?.let { itemId ->
            if (noted) {
                resolveNotedFromItemId(itemId)?.let { return it }
            } else {
                toRscm(itemId)?.let { return it }
            }
        }

        return if (noted) {
            resolveNotedByDisplayName(displayName)
        } else {
            resolveByDisplayName(displayName)
        }
    }

    /** Resolves spawn-line names that use the infobox base name instead of the disambiguated page title. */
    suspend fun resolveWikiItemOnPage(
        itemLookup: ItemWikiLookup,
        spawnName: String,
        pageTitle: String,
        noted: Boolean = false,
    ): String? {
        resolveWikiItem(itemLookup, spawnName, noted)?.let { return it }
        if (!spawnName.equals(pageTitle, ignoreCase = true)) {
            resolveWikiItem(itemLookup, pageTitle, noted)?.let { return it }
        }
        return null
    }

    fun canResolveLocally(displayName: String, noted: Boolean = false): Boolean =
        if (noted) {
            resolveNotedByDisplayName(displayName) != null
        } else {
            resolvePresetItemId(displayName) != null || resolveByDisplayName(displayName) != null
        }

    fun canResolveWithCachedWikiId(itemLookup: ItemWikiLookup, displayName: String, noted: Boolean = false): Boolean =
        itemLookup.cachedItemId(displayName)?.let { itemId ->
            if (noted) {
                resolveNotedFromItemId(itemId) != null
            } else {
                toRscm(itemId) != null
            }
        } == true

    fun resolveByDisplayName(displayName: String): String? {
        val cacheKey = displayName.trim().lowercase()
        displayNameCache[cacheKey]?.let { return it }

        val resolved =
            resolveWikiDisplayNameAlias(displayName)
                ?: resolvePresetItemId(displayName)
                ?: resolveDoseVariant(displayName)
                ?: resolveParentheticalVariant(displayName)
                ?: resolveRewardCasketTier(displayName)
                ?: keyCandidates(displayName).firstNotNullOfOrNull { lookupObjKey(it) }

        displayNameCache[cacheKey] = resolved
        return resolved
    }

    private fun resolveWikiDisplayNameAlias(displayName: String): String? =
        WIKI_DISPLAY_NAME_OBJ_KEYS[displayName.trim().lowercase()]?.let { lookupObjKey(it) }

    private fun resolveNotedFromItemId(itemId: Int): String? {
        val baseKey = toRscm(itemId)?.removePrefix("obj.") ?: return null
        return lookupObjKey("cert_$baseKey")
    }

    private fun resolveNotedByDisplayName(displayName: String): String? {
        val cacheKey = "noted:${displayName.trim().lowercase()}"
        displayNameCache[cacheKey]?.let { return it }

        val resolved =
            resolveWikiDisplayNameAlias(displayName)?.let { lookupObjKey("cert_${it.removePrefix("obj.")}") }
                ?: keyCandidates(displayName).firstNotNullOfOrNull { lookupObjKey("cert_$it") }
        displayNameCache[cacheKey] = resolved
        return resolved
    }

    /** Preset wiki item ids and clue scroll tiers — resolved before wiki infobox lookup. */
    private fun resolvePresetItemId(displayName: String): String? {
        val normalized = displayName.trim().lowercase()
        WIKI_DISPLAY_NAME_ITEM_IDS[normalized]?.let { itemId ->
            toRscm(itemId)?.let { return it }
        }

        val tier = CLUE_SCROLL_TIER_PATTERN.matchEntire(normalized)?.groupValues?.get(1) ?: return null
        CLUE_SCROLL_TIER_ITEM_IDS[tier]?.let { itemId ->
            toRscm(itemId)?.let { return it }
        }
        CLUE_SCROLL_TIER_OBJ_KEYS[tier]?.let { return lookupObjKey(it) }
        return lookupObjKey("trail_clue_$tier") ?: lookupObjKey("clue_scroll_$tier")
    }

    private fun resolveDoseVariant(displayName: String): String? {
        val match = Regex("""^(.+?)\((\d+)\)$""").find(displayName.trim()) ?: return null
        val baseRaw = match.groupValues[1].trim()
        val dose = match.groupValues[2]

        val candidates =
            buildList {
                val plusCompact = baseRaw.lowercase().replace(Regex("""[^a-z0-9+]+"""), "")
                if (plusCompact.isNotBlank()) {
                    add("$plusCompact$dose")
                }
                val firstToken =
                    baseRaw.substringBefore(' ').lowercase().replace(Regex("""[^a-z0-9]+"""), "")
                if (firstToken.isNotBlank()) {
                    add("${firstToken}_salve_${dose}_dose")
                }
                val lastWord =
                    baseRaw.substringAfterLast(' ', baseRaw).lowercase().replace(Regex("""[^a-z0-9]+"""), "")
                if (lastWord.isNotBlank()) {
                    add("${dose}dose2$lastWord")
                }
            }

        return candidates.distinct().firstNotNullOfOrNull { lookupObjKey(it) }
    }

    private fun resolveParentheticalVariant(displayName: String): String? {
        val match = Regex("""^(.+?)\s*\((.+)\)$""").find(displayName.trim()) ?: return null
        val base = variantBaseKey(normalizeItemKey(match.groupValues[1]))
        val qualifier = normalizeItemKey(match.groupValues[2])
        if (base.isBlank() || qualifier.isBlank()) {
            return null
        }
        return listOf("${base}_$qualifier", "${qualifier}_$base")
            .distinct()
            .firstNotNullOfOrNull { lookupObjKey(it) }
    }

    private fun variantBaseKey(base: String): String = base.replace("_s_", "_").removeSuffix("_s")

    private fun resolveRewardCasketTier(displayName: String): String? {
        val match =
            Regex("""reward casket \((.+)\)""", RegexOption.IGNORE_CASE).find(displayName.trim())
                ?: return null
        val tier = match.groupValues[1].trim().lowercase().replace(' ', '_')
        return lookupObjKey("trail_reward_casket_$tier")
    }

    private fun lookupObjKey(key: String): String? {
        val fullKey = if (key.startsWith("obj.")) key else "obj.$key"
        return if (hasMapping(fullKey)) fullKey else null
    }

    private fun reverseObj(itemId: Int): String? = reverseMapping(RSCMType.OBJ, itemId)

    private fun reverseMapping(type: RSCMType, id: Int): String? {
        val mapped =
            runCatching { RSCM.getReverseMapping(type, id) }
                .getOrNull()
                ?.trim()
                .orEmpty()
        if (mapped.isBlank() || mapped == "-1") {
            return null
        }
        return if (mapped.contains('.')) mapped else "${type.prefix}.$mapped"
    }

    private fun hasMapping(fullKey: String): Boolean =
        runCatching {
            RSCM.getRSCM(fullKey)
            true
        }.getOrDefault(false)

    private fun keyCandidates(displayName: String): List<String> {
        val normalized = normalizeItemKey(displayName)
        val compact = normalized.replace("_", "")
        val parenthetical =
            Regex("""^(.+?)\s*\((.+)\)$""").find(displayName.trim())?.let { match ->
                val base = normalizeItemKey(match.groupValues[1])
                val qualifier = normalizeItemKey(match.groupValues[2])
                listOf(
                    "${base}_$qualifier",
                    "${base}_($qualifier)",
                    "${base}($qualifier)",
                    "${base}${qualifier.replace("_", "")}",
                )
            }.orEmpty()

        return buildList {
            add(normalized)
            if (compact != normalized) {
                add(compact)
            }
            addAll(parenthetical)
            addAll(parenthetical.map { it.replace("_", "") }.filter { it !in this })
        }.distinct()
    }

    private fun normalizeItemKey(displayName: String): String =
        displayName.lowercase().replace(Regex("""[^a-z0-9]+"""), "_").trim('_')

    companion object {
        private val CLUE_SCROLL_TIER_PATTERN =
            Regex("""^clue scroll \((.+)\)$""", RegexOption.IGNORE_CASE)

        // --- Manual overrides: edit these when wiki id lookup is wrong or unavailable ---

        /** Display name → item id (reverse RSCM). Used before wiki lookup. */
        private val WIKI_DISPLAY_NAME_ITEM_IDS =
            mapOf(
                "key (medium)" to 19812,
            )

        /** Clue scroll tier → representative item id (wiki uses generic tier names). */
        private val CLUE_SCROLL_TIER_ITEM_IDS =
            mapOf(
                "beginner" to 23182,
                "easy" to 2677,
                "medium" to 12021,
                "hard" to 2722,
                "elite" to 12073,
                "master" to 19835,
            )

        /** Clue scroll tier → obj key fallback when [CLUE_SCROLL_TIER_ITEM_IDS] reverse lookup fails. */
        private val CLUE_SCROLL_TIER_OBJ_KEYS =
            mapOf(
                "beginner" to "trail_clue_beginner",
                "easy" to "trail_clue_easy_simple001",
                "medium" to "trail_medium_emote_exp1",
                "hard" to "trail_clue_hard_map001",
                "elite" to "trail_elite_emote_exp1",
                "master" to "trail_clue_master",
            )

        /** Display name → obj key when gameval name differs from normalized wiki name. */
        private val WIKI_DISPLAY_NAME_OBJ_KEYS =
            mapOf(
                "brimstone key" to "konar_key",
                "bird's egg" to "bird_egg_red",
                "birds egg" to "bird_egg_red",
                "mort myre fungus" to "mortmyremushroom",
                "pure essence" to "blankrune_high",
                "bark" to "hollow_bark",
                "supercompost" to "bucket_compost",
                "oak plank" to "plank_oak",
            )

        /** Item id → obj key when RSCM reverse lookup returns nothing useful. */
        private val WIKI_ITEM_ID_OBJ_KEYS =
            mapOf(
                23083 to "konar_key",
                20543 to "trail_reward_casket_elite",
                2677 to "trail_clue_easy_simple001",
                2722 to "trail_clue_hard_map001",
                12021 to "trail_medium_emote_exp1",
                12073 to "trail_elite_emote_exp1",
                19835 to "trail_clue_master",
                5076 to "bird_egg_red",
                5077 to "bird_egg_blue",
                5078 to "bird_egg_green",
            )

        fun load(rootDir: String? = null): ObjRscmLookup {
            GameValLoader.ensureLoaded(rootDir)
            return ObjRscmLookup()
        }
    }
}
