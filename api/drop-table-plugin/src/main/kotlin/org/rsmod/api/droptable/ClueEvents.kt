package org.rsmod.api.droptable

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent

/** Published as a trail's length is rolled; [length] is kept within [minimum]..[maximum]. */
public class ClueTrailLengthEvent(
    override val player: Player,
    public val tier: ClueTier,
    public val minimum: Int,
    public val maximum: Int,
    public var length: Int,
) : UnboundEvent, PlayerEvent

/** Published as a reward casket opens; [rolls] is kept within [minimum]..[maximum]. */
public class ClueCasketRollsEvent(
    override val player: Player,
    public val tier: ClueTier,
    public val minimum: Int,
    public val maximum: Int,
    public var rolls: Int,
) : UnboundEvent, PlayerEvent

/**
 * Published as the next step of a trail is chosen. [candidates] are the clue helper step row ids
 * the roll could have picked and [rowId] is the one it did; a handler may set [rowId] to another
 * candidate, for example one whose region the player has unlocked. A value outside [candidates] is
 * ignored.
 */
public class ClueStepRollEvent(
    override val player: Player,
    public val tier: ClueTier,
    public val candidates: List<Int>,
    public var rowId: Int,
) : UnboundEvent, PlayerEvent
