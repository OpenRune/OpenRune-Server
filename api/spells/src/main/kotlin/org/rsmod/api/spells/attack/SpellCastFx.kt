package org.rsmod.api.spells.attack

import dev.openrune.types.ItemServerType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.proj.ProjAnim

/**
 * The visual half of a spell cast, exposed separately from [SpellAttack] so content can play a real
 * cast at a target it does not damage - for example a water spell thrown at a fire wall, where the
 * only effect is the wall disappearing.
 *
 * [playImpact] is expected `clientDelay` ticks after the projectile from [playCast] was spawned.
 */
public interface SpellCastFx {
    /** Plays the cast animation, the launch spotanim and the projectile towards [target]. */
    public fun ProtectedAccess.playCast(target: PathingEntity, weapon: ItemServerType?): ProjAnim

    /** Plays the impact spotanim and the cast sound at [target] once the projectile lands. */
    public fun ProtectedAccess.playImpact(target: PathingEntity, clientDelay: Int)
}

public fun SpellCastFx.playCast(
    access: ProtectedAccess,
    target: PathingEntity,
    weapon: ItemServerType?,
): ProjAnim = access.playCast(target, weapon)

public fun SpellCastFx.playImpact(
    access: ProtectedAccess,
    target: PathingEntity,
    clientDelay: Int,
): Unit = access.playImpact(target, clientDelay)
