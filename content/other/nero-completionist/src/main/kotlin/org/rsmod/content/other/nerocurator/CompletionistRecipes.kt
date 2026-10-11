package org.rsmod.content.other.nerocurator

internal object CompletionistRecipes {
    data class Recipe(val name: String, val input: String, val cape: String, val hood: String)
    val all = listOf(
        Recipe("Fire Completionist cape", "obj.tzhaar_cape_fire", "obj.nero_completionist_fire", "obj.nero_completionist_hood_fire"),
        Recipe("Saradomin Completionist cape", "obj.saradomin_cape", "obj.nero_completionist_saradomin", "obj.nero_completionist_hood_saradomin"),
        Recipe("Zamorak Completionist cape", "obj.zamorak_cape", "obj.nero_completionist_zamorak", "obj.nero_completionist_hood_zamorak"),
        Recipe("Guthix Completionist cape", "obj.guthix_cape", "obj.nero_completionist_guthix", "obj.nero_completionist_hood_guthix"),
        Recipe("Accumulator Completionist cape", "obj.anma_50_reward", "obj.nero_completionist_accumulator", "obj.nero_completionist_hood_accumulator"),
        Recipe("Ardougne Completionist cape", "obj.ardy_cape_elite", "obj.nero_completionist_ardougne", "obj.nero_completionist_hood_ardougne"),
        Recipe("Infernal Completionist cape", "obj.infernal_cape", "obj.nero_completionist_infernal", "obj.nero_completionist_hood_infernal"),
        Recipe("Imbued Saradomin Completionist cape", "obj.ma2_saradomin_cape", "obj.nero_completionist_imbued_saradomin", "obj.nero_completionist_hood_imbued_saradomin"),
        Recipe("Imbued Zamorak Completionist cape", "obj.ma2_zamorak_cape", "obj.nero_completionist_imbued_zamorak", "obj.nero_completionist_hood_imbued_zamorak"),
        Recipe("Imbued Guthix Completionist cape", "obj.ma2_guthix_cape", "obj.nero_completionist_imbued_guthix", "obj.nero_completionist_hood_imbued_guthix"),
        Recipe("Assembler Completionist cape", "obj.avas_assembler", "obj.nero_completionist_assembler", "obj.nero_completionist_hood_assembler"),
        Recipe("Mythical Completionist cape", "obj.mythical_cape", "obj.nero_completionist_mythical", "obj.nero_completionist_hood_mythical"),
        Recipe("Masori assembler Completionist cape", "obj.avas_assembler_masori", "obj.nero_completionist_masori_assembler", "obj.nero_completionist_hood_masori_assembler"),
        Recipe("Dizana's Completionist cape", "obj.dizanas_quiver_infinite", "obj.nero_completionist_dizana", "obj.nero_completionist_hood_dizana"),
        Recipe("Infernal Completionist cape (l)", "obj.infernal_cape_trouver", "obj.nero_completionist_infernal_locked", "obj.nero_completionist_hood_infernal_locked"),
        Recipe("Fire Completionist cape (l)", "obj.tzhaar_cape_fire_trouver", "obj.nero_completionist_fire_locked", "obj.nero_completionist_hood_fire_locked"),
        Recipe("Assembler Completionist cape (l)", "obj.avas_assembler_trouver", "obj.nero_completionist_assembler_locked", "obj.nero_completionist_hood_assembler_locked"),
        Recipe("Imbued Saradomin Completionist cape (l)", "obj.ma2_saradomin_cape_trouver", "obj.nero_completionist_imbued_saradomin_locked", "obj.nero_completionist_hood_imbued_saradomin_locked"),
        Recipe("Imbued Zamorak Completionist cape (l)", "obj.ma2_zamorak_cape_trouver", "obj.nero_completionist_imbued_zamorak_locked", "obj.nero_completionist_hood_imbued_zamorak_locked"),
        Recipe("Imbued Guthix Completionist cape (l)", "obj.ma2_guthix_cape_trouver", "obj.nero_completionist_imbued_guthix_locked", "obj.nero_completionist_hood_imbued_guthix_locked"),
        Recipe("Masori assembler Completionist cape (l)", "obj.avas_assembler_masori_trouver", "obj.nero_completionist_masori_assembler_locked", "obj.nero_completionist_hood_masori_assembler_locked"),
        Recipe("Dizana's Completionist cape (l)", "obj.dizanas_quiver_infinite_trouver", "obj.nero_completionist_dizana_locked", "obj.nero_completionist_hood_dizana_locked"),
    )
}
