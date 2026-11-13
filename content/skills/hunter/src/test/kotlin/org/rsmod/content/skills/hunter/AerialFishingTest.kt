package org.rsmod.content.skills.hunter

import dev.openrune.util.Wearpos
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.table.hunter.HunterAerialFishRow
import org.rsmod.game.MapClock
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class AerialFishingTest {
    private val registry = ObjRegistry(ZoneUpdateMap())
    private val f = HunterFixture()
    private val script =
        AerialFishingScript(
            mockk(relaxed = true),
            mockk(relaxed = true),
            ObjRepository(MapClock(100), registry),
            mockk(relaxed = true),
            mockk(relaxed = true),
            f.random,
            MapClock(100),
            mockk(relaxed = true),
        )
    private val fish by lazy { HunterAerialFishRow.all().first() }
    private val fishes by lazy { HunterAerialFishRow.all().map { it.fish.internalName } }

    private fun ground() = registry.findAll(f.player.coords).toList()

    private fun catchFish() {
        val method = script.javaClass.declaredMethods.single { it.name.startsWith("catchFish") }
        method.isAccessible = true
        method.invoke(script, f.player, f.player.coords.packed)
    }

    private fun levels() {
        for (stat in listOf("stat.hunter", "stat.fishing")) {
            f.player.statMap.setBaseLevel(stat, 99)
            f.player.statMap.setCurrentLevel(stat, 99)
        }
    }

    @Test
    fun `one fish cut twice gives one offcut`() {
        f.player.inv[0] = InvObj(fish.fish.internalName, 1)
        f.call(script, "cut", fish)
        f.call(script, "cut", fish)
        assertEquals(0L, f.total(fish.fish.internalName))
        assertEquals(1L, f.total(AerialFishingScript.FISH_OFFCUTS))
    }

    @Test
    fun `a full offcut stack keeps the fish`() {
        f.player.inv[0] = InvObj(fish.fish.internalName, 1)
        f.player.inv[1] = InvObj(AerialFishingScript.FISH_OFFCUTS, Int.MAX_VALUE)
        f.call(script, "cut", fish)
        assertEquals(1L, f.total(fish.fish.internalName))
        assertEquals(Int.MAX_VALUE.toLong(), f.total(AerialFishingScript.FISH_OFFCUTS))
    }

    @Test
    fun `a tench sold twice gives one hundred pearls`() {
        f.player.inv[0] = InvObj(AerialFishingScript.GOLDEN_TENCH, 1)
        f.call(script, "sellTench")
        f.call(script, "sellTench")
        assertEquals(0L, f.total(AerialFishingScript.GOLDEN_TENCH))
        assertEquals(100L, f.total(AerialFishingScript.PEARL))
    }

    @Test
    fun `a full pearl stack keeps the tench`() {
        f.player.inv[0] = InvObj(AerialFishingScript.GOLDEN_TENCH, 1)
        f.player.inv[1] = InvObj(AerialFishingScript.PEARL, Int.MAX_VALUE - 10)
        f.call(script, "sellTench")
        assertEquals(1L, f.total(AerialFishingScript.GOLDEN_TENCH))
        assertEquals(Int.MAX_VALUE - 10L, f.total(AerialFishingScript.PEARL))
    }

    @Test
    fun `each catch takes one king worm and gives its fish`() {
        levels()
        f.player.inv[0] = InvObj(AerialFishingScript.KING_WORM, 100)
        repeat(20) { catchFish() }
        val caught = fishes.sumOf { f.total(it) }
        assertEquals(80L, f.total(AerialFishingScript.KING_WORM))
        assertEquals(20L, caught + f.total(AerialFishingScript.GOLDEN_TENCH))
        assertTrue(ground().all { it.type == InvObj(AerialFishingScript.PEARL, 1).id || it.type == InvObj(AerialFishingScript.GOLDEN_TENCH, 1).id })
    }

    @Test
    fun `a full inventory drops only pearls and tench, fish are lost`() {
        levels()
        f.player.inv[0] = InvObj(AerialFishingScript.KING_WORM, 100)
        f.fill()
        repeat(20) { catchFish() }
        assertEquals(0L, fishes.sumOf { f.total(it) })
        assertTrue(ground().all { it.type == InvObj(AerialFishingScript.PEARL, 1).id || it.type == InvObj(AerialFishingScript.GOLDEN_TENCH, 1).id })
    }

    @Test
    fun `offcut bait is taken once every three or four catches`() {
        levels()
        f.player.inv[0] = InvObj(AerialFishingScript.FISH_OFFCUTS, 100)
        repeat(24) { catchFish() }
        val used = 100L - f.total(AerialFishingScript.FISH_OFFCUTS)
        assertTrue(used in 6L..8L)
    }

    @Test
    fun `the whole-fish upgrade paid twice takes twenty pearls once`() {
        f.player.inv[0] = InvObj(AerialFishingScript.PEARL, AerialFishingScript.WHOLE_FISH_COST * 2)
        f.call(script, "payWholeFish")
        f.call(script, "payWholeFish")
        assertEquals(AerialFishingScript.WHOLE_FISH_COST.toLong(), f.total(AerialFishingScript.PEARL))
        assertEquals(1, f.player.vars[AerialFishingScript.WHOLE_FISH_PAID])
    }

    @Test
    fun `pearls gone before the upgrade is paid unlock nothing`() {
        f.player.inv[0] = InvObj(AerialFishingScript.PEARL, AerialFishingScript.WHOLE_FISH_COST - 1)
        f.call(script, "payWholeFish")
        assertEquals(AerialFishingScript.WHOLE_FISH_COST - 1L, f.total(AerialFishingScript.PEARL))
        assertEquals(0, f.player.vars[AerialFishingScript.WHOLE_FISH_PAID])
    }

    @Test
    fun `a bird taken twice gives one glove`() {
        f.call(script, "takeBird")
        f.call(script, "takeBird")
        assertEquals(1L, gloves())
    }

    @Test
    fun `a hand item equipped before the bird is given blocks the glove`() {
        f.player.worn[Wearpos.LeftHand.slot] = InvObj("obj.bronze_sq_shield", 1)
        f.call(script, "takeBird")
        assertEquals(0L, gloves())
        assertEquals(1L, f.total("obj.bronze_sq_shield"))
    }

    private fun gloves() = AerialFishingScript.GLOVES.sumOf { f.total(it) }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
