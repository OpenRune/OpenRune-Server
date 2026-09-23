package org.rsmod.content.areas.city.draynor

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DraynorCryptCandles @Inject constructor(private val random: GameRandom) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerSoftTimer(TIMER) { player.checkCandles() }
    }

    private fun Player.checkCandles() {
        if (!inCrypt(this)) {
            clearSoftTimer(TIMER)
            return
        }
        if (coords !in CANDLE_TILES) return
        mes("The candles on the floor burn your feet!")
        say(EXCLAMATIONS[random.of(0, EXCLAMATIONS.lastIndex)])
        soundSynth(SOUNDS[random.of(0, SOUNDS.lastIndex)], delay = 20)
    }

    companion object {
        const val TIMER = "timer.draynor_crypt_candles"
        const val INTERVAL = 8

        private val CANDLE_TILES =
            setOf(
                CoordGrid(3075, 9772, 0),
                CoordGrid(3075, 9777, 0),
                CoordGrid(3076, 9771, 0),
                CoordGrid(3076, 9772, 0),
                CoordGrid(3076, 9773, 0),
                CoordGrid(3077, 9772, 0),
                CoordGrid(3078, 9772, 0),
                CoordGrid(3079, 9771, 0),
                CoordGrid(3079, 9772, 0),
                CoordGrid(3079, 9773, 0),
            )
        private val EXCLAMATIONS = listOf("Ow!", "Eeek!", "Gah!", "Oooch!")
        private val SOUNDS =
            listOf(
                "synth.female_hit_1",
                "synth.female_hit_2",
                "synth.human_hit_1",
                "synth.human_hit_2",
                "synth.human_hit_3",
                "synth.human_hit_4",
            )

        fun inCrypt(player: Player): Boolean =
            player.coords.level == 0 && player.coords.x in 3070..3085 && player.coords.z in 9764..9780
    }
}
