package org.rsmod.content.quest.area.draynor.vampyreslayer

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class VampyreSlayerInteractionTest {
    @Test fun `the first talk with Harlow only records the meeting`() {
        val f = Fixture(stage = 1)
        f.talk()
        f.finish(listOf(1))
        assertEquals(1, f.player.vars["varbit.vampyre_slayer_met_harlow"])
        assertEquals(1, f.script.quest.getQuestStage(f.player))
        assertEquals(0, f.player.inv.count(STAKE))
    }

    @Test fun `handing Harlow a beer swaps exactly one beer for one stake`() {
        val f = Fixture(stage = 1, metHarlow = true)
        f.player.inv[0] = InvObj(BEER, 1)
        f.player.inv[1] = InvObj(BEER, 1)
        f.talk()
        f.finish(listOf(1, 3))
        assertEquals(1, f.player.inv.count(BEER))
        assertEquals(1, f.player.inv.count(STAKE))
        assertEquals(2, f.script.quest.getQuestStage(f.player))
        assertEquals(1, f.player.vars["varbit.harlow_given_beer"])
    }

    @Test fun `a full inventory still receives the stake in the beer's slot`() {
        val f = Fixture(stage = 1, metHarlow = true)
        f.player.inv[0] = InvObj(BEER, 1)
        for (slot in 1 until 28) f.player.inv[slot] = InvObj("obj.bronze_dagger", 1)
        f.talk()
        f.finish(listOf(1, 3))
        assertEquals(0, f.player.inv.count(BEER))
        assertEquals(1, f.player.inv.count(STAKE))
        assertEquals(27, f.player.inv.count("obj.bronze_dagger"))
        assertEquals(2, f.script.quest.getQuestStage(f.player))
    }

    @Test fun `without a beer Harlow takes nothing and gives nothing`() {
        val f = Fixture(stage = 1, metHarlow = true)
        f.talk()
        f.finish(listOf(1))
        assertEquals(0, f.player.inv.count(STAKE))
        assertEquals(1, f.script.quest.getQuestStage(f.player))
    }

    @Test fun `a lost stake is replaced once and never duplicated`() {
        val f = Fixture(stage = 2)
        f.talk()
        f.finish(listOf(4))
        assertEquals(1, f.player.inv.count(STAKE))
        f.talk()
        f.finish(listOf(4))
        assertEquals(1, f.player.inv.count(STAKE))
    }

    @Test fun `a full inventory leaves the lost stake pending`() {
        val f = Fixture(stage = 2)
        for (slot in 0 until 28) f.player.inv[slot] = InvObj("obj.bronze_dagger", 1)
        f.talk()
        f.finish()
        assertEquals(0, f.player.inv.count(STAKE))
        assertEquals(28, f.player.inv.count("obj.bronze_dagger"))
        f.player.inv[0] = null
        f.talk()
        f.finish(listOf(4))
        assertEquals(1, f.player.inv.count(STAKE))
    }

    private class Fixture(stage: Int = 0, metHarlow: Boolean = false) {
        val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("vampyre-slayer-test")
        private var result: Result<Unit>? = null
        private val context = ProtectedAccessContextFactory.empty().copy(
            getEventBus = { events }, getAlignment = { TextAlignment() },
            getNpcInteractions = { NpcInteractions(events) },
        )

        @OptIn(InternalApi::class)
        val player = Player().apply {
            this.client = this@Fixture.client
            uuid = 789L
            slotId = 1
            assignUid()
            coords = CoordGrid(3222, 3398, 0)
            currentMapClock = 100
            processedMapClock = 100
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
        }

        val script = VampyreSlayerQuest()

        init {
            with(script) { ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup() }
            VarPlayerIntMapSetter.set(player, "varbit.vampyre_slayer_progress", stage)
            if (metHarlow) VarPlayerIntMapSetter.set(player, "varbit.vampyre_slayer_met_harlow", 1)
        }

        fun access() = ProtectedAccess(player, coroutine, context)

        fun talk() {
            while (player.isDelayed) {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
            }
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            val npc = Npc("npc.dr_harlow", player.coords.translateZ(1))
            val block: suspend () -> Unit = {
                val op = NpcEvents.Op1(npc)
                assertTrue(events.publish(access(), op))
            }
            block.startCoroutine(object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) { this@Fixture.result = result }
            })
            result?.getOrThrow()
        }

        fun until(predicate: () -> Boolean) {
            repeat(150) {
                if (predicate()) return
                advance()
            }
            fail<Unit>("Condition was not reached: ${output()}")
        }

        fun finish(options: List<Int> = emptyList()) {
            val selections = options.iterator()
            repeat(150) {
                if (coroutine.isIdle) return
                advance(selections)
            }
            fail<Unit>("Interaction did not finish: ${output()}")
        }

        private fun advance(options: Iterator<Int> = emptyList<Int>().iterator()) {
            if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
                val parent = listOf("chat_left", "chat_right", "messagebox", "objectbox", "chatmenu")
                    .firstOrNull { player.ui.containsModal("interface.$it") }
                    ?: error("Unknown dialogue: ${output()}")
                val input = if (parent == "chatmenu") {
                    ResumePauseButtonInput("component.chatmenu:options", if (options.hasNext()) options.next() else 1)
                } else if (parent == "objectbox") {
                    ResumePauseButtonInput("component.objectbox:universe", 0)
                } else ResumePauseButtonInput("component.$parent:continue", -1)
                coroutine.resumeWith(input)
            } else {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
                coroutine.advance()
            }
            result?.getOrThrow()
        }

        fun cancel() {
            coroutine.cancel()
            assertInstanceOf(CancellationException::class.java, result?.exceptionOrNull())
            result = null
            player.activeCoroutine = null
        }

        fun output() = client.messages.joinToString("\n")
    }

    private class RecordingClient : Client<Any, Any> {
        val messages = mutableListOf<Any>()
        override fun write(message: Any) { messages += message }
        override fun close() {}
        override fun read(player: Player) {}
        override fun flush() {}
        override fun flushHighPriority() {}
        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
        private const val STAKE = "obj.stake"
        private const val BEER = "obj.beer"
        private val restored = mutableListOf<() -> Unit>()

        @OptIn(InternalApi::class)
        @JvmStatic @BeforeAll fun cache() {
            ServerCacheManager.init(240).close()
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
