package org.rsmod.content.skills.farming

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.table.SkillFeaturesRow
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * A chopped-down tree is a [Phase.STUMP] patch that regrows into its checked tree. The stump is
 * persisted in the patch varp and the regrowth countdown is a timer, which, unlike a queue, does
 * not hold up logout.
 */
@Singleton
class FarmingTreeStumps @Inject constructor(private val farming: FarmingService) {
    /** Turns the grown, checked tree on [patch] into its stump and schedules regrowth. */
    fun deplete(player: Player, patch: PatchDef, state: PatchState) {
        farming.store(player, patch, state.copy(phase = Phase.STUMP))
        // TODO: per-patch regrowth timing; one timer regrows every stump, which holds only while
        // no two tree patches can be chopped within the delay.
        player.clearSoftTimer(TIMER)
        player.softTimer(TIMER, REGROW_TICKS)
    }

    internal fun regrow(player: Player, patch: PatchDef) {
        val state = farming.stored(player, patch)
        if (state.phase == Phase.STUMP) farming.store(player, patch, state.copy(phase = Phase.CHECKED))
    }

    internal fun regrowAll(player: Player) {
        FarmingPatches.all.forEach { regrow(player, it) }
    }

    // TODO: how a stump saved at logout regrows is uncaptured; until then the countdown restarts.
    internal fun resume(player: Player) {
        if (FarmingPatches.all.any { farming.stored(player, it).phase == Phase.STUMP }) {
            player.softTimer(TIMER, REGROW_TICKS)
        }
    }

    internal companion object {
        const val TIMER = "timer.farming_tree_regrow"

        // UNVERIFIED: stump regrowth delay (plugin measurement)
        const val REGROW_TICKS = 16
    }
}

internal class FarmingTreeStumpsScript @Inject constructor(private val stumps: FarmingTreeStumps) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogin { stumps.resume(player) }
        onPlayerSoftTimer(FarmingTreeStumps.TIMER) {
            player.clearSoftTimer(FarmingTreeStumps.TIMER)
            stumps.regrowAll(player)
        }
    }
}

internal object FarmingTreeRoots {
    private const val FARMING_STAT = 21

    private val thresholds: Map<Int, List<Int>> by lazy {
        SkillFeaturesRow.all()
            .filter { it.skill.firstOrNull() == FARMING_STAT }
            .groupBy({ it.icon.id }, { it.skill[1] })
    }

    /** One root at the crop's level, plus one per cache tier the (boosted) [level] has reached. */
    fun count(roots: String, level: Int): Int =
        1 + thresholds[roots.asRSCM(RSCMType.OBJ)].orEmpty().count { it <= level }
}

internal enum class StumpDig {
    DONE,
    REGROWN,
    NO_ROOM,
    CHANGED,
}
