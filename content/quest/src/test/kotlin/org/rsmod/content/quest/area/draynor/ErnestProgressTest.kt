package org.rsmod.content.quest.area.draynor

import dev.openrune.ServerCacheManager
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContext
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QUEST_STAGE_MAP_ATTR
import org.rsmod.content.quest.manager.Quest
import org.rsmod.content.quest.manager.rewards
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ErnestProgressTest {
    private val eventBus = EventBus()
    private lateinit var quest: Quest
    private lateinit var progress: ErnestProgress

    @BeforeAll
    fun loadCache() {
        ServerCacheManager.init(240).close()
        val context = ScriptContext(eventBus, CheatCommandMap(), EngineQueueCache())
        with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { context.startup() }
        quest = Quest.register(
            "quest_ernestthechicken",
            "varp.haunted",
            ItemRewardDisplay("obj.coins"),
            rewards { item("obj.coins", ErnestProgress.CoinReward) },
        )
        progress = ErnestProgress(quest)
    }

    @Test
    fun `parts cannot be handed in before the quest or professor introduction`() {
        val access = access()
        addParts(access.player)
        assertFalse(progress.receiveParts(access))
        quest.advanceQuestStage(access)
        assertFalse(progress.receiveParts(access))
        assertTrue(progress.hasAllParts(access.player))
        assertFalse(progress.hasReceivedParts(access.player))
    }

    @Test
    fun `partial hand in leaves every held part untouched`() {
        val access = started()
        access.inv[0] = InvObj("obj.rubber_tube")
        access.inv[1] = InvObj("obj.pressure_gauge")
        assertFalse(progress.receiveParts(access))
        assertEquals(1, access.inv.count("obj.rubber_tube"))
        assertEquals(1, access.inv.count("obj.pressure_gauge"))
        assertFalse(progress.hasReceivedParts(access.player))
        assertEquals(1, quest.getQuestStage(access.player))
    }

    @Test
    fun `hand in consumes one of each part and leaves Ernest a chicken`() {
        val access = started()
        addParts(access.player)
        access.inv[3] = InvObj("obj.rubber_tube")
        assertTrue(progress.receiveParts(access))
        assertEquals(1, access.inv.count("obj.rubber_tube"))
        assertEquals(0, access.inv.count("obj.pressure_gauge"))
        assertEquals(0, access.inv.count("obj.oil_can"))
        assertTrue(progress.hasReceivedParts(access.player))
        assertEquals(1, access.player.vars["varp.haunted"])
        assertEquals(1, quest.getQuestStage(access.player))
        assertTrue(progress.receiveParts(access))
        assertEquals(1, access.inv.count("obj.rubber_tube"))
    }

    @Test
    fun `saved hand in resumes without requiring replacement parts`() {
        val original = started()
        addParts(original.player)
        assertTrue(progress.receiveParts(original))
        val resumed = access()
        resumed.player.vars.backing.putAll(original.player.vars.backing)
        resumed.player.attr[QUEST_STAGE_MAP_ATTR] =
            original.player.attr[QUEST_STAGE_MAP_ATTR]!!.toMutableMap()
        progress.foundErnest.set(resumed.player, progress.foundErnest.get(original.player))
        val restoredProgress = ErnestProgress(quest)
        assertFalse(restoredProgress.hasAllParts(resumed.player))
        assertTrue(restoredProgress.receiveParts(resumed))
        assertTrue(restoredProgress.canReceiveReward(resumed))
        runScene { restoredProgress.restoreErnest(resumed) {} }
        assertCompleted(resumed.player)
    }

    @Test
    fun `completion grants four quest points and 300 coins exactly once`() {
        val access = received()
        runScene {
            progress.restoreErnest(access) {
                assertEquals(quest.maxSteps, access.player.vars["varp.haunted"])
                assertEquals(1, quest.getQuestStage(access.player))
            }
        }
        assertCompleted(access.player)
        runScene { progress.restoreErnest(access) { error("Completion scene repeated") } }
        assertCompleted(access.player)
    }

    @Test
    fun `cancelling after the transformation still completes and rewards the quest`() {
        val access = received()
        var failure: Throwable? = null
        val scene: suspend () -> Unit = {
            progress.restoreErnest(access) { access.coroutine.pause { false } }
        }
        scene.startCoroutine(object : Continuation<Unit> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Unit>) {
                failure = result.exceptionOrNull()
            }
        })
        assertTrue(access.coroutine.isSuspended)
        assertFalse(quest.isQuestCompleted(access.player))
        access.coroutine.cancel()
        assertTrue(failure is CancellationException)
        assertCompleted(access.player)
        runScene { progress.restoreErnest(access) { error("Completion scene repeated") } }
        assertCompleted(access.player)
    }

    @Test
    fun `a full inventory after hand in can be recovered before the reward scene`() {
        val access = received()
        for (slot in access.inv.indices) {
            access.inv[slot] = InvObj("obj.bronze_dagger")
        }
        assertFalse(progress.canReceiveReward(access))
        assertFalse(quest.isQuestCompleted(access.player))
        assertTrue(progress.hasReceivedParts(access.player))
        access.inv[0] = null
        assertTrue(progress.canReceiveReward(access))
        assertEquals(0, access.inv.count("obj.coins"))
        runScene { progress.restoreErnest(access) {} }
        assertCompleted(access.player)
    }

    private fun assertCompleted(player: Player) {
        assertEquals(quest.maxSteps, quest.getQuestStage(player))
        assertEquals(quest.maxSteps, player.vars["varp.haunted"])
        assertEquals(4, player.vars["varp.qp"])
        assertEquals(1, player.vars["varbit.quests_completed_count"])
        assertEquals(300, player.inv.count("obj.coins"))
    }

    private fun started(): ProtectedAccess = access().also {
        quest.advanceQuestStage(it)
        progress.foundErnest.set(it.player, true)
    }

    private fun received(): ProtectedAccess = started().also {
        addParts(it.player)
        assertTrue(progress.receiveParts(it))
    }

    private fun addParts(player: Player) {
        player.inv[0] = InvObj("obj.rubber_tube")
        player.inv[1] = InvObj("obj.pressure_gauge")
        player.inv[2] = InvObj("obj.oil_can")
    }

    private fun access(): ProtectedAccess {
        val player = Player()
        player.inv = player.invMap.getOrPut("inv.inv")
        val context = ProtectedAccessContext(
            getRandom = { error("Unexpected random access") },
            getEventBus = { eventBus },
            getNpcList = { error("Unexpected NPC access") },
            getPlayerList = { error("Unexpected player list access") },
            getCollision = { error("Unexpected collision access") },
            getAreaChecker = { error("Unexpected area access") },
            getAlignment = { error("Unexpected dialogue access") },
            getLocInteractions = { error("Unexpected location interaction") },
            getNpcInteractions = { error("Unexpected NPC interaction") },
            getPlayerInteractions = { error("Unexpected player interaction") },
            getHeldInteractions = { error("Unexpected held interaction") },
            getWornInteractions = { error("Unexpected worn interaction") },
            getMusicPlayer = { error("Unexpected music player access") },
            getMarketPrices = { error("Unexpected market access") },
            getInstantHitProcessor = { error("Unexpected hit processing") },
            getTeleportValidator = { error("Unexpected teleport") },
            getHitModifier = { error("Unexpected hit modification") },
        )
        return ProtectedAccess(player, GameCoroutine(), context)
    }

    private fun runScene(scene: suspend () -> Unit) {
        var completed: Result<Unit>? = null
        scene.startCoroutine(object : Continuation<Unit> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Unit>) {
                completed = result
            }
        })
        checkNotNull(completed) { "Test scene unexpectedly suspended" }.getOrThrow()
    }
}
