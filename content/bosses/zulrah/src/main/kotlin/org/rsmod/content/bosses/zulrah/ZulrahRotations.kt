package org.rsmod.content.bosses.zulrah

internal data class ZulrahPhase(
    val duration: Int,
    val events: List<ZulrahRoutineEvent>,
    val evidence: String,
) {
    val emerge get() = events.first()

    init {
        require(events.first().kind == "emerge" && events.first().tick == 0)
        require(events.last().kind == "dive" && events.last().tick == duration - 3)
        require(events.zipWithNext().all { (a, b) -> a.tick <= b.tick })
        require(events.all { it.tick in 0 until duration })
    }
}

/** Wiki action sequences with recorded fragments; reconstruction rules are in ROTATION_EVIDENCE.md. */
internal object ZulrahRotations {
    const val MAGIC = "spotanim.snakeboss_fireball"
    const val RANGED = "spotanim.snakeboss_orb"
    private const val GREEN = "npc.snakeboss_boss_ranged"
    private const val TANZANITE = "npc.snakeboss_boss_magic"

    private enum class Action { Ranged, Mixed, Gas, Egg, Tail, JadMagic, JadRanged }

    private val captured: List<ZulrahPhase> by lazy {
        val routine = ZulrahRoutine.recorded
        val starts = routine.events.filter { it.kind == "emerge" }.map { it.tick }
        starts.mapIndexed { index, start ->
            val end = starts.getOrNull(index + 1) ?: routine.endTick
            ZulrahPhase(end - start,
                routine.events.filter { it.tick in start until end }.map { it.copy(tick = it.tick - start) },
                "RSProx 3276 phase ${index + 1}, ticks ${routine.startTick + start}..${routine.startTick + end}")
        }
    }

    val opening get() = captured[0]
    val recurringOpening get() = captured[12]

    // Sample weights, not a verified OSRS probability. Jad attacks are deliberately excluded.
    val mixedWeights: Map<String, Int> by lazy {
        captured.filter { it.emerge.symbol == TANZANITE }.flatMap { it.events }
            .filter { it.kind == "attack" }.groupingBy { it.symbol }.eachCount()
    }

    private fun recorded(number: Int): ZulrahPhase {
        val phase = captured[number - 1]
        return phase.copy(events = phase.events.map {
            it.copy(mixedAttack = it.kind == "attack" && phase.emerge.symbol == TANZANITE)
        })
    }

    val rotations: List<List<ZulrahPhase>> by lazy {
        val melee = recorded(14)
        val middleMagic = recorded(15)
        val westMagic = reconstruct(8, List(5) { Action.Mixed })
        val southAlternating = reconstruct(17, List(5) { Action.Mixed } +
            listOf(Action.Egg, Action.Gas, Action.Egg, Action.Gas, Action.Egg))
        val westJad = reconstruct(16, List(10) { Action.JadRanged } + List(4) { Action.Gas })
        listOf(
            listOf(opening, melee, middleMagic,
                reconstruct(17, List(5) { Action.Ranged } + List(2) { Action.Egg } +
                    List(2) { Action.Gas } + List(2) { Action.Egg }, GREEN),
                melee, westMagic,
                reconstruct(7, List(3) { Action.Gas } + List(4) { Action.Egg }),
                southAlternating, westJad, melee),
            listOf(opening, melee, middleMagic, recorded(16), recorded(17), recorded(18),
                recorded(19), southAlternating, westJad, melee),
            listOf(opening,
                reconstruct(19, List(5) { Action.Ranged } + List(3) { Action.Egg }),
                reconstruct(5, List(6) { if (it % 2 == 0) Action.Gas else Action.Egg } +
                    List(2) { Action.Tail }),
                westMagic,
                reconstruct(3, List(5) { Action.Ranged }),
                reconstruct(2, List(5) { Action.Mixed }),
                reconstruct(13, List(3) { Action.Gas } + List(3) { Action.Egg }),
                reconstruct(16, List(5) { Action.Ranged }),
                reconstruct(10, List(5) { Action.Mixed } + List(2) { Action.Gas } +
                    List(3) { Action.Egg }),
                reconstruct(11, List(10) { Action.JadMagic }), recorded(12)),
            (1..12).map { number ->
                when (number) {
                    3 -> reconstruct(3, List(4) { Action.Ranged } + List(2) { Action.Gas })
                    9 -> reconstruct(9, List(4) { Action.Ranged })
                    else -> recorded(number)
                }
            },
        )
    }

    private fun reconstruct(template: Int, actions: List<Action>, form: String? = null): ZulrahPhase {
        val base = captured[template - 1]
        val emerge = base.emerge.copy(symbol = form ?: base.emerge.symbol, initial = false)
        val candidates = (listOf(base) + captured.filter {
            it.emerge.x == emerge.x && it.emerge.z == emerge.z
        } + captured).distinct()
        val cursors = mutableMapOf<String, Int>()
        val events = mutableListOf(emerge)
        var tick = 3
        var jadIndex = 0
        val donors = mutableSetOf<String>()
        for ((index, action) in actions.withIndex()) {
            val kind = when (action) {
                Action.Gas -> "gas"
                Action.Egg -> "egg"
                Action.Tail -> "tail"
                else -> "attack"
            }
            val donor = candidates.first { phase -> phase.events.any { it.kind == kind } }
            donors += donor.evidence
            val groups = donor.events.filter { it.kind == kind }.groupBy { it.tick }.values.toList()
            val cursor = cursors.getOrDefault(kind, 0)
            cursors[kind] = cursor + 1
            for (event in groups[cursor % groups.size]) {
                val symbol = when (action) {
                    Action.Ranged -> RANGED
                    Action.Mixed -> MAGIC
                    Action.JadMagic -> if (jadIndex++ % 2 == 0) MAGIC else RANGED
                    Action.JadRanged -> if (jadIndex++ % 2 == 0) RANGED else MAGIC
                    else -> event.symbol
                }
                events += event.copy(tick = tick, symbol = symbol,
                    source = ZulrahPoint(event.source.x - donor.emerge.x + emerge.x,
                        event.source.z - donor.emerge.z + emerge.z),
                    mixedAttack = action == Action.Mixed)
            }
            val next = actions.getOrNull(index + 1)
            tick += when {
                action == Action.Tail -> if (next == Action.Tail) 7 else 8
                action == Action.Egg && next in listOf(Action.Mixed, Action.Ranged) -> 4
                action == Action.Gas && next == Action.Egg -> 4
                else -> 3
            }
        }
        val duration = events.last().tick + when (actions.last()) {
            Action.Tail -> 12
            Action.Gas, Action.Egg -> 7
            else -> 6
        }
        events += ZulrahRoutineEvent(duration - 3, "dive")
        return ZulrahPhase(duration, events,
            "Provisional reconstruction from ${donors.joinToString("; ")}")
    }
}
