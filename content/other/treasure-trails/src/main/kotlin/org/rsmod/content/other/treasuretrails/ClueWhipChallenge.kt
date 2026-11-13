package org.rsmod.content.other.treasuretrails

import dev.openrune.util.Wearpos
import org.rsmod.api.script.advanced.onWearposChange
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ClueWhipChallenge : PluginScript() {
    override fun ScriptContext.startup() {
        onWearposChange {
            if (wearpos != Wearpos.RightHand) return@onWearposChange
            val worn = player.worn[Wearpos.RightHand.slot] ?: return@onWearposChange
            if (WHIPS.none { worn.isType(it) }) return@onWearposChange
            if (inDemonArea(player)) {
                ClueChallengeTasks.complete(player, ClueChallengeTasks.EQUIP_WHIP)
            }
        }
    }

    private fun inDemonArea(player: Player): Boolean {
        val coords = player.coords
        return coords.level == DEMON_LEVEL && coords.x in DEMON_X && coords.z in DEMON_Z
    }

    private companion object {
        const val DEMON_LEVEL = 2
        val DEMON_X = 3405..3430
        val DEMON_Z = 3555..3580
        val WHIPS =
            listOf(
                "obj.abyssal_whip",
                "obj.abyssal_whip_ice",
                "obj.abyssal_whip_lava",
                "obj.abyssal_tentacle",
            )
    }
}
