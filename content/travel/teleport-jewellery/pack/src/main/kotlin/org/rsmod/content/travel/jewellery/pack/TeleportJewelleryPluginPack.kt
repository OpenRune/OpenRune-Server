package org.rsmod.content.travel.jewellery.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class TeleportJewelleryPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> =
        listOf(TeleportJewelleryTable.destinations(), TeleportJewelleryTable.pieces())
}
