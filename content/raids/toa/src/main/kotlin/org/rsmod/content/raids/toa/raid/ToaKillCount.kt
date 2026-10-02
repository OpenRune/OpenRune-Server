package org.rsmod.content.raids.toa.raid

import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player

internal object ToaKillCount {
    private var Player.entry by intVarp("varp.total_completed_tombsofamascut_entry")
    private var Player.normal by intVarp("varp.total_completed_tombsofamascut")
    private var Player.expert by intVarp("varp.total_completed_tombsofamascut_expert")

    fun record(player: Player, mode: String) {
        val count =
            when (mode) {
                EXPERT -> ++player.expert
                NORMAL -> ++player.normal
                else -> ++player.entry
            }
        val name = "Tombs of Amascut${modeSuffix(mode)}"
        player.mes("Your completed $name count is: <col=ff0000>$count</col>.")
    }

    fun normalAndExpert(player: Player): Int = player.normal + player.expert

    fun summary(player: Player): String = "${player.entry} / ${player.normal} / ${player.expert}"

    fun modeSuffix(mode: String): String = if (mode == NORMAL) "" else ": $mode Mode"

    private const val NORMAL = "Normal"
    private const val EXPERT = "Expert"
}
