package org.rsmod.content.skills.cooking

import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.invtx.InvCost
import org.rsmod.api.invtx.invConsumeAndAdd
import org.rsmod.api.player.events.skilling.SkillingActionCompleteEvent
import org.rsmod.api.player.events.skilling.SkillingActionContext
import org.rsmod.api.player.events.skilling.SkillingProductSource
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.cookingLvl
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.script.onOpHeldU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PotatoButterEvents : PluginScript() {
    override fun ScriptContext.startup() {
        BUTTERS.forEach { butter -> onOpHeldU(butter, POTATO) { butterPotato(butter) } }
    }

    private fun ProtectedAccess.butterPotato(butter: String) {
        if (player.cookingLvl < COOKING_LEVEL) {
            mes("You need a Cooking level of $COOKING_LEVEL to make a potato with butter.")
            return
        }
        val made =
            player.invConsumeAndAdd(
                inv = inv,
                consume = listOf(InvCost(butter.asRSCM(), 1), InvCost(POTATO.asRSCM(), 1)),
                obj = PRODUCT.asRSCM(),
                count = 1,
            )
        if (made.failure) {
            return
        }
        statAdvance("stat.cooking", EXPERIENCE)
        publish(
            SkillingActionCompleteEvent(
                player = player,
                context =
                    SkillingActionContext.Product(
                        skill = "stat.cooking",
                        item = PRODUCT,
                        count = 1,
                        experienceGranted = EXPERIENCE,
                        source = SkillingProductSource.PotatoTopping,
                    ),
            ),
        )
    }

    private companion object {
        const val POTATO = "obj.potato_baked"
        const val PRODUCT = "obj.potato_butter"
        const val COOKING_LEVEL = 39
        const val EXPERIENCE = 40.0

        val BUTTERS = listOf("obj.pot_of_butter", "obj.pot_of_not_garlic_butter")
    }
}
