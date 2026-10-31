package org.rsmod.content.skills.hunter

import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.table.hunter.HunterCustomFursRow
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class HunterFurClothingScript : PluginScript() {
    private val garments by lazy { HunterCustomFursRow.all().associateBy { it.garment.id } }

    override fun ScriptContext.startup() {
        onIfModalButton(CHOICES) {
            val obj = it.obj ?: return@onIfModalButton
            when (it.op) {
                IfButtonOp.Op1 -> value(obj)
                IfButtonOp.Op2 -> make(obj, 1)
                IfButtonOp.Op3 -> make(obj, 5)
                IfButtonOp.Op4 -> make(obj, 10)
                else -> Unit
            }
        }
    }

    private fun ProtectedAccess.value(obj: ItemServerType) {
        val garment = garments[obj.id] ?: return
        mes("${obj.name}: costs ${garment.fee} coins and ${garment.count} ${garment.furs.last().name.lowercase()}.")
    }

    private fun ProtectedAccess.make(obj: ItemServerType, amount: Int) {
        val garment = garments[obj.id] ?: return
        repeat(amount) { made ->
            if (!make(garment)) {
                if (made == 0) {
                    mes("You need ${garment.count} ${garment.furs.last().name.lowercase()} and ${garment.fee} coins for that.")
                }
                return
            }
        }
    }

    private fun ProtectedAccess.make(garment: HunterCustomFursRow): Boolean {
        val fur = garment.furs.firstOrNull { inv.count(it.internalName) >= garment.count }
        if (fur == null || inv.count(COINS) < garment.fee) return false
        val slot = inv.indices.first { inv[it]?.id == fur.id }
        if (invDel(inv, COINS, garment.fee).failure) return false
        if (invReplaceSlot(inv, slot, 1, garment.garment).failure) return false
        if (garment.count > 1) invDel(inv, fur.internalName, garment.count - 1)
        return true
    }

    private companion object {
        const val CHOICES = "component.hunting_customfurs:choices_items"
        const val COINS = "obj.coins"
    }
}
