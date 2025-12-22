package org.alter.interfaces.journal

import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onButton
import org.alter.game.pluginnew.event.impl.onIfOpen
import org.alter.interfaces.journal.Journal.openActiveJournal
import org.alter.interfaces.journal.Journal.switchJournalTab

class JournalTabEvents : PluginEvent() {

    override fun init() {

        onIfOpen("interfaces.side_journal") {
            openActiveJournal(player)
        }

        onButton("components.side_journal:summary_list") {
            switchJournalTab(player,SideJournalTab.Summary)
        }

        onButton("components.side_journal:quest_list") {
            switchJournalTab(player,SideJournalTab.Quests)
        }

        onButton("components.side_journal:task_list") {
            switchJournalTab(player,SideJournalTab.Tasks)
        }

    }

}