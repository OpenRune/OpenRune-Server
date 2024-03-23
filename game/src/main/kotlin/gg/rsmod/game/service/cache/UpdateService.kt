package gg.rsmod.game.service.cache

import dev.openrune.cache.tools.Builder
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.TaskType
import dev.openrune.cache.tools.tasks.impl.PackModels
import gg.rsmod.util.ServerProperties
import java.io.*

class UpdateService {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val yamlFile = File("./game.yml")
            val gameProperties = ServerProperties()
            gameProperties.loadYaml(yamlFile)
            val cacheBuildValue = gameProperties.get<Int>("revision") ?: 220

            val builder = Builder(type = TaskType.BUILD, revision = cacheBuildValue,File("./data/cache/"))
            builder.extraTasks(*tasks).build().initialize()
            TypeDumper.int(cacheBuildValue)
        }
    }
}