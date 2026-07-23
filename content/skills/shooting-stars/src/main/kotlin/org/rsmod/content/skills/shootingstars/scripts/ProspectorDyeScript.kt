package org.rsmod.content.skills.shootingstars.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeldU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ProspectorDyeScript @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        for ((plain, gold) in DYE_PAIRS) {
            onOpHeldU("obj.star_fragment", plain) { dye(plain, gold) }
        }
    }

    private fun ProtectedAccess.dye(plain: String, gold: String) {
        if (inv.count("obj.star_fragment") < 1 || inv.count(plain) < 1) {
            return
        }
        invDel(inv, "obj.star_fragment", 1)
        invDel(inv, plain, 1)
        invAdd(inv, gold, 1)
        mes("You use the star fragment to dye your prospector equipment.")
    }

    companion object {
        private val DYE_PAIRS =
            listOf(
                "obj.motherlode_reward_boots" to "obj.motherlode_reward_boots_gold",
                "obj.motherlode_reward_hat" to "obj.motherlode_reward_hat_gold",
                "obj.motherlode_reward_top" to "obj.motherlode_reward_top_gold",
                "obj.motherlode_reward_legs" to "obj.motherlode_reward_legs_gold",
            )
    }
}
