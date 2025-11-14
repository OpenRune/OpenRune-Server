package org.alter.areas.lumbridge.objs

import org.alter.api.ext.*
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.move.MovementQueue
import org.alter.game.model.move.walkTo
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onObjectOption
import org.alter.rscm.RSCM.getRSCM

class AlkharidGate : PluginEvent() {

    private val closedGates = arrayOf(
        "objects.kharidmetalgateclosedl_2op",
        "objects.kharidmetalgateclosedr_2op"
    )

    private val guard = getRSCM("npcs.borderguard1")
    private val coins = getRSCM("items.coins")

    override fun init() {

        closedGates.forEach { gateId ->

            onObjectOption(gateId, "pay-toll(10gp)") {
                val hasCoins = player.inventory.getItemCount(coins) >= 10

                if (!hasCoins) {
                    player.queue { dialog(player) }
                    return@onObjectOption
                }

                handleGate(player, world)
            }

            onObjectOption(gateId, "open") {
                player.queue { dialog(player) }
            }
        }
    }

    private fun handleGate(player: Player, world: World) {
        player.inventory.remove(coins, 10)

        // If on the east side (Al-Kharid → Lumbridge)
        val toLumbridge = player.tile.x == 3268
        val targetX = if (toLumbridge) 3267 else 3268

        world.queue {
            // Open both gate parts
            world.openDoor(world.getObject(Tile(3268, 3228), 0)!!, "objects.inacmetalgateopenr")
            world.openDoor(
                world.getObject(Tile(3268, 3227), 0)!!,
                "objects.inacmetalgateopenl",
                invertRot = true
            )

            // Walk the player through
            player.walkTo(targetX, player.tile.z, MovementQueue.StepType.FORCED_WALK)
            wait(3)

            // Close again
            world.closeDoor(
                world.getObject(Tile(3267, 3227), 0)!!,
                "objects.kharidmetalgateclosedl_2op",
                invertTransform = true,
                invertRot = true
            )
            world.closeDoor(world.getObject(Tile(3267, 3228), 0)!!, "objects.kharidmetalgateclosedr_2op")
        }
    }

    suspend fun QueueTask.dialog(player: Player) {

        chatPlayer(player, "Can I come through this gate?", animation = "sequences.chatneu1")
        chatNpc(
            player = player,
            npc = guard,
            message = "You must pay a toll of 10 gold coins to pass.",
            animation = "sequences.chatneu3"
        )

        when (options(player, "No thank you, I'll walk around.", "Who does my money go to?", "Yes, ok.")) {
            1 -> {
                chatPlayer(player, "No thank you, I'll walk around.", animation = "sequences.chathap1")
                chatNpc(player = player, npc = guard, message = "Ok suit yourself.", animation = "sequences.chatneu1")
            }

            2 -> {
                chatPlayer(player, "Who does my money go to?", animation = "sequences.chathap1")
                chatNpc(
                    player = player,
                    npc = guard,
                    message = "The money goes to the city of Al-Kharid.",
                    animation = "sequences.chatneu3"
                )
            }

            3 -> {
                chatPlayer(player, "Yes, ok.", animation = "sequences.chathap1")
                handleGate(player, world)
            }
        }
    }
}