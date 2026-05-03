package org.rsmod.api.combat.scripts.misc

import dev.openrune.types.ItemServerType
import dev.openrune.util.Wearpos
import org.rsmod.api.player.righthand
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.advanced.onWearposChange
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class DinhsBulwarkScript : PluginScript() {
    private var Player.passiveDelay by intVarp("varp.dinhs_passive_delay")

    override fun ScriptContext.startup() {
        onWearposChange { player.wearposChange(wearpos, objType) }
    }

    private fun Player.wearposChange(wearpos: Wearpos, obj: ItemServerType) {
        if (wearpos != Wearpos.RightHand || !obj.isCategoryType("category.dinhs_bulwark")) {
            return
        }

        val equipped = righthand.isType(obj)
        if (equipped) {
            spotanim("spotanim.dinhs_bulwark_glow_idle")
            passiveDelay = currentMapClock + 8
            return
        }
    }
}
