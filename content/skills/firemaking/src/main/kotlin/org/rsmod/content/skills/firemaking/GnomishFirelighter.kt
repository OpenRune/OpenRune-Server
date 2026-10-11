package org.rsmod.content.skills.firemaking

import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.invtx.*
import org.rsmod.api.table.FiremakingColoredLogsRow
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

/** The charge count and colour travel with this particular item in its inventory vars. */
internal object GnomishFirelighter {
    const val EMPTY = "obj.gnomish_firelighter"
    const val CHARGED = "obj.gnomish_firelighter_charged"
    const val MAX = 1000
    val colours by lazy { FiremakingColoredLogsRow.all().sortedBy { it.firelighter.id } }
    fun colour(item: InvObj): FiremakingColoredLogsRow? = if (item.id == CHARGED.asRSCM()) colours.getOrNull((item.vars ushr 10 and 7) - 1) else null
    fun count(item: InvObj): Int = if (colour(item) != null && item.vars and 1023 in 1..MAX) item.vars and 1023 else 0
    fun write(item: InvObj, colour: FiremakingColoredLogsRow, amount: Int): InvObj {
        require(amount in 0..MAX)
        val index = colours.indexOfFirst { it.firelighter.id == colour.firelighter.id }
        require(index >= 0)
        return item.copy(id = (if (amount == 0) EMPTY else CHARGED).asRSCM(), vars = if (amount == 0) 0 else (index + 1 shl 10) or amount)
    }
    fun fill(player: Player, slot: Int, sourceSlot: Int): Boolean {
        val item = player.inv[slot] ?: return false
        val source = player.inv[sourceSlot] ?: return false
        if (item.id !in setOf(EMPTY.asRSCM(), CHARGED.asRSCM()) || slot == sourceSlot) return false
        val colour = colours.firstOrNull { it.firelighter.id == source.id } ?: return false
        val before = count(item)
        if (before > 0 && colour(item)?.firelighter?.id != source.id) return false
        val amount = minOf(source.count, MAX - before)
        if (amount <= 0) return false
        val next = write(item, colour, before + amount)
        return player.invTransaction(player.inv) {
            val i = select(player.inv); delete(i, item.id, 1, slot = slot); add(i, next.id, 1, vars = next.vars, slot = slot)
            delete(i, source.id, amount, slot = sourceSlot)
        }.success
    }
    fun uncharge(player: Player, slot: Int): Boolean {
        val item = player.inv[slot] ?: return false
        val colour = colour(item) ?: return false
        val amount = count(item)
        if (amount == 0) return false
        return player.invTransaction(player.inv) {
            val i = select(player.inv); delete(i, item.id, 1, slot = slot); add(i, EMPTY.asRSCM(), 1, slot = slot)
            add(i, colour.firelighter.id, amount)
        }.success
    }
    fun consume(player: Player, slot: Int, original: InvObj): Boolean {
        if (player.inv[slot] !== original) return false
        val colour = colour(original) ?: return false
        val amount = count(original)
        if (amount <= 0) return false
        val next = write(original, colour, amount - 1)
        return player.invTransaction(player.inv) {
            val i = select(player.inv); delete(i, original.id, 1, slot = slot); add(i, next.id, 1, vars = next.vars, slot = slot)
        }.success
    }
}
