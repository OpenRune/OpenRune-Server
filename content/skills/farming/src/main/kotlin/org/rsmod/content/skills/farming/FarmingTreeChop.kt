package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.skills.woodcutting.scripts.TreeCutting
import org.rsmod.game.loc.BoundLocInfo

/**
 * Chops a checked, fully grown tree through the patch loc. Level, xp, logs and axe rates come from
 * the crop's wild tree; only the depletion rule is farming's own.
 */
@Singleton
class FarmingTreeChop
@Inject
constructor(private val cutting: TreeCutting, private val stumps: FarmingTreeStumps) {
    /** Returns false when the patch holds no choppable tree, so the caller can fall through. */
    suspend fun ProtectedAccess.chop(
        patch: PatchDef,
        bound: BoundLocInfo,
        state: PatchState,
    ): Boolean {
        val crop = state.crop ?: return false
        val grown = state.grown(crop) && state.phase == Phase.CHECKED
        if (crop.harvest != HarvestModel.CHOP || !grown) return false
        // UNVERIFIED: farmed tree woodcutting level/xp equal the wild tree (wiki description)
        val type = crop.woodLoc?.let { ServerCacheManager.getObject(it.asRSCM(RSCMType.LOC)) }
        if (type == null) return false

        if (!with(cutting) { canCut(type) }) return true
        // Same two-step start as wild trees: the first click only arms the delays and re-issues
        // the op; the second tick swings once, then the run loops here until it ends.
        if (actionDelay < mapClock) {
            actionDelay = mapClock + 3
            skillAnimDelay = mapClock + 3
            opLoc1(bound)
            return true
        }
        if (!with(cutting) { beginSwing(type) }) return true
        val rng = random
        var depleted = false
        while (!depleted) {
            val cut =
                with(cutting) {
                    cutTick(
                        tree = bound,
                        type = type,
                        depletes = { cutLogs -> cutLogs && rng.of(DEPLETE_ODDS) == 0 },
                        onDeplete = {
                            depleted = true
                            stumps.deplete(player, patch, state)
                            resetAnim()
                            soundSynth("synth.tree_fall_sound")
                        },
                    )
                }
            if (!cut || depleted) break
            delay(1)
        }
        return true
    }

    private companion object {
        // UNVERIFIED: depletion chance per log of 1/8 (wiki description)
        const val DEPLETE_ODDS = 8
    }
}
