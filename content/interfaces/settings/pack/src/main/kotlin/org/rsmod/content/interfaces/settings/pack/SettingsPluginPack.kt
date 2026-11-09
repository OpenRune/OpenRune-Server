package org.rsmod.content.interfaces.settings.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class SettingsPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> = listOf(AccountDefaultVarsTable.accountDefaultVars())
}
