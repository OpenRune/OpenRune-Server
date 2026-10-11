package org.rsmod.content.other.special.weapons.melee

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
 * Ordinary attacks (Lunge/Swipe/Pound/Block) - the item's own cache-defined bonuses (including its
 * vampyre accuracy/damage bonus, already read generically off cache params), no bespoke special
 * multiplier. No passive; this weapon is used mostly for its special attack (see
 * `SunspearSpecialAttack.kt`).
 *
 * The cache entry for `obj.sunspear` (a Blood Moon Rises quest reward, June 2026) is missing
 * `attack_anim_stance*`/`attack_sound_stance*` params entirely, same situation as Thunder khopesh,
 * Fang of the Hound, and Crimson kisten, so `WeaponAttackManager.playWeaponFx` has nothing to read
 * and falls back to the generic unarmed punch/kick. Played directly here instead. Unlike those
 * three, gameval only has a named animation for Sunspear's *special* attack
 * (`seq.human_weapons_sunspear_spec`) - its normal swing was never given a unique animation, so
 * this reuses the generic Spear-category animations real spears (e.g. the rune spear) fall back to.
 */
class SunspearWeapons @Inject constructor() : WeaponMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        val spear = Sunspear(manager)
        register("obj.sunspear", spear)
    }

    private class Sunspear(private val manager: WeaponAttackManager) : MeleeWeapon {
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
            val (anim, soundId) =
                when (attack.stance) {
                    CombatStance.Stance2 -> SWIPE_ANIM to SWIPE_SOUND
                    CombatStance.Stance3 -> POUND_ANIM to POUND_SOUND
                    else -> LUNGE_ANIM to LUNGE_SOUND
                }
            player.anim(anim, priority = 6)
            player.soundSynth(soundId)
        }
    }

    private companion object {
        // Generic Spear-category animations (matches the rune spear's own fallback set); no
        // dedicated normal-attack sequence exists for the sunspear model itself, only its special.
        const val LUNGE_ANIM: String = "seq.human_spear_spike"
        const val LUNGE_SOUND: Int = 2562
        const val SWIPE_ANIM: String = "seq.human_scythe_sweep"
        const val SWIPE_SOUND: Int = 2556
        const val POUND_ANIM: String = "seq.human_spear_lunge"
        const val POUND_SOUND: Int = 2555
    }
}
