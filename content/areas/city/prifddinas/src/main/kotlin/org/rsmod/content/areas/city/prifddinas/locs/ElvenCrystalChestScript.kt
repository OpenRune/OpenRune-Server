package org.rsmod.content.areas.city.prifddinas.locs

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

private const val PRIF_CRYSTAL_KEY = "obj.prif_crystal_key"

private const val CHEST_CLOSED = "loc.prif_crystal_chest_closed"
private const val CHEST_OPEN = "loc.prif_crystal_chest_open"
private const val CHEST_OPEN_TICKS = 1

private const val VARP_KC_ELVEN_CRYSTAL_CHEST = "varp.kc_elven_crystal_chest"

class ElvenCrystalChestScript @Inject constructor(
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

        if (invTotal(inv, PRIF_CRYSTAL_KEY) == 0) {
            mes("The chest is locked. You need a key to open it.")
            return
        }

        invDel(inv, PRIF_CRYSTAL_KEY)
        anim("seq.human_openchest")
        locRepo.change(loc, CHEST_OPEN, CHEST_OPEN_TICKS)
        mes("You unlock the chest with your key.")

        VarPlayerIntMapSetter.set(
            player,
            VARP_KC_ELVEN_CRYSTAL_CHEST,
            player.vars[VARP_KC_ELVEN_CRYSTAL_CHEST] + 1,
        )

        val table = dropRegistry.forLoc(CHEST_CLOSED) ?: return
        when (val result = table.roll(player, ArgMap()).flatten()) {
            is RollResult.Nothing -> Unit
            is RollResult.Single -> giveDrop(result.result)
            is RollResult.ListOf -> result.results.forEach { giveDrop(it) }
        }
    }

    private fun ProtectedAccess.checkCount() {
        val count = player.vars[VARP_KC_ELVEN_CRYSTAL_CHEST]
        mes("You have opened the Elven Crystal Chest $count ${if (count == 1) "time" else "times"}.")
    }

    private fun ProtectedAccess.giveDrop(drop: DropRollItem) {
        if (drop.isNothing || !drop.condition(player)) return
        val obj = drop.transformObj(player) ?: drop.obj
        invAddOrDrop(objRepo, obj, drop.rollCount(random))
    }
}
