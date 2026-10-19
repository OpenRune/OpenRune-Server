package org.rsmod.content.other.treasuretrails

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.npc.owner.assignSpawnOwner
import org.rsmod.api.npc.owner.isSpawnOwnedBy
import org.rsmod.api.npc.owner.ownedBy
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.table.TrailTierRow
import org.rsmod.api.table.cluehelper.CluehelperCombatEncounterRow
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcUid
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.LineValidator
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

@Singleton
class ClueEncounters
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val npcList: NpcList,
    private val worldRepo: WorldRepository,
    private val mapClock: MapClock,
    private val collision: CollisionFlagMap,
    private val aiPlayerInteractions: AiPlayerInteractions,
    private val random: GameRandom,
) {
    private val lineValidator = LineValidator(collision)

    private val encounterNpcIds: Set<Int> by lazy {
        CluehelperCombatEncounterRow.all().flatMap { row -> row.npcs.map { it.id } }.toSet()
    }

    fun isEncounterNpc(npc: Npc): Boolean = npc.type.id in encounterNpcIds

    fun alive(player: Player): List<Npc> =
        npcList.ownedBy(player).filter { isEncounterNpc(it) && it.hitpoints > 0 }.toList()

    fun belongsTo(step: ClueStep, npc: Npc): Boolean =
        step.encounters.any { encounter -> encounter.npcs.any { it.id == npc.type.id } }

    fun spawn(player: Player, step: ClueStep): Boolean {
        if (alive(player).isNotEmpty()) return false
        val encounter = random.pick(step.encounters)
        for (type in encounter.npcs) {
            val npc = Npc(type, spawnTile(player.coords))
            npc.respawns = false
            npcRepo.add(npc, ENCOUNTER_LIFETIME)
            worldRepo.spotanimMap(
                SpotanimType("spotanim.smokepuff_large".asRSCM(RSCMType.SPOTANIM)),
                npc.coords,
            )
            npc.assignSpawnOwner(player, mapClock.cycle)
            if (npc.type.id in RANGED_ENCOUNTERS) {
                npc.apPlayer2(player, aiPlayerInteractions)
            } else {
                npc.opPlayer2(player, aiPlayerInteractions)
            }
        }
        return true
    }

    fun summonUri(player: Player, tier: TrailTierRow) {
        if (summonedUri(player) != null) return
        VarPlayerIntMapSetter.set(player, TrailFlow.EMOTE_FIRST, 0)
        val coords = player.coords.translateZ(-1)
        val uri = Npc(tier.uriNpc, coords)
        npcRepo.add(uri, URI_DURATION)
        uri.facePlayer(player)
        worldRepo.spotanimMap(SpotanimType("spotanim.smokepuff".asRSCM(RSCMType.SPOTANIM)), coords)
        ClueUri.remember(player, uri)
    }

    private fun summonedUri(player: Player): Npc? =
        ClueUri.summoned(player)?.let { NpcUid(it).resolve(npcList) }

    fun isOwnedEncounter(player: Player, npc: Npc): Boolean =
        isEncounterNpc(npc) && npc.isSpawnOwnedBy(player)

    private fun spawnTile(near: CoordGrid): CoordGrid {
        repeat(SPAWN_ATTEMPTS) {
            val candidate = random.of(near, SPAWN_RADIUS)
            if (isValidSpawn(near, candidate)) return candidate
        }
        for (dx in -SPAWN_RADIUS..SPAWN_RADIUS) {
            for (dz in -SPAWN_RADIUS..SPAWN_RADIUS) {
                val candidate = near.translate(dx, dz)
                if (isValidSpawn(near, candidate)) return candidate
            }
        }
        return near
    }

    private fun isValidSpawn(player: CoordGrid, candidate: CoordGrid): Boolean =
        candidate != player && isWalkable(candidate) && canReach(player, candidate)

    private fun isWalkable(coords: CoordGrid): Boolean =
        collision[coords.x, coords.z, coords.level] and BLOCKED_FLAGS == 0

    private fun canReach(from: CoordGrid, to: CoordGrid): Boolean =
        lineValidator.hasLineOfWalk(from.level, from.x, from.z, to.x, to.z) &&
            lineValidator.hasLineOfSight(from.level, from.x, from.z, to.x, to.z)

    internal companion object {
        const val URI_DURATION = 100
        const val ENCOUNTER_LIFETIME = 500
        const val SPAWN_RADIUS = 2
        const val SPAWN_ATTEMPTS = 20
        const val BLOCKED_FLAGS =
            CollisionFlag.LOC or CollisionFlag.BLOCK_WALK or CollisionFlag.GROUND_DECOR

        val RANGED_ENCOUNTERS =
            listOf(
                    "npc.trail_hard",
                    "npc.trail_hard2",
                    "npc.elite_npc_1",
                    "npc.trail_zaros_wizard",
                    "npc.trail_zaros_range",
                    "npc.trail_cabbage_mage",
                )
                .map { it.asRSCM(RSCMType.NPC) }
                .toSet()
    }
}
