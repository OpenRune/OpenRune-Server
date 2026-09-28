package org.rsmod.content.bosses.gemstonecrab

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.random.Random
import org.rsmod.api.bossbar.plugin.BossHpBarScript
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.game.MapClock
import org.rsmod.game.damage.DamageContributor
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.map.CoordGrid

@Singleton
public class GemstoneCrabManager
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val mapClock: MapClock,
    private val playerList: PlayerList,
    private val hpBar: BossHpBarScript,
) {
    public var liveNpc: Npc? = null
        private set

    public var remainsNpc: Npc? = null
        private set

    public var eligibleMiners: Set<Long> = emptySet()
        private set

    private var currentLocationIndex = -1
    private var nextLocationIndex = -1
    private var burrowCycle = -1
    private var burrowStartCycle = -1
    private var activeLifetime = 0
    private var remainsExpireCycle = -1
    private var remainsDisintegrating = false
    private var remainsSpotCycle = -1
    private var topDealers: List<DamageContributor.ByPlayer> = emptyList()

    private val barOpenPlayers: MutableSet<Player> = Collections.newSetFromMap(IdentityHashMap())
    private val claimedMiners: MutableSet<Long> = mutableSetOf()
    private val playersInSpotArea: Array<MutableSet<Player>> =
        Array(SPOTS.size) { Collections.newSetFromMap(IdentityHashMap()) }

    public val isCrabActive: Boolean
        get() = liveNpc != null && burrowStartCycle == -1

    public fun isEligibleMiner(uuid: Long): Boolean = uuid in eligibleMiners

    public fun hasClaimedMining(uuid: Long): Boolean = uuid in claimedMiners

    public fun claimMining(uuid: Long): Boolean = claimedMiners.add(uuid)

    public fun start() {
        if (liveNpc != null || remainsNpc != null) {
            return
        }
        spawnNext()
    }

    public fun tick() {
        val live = liveNpc
        if (live != null) {
            if (burrowStartCycle != -1) {
                tickBurrow(live)
                return
            }
            for (player in barOpenPlayers) {
                updateBar(player, live)
            }
            if (mapClock.cycle >= burrowCycle) {
                startBurrow(live)
            }
            return
        }

        val remains = remainsNpc ?: return
        if (mapClock.cycle >= remainsExpireCycle) {
            expireRemains(remains)
            return
        }
        if (remainsDisintegrating) {
            return
        }
        if (mapClock.cycle >= remainsExpireCycle - DISINTEGRATE_CYCLES) {
            disintegrateRemains(remains)
            return
        }
        if (mapClock.cycle >= remainsSpotCycle) {
            remains.spotanim("spotanim.crab_boss_remains_idle_spot")
            remainsSpotCycle = mapClock.cycle + REMAINS_SPOT_INTERVAL_CYCLES
        }
    }

    public fun caveExitFor(cave: CoordGrid): CoordGrid? {
        val index = if (isCrabActive) currentLocationIndex else nextLocationIndex
        val destination = SPOTS.getOrNull(index) ?: return null
        return destination.caveExit.takeIf { destination.cave != cave }
    }

    public fun forceBurrow() {
        val live = liveNpc ?: return
        if (burrowStartCycle == -1) {
            startBurrow(live)
        }
    }

    public fun openBarFor(player: Player, npc: Npc) {
        if (barOpenPlayers.add(player)) {
            hpBar.onOpen(player, npc)
            updateBar(player, npc)
        }
    }

    public fun releaseBar(player: Player) {
        barOpenPlayers.remove(player)
    }

    public fun enterSpotArea(player: Player, spotIndex: Int) {
        playersInSpotArea[spotIndex].add(player)
        val live = liveNpc ?: return
        if (currentLocationIndex == spotIndex) {
            openBarFor(player, live)
        }
    }

    public fun exitSpotArea(player: Player, spotIndex: Int) {
        playersInSpotArea[spotIndex].remove(player)
        val live = liveNpc ?: return
        if (barOpenPlayers.remove(player)) {
            hpBar.onClose(player, live)
        }
    }

    private fun spawnNext() {
        currentLocationIndex = if (nextLocationIndex != -1) nextLocationIndex else pickNextLocation()
        nextLocationIndex = -1
        val npc = Npc(resolveNpc("npc.gemstone_crab"), SPOTS[currentLocationIndex].crab)
        val lifetime = ACTIVE_DURATION_CYCLES.random()
        npcRepo.add(npc, Int.MAX_VALUE)
        npc.anim("seq.crab_boss_spawn")
        npc.spotanim("spotanim.vfx_crab_boss_spawn")
        liveNpc = npc
        activeLifetime = lifetime
        burrowCycle = mapClock.cycle + lifetime

        for (player in playersInSpotArea[currentLocationIndex]) {
            openBarFor(player, npc)
        }
    }

    private fun pickNextLocation(): Int {
        var next: Int
        do {
            next = Random.nextInt(SPOTS.size)
        } while (next == currentLocationIndex)
        return next
    }

    private fun startBurrow(live: Npc) {
        burrowStartCycle = mapClock.cycle
        nextLocationIndex = pickNextLocation()
        topDealers =
            live.damageContributions
                .sortedByDamageDescending()
                .filterIsInstance<DamageContributor.ByPlayer>()
                .take(TOP_DAMAGE_DEALERS)

        burrowCycle = mapClock.cycle
        for (player in barOpenPlayers) {
            updateBar(player, live)
        }
        live.noneMode()
        live.hideAllOps()
        live.anim("seq.crab_boss_death")
        live.spotanim("spotanim.vfx_crab_boss_death")
    }

    private fun tickBurrow(live: Npc) {
        when (mapClock.cycle - burrowStartCycle) {
            BURROW_ANNOUNCE_DELAY_CYCLES -> announceBurrow()
            BURROW_REMOVE_DELAY_CYCLES -> finishBurrow(live)
        }
    }

    private fun announceBurrow() {
        broadcast("The gemstone crab burrows away, leaving a piece of its shell behind.")

        val names = topDealers.take(3).mapNotNull { it.resolve(playerList)?.displayName }
        if (names.isNotEmpty()) {
            broadcast(topDealerMessage(names))
        }

        for (dealer in topDealers) {
            val player = dealer.resolve(playerList) ?: continue
            player.mes(
                "<col=005f00>You gained enough understanding of the crab to mine from its remains.",
            )
        }
    }

    private fun finishBurrow(live: Npc) {
        eligibleMiners = topDealers.map { it.uuid }.toSet()
        claimedMiners.clear()
        topDealers = emptyList()
        burrowStartCycle = -1

        val coords = live.coords
        npcRepo.del(live, Int.MAX_VALUE)
        liveNpc = null

        for (player in barOpenPlayers) {
            hpBar.onClose(player, live)
        }
        barOpenPlayers.clear()

        val remains = Npc(resolveNpc("npc.gemstone_crab_remains"), coords)
        npcRepo.add(remains, Int.MAX_VALUE)
        remains.spotanim("spotanim.crab_boss_remains_idle_spot")
        remainsNpc = remains
        remainsSpotCycle = mapClock.cycle + REMAINS_FIRST_SPOT_DELAY_CYCLES
        remainsDisintegrating = false
        remainsExpireCycle = mapClock.cycle + REMAINS_CYCLES
    }

    private fun disintegrateRemains(remains: Npc) {
        remainsDisintegrating = true
        remains.hideAllOps()
        remains.anim("seq.crab_boss_remains_disintegrate")
        remains.spotanim("spotanim.vfx_crab_boss_disintegrate")
    }

    private fun expireRemains(remains: Npc) {
        npcRepo.del(remains, Int.MAX_VALUE)
        remainsNpc = null
        eligibleMiners = emptySet()
        spawnNext()
    }

    private fun updateBar(player: Player, npc: Npc) {
        val remaining = (burrowCycle - mapClock.cycle).coerceIn(0, activeLifetime)
        hpBar.onUpdate(player, npc, remaining, activeLifetime)
    }

    private fun broadcast(text: String) {
        for (player in playerList) {
            player.mes(text, ChatType.Broadcast)
        }
    }

    private fun topDealerMessage(names: List<String>): String =
        when (names.size) {
            1 -> "The top crab crusher was ${names[0]}!"
            2 -> "The top two crab crushers were ${joinNames(names)}!"
            else -> "The top three crab crushers were ${joinNames(names)}!"
        }

    private fun joinNames(names: List<String>): String =
        when (names.size) {
            1 -> names[0]
            2 -> "${names[0]} & ${names[1]}"
            else -> "${names.dropLast(1).joinToString(", ")}, & ${names.last()}"
        }

    private fun resolveNpc(name: String) =
        ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC)) ?: error("Unknown npc: $name")

    private companion object {
        private val SPOTS =
            listOf(
                Spot(
                    crab = CoordGrid(1271, 3171, 0),
                    cave = CoordGrid(1278, 3167, 0),
                    caveExit = CoordGrid(1277, 3169, 0),
                ),
                Spot(
                    crab = CoordGrid(1351, 3110, 0),
                    cave = CoordGrid(1350, 3123, 0),
                    caveExit = CoordGrid(1351, 3122, 0),
                ),
                Spot(
                    crab = CoordGrid(1237, 3042, 0),
                    cave = CoordGrid(1245, 3035, 0),
                    caveExit = CoordGrid(1247, 3038, 0),
                ),
            )
        private val ACTIVE_DURATION_CYCLES = 918..967
        private const val REMAINS_CYCLES = 150
        private const val DISINTEGRATE_CYCLES = 4
        private const val TOP_DAMAGE_DEALERS = 16
        private const val BURROW_ANNOUNCE_DELAY_CYCLES = 2
        private const val BURROW_REMOVE_DELAY_CYCLES = 4
        private const val REMAINS_FIRST_SPOT_DELAY_CYCLES = 13
        private const val REMAINS_SPOT_INTERVAL_CYCLES = 14
    }
}

private data class Spot(val crab: CoordGrid, val cave: CoordGrid, val caveExit: CoordGrid)
