package org.rsmod.content.other.special.attacks.ranged

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.RangedAmmoManager
import org.rsmod.api.config.refs.params
import org.rsmod.api.mechanics.toxins.WeaponPoisonEffect
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.RangedSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getInvObj

/** Duality: throws two dragon knives, each with a separate normal ranged damage roll. */
class DragonKnifeSpecialAttack
@Inject
constructor(
    private val ammunition: RangedAmmoManager,
    private val poison: WeaponPoisonEffect,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val duality = DragonKnife(manager, ammunition, poison)
        registerRanged("obj.dragon_knife", duality)
        registerRanged("obj.dragon_knife_p", duality)
        registerRanged("obj.dragon_knife_p+", duality)
        registerRanged("obj.dragon_knife_p++", duality)
        registerRanged("obj.br_dragon_knife", duality)
    }

    private class DragonKnife(
        private val manager: SpecialAttackManager,
        private val ammunition: RangedAmmoManager,
        private val poison: WeaponPoisonEffect,
    ) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean = duality(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean = duality(target, attack)

        private fun ProtectedAccess.duality(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Boolean {
            val weaponType = getInvObj(attack.weapon)
            if ((player.righthand?.count ?: 0) < KNIVES_PER_SPECIAL) {
                manager.stopCombat(this)
                mes("You need at least 2 dragon knives equipped to use this special attack.")
                return false
            }

            val poisoned = DragonKnifeVariant.isPoisoned(weaponType.name)
            val travelSpot =
                if (poisoned) POISONED_SPECIAL_TRAVEL_SPOTANIM else SPECIAL_TRAVEL_SPOTANIM
            val projanim =
                weaponType.paramOrNull(params.proj_type)?.let {
                    RSCM.getReverseMapping(RSCMType.PROJANIM, it.id)
                } ?: THROWN_PROJANIM

            anim(if (poisoned) POISONED_SPECIAL_ANIM else SPECIAL_ANIM)
            weaponType.paramOrNull(params.attack_sound_stance1)?.let { soundSynth(it) }

            val firstProjectile = manager.spawnProjectile(this, target, travelSpot, projanim)
            val secondProjectile = manager.spawnProjectile(this, target, travelSpot, projanim)
            val firstDamage = manager.rollRangedDamage(this, target, attack)
            val secondDamage = manager.rollRangedDamage(this, target, attack)

            manager.giveCombatXp(this, target, attack, firstDamage + secondDamage)

            ammunition.useThrownWeapon(
                player = player,
                weaponType = weaponType,
                dropCoord = target.coords,
                dropDelay = firstProjectile.serverCycles,
            )
            manager.queueRangedHit(
                source = this,
                target = target,
                ammo = null,
                damage = firstDamage,
                clientDelay = secondProjectile.clientCycles,
                hitDelay = firstProjectile.serverCycles,
            )
            // Each thrown knife is its own independent roll for weapon poison, same as any other
            // poisoned ammo - this variant already detected "poisoned" for its visuals (see
            // DragonKnifeVariant) but never actually applied the poison itself.
            poison.rollOnRangedHit(player, target, weaponType, firstDamage)

            ammunition.useThrownWeapon(
                player = player,
                weaponType = weaponType,
                dropCoord = target.coords,
                dropDelay = secondProjectile.serverCycles,
            )
            manager.queueRangedDamage(
                source = this,
                target = target,
                ammo = null,
                damage = secondDamage,
                hitDelay = secondProjectile.serverCycles,
            )
            poison.rollOnRangedHit(player, target, weaponType, secondDamage)

            if (player.righthand == null) {
                mes("That was your last one!")
            }
            manager.continueCombat(this, target)
            return true
        }
    }

    private companion object {
        const val KNIVES_PER_SPECIAL = 2
        const val SPECIAL_ANIM = "seq.human_dragon_tknives_spec"
        const val POISONED_SPECIAL_ANIM = "seq.human_dragon_tknives_spec_poison"
        const val SPECIAL_TRAVEL_SPOTANIM = "spotanim.dragon_tknife_travel_spec"
        const val POISONED_SPECIAL_TRAVEL_SPOTANIM = "spotanim.dragon_tknife_travel_spec_p"
        const val THROWN_PROJANIM = "projanim.thrown"
    }
}

/** Pure variant detection, kept separate from [ProtectedAccess] so it can be unit tested. */
internal object DragonKnifeVariant {
    fun isPoisoned(itemName: String): Boolean = itemName.contains("(p", ignoreCase = true)
}
