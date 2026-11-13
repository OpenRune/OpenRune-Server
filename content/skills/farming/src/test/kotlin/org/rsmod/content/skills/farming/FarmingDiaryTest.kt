package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.server.config.GameplayConfig
import org.rsmod.api.server.config.QuestRequirementsYaml
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
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@ResourceLock("QuestRequirements")
class FarmingDiaryTest {
    private val previousPolicy = QuestRequirements.activePolicy()

    @AfterEach fun restorePolicy() { QuestRequirements.install(previousPolicy) }

    @Test fun `strawberries require a positive committed harvest of the matching crop at either Ardougne allotment`() {
        val f = Fixture()
        val crop = crop("obj.strawberry_seed")
        val patches = FarmingPatches.bySite("north_ardougne").filter { it.kind == PatchKind.ALLOTMENT }
        val patch = patches.first()
        val variable = "varp.ardounge_achievement_diary"
        f.events.publish(FarmingPlantEvent(f.player, patch, crop))
        f.events.publish(FarmingCheckHealthEvent(f.player, patch, crop))
        f.events.publish(FarmingClearEvent(f.player, patch, crop, false))
        f.harvest(patch("north_catherby", PatchKind.ALLOTMENT), crop)
        f.harvest(patch, crop("obj.watermelon_seed"))
        f.harvest(patch, crop, count = 0)
        f.harvest(patch, crop, obj = "obj.watermelon")
        assertEquals(0, f.player.vars[variable])
        for (candidate in patches) {
            set(f.player, variable, Int.MIN_VALUE or 7)
            f.harvest(candidate, crop, final = false)
            assertEquals(Int.MIN_VALUE or 7 or (1 shl 15), f.player.vars[variable])
        }
    }

    @Test fun `harmony completion respects configured quest access without changing quest progress`() {
        for ((mode, completions, expected) in listOf(
            Triple("respect-progress", emptySet(), 0),
            Triple("virtual-completions", emptySet(), 0),
            Triple("virtual-completions", setOf("quest_greatbrainrobbery"), 1 shl 25),
            Triple("assume-completed", emptySet(), 1 shl 25),
        )) {
            val f = Fixture(mode, completions)
            f.harvest(patch("harmony_island_allotment"), crop("obj.watermelon_seed"))
            assertEquals(expected, f.player.vars["varp.morytania_achievement_diary"], mode)
        }
    }

    @Test fun `full inventory harvest failure leaves the native task and crop unchanged`() {
        val f = Fixture()
        val patch = patch("north_ardougne", PatchKind.ALLOTMENT)
        val strawberry = crop("obj.strawberry_seed")
        val grown = PatchState(weeds = 3, cropIndex = strawberry.index, stage = strawberry.stages, produce = 3)
        set(f.player, patch.varp, grown.pack())
        for (slot in f.player.inv.indices) f.player.inv[slot] = InvObj(SPADE)
        f.start { with(f.service) { harvest(patch) } }
        f.finish()
        assertEquals(grown, PatchState.unpack(f.player.vars[patch.varp]))
        assertEquals(0, f.player.vars["varp.ardounge_achievement_diary"])
        assertEquals(0, f.player.inv.count("obj.strawberry"))
        f.player.inv[1] = null
        f.start { with(f.service) { harvest(patch) } }
        f.finish()
        assertEquals(1, f.player.inv.count("obj.strawberry"))
        assertEquals(1 shl 15, f.player.vars["varp.ardounge_achievement_diary"])
    }

    private class Fixture(mode: String = "assume-completed", completed: Set<String> = emptySet()) {
        val events = EventBus()
        private val random = object : GameRandom {
            override fun of(maxExclusive: Int) = maxExclusive - 1
            override fun of(minInclusive: Int, maxInclusive: Int) = maxInclusive
            override fun randomDouble() = 0.999
        }
        private val access = FarmingSiteAccess(QuestRequirementResolver(ServerConfig("test", 0, 241, "test", 1,
            gameplay = GameplayConfig(QuestRequirementsYaml(mode, completed)))))
        val service = FarmingService(XpModifiers(emptySet()), random, events,
            FarmingRewardTestSupport.rewards(random, events), FarmingEquipment(random), access)
        private val coroutine = GameCoroutine("farming-diary-test")
        private var result: Result<Unit>? = null

        @OptIn(InternalApi::class)
        val player = Player().apply {
            client = SilentClient()
            uuid = 800L
            slotId = 1
            assignUid()
            currentMapClock = 100
            processedMapClock = 100
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
            statMap.setBaseLevel(STAT_FARMING, 99)
            statMap.setCurrentLevel(STAT_FARMING, 99)
            set(this, "varp.farming_clock", Int.MAX_VALUE)
        }

        init { with(FarmingDiaryScript(access)) { ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup() } }

        fun harvest(patch: PatchDef, crop: Crop, count: Int = 1, final: Boolean = false, obj: String = checkNotNull(crop.produce)) {
            events.publish(FarmingHarvestEvent(player, patch, crop, obj, count, final))
        }
        fun start(body: suspend ProtectedAccess.() -> Unit) {
            result = null
            player.activeCoroutine = coroutine
            val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { events }, getRandom = { random })
            val protectedAccess = ProtectedAccess(player, coroutine, context)
            val block: suspend () -> Unit = { protectedAccess.body() }
            block.startCoroutine(object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) { this@Fixture.result = result }
            })
            result?.getOrThrow()
        }
        fun finish() {
            repeat(20) {
                if (coroutine.isIdle) { result?.getOrThrow(); return }
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
                coroutine.advance()
                result?.getOrThrow()
            }
            fail<Unit>("Farming action did not finish")
        }
    }

    companion object {
        private val RESTORE = mutableListOf<() -> Unit>()
        private fun crop(seed: String) = checkNotNull(FarmingCrops.bySeed(seed))
        private fun patch(site: String, kind: PatchKind? = null) =
            FarmingPatches.bySite(site).first { kind == null || it.kind == kind }
        private fun set(player: Player, key: String, value: Int) = VarPlayerIntMapSetter.set(player, key, value)

        @JvmStatic @BeforeAll fun cache() {
            loadFarmingTestCache()
            installFarmingTestInventory(RESTORE)
        }

        @JvmStatic @AfterAll fun restore() { RESTORE.asReversed().forEach { it() }; RESTORE.clear() }
    }
}
