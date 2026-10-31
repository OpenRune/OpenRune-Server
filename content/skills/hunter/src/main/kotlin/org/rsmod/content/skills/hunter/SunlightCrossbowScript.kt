package org.rsmod.content.skills.hunter

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
        openSkillMulti(menu) { selection ->
            repeat(selection.amount) {
                if (CHISEL !in inv || invDel(inv, ANTLER).failure) return@openSkillMulti
                if (invDel(inv, CROSSBOW).failure) {
                    invAdd(inv, ANTLER)
                    return@openSkillMulti
                }
                invAdd(inv, SUNLIGHT_CROSSBOW)
                mes("You create a Sunlight Hunters' Crossbow.")
            }
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
