package org.rsmod.content.slayer.requirements

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invDel
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.npc.hit.NpcHitpointsFloorHook
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.npc.vars.intVarn
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onApNpcU
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpNpcU
import org.rsmod.api.table.slayer.SlayerFinishersRow
import org.rsmod.content.slayer.core.SlayerTaskManager
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

object SlayerFinishers {
    const val FINISHING_HIT_DELAY = 1
    const val THROWN_RANGE = 7

    fun canFinish(finisher: SlayerFinishersRow, hitpoints: Int): Boolean =
        hitpoints in 1..finisher.threshold

    private val itemsByRow: Map<Int, Map<ItemServerType, ItemServerType?>> =
        SlayerFinishersRow.all().associate { it.rowId to it.items.backing }

    private fun items(finisher: SlayerFinishersRow): Map<ItemServerType, ItemServerType?> =
        itemsByRow.getValue(finisher.rowId)

    fun usableItems(finisher: SlayerFinishersRow): Set<ItemServerType> = items(finisher).keys

    fun finishingItemIds(finisher: SlayerFinishersRow): Set<Int> {
        val items = items(finisher)
        return (items.keys + items.values.filterNotNull()).mapTo(HashSet()) { it.id }
    }

    fun nextCharge(finisher: SlayerFinishersRow, itemId: Int): ItemServerType? =
        items(finisher).entries.firstOrNull { it.key.id == itemId }?.value?.takeIf { it.id != itemId }

    fun floor(finishingBlow: Boolean, ignoresFloor: Boolean): Int =
        if (finishingBlow || ignoresFloor) 0 else 1

    fun isFinishingBlow(finisher: SlayerFinishersRow, hit: Hit): Boolean {
        val secondary = hit.secondaryType() ?: return false
        return secondary.id in finishingItemIds(finisher)
    }

    fun wears(worn: Array<InvObj?>, item: ItemServerType?): Boolean =
        item != null && worn.any { it != null && it.id == item.id }

    fun hasUnlock(player: Player, finisher: SlayerFinishersRow): Boolean =
        SlayerTaskManager.hasUnlockedReward(player, finisher.unlockBit)

    fun autoChoice(player: Player, finisher: SlayerFinishersRow): AutoChoice? {
        if (wears(player.worn.objs, finisher.autoWorn)) {
            return AutoChoice(finisher.autoWorn!!, slot = null)
        }
        if (!hasUnlock(player, finisher)) {
            return null
        }
        val ordered = usableItems(finisher).sortedBy { it.id == finisher.consumed?.id }
        for (item in ordered) {
            val slot = player.inv.objs.indexOfFirst { it != null && it.id == item.id }
            if (slot >= 0) {
                return AutoChoice(item, slot)
            }
        }
        return null
    }

    fun spend(
        player: Player,
        slot: Int,
        item: ItemServerType,
        finisher: SlayerFinishersRow,
    ): Boolean {
        val inv = player.inv
        if (inv[slot]?.id != item.id) {
            return false
        }
        val next = nextCharge(finisher, item.id)
        return when {
            next != null ->
                player
                    .invTransaction(inv, true) {
                        val fromInv = select(inv)
                        delete {
                            this.from = fromInv
                            this.obj = item.id
                            this.strictCount = 1
                            this.strictSlot = slot
                        }
                        insert {
                            this.into = fromInv
                            this.obj = next.id
                            this.strictSlot = slot
                            this.strictCount = 1
                        }
                    }
                    .success
            finisher.consumed?.id == item.id -> player.invDel(inv, item.id, 1, slot = slot).success
            else -> true
        }
    }

    class AutoChoice(val item: ItemServerType, val slot: Int?)
}

internal class SlayerFinisherFloor @Inject constructor(private val players: PlayerList) :
    NpcHitpointsFloorHook {
    override fun floor(npc: Npc, hit: Hit): Int {
        val finisher = SlayerRules.finisher(npc) ?: return 0
        val sabre =
            hit.isFromPlayer &&
                hit.resolvePlayerSource(players)?.let {
                    SlayerFinishers.wears(it.worn.objs, finisher.bypassWorn)
                } == true
        return SlayerFinishers.floor(SlayerFinishers.isFinishingBlow(finisher, hit), sabre)
    }
}

class SlayerFinisherScript
@Inject
constructor(private val players: PlayerList, private val hitModifier: NpcHitModifier) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (finisher in SlayerFinishersRow.all()) {
            for (npc in finisher.npcs) {
                for (item in SlayerFinishers.usableItems(finisher)) {
                    onOpNpcU(npc, item) { use(it.npc, it.invSlot, it.objType, finisher) }
                    if (item.id == finisher.thrown?.id) {
                        onApNpcU(npc, item) { apUse(it.npc, it.invSlot, it.objType, finisher) }
                    }
                }
                onEvent<NpcHitEvents.Impact>(npc.id) { autoFinish(this.npc, hit, finisher) }
            }
        }
    }

    private fun ProtectedAccess.apUse(
        npc: Npc,
        slot: Int,
        item: ItemServerType,
        finisher: SlayerFinishersRow,
    ) {
        if (isWithinApRange(npc, SlayerFinishers.THROWN_RANGE)) {
            use(npc, slot, item, finisher)
        }
    }

    private fun ProtectedAccess.use(
        npc: Npc,
        slot: Int,
        item: ItemServerType,
        finisher: SlayerFinishersRow,
    ) {
        if (npc.isBeingFinished()) {
            return
        }
        val finishes = SlayerFinishers.canFinish(finisher, npc.hitpoints)
        if (!finishes && finisher.wastedEarly != true) {
            return
        }
        if (!SlayerFinishers.spend(player, slot, item, finisher)) {
            return
        }
        if (finishes) {
            finish(player, npc, item)
        }
    }

    private fun autoFinish(npc: Npc, hit: Hit, finisher: SlayerFinishersRow) {
        if (!hit.isFromPlayer || !SlayerFinishers.canFinish(finisher, npc.hitpoints)) {
            return
        }
        if (SlayerFinishers.isFinishingBlow(finisher, hit) || npc.isBeingFinished()) {
            return
        }
        val player = hit.resolvePlayerSource(players) ?: return
        val choice = SlayerFinishers.autoChoice(player, finisher) ?: return
        val slot = choice.slot
        if (slot != null && !SlayerFinishers.spend(player, slot, choice.item, finisher)) {
            return
        }
        finish(player, npc, choice.item)
    }

    private fun finish(player: Player, npc: Npc, item: ItemServerType) {
        npc.finishingUntil = npc.currentMapClock + SlayerFinishers.FINISHING_HIT_DELAY + 1
        npc.queueHit(
            source = player,
            delay = SlayerFinishers.FINISHING_HIT_DELAY,
            type = HitType.Typeless,
            damage = npc.hitpoints,
            modifier = hitModifier,
            sourceSecondary = item,
        )
    }

    private fun Npc.isBeingFinished(): Boolean = currentMapClock < finishingUntil

    private var Npc.finishingUntil: Int by intVarn("varn.slayer_finishing_until")
}
