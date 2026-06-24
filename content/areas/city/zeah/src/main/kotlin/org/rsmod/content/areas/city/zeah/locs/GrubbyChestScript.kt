package org.rsmod.content.areas.city.zeah.locs

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.flatten
import jakarta.inject.Inject
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.DropTableRegistry
import org.rsmod.api.droptable.rollCount
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private const val GRUBBY_KEY = "obj.hosdun_grubby_key"

private const val CHEST_LOC = "loc.hosdun_grubby_chest"
private const val VARBIT_CHEST = "varbit.hosdun_chest_status"
private const val VARP_KC = "varp.kc_grubby_chest"

class GrubbyChestScript @Inject constructor(
    private val objRepo: ObjRepository,
    private val dropRegistry: DropTableRegistry,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1(CHEST_LOC) { openChest() }
        onOpLoc2(CHEST_LOC) { checkCount() }
    }

    private suspend fun ProtectedAccess.openChest() {
        arriveDelay()

        if (invTotal(inv, GRUBBY_KEY) == 0) {
            mes("You don't have a key which can open this chest.")
            return
        }

        invDel(inv, GRUBBY_KEY)
        anim("seq.human_openchest")
        VarPlayerIntMapSetter.toggle(player, VARBIT_CHEST)
        mes("You find treasure and supplies within the chest.")

        VarPlayerIntMapSetter.set(player, VARP_KC, player.vars[VARP_KC] + 1)

        val table = dropRegistry.forLoc(CHEST_LOC) ?: return
        when (val result = table.roll(player, ArgMap()).flatten()) {
            is RollResult.Nothing -> Unit
            is RollResult.Single -> giveDrop(result.result)
            is RollResult.ListOf -> result.results.forEach { giveDrop(it) }
        }

        delay(1)
        VarPlayerIntMapSetter.toggle(player, VARBIT_CHEST)
    }

    private fun ProtectedAccess.checkCount() {
        val count = player.vars[VARP_KC]
        mes("You have opened the Grubby Chest $count ${if (count == 1) "time" else "times"}.")
    }

    private fun ProtectedAccess.giveDrop(drop: DropRollItem) {
        if (drop.isNothing || !drop.condition(player)) return
        val obj = drop.transformObj(player) ?: drop.obj
        invAddOrDrop(objRepo, obj, drop.rollCount(random))
    }
}
