package org.rsmod.content.events.leaguetasks.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class LeagueTasksPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> =
        listOf(LeagueTaskTriggerTable.triggers(), LeagueTutorialHintTable.hints())
}
