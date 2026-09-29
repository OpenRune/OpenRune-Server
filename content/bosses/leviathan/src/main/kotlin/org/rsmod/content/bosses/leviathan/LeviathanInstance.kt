package org.rsmod.content.bosses.leviathan

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceEnterTransition
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.instances.withInstanceLeaveTransition
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class LeviathanInstance
@Inject
internal constructor(registry: BossInstanceRegistry, private val fights: LeviathanFights) :
    InstanceScript(registry) {

    override fun settingsRow(): String = "dbrow.instance_leviathan"

    override fun area(): InstanceArea = INSTANCE

    override fun ScriptContext.configure() {
        onEnterTransition(InstanceEnterTransition(message = ROW_MESSAGE))
        onExitObject { withInstanceLeaveTransition(InstanceEnterTransition(message = ROW_BACK_MESSAGE)) { defaultLeaveFlow() } }
        onOpLoc2(LeviathanArenaLocs.BOAT_ESCAPE_LOC) { defaultLeaveFlow() }
        onOpLoc2(LeviathanArenaLocs.BOAT_LEAVE_LOC) {
            withInstanceLeaveTransition(InstanceEnterTransition(message = ROW_BACK_MESSAGE)) { defaultLeaveFlow() }
        }

        onInstancePlayerJoin {
            val session = manager.sessionForId(instanceId) ?: return@onInstancePlayerJoin
            fights.placeArenaLocs(session)
            fights.ensureTails(session)
        }

        onOpLoc1(LeviathanArenaLocs.HANDHOLDS_ENTER_LOC) { climb(it.loc.coords.x) }
        onOpLoc1(LeviathanArenaLocs.HANDHOLDS_EXIT_LOC) { climb(it.loc.coords.x) }
    }

    private suspend fun ProtectedAccess.climb(handholdsX: Int) {
        val session = manager.sessionForPlayer(player) ?: return
        val climbingIn = player.coords.x < handholdsX
        anim(CLIMB_SEQ)
        delay(1)
        telejump(player.coords.translate(if (climbingIn) CLIMB_DISTANCE else -CLIMB_DISTANCE, 0))

        if (!climbingIn) {
            fights.setHandholds(session, LeviathanArenaLocs.HANDHOLDS_ENTER_LOC)
            return
        }
        mes(HANDHOLDS_MESSAGE, ChatType.Spam)
        if (!fights.hasBoss(session)) {
            fights.beginEncounter(session, player, SPAWN_DELAY)
        }
    }

    private companion object {
        private const val ARENA_REGION = 8291
        private const val CLIMB_SEQ = "seq.human_reachforladder"
        private const val CLIMB_DISTANCE = 2
        private const val SPAWN_DELAY = 5
        private const val ROW_MESSAGE = "You row out to the island..."
        private const val ROW_BACK_MESSAGE = "You row back to the camp..."
        private const val HANDHOLDS_MESSAGE = "The brain holds fall away as you use them."

        private val INSTANCE = InstanceArea.copyRegions(regionIds = listOf(ARENA_REGION))
    }
}

@Singleton
internal class LeviathanArenaLocs
@Inject
constructor(
    private val deps: BossDeps,
    private val instances: InstanceManager,
    private val locRepo: LocRepository,
) {
    private val sessionTails: MutableMap<Long, List<Npc>> = HashMap()

    fun arenaPlayers(fight: LeviathanFight): List<Player> {
        val instanceId = instances.instanceForNpc(fight.npc) ?: return emptyList()
        return deps.playerList.filter {
            it.isValidTarget() &&
                instances.sessionForPlayer(it)?.id == instanceId &&
                fight.arena.inSearchBox(it.coords) &&
                !isOnIsland(fight.arena, it.coords)
        }
    }

    fun isOnIsland(arena: Arena, coords: CoordGrid): Boolean =
        arena.toStatic(coords).x < LeviathanArena.HANDHOLDS_INSIDE.x

    fun freeArenaTiles(fight: LeviathanFight): List<CoordGrid> {
        val sw = fight.arena.at(LeviathanArena.SEARCH_SW)
        val ne = fight.arena.at(LeviathanArena.SEARCH_NE)
        return buildList {
            for (x in sw.x..ne.x) {
                for (z in sw.z..ne.z) {
                    val tile = CoordGrid(x, z, sw.level)
                    if (isFreeTile(fight, tile)) add(tile)
                }
            }
        }
    }

    private fun isFreeTile(fight: LeviathanFight, tile: CoordGrid): Boolean =
        fight.arena.inSearchBox(tile) &&
            !isOnIsland(fight.arena, tile) &&
            !deps.encounter(fight.npc).ownsLocAt(tile) &&
            !deps.collision.isWalkBlocked(tile)

    fun animateTails(session: InstanceSession) {
        sessionTails[session.id.value]
            ?.filter { it.isSlotAssigned }
            ?.forEach { it.anim(deps.random.pick(TAIL_STUN_SEQS)) }
    }

    fun placeArenaLocs(session: InstanceSession, bossActive: Boolean) {
        setHandholds(session, if (bossActive) HANDHOLDS_SEALED_LOC else HANDHOLDS_ENTER_LOC)
        setBoat(session, escape = bossActive)
        instances.resolveCoord(session, LeviathanArena.HANDHOLDS_NOOP)?.let {
            locRepo.add(it, HANDHOLDS_NOOP_LOC, Int.MAX_VALUE, LocAngle.East, LocShape.CentrepieceStraight)
        }
    }

    fun setHandholds(session: InstanceSession, loc: String) {
        val coords = instances.resolveCoord(session, LeviathanArena.HANDHOLDS) ?: return
        locRepo.add(coords, loc, Int.MAX_VALUE, LocAngle.West, LocShape.CentrepieceStraight)
    }

    fun setBoat(session: InstanceSession, escape: Boolean) {
        val coords = instances.resolveCoord(session, LeviathanArena.ISLAND_BOAT) ?: return
        val loc = if (escape) BOAT_ESCAPE_LOC else BOAT_LEAVE_LOC
        locRepo.add(coords, loc, Int.MAX_VALUE, LocAngle.South, LocShape.CentrepieceStraight)
    }

    fun ensureTails(session: InstanceSession) {
        if (sessionTails[session.id.value]?.all { it.isSlotAssigned } == true) return
        sessionTails[session.id.value]?.forEach { if (it.isSlotAssigned) deps.npcRepo.del(it, Int.MAX_VALUE) }
        sessionTails[session.id.value] =
            LeviathanArena.TAILS.mapNotNull { tail ->
                val coords = instances.resolveCoord(session, tail.coords) ?: return@mapNotNull null
                val faces = instances.resolveCoord(session, tail.faces) ?: return@mapNotNull null
                val type = ServerCacheManager.getNpc(tail.npc.asRSCM(RSCMType.NPC)) ?: return@mapNotNull null
                val npc = Npc(type, coords)
                npc.mode = NpcMode.None
                deps.npcRepo.add(npc, Int.MAX_VALUE)
                instances.attachNpc(session.id, npc)
                npc.faceSquare(faces)
                npc
            }
    }

    companion object {
        const val HANDHOLDS_ENTER_LOC = "loc.leviathan_wall_climb"
        const val HANDHOLDS_EXIT_LOC = "loc.leviathan_wall_climb_quest_exit"
        const val HANDHOLDS_SEALED_LOC = "loc.dt2_scar_wallkit01_short"
        const val HANDHOLDS_NOOP_LOC = "loc.leviathan_wall_climb_noop"
        const val BOAT_ESCAPE_LOC = "loc.dt2_scar_boat_island_escape"
        const val BOAT_LEAVE_LOC = "loc.dt2_scar_boat_island_leave"

        private val TAIL_STUN_SEQS =
            listOf(
                "seq.npc_leviathan_tail01_stun01",
                "seq.npc_leviathan_tail01_stunvariant01",
                "seq.npc_leviathan_tail01_stunvariant02",
            )
    }
}
