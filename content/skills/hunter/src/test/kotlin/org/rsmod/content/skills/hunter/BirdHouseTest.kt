package org.rsmod.content.skills.hunter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.MapClock
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class BirdHouseTest {
    private val registry = ObjRegistry(ZoneUpdateMap())
    private val f = HunterFixture()
    private val script = BirdHouseScript(ObjRepository(MapClock(100), registry))
    private val space = BirdHouseScript.SPACES.first()
    private val tier by lazy { BirdHouseScript.TIERS.first() }

    private fun levels() {
        for (stat in listOf("stat.hunter", "stat.crafting")) {
            f.player.statMap.setBaseLevel(stat, 99)
            f.player.statMap.setCurrentLevel(stat, 99)
        }
    }

    private fun seeded() = VarPlayerIntMapSetter.set(f.player, space.transmitVarp, tier.tier * 3 + 3)

    private fun ground(obj: String) = registry.findAll(f.player.coords).filter { it.type == InvObj(obj, 1).id }.sumOf { it.count.toLong() }

    private fun state() = script.state(f.player, space)

    @Test
    fun `one bird house built twice places one house`() {
        levels()
        f.player.inv[0] = InvObj(tier.item.internalName, 1)
        f.call(script, "build", space, tier)
        VarPlayerIntMapSetter.set(f.player, space.transmitVarp, 0)
        f.call(script, "build", space, tier)
        assertEquals(0L, f.total(tier.item.internalName))
        assertEquals(0, state())
    }

    @Test
    fun `seeds added twice take ten seeds`() {
        levels()
        VarPlayerIntMapSetter.set(f.player, space.transmitVarp, tier.tier * 3 + 1)
        val seed = BirdHouseScript.SEEDS.first { it !in BirdHouseScript.HIGH_LEVEL_SEEDS }
        f.player.inv[0] = InvObj(seed, 25)
        f.run(script, "addSeeds", space, seed)
        f.run(script, "addSeeds", space, seed)
        assertEquals(15L, f.total(seed))
        assertEquals(tier.tier * 3 + 3, state())
    }

    @Test
    fun `a house emptied twice gives its loot once`() {
        levels()
        seeded()
        f.call(script, "empty", space)
        val feathers = f.total(BirdHouseScript.FEATHER)
        f.call(script, "empty", space)
        assertEquals(1L, f.total(BirdHouseScript.CLOCKWORK))
        assertEquals(feathers, f.total(BirdHouseScript.FEATHER))
        assertEquals(BirdHouseScript.BIRDS.toLong(), ground(BirdHouseScript.RAW_BIRD_MEAT))
        assertEquals(0, state())
    }

    @Test
    fun `a full inventory drops the loot`() {
        levels()
        seeded()
        f.fill()
        f.call(script, "empty", space)
        assertEquals(1L, ground(BirdHouseScript.CLOCKWORK))
        assertTrue(ground(BirdHouseScript.FEATHER) > 0)
    }

    @Test
    fun `a full feather stack drops the feathers`() {
        levels()
        seeded()
        f.player.inv[0] = InvObj(BirdHouseScript.FEATHER, Int.MAX_VALUE)
        f.call(script, "empty", space)
        assertEquals(Int.MAX_VALUE.toLong(), f.total(BirdHouseScript.FEATHER))
        assertTrue(ground(BirdHouseScript.FEATHER) > 0)
        assertEquals(1L, f.total(BirdHouseScript.CLOCKWORK))
    }

    @Test
    fun `a reset uses the emptied clockwork and one log`() {
        levels()
        seeded()
        f.player.inv[0] = InvObj(BirdHouseScript.HAMMER, 1)
        f.player.inv[1] = InvObj(BirdHouseScript.CHISEL, 1)
        f.player.inv[2] = InvObj(tier.logs.internalName, 1)
        f.run(script, "reset", space)
        f.run(script, "reset", space)
        assertEquals(0L, f.total(BirdHouseScript.CLOCKWORK))
        assertEquals(0L, f.total(tier.logs.internalName))
        assertEquals(tier.tier * 3 + 1, state())
    }

    private fun materials() {
        f.player.inv[0] = InvObj(BirdHouseScript.HAMMER, 1)
        f.player.inv[1] = InvObj(BirdHouseScript.CHISEL, 1)
        f.player.inv[2] = InvObj(tier.logs.internalName, 1)
        f.player.inv[3] = InvObj(tier.logs.internalName, 1)
        f.player.inv[4] = InvObj(BirdHouseScript.CLOCKWORK, 1)
    }

    @Test
    fun `a house built twice from logs in a full inventory takes one log and one clockwork`() {
        levels()
        materials()
        f.fill()
        f.run(script, "buildFromLogs", space)
        f.run(script, "buildFromLogs", space)
        assertEquals(0L, f.total(BirdHouseScript.CLOCKWORK))
        assertEquals(1L, f.total(tier.logs.internalName))
        assertEquals(0L, f.total(tier.item.internalName))
        assertEquals(tier.tier * 3 + 1, state())
    }

    @Test
    fun `logs dropped while building keep the clockwork`() {
        levels()
        materials()
        f.player.inv[3] = null
        f.call(script, "buildFromLogs", space)
        f.player.inv[2] = null
        f.finish()
        assertEquals(1L, f.total(BirdHouseScript.CLOCKWORK))
        assertEquals(0L, f.total(tier.item.internalName))
        assertEquals(0, state())
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
