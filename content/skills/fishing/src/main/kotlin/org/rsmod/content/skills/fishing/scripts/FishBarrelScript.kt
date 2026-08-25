package org.rsmod.content.skills.fishing.scripts

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.table.fishing.FishingSpotRow
import jakarta.inject.Inject
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.events.interact.HeldBanksideEvents
import org.rsmod.api.player.events.skilling.SkillingActionCompleteEvent
import org.rsmod.api.player.events.skilling.SkillingActionContext
import org.rsmod.api.player.events.skilling.SkillingProductSource
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeld5
import org.rsmod.api.script.onOpHeldU
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FishBarrelScript @Inject constructor() : PluginScript() {

    private val rawFish: Set<String> by lazy {
        FishingSpotRow.all().mapTo(mutableSetOf()) { RSCM.getReverseMapping(RSCMType.OBJ, it.fish.id) }
            .apply { add("obj.tbwt_raw_karambwan") }
    }

    override fun ScriptContext.startup() {
        for (barrel in ALL_BARRELS) {
            onOpHeld1(barrel) { dispatch(1, barrel) }
            onOpHeld2(barrel) { dispatch(2, barrel) }
            onOpHeld3(barrel) { dispatch(3, barrel) }
            onOpHeld4(barrel) { dispatch(4, barrel) }
            onOpHeld5(barrel) { dispatch(5, barrel) }
            onEvent<HeldBanksideEvents.Type>(barrel.asRSCM(RSCMType.OBJ)) { emptyIntoBank(player) }
        }

        onOpHeldU(FISH_SACK, BARREL_CLOSED) { combine(BARREL_CLOSED, SACK_CLOSED) }
        onOpHeldU(FISH_SACK, BARREL_OPEN) { combine(BARREL_OPEN, SACK_OPEN) }

        onEvent<SkillingActionCompleteEvent> {
            val ctx = context
            if (ctx is SkillingActionContext.Product && ctx.source is SkillingProductSource.Fishing) {
                tryAutoDeposit(player, ctx.item)
            }
        }
    }

    private fun ProtectedAccess.combine(barrel: String, sackBarrel: String) {
        invDel(inv, FISH_SACK)
        invDel(inv, barrel)
        invAdd(inv, sackBarrel)
        mes("You attach the fish sack to the barrel, making a fish sack barrel.")
    }

    private fun emptyIntoBank(player: Player) {
        val store = player.attr.getOrPut(FISH_BARREL_ATTR) { mutableMapOf() }
        if (store.isEmpty()) {
            player.mes("The fish barrel is empty.")
            return
        }
        val bank = player.invMap.getOrPut("inv.bank")
        val iterator = store.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            player.invAdd(bank, entry.key, entry.value)
            iterator.remove()
        }
        player.mes("You empty the fish barrel into your bank.")
    }

    private fun ProtectedAccess.dispatch(op: Int, barrel: String) {
        when (op) {
            1 -> fill()
            2 -> check()
            3 -> toggleOpen()
            4 -> empty()
            else -> {}
        }
    }

    private fun ProtectedAccess.contents(): MutableMap<String, Int> =
        player.attr.getOrPut(FISH_BARREL_ATTR) { mutableMapOf() }

    private fun MutableMap<String, Int>.total(): Int = values.sum()

    private fun ProtectedAccess.fill() {
        val store = contents()
        var filled = false
        for (name in rawFish) {
            if (store.total() >= CAPACITY) break
            val have = invTotal(inv, name)
            if (have <= 0) continue
            val room = CAPACITY - store.total()
            val move = minOf(have, room)
            if (move <= 0) break
            invDel(inv, name, move)
            store[name] = (store[name] ?: 0) + move
            filled = true
        }
        mes(if (filled) "You fill the fish barrel." else "You have no raw fish to store, or the barrel is full.")
    }

    private fun ProtectedAccess.empty() {
        val store = contents()
        if (store.isEmpty()) {
            mes("The fish barrel is empty.")
            return
        }
        val iterator = store.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val result = invAdd(inv, entry.key, entry.value)
            val added = if (result.success) entry.value else 0
            if (added >= entry.value) {
                iterator.remove()
            } else {
                entry.setValue(entry.value - added)
                mes("You don't have enough inventory space to empty the whole barrel.")
                return
            }
        }
        mes("You empty the fish barrel.")
    }

    private fun ProtectedAccess.check() {
        val store = contents()
        if (store.isEmpty()) {
            mes("The fish barrel is empty.")
            return
        }
        mes("The fish barrel contains (${store.total()}/$CAPACITY):")
        for ((name, count) in store) {
            val display = ServerCacheManager.getItem(name.asRSCM(RSCMType.OBJ))?.name ?: name
            mes("$display x $count")
        }
    }

    private fun ProtectedAccess.toggleOpen() {
        val (from, to, opened) =
            when {
                BARREL_OPEN in inv -> Triple(BARREL_OPEN, BARREL_CLOSED, false)
                BARREL_CLOSED in inv -> Triple(BARREL_CLOSED, BARREL_OPEN, true)
                SACK_OPEN in inv -> Triple(SACK_OPEN, SACK_CLOSED, false)
                SACK_CLOSED in inv -> Triple(SACK_CLOSED, SACK_OPEN, true)
                else -> return
            }
        invDel(inv, from)
        invAdd(inv, to)
        mes(if (opened) "You open the fish barrel." else "You close the fish barrel.")
    }

    private fun tryAutoDeposit(player: Player, item: String) {
        if (BARREL_OPEN !in player.inv && SACK_OPEN !in player.inv) return
        val store = player.attr.getOrPut(FISH_BARREL_ATTR) { mutableMapOf() }
        if (store.values.sum() >= CAPACITY) return
        if (item !in rawFish) return
        player.invDel(player.inv, item, 1)
        store[item] = (store[item] ?: 0) + 1
    }

    private companion object {
        private const val BARREL_CLOSED = "obj.fish_barrel_closed"
        private const val BARREL_OPEN = "obj.fish_barrel_open"
        private const val SACK_CLOSED = "obj.fish_sack_barrel_closed"
        private const val SACK_OPEN = "obj.fish_sack_barrel_open"
        private const val FISH_SACK = "obj.fish_sack"
        private const val CAPACITY = 28

        private val ALL_BARRELS = listOf(BARREL_CLOSED, BARREL_OPEN, SACK_CLOSED, SACK_OPEN)

        private val FISH_BARREL_ATTR =
            AttributeKey<MutableMap<String, Int>>(persistenceKey = "fish_barrel")
    }
}
