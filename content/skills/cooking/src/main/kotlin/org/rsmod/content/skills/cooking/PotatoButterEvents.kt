package org.rsmod.content.skills.cooking

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.ItemServerType
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

/**
 * A potato with butter is made by using a pat of butter on a baked potato. No fire or range is
 * needed, and the level is checked before anything is consumed.
 */
class PotatoButterEvents : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeldU(BUTTER, POTATO) { butterPotato(it.first) }
    }

    private fun ProtectedAccess.butterPotato(butter: ItemServerType) {
        if (player.cookingLvl < COOKING_LEVEL) {
            mes("You need a Cooking level of $COOKING_LEVEL to make a potato with butter.")
            return
        }
        val made =
            player.invConsumeAndAdd(
                inv = inv,
                consume = listOf(InvCost(BUTTER.asRSCM(), 1), InvCost(POTATO.asRSCM(), 1)),
                obj = PRODUCT.asRSCM(),
                count = 1,
            )
        if (made.failure) {
            return
        }
        statAdvance("stat.cooking", EXPERIENCE)
        // UNVERIFIED: the message is not in any capture.
        mes("You spread the butter over the potato.")
        publish(
            SkillingActionCompleteEvent(
                player = player,
                context =
                    SkillingActionContext.Product(
                        skill = "stat.cooking",
                        item = PRODUCT,
                        count = 1,
                        experienceGranted = EXPERIENCE,
                        source = SkillingProductSource.Combine(butter),
                    ),
            ),
        )
    }

    private companion object {
        const val BUTTER = "obj.pot_of_butter"
        const val POTATO = "obj.potato_baked"
        const val PRODUCT = "obj.potato_butter"
        const val COOKING_LEVEL = 39
        const val EXPERIENCE = 40.0
    }
}
