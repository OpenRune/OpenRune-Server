package org.rsmod.tools.wiki.dumping

/** Maps wiki drop-variant headings to RSCM area keys for runtime table selection. */
object DropVariantAreas {
    private val variantKeywordsToArea =
        listOf(
            "catacombs of kourend" to "area.catacombs_of_kourend",
            "stronghold slayer cave" to "area.stronghold_slayer_dungeon",
            "slayer tower" to "area.slayer_tower",
            "karuulm slayer dungeon" to "area.karuulm_slayer_dungeon",
            "fremennik slayer dungeon" to "area.fremennik_slayer_dungeon",
        )

    fun areasForVariant(variantName: String): List<String> {
        if (variantName.isBlank()) {
            return emptyList()
        }

        val normalized = variantName.lowercase()
        return variantKeywordsToArea
            .filter { (keyword, _) -> normalized.contains(keyword) }
            .map { (_, area) -> area }
            .distinct()
    }
}
