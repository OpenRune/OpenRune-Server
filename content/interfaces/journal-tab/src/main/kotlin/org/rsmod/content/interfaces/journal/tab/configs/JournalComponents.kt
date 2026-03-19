package org.rsmod.content.interfaces.journal.tab.configs

import dev.openrune.component

typealias journal_components = JournalComponents

object JournalComponents {
    val tab_container = component("side_journal:tab_container")
    val summary_list = component("side_journal:summary_list")
    val quest_list = component("side_journal:quest_list")
    val task_list = component("side_journal:task_list")

    val summary_contents = component("account_summary_sidepanel:summary_contents")
    val summary_click_layer = component("account_summary_sidepanel:summary_click_layer")
}
