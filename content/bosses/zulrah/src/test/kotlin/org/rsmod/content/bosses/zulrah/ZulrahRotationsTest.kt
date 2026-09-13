package org.rsmod.content.bosses.zulrah

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.Selector

class ZulrahRotationsTest {
    @Test
    fun `all four Wiki form and anchor sequences are present`() {
        val expected = listOf(
            "GN MN TN GS MN TW GS TS GW MN",
            "GN MN TN GW TS MN GE TS GW MN",
            "GN GE MN TW GS TE GN GW TN GE TN",
            "GN TE GS TW MN GE GS TW GN TN GE TN",
        )
        val actual = ZulrahRotations.rotations.map { phases ->
            phases.joinToString(" ") { phase ->
                val form = when (phase.emerge.symbol) {
                    "npc.snakeboss_boss_ranged" -> "G"
                    "npc.snakeboss_boss_melee" -> "M"
                    "npc.snakeboss_boss_magic" -> "T"
                    else -> error("Unknown form")
                }
                val location = when (ZulrahPoint(phase.emerge.x, phase.emerge.z)) {
                    ZulrahPoint(0, 0) -> "N"
                    ZulrahPoint(10, -2) -> "E"
                    ZulrahPoint(-10, -2) -> "W"
                    ZulrahPoint(0, -11) -> "S"
                    else -> error("Unknown anchor")
                }
                form + location
            }
        }
        assertEquals(expected, actual)
    }

    @Test
    fun `Wiki action counts include paired gas barrages and two disputed green corrections`() {
        val expected = listOf(
            listOf("0/8/0/0", "0/0/0/2", "4/0/0/0", "5/4/4/0", "0/0/0/2",
                "5/0/0/0", "0/6/4/0", "5/4/3/0", "10/8/0/0", "0/0/0/2"),
            listOf("0/8/0/0", "0/0/0/2", "4/0/0/0", "0/6/4/0", "5/4/4/0",
                "0/0/0/2", "5/0/0/0", "5/4/3/0", "10/8/0/0", "0/0/0/2"),
            listOf("0/8/0/0", "5/0/3/0", "0/6/3/2", "5/0/0/0", "5/0/0/0",
                "5/0/0/0", "0/6/3/0", "5/0/0/0", "5/4/3/0", "10/0/0/0", "0/0/4/0"),
            listOf("0/8/0/0", "6/0/4/0", "4/4/0/0", "4/0/4/0", "0/4/0/2",
                "4/0/0/0", "0/6/6/0", "5/0/4/0", "4/0/0/0", "4/6/0/0", "8/0/0/0", "0/0/4/0"),
        )
        assertEquals(expected, ZulrahRotations.rotations.map { phases -> phases.map { phase ->
            listOf("attack", "gas", "egg", "tail").joinToString("/") { kind ->
                phase.events.count { it.kind == kind }.toString()
            }
        } })
    }

    @Test
    fun `Jad alternation is fixed and never uses the tanzanite random selector`() {
        for ((rotation, index) in listOf(8, 8, 9, 10).withIndex()) {
            val phase = ZulrahRotations.rotations[rotation][index]
            val attacks = phase.events.filter { it.kind == "attack" }
            val magicFirst = rotation >= 2
            assertEquals(if (rotation == 3) 8 else 10, attacks.size)
            attacks.forEachIndexed { shot, event ->
                assertFalse(event.mixedAttack)
                assertEquals(if ((shot % 2 == 0) == magicFirst) ZulrahRotations.MAGIC else ZulrahRotations.RANGED,
                    event.symbol)
            }
            assertTrue(attacks.zipWithNext().all { (a, b) -> b.tick - a.tick == 3 })
        }
    }

    @Test
    fun `all hazards retain observed arena targets and bounded expiry`() {
        val recorded = ZulrahRoutine.recorded.events
        val targets = recorded.filter { it.kind in setOf("gas", "egg") }.groupBy { it.kind }
        for (phase in ZulrahRotations.rotations.flatten()) {
            assertTrue(phase.evidence.isNotBlank())
            for (event in phase.events.filter { it.kind in targets }) {
                assertTrue(targets.getValue(event.kind).any { it.target == event.target })
                assertTrue(event.impactDelay > 0)
                assertTrue(event.source.x - phase.emerge.x in 1..3)
                assertTrue(event.source.z - phase.emerge.z in 1..3)
                if (event.kind == "gas") assertEquals(30, event.cloudLifetime)
                else assertTrue(event.spawn in setOf(ZulrahEncounterController.MELEE_SNAKE,
                    ZulrahEncounterController.MAGIC_SNAKE))
            }
            assertEquals(phase.duration - 3, phase.events.last().tick)
        }
    }

    @Test
    fun `initial opening differs from the recurring opening and the latter loops into selection`() {
        assertEquals(0, ZulrahRotations.opening.events.count { it.kind == "attack" })
        assertTrue(ZulrahRotations.opening.emerge.initial)
        assertEquals(5, ZulrahRotations.recurringOpening.events.count { it.kind == "attack" })
        assertEquals(8, ZulrahRotations.recurringOpening.events.count { it.kind == "gas" })
        assertFalse(ZulrahRotations.recurringOpening.emerge.initial)
        val spec = ZulrahSpec.boss
        assertFalse("evidence_limit" in spec.phases)
        assertEquals("select_rotation", spec.phases.filterKeys { it.startsWith("recurring_") }.values.last().nextPhase)
        for (rotation in 1..4) {
            assertEquals("recurring_0", spec.phases.filterKeys { it.startsWith("rotation_${rotation}_") }.values.last().nextPhase)
        }
        val choose = spec.abilities.getValue("select_rotation") as Effect.Choose
        val selector = choose.selector as Selector.WeightedRandom
        assertEquals(4, selector.entries.size)
        assertTrue(selector.entries.all { it.weight == 1 && it.cooldown == 0 })
        for ((name, branch) in choose.branches) {
            assertEquals(listOf(Effect.TransitionTo("${name}_0"), Effect.Run("${name}_event_0")),
                (branch as Effect.Sequence).effects)
        }
    }

    @Test
    fun `normal tanzanite shots sample both observed styles without affecting green attacks`() {
        assertEquals(setOf(ZulrahRotations.MAGIC, ZulrahRotations.RANGED), ZulrahRotations.mixedWeights.keys)
        assertTrue(ZulrahRotations.mixedWeights.getValue(ZulrahRotations.MAGIC) >
            ZulrahRotations.mixedWeights.getValue(ZulrahRotations.RANGED))
        for (phase in ZulrahRotations.rotations.flatten()) {
            for (event in phase.events.filter { it.kind == "attack" }) {
                assertEquals(phase.emerge.symbol == "npc.snakeboss_boss_magic", event.mixedAttack)
            }
        }
    }
}
