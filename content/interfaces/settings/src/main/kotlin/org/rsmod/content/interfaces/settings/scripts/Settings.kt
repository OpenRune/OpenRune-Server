package org.rsmod.content.interfaces.settings.scripts

import com.google.common.collect.ImmutableMap
import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.StructType
import dev.openrune.types.enums.EnumTypeMap
import java.util.*
import org.rsmod.api.enums.SettingsEnums


object Settings {

    val categories: List<SettingCategory> = getCategories(SettingsEnums.settings_search_categories)
    val nonSearchableCategories: List<SettingCategory> = getCategories(SettingsEnums.settings_non_search_categories)
    val allSettings: List<Setting> = categories.flatMap { it.settings }
    val settingsByType: Map<SettingType, List<Setting>> = allSettings.groupBy(Setting::type)

    init {
        require(categories !== nonSearchableCategories) {
            "Searchable and non-searchable categories are no longer expected to be the same instance."
        }
    }

    private fun getCategories(categoriesEnum: EnumTypeMap<Int, Int>): List<SettingCategory> {
        val sorted = TreeMap(categoriesEnum.backing)
        return sorted.values.mapNotNull { structId ->
            val struct: StructType = ServerCacheManager.getStruct(structId as Int) ?: return@mapNotNull null
            SettingCategory(struct)
        }
    }

    fun getCategory(id: Int): SettingCategory? =
        categories.firstOrNull { it.id == id }

    fun getSetting(id: Int): Setting =
        allSettings.firstOrNull { it.id == id } ?: error("Setting with struct '$id' does not exist.")

    fun findSettingByStructId(id: Int): Setting =
        allSettings.firstOrNull { it.structId == id } ?: error("Setting with struct '$id' does not exist.")


    val DEFAULT_KEYBINDS_STRUCTS = mapOf(
        2739 to "F1",
        2742 to "F2",
        2745 to "F3",
        2751 to "F4",
        2740 to "F5",
        2743 to "F6",
        2747 to "F7",
        2746 to "F8",
        2749 to "F9",
        2741 to "F10",
        2744 to "F11",
        2750 to "F12",
        2752 to "None"
    )

    fun getDefaultKeybinds(): Map<Setting, Int> {
        val map = mutableMapOf<Setting, Int>()
        for ((structId, selection) in DEFAULT_KEYBINDS_STRUCTS) {
            val setting = findSettingByStructId(structId)
            val key = setting.dropdownEntries?.firstOrNull { it.value == selection }?.key ?: continue
            map[setting] = key
        }
        return map
    }

}
