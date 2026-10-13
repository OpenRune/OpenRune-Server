package org.rsmod.content.other.combatachievements

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.combatachievements.CombatAchievements
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.ifOpenMainModal
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfModalPauseButton
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onPlayerInit
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CombatAchievementsScript
@Inject
constructor(private val achievements: CombatAchievements, private val eventBus: EventBus) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerInit { achievements.refresh(player) }
        for (screen in SCREENS) {
            onIfOpen("interface.$screen") { player.onOpen(screen) }
            onIfModalButton("component.$screen:burger_menu_frame") {
                when (it.comsub) {
                    10 -> open("ca_overview")
                    12 -> open("ca_tasks")
                    14 -> open("ca_bosses")
                    16 -> open("ca_rewards")
                }
            }
        }

        onIfModalButton("component.ca_overview:ca_buttons_click") {
            if (it.comsub in 0..5) {
                player.setTaskFilters(tier = it.comsub + 1)
                open("ca_tasks")
            }
        }

        onIfModalPauseButton("component.ca_bosses:bosses_button_click_pause") {
            val bosses = checkNotNull(ServerCacheManager.getEnum(BOSS_ENUM.asRSCM(RSCMType.ENUM))).values
            if (comsub in bosses) {
                VarPlayerIntMapSetter.set(player, "varbit.ca_boss_selected", comsub)
                // ca_boss_init_stats reads this shared dialog var. No audited PB adapter exists
                // yet, so hide the PB field rather than display a previous interface's value.
                VarPlayerIntMapSetter.set(player, "varp.if1", -1)
                player.ifOpenMainModal("interface.ca_boss", eventBus)
            }
        }

        onIfModalButton("component.ca_boss:ca_boss_stats_click") {
            if (it.comsub == 0) open("ca_bosses")
        }
        onIfModalButton("component.ca_boss:progress_bar") {
            if (it.comsub == 0) {
                player.setTaskFilters(boss = player.vars["varbit.ca_boss_selected"])
                open("ca_tasks")
            }
        }

        onIfModalButton("component.ca_rewards:tiers_click") {
            if (it.comsub in 0..5) {
                VarPlayerIntMapSetter.set(player, "varbit.ca_last_reward_tier_selected", it.comsub)
            }
        }
    }

    private fun Player.onOpen(screen: String) {
        achievements.refresh(this)
        ifSetEvents("component.$screen:burger_menu_frame", 10..16, IfEvent.Op1)
        when (screen) {
            "ca_overview" -> ifSetEvents("component.ca_overview:ca_buttons_click", 0..5, IfEvent.Op1)
            "ca_rewards" -> ifSetEvents("component.ca_rewards:tiers_click", 0..5, IfEvent.Op1)
            "ca_boss" -> {
                ifSetEvents("component.ca_boss:ca_boss_stats_click", 0..0, IfEvent.Op1)
                ifSetEvents("component.ca_boss:progress_bar", 0..0, IfEvent.Op1)
            }
            "ca_bosses" -> {
                // The hidden pause child's ID is the enum key, independent of search order.
                // Visible card child IDs are only positions in the filtered list.
                val bosses = checkNotNull(ServerCacheManager.getEnum(BOSS_ENUM.asRSCM(RSCMType.ENUM))).values.keys
                for (boss in bosses) {
                    ifSetEvents("component.ca_bosses:bosses_button_click_pause", boss..boss, IfEvent.PauseButton)
                }
            }
        }
    }

    private fun ProtectedAccess.open(screen: String) {
        ifOpenMainModal("interface.$screen")
    }

    private fun Player.setTaskFilters(tier: Int = 0, boss: Int = 0) {
        VarPlayerIntMapSetter.set(this, "varbit.ca_task_filter_tier", tier)
        VarPlayerIntMapSetter.set(this, "varbit.ca_task_filter_type", 0)
        VarPlayerIntMapSetter.set(this, "varbit.ca_task_filter_monster", boss)
        VarPlayerIntMapSetter.set(this, "varbit.ca_task_filter_completed", 0)
    }

    private companion object {
        // Verified revision-240 enum used by ca_bosses_draw_list and ca_boss_init.
        const val BOSS_ENUM = "enum.ca_bosses"
        val SCREENS = listOf("ca_overview", "ca_tasks", "ca_bosses", "ca_boss", "ca_rewards")
    }
}
