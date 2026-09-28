package org.rsmod.content.raids.toa.raid

import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player

/**
 * Completed raids per mode, in the cache's own varps (3645-3647; the client's combat
 * achievements read them). Cache varps persist by default, so nothing else is needed to save them.
 */
internal object ToaKillCount {
    private var Player.entry by intVarp("varp.total_completed_tombsofamascut_entry")
    private var Player.normal by intVarp("varp.total_completed_tombsofamascut")
    private var Player.expert by intVarp("varp.total_completed_tombsofamascut_expert")

    /**
     * Adds a completion in [mode] ("Entry", "Normal" or "Expert") and sends the count, e.g.
     * "Your completed Tombs of Amascut: Entry Mode count is: <col=ff0000>3</col>." Normal mode
     * has no suffix (RuneLite ChatCommandsPluginTest, from game messages).
     */
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

    /** "entry / normal / expert", as the party details list shows it. */
    fun summary(player: Player): String = "${player.entry} / ${player.normal} / ${player.expert}"

    /** ": Entry Mode" or ": Expert Mode"; nothing for Normal, as in the game's messages. */
    fun modeSuffix(mode: String): String = if (mode == NORMAL) "" else ": $mode Mode"

    private const val NORMAL = "Normal"
    private const val EXPERT = "Expert"
}
