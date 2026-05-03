package org.rsmod.content.interfaces.journal.tab

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.ui.ifCloseSub
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.vars.enumVarBit
import org.rsmod.api.player.vars.resyncVar
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player

internal var Player.sideJournalTab by enumVarBit<SideJournalTab>("varbit.side_journal_tab")

internal fun Player.openJournalTab(tab: SideJournalTab, eventBus: EventBus) =
    when (tab) {
        SideJournalTab.Summary -> openSummaryTab(eventBus)
        SideJournalTab.Quests -> openQuestTab(eventBus)
        SideJournalTab.Tasks -> openTaskTab(eventBus)
    }

internal fun Player.openSummaryTab(eventBus: EventBus) {
    updateSummaryTimePlayed()
    updateSummaryCombatLevel()
    ifOpenOverlay("interface.account_summary_sidepanel",
        "component.side_journal:tab_container", eventBus)
}

internal fun Player.updateSummaryTimePlayed() {
    val minutesPlayed = vars["varp.playtime"] / 100
    runClientScript(
        3970,
        "component.account_summary_sidepanel:summary_contents".asRSCM(RSCMType.COMPONENT),
        "component.account_summary_sidepanel:summary_click_layer".asRSCM(RSCMType.COMPONENT),
        minutesPlayed,
    )
}

internal fun Player.updateSummaryCombatLevel() {
    runClientScript(
        3954,
        "component.account_summary_sidepanel:summary_contents".asRSCM(RSCMType.COMPONENT),
        "component.account_summary_sidepanel:summary_click_layer".asRSCM(RSCMType.COMPONENT),
        combatLevel,
    )
}

internal fun Player.openQuestTab(eventBus: EventBus) {
    ifOpenOverlay("interface.questlist", "component.side_journal:tab_container", eventBus)
}

internal fun Player.openTaskTab(eventBus: EventBus) {
    ifOpenOverlay("interface.area_task", "component.side_journal:tab_container", eventBus)
}

internal fun Player.prepareJournalTab(tab: SideJournalTab) =
    when (tab) {
        SideJournalTab.Summary -> prepareSummaryTab()
        SideJournalTab.Quests -> prepareQuestTab()
        SideJournalTab.Tasks -> {}
    }

internal fun Player.prepareSummaryTab() {
    resyncVar("varp.collection_count_other_max")
    resyncVar("varp.collection_count_other")
    resyncVar("varp.collection_count_minigames_max")
    resyncVar("varp.collection_count_minigames")
    resyncVar("varp.collection_count_clues_max")
    resyncVar("varp.collection_count_clues")
    resyncVar("varp.collection_count_raids_max")
    resyncVar("varp.collection_count_raids")
    resyncVar("varp.collection_count_bosses_max")
    resyncVar("varp.collection_count_bosses")
    resyncVar("varp.collection_count_max")
    resyncVar("varp.collection_count")
}

internal fun Player.prepareQuestTab() {
    ClientScripts.playerMember(this)
}

internal fun Player.closeJournalTab(tab: SideJournalTab, eventBus: EventBus) =
    when (tab) {
        SideJournalTab.Summary -> ifCloseSub("interface.account_summary_sidepanel", eventBus)
        SideJournalTab.Quests -> ifCloseSub("interface.questlist", eventBus)
        SideJournalTab.Tasks -> ifCloseSub("interface.area_task", eventBus)
    }

internal fun Player.switchJournalTab(open: SideJournalTab, eventBus: EventBus) {
    val previous = sideJournalTab
    sideJournalTab = open
    closeJournalTab(previous, eventBus)
    prepareJournalTab(open)
    openJournalTab(open, eventBus)
}
