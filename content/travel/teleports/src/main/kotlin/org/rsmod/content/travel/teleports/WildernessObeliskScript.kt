package org.rsmod.content.travel.teleports

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ObjectServerType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

/**
 * The six Wilderness obelisks. Each is four pillars around a 3x3 pad, whose centres are the
 * `wilderness_obelisk` rows of `teleport_destination`.
 *
 * Touching a pillar lights all four. A few ticks later everyone standing on the pad is sent to the
 * same spot on another obelisk's pad: a random one for `Activate`, or the one saved in
 * `varbit.wilderness_portal_choice` for `Teleport to Destination`. `Set Destination` only saves
 * that choice. Both of those need the Hard Wilderness Diary reward, as in OSRS. The countdown runs
 * on the world queue, so the player who touched the pillar is free to step onto the pad meanwhile.
 */
class WildernessObeliskScript
@Inject
constructor(
    private val locRepo: LocRepository,
    private val playerList: PlayerList,
    private val worldQueues: WorldQueueList,
    private val collision: CollisionFlagMap,
    private val random: GameRandom,
) : PluginScript() {
    private val obelisks: List<TeleportDestination> by lazy { TeleportNetworks.of(NETWORK) }
    private val active = mutableSetOf<Int>()
    private val Player.hasHardDiary by boolVarBit("varbit.wilderness_hard_reward")
    private var Player.savedDestination by intVarBit("varbit.wilderness_portal_choice")
    private val pillarIds: Set<Int> by lazy { PILLARS.map { it.asRSCM(RSCMType.LOC) }.toSet() }
    private val glowing: ObjectServerType by lazy {
        ServerCacheManager.getObject(GLOWING.asRSCM(RSCMType.LOC)) ?: error("Missing $GLOWING")
    }

    override fun ScriptContext.startup() {
        for (pillar in PILLARS) {
            onOpLoc1(pillar) { activate(it.loc, destination = null) }
            onOpLoc2(pillar) { teleportToDestination(it.loc) }
            onOpLoc3(pillar) { setDestination(it.loc) }
        }
    }

    private suspend fun ProtectedAccess.setDestination(pillar: BoundLocInfo) {
        if (!checkHardDiary() || obeliskAt(pillar.coords) == null) {
            return
        }
        val labels = obelisks.map { it.name } + CANCEL
        val picked = menu(MENU_TITLE, hotkeys = true, choices = labels)
        val destination = obelisks.getOrNull(picked) ?: return
        player.savedDestination = picked + 1
        // UNVERIFIED: confirmation wording
        mes("The obelisks will now send you to ${destination.name}.")
    }

    private fun ProtectedAccess.teleportToDestination(pillar: BoundLocInfo) {
        if (!checkHardDiary()) {
            return
        }
        val from = obeliskAt(pillar.coords) ?: return
        val destination = player.savedDestination - 1
        if (destination !in obelisks.indices) {
            // UNVERIFIED: wording when no destination has been set
            mes("You need to set a destination for the obelisks first.")
            return
        }
        if (destination == from) {
            // UNVERIFIED: wording when the saved destination is this obelisk
            mes("The obelisk you are standing at is already set as your destination.")
            return
        }
        activate(pillar, destination)
    }

    private fun ProtectedAccess.checkHardDiary(): Boolean {
        if (player.hasHardDiary) {
            return true
        }
        // UNVERIFIED: wording for players without the Hard Wilderness Diary reward
        mes("You need to complete the Hard Wilderness Diary to choose an obelisk's destination.")
        return false
    }

    private fun ProtectedAccess.activate(pillar: BoundLocInfo, destination: Int?) {
        val from = obeliskAt(pillar.coords) ?: return
        if (!active.add(from)) {
            return
        }
        val to = destination ?: random.pick(obelisks.indices.filter { it != from })
        anim(TOUCH_ANIM)
        val centre = obelisks[from].coords
        for (corner in pillarCoords(centre)) {
            val loc = locRepo.findAll(corner).firstOrNull { it.id in pillarIds } ?: continue
            locRepo.change(loc, glowing, GLOW_TICKS)
        }
        worldQueues.add(CHARGE_TICKS) {
            val riders = playersOnPad(centre)
            riders.forEach {
                it.anim(TELEPORT_ANIM)
                it.spotanim(TELEPORT_SPOTANIM)
            }
            worldQueues.add(TRAVEL_TICKS) {
                val target = obelisks[to].coords
                for (rider in riders.filter { it.isOnPad(centre) }) {
                    val offset = rider.coords.translate(target.x - centre.x, target.z - centre.z, 0)
                    PathingEntityCommon.telejump(rider, collision, offset)
                    rider.resetAnim()
                    rider.resetSpotanim()
                }
                active -= from
            }
        }
    }

    private fun obeliskAt(coords: CoordGrid): Int? =
        obelisks.indices.firstOrNull {
            obelisks[it].coords.chebyshevDistance(coords) <= PILLAR_RANGE
        }

    private fun playersOnPad(centre: CoordGrid): List<Player> =
        playerList.filter { it.isOnPad(centre) }

    private fun Player.isOnPad(centre: CoordGrid): Boolean =
        coords.level == centre.level && coords.chebyshevDistance(centre) <= PAD_RADIUS

    private fun pillarCoords(centre: CoordGrid): List<CoordGrid> =
        listOf(-2, 2).flatMap { dx -> listOf(-2, 2).map { dz -> centre.translate(dx, dz, 0) } }

    private companion object {
        private const val NETWORK = "wilderness_obelisk"
        private const val MENU_TITLE = "Select a destination"
        private const val CANCEL = "Cancel"

        private val PILLARS = (0..5).map { "loc.wilderness_portal_stone_$it" }
        private const val GLOWING = "loc.wilderness_portal_stone_glowing"

        private const val TOUCH_ANIM = "seq.human_pickupfloor"
        private const val TELEPORT_ANIM = "seq.human_teleport_other_impact"
        private const val TELEPORT_SPOTANIM = "spotanim.wilderness_portal"

        private const val PILLAR_RANGE = 2
        private const val PAD_RADIUS = 1
        private const val CHARGE_TICKS = 4
        private const val TRAVEL_TICKS = 2
        private const val GLOW_TICKS = CHARGE_TICKS + TRAVEL_TICKS
    }
}
