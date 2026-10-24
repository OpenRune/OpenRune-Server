package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import java.nio.file.Files
import java.nio.file.Path
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
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class FarmingInventoryTest {
    @Test
    fun `saved crop indices retain their plants when lower numbered rows are added`() {
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
        f.grown(lives = 3)
        f.op()
        assertEquals(3, f.count("obj.unidentified_huasca"))
        assertEquals(4165, f.xp())
        assertEquals(PatchState().empty(), f.state())
        f.op()
        assertEquals(3, f.count("obj.unidentified_huasca"))
        assertEquals(4165, f.xp())
    }

    @Test
    fun `white lily harvest ends protection and pays its completion XP once`() {
        val f = Fixture("white lily")
        f.state(PatchState().empty())
        f.give(DIBBER)
        f.give(f.crop.seed)
        f.player.statMap.setCurrentLevel(STAT_FARMING, 57)
        f.use(f.crop.seed)
        assertEquals(1, f.count(f.crop.seed))
        assertEquals(0, f.state().cropIndex)
        f.player.statMap.setCurrentLevel(STAT_FARMING, 58)
        f.use(f.crop.seed)
        assertEquals(0, f.count(f.crop.seed))
        assertEquals(29, f.state().cropIndex)
        assertEquals(0, f.xp())
        val lily = f.grown()
        val sweetcorn = checkNotNull(FarmingCrops.bySeed("obj.sweetcorn_seed"))
        val area = FarmingPatches.areas.first()
        val patches = listOf(
            area.patches.first { it.kind == PatchKind.ALLOTMENT },
            area.patches.first { it.kind == PatchKind.FLOWER },
        )
        val crop = planted(sweetcorn).copy(minutes = 1)
        val protected = area.copy(patches = patches).advance(listOf(crop, lily), 1) { true }
        assertEquals(Health.HEALTHY, protected[0].health)
        f.op()
        assertEquals(1, f.count("obj.whitelilly"))
        assertEquals(2920, f.xp())
        val exposed = area.copy(patches = patches).advance(listOf(crop, f.state()), 1) { true }
        assertEquals(Health.DISEASED, exposed[0].health)
        f.op()
        assertEquals(1, f.count("obj.whitelilly"))
        assertEquals(2920, f.xp())
    }

    @Test
    fun `white lily protects all allotments but never herbs`() {
        val lily = checkNotNull(FarmingCrops.bySeed("obj.white_lily_seed"))
        val huasca = checkNotNull(FarmingCrops.bySeed("obj.huasca_seed"))
        val area = FarmingPatches.areas.first()
        val patches = listOf(
            area.patches.first { it.kind == PatchKind.ALLOTMENT },
            area.patches.first { it.kind == PatchKind.FLOWER },
            area.patches.first { it.kind == PatchKind.HERB },
        )
        for (crop in FarmingCrops.all.filter { it.kind == PatchKind.ALLOTMENT }) {
            val states = listOf(
                planted(crop).copy(minutes = 1), grown(lily), planted(huasca).copy(minutes = 1),
            )
            val next = area.copy(patches = patches).advance(states, 1) { true }
            assertEquals(Health.HEALTHY, next[0].health, crop.name)
            assertEquals(Health.DISEASED, next[2].health, crop.name)
        }
    }

    @Test
    fun `immature or unhealthy lilies do not protect and cannot protect themselves`() {
        val lily = checkNotNull(FarmingCrops.bySeed("obj.white_lily_seed"))
        val sweetcorn = checkNotNull(FarmingCrops.bySeed("obj.sweetcorn_seed"))
        val area = FarmingPatches.areas.first()
        val patches = listOf(
            area.patches.first { it.kind == PatchKind.ALLOTMENT },
            area.patches.first { it.kind == PatchKind.FLOWER },
        )
        for (flower in listOf(
            planted(lily).copy(stage = 1, minutes = 1),
            planted(lily).copy(stage = 1, health = Health.DISEASED, minutes = 1),
            planted(lily).copy(stage = 1, health = Health.DEAD, minutes = 0),
        )) {
            val next = area.copy(patches = patches).advance(
                listOf(planted(sweetcorn).copy(minutes = 1), flower), 1,
            ) { true }
            assertEquals(Health.DISEASED, next[0].health)
            assertEquals(
                if (flower.health == Health.HEALTHY) Health.DISEASED else Health.DEAD,
                next[1].health,
            )
        }
    }

    @Test
    fun `full inventory preserves the woad batch and space allows one reward`() {
        val f = Fixture("woad")
        val pending = f.grown()
        f.fill()
        f.op()
        assertEquals(pending, f.state())
        assertEquals(0, f.count(f.crop.produce))
        assertEquals(0, f.xp())
        f.player.inv[0] = null
        f.op()
        assertEquals(3, f.count(f.crop.produce))
        assertEquals(1360, f.xp())
        assertEquals(pending.empty(), f.state())
        f.op()
        assertEquals(3, f.count(f.crop.produce))
        assertEquals(1360, f.xp())
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
    fun `failed herb award spends neither life nor XP nor save roll`() {
        val f = Fixture("guam")
        val pending = f.grown(lives = 2)
        f.fill()
        f.op()
        assertEquals(pending, f.state())
        assertEquals(0, f.xp())
        assertTrue(f.random.bounds.isEmpty())
        assertEquals(0, f.count(f.crop.produce))
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
        val mature = f.state().copy(stage = f.crop.stages, minutes = 0, produce = 2)
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
    fun `magic secateurs save lives from inventory or worn equipment`() {
        for (worn in listOf(false, true)) {
            val f = Fixture("guam", rolls = listOf(85))
            val pending = f.grown(lives = 1)
            f.fill()
            f.player.inv[0] = null
            if (worn) f.player.worn[3] = InvObj(MAGIC_SECATEURS)
            else f.player.inv[1] = InvObj(MAGIC_SECATEURS)
            f.op()
            assertEquals(1, f.count(f.crop.produce))
            assertEquals(pending, f.state())
            assertEquals(125, f.xp())
        }
    }

    @Test
    fun `watering replaces a can atomically even with a full inventory`() {
        val f = Fixture("potato")
        f.planted()
        f.fill()
        f.player.inv[0] = InvObj(WATERING_CANS[8])
        f.use(WATERING_CANS[8])
        assertEquals(28, f.player.inv.occupiedSpace())
        assertEquals(0, f.count(WATERING_CANS[8]))
        assertEquals(1, f.count(WATERING_CANS[7]))
        assertTrue(f.state().watered)
        assertEquals(10, f.xp())
        f.use(WATERING_CANS[7])
        assertEquals(1, f.count(WATERING_CANS[7]))
        assertEquals(0, f.count(WATERING_CANS[6]))
        assertEquals(10, f.xp())
    }

    @Test
    fun `a vanished watering can changes neither crop nor inventory nor XP`() {
        val f = Fixture("potato")
        val planted = f.planted()
        f.give(WATERING_CANS[8])
        f.startUse(WATERING_CANS[8])
        f.player.inv[0] = null
        f.finish()
        assertEquals(planted, f.state())
        assertEquals(0, f.count(WATERING_CANS[7]))
        assertEquals(0, f.xp())
    }

    @Test
    fun `watering uses the current stage and refuses a crop that died during the delay`() {
        val f = Fixture("potato")
        val planted = f.planted()
        f.give(WATERING_CANS[8])
        f.startUse(WATERING_CANS[8])
        val advanced = planted.copy(stage = 1, minutes = 3)
        f.state(advanced)
        f.finish()
        assertEquals(advanced.copy(watered = true), f.state())
        f.state(advanced)
        f.startUse(WATERING_CANS[7])
        f.state(advanced.copy(health = Health.DEAD))
        f.finish()
        assertEquals(1, f.count(WATERING_CANS[7]))
        assertEquals(Health.DEAD, f.state().health)
    }

    @Test
    fun `cure cannot resurrect a crop that dies during its delay`() {
        val f = Fixture("guam")
        val diseased = f.planted().copy(health = Health.DISEASED, minutes = 1)
        f.state(diseased)
        f.give(PLANT_CURE)
        f.startUse(PLANT_CURE)
        val dead = diseased.copy(health = Health.DEAD, minutes = 0)
        f.state(dead)
        f.finish()
        assertEquals(dead, f.state())
        assertEquals(1, f.count(PLANT_CURE))
        assertEquals(0, f.xp())
    }

    @Test
    fun `cure preserves the current countdown and consumes exactly one source`() {
        val f = Fixture("guam")
        val diseased = f.planted().copy(health = Health.DISEASED)
        f.state(diseased)
        f.give(PLANT_CURE, 2)
        f.startUse(PLANT_CURE)
        f.state(diseased.copy(minutes = 2))
        f.finish()
        assertEquals(diseased.copy(health = Health.HEALTHY, minutes = 2), f.state())
        assertEquals(1, f.count(PLANT_CURE))
        f.use(PLANT_CURE)
        assertEquals(1, f.count(PLANT_CURE))
    }

    @Test
    fun `vanished cure does not cure the crop`() {
        val f = Fixture("guam")
        val diseased = f.planted().copy(health = Health.DISEASED)
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
            val advanced = planted.copy(stage = 1, minutes = 3)
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
                if (dead) planted.copy(health = Health.DEAD)
                else planted.copy(stage = f.crop.stages, produce = 3, minutes = 0)
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
        assertEquals(FarmingCrops.index(f.crop), f.state().cropIndex)
        assertEquals(0, f.xp())
        f.use(f.crop.seed)
        assertEquals(3, f.count(f.crop.seed))
        assertEquals(0, f.xp())
    }

    @Test
    fun `vanished seeds or new weeds during planting consume nothing`() {
        for (vanish in listOf(false, true)) {
            val f = Fixture("potato")
            f.state(PatchState().empty())
            f.give(f.crop.seed, 3)
            f.give(DIBBER)
            f.startUse(f.crop.seed)
            if (vanish) f.player.inv[0] = null
            else f.state(f.state().copy(weeds = 2))
            val current = f.state()
            f.finish()
            assertEquals(current, f.state())
            assertEquals(if (vanish) 0 else 3, f.count(f.crop.seed))
            assertEquals(0, f.xp())
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
            val current = f.planted().copy(health = health)
            f.state(current)
            f.inspect()
            assertEquals(current, f.state())
        }
        assertEquals(1, f.count(PLANT_CURE))
        assertEquals(0, f.xp())
    }

    @Test
    fun `dead clear and spade use leave clean soil with the weed countdown restarted`() {
        for (item in listOf(false, true)) {
            val f = Fixture("potato")
            val dead = f.planted().copy(health = Health.DEAD, compost = Compost.ULTRA)
            f.state(dead)
            f.give(SPADE)
            if (item) f.use(SPADE) else f.op()
            assertEquals(dead.empty(), f.state())
            assertEquals(1, f.count(SPADE))
            assertEquals(80, f.xp())
            if (item) f.use(SPADE) else f.op()
            assertEquals(dead.empty(), f.state())
            assertEquals(80, f.xp())
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
        val dead = f.state().copy(health = Health.DEAD)
        f.state(dead)
        f.give(SPADE)
        f.startOp()
        f.player.inv[0] = null
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
        assertEquals(5, f.state().minutes)
        assertEquals(40, f.xp())
    }

    @Test
    fun `offline protection starts at flower maturity regardless of patch ordering`() {
        val potato = checkNotNull(FarmingCrops.bySeed("obj.potato_seed"))
        val marigold = checkNotNull(FarmingCrops.byIndex(potato.protectionIndex))
        val sweetcorn = checkNotNull(FarmingCrops.bySeed("obj.sweetcorn_seed"))
        val lily = checkNotNull(FarmingCrops.bySeed("obj.white_lily_seed"))
        val area = FarmingPatches.areas.first()
        val allotment = area.patches.first { it.kind == PatchKind.ALLOTMENT }
        val flower = area.patches.first { it.kind == PatchKind.FLOWER }
        for ((crop, protector) in listOf(potato to marigold, sweetcorn to lily)) {
            val planted = planted(crop)
            val almostMature = planted(protector).copy(stage = protector.stages - 1, minutes = 11)
            for (reversed in listOf(false, true)) {
                val patches = if (reversed) listOf(flower, allotment) else listOf(allotment, flower)
                val states = if (reversed) listOf(almostMature, planted) else listOf(planted, almostMature)
                var rolls = 0
                val next = area.copy(patches = patches).advance(states, 30) { rolls++; false }
                assertEquals(1, rolls)
                assertEquals(3, next[if (reversed) 1 else 0].stage)
            }
        }
    }

    @Test
    fun `only a mature matching flower in the same farm suppresses disease rolls`() {
        val potato = checkNotNull(FarmingCrops.bySeed("obj.potato_seed"))
        val marigold = checkNotNull(FarmingCrops.byIndex(potato.protectionIndex))
        val woad = checkNotNull(FarmingCrops.bySeed("obj.woad_seed"))
        val area = FarmingPatches.areas.first()
        val patches = listOf(
            area.patches.first { it.kind == PatchKind.ALLOTMENT },
            area.patches.first { it.kind == PatchKind.FLOWER },
        )
        for ((flower, expectedRolls) in listOf(grown(marigold) to 0, grown(woad) to 1)) {
            var rolls = 0
            val next = area.copy(patches = patches).advance(listOf(planted(potato), flower), 10) {
                rolls++
                true
            }
            assertEquals(expectedRolls, rolls)
            assertEquals(if (expectedRolls == 0) Health.HEALTHY else Health.DISEASED, next[0].health)
        }
    }

    private class Fixture(name: String, rolls: List<Int> = emptyList()) {
        val crop = FarmingCrops.all.first { it.name == name }
        private val patch = FarmingPatches.all.first { it.kind == crop.kind }
        val random = FixedRandom(rolls)
        private val script = FarmingScript(XpModifiers(emptySet()), random)
        private val coroutine = GameCoroutine("farming-inventory-test")
        private var result: Result<Unit>? = null
        private val events = EventBus()
        private val context = ProtectedAccessContextFactory.empty().copy(
            getEventBus = { events }, getAlignment = { TextAlignment() },
            getRandom = { random },
        )

        @OptIn(InternalApi::class)
        val player = Player().apply {
            client = SilentClient()
            uuid = 798L
            slotId = 1
            assignUid()
            coords = FarmingPatches.areaOf(patch).centre
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
            with(script) { ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup() }
        }

        fun xp(): Int = player.statMap.getFineXP(STAT_FARMING) - initialXp
        fun count(obj: String): Int = player.inv.count(obj)
        fun state(): PatchState = PatchState.unpack(player.vars[patch.varp])
        fun state(state: PatchState) = VarPlayerIntMapSetter.set(player, patch.varp, state.pack())
        fun planted(): PatchState = planted(crop).also { state(it) }
        fun grown(lives: Int = 1): PatchState = grown(crop).copy(produce = lives).also { state(it) }
        fun fill() { for (slot in 0 until 28) player.inv[slot] = InvObj(SPADE) }
        fun give(obj: String, count: Int = 1) {
            val stackable = checkNotNull(ServerCacheManager.getItem(obj.asRSCM())).stackable
            repeat(if (stackable) 1 else count) {
                val slot = (0 until 28).first { player.inv[it] == null }
                player.inv[slot] = InvObj(obj, if (stackable) count else 1)
            }
        }
        fun op() { startOp(); finish() }
        fun startOp() = start {
            val type = checkNotNull(ServerCacheManager.getObject(patch.loc.asRSCM(RSCMType.LOC)))
            val loc = BoundLocInfo(player.coords, LocEntity(type.id, 10, 0), 2, 1, 1, 0)
            assertTrue(events.publish(this, LocEvents.Op1(loc, loc, type)))
        }
        fun use(obj: String) { startUse(obj); finish() }
        fun startUse(obj: String) = start { with(script) { useItem(patch, obj) } }
        fun inspect() {
            start {
                val type = checkNotNull(ServerCacheManager.getObject(patch.loc.asRSCM(RSCMType.LOC)))
                val loc = BoundLocInfo(player.coords, LocEntity(type.id, 10, 0), 2, 1, 1, 0)
                assertTrue(events.publish(this, LocEvents.Op2(loc, loc, type)))
            }
            finish()
        }

        private fun start(action: suspend ProtectedAccess.() -> Unit) {
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

        fun finish() {
            repeat(200) {
                if (coroutine.isIdle) {
                    result?.getOrThrow()
                    return
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
        override fun of(maxExclusive: Int): Int {
            bounds += maxExclusive
            return if (rolls.hasNext()) rolls.next() else maxExclusive - 1
        }
        override fun of(minInclusive: Int, maxInclusive: Int): Int = minInclusive
        override fun randomDouble(): Double = 0.999
    }

    private class SilentClient : Client<Any, Any> {
        override fun write(message: Any) {}
        override fun close() {}
        override fun read(player: Player) {}
        override fun flush() {}
        override fun flushHighPriority() {}
        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
        private const val MAGIC_SECATEURS = "obj.fairy_enchanted_secateurs"
        private val restored = mutableListOf<() -> Unit>()

        private fun planted(crop: Crop): PatchState = PatchState(
            weeds = 3, cropIndex = FarmingCrops.index(crop), minutes = crop.stageMinutes,
        )
        private fun grown(crop: Crop): PatchState = planted(crop).copy(
            stage = crop.stages, minutes = 0, produce = 1,
        )

        @OptIn(InternalApi::class)
        @JvmStatic @BeforeAll fun cache() {
            val root = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                .first { Files.isRegularFile(it.resolve("game.yml")) }
            val revision = Files.readAllLines(root.resolve("game.yml"))
                .first { it.startsWith("revision:") }.substringAfter(':').trim().toInt()
            ServerCacheManager.init(revision).close()
            for ((owner, name) in listOf(
                "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
            )) {
                val field = Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                restored += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            restored += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic @AfterAll fun restore() {
            restored.asReversed().forEach { it() }
            restored.clear()
        }
    }
}
