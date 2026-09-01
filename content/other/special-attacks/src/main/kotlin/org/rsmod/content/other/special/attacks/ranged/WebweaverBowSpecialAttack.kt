package org.rsmod.content.other.special.attacks.ranged

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.CombatChargeManager
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.mechanics.toxins.NpcPoisonEffectService
import org.rsmod.api.mechanics.toxins.impl.PlayerPoison
import org.rsmod.api.npc.isValidTarget as isValidNpcTarget
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.obj.charges.ObjChargeManager.Companion.isFailure
import org.rsmod.api.player.cheat.adminMaxHit
import org.rsmod.api.player.isValidTarget as isValidPlayerTarget
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.RangedSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.game.type.getInvObj

/** Swarm consumes one ether and fires four independently rolled, poison-capable arrows. */
class WebweaverBowSpecialAttack
@Inject
constructor(
    private val charges: CombatChargeManager,
    private val random: GameRandom,
    private val poisons: NpcPoisonEffectService,
    private val worldQueues: WorldQueueList,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerRanged(
            "obj.wild_cave_webweaver_charged",
            Swarm(manager, charges, random, poisons, worldQueues),
        )
    }

    private class Swarm(
        private val manager: SpecialAttackManager,
        private val charges: CombatChargeManager,
        private val random: GameRandom,
        private val poisons: NpcPoisonEffectService,
        private val worldQueues: WorldQueueList,
    ) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean = swarm(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean = swarm(target, attack)

        private fun ProtectedAccess.swarm(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Boolean {
            val weaponType = getInvObj(attack.weapon)
            if (charges.getWeaponCharges(player, ETHER_VAROBJ) < 1) {
                detractWebweaverCharge()
                manager.stopCombat(this)
                mes("Your Webweaver bow has run out of charges.")
                return false
            }

            anim(SWARM_SEQUENCE)
            weaponType.paramOrNull(params.attack_sound_stance1)?.let { soundSynth(it) }
            spotanim(
                SWARM_LAUNCH_SPOTANIM,
                height = 96,
                slot = constants.spotanim_slot_combat,
            )

            // Live feedback (confirmed against the wiki's own screenshot of the special): Swarm
            // never spawns a visible flying projectile the way a normal shot does - just the
            // launch spotanim on the player and the impact spotanim on the target. The previous
            // version here called `manager.spawnProjectile` per hit, which rendered an actual
            // arrow model traveling to the target - removed entirely, along with the `proj_type`
            // resolution it needed. The first hit lands on the tick the special fires, and all
            // three remaining hits land together on the next tick - a 2-tick spread, not 4
            // separate ticks (one per hit, the previous behavior here).
            val hits =
                List(SWARM_HIT_COUNT) { index ->
                    val damage = swarmDamage(target, attack)
                    val poison = damage > 0 && random.randomBoolean(POISON_ROLL_DENOMINATOR)
                    val tickOffset = if (index == 0) 0 else 1
                    SwarmHit(
                        damage = damage,
                        clientDelay = tickOffset * CLIENT_CYCLES_PER_TICK,
                        hitDelay = 1 + tickOffset,
                        poison = poison,
                    )
                }

            val chargesLeft =
                detractWebweaverCharge()
                    ?: run {
                        manager.stopCombat(this)
                        return false
                    }

            manager.giveCombatXp(this, target, attack, hits.sumOf { it.damage })
            hits.forEachIndexed { index, hit ->
                target.spotanim(
                    SWARM_IMPACT_SPOTANIM,
                    delay = hit.clientDelay,
                    height = 96,
                )
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
                // Real OSRS doesn't clamp damage after the roll, so the already-known poison
                // outcome (rolled alongside damage above) is authentic - no impact callback needed.
                // Deferred to match this specific hit's own landing tick (hitDelay) rather than
                // applying at cast time for every hit regardless of when it actually lands - hits
                // 2-4 land a tick after hit 1, so their poison shouldn't start counting early.
                if (hit.poison) {
                    worldQueues.add(hit.hitDelay) {
                        if (target.isStillValidTarget()) {
                            applyPoison(player, target)
                        }
                    }
                }
            }

            if (chargesLeft == 0) {
                mes("Your Webweaver bow has run out of charges.")
                manager.stopCombat(this)
            } else {
                manager.continueCombat(this, target)
            }
            return true
        }

        // This used to hand-roll the bit manipulation directly (reading/writing `player.righthand`
        // vars via `getBits`/`withBits`), resolving the varobj via `RSCMType.VAROBJ` - correct on
        // its own, but `ObjChargeManager` (the shared, intended primitive for this exact purpose)
        // had its own separate, real bug: three of its four functions resolved the same kind of
        // "varobj.*" string via `RSCMType.VARCON` instead, which is a completely unrelated
        // namespace (content-script tracking vars, not item charges) - `asRSCM` enforces an exact
        // prefix match, so that always threw `IllegalArgumentException`. Fixed at the source in
        // `ObjChargeManager.kt` rather than worked around here, since it affected every caller
        // (Tumeken's shadow included) the moment they actually exercised these paths.
        private fun ProtectedAccess.detractWebweaverCharge(): Int? {
            val result = charges.attemptDetractWeapon(player, ETHER_VAROBJ)
            if (result.isFailure()) {
                return null
            }
            return (result as ObjChargeManager.Uncharge.Success).chargesLeft
        }

        private fun ProtectedAccess.swarmDamage(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Int {
            val accurate =
                manager.rollRangedAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = attack.type,
                    multiplier = SWARM_ACCURACY_MULTIPLIER,
                )
            if (!accurate) {
                return 0
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
            val swarmMaxHit = WebweaverBowSpecialDamage.maxHit(normalMaxHit)
            if (swarmMaxHit <= 0) {
                return 0
            }
            return if (player.adminMaxHit) swarmMaxHit else random.of(1..swarmMaxHit)
        }

        private fun applyPoison(source: Player, target: PathingEntity) {
            when (target) {
                is Npc -> poisons.apply(source, target, WEBWEAVER_POISON_DAMAGE)
                is Player -> PlayerPoison.tryPoison(target, initialDamage = WEBWEAVER_POISON_DAMAGE)
            }
        }

        private data class SwarmHit(
            val damage: Int,
            val clientDelay: Int,
            val hitDelay: Int,
            val poison: Boolean,
        )

        private fun PathingEntity.isStillValidTarget(): Boolean =
            when (this) {
                is Npc -> isValidNpcTarget()
                is Player -> isValidPlayerTarget()
            }
    }

    private companion object {
        const val ETHER_VAROBJ: String = "varobj.charges_16383"
        const val SWARM_HIT_COUNT: Int = 4
        const val SWARM_ACCURACY_MULTIPLIER: Double = 2.0
        const val POISON_ROLL_DENOMINATOR: Int = 4
        const val WEBWEAVER_POISON_DAMAGE: Int = 4
        const val CLIENT_CYCLES_PER_TICK: Int = 30
        const val SWARM_SEQUENCE: String = "seq.human_special01_webweaver"
        const val SWARM_LAUNCH_SPOTANIM: String = "spotanim.fx_webweaver01_launch_spotanim"
        const val SWARM_IMPACT_SPOTANIM: String = "spotanim.fx_webweaver01_impact_spotanim"
    }
}

internal object WebweaverBowSpecialDamage {
    /** Ceiling of forty percent of the normal maximum hit. */
    fun maxHit(normalMaxHit: Int): Int =
        ((normalMaxHit.coerceAtLeast(0) * 2) + 4) / 5
}
