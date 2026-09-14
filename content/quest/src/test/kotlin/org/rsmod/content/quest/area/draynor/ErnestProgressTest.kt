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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContext
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.content.quest.manager.QUEST_STAGE_MAP_ATTR
import org.rsmod.content.quest.manager.Quest
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ErnestProgressTest {
    private val eventBus = EventBus()
    private lateinit var quest: Quest
    private lateinit var script: ErnestTheChicken
    private lateinit var progress: ErnestProgress

    @BeforeAll
    fun loadCache() {
        ServerCacheManager.init(240).close()
        val context = ScriptContext(eventBus, CheatCommandMap(), EngineQueueCache())
        with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { context.startup() }
        val collision = CollisionFlagMap()
        val npcs = NpcList()
        val registry = NpcRegistry(npcs, collision, eventBus)
        script = ErnestTheChicken(
            WorldRepository(ZoneUpdateMap()),
            NpcRepository(MapClock(), registry, npcs),
            collision,
        )
        quest = script.quest
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

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3, 4, 5, 6, 7])
    fun `journal reports every held and missing machine part independently`(heldMask: Int) {
        val access = started()
        val parts = listOf("rubber_tube", "pressure_gauge", "oil_can")
        for ((index, part) in parts.withIndex()) {
            if (heldMask and (1 shl index) != 0) access.inv[index] = InvObj("obj.$part")
        }

        val journal = script.questLog(access)
        assertTrue(journal.contains("needs three parts for his machine"))
        for ((index, part) in parts.withIndex()) {
            val name = part.replace('_', ' ')
            val found = "I have found the <red>$name</red>."
            val missing = "I still need to find the <red>$name</red>."
            val held = heldMask and (1 shl index) != 0
            assertEquals(held, journal.contains(found), "$name found status for mask $heldMask")
            assertEquals(!held, journal.contains(missing), "$name missing status for mask $heldMask")
        }
    }

    @Test
    fun `journal waits for the professor introduction before showing machine parts`() {
        val access = access()
        quest.advanceQuestStage(access)
        addParts(access.player)
        val journal = script.questLog(access)
        assertTrue(journal.contains("I should speak to whoever lives in the manor."))
        assertFalse(journal.contains("I have found the"))
        assertFalse(journal.contains("I still need to find the"))
    }

    @Test
    fun `journal replaces collection objectives after hand in and records completion`() {
        val access = received()
        val handedIn = script.questLog(access)
        assertTrue(handedIn.contains("I gave all three parts to <red>Professor Oddenstein</red>."))
        assertFalse(handedIn.contains("I have found the"))
        assertFalse(handedIn.contains("I still need to find the"))

        runScene { progress.restoreErnest(access) {} }
        val completed = script.completedLog(access)
        assertTrue(completed.contains("I recovered the rubber tube, pressure gauge and oil can"))
        assertTrue(completed.contains("QUEST COMPLETE!"))
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
