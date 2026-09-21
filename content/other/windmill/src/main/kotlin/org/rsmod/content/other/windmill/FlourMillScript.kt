package org.rsmod.content.other.windmill

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private var Player.hopperGrain by boolVarBit("varbit.windmill_hopper_grain")
private var Player.flourInBin by intVarBit("varbit.mill_flour")
private var Player.showFlour by intVarBit("varbit.mill_showflour")

class FlourMillScript @Inject constructor(private val locRepo: LocRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.hopper1") { fillHopper() }
        onOpLocU("loc.hopper1", "obj.grain") { fillHopper() }
        onOpLoc1("loc.hopperlevers1") { operateHopper(it.loc) }
        onOpLoc1("loc.millbase") { takeFlour() }
        onOpLocU("loc.millbase", "obj.pot_empty") { takeFlour() }
    }

    private suspend fun ProtectedAccess.fillHopper() {
        if (player.hopperGrain) {
            mes("There is already grain in the hopper.")
            return
        }
        if (!inv.contains(GRAIN)) {
            objbox(GRAIN, "You haven't got anything to fill the hopper with.")
            return
        }
        arriveDelay()
        anim("seq.qip_cook_hopper_grain")
        invDel(inv, GRAIN)
        player.hopperGrain = true
        delay(1)
        mes("You put the grain in the hopper. You should now pull the lever nearby to operate the hopper.")
    }

    private suspend fun ProtectedAccess.operateHopper(lever: BoundLocInfo) {
        arriveDelay()
        anim("seq.qip_cook_hopper_leaver")
        locRepo.change(lever, "loc.inactivehopperlevers", 2)
        if (!player.hopperGrain) {
            mes("You operate the empty hopper. Nothing interesting happens.")
            return
        }
        if (player.flourInBin >= MAX_FLOUR) {
            mes("The flour bin downstairs is full, I should empty it first.")
            return
        }
        player.hopperGrain = false
        player.flourInBin++
        player.showFlour = player.flourInBin
        mes("You operate the hopper. The grain slides down the chute.")
    }

    private fun ProtectedAccess.takeFlour() {
        if (player.flourInBin <= 0) {
            mes("The flour bin is already empty. You need to place wheat in the hopper upstairs first.")
            return
        }
        if (!inv.contains(POT_EMPTY)) {
            mes("You need an empty pot to hold the flour in.")
            return
        }
        invDel(inv, POT_EMPTY)
        invAdd(inv, POT_FLOUR)
        player.flourInBin--
        player.showFlour = player.flourInBin
        if (player.flourInBin == 0) {
            mes("You fill a pot with the last of the flour in the bin.")
        } else {
            mes("You fill a pot with flour from the bin.")
        }
    }

    private companion object {
        const val GRAIN = "obj.grain"
        const val POT_EMPTY = "obj.pot_empty"
        const val POT_FLOUR = "obj.pot_flour"
        const val MAX_FLOUR = 30
    }
}
