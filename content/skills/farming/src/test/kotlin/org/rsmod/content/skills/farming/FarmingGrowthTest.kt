package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.random.GameRandom

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class FarmingGrowthTest {
    @Test
    fun `a 640 minute crop advances exactly on its offset boundaries`() {
        val crop = immune(640)
        val patch = patchFor(crop)
        for (offset in listOf(0, 7, 31)) {
            val boundary = 640L * 10 - offset
            val planted = planted(crop)
            assertEquals(planted, replay(patch, planted, boundary - 640, boundary - 1, offset))
            assertEquals(1, replay(patch, planted, boundary - 1, boundary, offset).stage)
            assertEquals(1, replay(patch, planted, boundary - 1, boundary + 639, offset).stage)
            assertEquals(2, replay(patch, planted, boundary - 1, boundary + 640, offset).stage)
            assertEquals(planted, replay(patch, planted, boundary, boundary + 639, offset))
        }
    }

    @Test
    fun `offline replay reaches maturity once and stays there`() {
        val crop = immune(640)
        val grown = replay(patchFor(crop), planted(crop), 0, 640L * (crop.stages + 50), 0)
        assertEquals(crop.stages, grown.stage)
        assertEquals(Health.HEALTHY, grown.health)
    }

    @Test
    fun `a diseased herb dies only on the next 40 minute boundary`() {
        val guam = crop("obj.guam_seed")
        val patch = patchFor(guam)
        val diseased = planted(guam).copy(stage = 2, health = Health.DISEASED)
        val offset = 3
        val cycle = 40L * 5 + 20 - offset
        assertEquals(diseased, replay(patch, diseased, cycle - 1, cycle, offset))
        val death = 40L * 6 - offset
        val dead = replay(patch, diseased, death - 1, death, offset)
        assertEquals(diseased.copy(health = Health.DEAD), dead)
        assertEquals(dead, replay(patch, dead, death, death + 40 * 4, offset))
    }

    @Test
    fun `legacy watering protection remains until the sourced reduction replaces the model`() {
        val potato = crop("obj.potato_seed")
        val watered = planted(potato).copy(stage = 1, watered = true)
        val minute = potato.cycleMinutes.toLong()
        val random = FixedRandom(roll = 0, double = 0.0)
        val next = replay(patchFor(potato), watered, minute - 1, minute, 0, random)
        assertEquals(watered.copy(stage = 2, watered = false), next)
        assertEquals(0, random.rolls)
        val diseased = replay(patchFor(potato), next, minute, minute * 2, 0, random)
        assertEquals(next.copy(health = Health.DISEASED), diseased)
    }

    @Test
    fun `anima withers at native stage eight and dies at stage nine`() {
        val attas = crop("obj.attas_seed")
        val patch = patchFor(attas)
        val minute = attas.cycleMinutes.toLong()
        val withered = replay(patch, planted(attas), 0, minute * 7, 0)
        assertEquals(7, withered.stage)
        assertEquals(Health.HEALTHY, withered.health)
        assertEquals(attas.art.healthy[7], withered.transmit(attas, attas.kind))
        val dead = replay(patch, withered, minute * 7, minute * 8, 0)
        assertEquals(withered.copy(stage = 8, health = Health.DEAD), dead)
        assertEquals(attas.art.healthy[8], dead.transmit(attas, attas.kind))
        assertEquals(dead, replay(patch, dead, minute * 8, minute * 20, 0))
    }

    @Test
    fun `disease replaces the stage advance and never strikes the first stage`() {
        val maple = crop("obj.plantpot_maple_sapling")
        val patch = patchFor(maple)
        val always = FixedRandom(roll = 0)
        val seedling = planted(maple)
        val first = maple.cycleMinutes.toLong()
        assertEquals(1, replay(patch, seedling, first - 1, first, 0, always).stage)
        val growing = seedling.copy(stage = 1, watered = true)
        assertEquals(growing.copy(health = Health.DISEASED, watered = false), replay(patch, growing, first - 1, first, 0, always))
        val never = FixedRandom(roll = 127)
        assertEquals(growing.copy(stage = 2, watered = false), replay(patch, growing, first - 1, first, 0, never))
    }

    @Test
    fun `disease chance is floor of max over compost divisor plus one in 128`() {
        val maple = crop("obj.plantpot_maple_sapling")
        val patch = patchFor(maple)
        val minute = maple.cycleMinutes.toLong()
        for ((compost, highest) in listOf(Compost.NONE to 12, Compost.NORMAL to 6, Compost.SUPER to 2, Compost.ULTRA to 1)) {
            val growing = planted(maple).copy(stage = 1, compost = compost)
            assertEquals(Health.DISEASED, replay(patch, growing, minute - 1, minute, 0, FixedRandom(highest)).health)
            assertEquals(Health.HEALTHY, replay(patch, growing, minute - 1, minute, 0, FixedRandom(highest + 1)).health)
        }
    }

    @Test
    fun `existing saved iasor affects sourced disease through withering but not after death`() {
        val maple = crop("obj.plantpot_maple_sapling")
        val iasor = crop("obj.iasor_seed")
        val patches = listOf(patchFor(maple), patchFor(iasor))
        val minute = maple.cycleMinutes.toLong()
        val growing = planted(maple).copy(stage = 1)
        for ((anima, health) in listOf(planted(iasor).copy(stage = 7) to Health.HEALTHY, planted(iasor).copy(stage = 8, health = Health.DEAD) to Health.DISEASED)) {
            val states = arrayOf(growing, anima)
            FarmingGrowth(patches, BooleanArray(2), FixedRandom(roll = 3)).replay(states, minute - 1, minute, 0)
            assertEquals(health, states[0].health)
        }
    }

    @Test
    fun `disease-free patches and gardener protection skip the roll`() {
        val maple = crop("obj.plantpot_maple_sapling")
        val patch = patchFor(maple)
        val minute = maple.cycleMinutes.toLong()
        val growing = planted(maple).copy(stage = 1)
        val random = FixedRandom(roll = 0)
        val free = arrayOf(growing)
        FarmingGrowth(listOf(patch), booleanArrayOf(true), random).replay(free, minute - 1, minute, 0)
        assertEquals(growing.copy(stage = 2), free[0])
        assertEquals(growing.copy(stage = 2, paid = true), replay(patch, growing.copy(paid = true), minute - 1, minute, 0, random))
        assertEquals(0, random.rolls)
    }

    @Test
    fun `one grown flower protects every allotment on its site and no other`() {
        val site = FarmingPatches.all.first { flowerSite(it.site) }.site
        val allotments = FarmingPatches.bySite(site).filter { it.kind == PatchKind.ALLOTMENT }
        val flower = FarmingPatches.bySite(site).first { it.kind == PatchKind.FLOWER }
        val elsewhere = FarmingPatches.all.first { it.kind == PatchKind.ALLOTMENT && it.site != site }
        val potato = crop("obj.potato_seed")
        val marigold = checkNotNull(FarmingCrops.byIndex(potato.protectionIndex))
        val lily = crop("obj.white_lily_seed")
        val woad = crop("obj.woad_seed")
        val patches = allotments + flower + elsewhere
        val growing = planted(potato).copy(stage = 1)
        val cases =
            listOf(
                grown(marigold) to true,
                grown(lily) to true,
                grown(woad) to false,
                planted(marigold).copy(stage = 1) to false,
                grown(marigold).copy(health = Health.DISEASED) to false,
                grown(marigold).copy(health = Health.DEAD) to false,
            )
        val minute = potato.cycleMinutes.toLong()
        for ((protector, protects) in cases) {
            val states = Array(patches.size) { if (patches[it] == flower) protector else growing }
            FarmingGrowth(patches, BooleanArray(patches.size), FixedRandom(roll = 0, double = 0.0))
                .replay(states, minute - 1, minute, 0)
            for (i in allotments.indices) {
                assertEquals(if (protects) Health.HEALTHY else Health.DISEASED, states[i].health, "$protector")
            }
            assertEquals(Health.DISEASED, states.last().health)
        }
    }

    @Test
    fun `protection starts the tick after the flower matures`() {
        val site = FarmingPatches.all.first { flowerSite(it.site) }.site
        val allotment = FarmingPatches.bySite(site).first { it.kind == PatchKind.ALLOTMENT }
        val flower = FarmingPatches.bySite(site).first { it.kind == PatchKind.FLOWER }
        val potato = crop("obj.potato_seed")
        val marigold = checkNotNull(FarmingCrops.byIndex(potato.protectionIndex))
        assertEquals(0, potato.cycleMinutes % marigold.cycleMinutes)
        val maturing = planted(marigold).copy(stage = marigold.stages - 1)
        val states = arrayOf(planted(potato).copy(stage = 1), maturing)
        val minute = potato.cycleMinutes.toLong()
        FarmingGrowth(listOf(allotment, flower), BooleanArray(2) { it == 1 }, FixedRandom(roll = 0, double = 0.0))
            .replay(states, minute - 1, minute, 0)
        assertEquals(Health.DISEASED, states[0].health)
        assertEquals(marigold.stages, states[1].stage)
    }

    @Test
    fun `empty patches regrow one weed stage on each five minute tick`() {
        val patch = FarmingPatches.all.first()
        val raked = PatchState(weeds = PatchState.CLEARED)
        assertEquals(raked, replay(patch, raked, 1, 4, 0))
        assertEquals(raked.copy(weeds = 2), replay(patch, raked, 4, 5, 0))
        assertEquals(raked.copy(weeds = 2), replay(patch, raked, 0, 4, 1))
        assertEquals(PatchState(), replay(patch, raked, 0, 15, 0))
        assertEquals(PatchState(), replay(patch, raked, 0, 525_600, 0))
    }

    @Test
    fun `empty grape plots never regrow weeds or discard their soil treatment`() {
        val patch = FarmingPatches.all.first { it.kind == PatchKind.GRAPE_VINE }
        for (weeds in 0..3) {
            val empty = PatchState(weeds = weeds, grapePrepared = true, fertilised = true)
            assertEquals(empty, replay(patch, empty, 0, 525_600, 0))
        }
    }

    @Test
    fun `auto weed prevents regrowth without removing existing weeds or a scarecrow`() {
        val flower = FarmingPatches.all.first { it.kind == PatchKind.FLOWER }
        val before = arrayOf(PatchState(weeds = 3), PatchState(weeds = 1, scarecrow = true))
        val states = before.copyOf()
        FarmingGrowth(listOf(flower, flower), BooleanArray(2), FixedRandom(127), autoWeed = true)
            .replay(states, 0, 525_600, 0)
        assertTrue(before.contentEquals(states))
        FarmingGrowth(listOf(flower, flower), BooleanArray(2), FixedRandom(127))
            .replay(states, 0, 15, 0)
        assertEquals(PatchState(), states[0])
        assertEquals(PatchState(scarecrow = true), states[1])
    }

    @Test
    fun `a weed covered scarecrow protects only sweetcorn on the same site`() {
        val site = FarmingPatches.all.first { flowerSite(it.site) }.site
        val allotment = FarmingPatches.bySite(site).first { it.kind == PatchKind.ALLOTMENT }
        val flower = FarmingPatches.bySite(site).first { it.kind == PatchKind.FLOWER }
        val elsewhere = FarmingPatches.all.first { it.kind == PatchKind.FLOWER && it.site != site }
        for (crop in listOf(crop("obj.sweetcorn_seed"), crop("obj.potato_seed"))) {
            for (protector in listOf(flower, elsewhere)) {
                val states = arrayOf(planted(crop).copy(stage = 1), PatchState(scarecrow = true))
                val minute = crop.cycleMinutes.toLong()
                FarmingGrowth(listOf(allotment, protector), BooleanArray(2), FixedRandom(0, 0.0))
                    .replay(states, minute - 1, minute, 0)
                val expected = if (crop.seed == "obj.sweetcorn_seed" && protector == flower) Health.HEALTHY else Health.DISEASED
                assertEquals(expected, states[0].health)
                assertEquals(PatchState(scarecrow = true), states[1])
            }
        }
    }

    @Test
    fun `checked renewables regrow one produce per regrow tick up to the maximum`() {
        val bush = FarmingCrops.all.first { it.kind == PatchKind.BUSH && it.regrowMinutes > 0 }
        val patch = patchFor(bush)
        val picked = grown(bush).copy(phase = Phase.CHECKED, produce = 0)
        val regrow = bush.regrowMinutes.toLong()
        assertEquals(1, replay(patch, picked, regrow - 1, regrow, 0).produce)
        assertEquals(picked, replay(patch, picked, regrow, regrow * 2 - 1, 0))
        assertEquals(bush.maxProduce, replay(patch, picked, 0, regrow * 100, 0).produce)
        val unchecked = picked.copy(phase = Phase.GROWING)
        assertEquals(unchecked, replay(patch, unchecked, 0, regrow * 100, 0))
    }

    @Test
    fun `a year offline with only settled patches finishes without rolling`() {
        val potato = crop("obj.potato_seed")
        val random = FixedRandom(roll = 0, double = 0.0)
        val states = arrayOf(grown(potato), grown(potato).copy(health = Health.DEAD), PatchState())
        val patches = FarmingPatches.all.filter { it.kind == PatchKind.ALLOTMENT }.take(3)
        FarmingGrowth(patches, BooleanArray(3), random).replay(states, 0, 525_600L * 1000, 0)
        assertEquals(0, random.rolls)
        assertTrue(states.contentEquals(arrayOf(grown(potato), grown(potato).copy(health = Health.DEAD), PatchState())))
    }

    private class FixedRandom(private val roll: Int, private val double: Double = 0.999) : GameRandom {
        var rolls = 0

        override fun of(maxExclusive: Int): Int = roll.also { rolls++ }

        override fun of(minInclusive: Int, maxInclusive: Int): Int = minInclusive

        override fun randomDouble(): Double = double.also { rolls++ }
    }

    private companion object {
        fun crop(seed: String): Crop = checkNotNull(FarmingCrops.bySeed(seed)) { seed }

        fun immune(cycle: Int): Crop =
            FarmingCrops.all.first {
                it.immune && it.cycleMinutes == cycle && it.firstStageMinutes == 0 && it.kind != PatchKind.ANIMA
            }

        fun patchFor(crop: Crop): PatchDef = FarmingPatches.all.first { it.kind == crop.kind }

        fun flowerSite(site: String): Boolean =
            FarmingPatches.bySite(site).count { it.kind == PatchKind.ALLOTMENT } == 2 &&
                FarmingPatches.bySite(site).any { it.kind == PatchKind.FLOWER }

        fun planted(crop: Crop) = PatchState(weeds = PatchState.CLEARED, cropIndex = crop.index)

        fun grown(crop: Crop) = planted(crop).copy(stage = crop.stages).matured(crop)

        fun replay(
            patch: PatchDef,
            state: PatchState,
            from: Long,
            to: Long,
            offset: Int,
            random: GameRandom = FixedRandom(roll = 127),
        ): PatchState {
            val states = arrayOf(state)
            FarmingGrowth(listOf(patch), BooleanArray(1), random).replay(states, from, to, offset)
            return states[0]
        }

        @JvmStatic
        @BeforeAll
        fun cache() {
            val root =
                generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                    .first { Files.isRegularFile(it.resolve("game.yml")) }
            val revision =
                Files.readAllLines(root.resolve("game.yml"))
                    .first { it.startsWith("revision:") }.substringAfter(':').trim().substringBefore('.').toInt()
            ServerCacheManager.init(revision).close()
        }
    }
}
