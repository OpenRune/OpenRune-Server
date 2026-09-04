package org.rsmod.api.mechanics.toxins

import dev.openrune.types.HitmarkTypeGroup
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.config.refs.done.hitmark_groups
import org.rsmod.api.mechanics.toxins.impl.PlayerPoison
import org.rsmod.api.mechanics.toxins.impl.PlayerVenom
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.npc.isValidTarget
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.queue.WorldQueueList

/**
 * NPC-side poison/venom, reusing [PlayerPoison]/[PlayerVenom]'s own damage formulas (severity
 * decaying at 1/tick, `damageForSeverity`; venom escalating 6/8/10.../20 every 30 ticks) since
 * NPCs otherwise get poisoned/envenomed identically to players.
 *
 * Lighter-weight than the player version: no varp/status-orb sync (NPCs have no such UI), no
 * worn-item or [ToxinImmunity]-style immunity checks, and tracked in-memory
 * (`WeakHashMap<Npc, ...>`, self-rescheduling via [WorldQueueList]) rather than persisted - the
 * same documented scope as this session's other timed-effect additions
 * ([BurnEffectService][org.rsmod.api.mechanics.toxins.BurnEffectService] is the sibling example).
 * Envenoming clears any active poison, matching [PlayerVenom]'s own behavior; poisoning an already
 * envenomed target is a no-op.
 */
@Singleton
public class NpcPoisonEffectService
@Inject
constructor(
    private val worldQueues: WorldQueueList,
    private val npcHitModifier: NpcHitModifier,
) {
    private val poisonSeverity = WeakHashMap<Npc, Int>()
    private val venomStrikes = WeakHashMap<Npc, Int>()

    /** Applies ordinary poison from [initialDamage] worth of damage (matches weapon-poison use). */
    public fun apply(source: Player, target: Npc, initialDamage: Int) {
        if (initialDamage <= 0 || venomStrikes.containsKey(target)) {
            return
        }
        val severity = PlayerPoison.severityForInitialDamage(initialDamage)
        val current = poisonSeverity[target] ?: 0
        if (!NpcPoisonOverride.shouldApply(initialDamage, severity, current)) {
            return
        }
        // Wiki: poison damages "once every 30 game ticks" as a recurring cycle - nothing hits the
        // instant it's applied (matches PlayerVenom.applyVenom's own already-correct behavior).
        // This used to fire an immediate hit here too, matching PlayerPoison.applyPoison's own
        // pre-fix bug.
        val alreadyTicking = current > 0
        poisonSeverity[target] = severity
        if (!alreadyTicking) {
            schedulePoisonTick(target)
        }
    }

    public fun applyVenom(source: Player, target: Npc) {
        if (venomStrikes.containsKey(target)) {
            return
        }
        poisonSeverity.remove(target)
        venomStrikes[target] = 0
        scheduleVenomTick(target)
    }

    private fun schedulePoisonTick(target: Npc) {
        worldQueues.add(PlayerPoison.TICK_INTERVAL) {
            var severity = poisonSeverity[target] ?: return@add
            if (severity <= 0 || !target.isValidTarget()) {
                poisonSeverity.remove(target)
                return@add
            }
            queueHit(target, PlayerPoison.damageForSeverity(severity), hitmark_groups.poison_damage)
            severity--
            if (severity <= 0) {
                poisonSeverity.remove(target)
            } else {
                poisonSeverity[target] = severity
                schedulePoisonTick(target)
            }
        }
    }

    private fun scheduleVenomTick(target: Npc) {
        worldQueues.add(PlayerVenom.TICK_INTERVAL) {
            val strikes = venomStrikes[target] ?: return@add
            if (!target.isValidTarget()) {
                venomStrikes.remove(target)
                return@add
            }
            queueHit(target, PlayerVenom.damageForStrikeIndex(strikes), hitmark_groups.venom)
            venomStrikes[target] = strikes + 1
            scheduleVenomTick(target)
        }
    }

    private fun queueHit(target: Npc, damage: Int, hitmark: HitmarkTypeGroup) {
        target.queueHit(
            delay = 1,
            type = HitType.Typeless,
            damage = damage,
            modifier = npcHitModifier,
            hitmark = hitmark,
        )
    }
}

/**
 * Whether a new poison application should override the currently active one, matching
 * [PlayerPoison]'s own override rule: a strictly weaker poison (lower first-hit damage) never
 * overrides; an equal-damage one only overrides if its severity (remaining duration) is actually
 * higher.
 */
internal object NpcPoisonOverride {
    fun shouldApply(newInitialDamage: Int, newSeverity: Int, currentSeverity: Int): Boolean {
        val currentDamage =
            if (currentSeverity > 0) PlayerPoison.damageForSeverity(currentSeverity) else 0
        if (newInitialDamage < currentDamage) {
            return false
        }
        if (newInitialDamage == currentDamage && newSeverity <= currentSeverity) {
            return false
        }
        return true
    }
}
