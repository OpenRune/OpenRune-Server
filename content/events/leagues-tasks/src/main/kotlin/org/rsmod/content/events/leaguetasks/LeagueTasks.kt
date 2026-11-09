package org.rsmod.content.events.leaguetasks

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.enums.enum
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.midiJingle
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.table.ActionRow
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player

@Singleton
public class LeagueTasks @Inject constructor(private val eventBus: EventBus) {
    private val leagues: Map<Int, League> = League.loadAll()
    private val tierPoints: Map<Int, Int> =
        enum<Int, Int>("league_task_tier_points").filterValuesNotNull().backing
    private val tierNames: Map<Int, String> =
        enum<Int, String>("league_task_tier_names").filterValuesNotNull().backing

    public val allTasks: Set<Int> = leagues.values.flatMapTo(HashSet()) { it.tasks }

    public fun leagueOf(player: Player): League? = leagues[player.vars["varbit.league_type"]]

    public fun league(type: Int): League? = leagues[type]

    public fun isComplete(player: Player, index: Int): Boolean =
        LeagueTaskBits.isSet(player.vars[completedVarp(index)], index)

    public fun complete(player: Player, task: Int): Boolean {
        val league = leagueOf(player) ?: return false
        val index = league.indexOf(task) ?: return false
        if (isComplete(player, index)) {
            return false
        }
        val action = ActionRow.getRow(task)
        val tier = action.actionDifficulty ?: return false
        val points = tierPoints[tier] ?: 0

        writeBit(player, index, complete = true)
        val total = player.vars["varbit.league_total_tasks_completed"] + 1
        VarPlayerIntMapSetter.set(player, "varbit.league_total_tasks_completed", total)
        val earned = player.vars["varp.league_points_completed"] + points
        VarPlayerIntMapSetter.set(player, "varp.league_points_completed", earned)

        announce(player, league, action.actionName, tier, points, earned)
        player.softTimer("timer.league_points_claim", cycles = 1)
        eventBus.publish(LeagueTaskCompletedEvent(player, league, task, index, tier, points))
        return true
    }

    internal fun claimPoints(player: Player) {
        val earned = player.vars["varp.league_points_completed"]
        VarPlayerIntMapSetter.set(player, "varp.league_points_claimed", earned)
    }

    internal fun reset(player: Player, task: Int): Boolean {
        val league = leagueOf(player) ?: return false
        val index = league.indexOf(task) ?: return false
        if (!isComplete(player, index)) {
            return false
        }
        val points = ActionRow.getRow(task).actionDifficulty?.let(tierPoints::get) ?: 0
        writeBit(player, index, complete = false)
        val total = (player.vars["varbit.league_total_tasks_completed"] - 1).coerceAtLeast(0)
        VarPlayerIntMapSetter.set(player, "varbit.league_total_tasks_completed", total)
        val earned = (player.vars["varp.league_points_completed"] - points).coerceAtLeast(0)
        VarPlayerIntMapSetter.set(player, "varp.league_points_completed", earned)
        VarPlayerIntMapSetter.set(player, "varp.league_points_claimed", earned)
        return true
    }

    internal fun resetAll(player: Player) {
        for (varp in 0 until LeagueTaskBits.VARP_COUNT) {
            VarPlayerIntMapSetter.set(player, "varp.league_task_completed_$varp", 0)
        }
        VarPlayerIntMapSetter.set(player, "varbit.league_total_tasks_completed", 0)
        VarPlayerIntMapSetter.set(player, "varp.league_points_completed", 0)
        VarPlayerIntMapSetter.set(player, "varp.league_points_claimed", 0)
    }

    private fun announce(
        player: Player,
        league: League,
        name: String,
        tier: Int,
        points: Int,
        earned: Int,
    ) {
        val tierName = tierNames[tier]?.lowercase().orEmpty()
        val article = if (tierName.firstOrNull() in VOWELS) "an" else "a"
        player.mes(
            "Congratulations, you've completed $article $tierName task: " +
                "${league.messageColour}$name</col>."
        )
        ClientScripts.notificationDisplay(
            player,
            "League Task Complete!",
            "Task Completed: <col=ffffff>$name</col><br><br>Points Earned: <col=ffffff>$points</col>",
            league.notificationColour,
        )
        player.midiJingle("jingle.dt2_horn_part_6")
        updateProgressBar(player, earned)
    }

    internal fun updateProgressBar(
        player: Player,
        points: Int = player.vars["varp.league_points_completed"],
    ) {
        player.runClientScript(
            "clientscript.[clientscript,league_side_panel_update_bar]".asRSCM(RSCMType.CLIENTSCRIPT),
            "component.league_side_panel:fill".asRSCM(RSCMType.COMPONENT),
            points,
        )
    }

    private fun writeBit(player: Player, index: Int, complete: Boolean) {
        val varp = completedVarp(index)
        val packed = player.vars[varp]
        val updated =
            if (complete) LeagueTaskBits.set(packed, index) else LeagueTaskBits.clear(packed, index)
        VarPlayerIntMapSetter.set(player, varp, updated)
    }

    private fun completedVarp(index: Int): String =
        "varp.league_task_completed_${LeagueTaskBits.varp(index)}"

    private companion object {
        val VOWELS = setOf('a', 'e', 'i', 'o', 'u')
    }
}
