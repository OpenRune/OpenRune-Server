package org.rsmod.content.travel.skillcapes.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class SkillcapeTeleportPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> = listOf(SkillcapeTeleportTable.destinations())
}
