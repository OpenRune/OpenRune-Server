package org.alter.mechanics.shops

import org.alter.api.*
import org.alter.api.CommonClientScripts
import org.alter.api.cfg.*
import org.alter.api.dsl.*
import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.model.attr.*
import org.alter.game.model.container.*
import org.alter.game.model.container.key.*
import org.alter.game.model.entity.*
import org.alter.game.model.item.*
import org.alter.game.model.queue.*
import org.alter.game.model.shop.*
import org.alter.game.model.timer.*
import org.alter.game.plugin.*
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onButton
import org.alter.game.pluginnew.event.impl.onInterfaceClose
import org.alter.game.pluginnew.event.impl.onInterfaceOpen

class ShopsPlugin : PluginEvent() {

    override fun init() {
        val buyOptions = intArrayOf(1, 5, 10, 50)
        val sellOptions = intArrayOf(1, 5, 10, 50)

        val optionLabels = listOf(
            "Value",
            "Sell ${buyOptions[0]}",
            "Sell ${buyOptions[1]}",
            "Sell ${buyOptions[2]}",
            "Sell ${buyOptions[3]}"
        ).map { "$it<col=ff9040>" }

        onInterfaceOpen("interfaces.shopmain") {
            val shop = player.attr[CURRENT_SHOP_ATTR] ?: return@onInterfaceOpen

            player.runClientScript(
                CommonClientScripts.INTERFACE_INV_INIT,
                19726336,
                93,
                4,
                7,
                0,
                -1,
                *optionLabels.toTypedArray()
            )

            shop.viewers.add(player.uid)
        }

        onInterfaceClose("interfaces.shopmain") {
            val shop = player.attr[CURRENT_SHOP_ATTR] ?: return@onInterfaceClose

            shop.viewers.remove(player.uid)

        }

        onButton("components.shopmain:items") {
            val shop = player.attr[CURRENT_SHOP_ATTR] ?: return@onButton

            val opt = player.getInteractingOption()
            val slot = player.getInteractingSlot() - 1
            val shopItem = shop.items[slot] ?: return@onButton

            when (opt) {
                1 -> shop.currency.onSellValueMessage(player, shopItem)
                10 -> world.sendExamine(player, shopItem.item, ExamineEntityType.ITEM)

                else -> {
                    val index = opt - 2
                    if (index !in buyOptions.indices) return@onButton
                    val amount = buyOptions[index]

                    shop.currency.sellToPlayer(player, shop, slot, amount)
                }
            }
        }

        onButton("components.shopside:items") {
            val shop = player.attr[CURRENT_SHOP_ATTR] ?: return@onButton

            val opt = player.getInteractingOption()
            val slot = player.getInteractingSlot()
            val item = player.inventory[slot] ?: return@onButton

            when (opt) {
                1 -> shop.currency.onBuyValueMessage(player, shop, item.id)
                10 -> world.sendExamine(player, item.id, ExamineEntityType.ITEM)

                else -> {
                    val index = opt - 2
                    if (index !in sellOptions.indices) return@onButton
                    val amount = sellOptions[index]

                    shop.currency.buyFromPlayer(player, shop, slot, amount)
                }
            }
        }
    }
}