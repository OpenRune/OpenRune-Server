package org.rsmod.content.interfaces.journal.tab.configs

import org.rsmod.api.type.refs.varbit.VarBitReferences

typealias journal_varbits = JournalVarBits

object JournalVarBits : VarBitReferences() {
    val display_playtime_remind_disable = varBit("account_summary_display_playtime_remind_disable")
    val display_playtime = varBit("account_summary_display_playtime")
}
