package org.alter.plugins.content.skills.woodcutting.handlers

import org.alter.api.ext.findClosestWalkableTile
import org.alter.api.ext.message
import org.alter.game.model.entity.GameObject
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.model.move.stopMovement
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.World
import org.alter.game.model.timer.TimeConstants
import org.alter.plugins.content.skills.woodcutting.TreeDepleteHandler
import org.alter.plugins.content.skills.woodcutting.WoodcuttingDefinitions
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM

/**
 * Handler for Blisterwood tree depletion.
 *
 * According to OSRS wiki: When a blisterwood tree "depletes" (1/10 chance),
 * instead of being chopped down into a stump, it spawns a spider and shows
 * a message. The player must click again to resume woodcutting.
 *
 * @see https://oldschool.runescape.wiki/w/Blisterwood_tree
 */
class BlisterwoodTreeDepleteHandler : TreeDepleteHandler {
    override val treeType: WoodcuttingDefinitions.TreeType = WoodcuttingDefinitions.TreeType.BLISTERWOOD

    override suspend fun handleDeplete(
        queueTask: QueueTask,
        player: Player,
        treeObject: GameObject,
        treeRscm: String,
        world: World
    ): Boolean {
        // Stop the player's current action
        player.interruptQueues()
        player.stopMovement()
        player.resetInteractions()

        // Find a walkable tile next to the player
        val spawnTile = player.findClosestWalkableTile() ?: player.tile

        // Spawn spider next to the player (owned by player so multiple can exist per player)
        val spider = Npc(player, getRSCM("npcs.spider"), spawnTile, world)
        spider.walkRadius = 10 // Allow spider to wander within 10 tiles of spawn point
        spider.setActive(true) // Ensure spider is active for wandering
        world.spawn(spider)

        // Show message to player
        player.message("A small spider jumps at you from the log you just cut.")

        // Stop woodcutting animation
        player.animate(RSCM.NONE)

        // Despawn spider after 2 minutes
        val despawnCycles = TimeConstants.minutesToCycles(2)!!
        world.queue {
            wait(despawnCycles)
            if (spider.isSpawned()) {
                world.remove(spider)
            }
        }

        // Return true to indicate we handled the depletion (prevents default stump creation)
        return true
    }
}

