package org.rsmod.content.skills.magic.runepouch

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.enums.RuneEnums.rune_compact_ids
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

/**
 * Rune pouch contents live in the client-visible varbits `rune_pouch_type_n` (the rune's compact
 * id, enum `rune_compact_ids`) and `rune_pouch_quantity_n`: the pouch interface (190) draws them,
 * spells cast from them and RuneLite's Rune Pouch plugin reads them.
 */
object RunePouch {
    const val CAPACITY = 16_000

    val regularPouches = listOf("obj.bh_rune_pouch", "obj.bh_rune_pouch_trouver")
    val divinePouches = listOf("obj.divine_rune_pouch", "obj.divine_rune_pouch_trouver")
    val pouches = regularPouches + divinePouches

    private val typeVarbits = (1..4).map { "varbit.rune_pouch_type_$it" }
    private val quantityVarbits = (1..4).map { "varbit.rune_pouch_quantity_$it" }

    private val compactIds: Map<String, Int> by lazy {
        rune_compact_ids.filterValuesNotNull().backing.entries.associate {
            RSCM.getReverseMapping(RSCMType.OBJ, it.key.id) to it.value
        }
    }
    private val runesByCompactId: Map<Int, String> by lazy {
        compactIds.entries.associate { it.value to it.key }
    }

    val runes: Set<String> get() = compactIds.keys

    fun compactId(obj: String): Int? = compactIds[obj]

    fun isRune(obj: String): Boolean = obj in compactIds

    fun slotCount(player: Player): Int =
        when {
            divinePouches.any { it in player.inv } -> 4
            regularPouches.any { it in player.inv } -> 3
            else -> 0
        }

    /** The rune and amount in pouch slot [slot] (0-based), or null when it is empty. */
    fun slot(player: Player, slot: Int): Pair<String, Int>? {
        val amount = player.vars[quantityVarbits[slot]]
        val rune = runesByCompactId[player.vars[typeVarbits[slot]]]
        return if (rune != null && amount > 0) rune to amount else null
    }

    fun setSlot(player: Player, slot: Int, rune: String?, amount: Int) {
        val empty = rune == null || amount <= 0
        val type = if (empty) 0 else compactIds.getValue(rune!!)
        VarPlayerIntMapSetter.set(player, typeVarbits[slot], type)
        VarPlayerIntMapSetter.set(player, quantityVarbits[slot], if (empty) 0 else amount)
    }

    fun storedAmount(player: Player, rune: String): Int =
        (0 until 4).sumOf { i -> slot(player, i)?.takeIf { it.first == rune }?.second ?: 0 }

    /** The slot that holds [rune], else the first free one, else null (no free type slot). */
    fun slotFor(player: Player, rune: String, slots: Int = slotCount(player)): Int? {
        val held = (0 until slots).firstOrNull { slot(player, it)?.first == rune }
        return held ?: (0 until slots).firstOrNull { slot(player, it) == null }
    }

    /** Adds up to [amount] of [rune]; returns how many fit. */
    fun add(player: Player, rune: String, amount: Int, slots: Int = slotCount(player)): Int {
        val target = slotFor(player, rune, slots) ?: return 0
        val current = slot(player, target)?.second ?: 0
        val added = minOf(amount, CAPACITY - current)
        if (added > 0) {
            setSlot(player, target, rune, current + added)
        }
        return added.coerceAtLeast(0)
    }

    fun remove(player: Player, slot: Int, amount: Int): Int {
        val (rune, current) = slot(player, slot) ?: return 0
        val removed = minOf(amount, current)
        setSlot(player, slot, rune, current - removed)
        return removed
    }

    fun swap(player: Player, from: Int, into: Int) {
        val a = slot(player, from)
        val b = slot(player, into)
        setSlot(player, from, b?.first, b?.second ?: 0)
        setSlot(player, into, a?.first, a?.second ?: 0)
    }
}
