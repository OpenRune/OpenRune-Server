package org.rsmod.content.slayer.superior

import dev.openrune.ServerCacheManager
import dev.openrune.types.NpcMode
import dev.openrune.types.NpcServerType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.random.Random
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.area.checker.isInWilderness
import org.rsmod.api.config.refs.BaseParams
import org.rsmod.api.npc.owner.assignSpawnOwner
import org.rsmod.api.npc.owner.clearSpawnOwner
import org.rsmod.api.npc.owner.isSpawnOwnedBy
import org.rsmod.api.npc.owner.ownedBy
import org.rsmod.api.player.mapMultiway
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.content.slayer.core.SlayerTaskManager
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

@Singleton
class SlayerSuperiorManager
@Inject
constructor(
    private val collision: CollisionFlagMap,
    private val mapClock: MapClock,
    private val npcList: NpcList,
    private val players: PlayerList,
    private val areaChecker: AreaChecker,
) {
    fun isSuperior(npc: Npc): Boolean = npc.visType.id in superiorNpcIds

    fun trySpawnSuperior(player: Player, killed: Npc, npcRepo: NpcRepository) {
        if (!isSuperiorEnabled(player)) return
        if (player.vars["varp.slayer_target"] == 0) return
        if (!SlayerTaskManager.isTaskNpc(killed, player.vars["varp.slayer_target"])) return

        val killedType = killed.visType
        if (!killedType.hasParam(BaseParams.slayer_superior)) return
        if (killed.coords.isInWilderness() && !killedType.isAvailableInWilderness()) return
        if (!canSpawnAnother(player)) return

        val superiorType = killedType.paramOrNull(BaseParams.slayer_superior)?.let { ServerCacheManager.getNpc(it.id) } ?: return
        if (!rollSpawn(player)) return

        val coords = SlayerSuperiorSpawnTiles.resolve(player, killed, superiorType, collision) ?: return
        spawnSuperior(player, coords, superiorType, npcRepo)
    }

    fun onSuperiorDeath(npc: Npc) {
        release(npc)
    }

    fun onPlayerLogout(player: Player, npcRepo: NpcRepository) {
        for (npc in ownedSuperiors(player).toList()) {
            despawn(npc, npcRepo)
            release(npc)
        }
        setSuperiorActive(player, false)
    }

    fun tickOwnedSuperiors(player: Player, npcRepo: NpcRepository) {
        if (!hasSuperiorActive(player)) return

        var anyRemaining = false
        for (npc in ownedSuperiors(player)) {
            anyRemaining = true
            tickPresence(player, npc, npcRepo)
        }
        if (!anyRemaining) {
            setSuperiorActive(player, false)
        }
    }

    private fun ownedSuperiors(player: Player): Sequence<Npc> = npcList.ownedBy(player).filter(::isSuperior)

    private fun canSpawnAnother(player: Player): Boolean =
        player.mapMultiway(areaChecker) || !hasSuperiorActive(player)

    private fun hasSuperiorActive(player: Player): Boolean = player.vars[VARBIT_SUPERIOR_ACTIVE] != 0

    private fun setSuperiorActive(player: Player, active: Boolean) {
        VarPlayerIntMapSetter.set(player, VARBIT_SUPERIOR_ACTIVE, if (active) 1 else 0)
    }

    private fun syncSuperiorActive(owner: PlayerUid) {
        val player = players.firstOrNull { it.uid == owner } ?: return
        setSuperiorActive(player, ownedSuperiors(player).any())
    }

    private fun isSuperiorEnabled(player: Player): Boolean {
        if (!SlayerTaskManager.hasUnlockedReward(player, BIGGER_AND_BADDER_BIT)) return false
        return player.vars["varbit.slayer_toggleoff_superiormobs"] == 0
    }

    private fun rollSpawn(player: Player): Boolean = Random.nextInt(spawnDenominator(player)) == 0

    private fun spawnDenominator(player: Player): Int {
        val onWildernessTask = SlayerTaskManager.getCurrentAssignedMaster(player)?.let(SlayerTaskManager::isWildernessMaster) == true

        if (!onWildernessTask) {
            return if (hasEliteCombatAchievements(player)) SPAWN_CHANCE_ELITE_CA else SPAWN_CHANCE_BASE
        }
        return if (hasEliteCombatAchievements(player)) SPAWN_CHANCE_WILDERNESS_ELITE_CA else SPAWN_CHANCE_WILDERNESS
    }

    private fun NpcServerType.isAvailableInWilderness(): Boolean =
        paramOrNull(BaseParams.availableInWilderness) == true

    private fun hasEliteCombatAchievements(player: Player): Boolean = false

    private fun spawnSuperior(
        player: Player,
        coords: CoordGrid,
        superiorType: NpcServerType,
        npcRepo: NpcRepository,
    ) {
        val npc = Npc(superiorType, coords)
        npc.mode = NpcMode.None
        npc.respawns = false
        npc.wanderRange = SUPERIOR_WANDER_RANGE
        npcRepo.add(npc, DESPAWN_IDLE_CYCLES)

        npc.assignSpawnOwner(player, mapClock.cycle)
        setSuperiorActive(player, true)
        player.mes(SPAWN_MESSAGE)
    }

    private fun release(npc: Npc) {
        val owner = npc.spawnOwner
        npc.clearSpawnOwner()
        if (owner != PlayerUid.NULL) {
            syncSuperiorActive(owner)
        }
    }

    private fun tickPresence(player: Player, npc: Npc, npcRepo: NpcRepository) {
        if (!npc.isSpawnOwnedBy(player)) return

        if (player.coords.chebyshevDistance(npc.coords) <= VICINITY_RADIUS) {
            npc.spawnOwnerLastNearCycle = mapClock.cycle
            return
        }

        val awayCycles = mapClock.cycle - npc.spawnOwnerLastNearCycle
        if (awayCycles >= DESPAWN_AWAY_CYCLES || mapClock.cycle >= npc.lifecycleDelCycle) {
            despawn(npc, npcRepo)
            release(npc)
        }
    }

    private fun despawn(npc: Npc, npcRepo: NpcRepository) {
        npcRepo.del(npc, duration = 0)
    }

    private val superiorNpcIds: Set<Int> by lazy {
        buildSet {
            for ((_, type) in ServerCacheManager.getNpcs()) {
                type.paramOrNull(BaseParams.slayer_superior)?.id?.let { add(it) }
            }
        }
    }

    companion object {
        private const val VARBIT_SUPERIOR_ACTIVE = "varbit.slayer_superior_active"
        const val SPAWN_MESSAGE = "A superior foe has appeared..."
        private const val BIGGER_AND_BADDER_BIT = 35
        private const val SUPERIOR_WANDER_RANGE = 2
        private const val SPAWN_CHANCE_BASE = 200
        private const val SPAWN_CHANCE_WILDERNESS = 180
        private const val SPAWN_CHANCE_ELITE_CA = 150
        private const val SPAWN_CHANCE_WILDERNESS_ELITE_CA = 135
        private const val VICINITY_RADIUS = 16
        private const val DESPAWN_IDLE_CYCLES = 200
        private const val DESPAWN_AWAY_CYCLES = 50
    }
}
