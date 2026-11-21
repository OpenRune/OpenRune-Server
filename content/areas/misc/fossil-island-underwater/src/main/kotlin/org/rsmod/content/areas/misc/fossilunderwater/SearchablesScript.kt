package org.rsmod.content.areas.misc.fossilunderwater

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ObjectServerType
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.misc.client.HintArrow
import org.rsmod.api.enums.NamedEnums.fossil_underwater_open_locs
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.MiscOutput
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.registry.player.PlayerRegistry
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The chests and clams of the sea floor. One of them is open at a time, world-wide: every
 * [WINDOW_TICKS] ticks another opens for a hundred ticks and an arrow points every diver to it.
 * The first one a diver finds on a dive only earns Mairin's thanks; after that the first search of
 * each newly opened one always pays, and searches after it succeed on a Thieving roll. A success
 * pays a mermaid's and a glistening tear, maybe more, and Agility and Thieving xp; a failure may
 * cost breath. The search repeats until the player stops or the chest closes.
 */
class SearchablesScript
@Inject
constructor(
    private val players: PlayerRegistry,
    private val locRepo: LocRepository,
    private val worldRepo: WorldRepository,
    private val mapClock: MapClock,
    private val random: GameRandom,
    private val invisibleLevels: InvisibleLevels,
) : PluginScript() {
    private lateinit var rewards: SearchRewards
    private lateinit var openLocs: Map<Int, ObjectServerType>
    private var pool: List<LocInfo> = emptyList()
    private var chosen: LocInfo? = null
    private var window = 0
    private var windowStart = 0
    private var nextWindow = 0

    override fun ScriptContext.startup() {
        rewards = SearchRewards.load()
        openLocs =
            fossil_underwater_open_locs
                .mapNotNull { (closed, open) -> open?.let { closed.id to it } }
                .toMap()
        onEvent<GameLifecycle.LateCycle> { tick() }
        onOpLoc1(CHEST_OPEN) { search(it.loc, it.type.name) }
        onOpLoc1(CLAM_OPEN) { search(it.loc, it.type.name) }
        onPlayerQueueWithArgs<Search>(SEARCH) { attempt(it.args) }
        onPlayerQueueWithArgs<Search>(OUTCOME) { outcome(it.args) }
        onPlayerQueueWithArgs<Search>(RECOVER) { recover(it.args) }
    }

    private fun tick() {
        val cycle = mapClock.cycle
        if (cycle >= nextWindow) {
            startWindow(cycle)
        }
        val loc = chosen ?: return
        when (cycle - windowStart) {
            1 -> worldRepo.locAnim(loc, OPEN_ANIM)
            OPENS_AT -> open(loc)
            SHUTS_AT -> shut(loc)
        }
        if (cycle % ARROW_CYCLE == 0) {
            players.findAll(Underwater.ZONES).forEach { pointTo(it, loc.coords) }
        }
    }

    private fun shut(loc: LocInfo) {
        val open = locRepo.findAll(loc.coords).firstOrNull { it.layer == loc.layer } ?: return
        worldRepo.locAnim(open, SHUT_ANIM)
    }

    private fun startWindow(cycle: Int) {
        windowStart = cycle
        nextWindow = cycle + WINDOW_TICKS
        if (pool.isEmpty()) {
            pool = scanPool()
        }
        val others = pool.filter { it != chosen }.ifEmpty { pool }
        val next = random.pickOrNull(others) ?: return
        chosen = next
        window = if (window >= MAX_WINDOW) 1 else window + 1
        worldRepo.locAnim(next, OPEN_ANIM)
        val decoys = pool.filter { it != next }
        // UNVERIFIED: how many closed ones live animates as decoys; two are seen in view at once.
        repeat(DECOYS) { random.pickOrNull(decoys)?.let { worldRepo.locAnim(it, OPEN_ANIM) } }
    }

    private fun open(loc: LocInfo) {
        val open = openLocs[loc.id] ?: return
        locRepo.add(loc.coords, open.internalName, OPEN_TICKS, loc.angle, loc.shape)
    }

    private fun scanPool(): List<LocInfo> {
        val locs = mutableListOf<LocInfo>()
        for (x in POOL_SOUTH_WEST.x..POOL_NORTH_EAST.x step ZoneGrid.LENGTH) {
            for (z in POOL_SOUTH_WEST.z..POOL_NORTH_EAST.z step ZoneGrid.LENGTH) {
                val zone = ZoneKey.from(CoordGrid(x, z, POOL_SOUTH_WEST.level))
                locs += locRepo.findAll(zone).filter { it.id in openLocs }
            }
        }
        return locs
    }

    private fun pointTo(player: Player, coords: CoordGrid) {
        MiscOutput.hintArrowReset(player)
        val arrow = HintArrow.TileHintArrow(coords.x, coords.z, ARROW_HEIGHT, WEST)
        player.client.write(HintArrow(arrow))
        VarPlayerIntMapSetter.set(player, ARROW_REFRESH, 0)
        VarPlayerIntMapSetter.set(player, ARROW_REFRESH, ARROW_REFRESH_ON)
    }

    private fun openWindowAt(coords: CoordGrid): Int? {
        val open = chosen ?: return null
        val elapsed = mapClock.cycle - windowStart
        val isOpen = open.coords == coords && elapsed in OPENS_AT until OPENS_AT + OPEN_TICKS
        return window.takeIf { isOpen }
    }

    private suspend fun ProtectedAccess.search(loc: BoundLocInfo, name: String) {
        arriveDelay()
        val window = openWindowAt(loc.coords) ?: return
        if (!player.foundOpen) {
            player.foundOpen = true
            player.foundWindow = true
            player.searchedWindow = window
            val kind = name.lowercase()
            mes(
                "You have successfully found an open $kind; the mermaids will reward you from " +
                    "now on."
            )
            startDialogue {
                chatNpcSpecific(
                    MAIRIN_NAME,
                    MAIRIN,
                    happy,
                    "Thanks, you should be able to get some tears from the next chest!",
                )
            }
            return
        }
        if (player.foundWindow && player.searchedWindow == window) {
            mes("You'll have to wait for the next chest or clam to open.")
            return
        }
        if (STEPS.any { it in player.queueList }) {
            return
        }
        attempt(Search(loc.coords, coords))
    }

    private fun ProtectedAccess.attempt(search: Search) {
        if (!stillSearching(search)) {
            return
        }
        faceSquare(search.loc)
        anim(STEAL)
        next(OUTCOME, search)
    }

    private fun ProtectedAccess.outcome(search: Search) {
        if (!stillSearching(search)) {
            return
        }
        val window = openWindowAt(search.loc) ?: return
        val first = player.searchedWindow != window
        if (!first && !statRandom(THIEVING, SEARCH_LOW, SEARCH_HIGH, invisibleLevels)) {
            anim(STEAL_FAIL)
            next(RECOVER, search)
            return
        }
        if (!pay()) {
            // UNVERIFIED: live's words when the tears won't fit.
            mes("You don't have enough inventory space.")
            return
        }
        player.foundWindow = false
        player.searchedWindow = window
        anim(STEAL_SUCCESS)
        soundSynth(SUCCESS_SYNTH)
        statAdvance(AGILITY, SEARCH_XP)
        statAdvance(THIEVING, SEARCH_XP)
        next(SEARCH, search)
    }

    private fun ProtectedAccess.recover(search: Search) {
        if (!stillSearching(search)) {
            return
        }
        // UNVERIFIED: the share of nibbles that cost breath; live shows about two in five.
        if (random.of(BREATH_ROLL) < BREATH_LOST) {
            player.oxygen = Breath.lose(player.oxygen, Breath.NIBBLE)
            mes("You jerk back as something nibbles your hand and lose some breath.", ChatType.Spam)
        } else {
            mes("You jerk back as something nibbles your hand.", ChatType.Spam)
        }
        anim(STEAL_FAIL_END)
        next(SEARCH, search)
    }

    private fun ProtectedAccess.stillSearching(search: Search): Boolean =
        coords == search.from && openWindowAt(search.loc) != null

    private fun ProtectedAccess.pay(): Boolean {
        val found = rewards.roll(random)
        val transaction =
            player.invTransaction(inv) {
                val into = select(inv)
                add(into, MERMAID_TEAR.asRSCM(RSCMType.OBJ), count = 1)
                add(into, GLISTENING_TEAR.asRSCM(RSCMType.OBJ), count = 1)
                if (found != null) {
                    add(into, found.obj.asRSCM(RSCMType.OBJ), count = found.count)
                }
            }
        return transaction.success
    }

    /**
     * Queues the next step of the search, replacing any step still waiting. The steps are normal
     * queues, which the clicks a player keeps making on the chest don't clear.
     */
    private fun ProtectedAccess.next(step: String, search: Search) {
        STEPS.forEach { clearQueue(it) }
        queue(step, 1, search)
    }

    private class Search(val loc: CoordGrid, val from: CoordGrid)

    private companion object {
        const val CHEST_OPEN = "loc.fossil_chest_open"
        const val CLAM_OPEN = "loc.fossil_clam_open"
        const val MAIRIN = "npc.fossil_mermaid_deepwater"
        const val MAIRIN_NAME = "Mairin"
        const val MERMAID_TEAR = "obj.fossil_mermaid_tear"
        const val GLISTENING_TEAR = "obj.fossil_glistening_tear"
        const val SEARCH = "queue.fossil_underwater_search"
        const val OUTCOME = "queue.fossil_underwater_search_outcome"
        const val RECOVER = "queue.fossil_underwater_search_recover"
        const val OPEN_ANIM = "seq.fossil_loc_clam_open"
        const val SHUT_ANIM = "seq.fossil_loc_clam_shut"
        const val STEAL = "seq.human_swim_steal"
        const val STEAL_SUCCESS = "seq.human_swim_steal_success"
        const val STEAL_FAIL = "seq.human_swim_steal_fail"
        const val STEAL_FAIL_END = "seq.human_swim_steal_fail_end"
        const val SUCCESS_SYNTH = "synth.jar_open"
        const val ARROW_REFRESH = "varbit.current_hint_arrow"
        const val AGILITY = "stat.agility"
        const val THIEVING = "stat.thieving"

        const val WINDOW_TICKS = 103
        const val OPENS_AT = 2
        const val OPEN_TICKS = 100
        const val SHUTS_AT = OPENS_AT + OPEN_TICKS - 1
        const val MAX_WINDOW = 1023
        const val DECOYS = 2
        const val ARROW_CYCLE = 3
        const val ARROW_HEIGHT = 128
        const val ARROW_REFRESH_ON = 6
        const val SEARCH_LOW = 30
        const val SEARCH_HIGH = 40
        const val SEARCH_XP = 4.5
        const val BREATH_ROLL = 5
        const val BREATH_LOST = 2

        val STEPS = listOf(SEARCH, OUTCOME, RECOVER)
        val WEST = HintArrow.TileHintArrow.HintArrowTilePosition.WEST
        val POOL_SOUTH_WEST = CoordGrid(3712, 10240, 1)
        val POOL_NORTH_EAST = CoordGrid(3839, 10303, 1)
    }
}
