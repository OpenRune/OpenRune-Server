package org.rsmod.content.skills.thieving

import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.invtx.*
import org.rsmod.api.player.events.skilling.PickpocketSuccessEvent
import org.rsmod.api.player.protect.ProtectedAccess

/** Only the successful native NPC interaction calls this; insertion failure grants no credit. */
internal fun ProtectedAccess.awardPickpocketLoot(target: Pickpocket, npcType: Int): List<Pair<Loot, Int>>? {
    if (stat("stat.thieving") < target.level || target.npcs.none { it.asRSCM() == npcType }) return null
    val pouch = target.pouch
    if (pouch != null && inv.count(pouch.obj) >= 28) return null
    val rolled = when {
        target.prifddinas && random.of(1024) == 0 -> Loot("obj.prif_teleport_seed")
        target.prifddinas && random.of(35) == 0 -> Loot("obj.prif_crystal_shard")
        else -> target.loot?.roll(random)
    }
    // Old citizen pouches remain guaranteed. Elf pouches replace only the coins roll.
    val rewards = buildList {
        if (pouch != null && (!target.elf || rolled?.obj == "obj.coins")) add(Loot(pouch.obj) to 1)
        if (rolled != null && !(target.elf && rolled.obj == "obj.coins")) add(rolled to random.of(rolled.min, rolled.max))
    }.map { (loot, count) ->
        val doubleLoot = target.elf && loot.obj != "obj.prif_crystal_shard" && ElfPickpockets.rogueOutfit.all { (slot, obj) -> player.worn[slot]?.id == obj.asRSCM() }
        loot to if (doubleLoot) count * 2 else count
    }
    if (rewards.isEmpty()) return null
    val result = if (pouch == null && rewards.size == 1) {
        val (loot, count) = rewards.single()
        player.invAdd(inv, loot.obj, count)
    } else player.invTransaction(inv) {
        val inventory = select(inv)
        for ((loot, count) in rewards) add(inventory, loot.obj.asRSCM(), count)
    }
    if (!result.success) return null
    statAdvance("stat.thieving", target.xp)
    publish(PickpocketSuccessEvent(player, npcType, if (target.elf) PickpocketSuccessEvent.Group.Elf else PickpocketSuccessEvent.Group.Other))
    return rewards
}

internal fun ProtectedAccess.openCoinPouches(pouch: CoinPouch, all: Boolean): Boolean {
    val count = if (all) inv.count(pouch.obj) else minOf(1, inv.count(pouch.obj))
    if (count <= 0) return false
    val coins = if (pouch.coins == pouch.maxCoins) count * pouch.coins else (1..count).sumOf { random.of(pouch.coins, pouch.maxCoins) }
    return player.invTransaction(inv) {
        val inventory = select(inv)
        delete(inventory, pouch.obj.asRSCM(), count)
        add(inventory, "obj.coins".asRSCM(), coins)
    }.success
}
