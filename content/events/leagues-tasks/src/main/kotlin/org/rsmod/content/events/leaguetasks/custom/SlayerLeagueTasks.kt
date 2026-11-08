package org.rsmod.content.events.leaguetasks.custom

import jakarta.inject.Inject
import org.rsmod.api.config.refs.BaseParams
import org.rsmod.api.script.onNpcKilled
import org.rsmod.api.script.onPlayerSlayerKillPrepare
import org.rsmod.api.script.onPlayerSlayerTaskComplete
import org.rsmod.content.events.leaguetasks.LeagueTasks
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SlayerLeagueTasks @Inject constructor(private val tasks: LeagueTasks) : PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    private val firstTask = task("league_task_complete_1_slayer_task")
    private val manyTasks = task("league_task_complete_200_slayer_tasks")
    private val manyKills = task("league_task_slay_250_creatures")
    private val abyssalTask = task("league_task_slay_an_abyssal_demon")
    private val abyssalDemons =
        npcs(
            "npc.slayer_abyssal",
            "npc.slayer_abyssal_strongholdcave",
            "npc.kourend_abyssal",
            "npc.wild_cave_abyssal",
        )
    private val superiorTasks =
        listOf(
            1 to task("league_task_defeat_a_superior_slayer_creature"),
            10 to task("league_task_defeat_10_superior_slayer_creatures"),
            20 to task("league_task_defeat_20_superior_slayer_creatures"),
            25 to task("league_task_25_superior_slayer_encounters"),
            50 to task("league_task_defeat_50_superior_slayer_creatures"),
            75 to task("league_task_75_superior_slayer_encounters"),
            100 to task("league_task_100_superior_slayer_encounters"),
        )

    override fun ScriptContext.startup() {
        onPlayerSlayerTaskComplete {
            tasks.complete(player, firstTask)
            if (player.advance(TASKS_DONE, 200) >= 200) {
                tasks.complete(player, manyTasks)
            }
        }
        onPlayerSlayerKillPrepare {
            if (npc.type.id in abyssalDemons || npc.visType.id in abyssalDemons) {
                tasks.complete(player, abyssalTask)
            }
            if (player.advance(TASK_KILLS, 250) >= 250) {
                tasks.complete(player, manyKills)
            }
        }
        onNpcKilled {
            if (!isHero || !npc.visType.hasParam(BaseParams.slayer_superior_form)) {
                return@onNpcKilled
            }
            val kills = player.advance(SUPERIOR_KILLS, 100)
            for ((needed, task) in superiorTasks) {
                if (kills >= needed) {
                    tasks.complete(player, task)
                }
            }
        }
    }

    private companion object {
        const val TASKS_DONE = "varbit.league_counter_complete_slayer_tasks"
        const val TASK_KILLS = "varbit.league_counter_slay_task_creatures"
        const val SUPERIOR_KILLS = "varbit.league_counter_kill_superiors"
    }
}
