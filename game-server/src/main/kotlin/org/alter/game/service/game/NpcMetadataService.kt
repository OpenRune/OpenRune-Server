package org.alter.game.service.game

import dev.openrune.filesystem.Cache
import gg.rsmod.util.ServerProperties
import org.alter.game.Server
import org.alter.game.fs.NpcExamineHolder
import org.alter.game.model.World
import org.alter.game.service.Service
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Service to load NPC examines from CSV file
 */
class NpcMetadataService : Service {
    private lateinit var path: Path

    override fun init(
        cache: Cache,
        server: Server,
        world: World,
        serviceProperties: ServerProperties,
    ) {
        path = Paths.get(serviceProperties.getOrDefault("path", "../data/cfg/npcs.csv"))
        if (!Files.exists(path)) {
            throw FileNotFoundException("Path does not exist. $path")
        }
        load()
    }

    private fun load() {
        path.toFile().forEachLine { line ->
            val parts = line.split(',', limit = 2)
            if (parts.size >= 2) {
                val id = parts[0].toIntOrNull()
                val examine = parts[1].trim()
                if (id != null && examine.isNotEmpty()) {
                    NpcExamineHolder.EXAMINES.put(id, examine.replace("\"", ""))
                }
            }
        }
    }
}

