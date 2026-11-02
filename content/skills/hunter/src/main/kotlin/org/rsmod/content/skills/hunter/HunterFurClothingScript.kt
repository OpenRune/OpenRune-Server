package org.rsmod.content.skills.hunter

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
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
                IfButtonOp.Op5 -> mes(obj.examine)
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
            if (!trade(garment)) {
                if (made == 0) {
                    mes("You need ${garment.count} ${garment.furs.last().name.lowercase()} and ${garment.fee} coins for that.")
                }
                return
            }
        }
    }

    private fun ProtectedAccess.trade(garment: HunterCustomFursRow): Boolean {
        val fur = garment.furs.firstOrNull { inv.count(it.internalName) >= garment.count } ?: return false
        return player
            .invTransaction(inv) {
                val target = select(inv)
                delete(target, COINS.asRSCM(RSCMType.OBJ), garment.fee)
                delete(target, fur.id, garment.count)
                add(target, garment.garment.id, 1)
            }
            .success
    }

    private companion object {
        const val CHOICES = "component.hunting_customfurs:choices_items"
        const val COINS = "obj.coins"
    }
}
