package org.rsmod.content.quest.area.taverley.druidicritual

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.npc.owner.assignSpawnOwner
import org.rsmod.api.npc.vars.intVarn
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.forcedWalk
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The prison door to the Cauldron of Thunder and the two suits of armour that guard it. */
class CauldronPrisonDoor
@Inject
constructor(
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val aiPlayerInteractions: AiPlayerInteractions,
    private val worldQueues: WorldQueueList,
    private val mapClock: MapClock,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1(DOOR) { useDoor() }
        onOpLoc1(DOOR_LEFT) { useDoor() }
    }

    private suspend fun ProtectedAccess.useDoor() {
        arriveDelay()
        val inside = player.coords.x >= DOOR_X
        if (!inside) {
            wakeSuits()
        }
        val z = player.coords.z.coerceIn(LEFT_Z, RIGHT_Z)
        val from = CoordGrid(if (inside) DOOR_X else DOOR_X - 1, z, player.coords.level)
        val to = CoordGrid(if (inside) DOOR_X - 1 else DOOR_X, z, player.coords.level)
        openDoor()
        val route = if (player.coords == from) listOf(to) else listOf(from, to)
        forcedWalk(route, route.size)
    }

    private suspend fun ProtectedAccess.wakeSuits() {
        val dormant =
            SUIT_TILES.mapNotNull { coords -> dormantSuit(coords) }
                .sortedBy { it.coords.chebyshevDistance(player.coords) }
        for ((index, suit) in dormant.withIndex()) {
            if (index == 0) {
                mes("Suddenly the suit of armour comes to life!")
                faceSquare(CoordGrid(DOOR_X, player.coords.z, player.coords.level))
            }
            animate(suit)
            delay(1)
        }
    }

    private fun dormantSuit(coords: CoordGrid): LocInfo? =
        locRepo.findAll(coords).firstOrNull {
            it.id == DORMANT_SUIT.asRSCM(RSCMType.LOC) && it.shape == LocShape.CentrepieceStraight
        }

    private fun ProtectedAccess.animate(dormant: LocInfo) {
        if (!locRepo.del(dormant, SUIT_LIFESPAN)) {
            return
        }
        val suit = Npc(SUIT_NPC, dormant.coords)
        npcRepo.add(suit, SUIT_LIFESPAN)
        suit.assignSpawnOwner(player, this@CauldronPrisonDoor.mapClock.cycle)
        suit.opPlayer2(player, aiPlayerInteractions)
        worldQueues.add(SUIT_IDLE_CHECK) { returnWhenIdle(suit, dormant) }
    }

    private fun returnWhenIdle(suit: Npc, dormant: LocInfo) {
        if (suit.isSlotAssigned && mapClock.cycle - suit.lastCombat < SUIT_IDLE_CHECK) {
            worldQueues.add(SUIT_IDLE_CHECK) { returnWhenIdle(suit, dormant) }
            return
        }
        if (suit.isSlotAssigned) {
            npcRepo.del(suit, Int.MAX_VALUE)
        }
        locRepo.add(dormant, Int.MAX_VALUE)
    }

    private fun ProtectedAccess.openDoor() {
        soundSynth(DOOR_SOUND)
        val level = player.coords.level
        for ((z, open, angle) in LEAVES) {
            val door = CoordGrid(DOOR_X, z, level)
            locRepo.add(door, INVISWALL, DOOR_TICKS, LocAngle.West, LocShape.WallStraight)
            locRepo.add(CoordGrid(DOOR_X - 1, z, level), open, DOOR_TICKS, angle, LocShape.WallStraight)
        }
    }

    private var Npc.lastCombat by intVarn("varn.lastcombat")

    private data class Leaf(val z: Int, val open: String, val angle: LocAngle)

    private companion object {
        const val DOOR = "loc.cauldrondoor"
        const val DOOR_LEFT = "loc.cauldrondoor_l"
        const val INVISWALL = "loc.inviswall"
        const val DOOR_SOUND = "synth.iron_door_open"
        const val DOOR_TICKS = 3
        const val DOOR_X = 2889
        const val LEFT_Z = 9830
        const val RIGHT_Z = 9831

        const val SUIT_NPC = "npc.suit_of_armour"
        const val DORMANT_SUIT = "loc.suitofarmour_darkknight"
        const val SUIT_LIFESPAN = 3000
        const val SUIT_IDLE_CHECK = 50

        val SUIT_TILES = listOf(CoordGrid(2887, 9829, 0), CoordGrid(2887, 9832, 0))
        val LEAVES =
            listOf(
                Leaf(LEFT_Z, "loc.inactiveprisondoor_l", LocAngle.South),
                Leaf(RIGHT_Z, "loc.inactiveprisondoor", LocAngle.North),
            )
    }
}
