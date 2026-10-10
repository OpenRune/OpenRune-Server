package org.rsmod.content.areas.revenants

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc5
import org.rsmod.api.script.onOpNpcU
import org.rsmod.content.areas.wilderness.applyVoluntarySkull
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class RevenantTraderScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc5("npc.wild_cave_trader") {
            mesbox("A PK skull means you drop ALL unprotected items on death.")
            if (choice2("Give me a PK skull", true, "Cancel", false)) player.applyVoluntarySkull()
        }
        onOpNpc1("npc.wild_cave_trader") { exchangeAll() }
        onOpNpc3("npc.wild_cave_trader") { exchangeAll() }
        for (artefact in AncientArtefact.entries) for (item in listOf(artefact.item, "obj.cert_${artefact.item.removePrefix("obj.")}")) {
            onOpNpcU(checkNotNull(ServerCacheManager.getNpc("npc.wild_cave_trader".asRSCM())), checkNotNull(ServerCacheManager.getItem(item.asRSCM()))) { exchangeAll() }
        }
    }
    private suspend fun ProtectedAccess.exchangeAll() {
        val items = carried(player)
        val value = total(items)
        if (value == 0L) { mes("Bring me ancient emblems, totems, statuettes, medallions, effigies or relics. I exchange them for coins."); return }
        if (!choice2("Exchange all", true, "Cancel", false, title = "Exchange ancient artefacts for %,d coins?".format(value))) return
        // Recheck the complete snapshot after the suspended confirmation.
        if (carried(player) != items) { mes("Your inventory changed. Please try again."); return }
        if (!exchange(player, items)) mes("There isn't room for these coins. Your artefacts are unchanged.")
        else mes("You exchange your ancient artefacts for %,d coins.".format(value))
    }
    internal companion object {
        fun artefact(item: InvObj): AncientArtefact? = AncientArtefact.entries.firstOrNull {
            item.id == it.item.asRSCM() || item.id == "obj.cert_${it.item.removePrefix("obj.")}".asRSCM()
        }
        fun carried(player: Player): List<Pair<Int, InvObj>> = player.inv.objs.mapIndexedNotNull { slot, item -> if (item != null && artefact(item) != null) slot to item else null }
        fun total(items: List<Pair<Int, InvObj>>): Long = items.sumOf { (_, item) -> checkNotNull(artefact(item)).coins.toLong() * item.count }
        fun exchange(player: Player, items: List<Pair<Int, InvObj>>): Boolean {
            val value = total(items)
            if (value <= 0 || value > Int.MAX_VALUE || carried(player) != items) return false
            return player.invTransaction(player.inv) {
                val bag = select(player.inv)
                for ((slot, item) in items) delete(bag, item.id, item.count, slot = slot)
                add(bag, "obj.coins".asRSCM(), value.toInt())
            }.success
        }
    }
}
