package org.rsmod.content.other.treasuretrails

import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

/**
 * Jagex's own master clue varbits, which the client's clue helper reads: a flag for a step that is
 * ready to be completed (Uri or the double agent overlay) and the kind of puzzle box handed out.
 */
object MasterVars {
    private const val READY = "varbit.master_clue_progress"
    private const val BOX = "varbit.master_clue_puzzle"
    private const val REMAINING = "varbit.trail_length_master"

    const val LIGHT_BOX = 1
    const val ZULRAH_BOX = 2
    const val CERBERUS_BOX = 3
    const val GNOME_CHILD_BOX = 4
    const val TOB_BOX = 5

    fun setReady(player: Player, ready: Boolean) {
        VarPlayerIntMapSetter.set(player, READY, if (ready) 1 else 0)
    }

    fun setBox(player: Player, box: Int) {
        VarPlayerIntMapSetter.set(player, BOX, box)
    }

    fun setRemaining(player: Player, remaining: Int) {
        VarPlayerIntMapSetter.set(player, REMAINING, remaining)
    }

    fun clear(player: Player) {
        setReady(player, false)
        setBox(player, 0)
    }
}
