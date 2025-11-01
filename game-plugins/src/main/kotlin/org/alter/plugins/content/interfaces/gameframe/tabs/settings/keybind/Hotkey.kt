package org.alter.plugins.content.interfaces.keybind

/**
 * @author Tom <rspsmods@gmail.com>
 */
enum class Hotkey(val id: Int, val child: Int, val varbit: String, val defaultValue: Int) {
    COMBAT(id = 0, child = 9, varbit = "varbits.stone_combat_key", defaultValue = 5),
    SKILLS(id = 1, child = 16, varbit = "varbits.stone_stats_key", defaultValue = 0),
    QUESTS(id = 2, child = 23, varbit = "varbits.stone_journal_key", defaultValue = 0),
    INVENTORY(id = 3, child = 30, varbit = "varbits.stone_inv_key", defaultValue = 1),
    EQUIPMENT(id = 4, child = 37, varbit = "varbits.stone_worn_key", defaultValue = 2),
    PRAYERS(id = 5, child = 44, varbit = "varbits.stone_prayer_key", defaultValue = 3),
    MAGIC(id = 6, child = 51, varbit = "varbits.stone_magic_key", defaultValue = 4),
    SOCIAL(id = 7, child = 58, varbit = "varbits.stone_friends_key", defaultValue = 8),
    ACCOUNT_MANAGEMENT(id = 8, child = 65, varbit = "varbits.stone_account_key", defaultValue = 9),
    LOG_OUT(id = 9, child = 72, varbit = "varbits.stone_logout_key", defaultValue = 0),
    SETTINGS(id = 10, child = 79, varbit = "varbits.stone_options1_key", defaultValue = 10),
    EMOTES(id = 11, child = 86, varbit = "varbits.stone_options2_key", defaultValue = 11),
    CLAN_CHAT(id = 12, child = 93, varbit = "varbits.stone_clanchat_key", defaultValue = 7),
    MUSIC(id = 13, child = 100, varbit = "varbits.stone_music_key", defaultValue = 12),
    ;

    companion object {
        val values = enumValues<Hotkey>()
    }
}
