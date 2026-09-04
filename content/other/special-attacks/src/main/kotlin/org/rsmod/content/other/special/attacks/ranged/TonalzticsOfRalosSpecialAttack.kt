package org.rsmod.content.other.special.attacks.ranged

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.magicLvl
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.RangedSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getInvObj

/**
 * Division makes the Tonalztics' regular 75%-maximum-hit roll and reduces Defence by ten percent
 * of the victim's current Magic level on each landed hit. Charged Tonalztics make two independent
 * rolls. Against NPCs, a first landed hit drains before the second roll; against players, both
 * rolls are made before either impact applies its drain.
 */
class TonalzticsOfRalosSpecialAttack @Inject constructor() : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerRanged(
            "obj.tonalztics_of_ralos_uncharged",
            Division(manager, hitCount = UNCHARGED_HIT_COUNT),
        )
        registerRanged(
            "obj.tonalztics_of_ralos_charged",
            Division(manager, hitCount = CHARGED_HIT_COUNT),
        )
    }

    private class Division(
        private val manager: SpecialAttackManager,
        private val hitCount: Int,
    ) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean = division(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean = division(target, attack)

        private fun ProtectedAccess.division(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Boolean {
            getInvObj(attack.weapon)
            anim(if (hitCount == CHARGED_HIT_COUNT) CHARGED_SPECIAL_ANIM else UNCHARGED_SPECIAL_ANIM)
            // The charged variant's spinning throw is the only one with dedicated special-attack
            // sounds on this item's own wiki "Sound effects" table (throw + spin); the uncharged
            // single throw has no separate special entry there. Unaliased in this cache's
            // gamevals.
            if (hitCount == CHARGED_HIT_COUNT) {
                soundSynth(CHARGED_THROW_SOUND)
                soundSynth(CHARGED_SPIN_SOUND, delay = CHARGED_SPIN_SOUND_DELAY)
            }
            spotanim(
                if (hitCount == CHARGED_HIT_COUNT) CHARGED_PLAYER_SPOTANIM
                else UNCHARGED_PLAYER_SPOTANIM,
            )

            val hits = ArrayList<DivisionHit>(hitCount)
            repeat(hitCount) { index ->
                val second = index == 1
                val projectile =
                    manager.spawnProjectile(
                        source = this,
                        target = target,
                        spotanim = if (second) SECOND_TRAVEL_SPOTANIM else FIRST_TRAVEL_SPOTANIM,
                        projanim = THROWN_PROJANIM,
                    )
                target.spotanim(
                    if (second) SECOND_IMPACT_SPOTANIM else FIRST_IMPACT_SPOTANIM,
                    delay = projectile.clientCycles,
                )
                val hit = rollDivisionHit(target, attack)
                if (target is Npc && hit.landed) {
                    target.drainDivisionDefence()
                }
                hits +=
                    hit.copy(
                        clientDelay = projectile.clientCycles,
                        hitDelay = projectile.serverCycles,
                    )
            }

            manager.giveCombatXp(this, target, attack, hits.sumOf { it.damage })
            hits.forEachIndexed { index, hit ->
                if (index == 0) {
                    manager.queueRangedHit(
                        source = this,
                        target = target,
                        ammo = null,
                        damage = hit.damage,
                        clientDelay = hit.clientDelay,
                        hitDelay = hit.hitDelay,
                    )
                } else {
                    manager.queueRangedDamage(
                        source = this,
                        target = target,
                        ammo = null,
                        damage = hit.damage,
                        hitDelay = hit.hitDelay,
                    )
                }
                // Real OSRS doesn't clamp damage after the roll, so the already-known hit.landed
                // here is the authentic signal for the Defence drain - no impact callback needed.
                if (target is Player && hit.landed) {
                    target.drainDivisionDefence()
                }
            }
            manager.continueCombat(this, target)
            return true
        }

        private fun ProtectedAccess.rollDivisionHit(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): DivisionHit {
            val accurate =
                manager.rollRangedAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = attack.type,
                    multiplier = 1.0,
                )
            if (!accurate) {
                return DivisionHit(landed = false, damage = 0)
            }

            val normalMaxHit =
                manager.calculateRangedMaxHit(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    multiplier = 1.0,
                    boltSpecDamage = 0,
                )
            val maxHit = TonalzticsOfRalosSpecialDamage.maxHit(normalMaxHit)
            if (maxHit <= 0) {
                return DivisionHit(landed = false, damage = 0)
            }
            return DivisionHit(
                landed = true,
                damage =
                    manager.rollRangedMaxHit(
                        source = this,
                        target = target,
                        attackType = attack.type,
                        attackStyle = attack.style,
                        multiplier = MAX_HIT_MULTIPLIER,
                        boltSpecDamage = 0,
                    ),
            )
        }

        private data class DivisionHit(
            val landed: Boolean,
            val damage: Int,
            val clientDelay: Int = 0,
            val hitDelay: Int = 0,
        )
    }

    private companion object {
        const val UNCHARGED_HIT_COUNT: Int = 1
        const val CHARGED_HIT_COUNT: Int = 2
        const val MAX_HIT_MULTIPLIER: Double = 0.75
        const val CHARGED_SPECIAL_ANIM: String = "seq.human_glaive_ralos01_charged_special"
        const val UNCHARGED_SPECIAL_ANIM: String = "seq.human_glaive_ralos01_uncharged_special"
        const val CHARGED_PLAYER_SPOTANIM: String = "spotanim.vfx_glaive_charged_special"
        const val UNCHARGED_PLAYER_SPOTANIM: String = "spotanim.vfx_glaive_uncharged_special"
        const val FIRST_TRAVEL_SPOTANIM: String = "spotanim.projanim_glaive_01_special"
        const val SECOND_TRAVEL_SPOTANIM: String = "spotanim.projanim_glaive_02_special"
        const val FIRST_IMPACT_SPOTANIM: String = "spotanim.vfx_glaive_01_impact_special_01"
        const val SECOND_IMPACT_SPOTANIM: String = "spotanim.vfx_glaive_02_impact_special_01"
        const val THROWN_PROJANIM: String = "projanim.thrown"

        const val CHARGED_THROW_SOUND = 7942
        const val CHARGED_SPIN_SOUND = 7943

        /** Matches this codebase's own same-tick-collision spacing convention (see Dragon
         * claws) so the spin cue doesn't collide with the throw cue. */
        const val CHARGED_SPIN_SOUND_DELAY = 20
    }
}

private fun PathingEntity.drainDivisionDefence() {
    when (this) {
        is Player -> {
            val drain = TonalzticsOfRalosSpecialDamage.defenceDrain(magicLvl)
            if (drain > 0) {
                statSub("stat.defence", constant = drain, percent = 0)
            }
        }

        is Npc -> {
            val drain = TonalzticsOfRalosSpecialDamage.defenceDrain(magicLvl)
            defenceLvl = (defenceLvl - drain).coerceAtLeast(0)
        }
    }
}

/** Pure Division values shared by all of its hit rolls. */
internal object TonalzticsOfRalosSpecialDamage {
    fun maxHit(normalMaxHit: Int): Int = normalMaxHit.coerceAtLeast(0) * 3 / 4

    fun defenceDrain(targetMagicLevel: Int): Int = targetMagicLevel.coerceAtLeast(0) / 10
}
