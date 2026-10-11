package org.rsmod.content.other.containers

import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.invtx.*
import org.rsmod.api.player.events.interact.HeldBanksideEvents
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.craftingLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.script.*
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Full crates contain ten fish; a partial crate owns its remaining fish in its item vars. */
internal class FishCrateScript @Inject constructor(private val xp: XpModifiers) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(CAMPHOR) { mes("Add an ice cooler to make a fish crate for deep sea fish.") }
        onOpHeldU(CAMPHOR, COOLER) {
            if (inv[it.firstSlot]?.id != CAMPHOR.asRSCM() || inv[it.secondSlot]?.id != COOLER.asRSCM()) return@onOpHeldU
            player.invTransaction(inv) { val i = select(inv); delete(i, CAMPHOR.asRSCM(), 1); delete(i, COOLER.asRSCM(), 1); add(i, EMPTY.asRSCM(), 1) }
        }
        for (fish in fishTypes) {
            onOpHeldU(EMPTY, fish.fish) {
                val crate = inv[it.firstSlot] ?: return@onOpHeldU
                val source = inv[it.secondSlot] ?: return@onOpHeldU
                if (crate.id != EMPTY.asRSCM() || source.id != fish.fish.asRSCM()) return@onOpHeldU
                if (inv.physicalCount(fish.fish) < 10) { mes("You need ten fish to fill a crate."); return@onOpHeldU }
                val origin = coords
                delay(1)
                if (!alive(player) || coords != origin || inv[it.firstSlot] !== crate || inv[it.secondSlot] !== source) return@onOpHeldU
                player.invTransaction(inv) { val i = select(inv); delete(i, EMPTY.asRSCM(), 1, slot = it.firstSlot); delete(i, fish.fish.asRSCM(), 10); add(i, fish.full.asRSCM(), 1) }
            }
            for (form in listOf(fish.full, fish.partial)) {
                onOpHeld1(form) { empty(player, it.slot, fish, false) }
                onEvent<HeldBanksideEvents.Type>(form.asRSCM()) {
                    if (player.ui.containsModal("interface.bankmain") && player.ui.containsModal("interface.bankside")) empty(player, slot, fish, true)
                }
            }
        }
        for (tool in listOf("obj.hammer", "obj.chisel", "obj.imcando_hammer", "obj.imcando_hammer_offhand")) onOpHeldU("obj.camphor_logs", tool) {
            val log = inv[it.firstSlot] ?: return@onOpHeldU
            val heldTool = inv[it.secondSlot] ?: return@onOpHeldU
            if (log.id != "obj.camphor_logs".asRSCM() || heldTool.id != tool.asRSCM()) return@onOpHeldU
            if (!craftable()) return@onOpHeldU
            val origin = coords
            delay(2)
            if (!alive(player) || coords != origin || inv[it.firstSlot] !== log || inv[it.secondSlot] !== heldTool || !craftable()) return@onOpHeldU
            val result = player.invTransaction(inv) { val i = select(inv); delete(i, log.id, 1, slot = it.firstSlot); delete(i, "obj.nails_adamant".asRSCM(), 4); add(i, CAMPHOR.asRSCM(), 1) }
            if (result.success) statAdvance("stat.crafting", 50.0 * xp.get(player, "stat.crafting"))
        }
    }

    private fun ProtectedAccess.craftable(): Boolean {
        val hammer = listOf("obj.hammer", "obj.imcando_hammer", "obj.imcando_hammer_offhand").any { inv.physicalCount(it) > 0 || it != "obj.hammer" && it in player.worn }
        if (player.craftingLvl < 62 || !hammer || inv.physicalCount("obj.chisel") == 0 || inv.physicalCount("obj.nails_adamant") < 4) {
            mes("You need Crafting 62, a hammer, a chisel and four adamantite nails.")
            return false
        }
        return true
    }

    private fun empty(p: Player, slot: Int, fish: Fish, bank: Boolean) {
        if (!alive(p)) return
        val crate = p.inv[slot] ?: return
        val remaining = when (crate.id) { fish.full.asRSCM() -> 10; fish.partial.asRSCM() -> crate.vars.takeIf { it in 1..9 } ?: return; else -> return }
        // Try a complete withdrawal first. Partial results keep the same physical crate and never
        // erase fish when the last camphor crate cannot also fit in the inventory.
        for (amount in remaining downTo 1) {
            val result = p.invTransaction(p.inv, autoCommit = false) {
                val i = select(p.inv)
                delete(i, crate.id, 1, slot = slot)
                if (amount < remaining) add(i, fish.partial.asRSCM(), 1, vars = remaining - amount, slot = slot)
                add(i, fish.fish.asRSCM(), amount)
                if (amount == remaining && bank) add(i, CAMPHOR.asRSCM(), 1)
            }
            if (result.success) { result.commitAll(); return }
        }
        p.mes("Your inventory is too full to remove any fish.")
    }

    internal data class Fish(val fish: String, val full: String, val partial: String)
    internal companion object {
        const val CAMPHOR = "obj.camphor_crate"
        const val COOLER = "obj.slayer_icy_water"
        const val EMPTY = "obj.fish_crate_empty"
        val fishTypes = listOf("giant_krill", "haddock", "yellowfin", "halibut", "bluefin", "marlin").map { Fish("obj.raw_$it", "obj.fish_crate_$it", "obj.fish_crate_${it}_var") }
        private fun alive(p: Player) = p.hitpoints > 0 && !p.pendingLogout && !p.loggingOut
    }
}
