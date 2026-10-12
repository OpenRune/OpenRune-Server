package org.rsmod.content.areas.city.prifddinas

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
import org.rsmod.content.areas.city.prifddinas.npcs.Anwen
import org.rsmod.content.areas.city.prifddinas.npcs.Ilfeen
import org.rsmod.content.areas.city.prifddinas.npcs.shops.HefinInn
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
class PrifddinasPurchasesInteractionTest {
    @Test fun `Eira sells each item for its price`() {
        for ((option, obj, price) in DRINKS) {
            val f = Fixture()
            f.player.inv[0] = InvObj(COINS, 25)
            f.talkTo(EIRA)
            f.finish(listOf(option, 1))
            assertEquals(25 - price, f.player.inv.count(COINS), obj)
            assertEquals(1, f.player.inv.count(obj), obj)
        }
    }

    @Test fun `Eira sells nothing to a player short of coins`() {
        for ((option, obj, price) in DRINKS) {
            val f = Fixture()
            f.player.inv[0] = InvObj(COINS, price - 1)
            f.talkTo(EIRA)
            f.finish(listOf(option, 1))
            assertEquals(price - 1, f.player.inv.count(COINS), obj)
            assertEquals(0, f.player.inv.count(obj), obj)
        }
    }

    @Test fun `declining Eira costs nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 25)
        f.talkTo(EIRA)
        f.finish(listOf(3, 2))
        assertEquals(25, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count("obj.stew"))
    }

    @Test fun `a full pack drops Eira's stew instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 30)
        f.talkTo(EIRA)
        f.finish(listOf(3, 1))
        assertEquals(10, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count("obj.stew"))
        assertEquals(1, f.groundCount("obj.stew"))
    }

    @Test fun `spending the last coins frees a slot for Eira's beer`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 2)
        f.talkTo(EIRA)
        f.finish(listOf(1, 1))
        assertEquals(0, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count("obj.beer"))
        assertEquals(0, f.groundCount("obj.beer"))
    }

    @Test fun `Anwen buys one silk at the agreed price`() {
        for ((options, price) in
            listOf(
                listOf(1) to 20,
                listOf(2, 1) to 40,
                listOf(2, 2) to 50,
                listOf(3, 1) to 50,
                listOf(3, 2) to 60,
            )) {
            val f = Fixture()
            f.player.inv[0] = InvObj(SILK, 1)
            f.player.inv[1] = InvObj(SILK, 1)
            f.talkTo(ANWEN)
            f.finish(options)
            assertEquals(price, f.player.inv.count(COINS), options.toString())
            assertEquals(1, f.player.inv.count(SILK), options.toString())
        }
    }

    @Test fun `refusing Anwen's offer keeps the silk`() {
        for (options in listOf(listOf(2, 3), listOf(3, 3), listOf(4))) {
            val f = Fixture()
            f.player.inv[0] = InvObj(SILK, 1)
            f.talkTo(ANWEN)
            f.finish(options)
            assertEquals(0, f.player.inv.count(COINS), options.toString())
            assertEquals(1, f.player.inv.count(SILK), options.toString())
        }
    }

    @Test fun `Anwen does not buy noted silk`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(NOTED_SILK, 5)
        f.talkTo(ANWEN)
        f.finish(emptyList())
        assertEquals(0, f.player.inv.count(COINS))
        assertEquals(5, f.player.inv.count(NOTED_SILK))
    }

    @Test fun `selling silk from a full pack keeps every coin`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(SILK, 1)
        f.talkTo(ANWEN)
        f.finish(listOf(1))
        assertEquals(20, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(SILK))
        assertEquals(0, f.groundCount(COINS))
    }

    @Test fun `Ilfeen hands over one crystal singing book`() {
        val f = Fixture()
        f.talkTo(ILFEEN)
        f.finish(listOf(1))
        assertEquals(1, f.player.inv.count(BOOK))
    }

    @Test fun `Ilfeen gives no second book`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(BOOK, 1)
        f.talkTo(ILFEEN)
        f.finish(listOf(1))
        assertEquals(1, f.player.inv.count(BOOK))
    }

    @Test fun `declining Ilfeen's book gives nothing`() {
        val f = Fixture()
        f.talkTo(ILFEEN)
        f.finish(listOf(2))
        assertEquals(0, f.player.inv.count(BOOK))
    }

    @Test fun `a full pack drops Ilfeen's book instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.talkTo(ILFEEN)
        f.finish(listOf(1))
        assertEquals(0, f.player.inv.count(BOOK))
        assertEquals(1, f.groundCount(BOOK))
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("prifddinas-purchases-test")
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
                coords = CoordGrid(3260, 6100, 0)
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
            with(HefinInn(objRepo)) { scripts.startup() }
            with(Anwen(objRepo)) { scripts.startup() }
            with(Ilfeen(objRepo)) { scripts.startup() }
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
        private const val COINS = "obj.coins"
        private const val SILK = "obj.silk"
        private const val NOTED_SILK = "obj.cert_silk"
        private const val BOOK = "obj.roving_crystal_book"
        private const val EIRA = "npc.prif_bartender"
        private const val ANWEN = "npc.prif_silk"
        private const val ILFEEN = "npc.ilfeen_prif"
        private val CONTINUE_COMPONENTS =
            mapOf(
                "interface.chat_left" to "component.chat_left:continue",
                "interface.chat_right" to "component.chat_right:continue",
                "interface.messagebox" to "component.messagebox:continue",
                "interface.objectbox" to "component.objectbox:universe",
            )
        private val DRINKS =
            listOf(
                Triple(1, "obj.beer", 2),
                Triple(2, "obj.cider", 2),
                Triple(3, "obj.stew", 20),
                Triple(4, "obj.prif_elven_dawn", 6),
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
