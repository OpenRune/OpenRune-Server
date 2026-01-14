package org.alter.interfaces.bank

import org.alter.api.ext.getVarbit
import org.alter.api.ext.incrementVarbit
import org.alter.api.ext.setVarbit
import org.alter.game.model.entity.Player
import kotlin.math.max


enum class BankTab(val index: Int, val sizeVarBit: String, val varValue: Int) {
    Tab1(0, "varbits.bank_tab_1", varValue = 1),
    Tab2(1, "varbits.bank_tab_2", varValue = 2),
    Tab3(2, "varbits.bank_tab_3", varValue = 3),
    Tab4(3, "varbits.bank_tab_4", varValue = 4),
    Tab5(4, "varbits.bank_tab_5", varValue = 5),
    Tab6(5, "varbits.bank_tab_6", varValue = 6),
    Tab7(6, "varbits.bank_tab_7", varValue = 7),
    Tab8(7, "varbits.bank_tab_8", varValue = 8),
    Tab9(8, "varbits.bank_tab_9", varValue = 9),
    Main(9, "varbits.bank_tab_main", varValue = 0);

    val isMainTab: Boolean
        get() = this == Main

    fun firstSlot(access: Player): Int {
        val indexRange = 0 until index
        return indexRange.sumOf {
            val tab = entries[it]
            access.getVarbit(tab.sizeVarBit)
        }
    }

    fun slotRange(access: Player): IntRange {
        val firstSlot = firstSlot(access)
        val occupiedSpace = occupiedSpace(access)
        return firstSlot until firstSlot + occupiedSpace
    }

    fun occupiedSpace(access: Player): Int = access.getVarbit(sizeVarBit)

    fun isEmpty(access: Player): Boolean = occupiedSpace(access) == 0

    fun decreaseSize(access: Player, amount: Int = 1) {
        val size = access.getVarbit(sizeVarBit)
        access.setVarbit(sizeVarBit,max(0, size - amount))
        assert(size >= amount) {
            "Decreased tab size with an amount higher than capacity: decrease=$amount, size=$size"
        }
    }

    fun increaseSize(access: Player, amount: Int = 1) {
        access.incrementVarbit(sizeVarBit,amount)
    }

    companion object {
        init {
            val sorted = entries.sortedBy(BankTab::index)
            check(sorted == entries) { "Entries must be sorted by `index`." }
        }

        val tabs = entries - Main

        fun forIndex(index: Int): BankTab? = entries.getOrNull(index)

        fun forSlot(access: Player, slot: Int): BankTab? {
            var currSlot = 0
            for (tab in entries) {
                val size = access.getVarbit(tab.sizeVarBit)
                if (slot in currSlot until currSlot + size) {
                    return tab
                }
                currSlot += size
            }
            return null
        }
    }
}
