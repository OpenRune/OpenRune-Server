package org.rsmod.tools.wiki.dumping

object NpcSpawnFilters {
    fun isExcluded(npcKey: String, displayName: String? = null): Boolean {
        if (containsLeague(npcKey)) {
            return true
        }
        if (displayName != null && containsLeague(displayName)) {
            return true
        }
        if (isTheSage(npcKey, displayName)) {
            return true
        }
        return false
    }

    fun isExcludedWikiPage(title: String): Boolean {
        if (containsLeague(title)) {
            return true
        }
        return title.equals("The Sage", ignoreCase = true)
    }

    private fun containsLeague(value: String): Boolean = value.contains("league", ignoreCase = true)

    private fun isTheSage(npcKey: String, displayName: String?): Boolean {
        if (displayName.equals("The Sage", ignoreCase = true)) {
            return true
        }
        val normalizedKey = npcKey.removePrefix("npc.").replace('_', ' ')
        return normalizedKey.equals("the sage", ignoreCase = true) ||
            normalizedKey.equals("event sage", ignoreCase = true)
    }
}
