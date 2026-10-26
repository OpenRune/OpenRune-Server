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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.server.config.GameplayConfig
import org.rsmod.api.server.config.QuestRequirementsYaml
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.api.shops.ShopOpenedEvent
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.quest.manager.QuestRequirementResolver
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.events.UnboundEvent
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
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

    @Test fun `palm health targets distinguish maze stronghold Lletya and Brimhaven without requiring an active boost`() {
        val f = Fixture()
        val palm = crop("obj.plantpot_palm_sapling")
        f.player.statMap.setCurrentLevel(STAT_FARMING, 1)
        f.check(patch("gnome_stronghold_fruit"), palm)
        assertEquals(0, f.player.vars["varp.ardounge_achievement_diary2"])
        f.harvest(patch("gnome_maze_fruit"), palm)
        assertEquals(0, f.player.vars["varp.ardounge_achievement_diary2"])
        f.check(patch("gnome_maze_fruit"), crop("obj.plantpot_papaya_sapling"))
        assertEquals(0, f.player.vars["varp.ardounge_achievement_diary2"])
        f.check(patch("gnome_maze_fruit"), palm)
        assertEquals(1, f.player.vars["varp.ardounge_achievement_diary2"])
        assertEquals(0, f.player.vars["varp.western_achievement_diary2"])
        f.check(patch("lletya_fruit"), palm)
        assertEquals(1, f.player.vars["varp.western_achievement_diary2"])
        f.check(patch("brimhaven_fruit"), palm)
        assertEquals(8, f.player.vars["varp.atjun_tasks_4"])
        assertEquals(1, f.player.vars["varbit.atjun_med_farming"])
        f.check(patch("summer_shore_calquat"), crop("obj.plantpot_calquat_sapling"))
        assertEquals(8, f.player.vars["varp.atjun_tasks_4"])
        f.check(patch("tai_bwo_wannai_calquat"), crop("obj.plantpot_calquat_sapling"))
        assertEquals(40, f.player.vars["varp.atjun_tasks_4"])
    }

    @Test fun `Karamja medium accepts other fruit trees only on health check and preserves adjacent native bits`() {
        val f = Fixture()
        val apple = crop("obj.plantpot_apple_sapling")
        val brimhaven = patch("brimhaven_fruit")
        val before = Int.MIN_VALUE or 3
        set(f.player, "varp.atjun_tasks_2", before)
        f.events.publish(FarmingPlantEvent(f.player, brimhaven, apple))
        assertEquals(before, f.player.vars["varp.atjun_tasks_2"])
        f.check(brimhaven, apple)
        assertEquals(before or (1 shl 12), f.player.vars["varp.atjun_tasks_2"])
        assertEquals(0, f.player.vars["varp.atjun_tasks_4"])
    }

    @Test fun `regional harvest tasks reject planting clearing wrong sites and repeats cannot change counters or rewards`() {
        val cases = listOf(
            HarvestCase("obj.torstol_seed", "north_ardougne", "varp.ardounge_achievement_diary2", 11),
            HarvestCase("obj.limpwurt_seed", "north_catherby", "varp.kandarin_achievement_diary", 19),
            HarvestCase("obj.dwarf_weed_seed", "north_catherby", "varp.kandarin_achievement_diary2", 6),
            HarvestCase("obj.belladonna_seed", "draynor_manor_belladonna", "varp.lumb_dray_achievement_diary2", 0),
            HarvestCase("obj.watermelon_seed", "harmony_island_allotment", "varp.morytania_achievement_diary", 25),
            HarvestCase("obj.mushroom_seed", "canifis_mushroom", "varp.morytania_achievement_diary", 29),
        )
        for (case in cases) {
            val f = Fixture()
            val crop = crop(case.seed)
            val patch = patch(case.site, crop.kind)
            val before = Int.MIN_VALUE or (1 shl 30)
            set(f.player, case.variable, before)
            set(f.player, "varp.achievement_diary_taskcounts", 12345)
            set(f.player, "varp.achievement_diary_rewards", 54321)
            f.events.publish(FarmingPlantEvent(f.player, patch, crop))
            f.events.publish(FarmingClearEvent(f.player, patch, crop, false))
            FarmingPatches.all.firstOrNull { it.kind == crop.kind && it.site != patch.site }
                ?.let { f.harvest(it, crop) }
            assertEquals(before, f.player.vars[case.variable], case.seed)
            f.player.statMap.setCurrentLevel(STAT_FARMING, 1)
            repeat(3) { f.harvest(patch, crop, final = false) }
            assertEquals(before or (1 shl case.bit), f.player.vars[case.variable], case.seed)
            assertEquals(12345, f.player.vars["varp.achievement_diary_taskcounts"])
            assertEquals(54321, f.player.vars["varp.achievement_diary_rewards"])
        }
    }

    @Test fun `committed harvest from a saved held crop still completes its sourced task`() {
        val f = Fixture()
        val ivy = crop("obj.poisonivy_bush_seed").copy(held = "new planting unavailable")
        f.harvest(patch("rimmington_bush"), ivy)
        assertEquals(0, f.player.vars["varp.ardounge_achievement_diary2"])
        f.harvest(patch("south_ardougne_bush"), ivy)
        assertEquals(2, f.player.vars["varp.ardounge_achievement_diary2"])
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

    @Test fun `jute requires the exact committed planting event rather than harvesting or clearing`() {
        val f = Fixture()
        val patch = patch("mcgrubors_wood_hops")
        val jute = crop("obj.jute_seed")
        val variable = "varp.kandarin_achievement_diary"
        f.harvest(patch, jute)
        f.events.publish(FarmingClearEvent(f.player, patch, jute, false))
        f.events.publish(FarmingPlantEvent(f.player, patch("yanille_hops"), jute))
        f.events.publish(FarmingPlantEvent(f.player, patch, crop("obj.barley_seed")))
        assertEquals(0, f.player.vars[variable])
        f.player.statMap.setBaseLevel(STAT_FARMING, 12)
        f.player.statMap.setCurrentLevel(STAT_FARMING, 12)
        repeat(2) { f.events.publish(FarmingPlantEvent(f.player, patch, jute)) }
        assertEquals(1 shl 5, f.player.vars[variable])
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

    @Test fun `scarecrow flags require committed placement at the exact flower patches`() {
        val f = Fixture()
        val falador = patch("south_falador", PatchKind.FLOWER)
        val morytania = patch("west_port_phasmatys", PatchKind.FLOWER)
        for (site in listOf("south_falador", "west_port_phasmatys")) {
            val flower = patch(site, PatchKind.FLOWER)
            val marigold = crop("obj.marigold_seed")
            f.events.publish(FarmingPlantEvent(f.player, flower, marigold))
            f.harvest(flower, marigold)
            f.events.publish(FarmingClearEvent(f.player, flower, marigold, false))
            f.events.publish(FarmingPatchChangeEvent(f.player, flower, PatchState(),
                PatchState(scarecrow = true)))
            f.events.publish(FarmingScarecrowPlacedEvent(f.player, patch(site, PatchKind.ALLOTMENT)))
        }
        f.events.publish(FarmingScarecrowPlacedEvent(f.player, patch("north_catherby", PatchKind.FLOWER)))
        assertEquals(0, f.player.vars["varp.falador_achievement_diary"])
        assertEquals(0, f.player.vars["varp.morytania_achievement_diary"])
        assertCommittedFlag(f, "varp.falador_achievement_diary", 1 shl 14,
            FarmingScarecrowPlacedEvent(f.player, falador))
        assertEquals(0, f.player.vars["varp.morytania_achievement_diary"])
        assertCommittedFlag(f, "varp.morytania_achievement_diary", 1 shl 8,
            FarmingScarecrowPlacedEvent(f.player, morytania))
    }

    @Test fun `Prissy protection requires the magic crop and Stronghold tree patch not planting or health`() {
        val f = Fixture()
        val patch = patch("gnome_stronghold_tree")
        val magic = crop("obj.plantpot_magic_tree_sapling")
        val variable = "varp.western_achievement_diary2"
        f.events.publish(FarmingPlantEvent(f.player, patch, magic))
        f.check(patch, magic)
        f.events.publish(FarmingClearEvent(f.player, patch, magic, false))
        f.events.publish(FarmingProtectionEvent(f.player, patch("falador_park_tree"), magic))
        f.events.publish(FarmingProtectionEvent(f.player, patch, crop("obj.plantpot_yew_sapling")))
        f.events.publish(FarmingProtectionEvent(f.player, patch("gnome_stronghold_fruit"), magic))
        assertEquals(0, f.player.vars[variable])
        assertCommittedFlag(f, variable, 1 shl 8, FarmingProtectionEvent(f.player, patch, magic))
    }

    @Test fun `Fertile Soil flag rejects ordinary compost patch changes and other patch kinds or sites`() {
        val f = Fixture()
        val patch = patch("west_port_phasmatys", PatchKind.HERB)
        val variable = "varp.morytania_achievement_diary2"
        f.events.publish(FarmingPatchChangeEvent(f.player, patch, PatchState(),
            PatchState(compost = Compost.SUPER)))
        f.events.publish(FarmingFertileSoilEvent(f.player, patch("north_catherby", PatchKind.HERB)))
        f.events.publish(FarmingFertileSoilEvent(f.player, patch("west_port_phasmatys", PatchKind.ALLOTMENT)))
        f.events.publish(FarmingFertileSoilEvent(f.player, patch("west_port_phasmatys", PatchKind.FLOWER)))
        val herb = crop("obj.torstol_seed")
        f.events.publish(FarmingPlantEvent(f.player, patch, herb))
        f.harvest(patch, herb)
        assertEquals(0, f.player.vars[variable])
        assertCommittedFlag(f, variable, 1 shl 5, FarmingFertileSoilEvent(f.player, patch))
    }

    @Test fun `white fruit and Guild entry flags only follow their dedicated committed events`() {
        val f = Fixture()
        val tree = patch("farming_guild_tree")
        val oak = crop("obj.plantpot_oak_sapling")
        f.events.publish(FarmingPlantEvent(f.player, tree, oak))
        f.check(tree, oak)
        f.harvest(patch("gnome_stronghold_fruit"), crop("obj.plantpot_apple_sapling"),
            obj = "obj.garden_white_tree_fruit")
        assertEquals(0, f.player.vars["varp.varrock_achievement_diary"])
        assertEquals(0, f.player.vars["varp.kourend_achievement_diary"])
        assertCommittedFlag(f, "varp.varrock_achievement_diary", 1 shl 26,
            FarmingWhiteTreeHarvestEvent(f.player))
        assertEquals(0, f.player.vars["varp.kourend_achievement_diary"])
        assertCommittedFlag(f, "varp.kourend_achievement_diary", 1 shl 21,
            FarmingGuildEntryEvent(f.player))
    }

    @Test fun `Tithe planting requires committed Logavano seed C and preserves the signed native mask`() {
        val f = Fixture()
        val variable = "varp.kourend_achievement_diary"
        for (seed in listOf("obj.hosidius_tithe_seed_a", "obj.hosidius_tithe_seed_b",
            "obj.strawberry_seed")) {
            f.events.publish(FarmingTithePlantEvent(f.player, seed))
        }
        val patch = patch("farming_guild_allotment_flower", PatchKind.ALLOTMENT)
        f.events.publish(FarmingPlantEvent(f.player, patch, crop("obj.watermelon_seed")))
        assertEquals(0, f.player.vars[variable])
        assertCommittedFlag(f, variable, Int.MIN_VALUE,
            FarmingTithePlantEvent(f.player, "obj.hosidius_tithe_seed_c"))
    }

    @Test fun `Sarah browse requires her actual shop rather than another farming shop`() {
        val f = Fixture()
        val variable = "varp.falador_achievement_diary"
        for (shop in listOf("inv.farming_shop_1", "inv.farmer_supplies", "inv.inv")) {
            val inventory = checkNotNull(ServerCacheManager.getInventory(shop.asRSCM(RSCMType.INV)))
            f.events.publish(ShopOpenedEvent(f.player, inventory))
        }
        assertEquals(0, f.player.vars[variable])
        val sarah = checkNotNull(ServerCacheManager.getInventory("inv.farming_shop_3".asRSCM(RSCMType.INV)))
        assertCommittedFlag(f, variable, 1 shl 2, ShopOpenedEvent(f.player, sarah))
    }

    private fun assertCommittedFlag(f: Fixture, variable: String, mask: Int, event: UnboundEvent) {
        val before = (1 shl 30) or 1
        set(f.player, variable, before)
        set(f.player, "varp.achievement_diary_taskcounts", 12345)
        set(f.player, "varp.achievement_diary_rewards", 54321)
        repeat(3) { f.events.publish(event) }
        assertEquals(before or mask, f.player.vars[variable])
        assertEquals(12345, f.player.vars["varp.achievement_diary_taskcounts"])
        assertEquals(54321, f.player.vars["varp.achievement_diary_rewards"])
    }

    private data class HarvestCase(val seed: String, val site: String, val variable: String, val bit: Int)

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

        fun check(patch: PatchDef, crop: Crop) { events.publish(FarmingCheckHealthEvent(player, patch, crop)) }
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

    private class SilentClient : Client<Any, Any> {
        override fun write(message: Any) {}
        override fun close() {}
        override fun read(player: Player) {}
        override fun flush() {}
        override fun flushHighPriority() {}
        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
        private val RESTORE = mutableListOf<() -> Unit>()
        private fun crop(seed: String) = checkNotNull(FarmingCrops.bySeed(seed))
        private fun patch(site: String, kind: PatchKind? = null) =
            FarmingPatches.bySite(site).first { kind == null || it.kind == kind }
        private fun set(player: Player, key: String, value: Int) = VarPlayerIntMapSetter.set(player, key, value)

        @OptIn(InternalApi::class)
        @JvmStatic @BeforeAll fun cache() {
            val root = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                .first { Files.isRegularFile(it.resolve("game.yml")) }
            val revision = Files.readAllLines(root.resolve("game.yml"))
                .first { it.startsWith("revision:") }.substringAfter(':').trim().substringBefore('.').toInt()
            ServerCacheManager.init(revision).close()
            for ((owner, name) in listOf(
                "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
            )) {
                val field = Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                RESTORE += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            RESTORE += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic @AfterAll fun restore() { RESTORE.asReversed().forEach { it() }; RESTORE.clear() }
    }
}
