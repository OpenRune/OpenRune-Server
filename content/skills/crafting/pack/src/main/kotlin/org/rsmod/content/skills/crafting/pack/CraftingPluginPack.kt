package org.rsmod.content.skills.crafting.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class CraftingPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> =
        listOf(
            Crafting.facilities(),
            Crafting.hand(),
            Crafting.tanning(),
            Crafting.silver(),
            Crafting.gold(),
        )
}
