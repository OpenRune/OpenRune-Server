package org.rsmod.content.quest.area.wizardstower

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
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.world.WorldRepository
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
class ImpCatcherInteractionTest {
    @Test fun `accepting Mizgog's request stores native progress without attributes`() {
        val f = Fixture()
        f.talk()
        f.finish(listOf(1, 1))
        assertEquals(1, f.script.quest.getQuestStage(f.player))
        assertEquals(1, f.player.vars["varp.imp"])
        assertEquals(0, f.player.vars["varp.qp"])
    }

    @Test fun `declining leaves the quest unstarted`() {
        val f = Fixture()
        f.talk()
        f.finish(listOf(1, 2))
        assertEquals(0, f.script.quest.getQuestStage(f.player))
    }

    @Test fun `threatening Mizgog does not start the quest`() {
        val f = Fixture()
        f.talk()
        f.finish(listOf(2))
        assertEquals(0, f.script.quest.getQuestStage(f.player))
    }

    @Test fun `partial bead collections are not consumed`() {
        val f = Fixture(1)
        f.player.inv[0] = InvObj(Beads[0], 1)
        f.talk()
        f.finish()
        assertEquals(1, f.player.inv.count(Beads[0]))
        assertEquals(1, f.script.quest.getQuestStage(f.player))
        assertEquals(0, f.player.statMap.getXP("stat.magic"))
    }

    @Test fun `the journal lists every carried bead without replacing other lines`() {
        for (mask in 0 until (1 shl Beads.size)) {
            val f = Fixture(1)
            Beads.forEachIndexed { index, bead ->
                if (mask and (1 shl index) != 0) f.player.inv[index] = InvObj(bead, 1)
            }
            val journal = f.script.questLog(f.access())
            assertTrue(journal.contains("I need to bring him"), "Missing objective for mask $mask")
            Beads.forEachIndexed { index, bead ->
                val colour = bead.removePrefix("obj.").removeSuffix("_bead")
                val line = "I have found the <red>$colour bead</red>."
                assertEquals(mask and (1 shl index) != 0, journal.contains(line), "Mask $mask: $colour")
            }
            f.player.inv.fillNulls()
            assertFalse(f.script.questLog(f.access()).contains("I have found the"))
        }
    }

    @Test fun `handing in the beads grants one amulet quest point and Magic reward`() {
        val f = Fixture(1)
        f.beads()
        f.talk()
        f.finish()
        f.assertReward()
        assertTrue(f.player.ui.containsModal("interface.questscroll"))
        f.talk()
        f.finish(listOf(1))
        f.assertReward()
    }

    @Test fun `bringing the beads before accepting completes the same hand-in`() {
        val f = Fixture()
        f.beads()
        f.talk()
        f.finish(listOf(1, 1))
        f.assertReward()
    }

    @Test fun `a full inventory still has space after the four beads are removed`() {
        val f = Fixture(1)
        f.beads()
        for (slot in 4 until 28) f.player.inv[slot] = InvObj("obj.coins", 1)
        f.talk()
        f.finish()
        f.assertReward()
    }

    @Test fun `cancelling before bead removal leaves progress and inventory untouched`() {
        val f = Fixture(1)
        f.beads()
        f.talk()
        f.until { f.output().contains("You give four coloured beads") }
        f.cancel()
        assertEquals(1, f.script.quest.getQuestStage(f.player))
        Beads.forEach { assertEquals(1, f.player.inv.count(it)) }
        assertEquals(0, f.player.statMap.getXP("stat.magic"))
    }

    @Test fun `cancelling after bead removal completes rewards without a premature saved endstate`() {
        val f = Fixture(1)
        f.beads()
        f.talk()
        f.until { f.player.inv.count(Beads[0]) == 0 }
        assertEquals(1, f.script.quest.getQuestStage(f.player))
        assertEquals(1, f.player.vars["varp.imp"])
        assertTrue(f.output().contains("VarpSmall(id=160, value=2)"), f.output())
        f.cancel()
        f.assertReward()
        f.talk()
        f.finish(listOf(1))
        f.assertReward()
    }

    @Test fun `an interrupted inventory change never removes only some beads`() {
        val f = Fixture(1)
        f.beads()
        f.talk()
        f.until { f.output().contains("You give four coloured beads") }
        f.player.inv[0] = null
        f.finish()
        Beads.drop(1).forEach { assertEquals(1, f.player.inv.count(it)) }
        assertEquals(1, f.script.quest.getQuestStage(f.player))
        assertEquals(0, f.player.inv.count("obj.amulet_of_accuracy"))
        assertTrue(f.output().contains("You no longer have all four beads."))
    }

    @Test fun `the purchase option cannot exchange beads before quest completion`() {
        val f = Fixture()
        f.beads()
        f.talk(purchase = true)
        f.finish(listOf(2))
        assertEquals(0, f.script.quest.getQuestStage(f.player))
        assertEquals(0, f.player.inv.count("obj.amulet_of_accuracy"))
        Beads.forEach { assertEquals(1, f.player.inv.count(it)) }
    }

    @Test fun `post-quest exchange consumes exactly one of each bead without extra XP`() {
        val f = Fixture(2)
        f.beads(count = 2)
        f.talk(purchase = true)
        f.finish()
        Beads.forEach { assertEquals(1, f.player.inv.count(it)) }
        assertEquals(1, f.player.inv.count("obj.amulet_of_accuracy"))
        assertEquals(0, f.player.statMap.getXP("stat.magic"))
        assertEquals(0, f.player.vars["varp.qp"])
    }

    @Test fun `Grayzag dialogue follows real quest progress`() {
        for (stage in 0..2) {
            val f = Fixture(stage)
            f.talk(grayzag = true)
            f.finish()
            val text = when (stage) {
                0 -> "Not now, I'm trying to concentrate"
                1 -> "Good luck. Ha!"
                else -> "Well yes, actually."
            }
            assertTrue(f.output().contains(text), f.output())
            assertEquals(stage, f.script.quest.getQuestStage(f.player))
        }
    }

    private class Fixture(stage: Int = 0) {
        val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("imp-catcher-test")
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
            coords = CoordGrid(3103, 3162, 2)
            currentMapClock = 100
            processedMapClock = 100
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
        }

        val script = ImpCatcher(WorldRepository(ZoneUpdateMap()))

        init {
            with(script) { ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup() }
            VarPlayerIntMapSetter.set(player, "varp.imp", stage)
        }

        fun access() = ProtectedAccess(player, coroutine, context)

        fun beads(count: Int = 1) {
            var slot = 0
            repeat(count) { Beads.forEach { player.inv[slot++] = InvObj(it, 1) } }
        }

        fun talk(purchase: Boolean = false, grayzag: Boolean = false) {
            while (player.isDelayed) {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
            }
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            val npc = Npc(if (grayzag) "npc.wizard_grayzag" else "npc.wizard_mizgog", player.coords.translateZ(1))
            val block: suspend () -> Unit = {
                val op = if (purchase) NpcEvents.Op3(npc) else NpcEvents.Op1(npc)
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

        fun cancel() {
            coroutine.cancel()
            assertInstanceOf(CancellationException::class.java, result?.exceptionOrNull())
            result = null
            player.activeCoroutine = null
        }

        fun assertReward() {
            assertEquals(2, script.quest.getQuestStage(player))
            assertEquals(2, player.vars["varp.imp"])
            assertEquals(1, player.vars["varp.qp"])
            assertEquals(875, player.statMap.getXP("stat.magic"))
            assertEquals(1, player.inv.count("obj.amulet_of_accuracy"))
            Beads.forEach { assertEquals(0, player.inv.count(it)) }
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
        private val Beads = listOf("obj.black_bead", "obj.red_bead", "obj.white_bead", "obj.yellow_bead")
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
