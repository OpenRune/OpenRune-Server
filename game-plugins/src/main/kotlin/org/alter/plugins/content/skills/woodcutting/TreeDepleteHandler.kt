package org.alter.plugins.content.skills.woodcutting

import org.alter.game.model.entity.GameObject
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.World

/**
 * Handler interface for tree deplete events.
 * Each tree type can have its own handler that defines what happens when the tree depletes.
 */
interface TreeDepleteHandler {
    /**
     * The tree type identifier (e.g., "blisterwood", "oak", "magic")
     */
    val treeTypeId: String

    /**
     * Handles the tree depletion event.
     *
     * @param queueTask The QueueTask context for suspending operations
     * @param player The player who chopped the tree
     * @param treeObject The GameObject representing the tree that was depleted
     * @param treeRscm The RSCM identifier of the tree
     * @param world The game world
     * @return true if the handler processed the depletion (prevents default stump creation), false otherwise
     */
    suspend fun handleDeplete(
        queueTask: QueueTask,
        player: Player,
        treeObject: GameObject,
        treeRscm: String,
        world: World
    ): Boolean
}

