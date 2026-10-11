package org.rsmod.content.other.spawnfacing

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.michaelbull.logging.InlineLogger
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Turns map npcs to the direction they face in game. Map spawns carry no direction, so every npc
 * would otherwise face its type's `respawnDir` (south for most).
 *
 * Runs in `startup()`, when map npcs are queued but not yet added: the client avatar reads
 * `respawnDir` when the npc is added, so setting it later would need a turn.
 */
class SpawnFacingScript @Inject constructor(private val npcRepo: NpcRepository) :
    PluginScript() {
    private val logger = InlineLogger()

    override fun ScriptContext.startup() {
        val facings = load().associateBy { it.coordGrid() }
        var applied = 0
        for (npc in npcRepo.pendingDelayedAdds()) {
            val facing = facings[npc.coords] ?: continue
            if (npc.id != facing.npc.asRSCM(RSCMType.NPC)) {
                continue
            }
            npc.respawnDir = facing.direction()
            applied++
        }
        if (applied != facings.size) {
            logger.warn { "Spawn facing: applied $applied of ${facings.size} entries." }
        }
    }

    private fun load(): List<SpawnFacing> {
        val stream =
            javaClass.classLoader.getResourceAsStream(RESOURCE)
                ?: error("Missing resource: $RESOURCE")
        return stream.use { MAPPER.readValue<SpawnFacingFile>(it).facing }
    }

    private data class SpawnFacingFile(val facing: List<SpawnFacing>)

    /** [coords] use the spawn files' `level_mx_mz_lx_lz` format. */
    private data class SpawnFacing(val npc: String, val coords: String, val dir: String) {
        fun coordGrid(): CoordGrid {
            val (level, mx, mz, lx, lz) = coords.split('_').map(String::toInt)
            return CoordGrid(level, mx, mz, lx, lz)
        }

        fun direction(): Direction =
            Direction.entries.firstOrNull { it.name.equals(dir, ignoreCase = true) }
                ?: error("Unknown direction '$dir' for $npc at $coords")
    }

    private companion object {
        const val RESOURCE = "spawn_facing.toml"

        val MAPPER: ObjectMapper = ObjectMapper(TomlFactory()).registerKotlinModule()
    }
}
