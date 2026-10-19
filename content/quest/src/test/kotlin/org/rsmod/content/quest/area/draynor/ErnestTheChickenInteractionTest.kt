package org.rsmod.content.quest.area.draynor

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
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.content.quest.manager.QUEST_STAGE_MAP_ATTR
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class ErnestTheChickenInteractionTest {
    @Test fun `resuming with a full inventory keeps the reward pending until space is freed`() {
        val f = Fixture(partsReceived = true)
        f.fillInventory()
        f.talk()
        f.finish()
        f.assertPending()
        assertEquals(0, f.player.inv.count("obj.coins"))
        assertEquals(28, f.player.inv.count("obj.bronze_dagger"))

        f.player.inv[0] = null
        f.talk()
        f.finish()
        f.assertReward(300)
        assertEquals(27, f.player.inv.count("obj.bronze_dagger"))

        f.talk()
        f.finish()
        f.assertReward(300)
    }

    @Test fun `an overflowing coin stack refuses the reward without partial payment and allows retry`() {
        val f = Fixture(partsReceived = true)
        f.player.inv[0] = InvObj("obj.coins", Int.MAX_VALUE - 299)
        f.talk()
        f.finish()
        f.assertPending()
        assertEquals(Int.MAX_VALUE - 299, f.player.inv.count("obj.coins"))

        f.player.inv[0] = InvObj("obj.coins", 1)
        f.talk()
        f.finish()
        f.assertReward(301)

        f.talk()
        f.finish()
        f.assertReward(301)
    }

    @Test fun `a full inventory can receive the reward after handing in all three parts`() {
        val f = Fixture(partsReceived = false)
        f.fillInventory()
        Parts.forEachIndexed { slot, part -> f.player.inv[slot] = InvObj(part, 1) }
        f.talk()
        f.finish()
        f.assertReward(300)
        assertEquals(25, f.player.inv.count("obj.bronze_dagger"))
        Parts.forEach { assertEquals(0, f.player.inv.count(it)) }

        f.talk()
        f.finish()
        f.assertReward(300)
    }

    @Test fun `filling the inventory during the final dialogue keeps rewards pending on finish or cancellation`() {
        for (cancel in listOf(false, true)) {
            val f = Fixture(partsReceived = true)
            f.talk()
            f.untilFinalDialogue()
            f.fillInventory()
            if (cancel) f.cancel() else f.finish()
            f.assertPending()
            assertEquals(0, f.player.inv.count("obj.coins"))
            assertEquals(28, f.player.inv.count("obj.bronze_dagger"))

            f.player.inv[0] = null
            f.talk()
            f.finish()
            f.assertReward(300)
            f.talk()
            f.finish()
            f.assertReward(300)
        }
    }

    @Test fun `cancelling the final dialogue with capacity still grants the reward only once`() {
        val f = Fixture(partsReceived = true)
        f.talk()
        f.untilFinalDialogue()
        f.cancel()
        f.assertReward(300)
        f.talk()
        f.finish()
        f.assertReward(300)
    }

    private class Fixture(partsReceived: Boolean) {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("ernest-reward-test")
        private var result: Result<Unit>? = null
        private val collision = CollisionFlagMap()
        private val npcs = NpcList()
        private val npcRepo = NpcRepository(MapClock(100), NpcRegistry(npcs, collision, events), npcs)
        private val context = ProtectedAccessContextFactory.empty().copy(
            getEventBus = { events }, getAlignment = { TextAlignment() },
            getNpcInteractions = { NpcInteractions(events) },
        )

        @OptIn(InternalApi::class)
        val player = Player().apply {
            this.client = this@Fixture.client
            uuid = 793L
            slotId = 1
            assignUid()
            coords = CoordGrid(3110, 3367, 2)
            currentMapClock = 100
            processedMapClock = 100
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
        }

        val script = ErnestTheChicken(WorldRepository(ZoneUpdateMap()), npcRepo, collision)

        init {
            for (x in 3104..3119 step 8) for (z in 3360..3375 step 8) {
                collision.allocateIfAbsent(x, z, 2)
            }
            with(script) { ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup() }
            player.attr.getOrPut(QUEST_STAGE_MAP_ATTR) { mutableMapOf() }[script.quest.key] = 1
            VarPlayerIntMapSetter.set(player, "varp.haunted", 1)
            VarPlayerIntMapSetter.set(player, "varbit.ernesthandin_complete", if (partsReceived) 1 else 0)
            ErnestProgress(script.quest).foundErnest.set(player, true)
        }

        fun fillInventory() {
            for (slot in 0 until 28) player.inv[slot] = InvObj("obj.bronze_dagger", 1)
        }

        fun talk() {
            while (player.isDelayed) {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
            }
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            val npc = Npc("npc.professor_oddenstein", player.coords.translateX(1))
            val block: suspend () -> Unit = {
                assertTrue(events.publish(ProtectedAccess(player, coroutine, context), NpcEvents.Op1(npc)))
            }
            block.startCoroutine(object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) { this@Fixture.result = result }
            })
            result?.getOrThrow()
        }

        fun finish() {
            repeat(150) {
                if (coroutine.isIdle) return
                advance()
            }
            fail<Unit>("Interaction did not finish: ${output()}")
        }

        fun untilFinalDialogue() {
            repeat(150) {
                if (output().contains("Of course, of course.")) return
                advance()
            }
            fail<Unit>("Final dialogue was not reached: ${output()}")
        }

        fun cancel() {
            coroutine.cancel()
            assertInstanceOf(CancellationException::class.java, result?.exceptionOrNull())
            result = null
            player.activeCoroutine = null
        }

        private fun advance() {
            if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
                val parent = listOf("chat_left", "chat_right", "messagebox", "chatmenu")
                    .firstOrNull { player.ui.containsModal("interface.$it") }
                    ?: error("Unknown dialogue: ${output()}")
                val input = if (parent == "chatmenu") {
                    ResumePauseButtonInput("component.chatmenu:options", 1)
                } else ResumePauseButtonInput("component.$parent:continue", -1)
                coroutine.resumeWith(input)
            } else {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
                coroutine.advance()
            }
            result?.getOrThrow()
        }

        fun assertPending() {
            assertEquals(1, script.quest.getQuestStage(player))
            assertEquals(1, player.vars["varp.haunted"])
            assertEquals(1, player.vars["varbit.ernesthandin_complete"])
            assertEquals(0, player.vars["varp.qp"])
            assertEquals(0, player.vars["varbit.quests_completed_count"])
            assertFalse(player.ui.containsModal("interface.questscroll"))
        }

        fun assertReward(coins: Int) {
            assertEquals(script.quest.maxSteps, script.quest.getQuestStage(player))
            assertEquals(script.quest.maxSteps, player.vars["varp.haunted"])
            assertEquals(script.quest.questPoints, player.vars["varp.qp"])
            assertEquals(1, player.vars["varbit.quests_completed_count"])
            assertEquals(coins, player.inv.count("obj.coins"))
        }

        private fun output() = client.messages.joinToString("\n")
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
        private val Parts = listOf("obj.rubber_tube", "obj.pressure_gauge", "obj.oil_can")
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
