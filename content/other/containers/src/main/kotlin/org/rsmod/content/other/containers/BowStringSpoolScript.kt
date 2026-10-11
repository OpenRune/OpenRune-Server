package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.*
import org.rsmod.api.player.events.skilling.*
import org.rsmod.api.player.stat.fletchingLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.*
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class BowStringSpoolScript @Inject constructor(private val storage: PlayerItemStorage, private val xp: XpModifiers) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(BowStringSpool.ITEM) {
            if (inv[it.slot]?.id != BowStringSpool.ITEM.asRSCM()) return@onOpHeld1
            val ordinary = BowStringSpool.STRING.asRSCM()
            BowStringSpool.fill(player, ordinary)
            BowStringSpool.fill(player, ServerCacheManager.getItem(ordinary)!!.certlink)
        }
        onOpHeld2(BowStringSpool.ITEM) { mes("Your spool holds ${BowStringSpool.count(player)} / ${BowStringSpool.capacity(player)} bow strings.") }
        onOpHeld3(BowStringSpool.ITEM) {
            val spool = inv[it.slot] ?: return@onOpHeld3
            val option = menu("Withdraw bow strings", "Strings", "Notes", "Cancel", hotkeys = true)
            if (option !in 0..1 || inv[it.slot] !== spool) return@onOpHeld3
            val amount = countDialog()
            if (amount > 0 && inv[it.slot] === spool) BowStringSpool.withdraw(player, option == 1, amount)
        }
        onOpHeldU(BowStringSpool.ITEM, BowStringSpool.ITEM) {
            if (it.firstSlot == it.secondSlot || inv[it.firstSlot]?.id != BowStringSpool.ITEM.asRSCM() || inv[it.secondSlot]?.id != BowStringSpool.ITEM.asRSCM()) return@onOpHeldU
            val tier = BowStringSpool.tier(player)
            if (tier == 5) { mes("Your spool is already at maximum capacity."); return@onOpHeldU }
            if (player.invDel(inv, BowStringSpool.ITEM.asRSCM(), 1, slot = it.secondSlot).success) {
                VarPlayerIntMapSetter.set(player, BowStringSpool.SIZE, tier + 1)
                mes("Your spool can now hold ${BowStringSpool.capacity(player)} bow strings.")
            }
        }
        onOpHeldU(BowStringSpool.ITEM) {
            if (inv[it.firstSlot]?.id != BowStringSpool.ITEM.asRSCM() || inv[it.secondSlot]?.id != it.second.id) return@onOpHeldU
            BowStringSpool.fill(player, it.second.id)
        }
        for (recipe in recipes) onOpHeldU(BowStringSpool.ITEM, recipe.input) {
            val spoolSlot = it.firstSlot
            var spool = inv[spoolSlot] ?: return@onOpHeldU
            if (spool.id != BowStringSpool.ITEM.asRSCM() || inv[it.secondSlot]?.id != recipe.input.asRSCM()) return@onOpHeldU
            if (player.fletchingLvl < recipe.level) { mes("You need Fletching ${recipe.level} to string this bow."); return@onOpHeldU }
            if (BowStringSpool.count(player) == 0) return@onOpHeldU
            val amount = countDialog()
            val origin = coords
            try {
                repeat(amount.coerceIn(0, 27)) {
                    if (inv[spoolSlot] !== spool || coords != origin || player.hitpoints == 0 || player.pendingLogout || player.loggingOut || player.fletchingLvl < recipe.level || inv.physicalCount(recipe.input) == 0 || BowStringSpool.count(player) == 0) return@onOpHeldU
                    anim(recipe.animation); delay(3)
                    if (inv[spoolSlot] !== spool || coords != origin || player.hitpoints == 0 || player.pendingLogout || player.loggingOut || player.fletchingLvl < recipe.level || inv.physicalCount(recipe.input) == 0 || BowStringSpool.count(player) == 0) return@onOpHeldU
                    val plan = storage.planTransfer(player, inv, BowStringSpool.STRING, 1, incoming = false)
                    if (plan.amount != 1) return@onOpHeldU
                    val raw = player.invTransaction(inv, autoCommit = false) {
                        val i = select(inv); delete(i, recipe.input.asRSCM(), 1); add(i, recipe.output.asRSCM(), 1)
                    }
                    if (raw.failure || !plan.valid()) return@onOpHeldU
                    raw.copy(validateCommit = { plan.valid() }, afterCommit = { plan.commit() }).commitAll()
                    spool = inv[spoolSlot] ?: return@onOpHeldU
                    val award = recipe.xp * xp.get(player, "stat.fletching")
                    statAdvance("stat.fletching", award)
                    publish(SkillingActionCompleteEvent(player, SkillingActionContext.Product("stat.fletching", recipe.output, 1, award, SkillingProductSource.Fletching)))
                }
            } finally { resetAnim() }
        }
        onEvent<SkillingActionCompleteEvent> {
            val result = context as? SkillingActionContext.Product ?: return@onEvent
            if (result.source == SkillingProductSource.Crafting && result.item == BowStringSpool.STRING && !result.isBonus) BowStringSpool.fill(player, BowStringSpool.STRING.asRSCM(), result.count)
        }
    }
    data class Recipe(val input: String, val output: String, val level: Int, val xp: Double, val animation: String)
    companion object {
        val recipes = listOf(
            "shortbow" to (5 to 5.0), "longbow" to (10 to 10.0),
            "oak_shortbow" to (20 to 16.5), "oak_longbow" to (25 to 25.0),
            "willow_shortbow" to (35 to 33.2), "willow_longbow" to (40 to 41.5),
            "maple_shortbow" to (50 to 50.0), "maple_longbow" to (55 to 58.2),
            "yew_shortbow" to (65 to 67.5), "yew_longbow" to (70 to 75.0),
            "magic_shortbow" to (80 to 83.2), "magic_longbow" to (85 to 91.5),
        ).map { (name, values) -> Recipe("obj.unstrung_$name", "obj.$name", values.first, values.second, "seq.stringing_$name") }
    }
}
