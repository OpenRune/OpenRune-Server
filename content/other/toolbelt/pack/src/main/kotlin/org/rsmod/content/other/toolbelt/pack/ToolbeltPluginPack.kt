package org.rsmod.content.other.toolbelt.pack

import dev.openrune.cache.filestore.definition.InterfaceType
import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack
import java.io.File

class ToolbeltPluginPack : PluginPack() {
    override fun shouldAlwaysPack(): Boolean = true

    override fun isEnabled(projectRoot: File): Boolean = ToolbeltPackSettings.isEnabled(projectRoot)

    override fun packScriptDirectories(projectRoot: File): List<File> {
        if (!isEnabled(projectRoot)) {
            return emptyList()
        }
        return listOf(ToolbeltPackSettings.scriptDirectory(projectRoot))
    }

    override fun configDirectories(projectRoot: File): List<File> =
        listOf(ToolbeltPackSettings.configDirectory(projectRoot))

    override fun spriteDirectories(projectRoot: File): List<File> =
        listOf(ToolbeltPackSettings.spriteDirectory(projectRoot))

    override fun cs2Directories(projectRoot: File): List<File> =
        listOf(ToolbeltPackSettings.cs2Directory(projectRoot))

    override fun dbTables(): List<DBTable> = listOf(ToolbeltTable.slots())

    override fun interfaces(): List<InterfaceType> =
        listOf(buildWornItemsToolbelt(), buildToolbeltInterface())
}
