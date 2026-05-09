package org.rsmod.content.skills.prayer.ecto

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.skills.prayer.items.ZealotRobes.shouldConsume
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class EctoWorshipEvents : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1("loc.ahoy_ectofuntus") { worshipEctofuntus() }
    }

    private suspend fun ProtectedAccess.worshipEctofuntus() {
        if (!inv.contains("obj.bucket_ectoplasm")) {
            doubleobjbox(
                "obj.bucket_ectoplasm",
                "obj.pot_bonemeal",
                "You need a bucket of slime and some bonemeal to do this.",
            )
            return
        }
        val recipe = ECTO_RECIPES.firstOrNull { inv.contains(it.bonemeal) }
        if (recipe == null) {
            doubleobjbox(
                "obj.bucket_ectoplasm",
                "obj.pot_bonemeal",
                "You need a bucket of slime and some bonemeal to do this.",
            )
            return
        }

        anim("seq.ahoy_prayer")

        val shouldConsumeInputs = player.shouldConsume()
        if (shouldConsumeInputs) {
            invDel(inv, "obj.bucket_ectoplasm", 1)
            invDel(inv, recipe.bonemeal, 1)
            invAdd(inv, "obj.bucket_empty", 1)
            invAdd(inv, "obj.pot_empty", 1)
        }

        statAdvance("stat.prayer", recipe.xp * 4.0)
        ectoTokens += 5
    }
}
