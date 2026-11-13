package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import io.mockk.mockk
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
import org.rsmod.api.player.hands
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.stat.statBase
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
import org.rsmod.game.hit.Hit
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
    fun `full inventory preserves the woad batch and space allows one reward`() {
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
        assertTrue(f.rewardRandom.bounds.isEmpty())
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
            assertTrue(f.rewardRandom.bounds.isEmpty())
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
        assertEquals(
            listOf(f.crop.petChance - 99 * 25) + listOfNotNull(f.crop.hesporiSeedChance),
            f.rewardRandom.bounds,
        )
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
        assertTrue(f.rewardRandom.bounds.isEmpty())
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
    fun `magic secateurs save lives from inventory or worn equipment`() {
        for (worn in listOf(false, true)) {
            val f = Fixture("guam", rolls = listOf(85))
            val pending = f.grown(lives = 1)
            f.fill()
            f.player.inv[0] = null
            if (worn) f.player.worn[3] = InvObj(MAGIC_SECATEURS) else f.player.inv[1] = InvObj(MAGIC_SECATEURS)
            f.op()
            assertEquals(1, f.count(f.crop.produce))
            assertEquals(pending, f.state())
            assertEquals(125, f.xp())
        }
    }

    @Test
    fun `check-health pays check and planting XP once and fills renewable produce`() {
        val f = Fixture("redberry bush")
        val grown = f.grown(lives = 0)
        f.onCheck = {
            assertEquals(Phase.CHECKED, f.state().phase)
            assertEquals(((f.crop.checkXp + f.crop.plantXp) * 10).toInt(), f.xp())
        }
        f.op()
        assertEquals(grown.copy(phase = Phase.CHECKED, produce = f.crop.maxProduce), f.state())
        assertEquals(1, f.checks.size)
        val rolls = f.rewardRandom.bounds.toList()
        val checkedXp = f.xp()
        f.op()
        assertEquals(1, f.checks.size)
        assertEquals(rolls, f.rewardRandom.bounds)
        assertEquals(f.crop.maxProduce, f.count(f.crop.produce))
        assertEquals(checkedXp + (f.crop.harvestXp * 10).toInt() * f.crop.maxProduce, f.xp())
        assertEquals(grown.copy(phase = Phase.CHECKED, produce = 0), f.state())
        assertTrue(f.harvests.none(FarmingHarvestEvent::final))
    }

    @Test
    fun `renewable picking stops at a full inventory without losing produce`() {
        val f = Fixture("redberry bush")
        val checked = f.grown(lives = 0).copy(phase = Phase.CHECKED, produce = 4)
        f.state(checked)
        f.fill()
        f.player.inv[0] = null
        f.op()
        assertEquals(1, f.count(f.crop.produce))
        assertEquals(checked.copy(produce = 3), f.state())
        assertEquals(1, f.harvests.size)
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
    fun `unavailable crop content never consumes a seed or mutates saved patch state`() {
        for (name in listOf("oak tree", "redberry bush", "iasor plant", "calquat tree")) {
            val f = Fixture(name)
            f.state(PatchState().empty())
            f.give(f.crop.seed)
            f.give(SPADE)
            f.give(DIBBER)
            f.use(f.crop.seed)
            assertEquals(1, f.count(f.crop.seed))
            assertEquals(PatchState().empty(), f.state())
            assertEquals(0, f.xp())
        }
    }

    @Test
    fun `kronos planting in a full inventory consumes one seed and awards planting XP once`() {
        val f = Fixture("kronos plant")
        f.state(PatchState().empty())
        f.give(f.crop.seed, 2)
        f.give(DIBBER)
        for (slot in f.player.inv.indices) {
            if (f.player.inv[slot] == null) f.player.inv[slot] = InvObj(SPADE)
        }
        f.use(f.crop.seed)
        val planted = f.state()
        assertEquals(f.crop.index, planted.cropIndex)
        assertEquals(1, f.count(f.crop.seed))
        assertEquals(1000, f.xp())
        f.use(f.crop.seed)
        assertEquals(planted, f.state())
        assertEquals(1, f.count(f.crop.seed))
        assertEquals(1000, f.xp())
        assertEquals(1, f.plants.size)
    }

    @Test
    fun `filling a pot needs a trowel and an empty weeded patch`() {
        val f = Fixture("oak tree")
        f.state(PatchState().empty())
        f.give("obj.plantpot_empty", 2)
        f.use("obj.plantpot_empty")
        assertEquals(2, f.count("obj.plantpot_empty"))
        f.give("obj.gardening_trowel")
        f.use("obj.plantpot_empty")
        assertEquals(1, f.count("obj.plantpot_empty"))
        assertEquals(1, f.count("obj.plantpot_compost"))
        f.state(PatchState(weeds = 2))
        f.use("obj.plantpot_empty")
        assertEquals(1, f.count("obj.plantpot_empty"))
        assertEquals(0, f.xp())
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
    fun `mature cacti and calquats clear with produce remaining without repeating rewards`() {
        for (name in listOf("cactus", "potato cactus", "calquat tree")) {
            val f = Fixture(name)
            f.grown()
            f.op()
            val checked = f.state()
            val xp = f.xp()
            assertEquals(Phase.CHECKED, checked.phase)
            assertTrue(checked.produce > 0)
            f.startOp(5)
            f.finish()
            assertEquals(checked, f.state())
            f.fill()
            f.startOp(5)
            f.finish()
            assertEquals(checked.empty(), f.state())
            f.startOp(5)
            f.finish()
            assertEquals(xp, f.xp())
            assertEquals(0, f.count(f.crop.produce))
            assertEquals(28, f.count(SPADE))
            assertEquals(1, f.clears.size)
            assertFalse(f.clears.single().dead)
            assertEquals(1, f.checks.size)
            assertTrue(f.harvests.isEmpty())
        }
    }

    @Test
    fun `mature clearing preserves a changed crop or a crop whose spade vanished during digging`() {
        for (name in listOf("potato cactus", "calquat tree")) {
            for (replace in listOf(false, true)) {
                val f = Fixture(name)
                val checked = f.grown().copy(phase = Phase.CHECKED, produce = f.crop.maxProduce)
                f.state(checked)
                f.give(SPADE)
                f.startOp(5)
                val expected = if (replace) checked.copy(produce = 1) else checked
                if (replace) f.state(expected) else f.player.inv[0] = null
                f.finish()
                assertEquals(expected, f.state())
                assertEquals(0, f.xp())
                assertTrue(f.clears.isEmpty())
                assertEquals(0, f.count(f.crop.produce))
            }
        }
    }

    @Test
    fun `barren calquat clearing resets soil without duplicating XP or enabling held replanting`() {
        for (item in listOf(false, true)) {
            val f = Fixture("calquat tree")
            f.grown()
            f.op()
            val xp = f.xp()
            assertEquals(122255, xp)
            val barren = f.state().copy(
                produce = 0, compost = Compost.ULTRA, paid = true, resurrected = true,
            )
            f.state(barren)
            f.give(SPADE)
            f.give(f.crop.seed)
            val compost = checkNotNull(FarmingComposts.get(Compost.NORMAL).obj)
            f.give(compost)
            if (item) f.use(SPADE) else f.op()
            assertEquals(PatchState(weeds = PatchState.CLEARED), f.state())
            if (item) f.use(SPADE) else f.op()
            assertEquals(1, f.clears.size)
            assertFalse(f.clears.single().dead)
            assertEquals(1, f.checks.size)
            assertTrue(f.harvests.isEmpty())
            assertEquals(0, f.count(f.crop.produce))
            assertEquals(1, f.count(SPADE))
            assertEquals(xp, f.xp())
            f.use(compost)
            val soil = PatchState(weeds = PatchState.CLEARED, compost = Compost.NORMAL)
            assertEquals(soil, f.state())
            assertEquals(0, f.count(compost))
            assertEquals(1, f.count("obj.bucket_empty"))
            val treatedXp = f.xp()
            f.use(f.crop.seed)
            assertEquals(soil, f.state())
            assertEquals(1, f.count(f.crop.seed))
            assertEquals(1, f.count(SPADE))
            assertEquals(treatedXp, f.xp())
            assertTrue(f.plants.isEmpty())
        }
    }

    @Test
    fun `calquat removal cannot bypass its pending health check or create stump removal`() {
        val f = Fixture("calquat tree")
        f.give(SPADE)
        for (phase in listOf(Phase.GROWING, Phase.STUMP)) {
            val pending = f.grown(lives = 0).copy(phase = phase)
            f.state(pending)
            f.use(SPADE)
            assertEquals(pending, f.state())
            assertEquals(0, f.xp())
            assertEquals(1, f.count(SPADE))
            assertTrue(f.clears.isEmpty())
        }
    }

    @Test
    fun `belladonna rechecks protective gloves and deals one poison hit instead of harvesting`() {
        for (gloves in listOf(null, "obj.macro_mime_gloves")) {
            val f = Fixture("belladonna")
            val grown = f.grown()
            f.player.hands = InvObj("obj.jewl_bracelet_regen")
            f.startOp()
            f.player.hands = gloves?.let { InvObj(it) }
            f.finish()
            assertEquals(grown, f.state())
            assertEquals(0, f.count(f.crop.produce))
            assertEquals(0, f.xp())
            assertTrue(f.random.bounds.isEmpty())
            assertTrue(f.rewardRandom.bounds.isEmpty())
            assertTrue(f.harvests.isEmpty())
            assertEquals(1, f.player.queueList.size)
            val hit = checkNotNull(f.player.queueList.iterator()).next().args as Hit
            assertEquals(2, hit.damage)
            assertEquals("hitmark.poison_damage_lit".asRSCM(), hit.hitmark.self)
            assertEquals(0, f.player.vars["varp.poison_severity"])
            f.player.hands = InvObj("obj.jewl_bracelet_regen")
            f.op()
            assertEquals(18, f.count(f.crop.produce))
            assertEquals(grown.empty(), f.state())
            assertEquals(6030, f.xp())
            f.op()
            assertEquals(18, f.count(f.crop.produce))
            assertEquals(6030, f.xp())
            assertEquals(1, f.harvests.size)
            assertEquals(1, f.player.queueList.size)
        }
    }

    @Test
    fun `belladonna stack overflow preserves the batch and a later harvest commits it once`() {
        val f = Fixture("belladonna")
        val grown = f.grown()
        f.player.hands = InvObj("obj.leather_gloves")
        f.fill()
        f.player.inv[0] = InvObj(checkNotNull(f.crop.produce), Int.MAX_VALUE)
        f.op()
        assertEquals(grown, f.state())
        assertEquals(Int.MAX_VALUE, f.count(f.crop.produce))
        assertEquals(0, f.xp())
        assertTrue(f.harvests.isEmpty())
        assertTrue(f.rewardRandom.bounds.isEmpty())
        f.player.inv[0] = InvObj(checkNotNull(f.crop.produce), Int.MAX_VALUE - 18)
        f.op()
        assertEquals(Int.MAX_VALUE, f.count(f.crop.produce))
        assertEquals(27, f.count(SPADE))
        assertEquals(grown.empty(), f.state())
        assertEquals(6030, f.xp())
        f.op()
        assertEquals(Int.MAX_VALUE, f.count(f.crop.produce))
        assertEquals(6030, f.xp())
        assertEquals(1, f.harvests.size)
        assertTrue(f.player.queueList.isEmpty)
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
    fun `final harvest pet chance uses base level before the awarded XP levels up`() {
        val f = Fixture("guam", rolls = listOf(255))
        f.player.statMap.setXP(STAT_FARMING, 82)
        f.player.statMap.setBaseLevel(STAT_FARMING, 1)
        f.player.statMap.setCurrentLevel(STAT_FARMING, 1)
        f.grown(lives = 1)
        f.op()
        assertEquals(1, f.count(f.crop.produce))
        assertEquals(f.crop.petChance - 25, f.rewardRandom.bounds.first())
        assertEquals(2, f.player.statBase(STAT_FARMING))
        val bounds = f.rewardRandom.bounds.toList()
        f.op()
        assertEquals(bounds, f.rewardRandom.bounds)
    }

    @Test
    fun `a failed or stale protection payment changes nothing and success pays only once`() {
        val f = Fixture("guam")
        val planted = f.planted()
        var paid = 0
        assertEquals(ProtectResult.UNPAID, f.service.protect(f.player, f.patch, planted) { false })
        assertEquals(planted, f.state())
        assertEquals(ProtectResult.PROTECTED, f.service.protect(f.player, f.patch, planted) { paid++; true })
        assertEquals(planted.copy(paid = true), f.state())
        assertEquals(ProtectResult.STALE, f.service.protect(f.player, f.patch, planted) { paid++; true })
        assertEquals(ProtectResult.ALREADY, f.service.protect(f.player, f.patch) { paid++; true })
        assertEquals(1, paid)
    }

    @Test
    fun `a stale cure cannot charge for or mutate a replacement crop`() {
        val f = Fixture("guam")
        val expected = f.planted().copy(health = Health.DISEASED)
        val replacement = expected.copy(stage = 2)
        f.state(replacement)
        var paid = false
        assertEquals(CureResult.STALE, f.service.cure(f.player, f.patch, expected) { paid = true; true })
        assertFalse(paid)
        assertEquals(replacement, f.state())
        assertEquals(CureResult.CURED, f.service.cure(f.player, f.patch, replacement) { true })
        assertEquals(replacement.copy(health = Health.HEALTHY), f.state())
    }

    @Test
    fun `resurrection rolls only after payment and cannot reward the same crop twice`() {
        val f = Fixture("guam")
        val dead = f.planted().copy(stage = 2, health = Health.DEAD)
        f.state(dead)
        var rolls = 0
        var paid = 0
        val success = { rolls++; true }
        assertEquals(ResurrectResult.UNPAID, f.service.resurrect(f.player, f.patch, success, dead) { false })
        assertEquals(0, rolls)
        assertEquals(dead, f.state())
        assertEquals(ResurrectResult.RESURRECTED, f.service.resurrect(f.player, f.patch, success, dead) { paid++; true })
        val revived = f.state()
        assertEquals(Health.HEALTHY, revived.health)
        assertTrue(revived.resurrected)
        f.state(revived.copy(health = Health.DEAD))
        assertEquals(ResurrectResult.ALREADY_RESURRECTED, f.service.resurrect(f.player, f.patch, success) { paid++; true })
        assertEquals(1, rolls)
        assertEquals(1, paid)
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

    @Test
    fun `removing the seed box during planting does not spend its seeds or a bounty charge`() {
        val f = Fixture("potato", rolls = listOf(0))
        f.state(PatchState().empty())
        f.give(FarmingBags.SEED_BOX)
        f.give(DIBBER)
        f.player.worn[2] = InvObj(FarmingEquipment.BOUNTY)
        val seeds = FarmingBags.seedBox.store(f.player)
        seeds[0] = InvObj(f.crop.seed, 3)
        f.startUse(FarmingBags.SEED_BOX)
        f.player.inv[0] = null
        f.finish()
        assertEquals(3, seeds.count(f.crop.seed))
        assertEquals(0, f.state().cropIndex)
        assertEquals(0, f.player.vars[FarmingEquipment.BOUNTY_USES])
        assertTrue(f.random.bounds.isEmpty())
    }

    @Test
    fun `seed box bounty planting requires the full seed batch and commits one charge once`() {
        val f = Fixture("potato", rolls = listOf(0))
        f.state(PatchState().empty())
        f.give(FarmingBags.SEED_BOX)
        f.give(DIBBER)
        f.player.worn[2] = InvObj(FarmingEquipment.BOUNTY)
        val seeds = FarmingBags.seedBox.store(f.player)
        seeds[0] = InvObj(f.crop.seed, 2)
        f.use(FarmingBags.SEED_BOX)
        assertEquals(2, seeds.count(f.crop.seed))
        assertEquals(0, f.state().cropIndex)
        assertTrue(f.random.bounds.isEmpty())
        seeds[0] = InvObj(f.crop.seed, 3)
        f.use(FarmingBags.SEED_BOX)
        assertEquals(2, seeds.count(f.crop.seed))
        assertEquals(f.crop.index, f.state().cropIndex)
        assertEquals(1, f.player.vars[FarmingEquipment.BOUNTY_USES])
        f.use(FarmingBags.SEED_BOX)
        assertEquals(2, seeds.count(f.crop.seed))
        assertEquals(1, f.player.vars[FarmingEquipment.BOUNTY_USES])
        assertEquals(listOf(4), f.random.bounds)
    }

    @Test
    fun `attas boosts harvest through native withering and stops after death`() {
        val attas = FarmingCrops.bySeed("obj.attas_seed")!!
        val anima = FarmingPatches.all.first { it.kind == PatchKind.ANIMA }
        for (alive in listOf(true, false)) {
            val f = Fixture("guam", rolls = listOf(83))
            val grown = f.grown(lives = 1)
            f.fill()
            f.player.inv[0] = null
            VarPlayerIntMapSetter.set(f.player, anima.varp, PatchState(
                weeds = 3, cropIndex = attas.index, stage = if (alive) 7 else 8,
                health = if (alive) Health.HEALTHY else Health.DEAD,
            ).pack())
            f.op()
            assertEquals(1, f.count(f.crop.produce))
            assertEquals(if (alive) grown else grown.empty(), f.state())
        }
    }

    @Test
    fun `a final grape harvest dies once and clearing cannot pay planting XP again`() {
        val f = Fixture("grape vine", rolls = listOf(255))
        f.grown(lives = 1)
        f.give(SPADE)
        f.op()
        assertEquals(Phase.CHECKED, f.state().phase)
        val checkedXp = f.xp()
        f.op()
        assertEquals(1, f.count(f.crop.produce))
        assertEquals(Health.DEAD, f.state().health)
        assertEquals(f.crop.index, f.state().cropIndex)
        assertEquals(0, f.state().produce)
        val finalXp = checkedXp + (f.crop.harvestXp * 10).toInt()
        assertEquals(finalXp, f.xp())
        var paid = false
        assertEquals(ResurrectResult.EXCLUDED, f.service.resurrect(f.player, f.patch, { true }) { paid = true; true })
        assertFalse(paid)
        f.use(SPADE)
        assertEquals(PatchState().empty(), f.state())
        assertEquals(finalXp, f.xp())
        assertEquals(1, f.checks.size)
        assertEquals(1, f.harvests.size)
    }

    @Test
    fun `grape saltpetre prepares weedless soil and is consumed only once before planting`() {
        val f = Fixture("grape vine")
        f.state(PatchState())
        f.give(DIBBER)
        f.give(f.crop.seed)
        f.give(FarmingGrapes.SALTPETRE, 2)
        f.give("obj.gardening_trowel")
        f.use(f.crop.seed)
        assertEquals(1, f.count(f.crop.seed))
        assertEquals(0, f.state().cropIndex)
        f.onPatchChange = {
            assertEquals(1, f.count(FarmingGrapes.SALTPETRE))
        }
        f.use(FarmingGrapes.SALTPETRE)
        assertTrue(f.state().grapePrepared)
        assertEquals(1, f.count(FarmingGrapes.SALTPETRE))
        f.use(FarmingGrapes.SALTPETRE)
        assertEquals(1, f.count(FarmingGrapes.SALTPETRE))
        f.use(f.crop.seed)
        assertEquals(0, f.count(f.crop.seed))
        assertEquals(f.crop.index, f.state().cropIndex)
        assertTrue(f.state().grapePrepared)
    }

    @Test
    fun `winning rare rolls happen before produce while delivery cannot displace it`() {
        val f = Fixture("guam", rolls = listOf(255), rewardRolls = listOf(0, 0))
        val grown = f.grown(lives = 1)
        f.fill()
        f.player.inv[0] = null
        VarPlayerIntMapSetter.set(f.player, "varp.follower_obj", "obj.skillpetmining".asRSCM())
        f.rewardRandom.beforeRoll = {
            assertEquals(0, f.count(f.crop.produce))
            assertEquals(0, f.xp())
            assertEquals(grown, f.state())
        }
        f.op()
        assertEquals(1, f.count(f.crop.produce))
        assertEquals(27, f.count(SPADE))
        assertEquals(0, f.count("obj.hespori_seed"))
        assertEquals(0, f.count("obj.skillpetfarming"))
        assertEquals(2, f.rewardRandom.bounds.size)
        f.op()
        assertEquals(2, f.rewardRandom.bounds.size)
    }

    @Test
    fun `sulphurous fertiliser exchanges its bucket atomically even with a full inventory`() {
        val f = Fixture("grape vine")
        f.state(PatchState())
        f.fill()
        f.player.inv[0] = InvObj(FarmingGrapes.FERTILISER)
        f.onPatchChange = {
            assertEquals(0, f.count(FarmingGrapes.FERTILISER))
            assertEquals(1, f.count("obj.bucket_empty"))
        }
        f.use(FarmingGrapes.FERTILISER)
        assertTrue(f.state().fertilised)
        assertEquals(28, f.player.inv.occupiedSpace())
        assertEquals(27, f.count(SPADE))
        assertEquals(1, f.count("obj.bucket_empty"))
        f.use(FarmingGrapes.FERTILISER)
        assertEquals(1, f.patchChanges.size)
        assertEquals(1, f.count("obj.bucket_empty"))
    }

    @Test
    fun `wrong patch or stale selected fertiliser slot cannot charge or change soil`() {
        for (name in listOf("guam", "grape vine")) {
            val f = Fixture(name)
            val soil = PatchState()
            f.state(soil)
            f.give(SPADE)
            f.give(FarmingGrapes.FERTILISER)
            assertFalse(FarmingGrapes.prepareSoil(f.player, f.service, f.patch, FarmingGrapes.FERTILISER, 0))
            assertEquals(soil, f.state())
            assertEquals(1, f.count(FarmingGrapes.FERTILISER))
            assertEquals(1, f.count(SPADE))
            assertTrue(f.patchChanges.isEmpty())
        }
    }

    @Test
    fun `living anima removal requires confirmation and gives no second planting XP`() {
        val f = Fixture("attas plant")
        val planted = f.planted()
        f.give(SPADE)
        f.startUse(SPADE)
        f.choose(2)
        f.finish()
        assertEquals(planted, f.state())
        assertTrue(f.clears.isEmpty())
        f.startUse(SPADE)
        f.choose(1)
        f.finish()
        assertEquals(planted.empty(), f.state())
        assertEquals(0, f.xp())
        assertEquals(1, f.clears.size)
        f.use(SPADE)
        assertEquals(1, f.clears.size)
    }

    @Test
    fun `anima changed during confirmation is not cleared using the stale quote`() {
        val f = Fixture("attas plant")
        val planted = f.planted()
        f.give(SPADE)
        f.startUse(SPADE)
        val advanced = planted.copy(stage = 1)
        f.state(advanced)
        f.choose(1)
        f.finish()
        assertEquals(advanced, f.state())
        assertEquals(0, f.xp())
        assertTrue(f.clears.isEmpty())
    }

    @Test
    fun `scarecrow item use refuses missing source and wrong patch without an event`() {
        val flower = Fixture("marigold")
        val empty = PatchState().empty()
        flower.state(empty)
        repeat(2) { flower.use(FarmingScarecrow.SCARECROW) }
        assertEquals(empty, flower.state())
        assertEquals(0, flower.count(FarmingScarecrow.SCARECROW))
        assertEquals(0, flower.xp())
        assertTrue(flower.scarecrows.isEmpty())

        val herb = Fixture("guam")
        herb.state(empty)
        herb.give(FarmingScarecrow.SCARECROW)
        repeat(2) { herb.use(FarmingScarecrow.SCARECROW) }
        assertEquals(empty, herb.state())
        assertEquals(1, herb.count(FarmingScarecrow.SCARECROW))
        assertEquals(0, herb.xp())
        assertTrue(herb.scarecrows.isEmpty())
    }

    private class Fixture(
        name: String, rolls: List<Int> = emptyList(), rewardRolls: List<Int> = emptyList(),
    ) {
        val crop = FarmingCrops.all.first { it.name == name }
        val patch = FarmingPatches.all.first { it.kind == crop.kind }
        val random = FixedRandom(rolls)
        val rewardRandom = FixedRandom(rewardRolls)
        private val events = EventBus()
        val service = FarmingService(
            XpModifiers(emptySet()), random, events,
            FarmingRewardTestSupport.rewards(rewardRandom, events), FarmingEquipment(random),
            FarmingSiteAccess(quests),
        )
        private val script = FarmingScript(
            service, FarmingCompostService(service, XpModifiers(emptySet())),
            FarmingScarecrow(service), FarmingSpiritTrees(service, quests),
            mockk<FarmingTreeChop>(),
        )
        private val coroutine = GameCoroutine("farming-inventory-test")
        private var result: Result<Unit>? = null
        private val context = ProtectedAccessContextFactory.empty().copy(
            getEventBus = { events }, getAlignment = { TextAlignment() },
            getRandom = { random },
        )
        val plants = mutableListOf<FarmingPlantEvent>()
        val checks = mutableListOf<FarmingCheckHealthEvent>()
        val harvests = mutableListOf<FarmingHarvestEvent>()
        val clears = mutableListOf<FarmingClearEvent>()
        val refreshes = mutableListOf<FarmingRefreshEvent>()
        val patchChanges = mutableListOf<FarmingPatchChangeEvent>()
        val scarecrows = mutableListOf<FarmingScarecrowPlacedEvent>()
        var onPatchChange: (FarmingPatchChangeEvent) -> Unit = {}
        var onRefresh: (FarmingRefreshEvent) -> Unit = {}
        var onHarvest: (FarmingHarvestEvent) -> Unit = {}
        var onCheck: (FarmingCheckHealthEvent) -> Unit = {}

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
            events.subscribeUnbound(FarmingCheckHealthEvent::class.java) { onCheck(this); checks += this }
            events.subscribeUnbound(FarmingHarvestEvent::class.java) { onHarvest(this); harvests += this }
            events.subscribeUnbound(FarmingClearEvent::class.java) { clears += this }
            events.subscribeUnbound(FarmingRefreshEvent::class.java) { onRefresh(this); refreshes += this }
            events.subscribeUnbound(FarmingPatchChangeEvent::class.java) { onPatchChange(this); patchChanges += this }
            events.subscribeUnbound(FarmingScarecrowPlacedEvent::class.java) { scarecrows += this }
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
        fun startOp(option: Int = 1) = start {
            val name = patch.locs.firstOrNull { it.startsWith("loc.grapevine_patch_clickzone_") } ?: patch.locs.first()
            val type = checkNotNull(ServerCacheManager.getObject(name.asRSCM(RSCMType.LOC)))
            val loc = BoundLocInfo(player.coords, LocEntity(type.id, 10, 0), 2, 1, 1, 0)
            val event = if (option == 5) LocEvents.Op5(loc, loc, type) else LocEvents.Op1(loc, loc, type)
            assertTrue(events.publish(this, event))
        }
        fun use(obj: String) { startUse(obj); finish() }
        fun startUse(obj: String, selectedSlot: Int? = null) = start {
            val id = obj.asRSCM(RSCMType.OBJ)
            val slot = selectedSlot ?: player.inv.indices.firstOrNull { player.inv[it]?.id == id } ?: 0
            with(script) { useItem(patch, obj, slot) }
        }
        fun inspect() { start { with(service) { inspect(patch) } }; finish() }
        fun choose(option: Int) {
            assertTrue(coroutine.isAwaiting(ResumePauseButtonInput::class))
            coroutine.resumeWith(ResumePauseButtonInput("component.chatmenu:options", option))
            result?.getOrThrow()
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
        private const val MAGIC_SECATEURS = "obj.fairy_enchanted_secateurs"
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
