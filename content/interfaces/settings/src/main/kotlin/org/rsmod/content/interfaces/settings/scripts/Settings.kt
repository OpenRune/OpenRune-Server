package org.rsmod.content.interfaces.settings.scripts

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
}
