package org.rsmod.content.areas.city.portphasmatys

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
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
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.areas.city.portphasmatys.npcs.GreenGhost
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
class GreenGhostInteractionTest {
    @Test fun `the innkeeper gives one bedsheet for Robin`() {
        val f = Fixture()
        f.wearGhostspeak()
        f.talkTo(INNKEEPER)
        f.finish(TAKE_JOB)
        assertEquals(1, f.player.inv.count(BEDSHEET))
    }

    @Test fun `the innkeeper gives no second bedsheet`() {
        val f = Fixture()
        f.wearGhostspeak()
        f.player.inv[0] = InvObj(BEDSHEET, 1)
        f.talkTo(INNKEEPER)
        f.finish(TAKE_JOB)
        assertEquals(1, f.player.inv.count(BEDSHEET))
    }

    @Test fun `declining the job gives no bedsheet`() {
        val f = Fixture()
        f.wearGhostspeak()
        f.talkTo(INNKEEPER)
        f.finish(listOf(4, 2))
        assertEquals(0, f.player.inv.count(BEDSHEET))
    }

    @Test fun `the innkeeper gives nothing without ghostspeak`() {
        val f = Fixture()
        f.talkTo(INNKEEPER)
        f.finish(TAKE_JOB)
        assertEquals(0, f.player.inv.count(BEDSHEET))
    }

    @Test fun `a full pack drops the bedsheet instead of losing it`() {
        val f = Fixture()
        f.wearGhostspeak()
        f.fillInventory()
        f.talkTo(INNKEEPER)
        f.finish(TAKE_JOB)
        assertEquals(0, f.player.inv.count(BEDSHEET))
        assertEquals(1, f.groundCount(BEDSHEET))
    }

    @Test fun `Metarialus fills a pot with ale yeast for 5 ectotokens`() {
        val f = Fixture()
        f.wearGhostspeak()
        f.player.inv[0] = InvObj(ECTOTOKEN, 12)
        f.player.inv[1] = InvObj(POT, 1)
        f.player.inv[2] = InvObj(POT, 1)
        f.talkTo(METARIALUS)
        f.finish(BUY_YEAST)
        assertEquals(7, f.player.inv.count(ECTOTOKEN))
        assertEquals(1, f.player.inv.count(POT))
        assertEquals(1, f.player.inv.count(ALE_YEAST))
    }

    @Test fun `Metarialus takes nothing without a pot`() {
        val f = Fixture()
        f.wearGhostspeak()
        f.player.inv[0] = InvObj(ECTOTOKEN, 12)
        f.talkTo(METARIALUS)
        f.finish(BUY_YEAST)
        assertEquals(12, f.player.inv.count(ECTOTOKEN))
        assertEquals(0, f.player.inv.count(ALE_YEAST))
    }

    @Test fun `Metarialus takes nothing from a player short of ectotokens`() {
        val f = Fixture()
        f.wearGhostspeak()
        f.player.inv[0] = InvObj(ECTOTOKEN, 4)
        f.player.inv[1] = InvObj(POT, 1)
        f.talkTo(METARIALUS)
        f.finish(BUY_YEAST)
        assertEquals(4, f.player.inv.count(ECTOTOKEN))
        assertEquals(1, f.player.inv.count(POT))
        assertEquals(0, f.player.inv.count(ALE_YEAST))
    }

    @Test fun `a full pack still swaps the pot for ale yeast`() {
        val f = Fixture()
        f.wearGhostspeak()
        f.fillInventory()
        f.player.inv[0] = InvObj(ECTOTOKEN, 5)
        f.player.inv[1] = InvObj(POT, 1)
        f.talkTo(METARIALUS)
        f.finish(BUY_YEAST)
        assertEquals(0, f.player.inv.count(ECTOTOKEN))
        assertEquals(0, f.player.inv.count(POT))
        assertEquals(1, f.player.inv.count(ALE_YEAST))
        assertEquals(0, f.groundCount(ALE_YEAST))
    }

    @Test fun `Metarialus sells nothing without ghostspeak`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(ECTOTOKEN, 12)
        f.player.inv[1] = InvObj(POT, 1)
        f.talkTo(METARIALUS)
        f.finish(BUY_YEAST)
        assertEquals(12, f.player.inv.count(ECTOTOKEN))
        assertEquals(0, f.player.inv.count(ALE_YEAST))
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("green-ghost-test")
        private var result: Result<Unit>? = null
        private val clock = MapClock(100)
        private val objRegistry = ObjRegistry(ZoneUpdateMap())
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
                uuid = 794L
                observerUUID = 794L
                slotId = 1
                assignUid()
                coords = CoordGrid(3680, 3496, 0)
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
                invMap["inv.inv"] = inv
                invMap["inv.worn"] = worn
            }

        init {
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            val objRepo = ObjRepository(clock, objRegistry)
            with(GreenGhost(objRepo)) { scripts.startup() }
        }

        fun wearGhostspeak() {
            player.worn[2] = InvObj("obj.amulet_of_ghostspeak", 1)
        }

        fun fillInventory() {
            for (slot in 0 until 28) player.inv[slot] = InvObj("obj.bronze_dagger", 1)
        }

        fun groundCount(obj: String): Int =
            objRegistry.findAll(player.coords).filter { it.type == obj.asRSCM() }.sumOf { it.count }

        fun talkTo(npc: String) {
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            val target = Npc(npc, player.coords.translateZ(1))
            val body: suspend () -> Unit = {
                val access = ProtectedAccess(player, coroutine, context)
                assertTrue(events.publish(access, NpcEvents.Op1(target)))
            }
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

        fun finish(options: List<Int>) {
            val selections = options.iterator()
            repeat(100) {
                if (coroutine.isIdle) return
                advance(selections)
            }
            fail<Unit>("Interaction did not finish: ${client.messages.joinToString("\n")}")
        }

        private fun advance(options: Iterator<Int>) {
            if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
                val input =
                    if (player.ui.containsModal("interface.chatmenu")) {
                        ResumePauseButtonInput(
                            "component.chatmenu:options",
                            if (options.hasNext()) options.next() else 1,
                        )
                    } else {
                        val component =
                            CONTINUE_COMPONENTS.entries
                                .firstOrNull { player.ui.containsModal(it.key) }
                                ?.value
                                ?: error("Unknown dialogue: ${client.messages.joinToString("\n")}")
                        ResumePauseButtonInput(component, -1)
                    }
                coroutine.resumeWith(input)
            } else {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
                coroutine.advance()
            }
            result?.getOrThrow()
        }
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
        private const val INNKEEPER = "npc.ahoy_ghost_innkeeper"
        private const val METARIALUS = "npc.metarialus"
        private const val BEDSHEET = "obj.ahoy_bedsheet"
        private const val ECTOTOKEN = "obj.ectotoken"
        private const val POT = "obj.pot_empty"
        private const val ALE_YEAST = "obj.ale_yeast"
        private val TAKE_JOB = listOf(4, 1)
        private val BUY_YEAST = listOf(4, 5)
        private val CONTINUE_COMPONENTS =
            mapOf(
                "interface.chat_left" to "component.chat_left:continue",
                "interface.chat_right" to "component.chat_right:continue",
                "interface.messagebox" to "component.messagebox:continue",
                "interface.objectbox" to "component.objectbox:universe",
            )
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
