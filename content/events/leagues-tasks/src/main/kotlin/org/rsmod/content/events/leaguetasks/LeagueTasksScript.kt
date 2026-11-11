package org.rsmod.content.events.leaguetasks

import com.github.michaelbull.logging.InlineLogger
import dev.or2.central.account.Rights
import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onGameStartup
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.table.ActionRow
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class LeagueTasksScript
@Inject
constructor(
    private val tasks: LeagueTasks,
    private val progress: LeagueTaskProgress,
    private val definitions: LeagueTaskDefinitions,
) : PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    private val logger = InlineLogger()

    override fun ScriptContext.startup() {
        onGameStartup { logCoverage() }

        onCommand("reloadleaguetasks") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Re-read the league task TOML from the build output"
            cheat {
                try {
                    progress.reload()
                    player.mes("League tasks reloaded.")
                } catch (e: Exception) {
                    player.mes("League tasks not reloaded: ${e.message}")
                }
            }
        }

        onPlayerLogin {
            tasks.claimPoints(player)
            player.softTimer("timer.league_task_sync", cycles = 1)
        }

        onPlayerSoftTimer("timer.league_task_sync") {
            player.clearSoftTimer("timer.league_task_sync")
            progress.checkState(player)
        }

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

    private fun logCoverage() {
        for (set in definitions.active) {
            val all = set.league.tasks
            val custom = tasks.claimedTasks.filterTo(HashSet()) { it in all } - set.defined
            val neither = all - set.defined - set.fromAction - custom
            logger.info {
                "League ${set.key} (type ${set.league.type}): ${all.size} tasks, " +
                    "${set.defined.size} from task files, ${set.fromAction.size} from action rows, " +
                    "${custom.size} custom, ${neither.size} with no definition."
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
