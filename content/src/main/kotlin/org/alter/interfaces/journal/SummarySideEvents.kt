package org.alter.interfaces.journal

import dev.openrune.definition.type.widget.IfEvent
import org.alter.api.ext.boolVarBit
import org.alter.api.ext.options
import org.alter.game.model.attr.PLAYTIME_ATTR
import org.alter.game.model.entity.Player
import org.alter.game.model.timer.TimerKey
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onButton
import org.alter.game.pluginnew.event.impl.onIfOpen
import org.alter.game.pluginnew.event.impl.onLogin
import org.alter.game.pluginnew.event.impl.onTimer
import org.alter.interfaces.ifClose
import org.alter.interfaces.ifOpenMainModal
import org.alter.interfaces.ifOpenOverlay
import org.alter.interfaces.ifSetEvents
import org.alter.interfaces.journal.Journal.switchJournalTab
import org.alter.interfaces.journal.Journal.updateSummaryTimePlayed

class SummarySideEvents : PluginEvent() {

    companion object {
        val PLAYTIME_UPDATE_TIMER = TimerKey("playtime_update_timer", tickOffline = false)
        val PLAYTIME_TIMER = TimerKey("playtime_timer", tickOffline = false)
    }

    private var Player.displayPlaytime by boolVarBit("varbits.account_summary_display_playtime")
    private var Player.displayPlaytimeReminderDisabled by boolVarBit("varbits.account_summary_display_playtime_remind_disable")

    override fun init() {

        onTimer(PLAYTIME_UPDATE_TIMER) {
            val player = player as Player
            if (player.displayPlaytime) {
                updateSummaryTimePlayed(player)
                player.timers[PLAYTIME_UPDATE_TIMER] = 1
            } else {
                player.timers.remove(PLAYTIME_UPDATE_TIMER)
            }
        }

        onLogin {
            player.timers[PLAYTIME_TIMER] = 1
        }

        onTimer(PLAYTIME_TIMER) {
            val currentPlaytime = player.attr[PLAYTIME_ATTR] ?: 0
            player.attr[PLAYTIME_ATTR] = currentPlaytime + 1
            player.timers[PLAYTIME_TIMER] = 1
        }

        onIfOpen("interfaces.account_summary_sidepanel") {
            onSummarySideOpen(player)
        }

        onButton("components.account_summary_sidepanel:summary_click_layer") {
            clickSummaryLayer(player,slot)
        }
    }

    private fun clickSummaryLayer(player: Player,comsub: Int) {
        when (comsub) {
            3 -> switchJournalTab(player,SideJournalTab.Quests)
            4 -> switchJournalTab(player,SideJournalTab.Tasks)
            5 -> clickCombatAchievements(player)
            6 -> player.ifOpenOverlay("interfaces.collection")
            7 -> selectTimePlayedToggle(player)
            else -> throw NotImplementedError("Unhandled summary click: comsub=$comsub")
        }
    }

    private fun selectTimePlayedToggle(player: Player) {
        player.ifClose()
        player.timers.remove(PLAYTIME_UPDATE_TIMER)
        if (player.displayPlaytimeReminderDisabled || player.displayPlaytime) {
            player.displayPlaytime = !player.displayPlaytime
            updateSummaryTimePlayed(player)
            return
        }

        player.queue {
            val option = options(
                player,
                "Yes", "Yes and don't ask me again", "No" ,
                title = "Are you sure you want to display your time played?"
            )
            if (option == 3) {
                return@queue
            }

            if (option == 2) {
                player.displayPlaytimeReminderDisabled = true
            }
            player.displayPlaytime = !player.displayPlaytime
            player.timers[PLAYTIME_UPDATE_TIMER] = 100
            updateSummaryTimePlayed(player)
        }
    }

    private fun clickCombatAchievements(player: Player) {
        player.ifClose()
        player.ifOpenMainModal("interfaces.ca_overview")
    }

    private fun onSummarySideOpen(player: Player) {
        player.ifSetEvents(
            "components.account_summary_sidepanel:summary_click_layer", 3..7,
            IfEvent.Op1,
            IfEvent.Op2,
            IfEvent.Op3,
            IfEvent.Op4,
        )
    }

}