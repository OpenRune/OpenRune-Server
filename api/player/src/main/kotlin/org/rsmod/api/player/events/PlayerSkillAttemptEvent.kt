package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent
import org.rsmod.game.inv.InvObj

/**
 * What a skill action needs before an attempt, for relics that change speed or requirements. It is
 * published whenever a skill checks one of these, which can be several times per tick, so handlers
 * must only set fields. Set whichever apply; a skill only reads the ones it uses.
 *
 * - [cycles] is the ticks between attempts, 0 when the skill is checking something else. It is read
 *   by gathering, cooking, firemaking, herblore, prayer, smithing, crafting and alchemy.
 * - [tool] is the best tool found, or null. Set it to let the skill work without one: a substitute is
 *   used for the success rate and animation but is never added to the player. Fishing only checks
 *   that it is not null, because its animation comes from the method. Read by woodcutting, mining
 *   and fishing.
 * - [needsSpace] false skips the full inventory check, for woodcutting, mining, fishing and thieving.
 *   Pair it with `SkillingProduct.destination`, or the award still fails for lack of room.
 * - [needsBait] false means fishing neither checks for nor spends bait.
 * - [canBeCaught] false means nothing spots the player at a stall.
 *
 * To stop a resource running out, set `SkillingProduct.depletes` in the prepare event instead.
 */
public class PlayerSkillAttemptEvent(
    override val player: Player,
    public val stat: String,
    public val subject: String?,
    public var tool: InvObj?,
    public var cycles: Int,
    public var needsSpace: Boolean = true,
    public var needsBait: Boolean = true,
    public var canBeCaught: Boolean = true,
) : UnboundEvent, PlayerEvent
