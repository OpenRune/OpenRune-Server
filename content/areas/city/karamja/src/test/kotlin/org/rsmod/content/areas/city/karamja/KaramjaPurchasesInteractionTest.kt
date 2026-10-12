package org.rsmod.content.areas.city.karamja

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
import org.rsmod.content.areas.city.karamja.npcs.DeadMansChest
import org.rsmod.content.areas.city.karamja.npcs.ParamayaInn
import org.rsmod.content.areas.city.karamja.npcs.Seravel
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
class KaramjaPurchasesInteractionTest {

    @Test fun `the Dead Man's Chest bartender sells grog and rum`() {
        for ((options, item) in BAR_PURCHASES) {
            val f = Fixture()
            f.player.inv[0] = InvObj(COINS, 30)
            f.talkTo(DEAD_MANS_BARTENDER)
            f.finish(options)
            assertEquals(30 - item.second, f.player.inv.count(COINS), item.first)
            assertEquals(1, f.player.inv.count(item.first), item.first)
        }
    }

    @Test fun `the Dead Man's Chest bartender sells nothing to a player short of coins`() {
        for ((options, item) in BAR_PURCHASES) {
            val f = Fixture()
            f.player.inv[0] = InvObj(COINS, item.second - 1)
            f.talkTo(DEAD_MANS_BARTENDER)
            f.finish(options)
            assertEquals(item.second - 1, f.player.inv.count(COINS), item.first)
            assertEquals(0, f.player.inv.count(item.first), item.first)
        }
    }

    @Test fun `declining the Dead Man's Chest bartender costs nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 30)
        f.talkTo(DEAD_MANS_BARTENDER)
        f.finish(listOf(1))
        assertEquals(30, f.player.inv.count(COINS))
    }

    @Test fun `a full pack drops the bartender's drink instead of losing it`() {
        for ((options, item) in BAR_PURCHASES) {
            val f = Fixture()
            f.fillInventory()
            f.player.inv[0] = InvObj(COINS, 30)
            f.talkTo(DEAD_MANS_BARTENDER)
            f.finish(options)
            assertEquals(30 - item.second, f.player.inv.count(COINS), item.first)
            assertEquals(1, f.groundCount(item.first), item.first)
        }
    }

    @Test fun `Kaleb sells wine, beer and dormitory tickets`() {
        for ((option, item) in INN_PURCHASES) {
            val f = Fixture()
            f.player.inv[0] = InvObj(COINS, 200)
            f.talkTo(KALEB)
            f.finish(listOf(2, option, 5))
            assertEquals(200 - item.second, f.player.inv.count(COINS), item.first)
            assertEquals(item.third, f.player.inv.count(item.first), item.first)
        }
    }

    @Test fun `Kaleb sells nothing to a player short of coins`() {
        for ((option, item) in INN_PURCHASES) {
            val f = Fixture()
            f.player.inv[0] = InvObj(COINS, item.second - 1)
            f.talkTo(KALEB)
            f.finish(listOf(2, option, 5))
            assertEquals(item.second - 1, f.player.inv.count(COINS), item.first)
            assertEquals(0, f.player.inv.count(item.first), item.first)
        }
    }

    @Test fun `cancelling Kaleb's sale costs nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 200)
        f.talkTo(KALEB)
        f.finish(listOf(2, 5))
        assertEquals(200, f.player.inv.count(COINS))
    }

    @Test fun `buying twice from Kaleb charges twice`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 100)
        f.talkTo(KALEB)
        f.finish(listOf(2, 3, 3, 5))
        assertEquals(30, f.player.inv.count(COINS))
        assertEquals(2, f.player.inv.count(TICKET))
    }

    @Test fun `a full pack drops all five dorm tickets instead of losing them`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 200)
        f.player.inv[1] = null
        f.player.inv[2] = null
        f.talkTo(KALEB)
        f.finish(listOf(2, 4, 5))
        assertEquals(25, f.player.inv.count(COINS))
        assertEquals(2, f.player.inv.count(TICKET))
        assertEquals(3, f.groundCount(TICKET))
    }

    @Test fun `Seravel sells a ship ticket for 25 coins`() {
        for (options in listOf(listOf(1), listOf(3, 1))) {
            val f = Fixture()
            f.player.inv[0] = InvObj(COINS, 30)
            f.talkTo(SERAVEL)
            f.finish(options)
            assertEquals(5, f.player.inv.count(COINS))
            assertEquals(1, f.player.inv.count(SHIP_TICKET))
        }
    }

    @Test fun `Seravel sells nothing to a player short of coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 24)
        f.talkTo(SERAVEL)
        f.finish(listOf(1))
        assertEquals(24, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(SHIP_TICKET))
    }

    @Test fun `declining Seravel costs nothing`() {
        for (options in listOf(listOf(2), listOf(3, 2))) {
            val f = Fixture()
            f.player.inv[0] = InvObj(COINS, 30)
            f.talkTo(SERAVEL)
            f.finish(options)
            assertEquals(30, f.player.inv.count(COINS))
            assertEquals(0, f.player.inv.count(SHIP_TICKET))
        }
    }

    @Test fun `a full pack drops Seravel's ticket instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 30)
        f.talkTo(SERAVEL)
        f.finish(listOf(1))
        assertEquals(5, f.player.inv.count(COINS))
        assertEquals(1, f.groundCount(SHIP_TICKET))
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("karamja-purchases-test")
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
                coords = CoordGrid(3213, 3428, 0)
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

        init {
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            val objRepo = ObjRepository(clock, objRegistry)
            with(DeadMansChest(objRepo)) { scripts.startup() }
            with(ParamayaInn(objRepo)) { scripts.startup() }
            with(Seravel(objRepo)) { scripts.startup() }
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
        private val CONTINUE_COMPONENTS =
            mapOf(
                "interface.chat_left" to "component.chat_left:continue",
                "interface.chat_right" to "component.chat_right:continue",
                "interface.messagebox" to "component.messagebox:continue",
                "interface.objectbox" to "component.objectbox:universe",
            )
        private const val COINS = "obj.coins"
        private const val TICKET = "obj.paramayaticket"
        private const val DEAD_MANS_BARTENDER = "npc.deadmans_bartender"
        private const val SERAVEL = "npc.shiloshiptickets"
        private const val SHIP_TICKET = "obj.shiloshipticket"
        private const val KALEB = "npc.shiloinnowner"
        private val BAR_PURCHASES =
            mapOf(listOf(2) to ("obj.grog" to 3), listOf(3) to ("obj.karamja_rum" to 27))
        private val INN_PURCHASES =
            mapOf(
                1 to Triple("obj.jug_wine", 1, 1),
                2 to Triple("obj.beer", 2, 1),
                3 to Triple(TICKET, 35, 1),
                4 to Triple(TICKET, 175, 5),
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
