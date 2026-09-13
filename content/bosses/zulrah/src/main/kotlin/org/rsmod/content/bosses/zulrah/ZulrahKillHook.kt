package org.rsmod.content.bosses.zulrah

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.generic.killcount.BossRecords

internal class ZulrahKillHook @Inject constructor(
    private val controller: ZulrahEncounterController,
) : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        val player = context.hero
        val ticks = controller.claimCompletion(context.npc, player) ?: return
        val varp = KILLCOUNT.asRSCM(RSCMType.VARP)
        val count = (player.vars[KILLCOUNT].toLong() + 1).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        VarPlayerIntMapSetter.set(player, KILLCOUNT, count)
        val newBest = BossRecords.recordBest(player, varp, ticks)
        val precise = player.vars["varbit.option_precise_timing"] == 1
        val separateHours = player.vars["varbit.option_separate_hours"] == 1
        val duration = BossRecords.formatTime(ticks, precise, separateHours)
        val best = BossRecords.formatTime(BossRecords.bestTicks(player, varp), precise, separateHours)
        player.mes("Your Zulrah kill count is: <col=ff0000>$count</col>.")
        player.mes(if (newBest) "Fight duration: <col=ff0000>$duration</col> (new personal best)"
            else "Fight duration: <col=ff0000>$duration</col>. Personal best: $best")
    }

    companion object {
        const val KILLCOUNT = "varp.total_snakeboss_kills"
    }
}
