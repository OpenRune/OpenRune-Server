package org.alter.skills.smithing

import org.alter.api.ext.*
import org.alter.game.model.attr.COAL_BAG_AMOUNT_ATTR
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.MenuOption
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onItemOption
import org.alter.rscm.RSCM.asRSCM
import org.alter.skills.mining.OreObtainingEvent

class CoalBagEvents : PluginEvent() {

    private val coalBagClosedId = "items.coal_bag".asRSCM()
    private val coalBagOpenId = "items.coal_bag_open".asRSCM()
    private val coalId = "items.coal".asRSCM()

    private val baseCapacity = 27
    private val smithingCapeCapacity = 36

    override fun init() {
        on<OreObtainingEvent> {
            where { oreItemId == coalId && hasCoalBagOpen(player) }
            then { depositMinedCoalToBag(player, amount) }
        }

        onItemOption(item = coalBagClosedId, op = MenuOption.OP2) { fillBag(player) }
        onItemOption(item = coalBagClosedId, op = MenuOption.OP3) { openBag(player, slot) }
        onItemOption(item = coalBagClosedId, op = MenuOption.OP4) { checkBag(player) }
        onItemOption(item = coalBagClosedId, op = MenuOption.OP6) { emptyBag(player) }

        onItemOption(item = coalBagOpenId, op = MenuOption.OP2) { fillBag(player) }
        onItemOption(item = coalBagOpenId, op = MenuOption.OP3) { closeBag(player, slot) }
        onItemOption(item = coalBagOpenId, op = MenuOption.OP4) { checkBag(player) }
        onItemOption(item = coalBagOpenId, op = MenuOption.OP6) { emptyBag(player) }
    }

    private fun openBag(player: Player, slot: Int) {
        player.inventory.replace(coalBagClosedId, coalBagOpenId, slot)
    }

    private fun closeBag(player: Player, slot: Int) {
        player.inventory.replace(coalBagOpenId, coalBagClosedId, slot)
    }

    private fun getCoalBagCapacity(player: Player): Int =
        if (player.equipment.containsAny(
                "items.skillcape_smithing",
                "items.skillcape_smithing_trimmed",
                "items.skillcape_max"
            )
        ) smithingCapeCapacity else baseCapacity

    private fun fillBag(player: Player) {
        val current = player.attr.getOrDefault(COAL_BAG_AMOUNT_ATTR, 0)
        val capacity = getCoalBagCapacity(player)
        val freeSpace = capacity - current

        if (freeSpace <= 0) {
            player.message("Your coal bag is full.")
            return
        }

        val coalInInv = player.inventory.getItemCount(coalId)
        if (coalInInv <= 0) {
            player.message("You don't have any coal to fill your coal bag with.")
            return
        }

        val toDeposit = minOf(coalInInv, freeSpace)
        player.inventory.remove(coalId, toDeposit)
        player.attr[COAL_BAG_AMOUNT_ATTR] = current + toDeposit
        player.message("You fill the coal bag with $toDeposit coal.")
    }

    private fun emptyBag(player: Player) {
        val current = player.attr.getOrDefault(COAL_BAG_AMOUNT_ATTR, 0)
        if (current <= 0) {
            player.message("Your coal bag is empty.")
            return
        }

        val result = player.inventory.add(coalId, current, assureFullInsertion = false)
        val added = result.completed
        if (added <= 0) {
            player.message("You don't have any free space in your inventory.")
            return
        }

        player.attr[COAL_BAG_AMOUNT_ATTR] = current - added
        player.message("You empty $added coal from the coal bag.")
    }

    private fun checkBag(player: Player) {
        val current = player.attr.getOrDefault(COAL_BAG_AMOUNT_ATTR, 0)
        if (current <= 0) {
            player.message("Your coal bag is empty.")
            return
        }
        val capacity = getCoalBagCapacity(player)
        player.message("Your coal bag contains $current of $capacity coal.")
    }

    private fun hasCoalBagOpen(player: Player): Boolean =
        player.inventory.contains(coalBagOpenId)

    private fun depositMinedCoalToBag(player: Player, amount: Int) {
        val current = player.attr.getOrDefault(COAL_BAG_AMOUNT_ATTR, 0)
        val capacity = getCoalBagCapacity(player)
        val freeSpace = capacity - current
        if (freeSpace <= 0) return
        val toDeposit = minOf(amount, freeSpace)
        player.attr[COAL_BAG_AMOUNT_ATTR] = current + toDeposit
    }

    companion object {
        private val coalBagClosedId = "items.coal_bag".asRSCM()
        private val coalBagOpenId = "items.coal_bag_open".asRSCM()

        private fun hasCoalBag(player: Player): Boolean =
            player.inventory.contains(coalBagClosedId) || player.inventory.contains(coalBagOpenId)

        fun getCoalCount(player: Player): Int {
            if (!player.inventory.contains(coalBagClosedId) && !player.inventory.contains(coalBagOpenId)) return 0
            return player.attr.getOrDefault(COAL_BAG_AMOUNT_ATTR, 0)
        }

        fun getEffectiveCoalCount(player: Player): Int =
            player.inventory.getItemCount("items.coal".asRSCM()) + getCoalCount(player)

        fun consumeCoal(player: Player, amount: Int): Boolean {
            val coalId = "items.coal".asRSCM()
            var remaining = amount

            // Use inventory coal first
            val fromInv = minOf(player.inventory.getItemCount(coalId), remaining)
            if (fromInv > 0) {
                player.inventory.remove(coalId, fromInv)
                remaining -= fromInv
            }

            // Use bag coal for any leftover
            if (remaining > 0 && hasCoalBag(player)) {
                val inBag = player.attr.getOrDefault(COAL_BAG_AMOUNT_ATTR, 0)
                val fromBag = minOf(inBag, remaining)
                if (fromBag > 0) {
                    player.attr[COAL_BAG_AMOUNT_ATTR] = inBag - fromBag
                    remaining -= fromBag
                }
            }

            return remaining == 0
        }
    }
}
