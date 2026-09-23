package org.rsmod.content.areas.city.draynor

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.map.CoordGrid
import org.rsmod.map.util.Translation
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DraynorManorStairs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(StairsUp) { climb(StairsUpTranslation) }
        onOpLoc1(StairsDown) { climb(StairsDownTranslation) }
        onOpLoc1(SpiralStairs) { climbSpiral() }

        onOpLoc1(BasementLadderTop) { descendToBasement() }
        onOpLoc1(BasementLadder) { climbFromBasement() }

        onOpLoc1(CryptStairsDown) { enterCrypt() }
        onOpLoc1(CryptStairsUp) { leaveCrypt() }
    }

    private suspend fun ProtectedAccess.enterCrypt() {
        arriveDelay()
        telejump(CryptCoord.translateX(cryptColumnOffset(CryptEntryX)))
    }

    private suspend fun ProtectedAccess.leaveCrypt() {
        arriveDelay()
        telejump(CryptExitCoord.translateX(cryptColumnOffset(CryptCoord.x)))
    }

    // Both staircases are two columns wide; preserve the approach column in the landing.
    private fun ProtectedAccess.cryptColumnOffset(westColumnX: Int): Int =
        (player.coords.x - westColumnX).coerceIn(0, 1)

    private suspend fun ProtectedAccess.climb(translation: Translation) {
        arriveDelay()
        telejump(player.coords.translate(translation))
    }

    private suspend fun ProtectedAccess.climbSpiral() {
        arriveDelay()
        val translation =
            if (player.coords.level >= SpiralTopLevel) SpiralDownTranslation
            else SpiralUpTranslation
        telejump(player.coords.translate(translation))
    }

    private suspend fun ProtectedAccess.descendToBasement() {
        arriveDelay()
        anim(ClimbAnim)
        delay(1)
        telejump(BasementCoord)
    }

    private suspend fun ProtectedAccess.climbFromBasement() {
        arriveDelay()
        anim(ClimbAnim)
        delay(1)
        telejump(ManorGroundCoord)
        mes("You climb up the ladder.")
    }

    private companion object {
        private const val StairsUp = "loc.draynor_manor_stairs_up"
        private const val StairsDown = "loc.draynor_manor_stairs_down"
        private const val SpiralStairs = "loc.draynor_spiralstairs"
        private const val BasementLadderTop = "loc.puzzle_ladder_top"
        private const val BasementLadder = "loc.puzzle_ladder"

        private const val ClimbAnim = "seq.human_reachforladder"

        private val StairsUpTranslation = Translation(x = 0, z = 5, level = 1)
        private val StairsDownTranslation = Translation(x = 0, z = -5, level = -1)
        private val SpiralUpTranslation = Translation(x = -1, z = 1, level = 1)
        private val SpiralDownTranslation = Translation(x = 1, z = -1, level = -1)

        private const val SpiralTopLevel = 2

        private val BasementCoord = CoordGrid(3117, 9753, 0)
        private val ManorGroundCoord = CoordGrid(3092, 3361, 0)

        private const val CryptStairsDown = "loc.cryptstairsdown"
        private const val CryptStairsUp = "loc.cryptstairsup"

        private val CryptCoord = CoordGrid(3077, 9771, 0)
        private val CryptExitCoord = CoordGrid(3115, 3356, 0)

        private const val CryptEntryX = 3115
    }
}
