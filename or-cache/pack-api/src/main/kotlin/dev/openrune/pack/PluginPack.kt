package dev.openrune.pack

import dev.openrune.cache.filestore.definition.InterfaceType
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.definition.dbtables.DBTable
import java.io.File

abstract class PluginPack {
    /**
     * When true, the full plugin ([configDirectories], sprites, cs2, scripts, interfaces, [dbTables])
     * is packed even if [isEnabled] is false.
     */
    open fun shouldAlwaysPack(): Boolean = false

    open fun isEnabled(projectRoot: File): Boolean = true

    fun shouldPack(projectRoot: File): Boolean = shouldAlwaysPack() || isEnabled(projectRoot)

    /**
     * Packed when [shouldPack] is false. Use for varbits, enums, and dbtables the server/client still
     * need while feature scripts stay off.
     */
    open fun configDirectoriesWhenDisabled(projectRoot: File): List<File> = emptyList()

    open fun dbTablesWhenDisabled(): List<DBTable> = emptyList()

    open fun configDirectories(projectRoot: File): List<File> = emptyList()

    open fun spriteDirectories(projectRoot: File): List<File> = emptyList()

    open fun packScriptDirectories(projectRoot: File): List<File> = emptyList()

    open fun cs2Directories(projectRoot: File): List<File> = emptyList()

    open fun dbTables(): List<DBTable> = emptyList()

    open fun interfaces(): List<InterfaceType> = emptyList()

    open fun extraTasks(): List<CacheTask> = emptyList()
}
