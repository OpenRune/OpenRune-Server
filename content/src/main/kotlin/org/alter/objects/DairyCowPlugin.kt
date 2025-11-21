package org.alter.objects

import org.alter.api.ext.loopAnim
import org.alter.api.ext.message
import org.alter.api.ext.stopLoopAnim
import org.alter.game.model.entity.GroundItem
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onObjectOption
import org.alter.rscm.RSCM.getRSCM

class DairyCowPlugin : PluginEvent() {

    override fun init() {
        val bucketEmpty = getRSCM("items.bucket_empty")
        val bucketMilk = getRSCM("items.bucket_milk")
        val pengCowbell = getRSCM("items.peng_cowbell")

        // Milk option
        onObjectOption("objects.fat_cow", "milk") {
            // Check if player has empty bucket
            if (!player.inventory.contains(bucketEmpty)) {
                player.message("You'll need a bucket to put the milk in.")
                return@onObjectOption
            }

            player.queue {
                // Face the cow
                player.faceTile(gameObject.tile)
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
        onObjectOption("objects.fat_cow", "steal-cowbell") {
            player.queue {
                player.faceTile(gameObject.tile)

                // Perform pickup animation
                player.animate("sequences.human_pickuptable", interruptable = true)

                wait(1)

                // Try to add cowbell to inventory
                val addResult = player.inventory.add(item = pengCowbell, amount = 1, assureFullInsertion = false)

                if (!(addResult.completed > 0)) {
                    // Inventory is full, drop on floor
                    val groundItem: GroundItem = GroundItem(pengCowbell, 1, player.tile, player)
                    player.world.spawn(groundItem)
                }
            }
        }
    }
}

