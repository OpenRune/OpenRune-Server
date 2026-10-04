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
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.hook.SpadeDigHook
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
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
class XMarksTheSpotInteractionTest {
    @Test fun `accepting the hunt hands over the first treasure scroll`() {
        val f = Fixture()
        f.talkToVeos()
        f.finish(listOf(2, 1, 1))
        assertEquals(2, f.stage())
        assertEquals(1, f.player.inv.count(BobsScroll))
        assertEquals(2, f.player.vars["varbit.veos_lumbridge_vis"])
    }

    @Test fun `declining leaves the quest unstarted`() {
        val f = Fixture()
        f.talkToVeos()
        f.finish(listOf(2, 1, 2))
        assertEquals(0, f.stage())
        assertEquals(0, f.player.inv.count(BobsScroll))
    }

    @Test fun `a full inventory defers the scroll until there is room`() {
        val f = Fixture()
        f.fillInventory()
        f.talkToVeos()
        f.finish(listOf(2, 1, 1))
        assertEquals(1, f.stage())
        f.player.inv[27] = null
        f.talkToVeos()
        f.finish()
        assertEquals(2, f.stage())
        assertEquals(1, f.player.inv.count(BobsScroll))
    }

    @Test fun `each dig spot only works at its own stage and swaps in the next clue`() {
        val steps =
            listOf(
                Triple(2, CoordGrid(3230, 3209, 0), "obj.cluequest_clue2"),
                Triple(3, CoordGrid(3203, 3212, 0), Orb),
                Triple(4, CoordGrid(3110, 3262, 0), "obj.cluequest_clue4"),
                Triple(5, CoordGrid(3078, 3259, 0), Casket),
            )
        for ((stage, spot, reward) in steps) {
            val f = Fixture(stage)
            f.player.coords = spot.translate(10, 0)
            assertFalse(f.script.claims(f.player), "stage $stage claimed the wrong tile")
            f.player.coords = spot
            assertTrue(f.script.claims(f.player), "stage $stage did not claim its tile")
            f.dig()
            assertEquals(stage + 1, f.stage())
            assertEquals(1, f.player.inv.count(reward))
        }
    }

    @Test fun `the orb reports temperature and whether it is getting warmer`() {
        val f = Fixture(4)
        f.player.coords = CoordGrid(3209, 3264, 0)
        f.held(Orb)
        f.player.coords = CoordGrid(3159, 3264, 0)
        f.held(Orb)
        f.player.coords = CoordGrid(3110, 3265, 0)
        f.held(Orb)
        val output = f.output()
        assertTrue(output.contains("The orb is warm."), output)
        assertTrue(output.contains("The orb is very hot, and warmer than last time."), output)
        assertTrue(output.contains("The orb is visibly shaking."), output)
    }

    @Test fun `Veos returns a lost clue without moving the quest on`() {
        val f = Fixture(3)
        f.talkToVeos()
        f.finish()
        assertEquals(3, f.stage())
        assertEquals(1, f.player.inv.count("obj.cluequest_clue2"))
    }

    @Test fun `delivering the casket completes the quest with every reward once`() {
        val f = Fixture(6)
        f.player.inv[0] = InvObj(Casket, 1)
        f.talkToVeosAtSarim()
        f.finish()
        f.assertReward()
        f.talkToVeosAtSarim(expectQuestDialogue = false)
        f.finish()
        f.assertReward()
        assertEquals(0, f.player.vars["varbit.veos_lumbridge_vis"])
    }

    @Test fun `the casket is kept when there is no room for the reward`() {
        val f = Fixture(6)
        f.fillInventory()
        f.player.inv[0] = InvObj(Casket, 1)
        f.talkToVeosAtSarim()
        f.finish()
        assertEquals(6, f.stage())
        assertEquals(1, f.player.inv.count(Casket))
    }

    @Test fun `the antique lamp grants experience in the chosen skill`() {
        val f = Fixture(8)
        f.player.inv[0] = InvObj(Lamp, 1)
        f.held(Lamp, lampChoice = 0)
        assertEquals(0, f.player.inv.count(Lamp))
        assertEquals(300, f.player.statMap.getXP("stat.attack"))
        assertEquals(1, f.player.vars["varbit.cluequest_lamp_reward"])
    }

    private class Fixture(stage: Int = 0) {
        val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("x-marks-test")
        private var result: Result<Unit>? = null
        private var lampChoice = 0
        private val context =
            ProtectedAccessContextFactory.empty()
                .copy(
                    getEventBus = { events },
                    getAlignment = { TextAlignment() },
                    getNpcInteractions = { NpcInteractions(events) },
                )

        @OptIn(InternalApi::class)
        val player =
            Player().apply {
                this.client = this@Fixture.client
                uuid = 791L
                slotId = 1
                assignUid()
                coords = CoordGrid(3228, 3241, 0)
                currentMapClock = 100
                processedMapClock = 100
                inv =
                    Inventory(
                        checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())),
                        arrayOfNulls(28),
                    )
                worn =
                    Inventory(
                        checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())),
                        arrayOfNulls(14),
                    )
            }

        val script = XMarksTheSpot(ObjRepository(MapClock(100), ObjRegistry(ZoneUpdateMap())))

        init {
            with(script) { ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup() }
            VarPlayerIntMapSetter.set(player, "varbit.cluequest", stage)
        }

        fun stage() = script.quest.getQuestStage(player)

        fun access() = ProtectedAccess(player, coroutine, context)

        fun fillInventory() {
            for (slot in 0 until 28) player.inv[slot] = InvObj("obj.bronze_dagger", 1)
        }

        fun talkToVeos() = start {
            assertTrue(events.publish(this, NpcEvents.Op1(Npc("npc.veos_lumbridge", coords))))
        }

        fun talkToVeosAtSarim(expectQuestDialogue: Boolean = true) = start {
            startDialogue(Npc("npc.veos_sarim", coords)) {
                with(script) { assertEquals(expectQuestDialogue, veosSarimQuest()) }
            }
        }

        fun dig() {
            start { with(script as SpadeDigHook) { dig() } }
            finish()
        }

        fun held(obj: String, lampChoice: Int = 0) {
            this.lampChoice = lampChoice
            val slot = (0 until 28).firstOrNull { player.inv[it]?.id == obj.asRSCM() } ?: 0
            if (player.inv[slot] == null) player.inv[slot] = InvObj(obj, 1)
            val type = checkNotNull(ServerCacheManager.getItem(obj.asRSCM()))
            start {
                val event = HeldObjEvents.Op1(slot, checkNotNull(inv[slot]), type, inv)
                assertTrue(events.publish(this, event))
            }
            finish()
        }

        private fun start(block: suspend ProtectedAccess.() -> Unit) {
            while (player.isDelayed) {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
            }
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            val body: suspend () -> Unit = { access().block() }
            body.startCoroutine(
                object : Continuation<Unit> {
                    override val context = EmptyCoroutineContext

                    override fun resumeWith(result: Result<Unit>) {
                        this@Fixture.result = result
                    }
                }
            )
            result?.getOrThrow()
        }

        fun finish(options: List<Int> = emptyList()) {
            val selections = options.iterator()
            repeat(300) {
                if (coroutine.isIdle) return
                advance(selections)
            }
            fail<Unit>("Interaction did not finish: ${output()}")
        }

        private fun advance(options: Iterator<Int>) {
            if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
                val input =
                    when {
                        player.ui.containsModal("interface.chatmenu") ->
                            ResumePauseButtonInput(
                                "component.chatmenu:options",
                                if (options.hasNext()) options.next() else 1,
                            )
                        player.ui.containsModal("interface.xpreward") ->
                            ResumePauseButtonInput("component.xpreward:universe", lampChoice)
                        player.ui.containsModal("interface.objectbox") ->
                            ResumePauseButtonInput("component.objectbox:universe", -1)
                        else -> {
                            val parent =
                                listOf("chat_left", "chat_right", "messagebox").firstOrNull {
                                    player.ui.containsModal("interface.$it")
                                } ?: error("Unknown dialogue: ${output()}")
                            ResumePauseButtonInput("component.$parent:continue", -1)
                        }
                    }
                coroutine.resumeWith(input)
            } else {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
                coroutine.advance()
            }
            result?.getOrThrow()
        }

        fun assertReward() {
            assertEquals(8, stage())
            assertEquals(1, player.vars["varp.qp"])
            assertEquals(200, player.inv.count("obj.coins"))
            assertEquals(1, player.inv.count(Lamp))
            assertEquals(1, player.inv.count("obj.league_clue_box_beginner"))
            assertEquals(0, player.inv.count(Casket))
        }

        fun output() = client.messages.joinToString("\n")
    }

    private class RecordingClient : Client<Any, Any> {
        val messages = mutableListOf<Any>()

        override fun write(message: Any) {
            messages += message
        }

        override fun close() {}

        override fun read(player: Player) {}

        override fun flush() {}

        override fun flushHighPriority() {}

        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
        private const val BobsScroll = "obj.cluequest_clue1"
        private const val Orb = "obj.cluequest_clue3"
        private const val Casket = "obj.cluequest_casket"
        private const val Lamp = "obj.cluequest_lamp"
        private val restored = mutableListOf<() -> Unit>()

        @OptIn(InternalApi::class)
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
            for ((owner, name) in
                listOf(
                    "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                    "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
                )) {
                val field =
                    Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                restored += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            restored += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic
        @AfterAll
        fun restore() {
            restored.asReversed().forEach { it() }
            restored.clear()
        }
    }
}
