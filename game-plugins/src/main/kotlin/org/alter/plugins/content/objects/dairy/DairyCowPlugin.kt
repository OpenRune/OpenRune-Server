package org.alter.plugins.content.objects.dairy

import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.GroundItem
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.rscm.RSCM.getRSCM

class DairyCowPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {
        val bucketEmpty = getRSCM("items.bucket_empty")
        val bucketMilk = getRSCM("items.bucket_milk")
        val pengCowbell = getRSCM("items.peng_cowbell")

        // Milk option
        onObjOption("objects.fat_cow", option = "milk") {
            val obj = player.getInteractingGameObj()

            // Check if player has empty bucket
            if (!player.inventory.contains(bucketEmpty)) {
                player.message("You'll need a bucket to put the milk in.")
                return@onObjOption
            }

            player.queue {
                // Face the cow
                player.faceTile(obj.tile)
                wait(1)

                player.loopAnim("sequences.milkit")

                // Replace empty buckets with milk buckets one at a time, every 3 ticks
                while (player.inventory.contains(bucketEmpty)) {
                    if (player.inventory.replace(bucketEmpty, bucketMilk)) {
                        wait(3)
                    } else {
                        break
                    }
                }

                player.stopLoopAnim()
            }
        }

        // Steal cowbell option
        onObjOption("objects.fat_cow", option = "steal-cowbell") {
            val obj = player.getInteractingGameObj()

            player.queue {
                player.faceTile(obj.tile)

                // Perform pickup animation
                player.animate("sequences.human_pickuptable", interruptable = true)

                wait(1)

                // Try to add cowbell to inventory
                val addResult = player.inventory.add(item = pengCowbell, amount = 1, assureFullInsertion = false)

                if (!(addResult.completed > 0)) {
                    // Inventory is full, drop on floor
                    val groundItem: GroundItem = GroundItem(pengCowbell, 1, player.tile, player)
                    world.spawn(groundItem)
                }
            }
        }
    }
}

