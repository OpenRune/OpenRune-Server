package org.rsmod.api.death

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.vars.typePlayerUidVarn
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.player.vars.typeNpcUidVarp
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.npc.NpcUid
import org.rsmod.map.CoordGrid

@Singleton
public class NpcDeath
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val players: PlayerList,
    private val objRepo: ObjRepository,
    private val deathDropHooks: Set<NpcDeathDropHook>,
    private val deathKillHooks: Set<NpcDeathKillHook>,
) {
    public suspend fun deathNoDrops(access: StandardNpcAccess) {
        access.death(npcRepo, players)
    }

    public suspend fun deathWithDrops(
        access: StandardNpcAccess,
        dropCoords: CoordGrid = access.coords,
    ) {
        access.death(npcRepo, players)
        access.npc.spawnDeathDrops(dropCoords)
    }

    private fun Npc.spawnDeathDrops(dropCoords: CoordGrid) {
        // TODO: Drop tables.
        val hero = findHero(players)
        if (hero != null) {
            val duration = hero.lootDropDuration ?: constants.lootdrop_duration

            val droppedRemains = paramOrNull(params.dropped_remains)?: ServerCacheManager.getItem("obj.bones".asRSCM())?: error("No bones")
            val ctx = NpcDeathDropContext(
                hero = hero,
                dropType = droppedRemains,
                dropCoords = dropCoords,
                duration = duration,
                objRepo = objRepo
            )

            var dropConsumed = false
            for (hook in deathDropHooks) {
                if (hook.tryConsume(ctx)) {
                    dropConsumed = true
                    break
                }
            }
            if (!dropConsumed) {
                objRepo.add(droppedRemains, dropCoords, duration, hero)
            }

            val killCtx = NpcDeathKillContext(hero = hero, npc = this)
            for (hook in deathKillHooks) {
                hook.onKill(killCtx)
            }
        }
    }

    // Note: We may be able to have `Npc` as the arg instead of `StandardNpcAccess`, however we
    // will need to wait and see how [spawnDeathDrops] ends up once it handles everything it needs
    // to.
    public fun spawnDrops(access: StandardNpcAccess, dropCoords: CoordGrid = access.coords) {
        access.npc.spawnDeathDrops(dropCoords)
    }
}

private var Player.lastCombat: Int by intVarp("varp.lastcombat")
private var Player.aggressiveNpc: NpcUid? by typeNpcUidVarp("varp.aggressive_npc")
private var Npc.aggressivePlayer by typePlayerUidVarn("varn.aggressive_player")

/**
 * Handles the death sequence of this [StandardNpcAccess.npc], including clearing interactions and
 * removing (or hiding, if it respawns) the npc from the world.
 *
 * **Notes:**
 * - This is **not** the way to "kill" a npc. This "death sequence" occurs after the npc has already
 *   been deemed dead and its death queue is being processed.
 * - To queue a npc's death, use [StandardNpcAccess.queueDeath] or [org.rsmod.api.npc.queueDeath]
 *   instead.
 * - This function **does not** spawn any drop table objs for the npc.
 * - Drop table spawns are handled via [NpcDeath.deathWithDrops], which is **automatically called**
 *   for queued deaths by default. However, if you override death queues for specific npc types
 *   (`onNpcQueue(npc_type, queues.death)`), you must explicitly handle drop spawns in the script by
 *   injecting `NpcDeath` and calling either [NpcDeath.deathWithDrops] or [NpcDeath.spawnDrops].
 */
public suspend fun StandardNpcAccess.death(npcRepo: NpcRepository, players: PlayerList) {
    walk(coords)
    noneMode()
    hideAllOps()
    arriveDelay()

    val aggressivePlayer = npc.aggressivePlayer
    if (aggressivePlayer != null) {
        val player = aggressivePlayer.resolve(players)

        val deathSound = paramOrNull(params.death_sound)
        if (deathSound != null && player != null) {
            player.soundSynth(deathSound)
        }

        // TODO(combat): Should we assert that npc.uid will always match player.aggressiveNpc at
        // this point?

        if (player != null && player.aggressiveNpc == npc.uid) {
            player.lastCombat = 0
        }
    }

    val deathAnim = param(params.death_anim)
    anim(RSCM.getReverseMapping(RSCMType.SEQ,deathAnim.id))
    delay(deathAnim)

    if (npc.respawns) {
        npcRepo.despawn(npc, npc.type.respawnRate)
        return
    }

    npcRepo.del(npc, Int.MAX_VALUE)
}
