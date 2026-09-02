package org.rsmod.content.other.special.attacks.ranged

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.RangedAmmoManager
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.done.hitmark_groups
import org.rsmod.api.player.hit.queueImpactHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.RangedSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.game.type.getInvObj

/**
 * Phantom Strike makes a normal Ranged hit and, against players, makes the target bleed 75% of
 * the final initial-hit damage in ten-damage Ranged ticks.
 *
 * Deadman and Bounty Rusher cache variants have different ammunition and accuracy rules, but use
 * the same visual assets and player-only bleed effect.
 */
class MorrigansJavelinSpecialAttack
@Inject
constructor(
    private val ammunition: RangedAmmoManager,
    private val bleed: MorrigansJavelinBleed,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val deadman =
            PhantomStrike(
                manager = manager,
                ammunition = ammunition,
                bleed = bleed,
                accuracyMultiplier = 1.0,
                consumesThrownAmmo = true,
            )
        registerRanged("obj.morrigans_javelin", deadman)
        registerRanged("obj.br_morrigans_javelin", deadman)

        registerRanged(
            "obj.morrigans_javelin_bh",
            PhantomStrike(
                manager = manager,
                ammunition = ammunition,
                bleed = bleed,
                accuracyMultiplier = 1.5,
                consumesThrownAmmo = false,
            ),
        )
    }

    private class PhantomStrike(
        private val manager: SpecialAttackManager,
        private val ammunition: RangedAmmoManager,
        private val bleed: MorrigansJavelinBleed,
        private val accuracyMultiplier: Double,
        private val consumesThrownAmmo: Boolean,
    ) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean = phantomStrike(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean = phantomStrike(target, attack)

        private fun ProtectedAccess.phantomStrike(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Boolean {
            val weaponType = getInvObj(attack.weapon)
            if (!ammunition.attemptAmmoUsage(player, weaponType, ammo = null)) {
                manager.stopCombat(this)
                return false
            }

            anim(MORRIGANS_JAVELIN_SEQUENCE)
            spotanim(
                spot = MORRIGANS_JAVELIN_LAUNCH_SPOTANIM,
                height = 0,
                slot = constants.spotanim_slot_combat,
            )
            val projectile =
                manager.spawnProjectile(
                    source = this,
                    target = target,
                    spotanim = MORRIGANS_JAVELIN_TRAVEL_SPOTANIM,
                    projanim = THROWN_PROJANIM,
                )
            val damage =
                manager.rollRangedDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = accuracyMultiplier,
                )

            if (consumesThrownAmmo) {
                ammunition.useThrownWeapon(
                    player = player,
                    weaponType = weaponType,
                    dropCoord = target.coords,
                    dropDelay = projectile.serverCycles,
                )
            }

            val source = player
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueRangedHit(
                source = this,
                target = target,
                ammo = null,
                damage = damage,
                clientDelay = projectile.clientCycles,
                hitDelay = projectile.serverCycles,
            )
            // Real OSRS doesn't clamp damage after the roll, so the already-known damage here is
            // the authentic value to seed the bleed from - no impact callback needed.
            if (target is Player && damage > 0) {
                bleed.apply(source, target, damage)
            }

            if (consumesThrownAmmo && player.righthand == null) {
                mes("That was your last one!")
            } else {
                manager.continueCombat(this, target)
            }
            return true
        }
    }

    private companion object {
        const val MORRIGANS_JAVELIN_SEQUENCE = "seq.weapon_morrigans_javelin_special01"
        const val MORRIGANS_JAVELIN_LAUNCH_SPOTANIM = "spotanim.morrigans_javelin_spotanim"
        const val MORRIGANS_JAVELIN_TRAVEL_SPOTANIM = "spotanim.morrigans_javelin_projanim"
        const val THROWN_PROJANIM = "projanim.thrown"
    }
}

/**
 * Owns all active Morrigan javelin bleed stacks. Each stack is independent, which preserves
 * successive successful javelin specials rather than replacing a prior bleed.
 *
 * Damage is deliberately queued as [HitType.Ranged] through [Player.queueImpactHit]. This means
 * each tick is modified at impact time by normal player hit rules, including Ranged protection
 * prayers, while retaining ordinary combat attribution and death processing.
 */
@Singleton
class MorrigansJavelinBleed
@Inject
constructor(private val worldQueues: WorldQueueList) {
    private val active: IdentityHashMap<Player, MutableList<BleedStack>> = IdentityHashMap()

    fun apply(
        source: Player,
        target: Player,
        initialFinalDamage: Int,
    ): Boolean {
        val remainingDamage = MorrigansJavelinBleedDamage.totalDamage(initialFinalDamage)
        if (remainingDamage <= 0 || !target.isValidTarget()) {
            return false
        }

        val stacks = active.getOrPut(target) { mutableListOf() }
        val hadActiveBleed = stacks.isNotEmpty()
        val stack =
            BleedStack(
                source = source,
                target = target,
                targetUid = target.uid.packed,
                remainingDamage = remainingDamage,
            )
        stacks += stack
        target.mes(if (hadActiveBleed) CONTINUE_BLEED_MESSAGE else START_BLEED_MESSAGE)
        arm(stack)
        return true
    }

    private fun arm(stack: BleedStack) {
        worldQueues.add(TICK_INTERVAL) {
            tick(stack)
        }
    }

    private fun tick(stack: BleedStack) {
        if (!isActive(stack) || !stack.targetIsCurrent()) {
            remove(stack)
            return
        }

        val damage = minOf(DAMAGE_PER_TICK, stack.remainingDamage)
        stack.remainingDamage -= damage
        stack.target.queueImpactHit(
            source = stack.source,
            delay = TARGET_HIT_DELAY,
            type = HitType.Ranged,
            damage = damage,
            hitmark = hitmark_groups.bleed,
        )

        if (stack.remainingDamage <= 0) {
            remove(stack)
        } else {
            arm(stack)
        }
    }

    private fun isActive(stack: BleedStack): Boolean = active[stack.target]?.contains(stack) == true

    private fun remove(stack: BleedStack) {
        val stacks = active[stack.target] ?: return
        stacks.remove(stack)
        if (stacks.isEmpty()) {
            active.remove(stack.target)
        }
    }

    private fun BleedStack.targetIsCurrent(): Boolean =
        target.isValidTarget() && target.uid.packed == targetUid

    private class BleedStack(
        val source: Player,
        val target: Player,
        val targetUid: Int,
        var remainingDamage: Int,
    )

    private companion object {
        const val DAMAGE_PER_TICK = 10
        const val TICK_INTERVAL = 1
        const val TARGET_HIT_DELAY = 1
        const val START_BLEED_MESSAGE = "You start to bleed as a result of the javelin strike."
        const val CONTINUE_BLEED_MESSAGE = "You continue to bleed as a result of the javelin strike."
    }
}

internal object MorrigansJavelinBleedDamage {
    fun totalDamage(initialFinalDamage: Int): Int =
        (initialFinalDamage.coerceAtLeast(0).toLong() * 75L / 100L).toInt()

    fun ticks(initialFinalDamage: Int): List<Int> {
        var remaining = totalDamage(initialFinalDamage)
        val ticks = mutableListOf<Int>()
        while (remaining > 0) {
            val damage = minOf(10, remaining)
            ticks += damage
            remaining -= damage
        }
        return ticks
    }
}
