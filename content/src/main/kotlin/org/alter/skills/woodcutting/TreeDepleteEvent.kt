package org.alter.skills.woodcutting

import org.alter.api.Skills
import org.alter.game.model.World
import org.alter.game.model.entity.GameObject
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.event.PlayerEvent
import org.alter.game.pluginnew.event.impl.SkillingActionCompletedGatheringEvent
import org.generated.tables.woodcutting.WoodcuttingTreesRow

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
    val treeType : String,
    val world: World
) : PlayerEvent(player)

/**
 * Event triggered when a player successfully obtains a log from a tree.
 *
 * This event is fired as part of the woodcutting skilling action once a player
 * has gathered a log from a tree. It extends [SkillingActionCompletedGatheringEvent],
 * providing details about the player, the tree object, the type of tree, and the
 * log obtained.
 *
 * @property player The [Player] who obtained the log.
 * @property treeObject The [GameObject] representing the tree that was cut.
 * @property treeData The [WoodcuttingDefinitions.TreeData] containing information
 * about the tree type, including the log ID and experience points awarded.
 *
 * @see SkillingActionCompletedGatheringEvent
 */
class TreeLogObtainedEvent(
    override val player: Player,
    treeObject: GameObject,
    val treeData: WoodcuttingTreesRow,
    val clueBaseChance : Int = treeData.clueBaseChance
) : SkillingActionCompletedGatheringEvent(
    player = player,
    skill = Skills.WOODCUTTING,
    actionObject = treeObject,
    experienceGained = treeData.xp.toDouble(),
    resourceId = treeData.logItem,
    amountGathered = 1
)

