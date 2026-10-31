package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ObjectServerType
import io.mockk.every
import io.mockk.mockk
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.interact.LocInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.quest.manager.QuestRequirementResolver
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@ResourceLock("QuestRequirements")
class FarmingInventoryTest {
    @Test
    fun `saved crop indices retain their plants when new rows are added`() {
        val seeds = listOf(
            "potato", "onion", "cabbage", "tomato", "sweetcorn", "strawberry", "watermelon",
            "marigold", "rosemary", "nasturtium", "woad", "limpwurt",
            "guam", "marrentill", "tarromin", "harralander", "ranarr", "toadflax", "irit",
            "avantoe", "kwuarm", "snapdragon", "cadantine", "lantadyme", "dwarf_weed", "torstol",
            "snape_grass", "huasca", "white_lily",
        )
        for ((index, seed) in seeds.withIndex()) {
            val saved = PatchState.unpack((3 shl 28) or (index + 1))
            assertEquals("obj.${seed}_seed", saved.crop?.seed)
        }
    }

    @Test
    fun `huasca requires level 65 and pays planting XP only on its final pick`() {
        val f = Fixture("huasca")
        f.state(PatchState().empty())
        f.give(DIBBER)
        f.give(f.crop.seed)
        f.player.statMap.setCurrentLevel(STAT_FARMING, 64)
        f.use(f.crop.seed)
        assertEquals(1, f.count(f.crop.seed))
        assertEquals(0, f.state().cropIndex)
        f.player.statMap.setCurrentLevel(STAT_FARMING, 65)
        f.use(f.crop.seed)
        assertEquals(0, f.count(f.crop.seed))
        assertEquals(28, f.state().cropIndex)
        assertEquals(0, f.xp())
        assertEquals(1, f.plants.size)
        f.grown(lives = 3)
        f.op()
        assertEquals(3, f.count("obj.unidentified_huasca"))
        assertEquals(4165, f.xp())
        assertEquals(PatchState().empty(), f.state())
        f.op()
        assertEquals(3, f.count("obj.unidentified_huasca"))
        assertEquals(4165, f.xp())
        assertEquals(listOf(false, false, true), f.harvests.map(FarmingHarvestEvent::final))
    }

    @Test
    fun `full inventory preserves the woad batch and space allows one batch`() {
        val f = Fixture("woad")
        val pending = f.grown()
        f.fill()
        f.op()
        assertEquals(pending, f.state())
        assertEquals(0, f.count(f.crop.produce))
        assertEquals(0, f.xp())
        assertTrue(f.harvests.isEmpty())
        f.player.inv[0] = null
        f.op()
        assertEquals(3, f.count(f.crop.produce))
        assertEquals(1360, f.xp())
        assertEquals(pending.empty(), f.state())
        f.op()
        assertEquals(3, f.count(f.crop.produce))
        assertEquals(1360, f.xp())
        assertEquals(1, f.harvests.size)
        assertEquals(3, f.harvests.single().count)
    }

    @Test
    fun `limpwurt requires capacity for its entire rolled batch and XP is awarded once`() {
        val f = Fixture("limpwurt", rolls = listOf(98, 98))
        val pending = f.grown()
        f.fill()
        repeat(11) { f.player.inv[it] = null }
        f.op()
        assertEquals(pending, f.state())
        assertEquals(0, f.count(f.crop.produce))
        assertEquals(0, f.xp())
        f.player.inv[11] = null
        f.op()
        assertEquals(12, f.count(f.crop.produce))
        assertEquals(1415, f.xp())
        assertEquals(pending.empty(), f.state())
        f.op()
        assertEquals(12, f.count(f.crop.produce))
        assertEquals(1415, f.xp())
        assertEquals(listOf(99, 99), f.random.bounds)
    }

    @Test
    fun `failed herb award spends neither life nor XP nor save roll nor event`() {
        val f = Fixture("guam")
        val pending = f.grown(lives = 2)
        f.fill()
        f.op()
        assertEquals(pending, f.state())
        assertEquals(0, f.xp())
        assertTrue(f.random.bounds.isEmpty())
        assertEquals(0, f.count(f.crop.produce))
        assertTrue(f.harvests.isEmpty())
    }

    @Test
    fun `harvest rechecks the patch and capacity after its delay`() {
        for (cleared in listOf(false, true)) {
            val f = Fixture("guam")
            val pending = f.grown()
            f.startOp()
            if (cleared) f.state(pending.empty()) else f.fill()
            val current = f.state()
            f.finish()
            assertEquals(current, f.state())
            assertEquals(0, f.count(f.crop.produce))
            assertEquals(0, f.xp())
            assertTrue(f.random.bounds.isEmpty())
        }
    }

    @Test
    fun `final harvest event fires once after the produce and state are stored`() {
        val f = Fixture("guam", rolls = listOf(255))
        f.grown(lives = 1)
        f.onHarvest = { event ->
            assertTrue(event.final)
            assertEquals(1, f.count(f.crop.produce))
            assertEquals(PatchState().empty(), f.state())
            assertEquals(235, f.xp())
        }
        f.op()
        assertEquals(1, f.harvests.size)
        f.op()
        assertEquals(1, f.harvests.size)
    }

    @Test
    fun `saved herb life persists after inventory fills and a later pick spends it`() {
        val f = Fixture("guam", rolls = listOf(0, 255))
        val pending = f.grown(lives = 1)
        f.fill()
        f.player.inv[0] = null
        f.op()
        assertEquals(1, f.count(f.crop.produce))
        assertEquals(pending, f.state())
        assertEquals(125, f.xp())
        f.player.inv[1] = null
        f.op()
        assertEquals(2, f.count(f.crop.produce))
        assertEquals(pending.empty(), f.state())
        assertEquals(360, f.xp())
        assertEquals(listOf(256, 256), f.random.bounds)
    }

    @Test
    fun `planting XP is deferred through a partial harvest until the final successful pick`() {
        val f = Fixture("guam", rolls = listOf(255, 255))
        f.state(PatchState().empty())
        f.give(f.crop.seed)
        f.give(DIBBER)
        f.use(f.crop.seed)
        assertEquals(0, f.xp())
        assertEquals(0, f.count(f.crop.seed))
        val mature = f.state().copy(stage = f.crop.stages, produce = 2)
        f.state(mature)
        f.fill()
        f.player.inv[0] = null
        f.op()
        assertEquals(1, f.count(f.crop.produce))
        assertEquals(mature.copy(produce = 1), f.state())
        assertEquals(125, f.xp())
        f.player.inv[1] = null
        f.op()
        assertEquals(2, f.count(f.crop.produce))
        assertEquals(mature.empty(), f.state())
        assertEquals(360, f.xp())
        f.op()
        assertEquals(360, f.xp())
        assertEquals(2, f.count(f.crop.produce))
    }

    @Test
    fun `watering replaces a can atomically even with a full inventory`() {
        val f = Fixture("potato")
        f.planted()
        f.fill()
        f.player.inv[0] = InvObj(CAN_8)
        f.use(CAN_8)
        assertEquals(28, f.player.inv.occupiedSpace())
        assertEquals(0, f.count(CAN_8))
        assertEquals(1, f.count(CAN_7))
        assertTrue(f.state().watered)
        assertEquals(10, f.xp())
        f.use(CAN_7)
        assertEquals(1, f.count(CAN_7))
        assertEquals(10, f.xp())
    }

    @Test
    fun `watering uses the current stage and refuses a crop that died during the delay`() {
        val f = Fixture("potato")
        val planted = f.planted()
        f.give(CAN_8)
        f.startUse(CAN_8)
        val advanced = planted.copy(stage = 1)
        f.state(advanced)
        f.finish()
        assertEquals(advanced.copy(watered = true), f.state())
        f.state(advanced)
        f.startUse(CAN_7)
        f.state(advanced.copy(health = Health.DEAD))
        f.finish()
        assertEquals(1, f.count(CAN_7))
        assertEquals(Health.DEAD, f.state().health)
    }

    @Test
    fun `watering consumes exactly the selected can when an identical can precedes it`() {
        val f = Fixture("potato")
        f.planted()
        f.player.inv[0] = InvObj(CAN_8)
        f.player.inv[5] = InvObj(CAN_8)
        f.startUse(CAN_8, 5)
        f.finish()
        assertEquals(CAN_8.asRSCM(RSCMType.OBJ), f.player.inv[0]?.id)
        assertEquals(CAN_7.asRSCM(RSCMType.OBJ), f.player.inv[5]?.id)
        assertEquals(1, f.count(CAN_8))
        assertEquals(1, f.count(CAN_7))
        assertTrue(f.state().watered)
        assertEquals(10, f.xp())
    }

    @Test
    fun `a selected can removed during the delay is not replaced by an identical can`() {
        val f = Fixture("potato")
        val planted = f.planted()
        f.player.inv[0] = InvObj(CAN_8)
        f.player.inv[5] = InvObj(CAN_8)
        f.startUse(CAN_8, 5)
        f.player.inv[5] = null
        f.finish()
        assertEquals(CAN_8.asRSCM(RSCMType.OBJ), f.player.inv[0]?.id)
        assertEquals(null, f.player.inv[5])
        assertEquals(1, f.count(CAN_8))
        assertEquals(0, f.count(CAN_7))
        assertEquals(planted, f.state())
        assertEquals(0, f.xp())
    }

    @Test
    fun `a selected can slot changed during the delay does not fall back to another can`() {
        for (replacement in listOf(CAN_7, SPADE)) {
            val f = Fixture("potato")
            val planted = f.planted()
            f.player.inv[0] = InvObj(CAN_8)
            f.player.inv[5] = InvObj(CAN_8)
            f.startUse(CAN_8, 5)
            f.player.inv[5] = InvObj(replacement)
            f.finish()
            assertEquals(CAN_8.asRSCM(RSCMType.OBJ), f.player.inv[0]?.id)
            assertEquals(replacement.asRSCM(RSCMType.OBJ), f.player.inv[5]?.id)
            assertEquals(1, f.count(CAN_8))
            assertEquals(planted, f.state())
            assertEquals(0, f.xp())
        }
    }

    @Test
    fun `cure cannot resurrect a crop that dies during its delay`() {
        val f = Fixture("guam")
        val diseased = f.planted().copy(stage = 1, health = Health.DISEASED)
        f.state(diseased)
        f.give(PLANT_CURE)
        f.startUse(PLANT_CURE)
        val dead = diseased.copy(health = Health.DEAD)
        f.state(dead)
        f.finish()
        assertEquals(dead, f.state())
        assertEquals(1, f.count(PLANT_CURE))
        assertEquals(0, f.xp())
    }

    @Test
    fun `cure keeps the current stage and turns exactly one source into a vial`() {
        val f = Fixture("guam")
        val diseased = f.planted().copy(stage = 1, health = Health.DISEASED)
        f.state(diseased)
        f.give(PLANT_CURE, 2)
        f.startUse(PLANT_CURE)
        f.finish()
        assertEquals(diseased.copy(health = Health.HEALTHY), f.state())
        assertEquals(1, f.count(PLANT_CURE))
        assertEquals(1, f.count("obj.vial_empty"))
        f.use(PLANT_CURE)
        assertEquals(1, f.count(PLANT_CURE))
    }

    @Test
    fun `vanished cure does not cure the crop`() {
        val f = Fixture("guam")
        val diseased = f.planted().copy(stage = 1, health = Health.DISEASED)
        f.state(diseased)
        f.give(PLANT_CURE)
        f.startUse(PLANT_CURE)
        f.player.inv[0] = null
        f.finish()
        assertEquals(diseased, f.state())
        assertTrue(f.player.inv.isEmpty())
    }

    @Test
    fun `post plant compost preserves the advanced crop and exchanges one bucket once`() {
        for (kind in listOf(Compost.NORMAL, Compost.SUPER, Compost.ULTRA)) {
            val f = Fixture("potato")
            val compost = FarmingComposts.get(kind)
            val obj = checkNotNull(compost.obj)
            val planted = f.planted()
            f.fill()
            f.player.inv[0] = InvObj(obj)
            f.player.inv[1] = InvObj(obj)
            f.startUse(obj)
            val advanced = planted.copy(stage = 1)
            f.state(advanced)
            f.finish()
            assertEquals(advanced.copy(compost = kind), f.state())
            assertEquals(1, f.count(obj))
            assertEquals(1, f.count("obj.bucket_empty"))
            assertEquals(28, f.player.inv.occupiedSpace())
            assertEquals((compost.xp * 10).toInt(), f.xp())
            f.use(obj)
            assertEquals(1, f.count(obj))
            assertEquals(1, f.count("obj.bucket_empty"))
            assertEquals((compost.xp * 10).toInt(), f.xp())
        }
    }

    @Test
    fun `vanished compost cannot create an empty bucket`() {
        val f = Fixture("potato")
        val planted = f.planted()
        val obj = checkNotNull(FarmingComposts.get(Compost.NORMAL).obj)
        f.give(obj)
        f.startUse(obj)
        f.player.inv[0] = null
        f.finish()
        assertEquals(planted, f.state())
        assertEquals(0, f.count("obj.bucket_empty"))
        assertEquals(0, f.xp())
    }

    @Test
    fun `compost cannot treat a crop that matures or dies during the delay`() {
        for (dead in listOf(false, true)) {
            val f = Fixture("potato")
            val planted = f.planted()
            val obj = checkNotNull(FarmingComposts.get(Compost.NORMAL).obj)
            f.give(obj)
            f.startUse(obj)
            val changed =
                if (dead) planted.copy(health = Health.DEAD) else planted.copy(stage = f.crop.stages, produce = 3)
            f.state(changed)
            f.finish()
            assertEquals(changed, f.state())
            assertEquals(1, f.count(obj))
            assertEquals(0, f.count("obj.bucket_empty"))
            assertEquals(0, f.xp())
        }
    }

    @Test
    fun `empty compost treatment survives planting without losing its bucket`() {
        val f = Fixture("potato")
        val obj = checkNotNull(FarmingComposts.get(Compost.NORMAL).obj)
        f.state(PatchState().empty())
        f.give(obj)
        f.use(obj)
        assertEquals(PatchState().empty().copy(compost = Compost.NORMAL), f.state())
        assertEquals(1, f.count("obj.bucket_empty"))
        f.give(f.crop.seed, f.crop.seedsPerPlant)
        f.give(DIBBER)
        f.use(f.crop.seed)
        assertEquals(Compost.NORMAL, f.state().compost)
        assertEquals(1, f.count("obj.bucket_empty"))
        assertEquals(0, f.count(obj))
        assertEquals(0, f.count(f.crop.seed))
    }

    @Test
    fun `planting consumes a strict seed batch once and retains current compost`() {
        val f = Fixture("potato")
        f.state(PatchState().empty())
        f.give(f.crop.seed, 6)
        f.give(DIBBER)
        f.startUse(f.crop.seed)
        f.state(f.state().copy(compost = Compost.SUPER))
        f.finish()
        assertEquals(3, f.count(f.crop.seed))
        assertEquals(Compost.SUPER, f.state().compost)
        assertEquals(f.crop.index, f.state().cropIndex)
        assertEquals(0, f.xp())
        f.use(f.crop.seed)
        assertEquals(3, f.count(f.crop.seed))
        assertEquals(0, f.xp())
        assertEquals(1, f.plants.size)
    }

    @Test
    fun `vanished seeds or new weeds during planting consume nothing`() {
        for (vanish in listOf(false, true)) {
            val f = Fixture("potato")
            f.state(PatchState().empty())
            f.give(f.crop.seed, 3)
            f.give(DIBBER)
            f.startUse(f.crop.seed)
            if (vanish) f.player.inv[0] = null else f.state(f.state().copy(weeds = 2))
            val current = f.state()
            f.finish()
            assertEquals(current, f.state())
            assertEquals(if (vanish) 0 else 3, f.count(f.crop.seed))
            assertEquals(0, f.xp())
            assertTrue(f.plants.isEmpty())
        }
    }

    @Test
    fun `inspect never consumes cures or harvests produce or clears dead crops`() {
        val f = Fixture("guam")
        f.give(PLANT_CURE)
        f.give(SPADE)
        val grown = f.grown()
        f.inspect()
        assertEquals(grown, f.state())
        assertEquals(0, f.count(f.crop.produce))
        for (health in listOf(Health.DISEASED, Health.DEAD)) {
            val current = f.planted().copy(stage = 1, health = health)
            f.state(current)
            f.inspect()
            assertEquals(current, f.state())
        }
        assertEquals(1, f.count(PLANT_CURE))
        assertEquals(0, f.xp())
    }

    @Test
    fun `dead clear and spade use leave weeded soil and pay planting XP once`() {
        for (item in listOf(false, true)) {
            val f = Fixture("potato")
            val dead = f.planted().copy(stage = 1, health = Health.DEAD, compost = Compost.ULTRA)
            f.state(dead)
            f.give(SPADE)
            if (item) f.use(SPADE) else f.op()
            assertEquals(dead.empty(), f.state())
            assertEquals(1, f.count(SPADE))
            assertEquals(80, f.xp())
            if (item) f.use(SPADE) else f.op()
            assertEquals(dead.empty(), f.state())
            assertEquals(80, f.xp())
            assertEquals(1, f.clears.size)
            assertTrue(f.clears.single().dead)
        }
    }

    @Test
    fun `final flower harvest and dead clearing allow immediate planting without raking`() {
        for (dead in listOf(false, true)) {
            val f = Fixture("marigold")
            f.state(PatchState().empty())
            f.give(DIBBER)
            f.give(SPADE)
            f.give(f.crop.seed, 2)
            f.use(f.crop.seed)
            assertEquals(1, f.count(f.crop.seed))
            assertEquals(0, f.xp())
            if (dead) {
                f.state(f.state().copy(stage = 1, health = Health.DEAD))
            } else {
                f.grown()
            }
            f.op()
            val completionXp = if (dead) 85 else 555
            assertEquals(completionXp, f.xp())
            assertEquals(if (dead) 0 else 1, f.count(f.crop.produce))
            f.use(f.crop.seed)
            assertEquals(0, f.count(f.crop.seed))
            assertEquals(f.crop.index, f.state().cropIndex)
            assertEquals(0, f.state().stage)
            assertEquals(completionXp, f.xp())
            assertEquals(2, f.plants.size)
        }
    }

    @Test
    fun `dead clearing defers planting XP while the spade is missing and awards it once`() {
        val f = Fixture("potato")
        f.state(PatchState().empty())
        f.give(f.crop.seed, f.crop.seedsPerPlant)
        f.give(DIBBER)
        f.use(f.crop.seed)
        assertEquals(0, f.xp())
        val dead = f.state().copy(stage = 1, health = Health.DEAD)
        f.state(dead)
        f.give(SPADE)
        f.startOp()
        f.player.inv[f.player.inv.indexOfFirst { it?.id == SPADE.asRSCM() }] = null
        f.finish()
        assertEquals(dead, f.state())
        assertEquals(0, f.xp())
        f.give(SPADE)
        f.op()
        assertEquals(dead.empty(), f.state())
        assertEquals(80, f.xp())
        f.op()
        assertEquals(dead.empty(), f.state())
        assertEquals(80, f.xp())
    }

    @Test
    fun `a spade digs up a healthy growing crop without XP`() {
        val f = Fixture("potato")
        val growing = f.planted().copy(stage = 2)
        f.state(growing)
        f.give(SPADE)
        f.use(SPADE)
        assertEquals(growing.empty(), f.state())
        assertEquals(0, f.xp())
        assertEquals(false, f.clears.single().dead)
    }

    @Test
    fun `raking awards weeds only with space and rechecks the rake after delay`() {
        val f = Fixture("potato")
        f.state(PatchState())
        f.fill()
        f.player.inv[0] = InvObj(RAKE)
        f.op()
        assertEquals(0, f.state().weeds)
        assertEquals(0, f.xp())
        f.player.inv[1] = null
        f.startOp()
        f.player.inv[0] = null
        f.finish()
        assertEquals(0, f.state().weeds)
        assertEquals(0, f.count(WEEDS))
        assertEquals(0, f.xp())
        f.player.inv[0] = InvObj(RAKE)
        f.op()
        assertEquals(1, f.count(WEEDS))
        assertEquals(1, f.state().weeds)
        assertEquals(40, f.xp())
    }

    @Test
    fun `offline refresh publishes only its final state after all patch writes`() {
        val f = Fixture("guam")
        val initial = f.planted()
        VarPlayerIntMapSetter.set(f.player, "varp.farming_clock", 1000)
        VarPlayerIntMapSetter.set(f.player, "varp.farming_growth_offset", 32)
        f.onRefresh = { event ->
            for (i in FarmingPatches.all.indices) {
                assertEquals(event.after[i], PatchState.unpack(f.player.vars[FarmingPatches.all[i].varp]))
            }
        }
        f.service.refresh(f.player, 1080 * FarmingClock.MINUTE_MILLIS)
        val index = FarmingPatches.all.indexOf(f.patch)
        val event = f.refreshes.single()
        assertEquals(initial, event.before[index])
        assertEquals(initial.copy(stage = 4).matured(f.crop), event.after[index])
        assertTrue(f.patchChanges.isEmpty())
        f.service.refresh(f.player, 1080 * FarmingClock.MINUTE_MILLIS)
        assertEquals(1, f.refreshes.size)
    }

    private class Fixture(
        name: String, rolls: List<Int> = emptyList(),
    ) {
        val crop = FarmingCrops.all.first { it.name == name }
        val patch = FarmingPatches.all.first { it.kind == crop.kind }
        val random = FixedRandom(rolls)
        private val events = EventBus()
        val service = FarmingService(XpModifiers(emptySet()), random, events, FarmingSiteAccess(quests))
        private val script = FarmingScript(service, FarmingCompostService(service))
        private val coroutine = GameCoroutine("farming-inventory-test")
        private var result: Result<Unit>? = null
        private var retriggered = false
        private lateinit var lastStep: suspend ProtectedAccess.() -> Unit
        private val locInteractions = mockk<LocInteractions>().also {
            every { it.interact(any(), any(), any(), any()) } answers { retriggered = true }
        }
        private val context = ProtectedAccessContextFactory.empty().copy(
            getEventBus = { events }, getAlignment = { TextAlignment() },
            getRandom = { random }, getLocInteractions = { locInteractions },
        )
        val plants = mutableListOf<FarmingPlantEvent>()
        val harvests = mutableListOf<FarmingHarvestEvent>()
        val clears = mutableListOf<FarmingClearEvent>()
        val refreshes = mutableListOf<FarmingRefreshEvent>()
        val patchChanges = mutableListOf<FarmingPatchChangeEvent>()
        var onPatchChange: (FarmingPatchChangeEvent) -> Unit = {}
        var onRefresh: (FarmingRefreshEvent) -> Unit = {}
        var onHarvest: (FarmingHarvestEvent) -> Unit = {}

        @OptIn(InternalApi::class)
        val player = Player().apply {
            client = SilentClient()
            uuid = 798L
            slotId = 1
            assignUid()
            coords = patch.coords.first()
            currentMapClock = 100
            processedMapClock = 100
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
            statMap.setXP(STAT_FARMING, 13_034_431)
            statMap.setBaseLevel(STAT_FARMING, 99)
            statMap.setCurrentLevel(STAT_FARMING, 99)
        }

        private val initialXp = player.statMap.getFineXP(STAT_FARMING)

        init {
            // Pins the farming clock in the future so growth never replays mid-test.
            VarPlayerIntMapSetter.set(player, "varp.farming_clock", Int.MAX_VALUE)
            events.subscribeUnbound(FarmingPlantEvent::class.java) { plants += this }
            events.subscribeUnbound(FarmingHarvestEvent::class.java) { onHarvest(this); harvests += this }
            events.subscribeUnbound(FarmingClearEvent::class.java) { clears += this }
            events.subscribeUnbound(FarmingRefreshEvent::class.java) { onRefresh(this); refreshes += this }
            events.subscribeUnbound(FarmingPatchChangeEvent::class.java) { onPatchChange(this); patchChanges += this }
            with(script) { ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup() }
        }

        fun xp(): Int = player.statMap.getFineXP(STAT_FARMING) - initialXp
        fun count(obj: String?): Int = player.inv.count(checkNotNull(obj))
        fun state(): PatchState = PatchState.unpack(player.vars[patch.varp])
        fun state(state: PatchState) = VarPlayerIntMapSetter.set(player, patch.varp, state.pack())
        fun planted(): PatchState = planted(crop).also { state(it) }
        fun grown(lives: Int = 1): PatchState =
            planted(crop).copy(stage = crop.stages, produce = lives).also { state(it) }
        fun fill() { for (slot in 0 until 28) player.inv[slot] = InvObj(SPADE) }
        fun give(obj: String, count: Int = 1) {
            val stackable = checkNotNull(ServerCacheManager.getItem(obj.asRSCM())).stackable
            repeat(if (stackable) 1 else count) {
                val slot = (0 until 28).first { player.inv[it] == null }
                player.inv[slot] = InvObj(obj, if (stackable) count else 1)
            }
        }
        fun op() { startOp(); finish() }
        private fun bound(): Pair<BoundLocInfo, ObjectServerType> {
            val name = patch.locs.first()
            val type = checkNotNull(ServerCacheManager.getObject(name.asRSCM(RSCMType.LOC)))
            return BoundLocInfo(player.coords, LocEntity(type.id, 10, 0), 2, 1, 1, 0) to type
        }
        fun startOp() = start {
            val (loc, type) = bound()
            assertTrue(events.publish(this, LocEvents.Op1(loc, loc, type)))
        }
        fun use(obj: String) { startUse(obj); finish() }
        fun startUse(obj: String, selectedSlot: Int? = null) = start {
            val id = obj.asRSCM(RSCMType.OBJ)
            val slot = selectedSlot ?: player.inv.indices.firstOrNull { player.inv[it]?.id == id } ?: 0
            with(script) { useItem(patch, obj, slot, bound().first) }
        }
        fun inspect() { start { with(service) { inspect(patch) } }; finish() }

        private fun start(action: suspend ProtectedAccess.() -> Unit) {
            lastStep = action
            retriggered = false
            player.currentMapClock++
            player.processedMapClock = player.currentMapClock
            while (player.isDelayed) {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
            }
            result = null
            player.activeCoroutine = coroutine
            val access = ProtectedAccess(player, coroutine, context)
            val body: suspend () -> Unit = { access.action() }
            body.startCoroutine(object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) { this@Fixture.result = result }
            })
            result?.getOrThrow()
        }

        /** Drains the coroutine, then replays the step one tick later for each re-trigger, like the engine. */
        fun finish() {
            repeat(200) {
                if (coroutine.isIdle) {
                    result?.getOrThrow()
                    if (!retriggered) return
                    start(lastStep)
                    return@repeat
                }
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
                coroutine.advance()
                result?.getOrThrow()
            }
            fail<Unit>("Farming action did not finish")
        }
    }

    private class FixedRandom(rolls: List<Int>) : GameRandom {
        private val rolls = rolls.iterator()
        val bounds = mutableListOf<Int>()
        var beforeRoll: () -> Unit = {}
        override fun of(maxExclusive: Int): Int {
            beforeRoll()
            bounds += maxExclusive
            return if (rolls.hasNext()) rolls.next() else maxExclusive - 1
        }
        override fun of(minInclusive: Int, maxInclusive: Int): Int = minInclusive
        override fun randomDouble(): Double = 0.999
    }

    companion object {
        private const val CAN_8 = "obj.watering_can_8"
        private const val CAN_7 = "obj.watering_can_7"
        private val restored = mutableListOf<() -> Unit>()
        private lateinit var quests: QuestRequirementResolver

        private fun planted(crop: Crop): PatchState = PatchState(weeds = 3, cropIndex = crop.index)

        @JvmStatic @BeforeAll fun cache() {
            val revision = loadFarmingTestCache()
            val previous = QuestRequirements.activePolicy()
            try {
                quests = QuestRequirementResolver(ServerConfig("farming-test", 0, revision, "test", 1))
            } finally {
                QuestRequirements.install(previous)
            }
            installFarmingTestInventory(restored)
        }

        @JvmStatic @AfterAll fun restore() {
            restored.asReversed().forEach { it() }
            restored.clear()
        }
    }
}
