package org.rsmod.content.raids.toa.raid.encounter.scabaras.kephri

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.bosses.spec.HitType as BossHitType
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.mechanics.toxins.impl.PlayerPoison
import org.rsmod.api.player.hit.queueImpactHit
import org.rsmod.api.player.output.mes
import org.rsmod.content.raids.toa.party.ToaInvocationKey
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.content.raids.toa.raid.encounter.crondis.zebak.isBeside
import org.rsmod.content.raids.toa.raid.encounter.spotanim
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitType
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid

internal class KephriGuardians(private val room: KephriEncounter) {
    private val deps = room.raid.deps
    private val guardians = ArrayList<Guardian>()
    private var generation = 0

    enum class Kind {
        SPITTING,
        SOLDIER,
        ARCANE,
    }

    private class Guardian(val npc: Npc, val kind: Kind, val spawned: Int) {
        var nextAttack = 0
        var nextHeal = 0
        var target: Player? = null
        var retarget = 0
        var chargeAt = 0
        var relocation = 0
        var hits = 0
        var poisonedOnce = false
    }

    fun spawnFor(stun: Int) {
        val overlords = room.raid.isActive(ToaInvocationKey.MoreOverlords)
        val kinds =
            if (stun == 1) {
                listOfNotNull(Kind.SPITTING, Kind.SOLDIER.takeIf { overlords })
            } else {
                listOfNotNull(Kind.SOLDIER, Kind.ARCANE, Kind.SPITTING.takeIf { overlords })
            }
        val launched = generation
        for ((index, kind) in kinds.withIndex()) {
            room.schedule(index) { if (launched == generation) spawn(kind) }
        }
    }

    fun arcaneHit(npc: Npc, hit: Hit) {
        val guardian = guardians.firstOrNull { it.npc === npc } ?: return
        if (!hit.isFromPlayer) return
        guardian.hits++
        if (hit.damage >= RELOCATE_DAMAGE || guardian.hits >= RELOCATE_BASE_HITS + room.teamSize) {
            relocate(guardian)
        }
    }

    fun clear() {
        generation++
        for (guardian in guardians) room.despawn(guardian.npc)
        guardians.clear()
    }

    fun tick() {
        if (room.stage != ToaStage.STARTED || guardians.isEmpty()) return
        val players = room.targets()
        val cycle = deps.mapClock.cycle
        for (guardian in guardians.toList()) {
            if (!guardian.npc.isSlotAssigned || guardian.npc.hitpoints <= 0) {
                guardians.remove(guardian)
                continue
            }
            when (guardian.kind) {
                Kind.SPITTING -> spit(guardian, players, cycle)
                Kind.SOLDIER -> soldier(guardian, players, cycle)
                Kind.ARCANE -> arcane(guardian, players, cycle)
            }
        }
    }

    fun spawnNow(kind: Kind) = spawn(kind)

    private fun spawn(kind: Kind) {
        val type =
            when (kind) {
                Kind.SPITTING -> KephriNpcs.GUARDIAN_RANGED
                Kind.SOLDIER -> KephriNpcs.GUARDIAN_MELEE
                Kind.ARCANE -> KephriNpcs.GUARDIAN_MAGE
            }
        val tile = room.coords(SPAWN_TILES.getValue(kind))
        val npc = room.spawn(type, tile)
        npc.anim(if (kind == Kind.ARCANE) KephriSeqs.FLYING_SPAWN else KephriSeqs.SCARAB_SPAWN)
        val cycle = deps.mapClock.cycle
        val guardian = Guardian(npc, kind, cycle)
        guardian.nextAttack = cycle + FIRST_ATTACK
        guardian.nextHeal = cycle + FIRST_HEAL
        guardian.chargeAt = cycle + CHARGE_START
        guardians += guardian
    }

    private fun spit(guardian: Guardian, players: List<Player>, cycle: Int) {
        if (cycle < guardian.nextAttack) return
        val npc = guardian.npc
        val inRange = players.filter { it.coords.chebyshevDistance(npc.coords) <= SPIT_RANGE }
        if (inRange.isEmpty()) return
        guardian.nextAttack = cycle + SPIT_SPEED
        npc.anim(KephriSeqs.SCARAB_RANGED)
        for (player in inRange) {
            projectile(KephriSpots.SCARAB_SHOT, npc.coords, player.coords, SPIT_HEIGHTS)
            room.schedule(SPIT_HIT_DELAY) {
                if (!npc.isSlotAssigned || room.stage != ToaStage.STARTED) return@schedule
                val damage = room.combatantOf(npc)?.rollScaled(SPIT_MAX_HIT) ?: 0
                player.queueImpactHit(
                    npc,
                    1,
                    HitType.Ranged,
                    damage,
                    deps.playerHitModifier,
                    penetration = PRAYER_IGNORED,
                )
                if (!guardian.poisonedOnce && !PlayerPoison.isPoisoned(player)) {
                    guardian.poisonedOnce = true
                    if (PlayerPoison.tryPoison(player, SPIT_POISON, severity = POISON_SEVERITY)) {
                        player.mes(POISON_TEXT)
                    }
                }
            }
        }
    }

    private fun soldier(guardian: Guardian, players: List<Player>, cycle: Int) {
        val npc = guardian.npc
        if (guardian.target.let { it == null || it !in players } || cycle >= guardian.retarget) {
            guardian.target = if (players.isEmpty()) null else players[deps.random.of(0, players.lastIndex)]
            guardian.retarget = cycle + deps.random.of(RETARGET_MIN, RETARGET_MAX)
        }
        val target = guardian.target
        if (target != null) {
            if (npc.isBeside(target)) {
                npc.abortRoute()
                if (cycle >= guardian.nextAttack) meleeHit(guardian, target, cycle)
            } else {
                npc.walk(target.coords)
            }
        }
        if (cycle >= guardian.nextHeal) heal(guardian, cycle)
    }

    private fun meleeHit(guardian: Guardian, target: Player, cycle: Int) {
        val npc = guardian.npc
        guardian.nextAttack = cycle + SOLDIER_SPEED
        npc.anim(KephriSeqs.SCARAB_MELEE)
        val combatant = room.combatantOf(npc) ?: return
        val damage =
            if (combatant.rollAccuracy(target, BossHitType.Melee, MeleeAttackType.Slash)) {
                combatant.rollScaled(SOLDIER_MAX_HIT)
            } else {
                0
            }
        target.queueImpactHit(npc, 1, HitType.Melee, damage, deps.playerHitModifier)
    }

    private fun heal(guardian: Guardian, cycle: Int) {
        guardian.nextHeal = cycle + HEAL_SPEED
        val boss = room.kephri ?: return
        if (boss.hitpoints <= 0) return
        val npc = guardian.npc
        projectile(KephriSpots.MELEE_HEAL, npc.coords, boss.coords.translate(2, 2), HEAL_HEIGHTS)
        val base = boss.baseHitpointsLvl
        val share = npc.hitpoints.toDouble() / npc.baseHitpointsLvl.coerceAtLeast(1)
        val percent = deps.random.of(HEAL_MIN_PERCENT, HEAL_MAX_PERCENT)
        room.healKephri((base * percent / 100.0 * share).toInt())
    }

    private fun arcane(guardian: Guardian, players: List<Player>, cycle: Int) {
        val npc = guardian.npc
        if (cycle == guardian.chargeAt) {
            deps.worldRepo.spotanimMap(spotanim(KephriSpots.MAGE_CHARGE), npc.coords)
        }
        if (cycle < guardian.chargeAt + CHARGE_TICKS) return
        for (player in players) {
            projectile(KephriSpots.MAGE_CHARGE, npc.coords, player.coords, BLAST_HEIGHTS)
            room.schedule(BLAST_HIT_DELAY) {
                if (!npc.isSlotAssigned || room.stage != ToaStage.STARTED) return@schedule
                val combatant = room.combatantOf(npc) ?: return@schedule
                val damage = combatant.rollScaled(BLAST_MAX_HIT / 2, BLAST_MAX_HIT)
                player.queueImpactHit(
                    npc,
                    1,
                    HitType.Magic,
                    damage,
                    deps.playerHitModifier,
                    penetration = MAGIC_PENETRATION,
                )
            }
        }
        relocate(guardian)
    }

    private fun relocate(guardian: Guardian) {
        val npc = guardian.npc
        guardian.relocation = (guardian.relocation + 1) % ARCANE_TILES.size
        guardian.hits = 0
        guardian.chargeAt = deps.mapClock.cycle + CHARGE_START
        npc.anim(KephriSeqs.FLYING_SPAWN)
        npc.teleport(deps.collision, room.coords(ARCANE_TILES[guardian.relocation]))
    }

    private fun projectile(spot: String, from: CoordGrid, to: CoordGrid, heights: Heights) {
        deps.worldRepo.projAnim(
            ProjAnim(
                spotanim = spot.asRSCM(RSCMType.SPOTANIM),
                startHeight = heights.start,
                endHeight = heights.end,
                startTime = heights.startTime,
                endTime = heights.flight,
                angle = heights.angle,
                progress = heights.progress,
                sourceIndex = 0,
                targetIndex = 0,
                startCoord = from,
                endCoord = to,
            )
        )
    }

    private class Heights(
        val start: Int,
        val end: Int,
        val startTime: Int,
        val flight: Int,
        val angle: Int,
        val progress: Int,
    )

    private companion object {
        const val FIRST_ATTACK = 5
        const val FIRST_HEAL = 7
        const val CHARGE_START = 5
        const val CHARGE_TICKS = 27
        const val SPIT_RANGE = 8
        const val SPIT_SPEED = 3
        const val SPIT_HIT_DELAY = 2
        const val SPIT_MAX_HIT = 10
        const val SPIT_POISON = 6
        const val POISON_SEVERITY = 11
        const val POISON_TEXT = "You have been poisoned!"
        const val PRAYER_IGNORED = 100
        const val SOLDIER_SPEED = 7
        const val SOLDIER_MAX_HIT = 17
        const val HEAL_SPEED = 5
        const val HEAL_MIN_PERCENT = 5
        const val HEAL_MAX_PERCENT = 7
        const val RETARGET_MIN = 5
        const val RETARGET_MAX = 10
        const val BLAST_MAX_HIT = 65
        const val BLAST_HIT_DELAY = 2
        const val MAGIC_PENETRATION = 33
        const val RELOCATE_DAMAGE = 39
        const val RELOCATE_BASE_HITS = 2

        val SPIT_HEIGHTS = Heights(96, 124, 36, 90, 16, 64)
        val HEAL_HEIGHTS = Heights(96, 96, 0, 90, 16, 0)
        val BLAST_HEIGHTS = Heights(96, 38, 0, 90, 16, 0)

        val SPAWN_TILES =
            mapOf(
                Kind.SPITTING to CoordGrid(3556, 5413, 0),
                Kind.SOLDIER to CoordGrid(3556, 5407, 0),
                Kind.ARCANE to CoordGrid(3556, 5401, 0),
            )

        val ARCANE_TILES =
            listOf(
                CoordGrid(3556, 5401, 0),
                CoordGrid(3544, 5413, 0),
                CoordGrid(3550, 5401, 0),
                CoordGrid(3556, 5413, 0),
            )
    }
}
