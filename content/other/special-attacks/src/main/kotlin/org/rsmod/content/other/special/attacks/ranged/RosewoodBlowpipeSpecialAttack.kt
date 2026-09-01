package org.rsmod.content.other.special.attacks.ranged

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.ranged.RangedAmmunition
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.righthand
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ranged.BlowpipeAmmo
import org.rsmod.api.random.GameRandom
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.RangedSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getInvObj

/** Rapid Burst fires two independently rolled stored darts at reduced accuracy and increased damage. */
class RosewoodBlowpipeSpecialAttack @Inject constructor(private val random: GameRandom) :
    SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerRanged("obj.rosewood_blowpipe", RapidBurst(manager, random))
    }

    private class RapidBurst(
        private val manager: SpecialAttackManager,
        private val random: GameRandom,
    ) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean = rapidBurst(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean = rapidBurst(target, attack)

        private fun ProtectedAccess.rapidBurst(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Boolean {
            val weaponType = getInvObj(attack.weapon)
            val equipped = player.righthand
            val dart = BlowpipeAmmo.loadedDart(equipped)
            if (dart == null || !BlowpipeAmmo.canUseLoadedDart(equipped)) {
                manager.stopCombat(this)
                mes("Your rosewood blowpipe has no usable darts loaded.")
                return false
            }
            if (!BlowpipeAmmo.hasDarts(equipped, DARTS_PER_SPECIAL)) {
                manager.stopCombat(this)
                mes("You need at least 2 darts loaded to use this special attack.")
                return false
            }

            val consumedDarts =
                (if (RangedAmmunition.conserveAmmo(player, random)) 0 else 1) +
                    (if (RangedAmmunition.conserveAmmo(player, random)) 0 else 1)

            anim(RAPID_BURST_SEQUENCE)
            weaponType.paramOrNull(params.attack_sound_stance1)?.let { soundSynth(it) }

            val projanim = weaponType.projectileType()
            val firstProjectile =
                manager.spawnProjectile(
                    source = this,
                    target = target,
                    spotanim = RAPID_BURST_TRAVEL_SPOTANIM,
                    projanim = projanim,
                )
            val secondProjectile =
                manager.spawnProjectile(
                    source = this,
                    target = target,
                    spotanim = RAPID_BURST_TRAVEL_SPOTANIM,
                    projanim = projanim,
                )
            val firstDamage = rapidBurstDamage(target, attack)
            val secondDamage = rapidBurstDamage(target, attack)

            val consumption = BlowpipeAmmo.consume(player, darts = consumedDarts)
            if (consumption == null) {
                manager.stopCombat(this)
                return false
            }

            manager.giveCombatXp(this, target, attack, firstDamage + secondDamage)
            manager.queueRangedHit(
                source = this,
                target = target,
                ammo = dart.type,
                damage = firstDamage,
                clientDelay = firstProjectile.clientCycles,
                hitDelay = firstProjectile.serverCycles,
            )
            manager.queueRangedDamage(
                source = this,
                target = target,
                ammo = dart.type,
                damage = secondDamage,
                hitDelay = secondProjectile.serverCycles,
            )

            if (consumption.becameEmpty) {
                mes("Your rosewood blowpipe has run out of darts.")
                manager.stopCombat(this)
            } else {
                manager.continueCombat(this, target)
            }
            return true
        }

        private fun ProtectedAccess.rapidBurstDamage(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Int =
            manager.rollRangedDamage(
                source = this,
                target = target,
                attack = attack,
                accuracyMultiplier = RAPID_BURST_ACCURACY_MULTIPLIER,
                maxHitMultiplier = RAPID_BURST_MAX_HIT_MULTIPLIER,
            )

        private fun ItemServerType.projectileType(): String =
            paramOrNull(params.proj_type)?.let {
                RSCM.getReverseMapping(RSCMType.PROJANIM, it.id)
            } ?: THROWN_PROJANIM
    }

    private companion object {
        const val DARTS_PER_SPECIAL: Int = 2
        const val RAPID_BURST_ACCURACY_MULTIPLIER: Double = 0.8
        const val RAPID_BURST_MAX_HIT_MULTIPLIER: Double = 1.1
        const val RAPID_BURST_SEQUENCE: String = "seq.rosewood_blowpipe_special_attack"
        const val RAPID_BURST_TRAVEL_SPOTANIM: String = "spotanim.rosewood_blowpipe_special_travel"
        const val THROWN_PROJANIM: String = "projanim.thrown"
    }
}
