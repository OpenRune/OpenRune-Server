package org.rsmod.content.areas.city.rimmington

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.random.Random
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
import org.rsmod.api.random.DefaultGameRandom
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.areas.city.rimmington.npcs.MakeoverMageScript
import org.rsmod.content.areas.city.rimmington.npcs.RimmingtonTownsfolkScript
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class RimmingtonPurchasesInteractionTest {
    @Test fun `the Chemist hands over one sieve`() {
        val f = Fixture()
        f.talkTo(CHEMIST)
        f.finish(listOf(3, 2))
        assertEquals(1, f.player.inv.count(SIEVE))
    }

    @Test fun `the Chemist gives no second sieve`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(SIEVE, 1)
        f.talkTo(CHEMIST)
        f.finish(listOf(3, 2))
        assertEquals(1, f.player.inv.count(SIEVE))
        assertEquals(0, f.groundCount(SIEVE))
    }

    @Test fun `a full pack drops the Chemist's sieve instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.talkTo(CHEMIST)
        f.finish(listOf(3, 2))
        assertEquals(0, f.player.inv.count(SIEVE))
        assertEquals(1, f.groundCount(SIEVE))
    }

    @Test fun `Anja gives a beggar 1 to 3 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 10)
        f.talkTo(ANJA)
        f.finish(listOf(2))
        assertTrue(f.player.inv.count(COINS) in 11..13)
    }

    @Test fun `a full pack drops Anja's coins instead of losing them`() {
        val f = Fixture()
        f.fillInventory()
        f.talkTo(ANJA)
        f.finish(listOf(2))
        assertEquals(0, f.player.inv.count(COINS))
        assertTrue(f.groundCount(COINS) in 1..3)
    }

    @Test fun `Hengel gives a beggar nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 10)
        f.talkTo(HENGEL)
        f.finish(listOf(2))
        assertEquals(10, f.player.inv.count(COINS))
    }

    @Test fun `the Makeover Mage sells an amulet for 100 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 150)
        f.talkTo(MAKEOVER_MAGE)
        f.finish(listOf(3, 1, 4))
        assertEquals(50, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(AMULET))
    }

    @Test fun `declining the Makeover Mage's amulet costs nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 150)
        f.talkTo(MAKEOVER_MAGE)
        f.finish(listOf(3, 2, 4))
        assertEquals(150, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(AMULET))
    }

    @Test fun `the Makeover Mage sells nothing to a player short of coins or space`() {
        val poor = Fixture()
        poor.player.inv[0] = InvObj(COINS, 99)
        poor.talkTo(MAKEOVER_MAGE)
        poor.finish(listOf(3, 4))
        assertEquals(99, poor.player.inv.count(COINS))
        assertEquals(0, poor.player.inv.count(AMULET))

        val full = Fixture()
        full.fillInventory()
        full.player.inv[0] = InvObj(COINS, 150)
        full.talkTo(MAKEOVER_MAGE)
        full.finish(listOf(3, 4))
        assertEquals(150, full.player.inv.count(COINS))
        assertEquals(0, full.player.inv.count(AMULET))
        assertEquals(0, full.groundCount(AMULET))
    }

    @Test fun `spending the last coins frees a slot for the amulet`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 100)
        f.talkTo(MAKEOVER_MAGE)
        f.finish(listOf(3, 1, 4))
        assertEquals(0, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(AMULET))
        assertEquals(0, f.groundCount(AMULET))
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("rimmington-purchases-test")
        private var result: Result<Unit>? = null
        private val clock = MapClock(100)
        private val objRegistry = ObjRegistry(ZoneUpdateMap())
        private val random = DefaultGameRandom(Random(SEED))
        private val context =
            ProtectedAccessContextFactory.empty()
                .copy(
                    getEventBus = { events },
                    getAlignment = { TextAlignment() },
                    getNpcInteractions = { NpcInteractions(events) },
                    getRandom = { random },
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
            val npcs = NpcList()
            val npcRepo = NpcRepository(clock, NpcRegistry(npcs, CollisionFlagMap(), events), npcs)
            with(RimmingtonTownsfolkScript(objRepo, npcRepo)) { scripts.startup() }
            with(MakeoverMageScript(random, objRepo)) { scripts.startup() }
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
        private const val SEED = 241L
        private const val COINS = "obj.coins"
        private const val SIEVE = "obj.mourning_sieve"
        private const val AMULET = "obj.postie_pete_yin_yang"
        private const val CHEMIST = "npc.chemist"
        private const val ANJA = "npc.rimmington_anja"
        private const val HENGEL = "npc.rimmington_hengel"
        private const val MAKEOVER_MAGE = "npc.makeover_mage"
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
