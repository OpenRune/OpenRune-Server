package org.alter.game.pluginnew.event.impl

import org.alter.game.model.entity.GameObject
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.event.impl.SkillingActionCompletedEvent

/**
 * Event triggered when a tree is depleted (chopped down) during woodcutting.
 * This event is posted when a tree has a 1/8 chance to deplete (or always for level 1 trees).
 *
 * @param player The player who chopped the tree
 * @param treeObject The GameObject representing the tree that was depleted
 * @param treeRscm The RSCM identifier of the tree
 */
class TreeDepleteEvent(
    override val player: Player,
    val treeObject: GameObject,
    override val actionRscm: String
) : SkillingActionCompletedEvent(player, treeObject, actionRscm)

