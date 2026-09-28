package org.rsmod.content.bosses.whisperer

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.math.sign
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceEnterTransition
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceNpc
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.instances.withInstanceEnterTransition
import org.rsmod.api.instances.withInstanceLeaveTransition
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.queueDeath
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenFullOverlay
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

class WhispererInstance
@Inject
constructor(
    registry: BossInstanceRegistry,
    private val locRepo: LocRepository,
    private val deps: BossDeps,
    private val aiPlayerInteractions: AiPlayerInteractions,
    private val collision: CollisionFlagMap,
    private val eventBus: EventBus,
) : InstanceScript(registry) {

    private val awakening: MutableSet<Npc> = Collections.newSetFromMap(IdentityHashMap())

    override fun settingsRow(): String = "dbrow.instance_whisperer"

    override fun area(): InstanceArea = INSTANCE

    override fun ScriptContext.configure() {
        onEnterPrelude { result, enter ->
            withInstanceEnterTransition(InstanceEnterTransition(), enter)
            if (result is InstanceManager.Result.Created) {
                placeBarriers(result.session)
            }
        }
        onEnterObject {
            if (!hasFragment(player) && inv.isFull()) {
                mes("You need a free inventory space to carry a blackstone fragment.")
                return@onEnterObject
            }
            defaultInstanceEntry()
        }
        onInstancePlayerJoin {
            giveFragment(player)
            startSanity(player)
        }
        onInstancePlayerLeave { stopSanity(player) }
        onPlayerSoftTimer(SANITY_TIMER) { tickSanity(player) }
        onPlayerSoftTimer(INSANITY_TIMER) { resolveInsanity(player) }
        onPlayerSoftTimer(FADE_TIMER) { player.disableSanityFade() }
        onExitObject { defaultLeaveFlow() }
        onOpHeld2(FRAGMENT_ACTIVE_OBJ) { switchRealm() }
        onOpHeld1(FRAGMENT_ACTIVE_OBJ) { switchRealm() }
        for (loc in listOf(ESCAPE_LOC, ESCAPE_SHADOW_LOC)) {
            onOpLoc1(loc) { leave() }
            onOpLoc2(loc) { leave() }
        }

        onOpNpc1(SPAWN_NPC) { awaken(it.npc) }
    }

    private fun hasFragment(player: Player): Boolean =
        FRAGMENT_OBJ in player.inv || FRAGMENT_ACTIVE_OBJ in player.inv

    private fun giveFragment(player: Player) {
        if (hasFragment(player)) return
        player.invAdd(player.inv, FRAGMENT_OBJ)
    }

    private fun startSanity(player: Player) {
        player.sanity = SANITY_MAX
        player.inShadowRealm = false
        player.canSwitchRealms = false
        player.ifOpenFullOverlay(SANITY_OVERLAY, eventBus)
        player.softTimer(SANITY_TIMER, SANITY_INTERVAL)
    }

    private fun stopSanity(player: Player) {
        player.clearSoftTimer(SANITY_TIMER)
        player.clearSoftTimer(INSANITY_TIMER)
        player.clearSoftTimer(FADE_TIMER)
        player.inShadowRealm = false
        player.canSwitchRealms = false
        player.ifCloseOverlay(SANITY_OVERLAY, eventBus)
    }

    private fun tickSanity(player: Player) {
        if (!player.inShadowRealm) {
            player.changeSanity(SANITY_RESTORE)
            return
        }
        player.changeSanity(-SHADOW_SANITY_DRAIN)
        player.statSub("stat.prayer", SHADOW_PRAYER_DRAIN, 0)
    }

    private fun resolveInsanity(player: Player) {
        player.clearSoftTimer(INSANITY_TIMER)
        if (player.inShadowRealm && player.sanity == 0) player.queueDeath()
    }

    private suspend fun ProtectedAccess.leave() {
        withInstanceLeaveTransition { defaultLeaveFlow() }
    }

    private suspend fun ProtectedAccess.switchRealm() {
        val session = manager.sessionForPlayer(player) ?: return
        val inShadow = manager.isShadowRealm(session, player.coords)
        if (!inShadow && !player.canSwitchRealms) {
            mes(NO_ENERGY_MESSAGE)
            return
        }
        val dx = if (inShadow) REALM_OFFSET_X else -REALM_OFFSET_X
        player.inShadowRealm = !inShadow
        if (inShadow) player.clearSoftTimer(INSANITY_TIMER)
        val dest = player.coords.translate(dx, 0)
        val bosses = bossesInInstance(session)
        bosses.forEach(Npc::resetFaceEntity)
        val target = player
        fadeTeleport(dest) {
            for (npc in bosses.filter { it.isSlotAssigned }) {
                npc.telejumpRealm(collision, dx)
            }
            deps.worldQueues.add(1) {
                bosses.filter { it.isSlotAssigned }.forEach { it.facePlayer(target) }
            }
        }
        if (inShadow && !player.canSwitchRealms) player.setFragmentActive(false)
    }

    private fun bossesInInstance(session: InstanceSession): List<Npc> {
        val formIds = BOSS_FORM_NPCS.map { it.asRSCM(RSCMType.NPC) }
        return manager.npcsForInstance(session.id).filter {
            it.isSlotAssigned && it.visType.id in formIds
        }
    }

    private suspend fun ProtectedAccess.awaken(npc: Npc) {
        if (!awakening.add(npc)) return
        anim(PICKUP_SEQ)
        delay(1)
        knockBack(npc)
        npc.anim(NPC_SPAWN_SEQ)
        val target = player
        deps.worldQueues.add(TRANSFORM_DELAY) { transform(npc, target) }
    }

    private fun ProtectedAccess.knockBack(npc: Npc) {
        val centre = npc.coords.translate(1, 1)
        val dx = (player.coords.x - centre.x).sign
        val dz = (player.coords.z - centre.z).sign
        val end = player.coords.translate(dx * KNOCKBACK_TILES, dz * KNOCKBACK_TILES)
        exactMove(player.coords, end, 0, KNOCKBACK_CYCLES, bearing(-dx, -dz))
        anim(FLYBACK_SEQ)
    }

    @OptIn(InternalApi::class)
    private fun transform(npc: Npc, target: Player) {
        awakening.remove(npc)
        if (!npc.isSlotAssigned) return
        val type = ServerCacheManager.getNpc(BOSS_NPC.asRSCM(RSCMType.NPC)) ?: return
        npc.resetAnim()
        npc.infoProtocol.setFaceAngle(NORTH, instant = true)
        npc.transmog(type, Int.MAX_VALUE)
        npc.copyStats(type)
        npc.assignUid()
        deps.suppressAttacks(npc, FIRST_VOLLEY_DELAY - WHISPERER_ATTACK_RATE)
        if (target.isValidTarget()) npc.apPlayer2(target, aiPlayerInteractions)
    }

    private fun placeBarriers(session: InstanceSession) {
        BARRIER_ANGLES.forEachIndexed { index, angle ->
            val template = CoordGrid(BARRIER_WEST_X + index, BARRIER_Z, 0)
            placeBarrier(session, template, ESCAPE_LOC, angle)
            placeBarrier(session, template.translate(-REALM_OFFSET_X, 0), ESCAPE_SHADOW_LOC, angle)
        }
    }

    private fun placeBarrier(session: InstanceSession, template: CoordGrid, loc: String, angle: LocAngle) {
        val coords = manager.resolveCoord(session, template) ?: return
        locRepo.add(coords, loc, Int.MAX_VALUE, angle, LocShape.CentrepieceStraight)
    }

    private fun bearing(dx: Int, dz: Int): Int =
        when {
            dx == 0 && dz < 0 -> SOUTH
            dx < 0 && dz < 0 -> SOUTH_WEST
            dx < 0 && dz == 0 -> WEST
            dx < 0 && dz > 0 -> NORTH_WEST
            dx == 0 && dz > 0 -> NORTH
            dx > 0 && dz > 0 -> NORTH_EAST
            dx > 0 && dz == 0 -> EAST
            else -> SOUTH_EAST
        }

    private companion object {
        private const val ARENA_REGION = 10595
        private const val SHADOW_ARENA_REGION = 9571
        private const val SPAWN_NPC = "npc.whisperer_spawn"
        private const val BOSS_NPC = "npc.whisperer"
        private val BOSS_FORM_NPCS = listOf(BOSS_NPC, "npc.whisperer_melee")
        private const val ESCAPE_LOC = "loc.whisperer_escape"
        private const val ESCAPE_SHADOW_LOC = "loc.whisperer_escape_shadow"
        private const val BARRIER_WEST_X = 2652
        private const val BARRIER_Z = 6384
        private const val SANITY_OVERLAY = "interface.sanity"
        private const val SANITY_INTERVAL = 4
        private const val SHADOW_SANITY_DRAIN = 3
        private const val SHADOW_PRAYER_DRAIN = 3
        private const val SANITY_RESTORE = 1
        private const val NO_ENERGY_MESSAGE = "Your blackstone fragment has no energy."

        private const val PICKUP_SEQ = "seq.human_pickupfloor"
        private const val FLYBACK_SEQ = "seq.human_troll_flyback_merge"
        private const val NPC_SPAWN_SEQ = "seq.npc_whisperer_01_spawn_01"

        private const val KNOCKBACK_TILES = 3
        private const val KNOCKBACK_CYCLES = 60
        private const val TRANSFORM_DELAY = 4
        private const val FIRST_VOLLEY_DELAY = 4

        private const val SOUTH = 0
        private const val SOUTH_WEST = 256
        private const val WEST = 512
        private const val NORTH_WEST = 768
        private const val NORTH = 1024
        private const val NORTH_EAST = 1280
        private const val EAST = 1536
        private const val SOUTH_EAST = 1792

        private val BARRIER_ANGLES =
            listOf(
                LocAngle.West,
                LocAngle.North,
                LocAngle.West,
                LocAngle.East,
                LocAngle.South,
                LocAngle.North,
                LocAngle.East,
                LocAngle.West,
                LocAngle.South,
            )

        private val INSTANCE =
            InstanceArea.copyRegions(
                regionIds = listOf(SHADOW_ARENA_REGION, ARENA_REGION),
                npcSpawns = listOf(InstanceNpc(SPAWN_NPC, CoordGrid(2655, 6368, 0))),
            )
    }
}
