package org.rsmod.content.bosses.doom

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.flatten
import dtx.core.with
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.DropTableRegistry
import org.rsmod.api.droptable.KillRollContext
import org.rsmod.api.droptable.rollCount
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invClear
import org.rsmod.api.invtx.invMoveAll
import org.rsmod.api.invtx.invTransfer
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarp
import org.rsmod.api.random.GameRandom
import org.rsmod.content.interfaces.collectionlog.CollectionLog
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.uncert

internal var Player.lootClaimed by boolVarp("varp.dom_loot_claimed")

@Singleton
internal class DoomLoot
@Inject
constructor(
    private val delves: DoomDelves,
    private val prices: MarketPrices,
    private val registry: DropTableRegistry,
    private val random: GameRandom,
    private val instances: InstanceManager,
    private val playerList: PlayerList,
    private val stats: DoomStats,
) {
    /** Returns true when this kill rolled a unique. */
    fun roll(access: StandardNpcAccess): Boolean {
        val npc = access.npc
        val player = killer(access)
        if (player == null) {
            println("[DoomLoot] roll aborted: no killer for npc=${npc.id}")
            return false
        }
        return rollFor(player, npc, recordStats = true)
    }

    fun rollFor(player: Player, npc: Npc, recordStats: Boolean): Boolean {
        val level = delves.currentLevel(player)
        debug(player, "roll start level=$level npc=${npc.id}")
        if (recordStats) stats.complete(player, level)
        val table = registry.forNpc(BOSS_TABLE)
        if (table == null) {
            debug(player, "roll aborted: no drop table for $BOSS_TABLE")
            return false
        }
        val result = table.roll(player, ArgMap(KillRollContext.npc with npc)).flatten()
        val drops =
            when (result) {
                is RollResult.Single -> listOf(result.result)
                is RollResult.ListOf -> result.results
                else -> emptyList()
            }
        debug(player, "table result=${result::class.simpleName} drops=${drops.size}")
        var unique = false
        for (drop in drops) unique = award(player, drop, level) || unique
        if (level >= TEARS_FROM_LEVEL) {
            val tears = minOf(TEARS_BASE + TEARS_STEP * (level - TEARS_FROM_LEVEL), TEARS_CAP)
            add(player, "obj.demon_tear", tears)
            debug(player, "tears +$tears")
        }
        debug(player, "earned slots used=${earned(player).objs.count { it != null }}")
        return unique
    }

    fun hasUnique(player: Player): Boolean {
        val uniques = UNIQUES.map { it.asRSCM(RSCMType.OBJ) }.toSet()
        return earned(player).objs.filterNotNull().any { it.id in uniques }
    }

    fun stash(player: Player) {
        val earned = earned(player)
        for (obj in earned.objs.filterNotNull()) {
            val type = ServerCacheManager.getItem(obj.id) ?: continue
            CollectionLog.grant(player, uncert(type).id, obj.count)
        }
        player.invMoveAll(earned, claimed(player))
    }

    fun ProtectedAccess.openChest() {
        player.lootClaimed = true
        openEndLevel()
    }

    fun killer(access: StandardNpcAccess): Player? {
        access.findHero(playerList)?.let { return it }
        access.topDamager(playerList)?.let { return it }
        val session = instances.instanceForNpc(access.npc)?.let(instances::sessionForId) ?: return null
        return playerList.firstOrNull { it.uuid in session.occupants }
    }

    private fun award(player: Player, drop: DropRollItem, level: Int): Boolean {
        if (drop.isNothing || !drop.condition(player)) {
            debug(player, "skipped drop nothing=${drop.isNothing} obj=${drop.obj}")
            return false
        }
        val base = drop.rollCount(random)
        val multiplier = QUANTITY_MULTIPLIER[minOf(level, QUANTITY_MULTIPLIER.size) - 1]
        val count = base + (base * multiplier).toInt()
        val obj = drop.transformObj(player) ?: drop.obj
        val added = add(player, obj, count.coerceAtLeast(1))
        debug(player, "award $obj base=$base count=${count.coerceAtLeast(1)} success=$added")
        var unique = obj in UNIQUES
        for (bonus in drop.bonusDrops) unique = award(player, bonus, level) || unique
        return unique
    }

    private fun debug(player: Player, message: String) {
        player.mes("[DoomLoot] $message")
    }

    fun earned(player: Player): Inventory = player.invMap.getOrPut(EARNED_INV)

    fun claimed(player: Player): Inventory = player.invMap.getOrPut(CLAIMED_INV)

    fun add(player: Player, obj: String, count: Int): Boolean =
        player.invAdd(earned(player), obj, count).success

    fun reset(player: Player) {
        player.invClear(earned(player))
        player.lootClaimed = false
    }

    fun ProtectedAccess.openEndLevel() {
        invTransmit(earned(player))
        invTransmit(claimed(player))
        ifOpenMainModal(INTERFACE)
        runClientScript(INIT_SCRIPT, delves.currentLevel(player) - 1, if (player.lootClaimed) 1 else 0, 0)
        ifSetEvents("$COMPONENT:btn_claim", 0..1, IfEvent.Op1)
        ifSetEvents("$COMPONENT:btn_descend", 0..1, IfEvent.Op1)
        ifSetEvents("$COMPONENT:btn_leave", 0..1, IfEvent.Op1)
        ifSetEvents("$COMPONENT:btn_inv_all", 0..1, IfEvent.Op1)
        ifSetEvents("$COMPONENT:btn_bank_all", 0..1, IfEvent.Op1)
        ifSetEvents(
            "$COMPONENT:loot_contents",
            0..27,
            IfEvent.Op1,
            IfEvent.Op2,
            IfEvent.Op3,
            IfEvent.Op4,
            IfEvent.Op5,
            IfEvent.Op10,
        )
        updateValue()
    }

    fun ProtectedAccess.closeEndLevel() {
        invStopTransmit(earned(player))
        invStopTransmit(claimed(player))
        ifClose()
    }

    suspend fun ProtectedAccess.claim() {
        if (player.lootClaimed) return
        val confirmed =
            confirmOverlay(
                "$COMPONENT:dialogs",
                "Are you sure you want to claim your loot?",
                "Claiming your loot early will <col=ffff00>forfeit your run</col>, not allowing you to proceed any further.",
                "Cancel",
                "Confirm",
            )
        if (!confirmed) return
        stash(player)
        player.lootClaimed = true
        runClientScript(CLAIMED_SCRIPT, 1)
        ifClose()
        openEndLevel()
    }

    fun ProtectedAccess.take(slot: Int, count: Int) {
        if (!player.lootClaimed) return
        val pile = claimed(player)
        val obj = pile[slot] ?: return
        player.invTransfer(pile, slot, minOf(count, obj.count), inv, strict = false)
        updateValue()
    }

    fun ProtectedAccess.takeAll(into: Inventory) {
        if (!player.lootClaimed) return
        val pile = claimed(player)
        for (slot in pile.objs.indices) {
            val obj = pile[slot] ?: continue
            val uncert = into == bank
            player.invTransfer(pile, slot, obj.count, into, strict = false, uncert = uncert)
        }
        updateValue()
    }

    fun ProtectedAccess.examine(slot: Int) {
        val pile = if (player.lootClaimed) claimed(player) else earned(player)
        objExamine(pile, slot)
    }

    private fun ProtectedAccess.updateValue() {
        val value = total(earned(player)) + total(claimed(player))
        ifSetText("$COMPONENT:loot_value", "Value: ${"%,d".format(value)} GP")
    }

    private fun ProtectedAccess.total(inv: Inventory): Long =
        inv.objs.filterNotNull().sumOf { obj ->
            val type = ServerCacheManager.getItem(obj.id) ?: return@sumOf 0L
            (prices[uncert(type)] ?: 0).toLong() * obj.count
        }

    private companion object {
        private const val INTERFACE = "interface.dom_end_level_ui"
        private const val COMPONENT = "component.dom_end_level_ui"
        private const val EARNED_INV = "inv.dom_lootpile_during"
        private const val CLAIMED_INV = "inv.dom_lootpile"
        private const val INIT_SCRIPT = 7927
        private const val CLAIMED_SCRIPT = 7928
        private const val BOSS_TABLE = "npc.dom_boss"
        private val UNIQUES =
            setOf("obj.avernic_treads", "obj.eye_of_ayak_uncharged", "obj.mokhaiotl_cloth", "obj.dompet")
        private const val TEARS_FROM_LEVEL = 3
        private const val TEARS_BASE = 50
        private const val TEARS_STEP = 10
        private const val TEARS_CAP = 100
        private val QUANTITY_MULTIPLIER = doubleArrayOf(-0.5, -0.35, 0.0, 0.05, 0.10, 0.12, 0.14, 0.17, 0.20)
    }
}
