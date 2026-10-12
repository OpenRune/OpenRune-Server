package org.rsmod.content.areas.city.gnomestronghold

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
import org.rsmod.content.areas.city.gnomestronghold.npcs.TenthSquad
import org.rsmod.content.areas.city.gnomestronghold.npcs.shops.BlurberryBar
import org.rsmod.content.areas.city.gnomestronghold.npcs.shops.GrandTreeGroceries
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
class GnomeStrongholdPurchasesInteractionTest {
    @Test
    fun `the Blurberry barman sells each ingredient for 20 coins`() {
        for ((option, ingredient) in INGREDIENTS) {
            val f = Fixture()
            f.player.inv[0] = InvObj(COINS, 50)
            f.talkTo(BARMAN)
            f.finish(listOf(3, option))
            assertEquals(30, f.player.inv.count(COINS), ingredient)
            assertEquals(1, f.player.inv.count(ingredient), ingredient)
        }
    }

    @Test
    fun `the barman sells nothing to a player short of coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 19)
        f.talkTo(BARMAN)
        f.finish(listOf(3, 1))
        assertEquals(19, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(LEMON))
    }

    @Test
    fun `the barman takes nothing from a full pack`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 50)
        f.talkTo(BARMAN)
        f.finish(listOf(3, 1))
        assertEquals(50, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(LEMON))
        assertEquals(0, f.groundCount(LEMON))
    }

    @Test
    fun `declining the ingredients costs nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 50)
        f.talkTo(BARMAN)
        f.finish(listOf(3, 4))
        assertEquals(50, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(LEMON))
    }

    @Test
    fun `Hudo buys every giant frog leg for 1000 coins each`() {
        val f = Fixture()
        for (slot in 0 until 3) f.player.inv[slot] = InvObj(FROG_LEGS, 1)
        f.player.inv[3] = InvObj(NOTED_FROG_LEGS, 5)
        f.talkTo(HUDO)
        f.finish(listOf(2, 1))
        assertEquals(3000, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(FROG_LEGS))
        assertEquals(5, f.player.inv.count(NOTED_FROG_LEGS))
    }

    @Test
    fun `Hudo buys a full pack of frog legs without dropping coins`() {
        val f = Fixture()
        for (slot in 0 until 28) f.player.inv[slot] = InvObj(FROG_LEGS, 1)
        f.talkTo(HUDO)
        f.finish(listOf(2, 1))
        assertEquals(28_000, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(FROG_LEGS))
        assertEquals(0, f.groundCount(COINS))
    }

    @Test
    fun `keeping the frog legs sells nothing to Hudo`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(FROG_LEGS, 1)
        f.talkTo(HUDO)
        f.finish(listOf(1, 1))
        assertEquals(0, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(FROG_LEGS))
    }

    @Test
    fun `Hudo's coins that overflow the stack are dropped instead of lost`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, Int.MAX_VALUE - 500)
        f.player.inv[1] = InvObj(FROG_LEGS, 1)
        f.talkTo(HUDO)
        f.finish(listOf(2, 1))
        assertEquals(Int.MAX_VALUE - 500, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(FROG_LEGS))
        assertEquals(1000, f.groundCount(COINS))
    }

    @Test
    fun `Zooknock hands out one monkeyspeak amulet`() {
        val f = Fixture()
        f.talkTo(ZOOKNOCK)
        f.finish(listOf(2, 2, 4))
        assertEquals(1, f.player.inv.count(AMULET))
    }

    @Test
    fun `Zooknock gives nothing when the amulet is already in the bank`() {
        val f = Fixture()
        val bank = f.player.invMap.getOrPut("inv.bank")
        bank[0] = InvObj(AMULET, 1)
        f.talkTo(ZOOKNOCK)
        f.finish(listOf(2))
        assertEquals(0, f.player.inv.count(AMULET))
        assertEquals(1, bank.count(AMULET))
    }

    @Test
    fun `Zooknock's talisman pages reach the karamjan monkey talisman`() {
        val f = Fixture()
        f.talkTo(ZOOKNOCK)
        f.finish(listOf(1, 4, 4, 3, 4))
        assertEquals(1, f.player.inv.count(KARAMJAN_GREEGREE))
    }

    @Test
    fun `a full pack drops Zooknock's sigil instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.talkTo(ZOOKNOCK)
        f.finish(listOf(3, 4))
        assertEquals(0, f.player.inv.count(SIGIL))
        assertEquals(1, f.groundCount(SIGIL))
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("gnome-stronghold-purchases-test")
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
                coords = CoordGrid(2465, 3495, 1)
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
            with(BlurberryBar(Shops(events), objRepo)) { scripts.startup() }
            with(GrandTreeGroceries(Shops(events), objRepo)) { scripts.startup() }
            with(TenthSquad(objRepo)) { scripts.startup() }
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
        private val CONTINUE_COMPONENTS =
            mapOf(
                "interface.chat_left" to "component.chat_left:continue",
                "interface.chat_right" to "component.chat_right:continue",
                "interface.messagebox" to "component.messagebox:continue",
                "interface.objectbox" to "component.objectbox:universe",
            )
        private const val BARMAN = "npc.blurberrybarmen"
        private const val HUDO = "npc.gnome_hudo"
        private const val ZOOKNOCK = "npc.mm2_zooknock_postquest"
        private const val LEMON = "obj.lemon"
        private const val FROG_LEGS = "obj.giant_frog_legs"
        private const val NOTED_FROG_LEGS = "obj.cert_giant_frog_legs"
        private const val AMULET = "obj.mm_amulet_of_monkey_speak"
        private const val SIGIL = "obj.mm_sigil"
        private const val KARAMJAN_GREEGREE = "obj.mm_monkey_greegree_for_normal_monkey"
        private val INGREDIENTS =
            mapOf(1 to LEMON, 2 to "obj.orange", 3 to "obj.cocktail_shaker")
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
