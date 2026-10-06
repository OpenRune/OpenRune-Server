package org.rsmod.server.app

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.michaelbull.logging.InlineLogger
import dev.openrune.DirectoryConstants
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import org.rsmod.api.parsers.toml.Toml
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid

class WorldTypeNpcSpawns
@Inject
constructor(
    private val npcRepo: NpcRepository,
    @Toml private val toml: ObjectMapper,
) {
    private val logger = InlineLogger()

    /** Spawns every npc declared for [worldTypes]. Returns how many were added. */
    fun spawnAll(worldTypes: Collection<String>): Int {
        var total = 0
        for (worldType in worldTypes) {
            val directory = rootDirectory.resolve(worldType)
            if (!directory.isDirectory()) {
                continue
            }
            val spawned = spawnFrom(directory, worldType)
            if (spawned > 0) {
                logger.info { "Spawned $spawned npc(s) for world type '$worldType'." }
            }
            total += spawned
        }
        return total
    }

    private fun spawnFrom(directory: Path, worldType: String): Int {
        var spawned = 0
        for (file in tomlFiles(directory)) {
            val parsed =
                try {
                    toml.readValue(file.toFile(), TomlNpcSpawnFile::class.java)
                } catch (e: Exception) {
                    logger.error(e) { "Could not read world-type npc spawns: $file" }
                    continue
                }
            for (spawn in parsed.spawn) {
                if (addSpawn(spawn, file, worldType)) {
                    spawned++
                }
            }
        }
        return spawned
    }

    /**
     * A bad entry skips itself and logs rather than aborting the file or the boot. All three steps
     * can throw on operator-written input: `asRSCM` for an unknown or wrongly prefixed symbol,
     * `parseCoordGrid` for a malformed coord, and `addDelayed` for coords inside a region's
     * working area.
     */
    private fun addSpawn(spawn: TomlNpcSpawn, file: Path, worldType: String): Boolean =
        try {
            val type =
                ServerCacheManager.getNpc(spawn.npc.asRSCM(RSCMType.NPC))
                    ?: error("no such npc type")
            val npc = Npc(type, parseCoordGrid(spawn.coords))
            npcRepo.addDelayed(npc, spawnDelay = 0, duration = Int.MAX_VALUE)
            true
        } catch (e: Exception) {
            logger.error(e) {
                "Skipped spawn '${spawn.npc}' at '${spawn.coords}' in $file " +
                    "(world type '$worldType')."
            }
            false
        }

    private fun tomlFiles(directory: Path): List<Path> =
        Files.list(directory).use { paths ->
            paths.filter { it.isRegularFile() && it.extension == "toml" }.sorted().toList()
        }

    private fun parseCoordGrid(value: String): CoordGrid {
        val split = value.split('_')
        require(split.size == 5) {
            "CoordGrid must contain 5 values separated by '_'. (ex: 0_50_50_0_0)"
        }
        return CoordGrid(
            level = split[0].toInt(),
            mx = split[1].toInt(),
            mz = split[2].toInt(),
            lx = split[3].toInt(),
            lz = split[4].toInt(),
        )
    }

    private val rootDirectory: Path
        get() = DirectoryConstants.DATA_PATH.resolve(ROOT_DIR_NAME)

    internal data class TomlNpcSpawnFile(val spawn: List<TomlNpcSpawn> = emptyList())

    internal data class TomlNpcSpawn(val npc: String, val coords: String)

    private companion object {
        private const val ROOT_DIR_NAME = "world-spawns"
    }
}
