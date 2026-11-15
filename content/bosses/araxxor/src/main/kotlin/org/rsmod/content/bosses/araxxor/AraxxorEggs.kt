package org.rsmod.content.bosses.araxxor

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.Collections
import java.util.IdentityHashMap
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.repeatTick
import org.rsmod.api.bosses.runtime.spawnOwnedNpc
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.npc.queueDeath
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType

private const val HATCH_SPOT_ACIDIC = "spotanim.araxyte_acid_spider_egg"
private const val HATCH_SPOT_RUPTURA = "spotanim.araxyte_explosive_spider_egg"
private const val HATCH_SPOT_MIRRORBACK = "spotanim.araxyte_mirrorback_spider_egg"

private val NoopNpcHitModifier = NpcHitModifier {}

private val logger = InlineLogger()

private const val CRUMBLE_RADIUS = 25
private const val CRUMBLE_GUARD_TICKS = 20
private const val ENGAGE_DELAY = 5
private const val ENGAGE_RANGE = 64
private const val ENGAGE_LOOP_TICKS = 100_000
private const val ACIDIC_AP_RANGE = 7

private const val MIRRORBACK_REDIRECT_PERCENT = 20
private const val MIRRORBACK_RECOIL_PERCENT = 50

private const val RUPTURA_NPC_RADIUS = 3
private val RUPTURA_NPC_DAMAGE = intArrayOf(80, 64, 48, 33)

private val EGG_OFFSETS =
    listOf(
        12 to -9,
        6 to -12,
        -1 to -11,
        -6 to -8,
        -11 to -1,
        -10 to 8,
        -2 to 13,
        6 to 13,
        13 to 9,
    )

private val EGG_CYCLE = listOf(EGG_ACIDIC, EGG_MIRRORBACK, EGG_RUPTURA)

private class Kind(val egg: String, val minion: String, val hatchSpot: String)

private val KINDS =
    listOf(
        Kind(EGG_ACIDIC, MINION_ACIDIC, HATCH_SPOT_ACIDIC),
        Kind(EGG_MIRRORBACK, MINION_MIRRORBACK, HATCH_SPOT_MIRRORBACK),
        Kind(EGG_RUPTURA, MINION_RUPTURA, HATCH_SPOT_RUPTURA),
    )

@Singleton
class AraxxorEggs
@Inject
constructor(
    private val deps: BossDeps,
    private val interactions: AiPlayerInteractions,
    private val npcList: NpcList,
) {
    private val crumbled: MutableSet<Npc> = Collections.newSetFromMap(IdentityHashMap())
    private val eggIds by lazy { KINDS.map { it.egg.asRSCM(RSCMType.NPC) }.toSet() }
    private val minionIds by lazy { KINDS.map { it.minion.asRSCM(RSCMType.NPC) }.toSet() }
    private val mirrorbackId by lazy { MINION_MIRRORBACK.asRSCM(RSCMType.NPC) }
    private val acidicId by lazy { MINION_ACIDIC.asRSCM(RSCMType.NPC) }
    private val ballId by lazy { BALL_NPC.asRSCM(RSCMType.NPC) }
    private val rangedIds by lazy { setOf(ARAXXOR.asRSCM(RSCMType.NPC), acidicId) }
    private val broodIds by lazy {
        (listOf(ARAXXOR) + KINDS.flatMap { listOf(it.egg, it.minion) })
            .map { it.asRSCM(RSCMType.NPC) }
            .toSet()
    }

    fun spawn(boss: Npc) {
        val pattern = deps.random.of(EGG_CYCLE.size)
        boss.vars[PATTERN_VARN] = pattern
        boss.vars[HATCH_IN_VARN] = FIRST_HATCH_STANDARDS
        EGG_OFFSETS.forEachIndexed { index, (dx, dz) ->
            val type = EGG_CYCLE[(index + pattern) % EGG_CYCLE.size]
            val egg = deps.spawnOwnedNpc(boss, type, boss.coords.translate(dx, dz)) ?: return@forEachIndexed
            egg.respawns = false
            egg.mode = NpcMode.None
            egg.anim(EGG_SPAWN_ANIM, 0, 0)
        }
    }

    fun hatchNext(boss: Npc) {
        if (!boss.isSlotAssigned || boss.hitpoints <= 0) return
        val egg =
            deps.encounterRegistry.of(boss).firstOwnedNpc {
                it.visType.id in eggIds && it.isSlotAssigned && it.hitpoints > 0
            } ?: return
        val kind = KINDS.first { it.egg.asRSCM(RSCMType.NPC) == egg.visType.id }
        val damageTaken = EGG_HITPOINTS - egg.hitpoints
        val tile = egg.coords
        deps.npcRepo.del(egg, Int.MAX_VALUE)
        if (damageTaken >= MINION_HITPOINTS) return

        val minion = deps.spawnOwnedNpc(boss, kind.minion, tile) ?: return
        minion.respawns = false
        minion.baseHitpointsLvl = MINION_HITPOINTS
        minion.hitpoints = MINION_HITPOINTS - damageTaken
        minion.mode = NpcMode.None
        minion.anim(HATCH_ANIM, 0, 0)
        minion.spotanim(kind.hatchSpot, 0, 0, 0)
        minion.vars[ENGAGE_AT_VARN] = deps.mapClock.cycle + ENGAGE_DELAY
    }

    /**
     * Re-targets the boss and every hatched araxyte each tick they have no interaction, since an
     * interaction is dropped whenever the target is lost; a single engage leaves them idle.
     */
    @OptIn(InternalApi::class)
    fun keepEngaged(boss: Npc) {
        deps.repeatTick(
            ticks = ENGAGE_LOOP_TICKS,
            onTick = tick@{
                if (!boss.isSlotAssigned || boss.hitpoints <= 0) return@tick false
                val target = nearestPlayer(boss) ?: return@tick true
                val now = deps.mapClock.cycle
                for (npc in deps.encounterRegistry.of(boss).ownedNpcSnapshot() + boss) {
                    if (!npc.isSlotAssigned || npc.hitpoints <= 0 || npc.hasInteraction()) continue
                    if (npc.visType.id in eggIds || npc.visType.id == ballId) continue
                    if (now < npc.vars[ENGAGE_AT_VARN]) continue
                    engage(npc, target)
                }
                true
            },
        )
    }

    private fun engage(npc: Npc, target: Player) {
        npc.facePlayer(target)
        if (npc.visType.id in rangedIds) {
            if (npc.visType.id == acidicId) npc.apRangeOverride = ACIDIC_AP_RANGE
            npc.apPlayer2(target, interactions)
        } else {
            npc.opPlayer2(target, interactions)
        }
    }

    private fun nearestPlayer(npc: Npc): Player? =
        deps.playerList
            .filter {
                it.isValidTarget() &&
                    it.coords.level == npc.coords.level &&
                    npc.edgeDistanceTo(it.coords) <= ENGAGE_RANGE
            }
            .minByOrNull { npc.edgeDistanceTo(it.coords) }

    fun crumbleEggs(boss: Npc) {
        if (!crumbled.add(boss)) return
        deps.worldQueues.add(CRUMBLE_GUARD_TICKS) { crumbled.remove(boss) }
        var killed = 0
        for (npc in npcList) {
            if (
                npc.visType.id in minionIds &&
                    npc.isSlotAssigned &&
                    npc.hitpoints > 0 &&
                    npc.edgeDistanceTo(boss) <= CRUMBLE_RADIUS
            ) {
                npc.hitpoints = 0
                npc.queueDeath()
                killed++
            }
        }
        logger.info { "Araxxor brood crumble: $killed araxytes queued to die." }
        deps.worldQueues.add(1) {
            for (npc in npcList) {
                if (
                    npc.visType.id in eggIds &&
                        npc.isSlotAssigned &&
                        npc.hitpoints > 0 &&
                        npc.edgeDistanceTo(boss) <= CRUMBLE_RADIUS
                ) {
                    npc.anim(EGG_DEATH_ANIM, 0, 0)
                }
            }
        }
    }

    fun redirect(boss: Npc, damage: Int, attacker: Player): Int {
        val mirrorback =
            deps.encounterRegistry.of(boss).firstOwnedNpc {
                it.visType.id == mirrorbackId && it.isSlotAssigned && it.hitpoints > 0
            } ?: return damage
        val redirected = damage * MIRRORBACK_REDIRECT_PERCENT / 100
        if (redirected <= 0) return damage
        mirrorback.queueHit(attacker, 1, HitType.Typeless, redirected, NoopNpcHitModifier)
        val recoil = redirected * MIRRORBACK_RECOIL_PERCENT / 100
        if (recoil > 0) attacker.queueHit(1, HitType.Typeless, recoil, NoopPlayerHitModifier)
        return damage - redirected
    }

    fun rupturaBlast(ruptura: Npc) {
        for (other in npcList) {
            if (other === ruptura || !other.isSlotAssigned || other.hitpoints <= 0) continue
            if (other.visType.id !in broodIds) continue
            val distance = ruptura.edgeDistanceTo(other)
            if (distance > RUPTURA_NPC_RADIUS) continue
            other.queueHit(ruptura, 1, HitType.Typeless, RUPTURA_NPC_DAMAGE[distance], NoopNpcHitModifier)
        }
    }
}
