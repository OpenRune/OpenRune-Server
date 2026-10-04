package org.rsmod.content.quest.area.lumbridge

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.attr.AttributeKey
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
import org.rsmod.content.quest.manager.QUEST_STAGE_MAP_ATTR
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
class CooksAssistantInteractionTest {
    @Test fun `accepting stores native progress without the legacy stage attribute`() {
        val f = Fixture()
        f.talk()
        f.finish(listOf(1, 1))
        assertEquals(1, f.script.quest.getQuestStage(f.player))
        assertEquals(1, f.player.vars["varp.cookquest"])
        assertNull(f.player.attr[QUEST_STAGE_MAP_ATTR]?.get("quest_cooksassistant"))
    }

    @Test fun `declining leaves the quest unstarted`() {
        val f = Fixture()
        f.talk()
        f.finish(listOf(1, 2))
        assertEquals(0, f.script.quest.getQuestStage(f.player))
        assertEquals(0, f.player.vars["varp.cookquest"])
    }

    @Test fun `a partial hand-in keeps progress and records each ingredient`() {
        val f = Fixture(stage = 1)
        f.player.inv[0] = InvObj("obj.bucket_milk", 1)
        f.talk()
        f.finish()
        assertEquals(0, f.player.inv.count("obj.bucket_milk"))
        assertEquals(1, f.player.vars["varbit.cooks_assistant_milk"])
        assertEquals(0, f.player.vars["varbit.cooks_assistant_egg"])
        assertEquals(1, f.script.quest.getQuestStage(f.player))

        f.player.inv[0] = InvObj("obj.egg", 1)
        f.player.inv[1] = InvObj("obj.pot_flour", 1)
        f.talk()
        f.finish()
        f.assertReward()
    }

    @Test fun `bringing every ingredient before accepting completes the quest`() {
        val f = Fixture()
        f.ingredients()
        f.talk()
        f.finish(listOf(1, 1))
        f.assertReward()
    }

    @Test fun `talking again after completion does not reward twice`() {
        val f = Fixture(stage = 1)
        f.ingredients()
        f.talk()
        f.finish()
        f.assertReward()
        f.talk()
        f.finish(listOf(1))
        f.assertReward()
    }

    @Test fun `legacy attribute progress migrates into the quest varbits`() {
        val f = Fixture()
        f.player.attr[QUEST_STAGE_MAP_ATTR] = mutableMapOf("quest_cooksassistant" to 1)
        f.player.attr[AttributeKey<Boolean>("quest.quest_cooksassistant.GIVEN_EGG")] = true

        f.script.quest.migrateLegacyStage(f.player)
        f.script.migrateLegacyIngredients(f.player)

        assertEquals(1, f.player.vars["varp.cookquest"])
        assertEquals(1, f.player.vars["varbit.cooks_assistant_egg"])
        assertEquals(0, f.player.vars["varbit.cooks_assistant_milk"])
        assertNull(f.player.attr[QUEST_STAGE_MAP_ATTR]?.get("quest_cooksassistant"))
    }

    private class Fixture(stage: Int = 0) {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("cooks-assistant-test")
        private var result: Result<Unit>? = null
        private val context = ProtectedAccessContextFactory.empty().copy(
            getEventBus = { events }, getAlignment = { TextAlignment() },
            getNpcInteractions = { NpcInteractions(events) },
        )

        @OptIn(InternalApi::class)
        val player = Player().apply {
            this.client = this@Fixture.client
            uuid = 790L
            slotId = 1
            assignUid()
            coords = CoordGrid(3207, 3214, 0)
            currentMapClock = 100
            processedMapClock = 100
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
        }

        val script = CooksAssistant()

        init {
            with(script) { ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup() }
            VarPlayerIntMapSetter.set(player, "varbit.cooks_assistant_progress", stage)
        }

        private fun access() = ProtectedAccess(player, coroutine, context)

        fun ingredients() {
            player.inv[0] = InvObj("obj.bucket_milk", 1)
            player.inv[1] = InvObj("obj.egg", 1)
            player.inv[2] = InvObj("obj.pot_flour", 1)
        }

        fun talk() {
            while (player.isDelayed) {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
            }
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            val npc = Npc("npc.cook", player.coords.translateZ(1))
            val block: suspend () -> Unit = { assertTrue(events.publish(access(), NpcEvents.Op1(npc))) }
            block.startCoroutine(object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) { this@Fixture.result = result }
            })
            result?.getOrThrow()
        }

        fun finish(options: List<Int> = emptyList()) {
            val selections = options.iterator()
            repeat(150) {
                if (coroutine.isIdle) return
                advance(selections)
            }
            fail<Unit>("Interaction did not finish: ${output()}")
        }

        private fun advance(options: Iterator<Int>) {
            if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
                val parent = listOf("chat_left", "chat_right", "messagebox", "chatmenu")
                    .firstOrNull { player.ui.containsModal("interface.$it") }
                    ?: error("Unknown dialogue: ${output()}")
                val input = if (parent == "chatmenu") {
                    ResumePauseButtonInput("component.chatmenu:options", if (options.hasNext()) options.next() else 1)
                } else ResumePauseButtonInput("component.$parent:continue", -1)
                coroutine.resumeWith(input)
            } else {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
                coroutine.advance()
            }
            result?.getOrThrow()
        }

        fun assertReward() {
            assertEquals(2, script.quest.getQuestStage(player))
            assertEquals(2, player.vars["varp.cookquest"])
            assertEquals(1, player.vars["varp.qp"])
            assertEquals(300, player.statMap.getXP("stat.cooking"))
            listOf("obj.bucket_milk", "obj.egg", "obj.pot_flour").forEach { assertEquals(0, player.inv.count(it)) }
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
        private val restored = mutableListOf<() -> Unit>()

        @OptIn(InternalApi::class)
        @JvmStatic @BeforeAll fun cache() {
            ServerCacheManager.init(241).close()
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
