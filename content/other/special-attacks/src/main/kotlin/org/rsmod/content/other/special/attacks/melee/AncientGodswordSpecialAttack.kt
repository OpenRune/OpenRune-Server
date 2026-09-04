package org.rsmod.content.other.special.attacks.melee

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.math.min
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.constants
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.player.hit.queueImpactHit
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseHitpointsLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.script.onEvent
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Blood Sacrifice is a doubled-accuracy Slash hit with a 10% maximum-hit increase. A successful
 * hit marks its target for eight cycles; the target escapes by ending at least five tiles away
 * from the attacker. Otherwise it receives an unprotected 25-damage typeless hit.
 */
class AncientGodswordSpecialAttack
@Inject
constructor(private val sacrifice: AncientGodswordBloodSacrifice) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val bloodSacrifice = BloodSacrifice(manager, sacrifice)
        registerMelee("obj.ancient_godsword", bloodSacrifice)
        registerMelee("obj.br_ancient_godsword", bloodSacrifice)
    }

    private class BloodSacrifice(
        private val manager: SpecialAttackManager,
        private val sacrifice: AncientGodswordBloodSacrifice,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            bloodSacrifice(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            bloodSacrifice(target, attack)
            return true
        }

        private fun ProtectedAccess.bloodSacrifice(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ) {
            anim("seq.ngs_special_player")

            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = 2.0,
                    maxHitMultiplier = 1.1,
                    blockType = MeleeAttackType.Slash,
                )
            val source = player
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(source = this, target = target, damage = damage)
            // Real OSRS damage isn't clamped after the roll (no "overhit" cap to remaining HP),
            // so `damage` here is already the value that will land - no need to wait for an
            // impact callback to know whether the hit was nonzero.
            if (damage > 0) {
                // Weapon-effect spotanims in this combat slot are attacker-anchored so they
                // sweep with the swing (same pattern as Dragon claws' verified slash trail) -
                // not target-anchored, which is what made this look like a flat, static shape
                // planted on the ground instead of a blade effect on the sword.
                spotanim(
                    spot = "spotanim.ngs_special_spotanim",
                    slot = constants.spotanim_slot_combat,
                    height = 48,
                )
                sacrifice.mark(source, target)
            }
            manager.continueCombat(this, target)
        }
    }
}

/**
 * Owns independent Blood Sacrifice marks so concurrent specials can stack on one target.
 *
 * The world queue is processed before entity queues. Consequently, when its eight-cycle delay
 * expires, a one-cycle target hit queue resolves in that same entity cycle rather than adding a
 * ninth cycle. The LateCycle hook sees teleport flags before post-tick cleanup and cancels marks
 * on targets that teleport or leave an instance.
 */
@Singleton
class AncientGodswordBloodSacrifice
@Inject
constructor(
    private val worldQueues: WorldQueueList,
    private val npcHitModifier: NpcHitModifier,
) {
    private val marks = mutableListOf<Mark>()

    /**
     * Applies a Blood Sacrifice mark.
     *
     * [playerDamage], [playerHealCap], and [playerHitType] retain the normal Ancient godsword
     * defaults. They are configurable for the Deadman dogsword, whose documented PvP copy of
     * the effect is a 15-damage Magic hit with a 10-hitpoint healing cap.
     */
    fun mark(
        source: Player,
        target: PathingEntity,
        playerDamage: Int = BLOOD_SACRIFICE_DAMAGE,
        playerHealCap: Int = PLAYER_HEAL_CAP,
        playerHitType: HitType = HitType.Typeless,
    ) {
        val mark =
            Mark(
                source = source,
                sourceUid = source.uid.packed,
                target = target,
                targetUid = target.uidPacked(),
                targetBaseHitpoints = target.baseHitpoints(),
                playerDamage = playerDamage,
                playerHealCap = playerHealCap,
                playerHitType = playerHitType,
            )
        marks += mark
        worldQueues.add(BLOOD_SACRIFICE_DELAY) { resolve(mark) }
    }

    fun cancelTeleportedTargets() {
        for (mark in marks) {
            if (mark.target.pendingTeleport || mark.target.pendingTelejump || !mark.targetIsAlive()) {
                mark.cancelled = true
            }
        }
        marks.removeAll { it.cancelled }
    }

    private fun resolve(mark: Mark) {
        marks.remove(mark)
        if (mark.cancelled || !mark.isValid()) {
            return
        }
        if (!mark.target.isWithinDistance(mark.source, BLOOD_SACRIFICE_ESCAPE_DISTANCE - 1)) {
            return
        }

        // Real OSRS doesn't clamp damage after the roll, and both hits below use a fixed,
        // already-known damage value (never a live roll) - so the heal can use that same known
        // value directly instead of waiting on an impact callback. worldQueues.add mirrors the
        // hit's own TARGET_HIT_DELAY so the heal still lands on the same cycle the hit does.
        when (val target = mark.target) {
            is Player -> {
                target.queueImpactHit(
                    source = mark.source,
                    delay = TARGET_HIT_DELAY,
                    type = mark.playerHitType,
                    damage = mark.playerDamage,
                )
                worldQueues.add(TARGET_HIT_DELAY) { heal(mark, mark.playerDamage) }
            }

            is Npc -> {
                target.queueHit(
                    source = mark.source,
                    delay = TARGET_HIT_DELAY,
                    type = HitType.Typeless,
                    damage = BLOOD_SACRIFICE_DAMAGE,
                    modifier = npcHitModifier,
                )
                worldQueues.add(TARGET_HIT_DELAY) { heal(mark, BLOOD_SACRIFICE_DAMAGE) }
            }
        }
    }

    private fun heal(mark: Mark, damage: Int) {
        if (damage <= 0 || !mark.sourceIsAlive()) {
            return
        }
        val healCap = if (mark.target is Player) mark.playerHealCap else NPC_HEAL_CAP
        val amount =
            BloodSacrificeHeal.healAmount(
                damage = damage,
                targetBaseHitpoints = mark.targetBaseHitpoints,
                healCap = healCap,
            )
        if (amount > 0) {
            mark.source.statHeal("stat.hitpoints", constant = amount, percent = 0)
        }
    }

    private fun Mark.isValid(): Boolean = sourceIsAlive() && targetIsAlive()

    private fun Mark.sourceIsAlive(): Boolean =
        source.isSlotAssigned && source.uid.packed == sourceUid && source.hitpoints > 0

    private fun Mark.targetIsAlive(): Boolean =
        target.isSlotAssigned && target.uidPacked() == targetUid && target.hitpoints() > 0

    private class Mark(
        val source: Player,
        val sourceUid: Int,
        val target: PathingEntity,
        val targetUid: Int,
        val targetBaseHitpoints: Int,
        val playerDamage: Int,
        val playerHealCap: Int,
        val playerHitType: HitType,
        var cancelled: Boolean = false,
    )

    private companion object {
        const val BLOOD_SACRIFICE_DAMAGE = 25
        const val BLOOD_SACRIFICE_DELAY = 8
        const val BLOOD_SACRIFICE_ESCAPE_DISTANCE = 5
        const val TARGET_HIT_DELAY = 1
        const val NPC_HEAL_CAP = 25
        const val PLAYER_HEAL_CAP = 15
    }
}

/**
 * Pure Blood Sacrifice heal math, kept separate from [AncientGodswordBloodSacrifice] so it's
 * testable without the world-queue/mark plumbing: the heal is 15% of the target's base hitpoints
 * (25 flat for NPC targets, whose base HP is usually far higher), capped further by the damage
 * that was actually dealt.
 */
internal object BloodSacrificeHeal {
    fun healAmount(damage: Int, targetBaseHitpoints: Int, healCap: Int): Int {
        val percentCap = targetBaseHitpoints * 15 / 100
        val maximum = min(percentCap, healCap)
        return min(damage, maximum).coerceAtLeast(0)
    }
}

/** Registers the teleport safeguard without requiring a SpecialAttackModule change. */
class AncientGodswordBloodSacrificeScript
@Inject
constructor(private val sacrifice: AncientGodswordBloodSacrifice) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<GameLifecycle.LateCycle> { sacrifice.cancelTeleportedTargets() }
    }
}

private fun PathingEntity.baseHitpoints(): Int =
    when (this) {
        is Npc -> baseHitpointsLvl
        is Player -> baseHitpointsLvl
    }

private fun PathingEntity.hitpoints(): Int =
    when (this) {
        is Npc -> hitpoints
        is Player -> hitpoints
    }

private fun PathingEntity.uidPacked(): Int =
    when (this) {
        is Npc -> uid.packed
        is Player -> uid.packed
    }
