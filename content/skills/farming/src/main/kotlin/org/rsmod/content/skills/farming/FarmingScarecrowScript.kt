package org.rsmod.content.skills.farming

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.farmingLvl
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FarmingScarecrowScript @Inject constructor(private val xpMods: XpModifiers) : PluginScript() {
    override fun ScriptContext.startup() {
        for (hay in listOf(
            "loc.haystack", "loc.haystack2", "loc.haystack3", "loc.haybail",
            "loc.hay_1", "loc.hay_2", "loc.hay_bale", "loc.hay_bale_stack",
            "loc.hay_bale_thin", "loc.hay_bale_stack_thin",
            "loc.shayzien_agility_haystack01", "loc.shayzien_agility_haystack02",
            "loc.hw22_bundle_hay01_bale01", "loc.hw22_bundle_hay01_bale02",
            "loc.hw22_bundle_hay01_stack01",
        )) {
            onOpLocU(hay, EMPTY_SACK) {
                if (invReplace(inv, EMPTY_SACK, 1, HAY_SACK).success) {
                    mes("You fill the sack with hay.")
                }
            }
        }
        onOpHeldU(HAY_SACK, "obj.bronze_spear") {
            assemble(HAY_SACK, it.firstSlot, "obj.bronze_spear", it.secondSlot, TORSO, false)
        }
        onOpHeldU(TORSO, "obj.watermelon") {
            assemble(TORSO, it.firstSlot, "obj.watermelon", it.secondSlot, FarmingScarecrow.SCARECROW, true)
        }
    }

    internal fun ProtectedAccess.assemble(
        first: String,
        firstSlot: Int,
        second: String,
        secondSlot: Int,
        output: String,
        complete: Boolean,
    ) {
        if (player.farmingLvl < 23) return mes("You need a Farming level of 23 to make a scarecrow.")
        val result = player.invTransaction(inv) {
            val inventory = select(inv)
            delete(inventory, first.asRSCM(RSCMType.OBJ), 1, firstSlot)
            delete(inventory, second.asRSCM(RSCMType.OBJ), 1, secondSlot)
            add(inventory, output.asRSCM(RSCMType.OBJ), 1)
        }
        if (result.failure) return
        if (complete) statAdvance(STAT_FARMING, 25.0 * xpMods.get(player, STAT_FARMING))
    }

    private companion object {
        const val EMPTY_SACK = "obj.sack_empty"
        const val HAY_SACK = "obj.scarecrow_torso"
        const val TORSO = "obj.scarecrow_torso_spear"
    }
}
