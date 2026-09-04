package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * Abyssal Puncture performs one accuracy roll against Slash defence, then follows it with two
 * independently rolled damage hits when that roll succeeds.
 */
class AbyssalDaggerSpecialAttack : SpecialAttackMap {
    private companion object {
        /** Unaliased in this cache's gamevals - no `synth.` name exists for it. */
        const val ABYSSAL_PUNCTURE_SOUND = 2537
    }

    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val puncture = AbyssalPuncture(manager, maxHitMultiplier = 0.85)
        val imbuedPuncture = AbyssalPuncture(manager, maxHitMultiplier = 0.95)

        registerMelee("obj.abyssal_dagger", puncture)
        registerMelee("obj.abyssal_dagger_p", puncture)
        registerMelee("obj.abyssal_dagger_p+", puncture)
        registerMelee("obj.abyssal_dagger_p++", puncture)

        registerMelee("obj.bh_abyssal_dagger_imbue", imbuedPuncture)
        registerMelee("obj.bh_abyssal_dagger_p_imbue", imbuedPuncture)
        registerMelee("obj.bh_abyssal_dagger_p+_imbue", imbuedPuncture)
        registerMelee("obj.bh_abyssal_dagger_p++_imbue", imbuedPuncture)
    }

    private class AbyssalPuncture(
        private val manager: SpecialAttackManager,
        private val maxHitMultiplier: Double,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            puncture(target, attack, secondHitDelay = AbyssalDaggerTiming.secondHitDelay(targetIsPlayer = false))
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            puncture(target, attack, secondHitDelay = AbyssalDaggerTiming.secondHitDelay(targetIsPlayer = true))
            return true
        }

        private fun ProtectedAccess.puncture(
            target: PathingEntity,
            attack: CombatAttack.Melee,
            secondHitDelay: Int,
        ) {
            anim("seq.abyssal_dagger_special")
            soundSynth(ABYSSAL_PUNCTURE_SOUND)
            spotanim(
                spot = "spotanim.abyssal_dagger_special_spotanim",
                slot = constants.spotanim_slot_combat,
                height = 0,
            )

            val successful =
                manager.rollMeleeAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = MeleeAttackType.Slash,
                    multiplier = 1.25,
                )
            val first =
                if (successful) {
                    manager.rollMeleeMaxHit(
                        source = this,
                        target = target,
                        attackType = attack.type,
                        attackStyle = attack.style,
                        multiplier = maxHitMultiplier,
                    )
                } else {
                    0
                }
            val second =
                if (successful) {
                    manager.rollMeleeMaxHit(
                        source = this,
                        target = target,
                        attackType = attack.type,
                        attackStyle = attack.style,
                        multiplier = maxHitMultiplier,
                    )
                } else {
                    0
                }

            manager.giveCombatXp(this, target, attack, first + second)
            manager.queueMeleeHit(this, target, first)
            manager.queueMeleeHit(this, target, second, delay = secondHitDelay)
            manager.continueCombat(this, target)
        }
    }
}

/** Pure timing, kept separate from [ProtectedAccess] so it can be unit tested. */
internal object AbyssalDaggerTiming {
    fun secondHitDelay(targetIsPlayer: Boolean): Int = if (targetIsPlayer) 1 else 2
}
