package org.rsmod.content.interfaces.journal.tab

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.ui.ifCloseSub
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.ui.ifSetEvents
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
        SideJournalTab.AdventurePaths -> openAdventurePathTab(eventBus)
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

/**
 * Adventure Paths, captured at tick 96 of `20260822T133202` (lines 2871-2876):
 *
 * ```
 * if_closesub com=side_journal:tab_container (629:43), id=questlist (399)
 * if_opensub  com=side_journal:tab_container (629:43), id=adventurepath_side (644), type=overlay
 * if_setevents_v2 com=adventurepath_side:toggles (644:4), start=11,  end=11,  events=[OP1]
 * if_setevents_v2 com=adventurepath_side:toggles (644:4), start=23,  end=23,  events=[OP1]
 * if_setevents_v2 com=adventurepath_side:toggles (644:4), start=35,  end=35,  events=[OP1]
 * if_setevents_v2 com=adventurepath_side:toggles (644:4), start=47,  end=47,  events=[OP1]
 * if_setevents_v2 com=adventurepath_side:toggles (644:4), start=59,  end=59,  events=[OP1]
 * ```
 *
 * Five rows on the same `11 + 12n` stride the full-screen modal's task list uses, each armed as
 * its own single-subcomponent write rather than a span, exactly as the live server sends them.
 *
 * The panel's other buttons — `task_name` (644:9), `task_description` (644:11),
 * `open_interface` (644:15) and `path_name` (644:16) — are never armed by a packet, so they are
 * armed in the cache and only need handlers; those live in `AdventurePathScript`.
 *
 * Unlike the quest list there is no `playermember` call beforehand, which is why
 * [prepareJournalTab] has nothing to do for this tab.
 */
internal fun Player.openAdventurePathTab(eventBus: EventBus) {
    ifOpenOverlay("interface.adventurepath_side", "component.side_journal:tab_container", eventBus)
    for (n in 0 until SIDE_TOGGLE_ROWS) {
        val sub = SIDE_TOGGLE_FIRST + (n * SIDE_TOGGLE_STRIDE)
        ifSetEvents("component.adventurepath_side:toggles", sub..sub, IfEvent.Op1)
    }
}

private const val SIDE_TOGGLE_FIRST = 11
private const val SIDE_TOGGLE_STRIDE = 12
private const val SIDE_TOGGLE_ROWS = 5

internal fun Player.prepareJournalTab(tab: SideJournalTab) =
    when (tab) {
        SideJournalTab.Summary -> prepareSummaryTab()
        SideJournalTab.Quests -> prepareQuestTab()
        SideJournalTab.Tasks -> {}
        SideJournalTab.AdventurePaths -> {}
    }

internal fun Player.prepareSummaryTab() {
    resyncVar("varp.collection_count_highscores")
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
        SideJournalTab.AdventurePaths -> ifCloseSub("interface.adventurepath_side", eventBus)
    }

internal fun Player.switchJournalTab(open: SideJournalTab, eventBus: EventBus) {
    val previous = sideJournalTab
    sideJournalTab = open
    closeJournalTab(previous, eventBus)
    prepareJournalTab(open)
    openJournalTab(open, eventBus)
}
