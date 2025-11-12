package org.alter.game.pluginnew.event.impl

import net.rsprot.protocol.util.CombinedId
import org.alter.game.model.Tile
import org.alter.game.model.entity.Entity
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.game.model.skill.Skill
import org.alter.game.pluginnew.MenuOption
import org.alter.game.pluginnew.event.PlayerEvent

/**
 * Base class for entity interaction events (Player, NPC, Object clicks)
 */
abstract class EntityInteractionEvent<T>(
    open val target: T,
    open val option: MenuOption,
    player: Player
) : PlayerEvent(player) {
    val optionName: String
        get() = resolveOptionName()
    protected abstract fun resolveOptionName(): String
}

/**
 * Base class for simple events that just carry a value
 */
abstract class ValueEvent<T>(
    val value: T,
    player: Player
) : PlayerEvent(player)

/**
 * Base class for events that carry a message/text
 */
abstract class MessageEvent(
    open val message: String,
    player: Player
) : PlayerEvent(player)

/**
 * Base class for events that carry a tile/location
 */
abstract class LocationEvent(
    open val tile: Tile,
    player: Player
) : PlayerEvent(player)

/**
 * Base hierarchy for skilling action completion events.
 * These events are triggered when a skilling action completes (e.g., tree depleted, item crafted, ore mined).
 *
 * This allows different skills to share common event handling patterns while still providing
 * specialised data for distinct skilling categories (gathering, production, etc.).
 *
 * @param player The player who performed the action
 * @param skill The skill that gained experience
 * @param actionObject The GameObject/NPC/Entity that was interacted with
 * @param experienceGained Total experience awarded for this action, if known
 */
abstract class SkillingActionCompletedEvent(
    override val player: Player,
    open val skill: Int,
    open val actionObject: Entity?,
    open val experienceGained: Double? = null
) : PlayerEvent(player)

/**
 * Represents completion of a gathering action that yields tangible resources (e.g., logs, ore, fish).
 *
 * @param resourceId The item identifier gathered
 * @param amountGathered The quantity of resources successfully gathered
 */
abstract class SkillingActionCompletedGatheringEvent(
    player: Player,
    skill: Int,
    actionObject: Entity,
    experienceGained: Double? = null,
    open val resourceId: Int,
    open val amountGathered: Int
) : SkillingActionCompletedEvent(
    player = player,
    skill = skill,
    actionObject = actionObject,
    experienceGained = experienceGained
)


