package org.rsmod.content.other.treasuretrails

import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.player.hook.LogBurnHook
import org.rsmod.api.player.hook.PrayerEnableHook
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.table.cluehelper.CluehelperClueSkillchallengeRow
import org.rsmod.game.entity.Player

/** Sherlock challenges that are completed by doing something rather than handing in an item. */
object ClueChallengeTasks {
    const val DONE = "varp.trail_challenge_done"

    internal const val BURN_YEW = "dbrow.cluehelper_skillchallenge_elite_18"
    internal const val CHIVALRY = "dbrow.cluehelper_skillchallenge_elite_22"
    internal const val DUST_DEVIL = "dbrow.cluehelper_skillchallenge_elite_10"

    private val actionSteps: Set<Int> by lazy {
        listOf(BURN_YEW, CHIVALRY, DUST_DEVIL)
            .map { CluehelperClueSkillchallengeRow.getRow(it).rowId }
            .toSet()
    }

    fun isAction(step: ClueStep): Boolean = step.rowId in actionSteps

    fun isDone(player: Player, step: ClueStep): Boolean = player.vars[DONE] == step.rowId

    fun complete(player: Player, task: String) {
        val rowId = CluehelperClueSkillchallengeRow.getRow(task).rowId
        val held =
            TrailFlow.heldTrails(player).any { it.step.kind == ClueKind.SkillChallenge && it.step.rowId == rowId }
        if (held) {
            VarPlayerIntMapSetter.set(player, DONE, rowId)
        }
    }

    fun clear(player: Player) {
        VarPlayerIntMapSetter.set(player, DONE, 0)
    }
}

class ClueChallengeBurnHook : LogBurnHook {
    override fun onBurn(player: Player, log: String) {
        if (log == YEW_LOGS) ClueChallengeTasks.complete(player, ClueChallengeTasks.BURN_YEW)
    }

    private companion object {
        const val YEW_LOGS = "obj.yew_logs"
    }
}

class ClueChallengePrayerHook : PrayerEnableHook {
    override fun onEnable(player: Player, prayerVarbit: String) {
        if (prayerVarbit == CHIVALRY_VARBIT) {
            ClueChallengeTasks.complete(player, ClueChallengeTasks.CHIVALRY)
        }
    }

    private companion object {
        const val CHIVALRY_VARBIT = "varbit.prayer_chivalry"
    }
}

class ClueChallengeKillHook : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        if (context.npc.type.name == DUST_DEVIL_NAME) {
            ClueChallengeTasks.complete(context.hero, ClueChallengeTasks.DUST_DEVIL)
        }
    }

    private companion object {
        const val DUST_DEVIL_NAME = "Dust devil"
    }
}
