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
 * Ordinary attacks (Pound/Pummel/Spike/Block) - the item's own cache-defined bonuses, no bespoke
 * accuracy or damage. No passive; this weapon is used almost exclusively for its special attack
 * (see `CrimsonKistenSpecialAttack.kt`).
 *
 * The cache entry for `obj.crimson_kisten` (a June 2026 Maggot King drop) originally had no
 * `weaponCategory` at all, which is why the combat-style tab showed Punch/Kick/Block instead of
 * this weapon's real styles - that part can only be fixed in the cache (added a
 * `weaponCategory="Spiked"` override), since the client reads it directly for that tab. The
 * animation itself is still played directly here rather than relying on
 * `WeaponAttackManager.playWeaponFx`'s cache-param lookup, using the weapon's own real, named
 * animations (`seq.human_weapons_crimson_kisten_*`) - per the wiki's own trivia, the crush styles
 * use the newer "attack" animation while the stab style deliberately kept the original
 * "attack_alt" animation after a post-release hotfix.
 */
class CrimsonKistenWeapons @Inject constructor() : WeaponMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        val kisten = CrimsonKisten(manager)
        register("obj.crimson_kisten", kisten)
    }

    private class CrimsonKisten(private val manager: WeaponAttackManager) : MeleeWeapon {
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
            // This hardcodes the animation directly, bypassing the item's own cache
            // attack_anim_stance* params entirely - meaning changing those params in items.toml
            // has zero visible effect for this weapon's normal attack. That cost a lot of wasted
            // rounds live-testing before this file itself was rechecked. Stance4 (Block) used to
            // play DEF_ANIM here on the theory that it was the correct Block-stance swing (torka's
            // own notes label 14257 "Defend"), but live feedback was clear it looks wrong -
            // reusing the regular attack swing instead.
            val anim =
                when (attack.stance) {
                    CombatStance.Stance3 -> ATTACK_ALT_ANIM
                    else -> ATTACK_ANIM
                }
            player.anim(anim, priority = 6)
            player.soundSynth(SWING_SOUND)
        }
    }

    private companion object {
        // Real named sequences (see `seq.human_weapons_crimson_kisten_*`); no `attack_anim_stance*`
        // param aliases exist in this revision's cache for this item.
        const val ATTACK_ANIM: String = "seq.human_weapons_crimson_kisten_attack"
        const val ATTACK_ALT_ANIM: String = "seq.human_weapons_crimson_kisten_attack_alt"
        const val SWING_SOUND: Int = 401
    }
}
