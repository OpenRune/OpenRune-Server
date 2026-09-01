package org.rsmod.content.other.special.weapons.melee

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.CombatStance
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.weapons.MeleeWeapon
import org.rsmod.api.weapons.WeaponAttackManager
import org.rsmod.api.weapons.WeaponMap
import org.rsmod.api.weapons.WeaponRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * Ordinary attacks (Chop/Slash/Smash/Block) - the item's own cache-defined bonuses, no bespoke
 * accuracy or damage. No passive; this weapon is used almost exclusively for its special attack
 * (see `DogswordSpecialAttack.kt`).
 *
 * `obj.echo_godsword`/`obj.deadman_dogsword` share the exact same real Bandos/Saradomin/Zamorak
 * godsword animation *IDs* (7045/7054/7055) as the actual godswords - this cache revision just
 * never gave them named RSCM aliases the way it did for a plain 2h sword's animations, so they
 * have to be referenced by raw ID via `RSCM.getReverseMapping` instead of a normal string param.
 * (An earlier fix here reused a generic `TwoHandedSword` swing instead, which compiled and fixed
 * the "Category: Unarmed" bug but still looked like a plain sword rather than a godsword swing.)
 */
class DogswordWeapons @Inject constructor() : WeaponMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        val dogsword = Dogsword(manager)
        register("obj.echo_godsword", dogsword)
        register("obj.deadman_dogsword", dogsword)
    }

    private class Dogsword(private val manager: WeaponAttackManager) : MeleeWeapon {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean = swing(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean = swing(target, attack)

        private fun ProtectedAccess.swing(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean {
            playSwingFx(attack)
            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = 1.0,
                    maxHitMultiplier = 1.0,
                )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            manager.continueCombat(this, target)
            return true
        }

        private fun ProtectedAccess.playSwingFx(attack: CombatAttack.Melee) {
            val (animId, soundId) =
                when (attack.stance) {
                    CombatStance.Stance3 -> SMASH_ANIM to SMASH_SOUND
                    CombatStance.Stance4 -> BLOCK_ANIM to SLASH_SOUND
                    else -> SLASH_ANIM to SLASH_SOUND
                }
            player.anim(RSCM.getReverseMapping(RSCMType.SEQ, animId), priority = 6)
            player.soundSynth(soundId)
        }
    }

    private companion object {
        // Raw godsword animation IDs (see class doc) - no named RSCM alias exists in this cache.
        const val SLASH_ANIM: Int = 7045
        const val SMASH_ANIM: Int = 7054
        const val BLOCK_ANIM: Int = 7055
        const val SLASH_SOUND: Int = 3847
        const val SMASH_SOUND: Int = 3846
    }
}
