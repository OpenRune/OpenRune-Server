package org.rsmod.content.events.leaguetasks

import dev.or2.central.account.Rights
import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.table.ActionRow
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class LeagueTasksScript @Inject constructor(private val tasks: LeagueTasks) : PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    override fun ScriptContext.startup() {
        tasks.load()

        onPlayerLogin { tasks.claimPoints(player) }

        onPlayerSoftTimer("timer.league_points_claim") {
            player.clearSoftTimer("timer.league_points_claim")
            tasks.claimPoints(player)
        }

        onCommand("leaguetask") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Complete, reset or inspect a league task: ::leaguetask <index> [complete|reset]"
            invalidArgs = "Use: ::leaguetask <index> [complete|reset]"
            cheat(::leagueTask)
        }

        onCommand("leaguetasks") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Reset every league task, total and point: ::leaguetasks reset"
            cheat {
                if (args.firstOrNull() != "reset") {
                    player.mes("Use: ::leaguetasks reset")
                    return@cheat
                }
                tasks.resetAll(player)
                player.mes("Every league task has been reset.")
            }
        }
    }

    private fun leagueTask(cheat: Cheat) {
        val player = cheat.player
        val league = tasks.leagueOf(player)
        if (league == null) {
            player.mes("You are not in a league with tasks.")
            return
        }
        val index = cheat.args.firstOrNull()?.toIntOrNull()
        val task = index?.let(league::rowAt)
        if (task == null) {
            player.mes("There is no task at that index (0 to ${league.size - 1}).")
            return
        }
        val name = ActionRow.getRow(task).actionName
        when (cheat.args.getOrNull(1)?.lowercase()) {
            "complete" -> if (!tasks.complete(player, task)) player.mes("$name is already complete.")
            "reset" ->
                if (tasks.reset(player, task)) {
                    player.mes("Reset $name.")
                } else {
                    player.mes("$name is not complete.")
                }
            else -> {
                val state = if (tasks.isComplete(player, index)) "complete" else "not complete"
                player.mes("Task $index, $name: $state.")
            }
        }
    }
}
