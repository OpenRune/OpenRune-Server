package org.rsmod.content.other.pouches.runepouch

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.varp.bits
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.game.entity.Player

/** Native A-J slots include fields split across unrelated permanent varps. */
internal object RunePouchLoadoutCodec {
    val names = AttributeKey<MutableMap<String, String>>(persistenceKey = "rune_pouch_loadout_custom_names")
    fun read(player: Player, index: Int): RunePouchContents {
        require(index in 0..9)
        val types = IntArray(4) { readField(player, fields(index, it, false)) }
        val counts = IntArray(4) { readField(player, fields(index, it, true)).coerceIn(0, RunePouchContents.MAX_STACK) }
        val seen = mutableSetOf<Int>()
        for (slot in 0..3) if (counts[slot] == 0 || RunePouchRunes.byCompact(types[slot]) == null || !seen.add(types[slot])) { types[slot] = 0; counts[slot] = 0 }
        return RunePouchContents(types, counts)
    }
    fun write(player: Player, index: Int, contents: RunePouchContents) {
        require(index in 0..9)
        for (slot in 0..3) {
            val count = contents.counts.getOrElse(slot) { 0 }.coerceIn(0, RunePouchContents.MAX_STACK)
            val type = if (count > 0) contents.types.getOrElse(slot) { 0 } else 0
            require(type == 0 || RunePouchRunes.byCompact(type) != null)
            writeField(player, fields(index, slot, false), type)
            writeField(player, fields(index, slot, true), count)
        }
    }
    private fun fields(index: Int, slot: Int, quantity: Boolean): List<String> {
        val letter = ('a'.code + index).toChar()
        val field = "varbit.rune_pouch_loadout_${letter}_${if (quantity) "cap" else "rune"}${slot + 1}"
        return when {
            !quantity && letter in "aei" && slot < 2 -> listOf("${field}_bitsa", "${field}_bitsb")
            quantity && letter in "dh" && slot == 2 -> listOf("${field}_bitsa", "${field}_bitsb", "${field}_bitsc")
            else -> listOf(field)
        }
    }
    private fun readField(player: Player, fields: List<String>): Int {
        var shift = 0; var value = 0
        for (field in fields) { val bits = ServerCacheManager.getVarbit(field.asRSCM())!!.bits; value = value or (player.vars[field] shl shift); shift += bits.last - bits.first + 1 }
        return value
    }
    private fun writeField(player: Player, fields: List<String>, value: Int) {
        var shift = 0
        for (field in fields) { val bits = ServerCacheManager.getVarbit(field.asRSCM())!!.bits; val width = bits.last - bits.first + 1; val mask = (1 shl width) - 1; VarPlayerIntMapSetter.set(player, field, (value ushr shift) and mask); shift += width }
    }
}

/** Bank configuration uses the accepted native chat menu while newer bank buttons are absent. */
internal class RunePouchLoadouts(private val bankScript: BankInvScript) {
    suspend fun configure(access: ProtectedAccess, pouchSlot: Int, open: () -> Unit) = with(access) {
        if (!player.ui.containsModal("interface.bankmain")) return@with
        val pouch = inv[pouchSlot] ?: return@with
        var openedPouch = false
        val origin = coords
        fun valid() = inv[pouchSlot] === pouch && player.hitpoints > 0 && !player.pendingLogout && !player.loggingOut && coords == origin
        try {
        val choice = menu("Configure rune pouch", hotkeys = true, choices = listOf("Open pouch", "Loadouts", "Cancel"))
        if (!valid()) return@with
        when (choice) {
            0 -> { openedPouch = true; open() }
            1 -> {
                var page = 0
                while (valid()) {
                    val indices = if (page == 0) 0..4 else 5..9
                    val selected = menu("Rune pouch loadouts", hotkeys = true, choices = indices.map { index -> "${index + 1}. ${player.attr[RunePouchLoadoutCodec.names]?.get(index.toString()) ?: "Loadout ${index + 1}"}" } + listOf("Next page", "Close"))
                    if (!valid() || selected == 6) return@with
                    if (selected == 5) { page = 1 - page; continue }
                    if (selected !in 0..4) return@with
                    val index = indices.first + selected
                    when (menu("Loadout ${index + 1}", hotkeys = true, choices = listOf("Load", "Save current pouch", "Edit quantities", "Rename", "Clear", "Back"))) {
                        0 -> if (valid()) load(this, index)
                        1 -> if (valid()) { RunePouchLoadoutCodec.write(player, index, player.readRunePouch(player.runePouchSlots() ?: return@with)); mes("You save this rune pouch loadout.") }
                        2 -> {
                            val saved = RunePouchLoadoutCodec.read(player, index)
                            val slots = saved.types.indices.filter { saved.counts[it] > 0 }
                            if (slots.isEmpty()) { mes("Save some runes into this loadout first."); continue }
                            val runeSlot = menu("Choose rune", hotkeys = true, choices = slots.map { "${RunePouchRunes.byCompact(saved.types[it])!!.type.name}: ${saved.counts[it]}" } + "Cancel")
                            if (runeSlot in slots.indices) { val count = countDialog(); if (valid() && count in 0..RunePouchContents.MAX_STACK) { saved.counts[slots[runeSlot]] = count; RunePouchLoadoutCodec.write(player, index, saved) } }
                        }
                        3 -> {
                            val name = stringDialog("Name this loadout (up to 20 letters, numbers or spaces)").filter { it.isLetterOrDigit() || it == ' ' }.trim().take(20)
                            if (valid() && name.isNotBlank()) { val names = player.attr[RunePouchLoadoutCodec.names].orEmpty().toMutableMap(); names[index.toString()] = name; player.attr[RunePouchLoadoutCodec.names] = names }
                        }
                        4 -> if (valid()) RunePouchLoadoutCodec.write(player, index, RunePouchContents.empty(4))
                    }
                }
            }
        }
            } finally {
            if (!openedPouch && valid()) ifOpenMainSidePair("interface.bankmain", "interface.bankside")
        }
    }
    private fun load(access: ProtectedAccess, index: Int) = with(access) {
        val slots = player.runePouchSlots() ?: return@with
        if (player.hitpoints <= 0 || player.pendingLogout || player.loggingOut) return@with
        val desired = RunePouchLoadoutCodec.read(player, index)
        if (desired.isEmpty()) { mes("This loadout is empty."); return@with }
        if (desired.counts.count { it > 0 } > slots) { mes("This loadout requires a divine rune pouch."); return@with }
        val contents = player.readRunePouch(slots)
        // Return obsolete/excess stacks first; a full bank retains any unbanked remainder.
        for (slot in 0 until slots) {
            val rune = RunePouchRunes.byCompact(contents.types[slot]) ?: continue
            val wanted = desired.slotOf(rune.compactId).takeIf { it >= 0 }?.let { desired.counts[it] } ?: 0
            val excess = (contents.counts[slot] - wanted).coerceAtLeast(0)
            val moved = with(bankScript) { depositStoredStack(rune.id, excess) }
            if (moved > 0) contents.remove(slot, moved)
        }
        for (slot in 0..3) {
            if (desired.counts[slot] <= 0) continue
            val rune = RunePouchRunes.byCompact(desired.types[slot]) ?: continue
            val existing = contents.slotOf(rune.compactId)
            if (existing < 0 && contents.freeSlot() < 0) continue
            val missing = desired.counts[slot] - if (existing < 0) 0 else contents.counts[existing]
            if (missing > 0) { val moved = with(bankScript) { withdrawStoredStack(rune.id, missing) }; contents.add(rune.compactId, moved) }
        }
        // A withdrawal can free a bank slot that was unavailable during the first return pass.
        for (slot in 0 until slots) {
            val rune = RunePouchRunes.byCompact(contents.types[slot]) ?: continue
            val wanted = desired.slotOf(rune.compactId).takeIf { it >= 0 }?.let { desired.counts[it] } ?: 0
            val excess = (contents.counts[slot] - wanted).coerceAtLeast(0)
            val moved = with(bankScript) { depositStoredStack(rune.id, excess) }
            if (moved > 0) contents.remove(slot, moved)
        }
        val order = desired.types.filter { it != 0 } + contents.types.filter { it != 0 && it !in desired.types }
        val reordered = RunePouchContents.empty(slots)
        for (type in order.distinct()) { val from = contents.slotOf(type); if (from >= 0) reordered.add(type, contents.counts[from]) }
        player.writeRunePouch(reordered)
        val complete = (0..3).all { slot -> desired.counts[slot] == 0 || reordered.slotOf(desired.types[slot]).takeIf { it >= 0 }?.let { reordered.counts[it] } == desired.counts[slot] } && reordered.types.filter { it != 0 }.all { it in desired.types }
        mes(if (complete) "You load your rune pouch preset." else "The bank could not supply or accept every rune in this loadout.")
    }
}
