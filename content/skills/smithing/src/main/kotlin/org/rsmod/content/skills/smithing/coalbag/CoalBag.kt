package org.rsmod.content.skills.smithing.coalbag

import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

object CoalBag {

    private var Player.storedCoal by intVarBit("varbit.coal_bag_storage_count")

    fun isCoal(itemInternal: String): Boolean = itemInternal == "obj.coal"

    fun isOpenInInventory(player: Player): Boolean = "obj.coal_bag_open" in player.inv

    fun shouldInterceptIncoming(player: Player): Boolean = isOpenInInventory(player)

    fun capacity(player: Player): Int =
        if ("obj.skillcape_smithing" in player.worn || "obj.skillcape_smithing_trimmed" in player.worn || "obj.skillcape_max" in player.worn) {
            36
        } else {
            27
        }

    fun storedAmount(player: Player): Int = player.storedCoal

    fun freeSpace(player: Player): Int =
        (capacity(player) - storedAmount(player)).coerceAtLeast(0)

    fun addStored(player: Player, amount: Int) {
        player.storedCoal += amount
    }

    fun removeStored(player: Player, amount: Int): Int {
        val removed = minOf(player.storedCoal, amount)
        player.storedCoal -= removed
        return removed
    }

    fun depositUpTo(player: Player, amount: Int): Int {
        val toAdd = minOf(amount, freeSpace(player))
        if (toAdd <= 0) {
            return 0
        }
        addStored(player, toAdd)
        return toAdd
    }
}
