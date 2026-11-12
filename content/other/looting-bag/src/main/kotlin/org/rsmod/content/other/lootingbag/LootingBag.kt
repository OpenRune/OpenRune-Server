package org.rsmod.content.other.lootingbag

import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory

/**
 * The looting bag (https://oldschool.runescape.wiki/w/Looting_bag): the carried obj says whether
 * the bag is open, and what it holds lives in the cache's own `inv.looting_bag`.
 */
public object LootingBag {
    public const val BAG: String = "obj.looting_bag"
    public const val OPEN_BAG: String = "obj.looting_bag_open"
    public const val INV: String = "inv.looting_bag"

    private const val BANK = "inv.bank"
    private val BAGS = listOf(BAG, OPEN_BAG)

    public fun contents(player: Player): Inventory = player.invMap.getOrPut(INV)

    public fun isOpen(player: Player): Boolean = OPEN_BAG in player.inv

    public fun owns(player: Player): Boolean {
        val bank = player.invMap[BANK]
        return BAGS.any { it in player.inv || bank != null && it in bank }
    }

    /** Puts every one of [objs] into the bag, or nothing when it can't hold them all. */
    public fun add(player: Player, objs: List<InvObj>): Boolean {
        val bag = contents(player)
        return player
            .invTransaction(bag) {
                val into = select(bag)
                for (obj in objs) {
                    add(into, obj.id, obj.count)
                }
            }
            .success
    }
}
