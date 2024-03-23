package gg.rsmod.game.service.cache

import dev.openrune.cache.tools.Builder
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.TaskType
import dev.openrune.cache.tools.tasks.impl.PackMaps
import dev.openrune.cache.tools.tasks.impl.defs.PackItems
import gg.rsmod.util.ServerProperties
import java.io.*

val tasks : Array<CacheTask> = arrayOf(
    PackItems(File("./custom/definitions/items/")),
    PackMaps(File("./custom/maps/"),File("./data/cache/xteas.json"))
)


class InstallService {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val yamlFile = File("./game.example.yml")
            val gameProperties = ServerProperties()
            gameProperties.loadYaml(yamlFile)
            val cacheBuildValue = gameProperties.get<Int>("revision") ?: 220

            val builder = Builder(type = TaskType.FRESH_INSTALL, revision = cacheBuildValue,File("./data/cache/"))
            builder.extraTasks(*tasks).build().initialize()
            TypeDumper.int(cacheBuildValue)
        }
    }
}