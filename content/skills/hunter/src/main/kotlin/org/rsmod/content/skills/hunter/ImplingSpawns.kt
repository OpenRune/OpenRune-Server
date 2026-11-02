package org.rsmod.content.skills.hunter

import dev.openrune.types.NpcServerType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.table.ImplingSpawnPointsRow
import org.rsmod.api.table.ImplingSpawnTablesRow
import org.rsmod.api.table.ImplingTypesRow
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

@Singleton
class ImplingSpawns @Inject constructor(private val npcRepo: NpcRepository, private val random: GameRandom) {
    internal class Spawn(val table: String, val coords: CoordGrid, val maze: Boolean, val due: Int)

    private val pending = mutableListOf<Spawn>()
    private val live = mutableMapOf<Npc, Spawn>()
    private var tick = 0
    private val later = mutableListOf<Pair<Int, () -> Unit>>()

    private val mazeTypes: Map<Int, NpcServerType> by lazy {
        ImplingTypesRow.all().associate { it.npc.id to it.npcPuroPuro }
    }

    internal fun tick() {
        if (tick == 0) startPuroPuro()
        if (tick % CYCLE_TICKS == 0) startCycle()
        val due = pending.filter { it.due <= tick }
        pending.removeAll(due)
        for (spawn in due) spawn(spawn)
        val actions = later.filter { it.first <= tick }
        later.removeAll(actions)
        for ((_, action) in actions) action()
        tick++
    }

    internal fun after(ticks: Int, action: () -> Unit) {
        later += (tick + ticks) to action
    }

    private fun startPuroPuro() {
        queue(HIGH_PURO_PURO, points(PURO_PURO_HIGH), maze = true)
        queue(MID, points(PURO_PURO_MID), maze = true)
        queue(LOW, points(PURO_PURO_LOW), maze = true)
    }

    private fun startCycle() {
        for ((npc, spawn) in live.entries.toList()) {
            if (spawn.maze) continue
            live.remove(npc)
            if (npc.isSlotAssigned) npcRepo.del(npc, Int.MAX_VALUE)
        }
        pending.removeAll { !it.maze }
        val spots = points(GIELINOR)
        val n = random.of(MIN_ROLLS, MAX_ROLLS)
        repeat(HIGH_ROLLS) { queue(HIGH, listOf(spots[random.of(spots.size)]), maze = false) }
        repeat(n) { queue(MID, listOf(spots[random.of(spots.size)]), maze = false) }
        repeat(n * 2) { queue(LOW, listOf(spots[random.of(spots.size)]), maze = false) }
        queue(LOW, points(GIELINOR_LOW), maze = false)
    }

    private fun queue(table: String, spots: List<CoordGrid>, maze: Boolean) {
        for (coords in spots) pending += Spawn(table, coords, maze, tick + INVISIBLE_TICKS)
    }

    private fun spawn(spawn: Spawn) {
        val type = pick(spawn.table)
        val npc = Npc(if (spawn.maze) mazeTypes[type.id] ?: type else type, spawn.coords)
        npcRepo.add(npc, Int.MAX_VALUE)
        live[npc] = spawn
    }

    private fun pick(table: String): NpcServerType {
        val implings = ImplingSpawnTablesRow.getRow(table).implings
        var roll = random.of(implings.sumOf { it.t1 })
        for ((type, weight) in implings) {
            if (roll < weight) return type
            roll -= weight
        }
        return implings.last().t0
    }

    internal fun caught(npc: Npc): Boolean {
        val spawn = live.remove(npc) ?: return false
        npcRepo.del(npc, Int.MAX_VALUE)
        pending += Spawn(spawn.table, spawn.coords, spawn.maze, tick + INVISIBLE_TICKS)
        return true
    }

    private fun points(row: String): List<CoordGrid> = ImplingSpawnPointsRow.getRow(row).spots

    internal companion object {
        const val CYCLE_TICKS = 3000
        const val INVISIBLE_TICKS = 200
        const val HIGH_ROLLS = 4
        const val MIN_ROLLS = 5
        const val MAX_ROLLS = 10

        const val LOW = "dbrow.impling_spawn_low"
        const val MID = "dbrow.impling_spawn_mid"
        const val HIGH = "dbrow.impling_spawn_high"
        const val HIGH_PURO_PURO = "dbrow.impling_spawn_high_puro_puro"
        const val GIELINOR = "dbrow.impling_points_gielinor"
        const val GIELINOR_LOW = "dbrow.impling_points_gielinor_low"
        const val PURO_PURO_HIGH = "dbrow.impling_points_puro_puro_high"
        const val PURO_PURO_MID = "dbrow.impling_points_puro_puro_mid"
        const val PURO_PURO_LOW = "dbrow.impling_points_puro_puro_low"
    }
}

class ImplingSpawnScript @Inject constructor(private val spawns: ImplingSpawns) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<GameLifecycle.LateCycle> { spawns.tick() }
    }
}
