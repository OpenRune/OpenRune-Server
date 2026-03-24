package org.alter.objects

import org.alter.api.Skills
import org.alter.api.ext.message
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ObjectClickEvent

class AltarPlugin : PluginEvent() {

    override fun init() {
        on<ObjectClickEvent> {
            where { objHasOption("Pray-at", "Pray") }
            then {
                player.queue {
                    player.faceTile(gameObject.tile)
                    player.animate("sequences.human_pray")
                    wait(3)
                    val maxPrayer = player.getSkills().getBaseLevel(Skills.PRAYER)
                    val currentPrayer = player.getSkills().getCurrentLevel(Skills.PRAYER)
                    if (currentPrayer < maxPrayer) {
                        player.getSkills().setCurrentLevel(Skills.PRAYER, maxPrayer)
                        player.message("You recharge your Prayer points.")
                    } else {
                        player.message("You already have full Prayer points.")
                    }
                }
            }
        }
    }
}
