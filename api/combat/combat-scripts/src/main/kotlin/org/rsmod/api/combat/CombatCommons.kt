package org.rsmod.api.combat

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.PlayerAttackManager
import org.rsmod.api.config.constants
import org.rsmod.api.player.events.AttackKind
import org.rsmod.api.player.events.discardAttackRecord
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

internal const val ACTIVE_COMBAT_DELAY = constants.combat_activecombat_delay

internal const val NULL_SPOTANIM_ID = 0xFFFF
internal const val MAX_ATTACK_RANGE = 10
internal const val MAGIC_ATTACK_RANGE = MAX_ATTACK_RANGE
internal const val MAGIC_STAFF_ATTACK_RATE = constants.combat_pstaff_attackrate
internal const val MAGIC_SPELL_ATTACK_RATE = constants.combat_spell_attackrate

internal inline fun PlayerAttackManager.recordingAttack(
    player: Player,
    target: PathingEntity,
    attack: CombatAttack.PlayerAttack,
    block: () -> Unit,
) {
    val kind =
        when (attack) {
            is CombatAttack.Melee -> AttackKind.Melee
            is CombatAttack.Ranged -> AttackKind.Ranged
            is CombatAttack.Spell -> AttackKind.Spell
            is CombatAttack.Staff -> AttackKind.Staff
        }
    val spell = (attack as? CombatAttack.Spell)?.spell?.obj
    beginAttack(player, target, kind, spell)
    try {
        block()
    } catch (t: Throwable) {
        player.discardAttackRecord()
        throw t
    }
    finishAttack(player)
}
