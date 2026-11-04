package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import org.rsmod.api.player.hook.PlayerMovementCompleteHook
import org.rsmod.api.player.hook.PlayerStepValidateHook
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.farmingLvl
import org.rsmod.api.registry.player.PlayerRegistry
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onGameStartup
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.table.farming.FarmingGuildRow
import org.rsmod.content.generic.locs.doors.DoorTranslations
import org.rsmod.content.interfaces.bank.tryOpenBank
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareGrid
import org.rsmod.map.square.MapSquareKey
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.module.PluginModule
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.StepValidator
import org.rsmod.routefinder.collision.CollisionFlagMap

internal data class GuildBarrier(
    val tier: Int,
    val first: CoordGrid,
    val last: CoordGrid,
    val direction: String,
) {
    fun enters(from: CoordGrid, to: CoordGrid): Boolean {
        if (from.level != first.level || to.level != first.level) return false
        return when (direction) {
            "NORTH" -> from.z < first.z && to.z >= first.z && to.x in first.x..last.x
            "WEST" -> from.x > first.x && to.x <= first.x && to.z in first.z..last.z
            else -> error("Unsupported Guild gate direction: $direction")
        }
    }
}

internal class GuildWing(private val base: CoordGrid, private val tiles: BooleanArray) {
    fun contains(coords: CoordGrid): Boolean =
        MapSquareKey.from(coords) == MapSquareKey.from(base) &&
            tiles[(coords.z - base.z) * WIDTH + coords.x - base.x]

    companion object {
        private const val WIDTH = MapSquareGrid.LENGTH

        fun discover(gate: GuildBarrier, collision: CollisionFlagMap): GuildWing {
            val square = MapSquareKey.from(gate.first)
            val base = square.toCoords(gate.first.level)
            val tiles = BooleanArray(WIDTH * WIDTH)
            val queue = IntArray(tiles.size)
            val steps = StepValidator(collision)
            var read = 0
            var write = 0
            val start = (gate.first.z - base.z) * WIDTH + gate.first.x - base.x
            tiles[start] = true
            queue[write++] = start
            while (read < write) {
                val index = queue[read++]
                val from = base.translate(index % WIDTH, index / WIDTH)
                for (direction in 0..3) {
                    val dx = if (direction == 0) -1 else if (direction == 1) 1 else 0
                    val dz = if (direction == 2) -1 else if (direction == 3) 1 else 0
                    val to = from.translate(dx, dz)
                    if (gate.enters(to, from)) continue
                    if (!steps.canTravel(from.level, from.x, from.z, dx, dz)) continue
                    check(MapSquareKey.from(to) == square) { "Farming Guild wing is not enclosed" }
                    val next = (to.z - base.z) * WIDTH + to.x - base.x
                    if (tiles[next]) continue
                    tiles[next] = true
                    queue[write++] = next
                }
            }

            // Enclosed crop/tree footprints also contain the native upper redwood platforms.
            val outside = BooleanArray(tiles.size)
            read = 0
            write = 1
            queue[0] = 0
            outside[0] = true
            while (read < write) {
                val index = queue[read++]
                val x = index % WIDTH
                val z = index / WIDTH
                for (direction in 0..3) {
                    val nx = x + if (direction == 0) -1 else if (direction == 1) 1 else 0
                    val nz = z + if (direction == 2) -1 else if (direction == 3) 1 else 0
                    if (nx !in 0 until WIDTH || nz !in 0 until WIDTH) continue
                    val next = nz * WIDTH + nx
                    if (tiles[next] || outside[next]) continue
                    outside[next] = true
                    queue[write++] = next
                }
            }
            for (index in tiles.indices) tiles[index] = !outside[index]
            return GuildWing(base, tiles)
        }
    }
}

object FarmingGuild {
    val CENTRAL_ARRIVAL = CoordGrid(1248, 3726)

    private lateinit var advancedWing: GuildWing
    private val GATES by lazy {
        FarmingGuildRow.all().single().gates.map { GuildBarrier(it.t0, it.t1, it.t2, it.t3) }
    }

    fun canEnter(player: Player, tier: Int): Boolean =
        player.farmingLvl >= FarmingContracts.requiredLevel(tier)

    internal fun deniedTier(from: CoordGrid, to: CoordGrid, level: Int): Int? =
        GATES.firstOrNull { it.enters(from, to) && level < FarmingContracts.requiredLevel(it.tier) }?.tier

    internal fun entersMain(from: CoordGrid, to: CoordGrid): Boolean =
        GATES.any { it.tier == 1 && it.enters(from, to) }

    internal fun initializeWing(collision: CollisionFlagMap) {
        advancedWing = GuildWing.discover(GATES.single { it.tier == 3 }, collision)
    }

    // Moves collision via telejump and zone membership via registry so the later registry.del cleans the final tile.
    internal fun logout(player: Player, collision: CollisionFlagMap, players: PlayerRegistry) {
        val departure = player.coords
        if (!advancedWing.contains(departure)) return
        PathingEntityCommon.telejump(player, collision, CENTRAL_ARRIVAL)
        players.change(player, ZoneKey.from(departure), ZoneKey.from(CENTRAL_ARRIVAL))
    }
}

internal class FarmingGuildStepHook @Inject constructor(private val events: EventBus) :
    PlayerStepValidateHook, PlayerMovementCompleteHook {
    override fun canStep(player: Player, from: CoordGrid, to: CoordGrid): Boolean {
        val tier = FarmingGuild.deniedTier(from, to, player.farmingLvl) ?: return true
        player.mes("Hey! You need a Farming level of at least ${FarmingContracts.requiredLevel(tier)} to enter that area of the Farming Guild.")
        return false
    }

    override fun onMove(player: Player, from: CoordGrid, to: CoordGrid) {
        val entered = when (player.pendingStepCount) {
            1 -> FarmingGuild.entersMain(from, to)
            2 -> FarmingGuild.entersMain(from, player.lastProcessedCoord) ||
                FarmingGuild.entersMain(player.lastProcessedCoord, to)
            else -> false
        }
        if (entered) events.publish(FarmingGuildEntryEvent(player))
    }
}

class FarmingGuildGateScript
@Inject
constructor(
    private val locs: LocRepository,
    private val collision: CollisionFlagMap,
    private val players: PlayerRegistry,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onGameStartup {
            for (entry in FarmingGuildRow.all().single().entrance) {
                val closed = locs.findExact(entry.t2, entry.t0) ?: continue
                val opened = DoorTranslations.translateOpen(closed.coords, closed.shape, closed.angle)
                if (locs.del(closed, Int.MAX_VALUE)) {
                    locs.add(opened, entry.t1.internalName, Int.MAX_VALUE, closed.turnAngle(entry.t3), closed.shape)
                }
            }
            FarmingGuild.initializeWing(collision)
        }
        onPlayerLogout { FarmingGuild.logout(player, collision, players) }
        onOpLoc1("loc.fairy_chest_bank") { tryOpenBank() }
    }
}

class FarmingGuildModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerStepValidateHook>(FarmingGuildStepHook::class.java)
    }
}
