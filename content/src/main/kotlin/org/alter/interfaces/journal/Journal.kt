package org.alter.interfaces.journal

import org.alter.api.CommonClientScripts
import org.alter.api.ext.enumVarBit
import org.alter.api.ext.playtime
import org.alter.api.ext.runClientScript
import org.alter.api.ext.syncVarp
import org.alter.game.model.entity.Player
import org.alter.interfaces.ifCloseSub
import org.alter.interfaces.ifOpenOverlay
import org.alter.rscm.RSCM.asRSCM

object Journal {

    var Player.sideJournalTab by enumVarBit<SideJournalTab>("varbits.side_journal_tab")

    fun openActiveJournal(player: Player) = openJournalTab(player,player.sideJournalTab)

    fun openJournalTab(player: Player,tab: SideJournalTab) = when (tab) {
        SideJournalTab.Summary -> openSummaryTab(player)
        SideJournalTab.Quests -> openQuestTab(player)
        SideJournalTab.Tasks -> openTaskTab(player)
    }

    internal fun closeJournalTab(player: Player,tab: SideJournalTab) = when (tab) {
        SideJournalTab.Summary -> player.ifCloseSub("interfaces.account_summary_sidepanel")
        SideJournalTab.Quests -> player.ifCloseSub("interfaces.questlist")
        SideJournalTab.Tasks -> player.ifCloseSub("interfaces.area_task")
    }

    internal fun openSummaryTab(player: Player) {
        updateSummaryTimePlayed(player)
        updateSummaryCombatLevel(player)
        player.ifOpenOverlay("interfaces.account_summary_sidepanel", "components.side_journal:tab_container")
    }

    internal fun updateSummaryCombatLevel(player: Player) {
        player.runClientScript(
            CommonClientScripts.COMBAT_LEVEL_TRANSMIT,
            "components.account_summary_sidepanel:summary_contents".asRSCM(),
            "components.account_summary_sidepanel:summary_click_layer".asRSCM(),
            player.combatLevel,
        )
    }

    internal fun updateSummaryTimePlayed(player: Player) {
        val minutesPlayed = player.playtime / 100
        player.runClientScript(
            CommonClientScripts.TIME_PLAYED,
            "components.account_summary_sidepanel:summary_contents".asRSCM(),
            "components.account_summary_sidepanel:summary_click_layer".asRSCM(),
            minutesPlayed,
        )
    }

    internal fun openQuestTab(player: Player) {
        player.ifOpenOverlay("interfaces.questlist", "components.side_journal:tab_container")
    }

    internal fun openTaskTab(player: Player) {
        player.ifOpenOverlay("interfaces.area_task", "components.side_journal:tab_container")
    }

    internal fun prepareJournalTab(player: Player,tab: SideJournalTab) =
        when (tab) {
            SideJournalTab.Summary -> prepareSummaryTab(player)
            SideJournalTab.Quests -> prepareQuestTab(player)
            SideJournalTab.Tasks -> {}
        }


    internal fun switchJournalTab(player: Player,open: SideJournalTab) {
        val previous = player.sideJournalTab
        player.sideJournalTab = open
        closeJournalTab(player,previous)
        prepareJournalTab(player,open)
        openJournalTab(player,open)
    }

    internal fun prepareSummaryTab(player: Player) {
        player.syncVarp("varp.collection_count_other_max")
        player.syncVarp("varp.collection_count_other")
        player.syncVarp("varp.collection_count_minigames_max")
        player.syncVarp("varp.collection_count_minigames")
        player.syncVarp("varp.collection_count_clues_max")
        player.syncVarp("varp.collection_count_clues")
        player.syncVarp("varp.collection_count_raids_max")
        player.syncVarp("varp.collection_count_raids")
        player.syncVarp("varp.collection_count_bosses_max")
        player.syncVarp("varp.collection_count_bosses")
        player.syncVarp("varp.collection_count_max")
        player.syncVarp("varp.collection_count")
    }

    internal fun prepareQuestTab(player: Player) {
        player.runClientScript(CommonClientScripts.MEMBERS,1)
    }

}