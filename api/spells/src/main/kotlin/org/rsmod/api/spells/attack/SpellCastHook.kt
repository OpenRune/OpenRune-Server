package org.rsmod.api.spells.attack

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.PathingEntity

public interface SpellCastHook {
    /** Called once the spell's requirements were consumed and the cast is going ahead. */
    public fun onCast(access: ProtectedAccess, attack: CombatAttack.Spell) {}

    /** Called after the spell's own attack has been fully handled against [target]. */
    public suspend fun afterSpell(
        access: ProtectedAccess,
        target: PathingEntity,
        attack: CombatAttack.Spell,
    ) {}
}
