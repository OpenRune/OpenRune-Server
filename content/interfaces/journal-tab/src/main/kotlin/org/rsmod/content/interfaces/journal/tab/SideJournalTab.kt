package org.rsmod.content.interfaces.journal.tab

import org.rsmod.api.utils.vars.VarEnumDelegate

enum class SideJournalTab(override val varValue: Int) : VarEnumDelegate {
    Summary(varValue = 0),
    Quests(varValue = 1),
    Tasks(varValue = 2),
    // Capture 20260822T133202 line 2856: clicking `side_journal:adventurepath_list` writes
    // `side_journal_tab (8168) 1 -> 3`, so Adventure Paths is 3 and not the next free ordinal.
    AdventurePaths(varValue = 3),
}
