package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.combat.weapon.WeaponSpeeds
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.assignInstance
import org.rsmod.api.instances.region.InstanceAreaResolver
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.events.interact.LocUDefaultEvents
import org.rsmod.api.player.ironman.PlayerGamemode
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.DefaultGameRandom
import org.rsmod.api.random.GameRandom
import org.rsmod.api.registry.controller.ControllerRegistry
import org.rsmod.api.registry.loc.LocRegistry
import org.rsmod.api.registry.loc.LocRegistryNormal
import org.rsmod.api.registry.loc.LocRegistryRegion
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.registry.zone.ZonePlayerActivityBitSet
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.interfaces.bank.BankTab
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.events.SuspendEvent
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.ControllerList
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocZoneKey
import org.rsmod.game.map.LocZoneStorage
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.game.region.RegionListLarge
import org.rsmod.game.region.RegionListSmall
import org.rsmod.game.region.RegionListWorldEntity
import org.rsmod.game.type.placeholder
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareKey
import org.rsmod.map.zone.ZoneGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class FarmingTitheInventoryTest {
    @Test
    fun `owned logavano planting publishes once after seed and plot commit`() {
        val f = PlantFixture()
        val seed = f.crop.seed.internalName
        f.events.subscribeUnbound(FarmingTithePlantEvent::class.java) {
            assertEquals(f.player, player)
            assertEquals(seed, this.seed)
            assertEquals(1, f.player.inv.physicalCount(seed))
            assertFalse(f.tithe.plant(player, f.plot, f.crop, 0))
        }
        assertTrue(f.tithe.plant(f.player, f.plot, f.crop, 0))
        assertEquals(listOf(seed), f.planted)
        assertEquals(1, f.player.inv.physicalCount(seed))
        assertFalse(f.tithe.plant(f.player, f.plot, f.crop, 0))
        assertEquals(listOf(seed), f.planted)
    }

    @Test
    fun `denied stale missing seed and foreign instance plants never publish or spend seeds`() {
        val f = PlantFixture()
        val player = f.player
        player.statMap.setCurrentLevel(STAT_FARMING, (f.crop.level - 1).toByte())
        assertFalse(f.tithe.plant(player, f.plot, f.crop, 0))
        player.statMap.setCurrentLevel(STAT_FARMING, 99)
        VarPlayerIntMapSetter.set(player, FarmingTithe.TYPE, 1)
        assertFalse(f.tithe.plant(player, f.plot, f.crop, 0))
        VarPlayerIntMapSetter.set(player, FarmingTithe.TYPE, f.crop.tier)
        player.inv[0] = null
        assertFalse(f.tithe.plant(player, f.plot, f.crop, 0))
        player.inv[0] = InvObj(f.crop.seed, 2)
        assertFalse(f.tithe.plant(player, f.plot, f.crop, 1))
        val foreign = BoundLocInfo(CoordGrid(1810, 3502), f.plot.entity, 2, 1, 1, 0)
        assertFalse(f.tithe.plant(player, foreign, f.crop, 0))
        val entered = player.coords
        player.coords = foreign.coords
        assertFalse(f.tithe.plant(player, f.plot, f.crop, 0))
        player.coords = entered
        val session = f.instances.sessionsForKey(FarmingTithe.KEY).single()
        val intruder = Fixture().player.apply {
            uuid = checkNotNull(player.uuid) + 1
            coords = player.coords
            inv[0] = InvObj(f.crop.seed, 2)
            inv[1] = InvObj(SPADE)
            inv[2] = InvObj(DIBBER)
        }
        assertFalse(f.tithe.plant(intruder, f.plot, f.crop, 0))
        intruder.assignInstance(session.id)
        assertFalse(f.tithe.plant(intruder, f.plot, f.crop, 0))
        assertTrue(f.locs.del(LocInfo(f.plot.layer, f.plot.coords, f.plot.entity), Int.MAX_VALUE))
        assertFalse(f.tithe.plant(player, f.plot, f.crop, 0))
        assertEquals(2, player.inv.physicalCount(f.crop.seed.internalName))
        assertEquals(2, intruder.inv.physicalCount(f.crop.seed.internalName))
        assertEquals(emptyList<String>(), f.planted)
    }

    @Test
    fun `seed supply rechecks boosted level, one tier and ten thousand cap`() {
        val f = Fixture()
        val crop = FarmingTitheData.crops.first()
        f.player.statMap.setCurrentLevel(STAT_FARMING, (crop.level - 1).toByte())
        assertFalse(takeTitheSeeds(f.player, crop, 10000))
        assertEquals(0, f.count(crop.seed.internalName))
        f.player.statMap.setCurrentLevel(STAT_FARMING, crop.level.toByte())
        assertTrue(takeTitheSeeds(f.player, crop, Int.MAX_VALUE))
        assertEquals(10000, f.count(crop.seed.internalName))
        assertFalse(takeTitheSeeds(f.player, crop, 1))
        f.player.statMap.setCurrentLevel(STAT_FARMING, 99)
        assertFalse(takeTitheSeeds(f.player, FarmingTitheData.crops[1], 100))
        assertEquals(10000, f.count(crop.seed.internalName))
        assertEquals(0, f.count(FarmingTitheData.crops[1].seed.internalName))
    }

    @Test
    fun `failed seed award preserves selected type and all inventory items`() {
        val f = Fixture()
        f.fill()
        f.set(FarmingTithe.TYPE, 3)
        assertFalse(takeTitheSeeds(f.player, FarmingTitheData.crops.first(), 100))
        assertEquals(3, f.player.vars[FarmingTithe.TYPE])
        assertEquals(28, f.count(SPADE))
    }

    @Test
    fun `partial sack consumes only its remainder and cannot pay the same fruit twice`() {
        val f = Fixture()
        val crop = FarmingTitheData.crops[2]
        f.player.inv[0] = InvObj(crop.fruit, 30)
        f.set(FarmingTithe.SESSION, 75)
        f.set(FarmingTithe.SCORE, 74)
        assertTrue(depositTitheFruit(f.player, crop, 1.0, 1.0))
        assertEquals(4, f.count(crop.fruit.internalName))
        assertEquals(0, f.player.vars[FarmingTithe.SCORE])
        assertEquals(101, f.player.vars[FarmingTithe.SESSION])
        assertEquals(11, f.player.vars[FarmingTithe.POINTS])
        assertEquals(177100, f.xp())
        assertTrue(depositTitheFruit(f.player, crop, 1.0, 1.0))
        val xp = f.xp()
        val points = f.player.vars[FarmingTithe.POINTS]
        assertEquals(0, f.count(crop.fruit.internalName))
        assertFalse(depositTitheFruit(f.player, crop, 1.0, 1.0))
        assertEquals(xp, f.xp())
        assertEquals(points, f.player.vars[FarmingTithe.POINTS])
    }

    @Test
    fun `points cap does not destroy deposit XP and instance ten thousand limit keeps excess fruit`() {
        val f = Fixture()
        val crop = FarmingTitheData.crops.first()
        f.set(FarmingTithe.POINTS, 15999)
        f.player.inv[0] = InvObj(crop.fruit, 100)
        assertTrue(depositTitheFruit(f.player, crop, 1.0, 1.0))
        assertEquals(16000, f.player.vars[FarmingTithe.POINTS])
        assertEquals(90600, f.xp())
        f.set(FarmingTithe.SESSION, 10000)
        f.set(FarmingTithe.SCORE, 99)
        f.player.inv[0] = InvObj(crop.fruit, 5)
        assertTrue(depositTitheFruit(f.player, crop, 1.0, 1.0))
        assertEquals(4, f.count(crop.fruit.internalName))
        assertEquals(10001, f.player.vars[FarmingTithe.SESSION])
        val xp = f.xp()
        assertFalse(depositTitheFruit(f.player, crop, 1.0, 1.0))
        assertEquals(xp, f.xp())
        assertEquals(4, f.count(crop.fruit.internalName))
    }

    @Test
    fun `last blessing frees a slot and retains the blessed output and rate for that pick`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(FarmingGrapes.BLESSING)
        f.fill()
        val harvest = FarmingGrapes.prepare(f.player, PatchState(), false, false)
        assertTrue(harvest.blessed)
        assertTrue(FarmingGrapes.prepareHarvest(f.player, harvest).success)
        assertEquals(1, f.count(FarmingGrapes.BLESSING))
        assertTrue(f.harvest(harvest))
        assertEquals(0, f.count(FarmingGrapes.BLESSING))
        assertEquals(1, f.count(FarmingGrapes.ZAMORAK_GRAPES))
        assertEquals(0, f.count(FarmingGrapes.GRAPES))
        assertFalse(f.harvest(harvest))
        assertEquals(1, f.count(FarmingGrapes.ZAMORAK_GRAPES))
    }

    @Test
    fun `full inventory cannot spend a blessing when its stack would remain`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(FarmingGrapes.BLESSING, 2)
        f.fill()
        val harvest = FarmingGrapes.prepare(f.player, PatchState(fertilised = true), true, true)
        assertFalse(FarmingGrapes.prepareHarvest(f.player, harvest).success)
        assertFalse(f.harvest(harvest))
        assertEquals(2, f.count(FarmingGrapes.BLESSING))
        assertEquals(0, f.count(FarmingGrapes.ZAMORAK_GRAPES))
        f.player.inv[1] = null
        assertTrue(f.harvest(harvest))
        assertEquals(1, f.count(FarmingGrapes.BLESSING))
        assertEquals(1, f.count(FarmingGrapes.ZAMORAK_GRAPES))
    }

    @Test
    fun `refund refuses point overflow and a vanished or already refunded item`() {
        val f = Fixture()
        val hat = checkNotNull(ServerCacheManager.getItem("obj.tithe_reward_hat_male".asRSCM()))
        f.player.inv[0] = InvObj(hat)
        f.set(FarmingTithe.POINTS, 15941)
        assertFalse(refundTitheOutfit(f.player, hat.id, 0, 60))
        assertEquals(hat.id, f.player.inv[0]?.id)
        f.set(FarmingTithe.POINTS, 15940)
        assertTrue(refundTitheOutfit(f.player, hat.id, 0, 60))
        assertEquals(16000, f.player.vars[FarmingTithe.POINTS])
        f.set(FarmingTithe.POINTS, 0)
        assertFalse(refundTitheOutfit(f.player, hat.id, 0, 60))
        assertEquals(0, f.player.vars[FarmingTithe.POINTS])
    }

    @Test
    fun `herb box spends one roll per delivered herb and full inventory preserves unopened box`() {
        val f = Fixture()
        val random = CountingRandom()
        val closed = InvObj(FarmingTitheHerbBoxes.CLOSED)
        f.player.inv[0] = closed
        f.fill()
        assertFalse(FarmingTitheHerbBoxes.take(f.player, 0, closed, random))
        assertEquals(0, random.calls)
        assertEquals(closed, f.player.inv[0])
        for (count in 1..7) {
            f.player.inv[count] = null
            val current = checkNotNull(f.player.inv[0])
            assertTrue(FarmingTitheHerbBoxes.take(f.player, 0, current, random))
            assertEquals(count, f.count("obj.unidentified_guam"))
            if (count < 7) {
                assertEquals(7 - count, FarmingTitheHerbBoxes.remaining(checkNotNull(f.player.inv[0])))
            }
        }
        assertEquals(7, random.calls)
        assertEquals(0, f.count(FarmingTitheHerbBoxes.CLOSED))
        assertEquals(0, f.count(FarmingTitheHerbBoxes.OPEN))
        assertFalse(FarmingTitheHerbBoxes.take(f.player, 0, closed, random))
        assertEquals(7, f.count("obj.unidentified_guam"))
        assertEquals(7, random.calls)
    }

    @Test
    fun `last herb replaces the final box even without a free slot`() {
        val f = Fixture()
        val random = CountingRandom()
        val final = InvObj(FarmingTitheHerbBoxes.OPEN, 1, vars = 1)
        f.player.inv[0] = final
        f.fill()
        assertTrue(FarmingTitheHerbBoxes.take(f.player, 0, final, random))
        assertEquals(1, f.count("obj.unidentified_guam"))
        assertEquals(27, f.count(SPADE))
        assertEquals(0, f.count(FarmingTitheHerbBoxes.OPEN))
    }

    @Test
    fun `full bank saves the whole box and a reconstructed player cannot reroll or claim it twice`() {
        val f = Fixture()
        val random = CountingRandom(highest = true)
        val closed = InvObj(FarmingTitheHerbBoxes.CLOSED)
        f.player.inv[0] = closed
        f.fill()
        f.set("varbit.bank_capacity", 1)
        f.bank[0] = InvObj("obj.coins", 100)
        f.set(BankTab.Main.sizeVarBit, 1)
        assertFalse(f.bankBox(closed, random))
        val saved = checkNotNull(f.player.inv[0])
        assertEquals(7, FarmingTitheHerbBoxes.remaining(saved))
        assertEquals(7, random.calls)
        assertEquals(100, f.bank.count("obj.coins"))
        assertFalse(f.bankBox(saved, random))
        assertEquals(saved, f.player.inv[0])
        assertEquals(7, random.calls)

        val restored = Fixture()
        restored.player.inv[0] = InvObj(saved)
        restored.fill()
        val afterRelog = CountingRandom()
        assertTrue(restored.bankBox(checkNotNull(restored.player.inv[0]), afterRelog))
        assertEquals(7, restored.bank.count("obj.unidentified_dwarf_weed"))
        assertEquals(0, restored.count("obj.unidentified_dwarf_weed"))
        assertEquals(27, restored.count(SPADE))
        assertEquals(null, restored.player.inv[0])
        assertEquals(0, afterRelog.calls)
        assertFalse(restored.bankBox(saved, afterRelog))
        assertEquals(7, restored.bank.count("obj.unidentified_dwarf_weed"))
    }

    @Test
    fun `bank stack overflow commits only delivered herbs and take one consumes the saved remainder`() {
        val f = Fixture()
        val random = CountingRandom()
        val closed = InvObj(FarmingTitheHerbBoxes.CLOSED)
        f.player.inv[0] = closed
        f.bank[0] = InvObj("obj.unidentified_guam", Int.MAX_VALUE - 2)
        f.set(BankTab.Main.sizeVarBit, 1)
        assertFalse(f.bankBox(closed, random))
        val pending = checkNotNull(f.player.inv[0])
        assertEquals(Int.MAX_VALUE, f.bank.count("obj.unidentified_guam"))
        assertEquals(5, FarmingTitheHerbBoxes.remaining(pending))
        assertEquals(7, random.calls)
        assertFalse(f.bankBox(pending, random))
        assertEquals(pending, f.player.inv[0])
        assertTrue(FarmingTitheHerbBoxes.take(f.player, 0, pending, random))
        assertEquals(1, f.count("obj.unidentified_guam"))
        assertEquals(4, FarmingTitheHerbBoxes.remaining(checkNotNull(f.player.inv[0])))
        assertEquals(7, random.calls)
        assertEquals(
            Int.MAX_VALUE.toLong() - 2 + 7,
            f.bank.count("obj.unidentified_guam").toLong() + f.count("obj.unidentified_guam") +
                FarmingTitheHerbBoxes.remaining(checkNotNull(f.player.inv[0])),
        )
    }

    @Test
    fun `bank all replaces a placeholder in a full bank without growing a tab`() {
        val f = Fixture()
        val herb = checkNotNull(ServerCacheManager.getItem("obj.unidentified_guam".asRSCM()))
        val closed = InvObj(FarmingTitheHerbBoxes.CLOSED)
        f.player.inv[0] = closed
        f.set("varbit.bank_capacity", 1)
        f.set(BankTab.Main.sizeVarBit, 1)
        f.bank[0] = InvObj(placeholder(herb))
        assertTrue(f.bankBox(closed, CountingRandom()))
        assertEquals(InvObj(herb, 7), f.bank[0])
        assertEquals(1, f.player.vars[BankTab.Main.sizeVarBit])
        assertEquals(null, f.player.inv[0])
    }

    @Test
    fun `new bank reward joins the selected tab and preserves following tab items`() {
        val f = Fixture()
        val closed = InvObj(FarmingTitheHerbBoxes.CLOSED)
        f.player.inv[0] = closed
        f.bank[0] = InvObj("obj.coins", 23)
        f.set(BankTab.Main.sizeVarBit, 1)
        f.set("varbit.bank_currenttab", BankTab.Tab1.varValue)
        assertTrue(f.bankBox(closed, CountingRandom()))
        assertEquals(InvObj("obj.unidentified_guam", 7), f.bank[0])
        assertEquals(InvObj("obj.coins", 23), f.bank[1])
        assertEquals(1, f.player.vars[BankTab.Tab1.sizeVarBit])
        assertEquals(1, f.player.vars[BankTab.Main.sizeVarBit])
    }

    @Test
    fun `UIM and stale item operations cannot consume or roll a box`() {
        val f = Fixture()
        val random = CountingRandom()
        val closed = InvObj(FarmingTitheHerbBoxes.CLOSED)
        f.player.inv[0] = closed
        f.player.gamemode = PlayerGamemode.ULTIMATE_IRONMAN
        assertFalse(f.bankBox(closed, random))
        assertEquals(closed, f.player.inv[0])
        assertEquals(0, random.calls)
        f.player.gamemode = PlayerGamemode.NORMAL
        f.player.inv[0] = InvObj(SPADE)
        assertFalse(f.bankBox(closed, random))
        assertEquals(InvObj(SPADE), f.player.inv[0])
        assertEquals(0, random.calls)
        assertEquals(0, f.bank.count("obj.unidentified_guam"))
    }

    @Test
    fun `fractional outfit XP is independent of how fruit are split into deposits`() {
        val together = Fixture()
        val split = Fixture()
        val crop = FarmingTitheData.crops[2]
        together.player.inv[0] = InvObj(crop.fruit, 100)
        assertTrue(depositTitheFruit(together.player, crop, 1.025, 1.0))
        repeat(100) {
            split.player.inv[0] = InvObj(crop.fruit)
            assertTrue(depositTitheFruit(split.player, crop, 1.025, 1.0))
        }
        assertEquals(together.xp(), split.xp())
        assertEquals(together.player.vars[FarmingTithe.POINTS], split.player.vars[FarmingTithe.POINTS])
        assertEquals(0, together.count(crop.fruit.internalName))
        assertEquals(0, split.count(crop.fruit.internalName))
    }

    @Test
    fun `native item use waters with exactly the clicked can slot and not an earlier identical can`() {
        val f = PlantFixture()
        f.plantAndStock()
        val dry = f.visible().id
        val before = f.snapshot()
        f.useCan("obj.watering_can_8", 4)
        assertNotEquals(dry, f.visible().id)
        assertEquals(before.toMutableList().also { it[4] = InvObj("obj.watering_can_7") }, f.snapshot())
        assertEquals(f.xp0, f.player.statMap.getFineXP(STAT_FARMING))
        assertEquals(f.points0, f.player.vars[FarmingTithe.POINTS])
    }

    @Test
    fun `native item use with a stale vanished or replaced can slot spends no other can and keeps the plot dry`() {
        val f = PlantFixture()
        f.plantAndStock()
        val dry = f.visible().id
        for (stale in listOf(null, InvObj(SPADE), InvObj("obj.watering_can_7"))) {
            f.player.inv[4] = stale
            val before = f.snapshot()
            f.useCan("obj.watering_can_8", 4)
            assertEquals(before, f.snapshot())
            assertEquals(dry, f.visible().id)
            assertEquals(f.xp0, f.player.statMap.getFineXP(STAT_FARMING))
            assertEquals(f.points0, f.player.vars[FarmingTithe.POINTS])
        }
    }

    @Test
    fun `native left click still auto selects a usable can past empty and irrelevant entries`() {
        val f = PlantFixture()
        f.plantAndStock()
        f.player.inv[3] = InvObj("obj.watering_can_0")
        f.player.inv[4] = InvObj(SPADE)
        f.player.inv[6] = InvObj("obj.watering_can_5")
        f.player.inv[7] = InvObj("obj.watering_can_8")
        val dry = f.visible().id
        val before = f.snapshot()
        f.leftClick()
        assertNotEquals(dry, f.visible().id)
        assertEquals(before.toMutableList().also { it[6] = InvObj("obj.watering_can_4") }, f.snapshot())
        assertEquals(f.xp0, f.player.statMap.getFineXP(STAT_FARMING))
        assertEquals(f.points0, f.player.vars[FarmingTithe.POINTS])
    }

    private class CountingRandom(private val highest: Boolean = false) : GameRandom {
        var calls = 0
        override fun of(maxExclusive: Int): Int { calls++; return if (highest) maxExclusive - 1 else 0 }
        override fun of(minInclusive: Int, maxInclusive: Int): Int = minInclusive
        override fun randomDouble(): Double = 0.0
    }
    private class PlantFixture {
        val events = EventBus()
        private val clock = MapClock().apply { cycle = 100 }
        private val collision = CollisionFlagMap()
        private val updates = ZoneUpdateMap()
        private val zones = LocZoneStorage()
        private val normal = LocRegistryNormal(updates, collision, zones)
        private val npcs = NpcList()
        private val npcRegistry = NpcRegistry(npcs, collision, events)
        private val regions = RegionRegistry(
            RegionListSmall(), RegionListLarge(), RegionListWorldEntity(), normal, collision, zones,
            npcRegistry, ControllerRegistry(clock, ControllerList()), ZonePlayerActivityBitSet(),
        )
        val locs = LocRepository(clock,
            LocRegistry(zones, normal, LocRegistryRegion(updates, collision, zones, regions)), regions)
        val instances = InstanceManager(RegionRepository(regions),
            NpcRepository(clock, npcRegistry, npcs), PlayerList(), events, InstanceAreaResolver(),
            clock, collision)
        val tithe = FarmingTithe(instances, locs, clock, XpModifiers(emptySet()),
            FarmerWornSetXpModifiers(), FarmingEquipment(DefaultGameRandom(1)), events)
        val player = Fixture().player
        val crop = FarmingTitheData.crops.single { it.tier == 3 }
        val planted = mutableListOf<String>()
        val plot: BoundLocInfo

        init {
            val source = CoordGrid(1810, 3502)
            val base = MapSquareKey.from(source).toCoords(0)
            for (level in 0..3) for (x in 0 until 64 step 8) for (z in 0 until 64 step 8) {
                collision.allocateIfAbsent(base.x + x, base.z + z, level)
            }
            val empty = LocEntity(FarmingTithe.EMPTY.asRSCM(RSCMType.LOC), 10, 0)
            zones.mapLocs[ZoneKey.from(source), LocZoneKey(ZoneGrid.from(source), 2)] = empty
            for (sack in FarmingTitheData.row.sacks) {
                zones.mapLocs[ZoneKey.from(sack), LocZoneKey(ZoneGrid.from(sack), 2)] =
                    LocEntity("loc.tithe_sack_of_fruit_empty".asRSCM(RSCMType.LOC), 10, 0)
            }
            player.inv[0] = InvObj(crop.seed, 2)
            player.inv[1] = InvObj(SPADE)
            player.inv[2] = InvObj(DIBBER)
            check(tithe.enter(player))
            val session = checkNotNull(instances.sessionForPlayer(player))
            val coords = checkNotNull(instances.resolveCoord(session, source))
            plot = BoundLocInfo(coords, empty, 2, 1, 1, 0)
            events.subscribeUnbound(FarmingTithePlantEvent::class.java) { planted += seed }
        }

        init {
            with(FarmingTitheScript(tithe)) { ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup() }
        }

        private var initialXp = 0
        private var initialPoints = 0
        val xp0 get() = initialXp
        val points0 get() = initialPoints

        /** Plants through production, then stocks two identical full cans at slots 3 and 4. */
        fun plantAndStock() {
            check(tithe.plant(player, plot, crop, 0))
            player.inv[3] = InvObj("obj.watering_can_8")
            player.inv[4] = InvObj("obj.watering_can_8")
            initialXp = player.statMap.getFineXP(STAT_FARMING)
            initialPoints = player.vars[FarmingTithe.POINTS]
        }

        fun snapshot(): List<InvObj?> = player.inv.indices.map { player.inv[it] }
        fun visible(): LocInfo = checkNotNull(locs.findExact(plot.coords, plot.shape))

        fun useCan(can: String, slot: Int) {
            val loc = BoundLocInfo(plot.coords, visible().entity, plot.layer, 1, 1, 0)
            val type = checkNotNull(ServerCacheManager.getObject(loc.id))
            val obj = checkNotNull(ServerCacheManager.getItem(can.asRSCM()))
            publish(LocUDefaultEvents.OpType(loc, loc, type, obj, slot))
        }

        fun leftClick() {
            val loc = BoundLocInfo(plot.coords, visible().entity, plot.layer, 1, 1, 0)
            publish(LocEvents.Op1(loc, loc, checkNotNull(ServerCacheManager.getObject(loc.id))))
        }

        private fun publish(event: SuspendEvent<ProtectedAccess>) {
            val coroutine = GameCoroutine("tithe-watering-test")
            player.activeCoroutine = coroutine
            val access = ProtectedAccess(player, coroutine, ProtectedAccessContextFactory.empty())
            var outcome: Result<Unit>? = null
            val action: suspend () -> Unit = { assertTrue(events.publish(access, event)) }
            action.startCoroutine(object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) { outcome = result }
            })
            try { checkNotNull(outcome).getOrThrow() } finally { player.activeCoroutine = null }
        }
    }

    private class Fixture {
        private val events = EventBus()
        private val banking = BankInvScript(events, WornBonuses(), WeaponSpeeds(AttackStyles()))

        @OptIn(InternalApi::class)
        val player = Player().apply {
            client = SilentClient()
            uuid = 811L
            slotId = 1
            assignUid()
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            statMap.setXP(STAT_FARMING, 13_034_431)
            statMap.setBaseLevel(STAT_FARMING, 99)
            statMap.setCurrentLevel(STAT_FARMING, 99)
            statMap.setBaseLevel("stat.prayer", 50)
            statMap.setCurrentLevel("stat.prayer", 0)
            statMap.setBaseLevel("stat.cooking", 65)
            statMap.setCurrentLevel("stat.cooking", 65)
        }
        private val initialXp = player.statMap.getFineXP(STAT_FARMING)
        val bank = player.invMap.getOrPut("inv.bank")
        init {
            set(FarmingTithe.SESSION, 1)
            set("varbit.bank_capacity", bank.size)
        }
        fun bankBox(expected: InvObj, random: GameRandom): Boolean {
            val coroutine = GameCoroutine("herb-box-bank-test")
            player.activeCoroutine = coroutine
            val access = ProtectedAccess(player, coroutine, ProtectedAccessContextFactory.empty())
            return try {
                FarmingTitheHerbBoxes.bankAll(access, banking, 0, expected, random)
            } finally {
                player.activeCoroutine = null
            }
        }
        fun set(variable: String, value: Int) = VarPlayerIntMapSetter.set(player, variable, value)
        fun count(obj: String): Int = player.inv.physicalCount(obj)
        fun xp(): Int = player.statMap.getFineXP(STAT_FARMING) - initialXp
        fun harvest(harvest: GrapeHarvest): Boolean {
            val result = FarmingGrapes.prepareHarvest(player, harvest)
            if (!result.success) return false
            result.commitAll()
            return true
        }
        fun fill() { for (slot in player.inv.indices) if (player.inv[slot] == null) player.inv[slot] = InvObj(SPADE) }
    }

    companion object {
        private val RESTORED = mutableListOf<() -> Unit>()

        @JvmStatic @BeforeAll fun cache() {
            loadFarmingTestCache()
            installFarmingTestInventory(RESTORED)
        }

        @JvmStatic @AfterAll fun restore() {
            RESTORED.asReversed().forEach { it() }
            RESTORED.clear()
        }
    }
}
