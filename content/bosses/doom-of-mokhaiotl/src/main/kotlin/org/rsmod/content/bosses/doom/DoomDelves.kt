package org.rsmod.content.bosses.doom

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.bossbar.plugin.BossHpBarScript
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.startEncounter
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape

private var Player.currentLevelVarp by intVarp("varp.dom_current_level_temp")

@Singleton
internal class DoomDelves
@Inject
constructor(
    private val deps: BossDeps,
    private val specs: DoomSpecs,
    private val instances: InstanceManager,
    private val hpBar: BossHpBarScript,
    private val aiPlayerInteractions: AiPlayerInteractions,
    private val acid: DoomAcid,
    private val stats: DoomStats,
) {
    fun currentLevel(player: Player): Int = player.vars[CURRENT_LEVEL_VARP] + 1

    fun setLevel(player: Player, level: Int) {
        player.currentLevelVarp = level - 1
    }

    fun resetLevel(player: Player) {
        player.currentLevelVarp = 0
    }

    fun startLevel(access: ProtectedAccess, session: InstanceSession, level: Int) {
        val player = access.player
        access.vars[CURRENT_LEVEL_VARP] = level - 1
        access.mes("@mes_hl_red@Delve level: $level</col>")
        if (DoomDelve.of(level).freshArena) clearArena(player, session)
        deps.worldQueues.add(SPAWN_DELAY) {
            if (instances.sessionForPlayer(player)?.id != session.id) return@add
            spawn(player, session, DoomDelve.of(level))
        }
    }

    private fun clearArena(player: Player, session: InstanceSession) {
        acid.wipe(player)
        val spawn = instances.resolveCoord(session, DoomArena.BOSS_SPAWN) ?: return
        val rock = ROCK_LOC.asRSCM(RSCMType.LOC)
        for (dx in DoomArena.FLOOR_MIN_X..DoomArena.FLOOR_MAX_X) {
            for (dz in DoomArena.FLOOR_MIN_Z..DoomArena.FLOOR_MAX_Z) {
                deps.locRepo.findExact(spawn.translate(dx, dz), LocShape.CentrepieceStraight)
                    ?.takeIf { it.id == rock }
                    ?.let { deps.locRepo.del(it, Int.MAX_VALUE) }
            }
        }
    }

    fun placeExit(session: InstanceSession) {
        val coords = instances.resolveCoord(session, DoomArena.GAP) ?: return
        deps.locRepo.add(coords, EXIT_LOC, Int.MAX_VALUE, LocAngle.North, LocShape.CentrepieceStraight)
    }

    fun removeHoles(session: InstanceSession) {
        val spawn = instances.resolveCoord(session, DoomArena.BOSS_SPAWN) ?: return
        val holes = setOf(HOLE_LOC.asRSCM(RSCMType.LOC), UNIQUE_HOLE_LOC.asRSCM(RSCMType.LOC))
        for (dx in DoomArena.FLOOR_MIN_X..DoomArena.FLOOR_MAX_X) {
            for (dz in DoomArena.FLOOR_MIN_Z..DoomArena.FLOOR_MAX_Z) {
                deps.locRepo.findExact(spawn.translate(dx, dz), LocShape.CentrepieceStraight)
                    ?.takeIf { it.id in holes }
                    ?.let { deps.locRepo.del(it, Int.MAX_VALUE) }
            }
        }
    }

    private fun spawn(player: Player, session: InstanceSession, delve: DoomDelve) {
        val coords = instances.resolveCoord(session, DoomArena.BOSS_SPAWN) ?: return
        val type = ServerCacheManager.getNpc(DoomNpcs.BOSS.asRSCM(RSCMType.NPC)) ?: return
        val npc = Npc(type, coords)
        npc.baseHitpointsLvl = delve.hitpoints
        npc.hitpoints = delve.hitpoints
        npc.apRequiresLineOfSight = false
        if (delve.hasBurrow) npc.apRangeOverride = DoomArena.ATTACK_RANGE
        deps.npcRepo.add(npc, Int.MAX_VALUE)
        instances.registerSessionNpc(player, npc)
        deps.startEncounter(npc, specs.of(delve))
        acid.attach(player, npc, delve)
        stats.startLevel(player)

        npc.anim(EMERGE_SEQ)
        npc.spotanim(EMERGE_SPOTANIM)
        deps.suppressAttacks(npc, FIRST_ATTACK_DELAY)
        hpBar.onOpen(player, npc)
        npc.apPlayer2(player, aiPlayerInteractions)
    }

    suspend fun onDeath(access: StandardNpcAccess, unique: Boolean) {
        val npc = access.npc
        access.noneMode()
        access.hideAllOps()
        access.anim(DESPAWN_SEQ)
        access.spotanim(DESPAWN_SPOTANIM)
        access.delay(HOLE_DELAY)
        val hole = deps.locRepo.add(npc.coords, if (unique) UNIQUE_HOLE_LOC else HOLE_LOC, Int.MAX_VALUE, LocAngle.West, LocShape.CentrepieceStraight)
        acid.forget(npc, npc.coords)
        deps.worldRepo.locAnim(hole, HOLE_SPAWN_SEQ)
        access.delay(DESPAWN_DELAY - HOLE_DELAY)
        deps.npcRepo.del(npc, Int.MAX_VALUE)
    }

    private companion object {
        private const val CURRENT_LEVEL_VARP = "varp.dom_current_level_temp"
        private const val EXIT_LOC = "loc.dom_entrance_exit"
        private const val HOLE_LOC = "loc.dom_descend_hole"
        private const val UNIQUE_HOLE_LOC = "loc.dom_descend_hole_unique"
        private const val ROCK_LOC = "loc.dom_rock"

        private const val SPAWN_DELAY = 4
        private const val FIRST_ATTACK_DELAY = 6
        private const val EMERGE_SEQ = "seq.dom_burrowed_emerge"
        private const val EMERGE_SPOTANIM = "spotanim.vfx_dom_burrowed_emerge"

        private const val DESPAWN_SEQ = "seq.dom_despawn"
        private const val DESPAWN_SPOTANIM = "spotanim.vfx_dom_despawn"
        private const val HOLE_SPAWN_SEQ = "seq.vfx_doom_hole_spawn"
        private const val HOLE_DELAY = 5
        private const val DESPAWN_DELAY = 7
    }
}
