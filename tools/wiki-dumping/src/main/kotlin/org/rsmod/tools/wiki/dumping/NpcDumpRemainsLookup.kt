package org.rsmod.tools.wiki.dumping

/**
 * NPC id → dropped remains (`param_46` / `dropped_remains`) from preloaded [NpcDumpIndex].
 *
 * The death system spawns these automatically; wiki "100%" remains must not be duplicated in drop tables.
 */
class NpcDumpRemainsLookup(
    private val remainsByNpcId: Map<Int, String>,
) {
    private val explicitRemainsObjKeys: Set<String> = remainsByNpcId.values.toSet()

    fun remainsFor(npcId: Int): String = remainsByNpcId[npcId] ?: DEFAULT_REMAINS

    fun remainsForNpcs(npcIds: Iterable<Int>): Set<String> {
        val ids = npcIds.toList()
        if (ids.isEmpty()) {
            return setOf(DEFAULT_REMAINS)
        }
        return ids.map(::remainsFor).toSet()
    }

    fun isEngineRemainsDrop(objKey: String, wikiName: String, npcIds: Iterable<Int>): Boolean {
        if (objKey in remainsForNpcs(npcIds)) {
            return true
        }
        if (isRemainsWikiName(wikiName)) {
            if (wikiName.equals("Bones", ignoreCase = true)) {
                return true
            }
            val expected = "obj.${wikiNameToObjKey(wikiName)}"
            if (objKey == expected) {
                return true
            }
            if (objKey in explicitRemainsObjKeys) {
                return true
            }
        }
        val lowerName = wikiName.lowercase()
        if (lowerName.endsWith(" bones") || lowerName.endsWith(" ashes")) {
            return objKey in remainsForNpcs(npcIds)
        }
        return false
    }

    /** Wiki display name for automatic death remains (100% guaranteed bones/ashes/carcass/etc.). */
    fun isRemainsWikiName(wikiName: String): Boolean {
        val name = wikiName.trim()
        if (name.equals("Bones", ignoreCase = true) || name.equals("Ashes", ignoreCase = true)) {
            return true
        }
        val lower = name.lowercase()
        if (lower in EXPLICIT_REMAINS_WIKI_NAMES) {
            return true
        }
        if (lower.startsWith("bones (") && lower.endsWith(")")) {
            return true
        }
        return lower.endsWith(" bones") ||
            lower.endsWith(" bone") ||
            lower.endsWith(" ashes") ||
            lower.endsWith(" ash") ||
            lower.endsWith(" remains") ||
            lower.endsWith(" carcass")
    }

    /**
     * True when [wikiName] is generic remains or matches this NPC's engine remains (`param_46`).
     * Used to ignore wiki ==Drops== sections that only duplicate automatic death loot.
     */
    fun isIgnorableRemainsWikiDrop(wikiName: String, npcIds: Iterable<Int>): Boolean {
        if (isRemainsWikiName(wikiName)) {
            return true
        }
        val objKey = "obj.${wikiNameToObjKey(wikiName)}"
        if (objKey in remainsForNpcs(npcIds)) {
            return true
        }
        // NMZ / variant npc ids may omit param_46; still matches quest boss remains in dump.npc.
        return objKey in explicitRemainsObjKeys
    }

    private fun wikiNameToObjKey(displayName: String): String =
        displayName
            .trim()
            .lowercase()
            .replace(Regex("""[^a-z0-9]+"""), "_")
            .trim('_')

    companion object {
        const val DEFAULT_REMAINS: String = "obj.bones"

        /** Wiki names for param_46-style remains that do not match suffix heuristics. */
        private val EXPLICIT_REMAINS_WIKI_NAMES =
            setOf(
                "crimson fibre",
            )
    }
}
