package org.rsmod.content.quest.area.varrock.gertrudescat

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.quest.area.varrock.gertrudescat.GertrudesCatQuest.Companion.DOOGLE_LEAVES
import org.rsmod.content.quest.area.varrock.gertrudescat.GertrudesCatQuest.Companion.RAW_SARDINE
import org.rsmod.content.quest.area.varrock.gertrudescat.GertrudesCatQuest.Companion.SEASONED_SARDINE
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DoogleSardines : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeldU(RAW_SARDINE, DOOGLE_LEAVES) { seasonSardine() }
        onOpLoc1(EMPTY_CRATE) { searchEmpty("crate") }
        onOpLoc1(EMPTY_BARREL) { searchEmpty("barrel") }
    }

    private fun ProtectedAccess.seasonSardine() {
        val seasoned =
            player.invTransaction(inv) {
                val inventory = select(inv)
                delete {
                    from = inventory
                    obj = RAW_SARDINE.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
                delete {
                    from = inventory
                    obj = DOOGLE_LEAVES.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
                insert {
                    into = inventory
                    obj = SEASONED_SARDINE.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
            }
        if (seasoned.failure) {
            return
        }
        anim("seq.human_pickuptable")
        mes("You rub the doogle leaves over the sardine.")
    }

    private suspend fun ProtectedAccess.searchEmpty(what: String) {
        arriveDelay()
        anim("seq.human_pickuptable")
        mes("You search the $what but find nothing.")
    }

    private companion object {
        const val EMPTY_CRATE = "loc.gertrudeempty_crate"
        const val EMPTY_BARREL = "loc.gertrudeempty_barrel"
    }
}
