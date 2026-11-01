package org.rsmod.content.skills.hunter

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.fletchingLvl
import org.rsmod.api.script.onOpHeldU
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.content.skills.skillMulti
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SunlightCrossbowScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeldU(ANTLER, CROSSBOW) { upgrade() }
    }

    private suspend fun ProtectedAccess.upgrade() {
        if (CHISEL !in inv) {
            mes("You need a chisel to do that.")
            return
        }
        if (player.fletchingLvl < LEVEL) {
            mes("You need a Fletching level of $LEVEL to do that.")
            return
        }
        val menu = skillMulti {
            verb("make")
            entry(SUNLIGHT_CROSSBOW) {
                material(ANTLER)
                material(CROSSBOW)
            }
        }
        openSkillMulti(menu) { selection -> create(selection.amount) }
    }

    private fun ProtectedAccess.create(amount: Int) {
        repeat(amount) {
            if (CHISEL !in inv) return
            val made =
                player.invTransaction(inv) {
                    val target = select(inv)
                    delete(target, ANTLER.asRSCM(RSCMType.OBJ), 1)
                    delete(target, CROSSBOW.asRSCM(RSCMType.OBJ), 1)
                    add(target, SUNLIGHT_CROSSBOW.asRSCM(RSCMType.OBJ), 1)
                }
            if (made.failure) return
            mes("You create a Sunlight Hunters' Crossbow.")
        }
    }

    private companion object {
        const val ANTLER = "obj.sunlight_antler"
        const val CROSSBOW = "obj.hunting_crossbow"
        const val SUNLIGHT_CROSSBOW = "obj.hunting_crossbow_sunlight"
        const val CHISEL = "obj.chisel"
        const val LEVEL = 74
    }
}
