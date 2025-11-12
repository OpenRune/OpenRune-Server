package org.alter.skills.woodcutting

import org.alter.api.Skills
import org.alter.game.model.World
import org.alter.game.model.entity.GameObject
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.event.impl.SkillingActionCompletedGatheringEvent

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
    val treeRscm: String,
    val treeType: Int,
    val logItemId: Int,
    val experiencePerLog: Double,
    val logsObtained: Int = 1,
    val queueTask: QueueTask,
    val world: World
) : SkillingActionCompletedGatheringEvent(
    player = player,
    skill = Skills.WOODCUTTING,
    actionObject = treeObject,
    experienceGained = experiencePerLog * logsObtained,
    resourceId = logItemId,
    amountGathered = logsObtained
)

