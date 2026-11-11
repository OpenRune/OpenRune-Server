package org.rsmod.api.player.events

import org.rsmod.api.player.hook.TeleportType
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent
import org.rsmod.map.CoordGrid

/**
 * Published for every teleport that is not [TeleportType.Exempt], after the
 * `PlayerTeleportValidateHook`s pass. That includes scripted moves such as ladders, stairs, doors
 * and ships, not only spells and items. Spells, tablets and items also publish it as a pre-check
 * before any cost, so one teleport can publish it more than once: handlers must only set
 * [refusal]. A non-null [refusal] stops the teleport and is shown to the player.
 */
public class PlayerTeleportValidateEvent(
    override val player: Player,
    public val dest: CoordGrid,
    public val type: TeleportType,
    public var refusal: String? = null,
) : UnboundEvent, PlayerEvent
