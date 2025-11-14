package org.alter.objects.hay

import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.model.entity.*
import org.alter.game.model.queue.*
import org.alter.game.plugin.*
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onObjectOption
import org.alter.rscm.RSCM.getRSCM

class SearchHayPlugin : PluginEvent() {

    private val hayObjects = setOf(
        "objects.haystack",
        "objects.haystack2",
        "objects.haystack3",
    )

    override fun init() {
        hayObjects.forEach { hay ->
            onObjectOption(obj = hay, "search") {
                val obj = player.getInteractingGameObj()
                val name = obj.getDef().name.lowercase()
                player.queue {
                    search(this, player, name)
                }
            }
        }
    }

    private suspend fun search(task: QueueTask, player: Player, objName: String) {
        player.lock()
        player.message("You search the $objName...")
        player.animate("sequences.human_pickupfloor")
        task.wait(3)
        player.unlock()

        when (world.random(100)) {
            0 -> handleNeedleFind(task, player)
            1 -> handleInjury(player)
            else -> player.message("You find nothing of interest.")
        }
    }

    private suspend fun handleNeedleFind(task: QueueTask, player: Player) {
        val add = player.inventory.add(item = "items.needle")
        if (add.hasFailed()) {
            world.spawn(GroundItem(item = getRSCM("items.needle"), amount = 1, tile = player.tile, owner = player))
        }

        task.chatPlayer(player, "Wow! A needle!<br>Now what are the chances of finding that?")
    }

    private fun handleInjury(player: Player) {
        player.hit(damage = 1)
        player.forceChat("Ouch!")
    }
}
