package org.alter.interfaces.bank

import org.alter.api.ext.boolVarBit
import org.alter.api.ext.intVarBit
import org.alter.api.ext.intVarp
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.Player

object BankState {

    // --- Persisted state (varps/varbits) ---
    var Player.bankWithdrawAsNote by boolVarBit("varbits.bank_withdrawnotes")
    var Player.bankInsertMode by boolVarBit("varbits.bank_insertmode")
    var Player.bankQuantityType by intVarBit("varbits.bank_quantity_type")

    var Player.bankTab0Size by intVarp("varp.bank_tab_display")
    var Player.bankTab1Size by intVarp("varp.bank_tab_1")
    var Player.bankTab2Size by intVarp("varp.bank_tab_2")
    var Player.bankTab3Size by intVarp("varp.bank_tab_3")
    var Player.bankTab4Size by intVarp("varp.bank_tab_4")
    var Player.bankTab5Size by intVarp("varp.bank_tab_5")
    var Player.bankTab6Size by intVarp("varp.bank_tab_6")
    var Player.bankTab7Size by intVarp("varp.bank_tab_7")
    var Player.bankTab8Size by intVarp("varp.bank_tab_8")
    var Player.bankTab9Size by intVarp("varp.bank_tab_9")

    // --- Transient state (lost on logout) ---
    val BANK_ACTIVE_TAB = AttributeKey<Int>()
    val BANK_SEARCH_MODE = AttributeKey<Boolean>()
    val BANK_PRE_SEARCH_TAB = AttributeKey<Int>()
    val BANK_LAST_X = AttributeKey<Int>()
    val BANK_PLACEHOLDER_MODE = AttributeKey<Boolean>()

    var Player.bankActiveTab: Int
        get() = attr[BANK_ACTIVE_TAB] ?: 0
        set(value) { attr[BANK_ACTIVE_TAB] = value }

    var Player.bankSearchMode: Boolean
        get() = attr[BANK_SEARCH_MODE] ?: false
        set(value) { attr[BANK_SEARCH_MODE] = value }

    var Player.bankPreSearchTab: Int
        get() = attr[BANK_PRE_SEARCH_TAB] ?: 0
        set(value) { attr[BANK_PRE_SEARCH_TAB] = value }

    var Player.bankLastXAmount: Int
        get() = attr[BANK_LAST_X] ?: 0
        set(value) { attr[BANK_LAST_X] = value }

    var Player.bankPlaceholderMode: Boolean
        get() = attr[BANK_PLACEHOLDER_MODE] ?: false
        set(value) { attr[BANK_PLACEHOLDER_MODE] = value }

    // --- Tab size helpers ---

    fun Player.getTabSizes(): IntArray = intArrayOf(
        bankTab0Size, bankTab1Size, bankTab2Size, bankTab3Size,
        bankTab4Size, bankTab5Size, bankTab6Size,
        bankTab7Size, bankTab8Size, bankTab9Size,
    )

    fun Player.setTabSizes(sizes: IntArray) {
        bankTab0Size = sizes[0]; bankTab1Size = sizes[1]; bankTab2Size = sizes[2]
        bankTab3Size = sizes[3]; bankTab4Size = sizes[4]; bankTab5Size = sizes[5]
        bankTab6Size = sizes[6]; bankTab7Size = sizes[7]; bankTab8Size = sizes[8]
        bankTab9Size = sizes[9]
    }

    fun tabStartSlot(sizes: IntArray, tab: Int): Int = sizes.take(tab).sum()
    fun tabEndSlot(sizes: IntArray, tab: Int): Int = sizes.take(tab + 1).sum()

    fun tabForSlot(sizes: IntArray, slot: Int): Int {
        var cumulative = 0
        for (i in sizes.indices) {
            cumulative += sizes[i]
            if (slot < cumulative) return i
        }
        return -1
    }
}
