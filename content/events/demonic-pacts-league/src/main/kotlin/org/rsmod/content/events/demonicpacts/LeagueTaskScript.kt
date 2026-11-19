package org.rsmod.content.events.demonicpacts

import org.rsmod.api.player.events.prayer.PrayerSkillAction
import org.rsmod.api.player.events.skilling.SkillingActionCompleteEvent
import org.rsmod.api.player.events.skilling.SkillingActionContext
import org.rsmod.api.player.stat.SkillActivity
import org.rsmod.api.script.onEvent
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Feeds finished skilling actions into the league task list. */
class LeagueTaskScript : PluginScript() {
    override val worldTypes = listOf(WorldType.DEMONIC_PACTS_LEAGUE)

    override fun ScriptContext.startup() {
        val previous = SkillActivity.listener
        SkillActivity.listener = { player, action, subjects, count ->
            previous(player, action, subjects, count)
            if (player.isDemonicPactsLeague) {
                LeagueTasks.onActivity(player, action, subjects, count)
            }
        }
        onEvent<SkillingActionCompleteEvent> {
            if (!player.isDemonicPactsLeague) {
                return@onEvent
            }
            when (val context = context) {
                is SkillingActionContext.Product ->
                    LeagueTasks.onActivity(
                        player,
                        GATHER,
                        listOf(context.item),
                        context.count,
                        stat = context.skill,
                    )
                is SkillingActionContext.Prayer -> {
                    val bury = context.action as? PrayerSkillAction.BuryComplete ?: return@onEvent
                    LeagueTasks.onActivity(player, BURY, listOf(bury.itemInternal), 1)
                }
            }
        }
    }

    private companion object {
        const val GATHER = "gather"
        const val BURY = "bury"
    }
}
