package dev.openrune.pack

import dev.openrune.cache.filestore.definition.InterfaceType
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.definition.dbtables.DBTable
import java.io.File

abstract class PluginPack {

    open fun shouldAlwaysPack(): Boolean = false

    open fun isEnabled(projectRoot: File): Boolean = true

    fun shouldPack(projectRoot: File): Boolean = shouldAlwaysPack() || isEnabled(projectRoot)

    open fun resourceRoot(): File? = conventionResourceRoot()

    fun configDirectory(): File? = resourceDirectory(CONFIGS)

    fun modelDirectory(): File? = resourceDirectory(MODELS)

    fun spriteDirectory(): File? = resourceDirectory(SPRITES)

    fun cs2Directory(): File? = resourceDirectory(CS2)

    open fun dbTables(): List<DBTable> = emptyList()

    open fun interfaces(): List<InterfaceType> = emptyList()

    open fun extraTasks(): List<CacheTask> = emptyList()

    private fun resourceDirectory(name: String): File? =
        resourceRoot()?.let { File(it, name) }?.takeIf { it.isDirectory }

    private fun conventionResourceRoot(): File? {
        val module = moduleRoot() ?: return null
        return listOfNotNull(module, module.parentFile)
            .map { File(it, RESOURCE_PATH) }
            .firstOrNull { it.isDirectory }
    }

    private fun moduleRoot(): File? {
        val location = javaClass.protectionDomain?.codeSource?.location ?: return null
        val source = runCatching { File(location.toURI()) }.getOrNull() ?: return null

        var dir: File? = if (source.isDirectory) source else source.parentFile
        while (dir != null) {
            if (dir.name == BUILD_DIR) {
                return dir.parentFile
            }
            dir = dir.parentFile
        }
        return null
    }

    private companion object {
        const val RESOURCE_PATH = "src/main/resources/pack"
        const val BUILD_DIR = "build"

        const val CONFIGS = "configs"
        const val MODELS = "models"
        const val SPRITES = "sprites"
        const val CS2 = "cs2"
    }
}
