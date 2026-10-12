package org.rsmod.content.areas.misc.guilds

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
import org.rsmod.content.areas.misc.guilds.fishing.FishingGuild
import org.rsmod.content.areas.misc.guilds.ranging.RangingGuild
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
class GuildPurchasesInteractionTest {
    @Test fun `Master fisher sells a Fishing cape and hood for 99000 coins`() {
        val f = Fixture()
        f.setFishing(99)
        f.player.inv[0] = InvObj(COINS, 100_000)
        f.talkTo(MASTER_FISHER)
        f.finish(listOf(1, 1))
        assertEquals(1_000, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(FISHING_CAPE))
        assertEquals(1, f.player.inv.count(FISHING_HOOD))
    }

    @Test fun `Master fisher sells nothing to a player short of coins`() {
        val f = Fixture()
        f.setFishing(99)
        f.player.inv[0] = InvObj(COINS, CAPE_PRICE - 1)
        f.talkTo(MASTER_FISHER)
        f.finish(listOf(1, 1))
        assertEquals(CAPE_PRICE - 1, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(FISHING_CAPE))
        assertEquals(0, f.player.inv.count(FISHING_HOOD))
    }

    @Test fun `Master fisher keeps the cape from a player without two free slots`() {
        val f = Fixture()
        f.setFishing(99)
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, CAPE_PRICE)
        f.player.inv[1] = null
        f.talkTo(MASTER_FISHER)
        f.finish(listOf(1, 1))
        assertEquals(CAPE_PRICE, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(FISHING_CAPE))
        assertEquals(0, f.groundCount(FISHING_CAPE))
        assertEquals(0, f.groundCount(FISHING_HOOD))
    }

    @Test fun `declining the Master fisher's price costs nothing`() {
        val f = Fixture()
        f.setFishing(99)
        f.player.inv[0] = InvObj(COINS, CAPE_PRICE)
        f.talkTo(MASTER_FISHER)
        f.finish(listOf(1, 2))
        assertEquals(CAPE_PRICE, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(FISHING_CAPE))
    }

    @Test fun `Master fisher offers no cape below 99 Fishing`() {
        val f = Fixture()
        f.setFishing(98)
        f.player.inv[0] = InvObj(COINS, CAPE_PRICE)
        f.talkTo(MASTER_FISHER)
        f.finish(listOf(1, 1))
        assertEquals(CAPE_PRICE, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(FISHING_CAPE))
    }

    @Test fun `Shield master hands over one flyer`() {
        val f = Fixture()
        f.talkTo(SHIELD_MASTER)
        f.finish(listOf(3))
        assertEquals(1, f.player.inv.count(FLYER))
    }

    @Test fun `Shield master gives no second flyer`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(FLYER, 1)
        f.talkTo(SHIELD_MASTER)
        f.finish(listOf(3))
        assertEquals(1, f.player.inv.count(FLYER))
        assertEquals(0, f.groundCount(FLYER))
    }

    @Test fun `Shield master drops the flyer for a full pack`() {
        val f = Fixture()
        f.fillInventory()
        f.talkTo(SHIELD_MASTER)
        f.finish(listOf(3))
        assertEquals(0, f.player.inv.count(FLYER))
        assertEquals(1, f.groundCount(FLYER))
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("guild-purchases-test")
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
                coords = CoordGrid(2611, 3393, 0)
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
            val shops = Shops(events)
            with(FishingGuild(objRepo, shops)) { scripts.startup() }
            with(RangingGuild(objRepo, shops)) { scripts.startup() }
        }

        fun setFishing(level: Int) {
            player.statMap.setBaseLevel("stat.fishing", level.toByte())
            player.statMap.setCurrentLevel("stat.fishing", level.toByte())
        }

        fun fillInventory() {
            for (slot in 0 until 28) player.inv[slot] = InvObj("obj.bronze_dagger", 1)
        }

        fun groundCount(obj: String): Int =
            objRegistry.findAll(player.coords).filter { it.type == obj.asRSCM() }.sumOf { it.count }

        fun talkTo(npc: String, op: (Npc) -> NpcEvents.Op = { NpcEvents.Op1(it) }) {
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            val target = Npc(npc, player.coords.translateZ(1))
            val body: suspend () -> Unit = {
                val access = ProtectedAccess(player, coroutine, context)
                assertTrue(events.publish(access, op(target)))
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
        private const val MASTER_FISHER = "npc.master_fisher"
        private const val SHIELD_MASTER = "npc.ranging_guild_shield_master"
        private const val FISHING_CAPE = "obj.skillcape_fishing"
        private const val FISHING_HOOD = "obj.skillcape_fishing_hood"
        private const val FLYER = "obj.leather_shield_info"
        private const val CAPE_PRICE = 99_000
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
