package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.quest.manager.QuestRequirementResolver
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

internal fun farmingSiteAccess(): FarmingSiteAccess {
    val previous = QuestRequirements.activePolicy()
    return try {
        FarmingSiteAccess(QuestRequirementResolver(ServerConfig("test", 0, 241, "test", 1)))
    } finally {
        QuestRequirements.install(previous)
    }
}

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@ResourceLock("QuestRequirements")
class FarmingSiteAccessTest {
    private val previous = QuestRequirements.activePolicy()

    @AfterEach fun restorePolicy() { QuestRequirements.install(previous) }

    @Test fun `harmony herb needs elite diary even under assume completed quests`() {
        val access = access("assume-completed")
        val player = Player()
        val herb = patch("harmony_island_herb")
        assertNotNull(access.denial(player, herb))
        set(player, "varbit.morytania_diary_hard_complete", 1)
        assertNotNull(access.denial(player, herb))
        set(player, "varbit.morytania_diary_elite_complete", 1)
        assertNull(access.denial(player, herb))
        set(player, "varbit.morytania_diary_elite_complete", 0)
        assertNotNull(access.denial(player, herb))
    }

    @Test fun `quest patches respect configured completion without changing real progress`() {
        val cases = listOf(
            "harmony_island_allotment" to "quest_greatbrainrobbery",
            "troll_stronghold_herb" to "quest_myarmsbigadventure",
            "coral_nurseries" to "quest_troubledtortugans",
            "locus_oasis_hardwood" to "quest_ribbitingtale",
        )
        val player = Player()
        for ((site, quest) in cases) {
            val patch = patch(site)
            assertNotNull(access("respect-progress").denial(player, patch))
            assertNotNull(access("virtual-completions").denial(player, patch))
            assertNull(access("virtual-completions", setOf(quest)).denial(player, patch))
            assertNull(access("assume-completed").denial(player, patch))
        }
        assertNotNull(access("virtual-completions", setOf("quest_greatbrainrobbery"))
            .denial(player, patch("harmony_island_herb")))
    }

    @Test fun `weiss requires built fire independently of quest policy at exact native boundary`() {
        val access = access("assume-completed")
        val player = Player()
        val patch = patch("weiss_herb")
        val rule = FarmingPatches.access(patch).single()
        assertNotNull(access.denial(player, patch))
        set(player, rule.variable, rule.minimum - 1)
        assertNotNull(access.denial(player, patch))
        set(player, rule.variable, rule.minimum)
        assertNull(access.denial(player, patch))
        set(player, rule.variable, rule.minimum + 1)
        assertNull(access.denial(player, patch))
    }

    @Test fun `denied allotment planting and protection never charge seeds or payment`() {
        access("respect-progress")
        val f = Fixture("harmony_island_allotment")
        f.startPlant()
        f.finish()

        assertEquals(3, f.player.inv.count("obj.potato_seed"))
        assertEquals(PatchState(weeds = 3), f.state())
        val planted = PatchState(weeds = 3, cropIndex = FarmingCrops.bySeed("obj.potato_seed")!!.index)
        set(f.player, f.patch.varp, planted.pack())
        var paid = false
        assertEquals(ProtectResult.UNPAID, f.service.protect(f.player, f.patch) { paid = true; true })
        assertFalse(paid)
        assertEquals(planted, f.state())
    }

    @Test fun `allowed allotment planting succeeds but revoked delayed access spends no seeds`() {
        access("virtual-completions", setOf("quest_greatbrainrobbery"))
        val allowed = Fixture("harmony_island_allotment")
        allowed.startPlant()
        allowed.finish()
        assertEquals(0, allowed.player.inv.count("obj.potato_seed"))
        assertEquals(FarmingCrops.bySeed("obj.potato_seed")!!.index, allowed.state().cropIndex)
        val revoked = Fixture("harmony_island_allotment")
        revoked.startPlant()
        access("respect-progress")
        revoked.finish()
        assertEquals(3, revoked.player.inv.count("obj.potato_seed"))
        assertEquals(PatchState(weeds = 3), revoked.state())
    }

    private fun access(mode: String, completed: Set<String> = emptySet()): FarmingSiteAccess =
        FarmingSiteAccess(QuestRequirementResolver(ServerConfig("test", 0, 241, "test", 1,
            gameplay = GameplayConfig(QuestRequirementsYaml(mode, completed)))))

    private fun patch(site: String) = FarmingPatches.all.first { it.site == site }

    private class Fixture(site: String) {
        val patch = FarmingPatches.all.first { it.site == site }
        private val events = EventBus()
        private val random = object : GameRandom {
            override fun of(bound: Int) = bound - 1
            override fun of(minInclusive: Int, maxInclusive: Int) = maxInclusive
            override fun randomDouble(): Double = 0.999
        }
        val service = FarmingService(XpModifiers(emptySet()), random, events,
            FarmingRewardTestSupport.rewards(random, events), FarmingEquipment(random), farmingSiteAccess())
        private val coroutine = GameCoroutine("farming-site-access-test")
        private var result: Result<Unit>? = null

        @OptIn(InternalApi::class)
        val player = Player().apply {
            client = SilentClient()
            uuid = 799L
            slotId = 1
            assignUid()
            coords = patch.coords.first()
            currentMapClock = 100
            processedMapClock = 100
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
            inv[0] = InvObj("obj.potato_seed", 3)
            inv[1] = InvObj(DIBBER)
            set(this, "varp.farming_clock", Int.MAX_VALUE)
            set(this, patch.varp, PatchState(weeds = 3).pack())
            statMap.setBaseLevel("stat.farming", 99)
            statMap.setCurrentLevel("stat.farming", 99)
        }
        fun state() = PatchState.unpack(player.vars[patch.varp])
        fun startPlant() {
            player.activeCoroutine = coroutine
            val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { events }, getRandom = { random })
            val access = ProtectedAccess(player, coroutine, context)
            val body: suspend () -> Unit = { with(service) { access.plant(patch, "obj.potato_seed") } }
            body.startCoroutine(object : Continuation<Unit> {
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
            fail<Unit>("Planting did not finish")
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
        private val restored = mutableListOf<() -> Unit>()
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
                restored += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            restored += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic @AfterAll fun restore() { restored.asReversed().forEach { it() }; restored.clear() }
    }
}
