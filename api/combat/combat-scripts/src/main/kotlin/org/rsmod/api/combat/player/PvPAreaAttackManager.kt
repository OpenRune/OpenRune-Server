package org.rsmod.api.combat.player

import jakarta.inject.Inject
import org.rsmod.api.death.PvPAttackValidateHook
import org.rsmod.api.death.PvPAttackValidateResult
import org.rsmod.api.death.PvPSkullHook
import org.rsmod.api.death.PvPSpecialAttackHook
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player

/**
 * PvP legality and side effects for area-of-effect special attacks (halberd sweeps, Dinh's shield
 * bash, Rune thrownaxe's chain, etc.) that can hit players other than the one the wielder
 * interacted with.
 *
 * The primary target already goes through [org.rsmod.api.combat.PvPCombat]'s own attack-validate
 * hooks and pk-var/skull bookkeeping before an area special ever runs. Every other player caught
 * in the area skips that path entirely, so this reapplies the same two pieces for them: the
 * [PvPAttackValidateHook] legality checks (wilderness level range, Ferox Enclave, would-skull
 * prevention, etc.) via [canAttack], and the pk-var/skull/special-attack-hook bookkeeping via
 * [applySecondarySpecialAttack].
 */
public class PvPAreaAttackManager
@Inject
constructor(
    private val attackValidateHooks: Set<PvPAttackValidateHook>,
    private val skullHooks: Set<PvPSkullHook>,
    private val specialAttackHooks: Set<PvPSpecialAttackHook>,
) {
    public fun canAttack(source: Player, target: Player): Boolean {
        if (!target.isValidTarget()) {
            return false
        }
        for (hook in attackValidateHooks) {
            if (hook.validate(source, target) is PvPAttackValidateResult.Deny) {
                return false
            }
        }
        return true
    }

    public fun applySecondarySpecialAttack(source: ProtectedAccess, target: Player) {
        for (hook in skullHooks) {
            hook.onPlayerAttack(source.player, target)
        }
        source.setPkVars(target)
        for (hook in specialAttackHooks) {
            hook.onPlayerSpecialAttack(source.player, target)
        }
    }
}
