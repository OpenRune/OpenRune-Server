package org.rsmod.content.areas.city.varrock

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
import org.rsmod.content.areas.city.varrock.npcs.Baraek
import org.rsmod.content.areas.city.varrock.npcs.Bartenders
import org.rsmod.content.areas.city.varrock.npcs.Benny
import org.rsmod.content.areas.city.varrock.npcs.BlueMoonCook
import org.rsmod.content.areas.city.varrock.npcs.MasterSmithingTutor
import org.rsmod.content.areas.city.varrock.npcs.Tramps
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
class VarrockPurchasesInteractionTest {
    @Test fun `Benny sells a newspaper for 50 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 120)
        f.talkTo(BENNY)
        f.finish(listOf(1, 1))
        assertEquals(70, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(NEWSPAPER))
    }

    @Test fun `Benny sells nothing to a player short of coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 49)
        f.talkTo(BENNY)
        f.finish(listOf(1))
        assertEquals(49, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(NEWSPAPER))
    }

    @Test fun `declining Benny costs nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 50)
        f.talkTo(BENNY)
        f.finish(listOf(2, 2))
        assertEquals(50, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(NEWSPAPER))
    }

    @Test fun `spending the last coins frees a slot for the newspaper`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 50)
        f.talkTo(BENNY)
        f.finish(listOf(1, 1))
        assertEquals(0, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(NEWSPAPER))
        assertEquals(0, f.groundCount(NEWSPAPER))
    }

    @Test fun `a full pack drops the newspaper instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 60)
        f.talkTo(BENNY)
        f.finish(listOf(1, 1))
        assertEquals(10, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(NEWSPAPER))
        assertEquals(1, f.groundCount(NEWSPAPER))
    }

    @Test fun `every Varrock bartender sells a beer for 2 coins`() {
        for ((bartender, options) in BEER_OPTIONS) {
            val f = Fixture()
            f.player.inv[0] = InvObj(COINS, 5)
            f.talkTo(bartender)
            f.finish(options)
            assertEquals(3, f.player.inv.count(COINS), bartender)
            assertEquals(1, f.player.inv.count(BEER), bartender)
        }
    }

    @Test fun `bartenders keep the beer from a player short of coins`() {
        for ((bartender, options) in BEER_OPTIONS) {
            val f = Fixture()
            f.player.inv[0] = InvObj(COINS, 1)
            f.talkTo(bartender)
            f.finish(options)
            assertEquals(1, f.player.inv.count(COINS), bartender)
            assertEquals(0, f.player.inv.count(BEER), bartender)
        }
    }

    @Test fun `a full pack drops the beer instead of losing it`() {
        for ((bartender, options) in BEER_OPTIONS) {
            val f = Fixture()
            f.fillInventory()
            f.player.inv[0] = InvObj(COINS, 3)
            f.talkTo(bartender)
            f.finish(options)
            assertEquals(1, f.player.inv.count(COINS), bartender)
            assertEquals(0, f.player.inv.count(BEER), bartender)
            assertEquals(1, f.groundCount(BEER), bartender)
        }
    }

    @Test fun `Baraek sells a bear fur for 20 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 120)
        f.talkTo(BARAEK)
        f.finish(listOf(1, 1))
        assertEquals(100, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(BEAR_FUR))
    }

    @Test fun `Baraek drops to 18 coins for a player short of 20`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 19)
        f.talkTo(BARAEK)
        f.finish(listOf(1, 1, 1))
        assertEquals(1, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(BEAR_FUR))
    }

    @Test fun `Baraek sells nothing to a player short of 18 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 17)
        f.talkTo(BARAEK)
        f.finish(listOf(1, 1, 1))
        assertEquals(17, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(BEAR_FUR))
    }

    @Test fun `declining Baraek's fur costs nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 19)
        f.talkTo(BARAEK)
        f.finish(listOf(1, 1, 2))
        assertEquals(19, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(BEAR_FUR))
    }

    @Test fun `a full pack drops Baraek's fur instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 30)
        f.talkTo(BARAEK)
        f.finish(listOf(1, 1))
        assertEquals(10, f.player.inv.count(COINS))
        assertEquals(1, f.groundCount(BEAR_FUR))
    }

    @Test fun `Baraek buys every grey wolf fur for 120 coins each`() {
        val f = Fixture()
        for (slot in 0 until 3) f.player.inv[slot] = InvObj(GREY_WOLF_FUR, 1)
        f.player.inv[3] = InvObj(BEAR_FUR, 1)
        f.talkTo(BARAEK)
        f.finish(listOf(3, 1))
        assertEquals(360, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(GREY_WOLF_FUR))
        assertEquals(1, f.player.inv.count(BEAR_FUR))
    }

    @Test fun `Baraek buys a full pack of furs for 12 coins each`() {
        val f = Fixture()
        for (slot in 0 until 20) f.player.inv[slot] = InvObj(BEAR_FUR, 1)
        for (slot in 20 until 28) f.player.inv[slot] = InvObj(FUR, 1)
        f.talkTo(BARAEK)
        f.finish(listOf(3, 1))
        assertEquals(28 * 12, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(BEAR_FUR))
        assertEquals(0, f.player.inv.count(FUR))
        assertEquals(0, f.groundCount(COINS))
    }

    @Test fun `keeping the fur sells nothing to Baraek`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(BEAR_FUR, 1)
        f.talkTo(BARAEK)
        f.finish(listOf(3, 2))
        assertEquals(0, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(BEAR_FUR))
    }

    @Test fun `the Blue Moon cook sells a cabbage for 1 coin`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 5)
        f.talkTo(BLUE_MOON_COOK)
        f.finish(listOf(1, 1))
        assertEquals(4, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(CABBAGE))
    }

    @Test fun `the Blue Moon cook refuses a player without coins or space`() {
        val poor = Fixture()
        poor.talkTo(BLUE_MOON_COOK)
        poor.finish(listOf(1))
        assertEquals(0, poor.player.inv.count(CABBAGE))

        val full = Fixture()
        full.fillInventory()
        full.player.inv[0] = InvObj(COINS, 5)
        full.talkTo(BLUE_MOON_COOK)
        full.finish(listOf(1))
        assertEquals(5, full.player.inv.count(COINS))
        assertEquals(0, full.player.inv.count(CABBAGE))
        assertEquals(0, full.groundCount(CABBAGE))
    }

    @Test fun `declining the cabbage costs nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 5)
        f.talkTo(BLUE_MOON_COOK)
        f.finish(listOf(1, 2))
        assertEquals(5, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(CABBAGE))
    }

    @Test fun `tramps take exactly one coin`() {
        for ((tramp, options) in mapOf(CHARLIE to listOf(4, 1), TRAMP to listOf(1))) {
            val f = Fixture()
            f.player.inv[0] = InvObj(COINS, 5)
            f.talkTo(tramp)
            f.finish(options)
            assertEquals(4, f.player.inv.count(COINS), tramp)
        }
    }

    @Test fun `refusing a tramp keeps the coins`() {
        for ((tramp, options) in mapOf(CHARLIE to listOf(2), TRAMP to listOf(2))) {
            val f = Fixture()
            f.player.inv[0] = InvObj(COINS, 5)
            f.talkTo(tramp)
            f.finish(options)
            assertEquals(5, f.player.inv.count(COINS), tramp)
        }
    }

    @Test fun `the master smithing tutor gives one hammer`() {
        val f = Fixture()
        f.talkTo(SMITHING_TUTOR)
        f.finish(listOf(1, 1, 3))
        assertEquals(1, f.player.inv.count(HAMMER))
    }

    @Test fun `a full pack drops the tutor's hammer instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.talkTo(SMITHING_TUTOR)
        f.finish(listOf(1, 3))
        assertEquals(0, f.player.inv.count(HAMMER))
        assertEquals(1, f.groundCount(HAMMER))
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("varrock-purchases-test")
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
            with(Benny(objRepo)) { scripts.startup() }
            with(Bartenders(objRepo)) { scripts.startup() }
            with(Baraek(objRepo)) { scripts.startup() }
            with(BlueMoonCook(objRepo)) { scripts.startup() }
            with(Tramps()) { scripts.startup() }
            with(MasterSmithingTutor(objRepo)) { scripts.startup() }
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
        private const val BEER = "obj.beer"
        private const val NEWSPAPER = "obj.qip_soa_newspaper2"
        private const val BENNY = "npc.qip_soa_newspaperseller"
        private val CONTINUE_COMPONENTS =
            mapOf(
                "interface.chat_left" to "component.chat_left:continue",
                "interface.chat_right" to "component.chat_right:continue",
                "interface.messagebox" to "component.messagebox:continue",
                "interface.objectbox" to "component.objectbox:universe",
            )
        private const val BARAEK = "npc.baraek"
        private const val BLUE_MOON_COOK = "npc.fai_varrock_bluemoon_chef"
        private const val CHARLIE = "npc.tramppg"
        private const val TRAMP = "npc.tramp"
        private const val SMITHING_TUTOR = "npc.aide_tutor_smithing_master"
        private const val BEAR_FUR = "obj.fur"
        private const val FUR = "obj.werewolve_fur"
        private const val GREY_WOLF_FUR = "obj.grey_wolf_fur"
        private const val CABBAGE = "obj.cabbage"
        private const val HAMMER = "obj.hammer"
        private val BEER_OPTIONS =
            mapOf(
                "npc.bluemoon_bartender" to listOf(1),
                "npc.donkey_bartender" to listOf(1),
                "npc.jollyboar_bartender" to listOf(1),
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
