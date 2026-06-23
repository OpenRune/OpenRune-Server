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
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private const val BRIMSTONE_KEY = "obj.konar_key"

private const val CHEST_CLOSED = "loc.brimstone_konar_chest_closed"

private const val VARP_KC = "varp.kc_brimstone_chest"
private const val VARBIT_CHEST = "varbit.brimstone_opening_konar_chest"

class BrimstoneChestScript @Inject constructor(
    private val locRepo: LocRepository,
    private val objRepo: ObjRepository,
    private val dropRegistry: DropTableRegistry,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1(CHEST_CLOSED) { openChest(it.loc) }
        onOpLoc2(CHEST_CLOSED) { checkCount() }
    }

    private suspend fun ProtectedAccess.openChest(loc: BoundLocInfo) {
        arriveDelay()

        if (invTotal(inv, BRIMSTONE_KEY) == 0) {
            mes("The chest is locked. You need a brimstone key to open it.")
            return
        }

        invDel(inv, BRIMSTONE_KEY)
        anim("seq.human_openchest")
        VarPlayerIntMapSetter.toggle(player, VARBIT_CHEST)
        mes("You unlock the chest with your key.")
        VarPlayerIntMapSetter.set(player, VARP_KC, player.vars[VARP_KC] + 1)

        val table = dropRegistry.forLoc(CHEST_CLOSED) ?: return
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
        mes("You have opened the Brimstone chest $count ${if (count == 1) "time" else "times"}.")
    }

    private fun ProtectedAccess.giveDrop(drop: DropRollItem) {
        if (drop.isNothing || !drop.condition(player)) return
        val obj = drop.transformObj(player) ?: drop.obj
        invAddOrDrop(objRepo, obj, drop.rollCount(random))
    }
}
