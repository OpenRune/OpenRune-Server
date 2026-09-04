package org.rsmod.content.other.special.weapons.ranged

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.area.checker.isInWilderness
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.CombatChargeManager
import org.rsmod.api.config.constants
import org.rsmod.api.obj.charges.ObjChargeManager.Companion.isFailure
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.weapons.RangedWeapon
import org.rsmod.api.weapons.WeaponAttackManager
import org.rsmod.api.weapons.WeaponMap
import org.rsmod.api.weapons.WeaponRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * Wiki: "identical in function to Craw's bow (i.e. generates its own ammo, requires revenant
 * ether to fire, passive effect in the Wilderness)." Was entirely unregistered for normal
 * attacks - the item's real cache `category` is `chargebow`, which the base combat scripts
 * explicitly refuse to fire unless a `WeaponMap` is registered for it (see the "refuses to fire"
 * message in `PvNCombat`/`PvPCombat`).
 *
 * Uses 1 revenant ether charge per shot via the same [CombatChargeManager] primitive the special
 * attack now also uses. Wiki: "an additional 50% ranged accuracy and damage boost" applies only
 * against NPCs while in the Wilderness.
 */
class WebweaverBowWeapons
@Inject
constructor(
    private val charges: CombatChargeManager,
    private val areaChecker: AreaChecker,
) : WeaponMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        register("obj.wild_cave_webweaver_uncharged", UnchargedWebweaverBow(manager))
        register("obj.wild_cave_webweaver_charged", WebweaverBow(manager, charges, areaChecker))
    }

    private class WebweaverBow(
        private val manager: WeaponAttackManager,
        private val charges: CombatChargeManager,
        private val areaChecker: AreaChecker,
    ) : RangedWeapon {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean {
            shoot(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean {
            shoot(target, attack)
            return true
        }

        private fun ProtectedAccess.shoot(target: PathingEntity, attack: CombatAttack.Ranged) {
            val chargeResult = charges.attemptDetractWeapon(player, ETHER_VAROBJ)
            if (chargeResult.isFailure()) {
                manager.stopCombat(this)
                mes("Your Webweaver bow has run out of charges.")
                return
            }

            manager.playWeaponFx(this, attack)
            spotanim(LAUNCH_SPOTANIM, height = 96, slot = constants.spotanim_slot_combat)

            val projectile =
                manager.spawnProjectile(this, target, ARROW_TRAVEL_SPOTANIM, ARROW_PROJANIM)
            val (serverDelay, clientDelay) = projectile.durations

            // Wiki: the Wilderness passive boosts ranged accuracy/damage against NPCs only.
            val wildernessBoost = target is Npc && player.coords.isInWilderness(areaChecker)
            val multiplier = if (wildernessBoost) WILDERNESS_MULTIPLIER else 1.0
            val damage =
                manager.rollRangedDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = multiplier,
                    maxHitMultiplier = multiplier,
                )

            manager.giveCombatXp(this, target, attack, damage)
            manager.queueRangedHit(this, target, ammo = null, damage, clientDelay, serverDelay)

            if (chargeResult.fullyUncharged) {
                manager.stopCombat(this)
                mes("Your Webweaver bow has run out of charges.")
                return
            }
            manager.continueCombat(this, target)
        }
    }

    private class UnchargedWebweaverBow(private val manager: WeaponAttackManager) : RangedWeapon {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean {
            terminateAttack()
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean {
            terminateAttack()
            return true
        }

        private fun ProtectedAccess.terminateAttack() {
            mes("Your Webweaver bow needs to be charged with revenant ether first.")
            manager.stopCombat(this)
        }
    }

    private companion object {
        const val ETHER_VAROBJ: String = "varobj.charges_16383"
        const val WILDERNESS_MULTIPLIER: Double = 1.5
        const val LAUNCH_SPOTANIM: String = "spotanim.wild_cave_bow_arrow_launch02"
        const val ARROW_TRAVEL_SPOTANIM: String = "spotanim.wild_cave_bow_arrow_travel02"
        const val ARROW_PROJANIM: String = "projanim.arrow"
    }
}
