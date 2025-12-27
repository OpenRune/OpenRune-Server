package org.alter.plugins.content.commands.commands.admin

import dev.openrune.ServerCacheManager
import dev.openrune.ServerCacheManager.getItemOrDefault
import dev.openrune.ServerCacheManager.itemSize
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.game.model.priv.Privilege
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.queue.TaskPriority
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import java.text.DecimalFormat
import org.alter.api.Colors

class ItemPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {

        onCommand("item", Privilege.ADMIN_POWER, description = "Spawn items") {
            val values = player.getCommandArgs()

            val itemId = values.getOrNull(0)?.toIntOrNull() ?: run {
                player.message("Invalid item id.")
                return@onCommand
            }

            if (itemId !in 0 until itemSize()) {
                player.message("Item $itemId does not exist in cache.")
                return@onCommand
            }

            val def = ServerCacheManager.getItem(itemId) ?: run {
                player.message("Item $itemId does not exist in cache.")
                return@onCommand
            }

            val amount = values.getOrNull(1)?.parseAmount()?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt() ?: 1

            val result = player.inventory.add(
                item = itemId,
                amount = amount,
                assureFullInsertion = false
            )

            val formattedAmount = DecimalFormat().format(result.completed)

            player.message("You have spawned <col=${Colors.RED}>$formattedAmount x ${def.name}</col> " +
                    "(ID: <col=${Colors.GREEN}>$itemId</col>)."
            )
        }
    }

    suspend fun QueueTask.spawn(player: Player): Item? {
        val item = searchItemInput(player, "Select an item to spawn:")
        if (item == -1) {
            return null
        }
        val amount =
            when (options(player, "1", "5", "X", "Max", title = "How many would you like to spawn?")) {
                1 -> 1
                2 -> 5
                3 -> inputInt(player, "Enter amount to spawn")
                4 -> Int.MAX_VALUE
                else -> return null
            }
        val add = player.inventory.add(item, amount, assureFullInsertion = false)
        return Item(item, add.completed)
    }
}
