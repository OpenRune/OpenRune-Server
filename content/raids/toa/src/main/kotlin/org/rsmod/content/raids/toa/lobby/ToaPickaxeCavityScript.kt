package org.rsmod.content.raids.toa.lobby

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ToaPickaxeCavityScript : PluginScript() {
    private var Player.storedPickaxe by intVarBit(LobbyCavity.STORED)

    override fun ScriptContext.startup() {
        for (loc in LobbyCavity.LOCS) {
            onOpLoc1(loc) { takePickaxe() }
            onOpLoc2(loc) { depositBest() }
            onOpLocU(loc) { depositUsed(it.invSlot) }
        }
    }

    private fun ProtectedAccess.takePickaxe() {
        val obj = ToaPickaxeStorage.obj(player.storedPickaxe) ?: return
        if (heldPickaxes().isNotEmpty()) {
            mes(LobbyCavity.ALREADY_HELD)
            return
        }
        if (!inv.hasFreeSpace()) {
            mes(LobbyCavity.NO_SPACE)
            return
        }
        anim(LobbyCavity.ANIM)
        player.storedPickaxe = 0
        invAdd(inv, obj)
        mes(LobbyCavity.TAKEN.format(nameOf(obj)))
    }

    private fun ProtectedAccess.depositBest() {
        val held = heldPickaxes()
        if (held.isEmpty()) {
            mes(LobbyCavity.NOTHING_TO_DEPOSIT)
            return
        }
        val best = held.maxBy { ToaPickaxeStorage.value(it.obj) }
        store(best)
    }

    private fun ProtectedAccess.depositUsed(slot: Int) {
        val used = held(inv, slot)
        if (used == null) {
            mes(LobbyCavity.NOTHING_INTERESTING)
            return
        }
        store(used)
    }

    private fun ProtectedAccess.store(held: Held) {
        if (player.storedPickaxe != 0) {
            mes(LobbyCavity.ALREADY_STORED)
            return
        }
        val value = ToaPickaxeStorage.value(held.obj)
        if (value == 0) {
            mes(
                if (held.obj == LobbyCavity.BRONZE) {
                    LobbyCavity.BRONZE_REFUSED
                } else {
                    LobbyCavity.NOTHING_INTERESTING
                }
            )
            return
        }
        anim(LobbyCavity.ANIM)
        invDel(held.inv, held.obj, slot = held.slot)
        player.storedPickaxe = value
        mes(LobbyCavity.PLACED.format(nameOf(held.obj)))
    }

    private fun ProtectedAccess.heldPickaxes(): List<Held> =
        listOf(worn, inv).flatMap { from -> from.indices.mapNotNull { held(from, it) } }

    private fun held(from: Inventory, slot: Int): Held? {
        val type = from[slot]?.let { getInvObj(it) } ?: return null
        return if (type.isContentType(LobbyCavity.PICKAXE_CONTENT)) {
            Held(from, slot, type.internalName)
        } else {
            null
        }
    }

    private fun nameOf(obj: String): String =
        ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ))?.name ?: obj

    private class Held(val inv: Inventory, val slot: Int, val obj: String)
}
