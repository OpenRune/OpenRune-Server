package org.rsmod.content.skills.farming

import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onOpHeld1
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FarmingAshTomeScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(TOME) { event ->
            objbox(
                TOME,
                "The book contains the secrets to an upgraded fertile soil spell, " +
                    "only available to those with access to lunar spells.",
            )
            if (choice2("Yes", true, "No", false, title = "Do you wish to read further?")) {
                if (!unlock(player, event.slot)) return@onOpHeld1
                mesbox(
                    "You read the book and obtain the power to use an upgraded fertile soil spell. " +
                        "You must carry at least 2 volcanic ash in order to empower it.",
                )
            }
        }
    }

    internal companion object {
        private const val TOME = "obj.fossil_mine_ultrasoil_book"

        fun unlock(player: Player, slot: Int): Boolean {
            if (player.vars[FarmingSpellsScript.ULTRA_UNLOCK] == 1) return false
            if (player.inv[slot]?.id != TOME.asRSCM()) return false
            if (player.invDel(player.inv, TOME, slot = slot).failure) return false
            VarPlayerIntMapSetter.set(player, FarmingSpellsScript.ULTRA_UNLOCK, 1)
            return true
        }
    }
}
