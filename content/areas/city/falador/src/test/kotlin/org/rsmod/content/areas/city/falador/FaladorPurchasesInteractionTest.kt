package org.rsmod.content.areas.city.falador

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
import org.rsmod.content.areas.city.falador.npcs.FaladorBarsScript
import org.rsmod.content.areas.city.falador.npcs.FaladorTownsfolkScript
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
class FaladorPurchasesInteractionTest {
    @Test fun `Wyson sells two woad leaves for 20 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 25)
        f.talkTo(WYSON)
        f.finish(listOf(1, 4))
        assertEquals(5, f.player.inv.count(COINS))
        assertEquals(2, f.player.inv.count(WOAD_LEAF))
    }

    @Test fun `Wyson sells one woad leaf for 15 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 15)
        f.talkTo(WYSON)
        f.finish(listOf(1, 3))
        assertEquals(0, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(WOAD_LEAF))
    }

    @Test fun `Wyson keeps his leaves from a player short of coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 19)
        f.talkTo(WYSON)
        f.finish(listOf(1, 4))
        assertEquals(19, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(WOAD_LEAF))
    }

    @Test fun `a lowball offer or declining Wyson costs nothing`() {
        for (options in listOf(listOf(1, 1), listOf(1, 2), listOf(3))) {
            val f = Fixture()
            f.player.inv[0] = InvObj(COINS, 20)
            f.talkTo(WYSON)
            f.finish(options)
            assertEquals(20, f.player.inv.count(COINS), "$options")
            assertEquals(0, f.player.inv.count(WOAD_LEAF), "$options")
        }
    }

    @Test fun `a full pack drops the woad leaves instead of losing them`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 25)
        f.talkTo(WYSON)
        f.finish(listOf(1, 4))
        assertEquals(5, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(WOAD_LEAF))
        assertEquals(2, f.groundCount(WOAD_LEAF))
    }

    @Test fun `every Rising Sun barmaid sells each ale for 3 coins`() {
        for (barmaid in BARMAIDS) {
            for ((option, ale) in ALES) {
                val f = Fixture()
                f.player.inv[0] = InvObj(COINS, 5)
                f.talkTo(barmaid)
                f.finish(listOf(option))
                assertEquals(2, f.player.inv.count(COINS), "$barmaid $ale")
                assertEquals(1, f.player.inv.count(ale), "$barmaid $ale")
            }
        }
    }

    @Test fun `barmaids keep the ale from a player short of coins`() {
        for (barmaid in BARMAIDS) {
            val f = Fixture()
            f.player.inv[0] = InvObj(COINS, 2)
            f.talkTo(barmaid)
            f.finish(listOf(1))
            assertEquals(2, f.player.inv.count(COINS), barmaid)
            assertEquals(0, f.player.inv.count(ASGARNIAN_ALE), barmaid)
        }
    }

    @Test fun `declining the ales costs nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 5)
        f.talkTo(BARMAIDS.first())
        f.finish(listOf(4))
        assertEquals(5, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(ASGARNIAN_ALE))
    }

    @Test fun `a full pack drops the ale instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 5)
        f.talkTo(BARMAIDS.first())
        f.finish(listOf(1))
        assertEquals(2, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(ASGARNIAN_ALE))
        assertEquals(1, f.groundCount(ASGARNIAN_ALE))
    }

    @Test fun `barmaids buy every beer glass for 2 coins each`() {
        val f = Fixture()
        f.fillInventory()
        for (slot in 0 until 3) f.player.inv[slot] = InvObj(BEER_GLASS, 1)
        f.talkTo(BARMAIDS.first())
        f.finish(listOf(2, 1))
        assertEquals(0, f.player.inv.count(BEER_GLASS))
        assertEquals(6, f.player.inv.count(COINS))
        assertEquals(0, f.groundCount(COINS))
    }

    @Test fun `keeping the beer glasses costs nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(BEER_GLASS, 1)
        f.talkTo(BARMAIDS.first())
        f.finish(listOf(2, 2))
        assertEquals(1, f.player.inv.count(BEER_GLASS))
        assertEquals(0, f.player.inv.count(COINS))
    }

    @Test fun `Party Room barmaids sell a beer for 2 coins`() {
        for ((barmaid, options) in PARTY_BEER) {
            val f = Fixture()
            f.player.inv[0] = InvObj(COINS, 3)
            f.talkTo(barmaid)
            f.finish(options)
            assertEquals(1, f.player.inv.count(COINS), barmaid)
            assertEquals(1, f.player.inv.count(BEER), barmaid)
        }
    }

    @Test fun `Party Room barmaids keep the beer from a player short of coins`() {
        for ((barmaid, options) in PARTY_BEER) {
            val f = Fixture()
            f.player.inv[0] = InvObj(COINS, 1)
            f.talkTo(barmaid)
            f.finish(options)
            assertEquals(1, f.player.inv.count(COINS), barmaid)
            assertEquals(0, f.player.inv.count(BEER), barmaid)
        }
    }

    @Test fun `a full pack drops the Party Room beer instead of losing it`() {
        for ((barmaid, options) in PARTY_BEER) {
            val f = Fixture()
            f.fillInventory()
            f.player.inv[0] = InvObj(COINS, 3)
            f.talkTo(barmaid)
            f.finish(options)
            assertEquals(1, f.player.inv.count(COINS), barmaid)
            assertEquals(0, f.player.inv.count(BEER), barmaid)
            assertEquals(1, f.groundCount(BEER), barmaid)
        }
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("falador-purchases-test")
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
                uuid = 795L
                observerUUID = 795L
                slotId = 1
                assignUid()
                coords = CoordGrid(2958, 3372, 0)
                currentMapClock = 100
                processedMapClock = 100
                inv =
                    Inventory(
                        checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())),
                        arrayOfNulls(28),
                    )
            }

        init {
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            val objRepo = ObjRepository(clock, objRegistry)
            with(FaladorTownsfolkScript(objRepo)) { scripts.startup() }
            with(FaladorBarsScript(objRepo)) { scripts.startup() }
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
                    } else if (player.ui.containsModal("interface.objectbox")) {
                        ResumePauseButtonInput("component.objectbox:universe", -1)
                    } else {
                        val parent =
                            listOf("chat_left", "chat_right", "messagebox").firstOrNull {
                                player.ui.containsModal("interface.$it")
                            } ?: error("Unknown dialogue: ${client.messages.joinToString("\n")}")
                        ResumePauseButtonInput("component.$parent:continue", -1)
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
        private const val BEER = "obj.beer"
        private const val BEER_GLASS = "obj.beer_glass"
        private const val WOAD_LEAF = "obj.woadleaf"
        private const val ASGARNIAN_ALE = "obj.asgarnian_ale"
        private const val WYSON = "npc.wyson"
        private val BARMAIDS =
            listOf("npc.risingsun_barmaid", "npc.risingsun_barmaid2", "npc.risingsun_barmaid3")
        private val ALES =
            listOf(1 to ASGARNIAN_ALE, 2 to "obj.wizards_mind_bomb", 3 to "obj.dwarven_stout")
        private val PARTY_BEER =
            mapOf("npc.partyroom_barmaid" to listOf(1), "npc.partyroom_barmaid2" to listOf())
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
