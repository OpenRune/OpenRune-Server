package org.rsmod.content.bosses.zulrah

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import jakarta.inject.Inject
import org.rsmod.api.instances.InstanceAccess
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.cinematic.Cinematic
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.ui.ifCloseSub
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.map.Direction
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext


internal class ZulrahBoatScript @Inject constructor(
    private val instances: InstanceManager,
    private val eventBus: EventBus,
    private val npcs: NpcRepository,
    private val npcInteractions: AiPlayerInteractions,
    private val players: PlayerList,
    private val worldQueues: WorldQueueList,
    private val encounters: ZulrahEncounterController,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.snakeboss_boat_1op") { board() }
        onOpLoc1("loc.snakeboss_boat_2ops") { board() }
        onOpLoc2("loc.snakeboss_boat_2ops") { board() }
        onOpLoc1("loc.snakeboss_exit") {
            val session = instances.sessionForPlayer(player) ?: return@onOpLoc1
            if (session.key != ZulrahIsland.KEY || session.owner != player.uuid) return@onOpLoc1
            telejump(instances.leave(player, session, mapClock), TeleportType.Exempt)
        }
    }

    private suspend fun ProtectedAccess.board() {
        arriveDelay()
        if (player.hitpoints <= 0) return
        val ownerId = player.uuid ?: return
        if (instances.sessionForPlayer(player) != null) return
        if (instances.sessionForOwner(ownerId) != null) return

        mesbox(ROWING_MESSAGE)
        player.ifCloseSub("interface.messagebox", eventBus)
        if (player.hitpoints <= 0) return
        if (instances.sessionForPlayer(player) != null) return
        if (instances.sessionForOwner(ownerId) != null) return

        val returnTo = player.coords
        var pending: InstanceSession? = null
        var destination: CoordGrid? = null
        var entered = false

        try {
            fadeOverlay(0, 255, 0, 0, 50)
            delay(3)
            if (player.hitpoints <= 0) return

            // Allocate only after the travel wait. Do not suspend between allocation,
            // teleport, and finalizeEntry. Keep the pending-entry window within one game tick.
            val result = instances.create(
                owner = player,
                key = ZulrahIsland.KEY,
                spec = ZulrahIsland.spec(returnTo),
                access = InstanceAccess.Private,
                currentTick = mapClock,
            )
            val (session, arrival) = when (result) {
                is InstanceManager.Result.Created -> result.session to result.enter
                is InstanceManager.Result.Joined -> result.session to result.enter
                is InstanceManager.Result.Failed -> return
            }
            pending = session
            destination = arrival
            telejump(arrival, TeleportType.Exempt)
            instances.finalizeEntry(player, session, mapClock)
            entered = true
            val owner = player
            worldQueues.add(1) { spawnZulrah(owner, session) }

            delay(1)
            if (instances.sessionForPlayer(player) !== session) return
            camForceAngle(280, 1780)
            fadeOverlay(0, 0, 0, 255, 50)
        } finally {
            Cinematic.closeFadeOverlay(player, eventBus)
            val session = pending
            if (!entered && session != null && instances.sessionForId(session.id) === session) {
                if (instances.sessionForPlayer(player) === session) {
                    instances.leave(player, session, mapClock)
                } else {
                    instances.cancelPendingEntry(player, mapClock)
                }
                if (player.coords == destination) telejump(returnTo, TeleportType.Exempt)
            }
        }
    }

    private fun spawnZulrah(player: Player, session: InstanceSession) {
        if (player.loggingOut || player.pendingLogout || players.none { it === player }) return
        if (instances.sessionForId(session.id) !== session) return
        if (instances.sessionForPlayer(player) !== session) return
        if (session.key != ZulrahIsland.KEY || session.owner != player.uuid) return
        if (player.uuid !in session.occupants || player.hitpoints <= 0) return
        val arrival = instances.resolveCoord(session, ZulrahIsland.arrival) ?: return
        if (player.coords.level != arrival.level || player.coords.chebyshevDistance(arrival) > 64) return

        val cached = requireNotNull(
            ServerCacheManager.getNpc(ZulrahIsland.RANGED_FORM.asRSCM(RSCMType.NPC)),
        )
        if (instances.npcsForInstance(session.id).any { it.id == cached.id }) return
        val coords = instances.resolveCoord(session, ZulrahIsland.openingSpawn) ?: return

        // Per-spawn movement settings; do not mutate the shared cache definition.
        val type = cached.copy(
            maxRange = 64,
            wanderRange = 0,
            defaultMode = NpcMode.None,
        ).also {
            it.paramMap = cached.paramMap
        }
        val npc = Npc(type, coords).apply {
            respawnDir = Direction.South
            faceDirection(Direction.South)
            movementLocked = true
            // These settings only drive the NPC's BossDSL tick, not player attack range/LOS.
            apRangeOverride = 64
            apRequiresLineOfSight = false
        }
        npcs.add(npc, Int.MAX_VALUE)
        npc.respawns = false
        check(instances.registerSessionNpc(player, npc))
        encounters.register(player, session, npc)
        npc.apPlayer2(player, npcInteractions)
    }

    private companion object {
        const val ROWING_MESSAGE =
            "The priestess rows you to Zulrah's shrine,<br>then hurriedly paddles away."
    }
}
