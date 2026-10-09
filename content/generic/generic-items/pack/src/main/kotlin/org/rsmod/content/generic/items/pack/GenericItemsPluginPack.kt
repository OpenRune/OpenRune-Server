package org.rsmod.content.generic.items.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class GenericItemsPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> = listOf(BooksTable.books())
}
