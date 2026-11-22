package org.rsmod.content.bosses.dagannothkings

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ObjectServerType
import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.forcedWalk
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class WaterbirthPressureDoors
@Inject
constructor(
    private val players: PlayerRepository,
    private val npcs: NpcRepository,
    private val locs: LocRepository,
    private val world: WorldRepository,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1("loc.dagannoth_pressure_door") { open(it.loc, it.type, SOUTH_PADS) }
        onOpLoc1("loc.dagannoth_pressure_door2") { open(it.loc, it.type, MIDDLE_PADS) }
        onOpLoc1("loc.dagannoth_pressure_door3") { open(it.loc, it.type, NORTH_PADS) }
    }

    private suspend fun ProtectedAccess.open(
        door: BoundLocInfo,
        closed: ObjectServerType,
        pads: List<CoordGrid>,
    ) {
        arriveDelay()
        if (!pressed(pads[0]) || !pressed(pads[1])) {
            mes("The door is held shut by a pressure mechanism.")
            return
        }
        world.locAnim(door, "seq.pressure_door_open")
        world.soundArea(door.coords, "synth.dagganoth_pressure_door_open")
        locs.add(
            door.coords,
            "loc.dagannoth_pressure_open",
            OPEN_TICKS,
            door.angle,
            door.shape,
        ) {
            locs.add(door.coords, closed.internalName, Int.MAX_VALUE, door.angle, door.shape)
        }
        val direction = if (player.coords.x <= door.coords.x) 1 else -1
        forcedWalk(door.coords.translateX(direction), crossTiles = 2)
        world.locAnim(door, "seq.pressure_door_close")
        world.soundArea(door.coords, "synth.dagganoth_pressure_door_openandclose")
        delay(1)
        locs.change(door, closed, Int.MAX_VALUE)
    }

    private fun pressed(pad: CoordGrid): Boolean {
        if (players.findAll(pad).any()) {
            return true
        }
        return npcs.findAll(pad).any { it.visType.id in PRESSURE_SUBSTITUTES }
    }

    private companion object {
        const val OPEN_TICKS = 50

        val SOUTH_PADS = listOf(CoordGrid(2490, 10162, 0), CoordGrid(2490, 10164, 0))
        val MIDDLE_PADS = listOf(CoordGrid(2490, 10146, 0), CoordGrid(2490, 10148, 0))
        val NORTH_PADS = listOf(CoordGrid(2490, 10130, 0), CoordGrid(2490, 10132, 0))

        val PRESSURE_SUBSTITUTES =
            setOf(
                "npc.dagannoth_dungeon_pressure_pet_rock".asRSCM(RSCMType.NPC),
                "npc.dagannoth_dungeon_pressure_pet_egg".asRSCM(RSCMType.NPC),
                "npc.dagannoth_dungeon_pressure_pet_easter26_egg".asRSCM(RSCMType.NPC),
            )
    }
}
