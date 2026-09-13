package org.rsmod.content.bosses.zulrah

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ZulrahRoutineTest {
    private val routine = ZulrahRoutine.recorded

    @Test
    fun `recorded branch includes only nineteen complete phases and stops at evidence limit`() {
        assertEquals(178, routine.events.size)
        assertEquals(19, routine.events.count { it.kind == "emerge" })
        assertEquals(520, routine.endTick)
        assertEquals("234.1", routine.revision)
        assertEquals("Grid Master", routine.mode)
        assertEquals(64, routine.sourceSha256.length)
        val spec = ZulrahSpec.recorded(routine)
        assertEquals(520, spec.phases.values.sumOf { it.exitAfter ?: 0 })
        val end = spec.phases.getValue("evidence_limit")
        assertNull(end.nextPhase)
        assertNull(end.exitAfter)
        assertTrue((end.selector as org.rsmod.api.bosses.spec.Selector.WeightedRandom).entries.isEmpty())
    }

    @Test
    fun `tail ability uses the DSL with the approved provisional impact rules`() {
        val spec = ZulrahSpec.recorded(routine)
        val tails = routine.events.filter { it.kind == "tail" }
        assertEquals(6, tails.size)
        for (tail in tails) {
            val sequence = spec.abilities.getValue("recorded_${tail.tick}") as org.rsmod.api.bosses.spec.Effect.Sequence
            assertEquals(2, sequence.effects.size)
            val windup = sequence.effects.first() as org.rsmod.api.bosses.spec.Effect.External
            assertEquals("zulrah.tail_attack", windup.handler)
            assertEquals(ZulrahTailAttack(4, 5, 20..30), windup.params)
        }
    }

    @Test
    fun `jad phase uses eight recorded alternating attacks starting with magic`() {
        val attacks = routine.events.filter { it.kind == "attack" && it.tick in 284 until 314 }
        assertEquals((287..308 step 3).toList(), attacks.map { it.tick })
        assertEquals(List(8) {
            if (it % 2 == 0) "spotanim.snakeboss_fireball" else "spotanim.snakeboss_orb"
        }, attacks.map { it.symbol })
    }

    @Test
    fun `gas impact delayed through dive retains its actual packet timing`() {
        val gas = routine.events.single {
            it.kind == "gas" && it.tick == 22 && it.target == ZulrahPoint(-4, -1)
        }
        assertEquals(10, gas.impactDelay)
        assertEquals(120, gas.endtime)
        assertEquals(29, routine.events.first { it.kind == "emerge" && !it.initial }.tick)
    }

    @Test
    fun `DSL phases preserve ordered event ticks and simultaneous projectiles`() {
        val spec = ZulrahSpec.recorded(routine)
        val ticks = routine.events.map { it.tick }.distinct()
        assertEquals(ticks.map { "step_$it" } + "evidence_limit", spec.phases.keys.toList())
        for ((index, tick) in ticks.withIndex()) {
            val phase = spec.phases.getValue("step_$tick")
            assertEquals("recorded_$tick", phase.entry)
            assertEquals((ticks.getOrNull(index + 1) ?: routine.endTick) - tick, phase.exitAfter)
        }
        val effects = (spec.abilities.getValue("recorded_13") as org.rsmod.api.bosses.spec.Effect.Sequence).effects
        assertEquals(2, effects.filterIsInstance<org.rsmod.api.bosses.spec.Effect.External>()
            .count { it.handler == "zulrah.hazard" })
        assertEquals(2, routine.events.count { it.tick == 13 && it.kind == "gas" })
    }

    @Test
    fun `opening and sixth egg wave match the Wiki while other green phases attack`() {
        val opening = routine.events.filter { it.tick < 29 }
        assertEquals(8, opening.count { it.kind == "gas" })
        assertEquals(0, opening.count { it.kind == "attack" || it.kind == "egg" })
        val wave = routine.events.filter { it.tick in 166 until 201 }
        assertEquals(6, wave.count { it.kind == "egg" })
        assertEquals(0, wave.count { it.kind == "attack" })
        assertEquals(4, routine.events.count { it.tick in 148 until 166 && it.kind == "attack" })
        assertEquals(5, routine.events.count { it.tick in 333 until 368 && it.kind == "attack" })
    }

    @Test
    fun `each cloud has a packet verified lifetime rather than a shared fallback`() {
        val clouds = routine.events.filter { it.kind == "gas" }
        assertEquals(46, clouds.size)
        assertTrue(clouds.all { it.cloudLifetime == 30 })
        val invalid = routine.events.map {
            if (it.kind == "gas") it.copy(cloudLifetime = 0) else it
        }
        assertThrows(IllegalArgumentException::class.java) { routine.copy(events = invalid) }
    }

    @Test
    fun `transitions use original anchors and three tick dives`() {
        val emerges = routine.events.filter { it.kind == "emerge" }
        assertEquals(setOf(ZulrahPoint(0, 0), ZulrahPoint(10, -2),
            ZulrahPoint(0, -11), ZulrahPoint(-10, -2)), emerges.map { ZulrahPoint(it.x, it.z) }.toSet())
        for (emerge in emerges.drop(1)) {
            assertTrue(routine.events.any { it.kind == "dive" && it.tick == emerge.tick - 3 })
        }
    }
}
