package org.rsmod.content.areas.city.taverley.locs

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
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private const val CRYSTAL_KEY = "obj.crystal_key"
private const val KEY_HALF_TOOTH = "obj.keyhalf1"
private const val KEY_HALF_LOOP = "obj.keyhalf2"

private const val CHEST_CLOSED = "loc.crystal_chestclosed"
private const val CHEST_OPEN = "loc.crystal_chestopen"
private const val CHEST_OPEN_TICKS = 2

private const val VARP_KC_CRYSTAL_CHEST = "varp.kc_crystal_chest"

class CrystalChestScript
@Inject
constructor(
    private val locRepo: LocRepository,
    private val objRepo: ObjRepository,
    private val dropRegistry: DropTableRegistry,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1(CHEST_CLOSED) { openChest(it.loc) }
        onOpHeldU(KEY_HALF_TOOTH, KEY_HALF_LOOP) { combineKeyHalves() }
    }

    private suspend fun ProtectedAccess.openChest(loc: BoundLocInfo) {
        arriveDelay()

        if (invTotal(inv, CRYSTAL_KEY) == 0) {
            mes("The chest is locked. You need a key to open it.")
            return
        }

        invDel(inv, CRYSTAL_KEY)
        anim("seq.human_openchest")
        locRepo.change(loc, CHEST_OPEN, CHEST_OPEN_TICKS)
        mes("You unlock the chest with your key.")

        VarPlayerIntMapSetter.set(
            player,
            VARP_KC_CRYSTAL_CHEST,
            player.vars[VARP_KC_CRYSTAL_CHEST] + 1,
        )

        val table = dropRegistry.forLoc(CHEST_CLOSED) ?: return
        when (val result = table.roll(player, ArgMap()).flatten()) {
            is RollResult.Nothing -> Unit
            is RollResult.Single -> giveDrop(result.result)
            is RollResult.ListOf -> result.results.forEach { giveDrop(it) }
        }
        delay(1)
    }

    private fun ProtectedAccess.giveDrop(drop: DropRollItem) {
        if (drop.isNothing || !drop.condition(player)) return
        val obj = drop.transformObj(player) ?: drop.obj
        invAddOrDrop(objRepo, obj, drop.rollCount(random))
    }

    private fun ProtectedAccess.combineKeyHalves() {
        invDel(inv, KEY_HALF_TOOTH, 1, KEY_HALF_LOOP, 1)
        invAddOrDrop(objRepo, CRYSTAL_KEY)
        mes("You join the two halves of the key together.")
    }
}
