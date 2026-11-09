package org.rsmod.content.other.planksack

import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

object PlankSack {
    const val SACK = "obj.plank_sack"
    const val CAPACITY = 28

    class Plank(val obj: String, val varbit: String, val label: String)

    val planks =
        listOf(
            Plank("obj.woodplank", "varbit.plank_sack_plain", "Regular planks"),
            Plank("obj.plank_oak", "varbit.plank_sack_oak", "Oak planks"),
            Plank("obj.plank_teak", "varbit.plank_sack_teak", "Teak planks"),
            Plank("obj.plank_mahogany", "varbit.plank_sack_mahogany", "Mahogany planks"),
            Plank("obj.plank_camphor", "varbit.plank_sack_camphor", "Camphor planks"),
            Plank("obj.plank_ironwood", "varbit.plank_sack_ironwood", "Ironwood planks"),
            Plank("obj.plank_rosewood", "varbit.plank_sack_rosewood", "Rosewood planks"),
        )

    private val byObj = planks.associateBy { it.obj }

    fun plank(obj: String): Plank? = byObj[obj]

    fun hasSack(player: Player): Boolean = SACK in player.inv

    fun stored(player: Player, plank: Plank): Int = player.vars[plank.varbit]

    fun total(player: Player): Int = planks.sumOf { stored(player, it) }

    fun freeSpace(player: Player): Int = (CAPACITY - total(player)).coerceAtLeast(0)

    fun add(player: Player, plank: Plank, amount: Int): Int {
        val added = minOf(amount, freeSpace(player))
        if (added > 0) {
            VarPlayerIntMapSetter.set(player, plank.varbit, stored(player, plank) + added)
        }
        return added.coerceAtLeast(0)
    }

    fun remove(player: Player, plank: Plank, amount: Int): Int {
        val removed = minOf(amount, stored(player, plank)).coerceAtLeast(0)
        if (removed > 0) {
            VarPlayerIntMapSetter.set(player, plank.varbit, stored(player, plank) - removed)
        }
        return removed
    }
}
