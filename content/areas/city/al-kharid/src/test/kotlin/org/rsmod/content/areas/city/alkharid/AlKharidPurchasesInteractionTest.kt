package org.rsmod.content.areas.city.alkharid

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
import org.rsmod.api.shops.Shops
import org.rsmod.content.areas.city.alkharid.npcs.AliTheLeafletDropper
import org.rsmod.content.areas.city.alkharid.npcs.ChancellorHassan
import org.rsmod.content.areas.city.alkharid.npcs.Karim
import org.rsmod.content.areas.city.alkharid.npcs.Shantay
import org.rsmod.content.areas.city.alkharid.npcs.SilkTrader
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
class AlKharidPurchasesInteractionTest {
    @Test fun `Karim sells a kebab for 1 coin`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 5)
        f.talkTo(KARIM)
        f.finish(listOf(2))
        assertEquals(4, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(KEBAB))
    }

    @Test fun `Karim sells nothing to a player without coins`() {
        val f = Fixture()
        f.talkTo(KARIM)
        f.finish(listOf(2))
        assertEquals(0, f.player.inv.count(KEBAB))
        assertEquals(0, f.groundCount(KEBAB))
    }

    @Test fun `declining Karim costs nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 5)
        f.talkTo(KARIM)
        f.finish(listOf(1))
        assertEquals(5, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(KEBAB))
    }

    @Test fun `a full pack drops the kebab instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 2)
        f.talkTo(KARIM)
        f.finish(listOf(2))
        assertEquals(1, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(KEBAB))
        assertEquals(1, f.groundCount(KEBAB))
    }

    @Test fun `the silk trader sells silk for 3 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 10)
        f.talkTo(SILK_TRADER)
        f.finish(listOf(1, 2))
        assertEquals(7, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(SILK))
    }

    @Test fun `haggling with the silk trader lowers the price to 2 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 10)
        f.talkTo(SILK_TRADER)
        f.finish(listOf(1, 1, 1))
        assertEquals(8, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(SILK))
    }

    @Test fun `the silk trader keeps the silk from a player short of coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 2)
        f.talkTo(SILK_TRADER)
        f.finish(listOf(1, 2))
        assertEquals(2, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(SILK))
    }

    @Test fun `refusing the haggled silk costs nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 10)
        f.talkTo(SILK_TRADER)
        f.finish(listOf(1, 1, 2))
        assertEquals(10, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(SILK))
    }

    @Test fun `a full pack drops the silk instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 5)
        f.talkTo(SILK_TRADER)
        f.finish(listOf(1, 2))
        assertEquals(2, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(SILK))
        assertEquals(1, f.groundCount(SILK))
    }

    @Test fun `Hassan hands out a jug of water`() {
        val f = Fixture()
        f.talkTo(HASSAN)
        f.finish(listOf(2, 4))
        assertEquals(1, f.player.inv.count(JUG_OF_WATER))
    }

    @Test fun `a full pack drops Hassan's water instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.talkTo(HASSAN)
        f.finish(listOf(2, 4))
        assertEquals(0, f.player.inv.count(JUG_OF_WATER))
        assertEquals(1, f.groundCount(JUG_OF_WATER))
    }

    @Test fun `the leaflet dropper hands out one flyer`() {
        val f = Fixture()
        f.talkTo(LEAFLET_DROPPER)
        f.finish(listOf(2))
        assertEquals(1, f.player.inv.count(FLYER))
        f.talkTo(LEAFLET_DROPPER)
        f.finish(listOf(2))
        assertEquals(1, f.player.inv.count(FLYER))
    }

    @Test fun `the leaflet dropper keeps the flyer from a full pack`() {
        val f = Fixture()
        f.fillInventory()
        f.talkTo(LEAFLET_DROPPER)
        f.finish(listOf(2))
        assertEquals(0, f.player.inv.count(FLYER))
        assertEquals(0, f.groundCount(FLYER))
    }

    @Test fun `Shantay sells a pass for 5 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 12)
        f.talkTo(SHANTAY, op = 4)
        f.finish(emptyList())
        assertEquals(7, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(SHANTAY_PASS))
    }

    @Test fun `Shantay sells a pass through the talk menu`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 5)
        f.talkTo(SHANTAY)
        f.finish(listOf(4))
        assertEquals(0, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(SHANTAY_PASS))
    }

    @Test fun `Shantay keeps the pass from a player short of coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 4)
        f.talkTo(SHANTAY, op = 4)
        f.finish(emptyList())
        assertEquals(4, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(SHANTAY_PASS))
    }

    @Test fun `Shantay takes nothing from a full pack`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 10)
        f.talkTo(SHANTAY, op = 4)
        f.finish(emptyList())
        assertEquals(10, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(SHANTAY_PASS))
        assertEquals(0, f.groundCount(SHANTAY_PASS))
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("al-kharid-purchases-test")
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
                coords = CoordGrid(3304, 3123, 0)
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
            with(Karim(objRepo)) { scripts.startup() }
            with(SilkTrader(objRepo)) { scripts.startup() }
            with(ChancellorHassan(objRepo)) { scripts.startup() }
            with(AliTheLeafletDropper()) { scripts.startup() }
            with(Shantay(Shops(events), objRepo)) { scripts.startup() }
        }

        fun fillInventory() {
            for (slot in 0 until 28) player.inv[slot] = InvObj("obj.bronze_dagger", 1)
        }

        fun groundCount(obj: String): Int =
            objRegistry.findAll(player.coords).filter { it.type == obj.asRSCM() }.sumOf { it.count }

        fun talkTo(npc: String, op: Int = 1) {
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            val target = Npc(npc, player.coords.translateZ(1))
            val body: suspend () -> Unit = {
                val access = ProtectedAccess(player, coroutine, context)
                val event = if (op == 4) NpcEvents.Op4(target) else NpcEvents.Op1(target)
                assertTrue(events.publish(access, event))
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
        private const val KEBAB = "obj.kebab"
        private const val SILK = "obj.silk"
        private const val JUG_OF_WATER = "obj.jug_water"
        private const val FLYER = "obj.leaflet_dropper_flyer"
        private const val SHANTAY_PASS = "obj.shantay_pass"
        private const val KARIM = "npc.kebab_seller"
        private const val SILK_TRADER = "npc.silk_trader"
        private const val HASSAN = "npc.hassan"
        private const val LEAFLET_DROPPER = "npc.ali_leaflet_dropper"
        private const val SHANTAY = "npc.shantay"
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
