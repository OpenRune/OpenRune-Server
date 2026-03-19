package org.rsmod.content.interfaces.journal.tab.configs

import dev.openrune.varBit

typealias journal_varbits = JournalVarBits

object JournalVarBits {
    val display_playtime_remind_disable = varBit("account_summary_display_playtime_remind_disable")
    val display_playtime = varBit("account_summary_display_playtime")
}
