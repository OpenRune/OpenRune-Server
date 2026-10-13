package org.rsmod.content.other.nerocurator

import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.game.inv.InvObj

internal data class CompletionistProtection(val key: String, val repairFee: Int) {
    val unlocked = "obj.nero_completionist_$key"
    val locked = "${unlocked}_locked"
    val broken = "${unlocked}_broken"
    val lockedBroken = "${unlocked}_locked_broken"
    val mangled = "${unlocked}_locked_mangled"
    val symbols = listOf(unlocked, locked, broken, lockedBroken, mangled)
    fun locked(item: InvObj) = listOf(locked, lockedBroken, mangled).any { it.asRSCM() == item.id }
    fun broken(item: InvObj) = listOf(broken, lockedBroken, mangled).any { it.asRSCM() == item.id }
    fun fee(item: InvObj) = if (item.id == mangled.asRSCM()) 500000 else repairFee
    fun usable(item: InvObj, locked: Boolean) = item.copy(id = (if (locked) this.locked else unlocked).asRSCM())
    companion object {
        val all = listOf(CompletionistProtection("infernal", 99000), CompletionistProtection("fire", 150000), CompletionistProtection("assembler", 99000), CompletionistProtection("imbued_saradomin", 99000), CompletionistProtection("imbued_zamorak", 99000), CompletionistProtection("imbued_guthix", 99000), CompletionistProtection("masori_assembler", 99000), CompletionistProtection("dizana", 400000))
        fun family(item: InvObj?) = item?.let { value -> all.firstOrNull { family -> family.symbols.any { it.asRSCM() == value.id } } }
        const val DEEP_COINS = 569250
    }
}
