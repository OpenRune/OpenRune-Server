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
import org.rsmod.api.table.fletching.HunterBoltFletchingRow
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.content.skills.skillMulti
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SunlightCrossbowScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeldU(ANTLER, CROSSBOW) { upgrade() }
        for (bolt in HunterBoltFletchingRow.all()) onOpHeldU(CHISEL, bolt.material.internalName) { fletch(bolt) }
    }

    private suspend fun ProtectedAccess.fletch(bolt: HunterBoltFletchingRow) {
        if (player.fletchingLvl < bolt.level) {
            mes("You need a Fletching level of ${bolt.level} to do that.")
            return
        }
        val menu = skillMulti {
            verb("make")
            entry(bolt.bolts.internalName) { material(bolt.material.internalName) }
        }
        openSkillMulti(menu) { selection -> bolts(bolt, selection.amount) }
    }

    private suspend fun ProtectedAccess.bolts(bolt: HunterBoltFletchingRow, amount: Int) {
        repeat(amount) {
            if (CHISEL !in inv || bolt.material.internalName !in inv) return
            anim(BOLT_SEQ)
            delay(bolt.ticks)
            if (CHISEL !in inv) return
            val made =
                player.invTransaction(inv) {
                    val target = select(inv)
                    delete(target, bolt.material.id, 1)
                    add(target, bolt.bolts.id, BOLTS_PER_MATERIAL)
                }
            if (made.failure) return
            statAdvance(FLETCHING, bolt.xpTenths / 10.0)
        }
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
        const val FLETCHING = "stat.fletching"
        const val BOLT_SEQ = "seq.human_fletching_huntingbolts_chisel"
        const val BOLTS_PER_MATERIAL = 12
    }
}
