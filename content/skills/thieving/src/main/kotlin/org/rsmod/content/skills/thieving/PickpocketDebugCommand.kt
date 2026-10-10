package org.rsmod.content.skills.thieving

import dev.openrune.types.NpcMode
import dev.or2.central.account.Rights
import jakarta.inject.Inject
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onCommand
import org.rsmod.api.table.thieving.ThievingPickpocketRow
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PickpocketDebugCommand
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("pickpockets") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Spawn every pickpocket target in the Lumbridge cow field"
            cheat { spawnTargets() }
        }
    }

    private fun Cheat.spawnTargets() {
        val spawned =
            ThievingPickpocketRow.all().mapIndexedNotNull { index, target ->
                val type = target.representativeType() ?: return@mapIndexedNotNull null
                val coords =
                    ORIGIN.translate((index % COLUMNS) * COLUMN_STEP, (index / COLUMNS) * ROW_STEP)
                val npc = Npc(type, coords)
                npc.mode = NpcMode.None
                npcRepo.add(npc, DURATION)
                "${type.name} (${target.level})"
            }
        protectedAccess.launch(player) { telejump(PLAYER_SPOT, TeleportType.Exempt) }
        player.mes("Spawned ${spawned.size} pickpocket targets for $DURATION ticks:")
        spawned.chunked(MES_CHUNK).forEach { player.mes(it.joinToString(", ")) }
    }

    private fun ThievingPickpocketRow.representativeType() =
        npcTypes()
            .filter { it.pickpocketSlot() != null }
            .sortedBy { type ->
                npcs.indexOfFirst { it.id == type.id }.takeIf { it >= 0 } ?: Int.MAX_VALUE
            }
            .firstOrNull()

    private companion object {
        val ORIGIN = CoordGrid(3253, 3264, 0)
        val PLAYER_SPOT = CoordGrid(3258, 3262, 0)
        const val COLUMNS = 6
        const val COLUMN_STEP = 2
        const val ROW_STEP = 3
        const val DURATION = 6000
        const val MES_CHUNK = 4
    }
}
