package org.rsmod.content.areas.city.varlamore

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
import org.rsmod.api.shops.Shops
import org.rsmod.content.areas.city.varlamore.aldarin.npcs.MoonriseBrewery
import org.rsmod.content.areas.city.varlamore.civitas.npcs.Pellem
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
class VarlamoreInteractionTest {
    @Test fun `Lucrezia fills a pot with ale yeast for 25 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 60)
        f.player.inv[1] = InvObj(POT, 1)
        f.player.inv[2] = InvObj(POT, 1)
        f.talkTo(LUCREZIA)
        f.finish(BUY_YEAST)
        assertEquals(35, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(POT))
        assertEquals(1, f.player.inv.count(ALE_YEAST))
    }

    @Test fun `Lucrezia takes nothing without a pot`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 60)
        f.talkTo(LUCREZIA)
        f.finish(BUY_YEAST)
        assertEquals(60, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(ALE_YEAST))
    }

    @Test fun `Lucrezia takes nothing from a player short of coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 24)
        f.player.inv[1] = InvObj(POT, 1)
        f.talkTo(LUCREZIA)
        f.finish(BUY_YEAST)
        assertEquals(24, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(POT))
        assertEquals(0, f.player.inv.count(ALE_YEAST))
    }

    @Test fun `declining Lucrezia's yeast costs nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 60)
        f.player.inv[1] = InvObj(POT, 1)
        f.talkTo(LUCREZIA)
        f.finish(DECLINE_YEAST)
        assertEquals(60, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(POT))
        assertEquals(0, f.player.inv.count(ALE_YEAST))
    }

    @Test fun `a full pack still swaps the pot for ale yeast`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 25)
        f.player.inv[1] = InvObj(POT, 1)
        f.talkTo(LUCREZIA)
        f.finish(BUY_YEAST)
        assertEquals(0, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(POT))
        assertEquals(1, f.player.inv.count(ALE_YEAST))
        assertEquals(0, f.groundCount(ALE_YEAST))
    }

    @Test fun `the vineyard foreman hands out one grape barrel`() {
        val f = Fixture()
        f.talkTo(FOREMAN)
        f.finish(ASK_FOR_WORK)
        assertEquals(1, f.player.inv.count(GRAPE_BARREL))
    }

    @Test fun `the vineyard foreman gives no second grape barrel`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(GRAPE_BARREL, 1)
        f.talkTo(FOREMAN)
        f.finish(ASK_FOR_WORK)
        assertEquals(1, f.player.inv.count(GRAPE_BARREL))
    }

    @Test fun `a full pack gets no grape barrel and loses nothing`() {
        val f = Fixture()
        f.fillInventory()
        f.talkTo(FOREMAN)
        f.finish(ASK_FOR_WORK)
        assertEquals(0, f.player.inv.count(GRAPE_BARREL))
        assertEquals(28, f.player.inv.count(DAGGER))
        assertEquals(0, f.groundCount(GRAPE_BARREL))
    }

    @Test fun `Pellem makes a mixed hide top for 20000 coins and two furs`() {
        val f = Fixture()
        f.setHunter(46)
        f.player.inv[0] = InvObj(COINS, 25_000)
        f.player.inv[1] = InvObj(SUN_FUR, 1)
        f.player.inv[2] = InvObj(SUN_FUR, 1)
        f.player.inv[3] = InvObj(SUN_FUR, 1)
        f.talkTo(PELLEM)
        f.finish(MAKE_TOP)
        assertEquals(5_000, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(SUN_FUR))
        assertEquals(1, f.player.inv.count(HIDE_TOP))
    }

    @Test fun `Pellem makes boots for 17500 coins and one fur`() {
        val f = Fixture()
        f.setHunter(46)
        f.player.inv[0] = InvObj(COINS, 17_500)
        f.player.inv[1] = InvObj(SUN_FUR, 1)
        f.talkTo(PELLEM)
        f.finish(MAKE_BOOTS)
        assertEquals(0, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(SUN_FUR))
        assertEquals(1, f.player.inv.count(HIDE_BOOTS))
    }

    @Test fun `Pellem takes nothing from a player short of fur`() {
        val f = Fixture()
        f.setHunter(46)
        f.player.inv[0] = InvObj(COINS, 25_000)
        f.player.inv[1] = InvObj(SUN_FUR, 1)
        f.talkTo(PELLEM)
        f.finish(MAKE_TOP)
        assertEquals(25_000, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(SUN_FUR))
        assertEquals(0, f.player.inv.count(HIDE_TOP))
    }

    @Test fun `Pellem takes nothing from a player short of coins`() {
        val f = Fixture()
        f.setHunter(46)
        f.player.inv[0] = InvObj(COINS, 19_999)
        f.player.inv[1] = InvObj(SUN_FUR, 1)
        f.player.inv[2] = InvObj(SUN_FUR, 1)
        f.talkTo(PELLEM)
        f.finish(MAKE_TOP)
        assertEquals(19_999, f.player.inv.count(COINS))
        assertEquals(2, f.player.inv.count(SUN_FUR))
        assertEquals(0, f.player.inv.count(HIDE_TOP))
    }

    @Test fun `Pellem serves nobody below 46 Hunter`() {
        val f = Fixture()
        f.setHunter(45)
        f.player.inv[0] = InvObj(COINS, 25_000)
        f.player.inv[1] = InvObj(SUN_FUR, 1)
        f.player.inv[2] = InvObj(SUN_FUR, 1)
        f.talkTo(PELLEM)
        f.finish(MAKE_TOP)
        assertEquals(25_000, f.player.inv.count(COINS))
        assertEquals(2, f.player.inv.count(SUN_FUR))
        assertEquals(0, f.player.inv.count(HIDE_TOP))
    }

    @Test fun `asking Pellem for nothing costs nothing`() {
        val f = Fixture()
        f.setHunter(46)
        f.player.inv[0] = InvObj(COINS, 25_000)
        f.player.inv[1] = InvObj(SUN_FUR, 1)
        f.player.inv[2] = InvObj(SUN_FUR, 1)
        f.talkTo(PELLEM)
        f.finish(MAKE_NOTHING)
        assertEquals(25_000, f.player.inv.count(COINS))
        assertEquals(2, f.player.inv.count(SUN_FUR))
        assertEquals(0, f.player.inv.count(HIDE_TOP))
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("varlamore-test")
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
                coords = CoordGrid(1390, 2930, 0)
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
            with(MoonriseBrewery()) { scripts.startup() }
            with(Pellem(Shops(events))) { scripts.startup() }
        }

        fun setHunter(level: Int) {
            player.statMap.setBaseLevel("stat.hunter", level.toByte())
            player.statMap.setCurrentLevel("stat.hunter", level.toByte())
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
        private const val LUCREZIA = "npc.aldarin_brewing_helper"
        private const val FOREMAN = "npc.aldarin_vineyard_foreman"
        private const val PELLEM = "npc.hg_fur_trader"
        private const val COINS = "obj.coins"
        private const val POT = "obj.pot_empty"
        private const val ALE_YEAST = "obj.ale_yeast"
        private const val GRAPE_BARREL = "obj.aldarin_grape_collection_barrel"
        private const val DAGGER = "obj.bronze_dagger"
        private const val SUN_FUR = "obj.hunting_antelopesun_fur"
        private const val HIDE_TOP = "obj.hide_top"
        private const val HIDE_BOOTS = "obj.hide_boots"
        private val BUY_YEAST = listOf(4, 1, 5)
        private val DECLINE_YEAST = listOf(4, 2, 5)
        private val ASK_FOR_WORK = listOf(1)
        private val MAKE_TOP = listOf(4, 1)
        private val MAKE_BOOTS = listOf(4, 3)
        private val MAKE_NOTHING = listOf(4, 5)
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
