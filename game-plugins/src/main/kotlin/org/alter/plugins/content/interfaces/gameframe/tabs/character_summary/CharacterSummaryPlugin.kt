package org.alter.plugins.content.interfaces.gameframe.tabs.character_summary

import org.alter.api.CommonClientScripts
import org.alter.api.InterfaceDestination
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.timer.TimerKey
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * @author CloudS3c
 */
class CharacterSummaryPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    companion object {
        /**
         * Timer key for updating playtime display. Fires every minute (100 cycles).
         */
        val PLAYTIME_UPDATE_TIMER = TimerKey("playtime_update_timer", tickOffline = false)
    }

    init {
        val CHARACTER_SUMMARY_TAB_INTERFACE = 712
        val QUEST_JOURNY_TAB_INTERFACE = 399
        val DIARY_PROGRESS_TAB_INTERFACE = 259

        onLogin {
            player.openInterface(InterfaceDestination.QUEST_ROOT.interfaceId, 43, player.getQuestRootTab(), 1)
            player.setVarbit("varbits.qp_max", 123) // @TODO
            player.setVarp("varp.qp", 100) // @TODO
            player.setVarbit("varbits.quests_total_count", 100) // @TODO
            player.setVarbit("varbits.quests_completed_count", 58) // @TODO
            updatePlaytimeDisplay(player)
        }

        onButton(InterfaceDestination.QUEST_ROOT.interfaceId, 2) {
            player.closeInterface(player.getQuestRootTab())
            player.setVarbit("varbits.side_journal_tab", 0)
            player.openInterface(InterfaceDestination.QUEST_ROOT.interfaceId, 43, CHARACTER_SUMMARY_TAB_INTERFACE, 1)
        }
        onButton(InterfaceDestination.QUEST_ROOT.interfaceId, 10) {
            player.closeInterface(player.getQuestRootTab())
            player.setVarbit("varbits.side_journal_tab", 1)
            player.openInterface(InterfaceDestination.QUEST_ROOT.interfaceId, 43, QUEST_JOURNY_TAB_INTERFACE, 1)
        }
        onButton(InterfaceDestination.QUEST_ROOT.interfaceId, 18) {
            player.closeInterface(player.getQuestRootTab())
            player.setVarbit("varbits.side_journal_tab", 2)
            player.openInterface(InterfaceDestination.QUEST_ROOT.interfaceId, 43, DIARY_PROGRESS_TAB_INTERFACE, 1)
        }
        onButton(CHARACTER_SUMMARY_TAB_INTERFACE, 3) {
            player.toggleVarbit("varbits.account_summary_display_playtime")

            updatePlaytimeDisplay(player)

            // Start periodic updates if playtime is now revealed, stop if hidden
            if (player.getVarbit("varbits.account_summary_display_playtime") == 1) {
                player.timers[PLAYTIME_UPDATE_TIMER] = 100 // Update every minute (100 cycles)
            } else {
                player.timers.remove(PLAYTIME_UPDATE_TIMER)
            }
        }

        // Update playtime display every minute if it's revealed
        onTimer(PLAYTIME_UPDATE_TIMER) {
            if (player.getVarbit("varbits.account_summary_display_playtime") == 1) {
                updatePlaytimeDisplay(player)
                player.timers[PLAYTIME_UPDATE_TIMER] = 100 // Reset timer for next minute
            } else {
                // Playtime was hidden, stop the timer
                player.timers.remove(PLAYTIME_UPDATE_TIMER)
            }
        }

        onInterfaceOpen(CHARACTER_SUMMARY_TAB_INTERFACE) {
            player.setInterfaceEvents(
                interfaceId = 712,
                component = 3,
                range = 3..7,
                setting = arrayOf(
                    InterfaceEvent.ClickOp1,
                    InterfaceEvent.ClickOp2,
                    InterfaceEvent.ClickOp3,
                    InterfaceEvent.ClickOp4
                )
            )
        }
        onInterfaceOpen(QUEST_JOURNY_TAB_INTERFACE) {
            player.setInterfaceEvents(
                interfaceId = 399,
                component = 7,
                range = 0..198,
                setting = arrayOf(
                    InterfaceEvent.ClickOp1,
                    InterfaceEvent.ClickOp2,
                    InterfaceEvent.ClickOp3,
                    InterfaceEvent.ClickOp4,
                    InterfaceEvent.ClickOp5
                )
            )
        }
        onInterfaceOpen(DIARY_PROGRESS_TAB_INTERFACE) {
            player.setInterfaceEvents(
                interfaceId = 259,
                component = 2,
                range = 0..11,
                setting = arrayOf(
                    InterfaceEvent.ClickOp1,
                    InterfaceEvent.ClickOp2
                )
            )
        }
    }

    /**
     * Updates the playtime display in the character summary interface.
     * Converts cumulative playtime from cycles to milliseconds and minutes.
     */
    private fun updatePlaytimeDisplay(player: Player) {
        val minutesPlayed = player.playtime / 100
        player.runClientScript(CommonClientScripts.TIME_PLAYED, 46661634, 46661635, minutesPlayed)
    }

    /**
     * 1 -> "Quest List"
     * 2 -> "Achievement Diary"
     * 3 -> "Adventure Path"
     * 4 -> "Leagues"
     * else -> "Character Summary"
     */
    private fun Player.getQuestRootTab(): Int = when (getVarbit("varbits.side_journal_tab")) {
        1 -> 399
        2 -> 259
        else -> 712
    }
}
