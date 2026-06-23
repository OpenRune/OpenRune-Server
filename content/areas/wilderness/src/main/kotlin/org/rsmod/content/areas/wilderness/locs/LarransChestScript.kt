package org.rsmod.content.areas.wilderness.locs

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

private const val WILDERNESS_KEY = "obj.slayer_wilderness_key"

private const val SMALL_CHEST_CLOSED = "loc.slayer_larran_chest_small_closed"
private const val VARP_KC_SMALL = "varp.kc_larrans_small_chest"

private const val BIG_CHEST_CLOSED = "loc.slayer_larran_chest_big_closed"
private const val VARP_KC_BIG = "varp.kc_larrans_big_chest"

private const val VARBIT_CHEST = "varbit.brimstone_opening_konar_chest"

class LarransChestScript @Inject constructor(
    private val locRepo: LocRepository,
    private val objRepo: ObjRepository,
    private val dropRegistry: DropTableRegistry,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1(SMALL_CHEST_CLOSED) { openChest(it.loc, SMALL_CHEST_CLOSED, VARP_KC_SMALL) }
        onOpLoc2(SMALL_CHEST_CLOSED) { checkCount(VARP_KC_SMALL, "Larran's small chest") }
        onOpLoc1(BIG_CHEST_CLOSED) { openChest(it.loc, BIG_CHEST_CLOSED,  VARP_KC_BIG) }
        onOpLoc2(BIG_CHEST_CLOSED) { checkCount(VARP_KC_BIG, "Larran's big chest") }
    }

    private suspend fun ProtectedAccess.openChest(
        loc: BoundLocInfo,
        closedLocName: String,
        kcVarp: String,
    ) {
        arriveDelay()

        if (invTotal(inv, WILDERNESS_KEY) == 0) {
            mes("The chest is locked. You need a Larran's key to open it.")
            return
        }

        invDel(inv, WILDERNESS_KEY)
        anim("seq.human_openchest")
        VarPlayerIntMapSetter.toggle(player, VARBIT_CHEST)
        mes("You unlock the chest with your key.")

        VarPlayerIntMapSetter.set(player, kcVarp, player.vars[kcVarp] + 1)

        val table = dropRegistry.forLoc(closedLocName) ?: return
        when (val result = table.roll(player, ArgMap()).flatten()) {
            is RollResult.Nothing -> Unit
            is RollResult.Single -> giveDrop(result.result)
            is RollResult.ListOf -> result.results.forEach { giveDrop(it) }
        }

        delay(1)
        VarPlayerIntMapSetter.toggle(player, VARBIT_CHEST)
    }

    private fun ProtectedAccess.checkCount(kcVarp: String, chestName: String) {
        val count = player.vars[kcVarp]
        mes("You have opened $chestName $count ${if (count == 1) "time" else "times"}.")
    }

    private fun ProtectedAccess.giveDrop(drop: DropRollItem) {
        if (drop.isNothing || !drop.condition(player)) return
        val obj = drop.transformObj(player) ?: drop.obj
        invAddOrDrop(objRepo, obj, drop.rollCount(random))
    }
}
