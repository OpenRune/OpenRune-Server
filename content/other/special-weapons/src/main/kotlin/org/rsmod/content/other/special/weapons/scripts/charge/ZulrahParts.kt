package org.rsmod.content.other.special.weapons.scripts.charge

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.utils.bits.getBits
import org.rsmod.utils.bits.withBits

class ZulrahParts : PluginScript() {
    override fun ScriptContext.startup() {
        for (symbol in scaleParts) onOpHeld4(symbol) {
            dismantle(it.inventory, it.slot, listOf(InvObj(SCALES, 20_000)), "Dismantle for 20,000 Zulrah's scales?")
        }
        for ((toxic, base) in detachable) onOpHeld4(toxic) {
            dismantle(it.inventory, it.slot, listOf(InvObj(base), InvObj("obj.magic_fang")), "Remove the magic fang?")
        }
        for ((toxic, base) in ornamentedTridents) {
            onOpHeld5(toxic) {
                dismantle(it.inventory, it.slot, listOf(InvObj(base), InvObj("obj.magic_fang")), "Remove the magic fang and keep the ornamented trident?")
            }
            onOpHeld5(toxic.replace("_uncharged", "_charged")) { mes("Uncharge your trident before dismantling it.") }
        }
        onOpHeld4("obj.toxic_blowpipe_ornament") {
            dismantle(it.inventory, it.slot, listOf(InvObj("obj.toxic_blowpipe"), InvObj("obj.toxic_blowpipe_ornament_kit")), "Remove the ornament kit?")
        }
        for ((empty, charged) in scaleEquipment) {
            for (symbol in listOf(empty, charged)) onOpHeldU(symbol, SCALES) {
                if (!charge(player, inv, it.firstSlot, it.secondSlot)) mes("Unable to add scales. Your items have not changed.")
            }
            onOpHeld3(charged) { mes("This item contains ${scales(it.inventory[it.slot]!!)} Zulrah's scales.") }
            onOpHeld5(charged) {
                val original = it.inventory[it.slot] ?: return@onOpHeld5
                if (!choice2("Uncharge.", true, "Cancel.", false, title = "Recover the remaining scales?")) return@onOpHeld5
                if (!uncharge(player, it.inventory, it.slot, original)) mes("You need more inventory space.")
            }
        }
        for ((colour, mutagen) in listOf("cyan" to "obj.cyan_mutagen", "red" to "obj.red_mutagen")) {
            for (charged in listOf(false, true)) {
                val base = "obj.serpentine_helm${if (charged) "_charged" else ""}"
                val coloured = "${base}_$colour"
                onOpHeldU(base, mutagen) {
                    val helm = inv[it.firstSlot] ?: return@onOpHeldU
                    if (!choice2("Apply.", true, "Cancel.", false, title = "Apply this mutagen? It will be consumed.")) return@onOpHeldU
                    if (!mutate(player, inv, it.firstSlot, it.secondSlot, helm, mutagen, coloured)) mes("Your items have not changed.")
                }
                onOpHeld4(coloured) {
                    val original = it.inventory[it.slot] ?: return@onOpHeld4
                    if (!choice2("Restore.", true, "Cancel.", false, title = "Restore the serpentine helm? The mutagen will be lost.")) return@onOpHeld4
                    if (!replace(player, it.inventory, it.slot, original, listOf(original.copy(id = base.asRSCM())))) mes("Your helm has not changed.")
                }
            }
        }
    }

    private suspend fun ProtectedAccess.dismantle(inventory: Inventory, slot: Int, outputs: List<InvObj>, title: String) {
        val original = inventory[slot] ?: return
        if (!choice2("Yes.", true, "No.", false, title = title)) return
        if (!replace(player, inventory, slot, original, outputs)) mes("You need more inventory space, or the item has changed.")
    }

    internal companion object {
        const val SCALES = "obj.snakeboss_scale"
        val scaleParts = listOf("obj.blowpipe_fang", "obj.magic_fang", "obj.serpentine_visage", "obj.serpentine_helm", "obj.toxic_blowpipe")
        val detachable = listOf("obj.toxic_tots_uncharged" to "obj.tots_uncharged", "obj.toxic_tots_i_uncharged" to "obj.tots_i_uncharged", "obj.toxic_sotd" to "obj.sotd")
        val ornamentedTridents = listOf("obj.toxic_tots_uncharged_orn" to "obj.tots_uncharged_orn", "obj.toxic_tots_i_uncharged_orn" to "obj.tots_i_uncharged_orn")
        val scaleEquipment = listOf("", "_cyan", "_red").map { "obj.serpentine_helm$it" to "obj.serpentine_helm_charged$it" } + ("obj.toxic_sotd" to "obj.toxic_sotd_charged")
        private val bits get() = checkNotNull(ServerCacheManager.getVarObj("varobj.charges_16383".asRSCM())).bits
        fun scales(item: InvObj): Int = if (scaleEquipment.any { it.second.asRSCM() == item.id }) item.vars.getBits(bits).coerceIn(0, 11_000) else 0
        fun replace(player: Player, inventory: Inventory, slot: Int, original: InvObj, outputs: List<InvObj>): Boolean {
            if (inventory[slot] !== original) return false
            return player.invTransaction(inventory) {
                val target = select(inventory)
                delete(target, original.id, 1, slot)
                for (output in outputs) add(target, output.id, output.count, output.vars)
            }.success
        }
        fun mutate(player: Player, inventory: Inventory, slot: Int, reagentSlot: Int, original: InvObj, reagent: String, output: String): Boolean {
            if (inventory[slot] !== original || inventory[reagentSlot]?.id != reagent.asRSCM()) return false
            return player.invTransaction(inventory) {
                val target = select(inventory)
                delete(target, original.id, 1, slot)
                delete(target, reagent.asRSCM(), 1, reagentSlot)
                add(target, output.asRSCM(), 1, original.vars, slot)
            }.success
        }
        fun charge(player: Player, inventory: Inventory, slot: Int, scaleSlot: Int): Boolean {
            val original = inventory[slot] ?: return false
            val pair = scaleEquipment.firstOrNull { original.id == it.first.asRSCM() || original.id == it.second.asRSCM() } ?: return false
            val resource = inventory[scaleSlot] ?: return false
            if (resource.id != SCALES.asRSCM()) return false
            val amount = minOf(resource.count, 11_000 - scales(original))
            if (amount <= 0) return false
            return player.invTransaction(inventory) {
                val target = select(inventory)
                delete(target, resource.id, amount, scaleSlot)
                delete(target, original.id, 1, slot)
                add(target, pair.second.asRSCM(), 1, original.vars.withBits(bits, scales(original) + amount), slot)
            }.success
        }
        fun uncharge(player: Player, inventory: Inventory, slot: Int, original: InvObj): Boolean {
            val pair = scaleEquipment.firstOrNull { original.id == it.second.asRSCM() } ?: return false
            val outputs = mutableListOf(original.copy(id = pair.first.asRSCM(), vars = original.vars.withBits(bits, 0)))
            if (scales(original) > 0) outputs += InvObj(SCALES, scales(original))
            return replace(player, inventory, slot, original, outputs)
        }
    }
}
