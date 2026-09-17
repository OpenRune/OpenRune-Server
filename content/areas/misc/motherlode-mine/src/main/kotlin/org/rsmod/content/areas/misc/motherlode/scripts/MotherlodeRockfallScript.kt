package org.rsmod.content.areas.misc.motherlode.scripts

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import kotlin.random.Random
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeAnim
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.game.proj.ProjAnim
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.game.type.getInvObj
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

class MotherlodeRockfallScript
@Inject
constructor(
    private val locRepo: LocRepository,
    private val worldRepo: WorldRepository,
    private val worldQueues: WorldQueueList,
    private val playerList: PlayerList,
    private val collision: CollisionFlagMap,
    private val xpMods: XpModifiers,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.motherlode_rockfall_1") { mineRockfall(it.loc) }
        onOpLoc1("loc.motherlode_rockfall_2") { mineRockfall(it.loc) }
    }

    private suspend fun ProtectedAccess.mineRockfall(rock: BoundLocInfo) {
        val pickaxe = Mining.findPickaxe(player)
        if (pickaxe == null) {
            mes("You need a pickaxe to mine this rock.")
            return
        }
        anim(RSCM.getReverseMapping(RSCMType.SEQ, getInvObj(pickaxe).pickaxeAnim.id))
        delay(MINE_CYCLES)
        resetAnim()

        val loc = LocInfo(rock.layer, rock.coords, rock.entity)
        if (!locRepo.del(loc, Int.MAX_VALUE)) {
            return
        }
        statAdvance("stat.mining", ROCKFALL_XP * xpMods.get(player, "stat.mining"))
        worldQueues.add(Random.nextInt(RESPAWN_CYCLES.first, RESPAWN_CYCLES.last + 1)) {
            collapse(loc)
        }
    }

    private fun collapse(loc: LocInfo) {
        val target = loc.coords
        val rockfall = ROCKFALL_SPOTANIM.asRSCM(RSCMType.SPOTANIM)
        for ((startTime, startHeight) in listOf(0 to 1000, 10 to 900)) {
            val source = target.translate(Random.nextInt(-2, 3), Random.nextInt(-2, 3))
            worldRepo.projAnim(
                ProjAnim(
                    spotanim = rockfall,
                    startHeight = startHeight,
                    endHeight = END_HEIGHT,
                    startTime = startTime,
                    endTime = FALL_CLIENT_CYCLES,
                    angle = 0,
                    progress = 0,
                    sourceIndex = 0,
                    targetIndex = 0,
                    startCoord = source,
                    endCoord = target,
                ),
            )
        }
        worldRepo.spotanimMap(SpotanimType(SPLASH_SPOTANIM.asRSCM(RSCMType.SPOTANIM)), target, delay = FALL_CLIENT_CYCLES)
        worldQueues.add(1) {
            locRepo.add(loc, Int.MAX_VALUE)
            crushPlayersUnder(target)
        }
    }

    private fun crushPlayersUnder(target: CoordGrid) {
        for (player in playerList) {
            if (player.coords != target) {
                continue
            }
            val damage = player.hitpoints * CRUSH_PERCENT / 100 + 1
            player.queueHit(delay = 1, type = HitType.Typeless, damage = damage, modifier = NoopPlayerHitModifier)
            freeTileAround(target)?.let { PathingEntityCommon.telejump(player, collision, it) }
        }
    }

    private fun freeTileAround(target: CoordGrid): CoordGrid? =
        NEIGHBOURS.map { (dx, dz) -> target.translate(dx, dz) }
            .shuffled()
            .firstOrNull { !collision.isWalkBlocked(it) }

    private companion object {
        const val MINE_CYCLES = 2
        const val ROCKFALL_XP = 10.0
        const val CRUSH_PERCENT = 4
        const val END_HEIGHT = 25
        const val FALL_CLIENT_CYCLES = 30
        const val ROCKFALL_SPOTANIM = "spotanim.motherlode_rockfall"
        const val SPLASH_SPOTANIM = "spotanim.castlewars_catapult_splash"

        val RESPAWN_CYCLES = 80..100
        val NEIGHBOURS = listOf(0 to 1, 1 to 0, 0 to -1, -1 to 0)
    }
}
