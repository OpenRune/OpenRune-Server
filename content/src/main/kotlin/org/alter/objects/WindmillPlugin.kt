package org.alter.objects

import org.alter.api.ext.message
import org.alter.game.model.attr.AttributeKey
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ItemOnObject
import org.alter.game.pluginnew.event.impl.onObjectOption
import org.alter.rscm.RSCM.getRSCM

class WindmillPlugin : PluginEvent() {

    companion object {
        /** Player attribute: grain has been placed in the hopper. */
        val GRAIN_IN_HOPPER = AttributeKey<Boolean>()

        /** Player attribute: flour is ready in the bin. */
        val FLOUR_READY = AttributeKey<Boolean>()
    }

    override fun init() {
        val grain = getRSCM("items.grain")
        val potEmpty = getRSCM("items.pot_empty")
        val potFlour = getRSCM("items.pot_flour")

        // Place grain in the hopper
        on<ItemOnObject> {
            where {
                item.id == grain && gameObject.internalID == getRSCM("objects.hopper")
            }
            then {
                player.queue {
                    player.faceTile(gameObject.tile)
                    wait(1)
                    player.inventory.remove(grain, 1)
                    player.attr[GRAIN_IN_HOPPER] = true
                    player.message("You put the grain in the hopper.")
                }
            }
        }

        // Operate the hopper controls
        onObjectOption("objects.hopper1", "operate") {
            player.queue {
                player.faceTile(gameObject.tile)
                wait(1)
                if (player.attr[GRAIN_IN_HOPPER] == true) {
                    player.attr.remove(GRAIN_IN_HOPPER)
                    player.attr[FLOUR_READY] = true
                    player.message("You operate the hopper. The grain slides down.")
                } else {
                    player.message("You operate the hopper. Nothing interesting happens.")
                }
            }
        }

        // Empty the flour bin (full variant)
        onObjectOption("objects.millbase_flour", "empty") {
            collectFlour(potEmpty, potFlour)
        }

        // Empty the flour bin (empty variant, in case flour is ready via attr)
        onObjectOption("objects.millbase", "empty") {
            collectFlour(potEmpty, potFlour)
        }
    }

    private suspend fun org.alter.game.pluginnew.event.impl.ObjectClickEvent.collectFlour(
        potEmpty: Int,
        potFlour: Int
    ) {
        player.queue {
            player.faceTile(gameObject.tile)
            wait(1)
            if (player.attr[FLOUR_READY] != true) {
                player.message("The flour bin is empty.")
                return@queue
            }
            if (!player.inventory.contains(potEmpty)) {
                player.message("You need an empty pot to collect the flour.")
                return@queue
            }
            player.inventory.remove(potEmpty, 1)
            player.inventory.add(potFlour, 1)
            player.attr.remove(FLOUR_READY)
            player.message("You fill a pot with flour from the bin.")
        }
    }
}
