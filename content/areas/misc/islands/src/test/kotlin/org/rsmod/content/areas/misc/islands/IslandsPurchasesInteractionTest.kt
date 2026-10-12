package org.rsmod.content.areas.misc.islands

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
import org.rsmod.content.areas.misc.islands.apeatoll.npcs.MarimMonkeys
import org.rsmod.content.areas.misc.islands.apeatoll.npcs.shops.MarimShops
import org.rsmod.content.areas.misc.islands.entrana.npcs.FritzTheGlassblower
import org.rsmod.content.areas.misc.islands.harmony.npcs.HarmonyIsland
import org.rsmod.content.areas.misc.islands.mosleharmless.npcs.shops.PirateShops
import org.rsmod.content.areas.misc.islands.waterbirth.npcs.WaterbirthIsland
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
class IslandsPurchasesInteractionTest {
    @Test fun `Fritz hands over one bucket while explaining glassmaking`() {
        val f = Fixture()
        f.talkTo(FRITZ)
        f.finish(listOf(1))
        assertEquals(1, f.player.inv.count(BUCKET))
    }

    @Test fun `declining Fritz's explanation gives no bucket`() {
        val f = Fixture()
        f.talkTo(FRITZ)
        f.finish(listOf(2))
        assertEquals(0, f.player.inv.count(BUCKET))
    }

    @Test fun `a full pack drops Fritz's bucket instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.talkTo(FRITZ)
        f.finish(listOf(1))
        assertEquals(0, f.player.inv.count(BUCKET))
        assertEquals(1, f.groundCount(BUCKET))
    }

    @Test fun `Daga sells a dragon scimitar for 100,000 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 100_050)
        f.talkTo(DAGA)
        f.finish(listOf(3, 1))
        assertEquals(50, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(DRAGON_SCIMITAR))
    }

    @Test fun `Daga sells nothing to a player short of coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 99_999)
        f.talkTo(DAGA)
        f.finish(listOf(3, 1))
        assertEquals(99_999, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(DRAGON_SCIMITAR))
    }

    @Test fun `declining Daga's dragon scimitar costs nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 100_000)
        f.talkTo(DAGA)
        f.finish(listOf(3, 2))
        assertEquals(100_000, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(DRAGON_SCIMITAR))
    }

    @Test fun `a full pack drops Daga's dragon scimitar instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 100_010)
        f.talkTo(DAGA)
        f.finish(listOf(3, 1))
        assertEquals(10, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(DRAGON_SCIMITAR))
        assertEquals(1, f.groundCount(DRAGON_SCIMITAR))
    }

    @Test fun `the monkey child lends one talisman`() {
        val f = Fixture()
        f.talkTo(MONKEY_CHILD)
        f.finish(listOf(2, 1))
        assertEquals(1, f.player.inv.count(MONKEY_TALISMAN))
    }

    @Test fun `refusing the monkey child's toy gives nothing`() {
        for (options in listOf(listOf(1), listOf(2, 2))) {
            val f = Fixture()
            f.talkTo(MONKEY_CHILD)
            f.finish(options)
            assertEquals(0, f.player.inv.count(MONKEY_TALISMAN), "options $options")
        }
    }

    @Test fun `Smith sells each blade at its price`() {
        for ((options, blade) in BLADES) {
            val price = BLADE_PRICES.getValue(blade)
            val f = Fixture()
            f.player.inv[0] = InvObj(BOOK_O_PIRACY, 1)
            f.player.inv[1] = InvObj(COINS, price + 5)
            f.talkTo(SMITH)
            f.finish(options + 1)
            assertEquals(5, f.player.inv.count(COINS), blade)
            assertEquals(1, f.player.inv.count(blade), blade)
            assertEquals(1, f.player.inv.count(BOOK_O_PIRACY), blade)
        }
    }

    @Test fun `Smith sells no blade to a player short of coins`() {
        for ((options, blade) in BLADES) {
            val price = BLADE_PRICES.getValue(blade)
            val f = Fixture()
            f.player.inv[0] = InvObj(BOOK_O_PIRACY, 1)
            f.player.inv[1] = InvObj(COINS, price - 1)
            f.talkTo(SMITH)
            f.finish(options + 1)
            assertEquals(price - 1, f.player.inv.count(COINS), blade)
            assertEquals(0, f.player.inv.count(blade), blade)
        }
    }

    @Test fun `declining Smith's blades costs nothing`() {
        for ((options, blade) in BLADES) {
            val f = Fixture()
            f.player.inv[0] = InvObj(BOOK_O_PIRACY, 1)
            f.player.inv[1] = InvObj(COINS, 30_000)
            f.talkTo(SMITH)
            f.finish(options + 2)
            assertEquals(30_000, f.player.inv.count(COINS), blade)
            assertEquals(0, f.player.inv.count(blade), blade)
        }
    }

    @Test fun `a full pack drops Smith's blade instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(BOOK_O_PIRACY, 1)
        f.player.inv[1] = InvObj(COINS, 1_050)
        f.talkTo(SMITH)
        f.finish(listOf(3, 1, 1))
        assertEquals(10, f.player.inv.count(COINS))
        assertEquals(1, f.groundCount(HARRYS_CUTLASS))
    }

    @Test fun `without the Book o' Piracy Smith neither talks nor sells`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 30_000)
        f.talkTo(SMITH)
        f.finish(listOf(3, 1, 1))
        assertEquals(30_000, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(HARRYS_CUTLASS))
    }

    @Test fun `Smith repairs the broken anchor for 230,000 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(BOOK_O_PIRACY, 1)
        f.player.inv[1] = InvObj(BROKEN_ANCHOR, 1)
        f.player.inv[2] = InvObj(COINS, 230_001)
        f.talkTo(SMITH)
        f.finish(listOf(4, 1))
        assertEquals(1, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(BROKEN_ANCHOR))
        assertEquals(1, f.player.inv.count(ANCHOR))
    }

    @Test fun `the anchor stays broken when the player is short or declines`() {
        for ((coins, options) in listOf(229_999 to listOf(4), 230_000 to listOf(4, 2))) {
            val f = Fixture()
            f.player.inv[0] = InvObj(BOOK_O_PIRACY, 1)
            f.player.inv[1] = InvObj(BROKEN_ANCHOR, 1)
            f.player.inv[2] = InvObj(COINS, coins)
            f.talkTo(SMITH)
            f.finish(options)
            assertEquals(coins, f.player.inv.count(COINS), "coins $coins")
            assertEquals(1, f.player.inv.count(BROKEN_ANCHOR), "coins $coins")
            assertEquals(0, f.player.inv.count(ANCHOR), "coins $coins")
        }
    }

    @Test fun `Brother Tranquility hands over a broken anchor once`() {
        val f = Fixture()
        f.talkTo(TRANQUILITY)
        f.finish(emptyList())
        assertEquals(1, f.player.inv.count(BROKEN_ANCHOR))
        f.talkTo(TRANQUILITY)
        f.finish(emptyList())
        assertEquals(1, f.player.inv.count(BROKEN_ANCHOR))
    }

    @Test fun `Brother Tranquility gives no anchor to a player who owns one`() {
        val f = Fixture()
        f.bank[0] = InvObj(ANCHOR, 1)
        f.talkTo(TRANQUILITY)
        f.finish(emptyList())
        assertEquals(0, f.player.inv.count(BROKEN_ANCHOR))
        assertEquals(1, f.bank.count(ANCHOR))
    }

    @Test fun `Askeladden buys every keg for 500 coins each`() {
        val f = Fixture()
        for (slot in 0 until 3) f.player.inv[slot] = InvObj(KEG_OF_BEER, 1)
        f.talkTo(ASKELADDEN)
        f.finish(listOf(1, 1))
        assertEquals(0, f.player.inv.count(KEG_OF_BEER))
        assertEquals(1_500, f.player.inv.count(COINS))
    }

    @Test fun `Askeladden buys kegs one at a time`() {
        val f = Fixture()
        for (slot in 0 until 3) f.player.inv[slot] = InvObj(KEG_OF_BEER, 1)
        f.talkTo(ASKELADDEN)
        f.finish(listOf(1, 2, 1, 2))
        assertEquals(1, f.player.inv.count(KEG_OF_BEER))
        assertEquals(1_000, f.player.inv.count(COINS))
    }

    @Test fun `declining Askeladden keeps the kegs`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(KEG_OF_BEER, 1)
        f.talkTo(ASKELADDEN)
        f.finish(listOf(2))
        assertEquals(1, f.player.inv.count(KEG_OF_BEER))
        assertEquals(0, f.player.inv.count(COINS))
    }

    @Test fun `selling the last keg one at a time stops the offer`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(KEG_OF_BEER, 1)
        f.talkTo(ASKELADDEN)
        f.finish(listOf(1, 2, 1))
        assertEquals(0, f.player.inv.count(KEG_OF_BEER))
        assertEquals(500, f.player.inv.count(COINS))
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("islands-purchases-test")
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
                coords = CoordGrid(3677, 2978, 0)
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

        val bank: Inventory =
            checkNotNull(ServerCacheManager.getInventory("inv.bank".asRSCM())).let { type ->
                Inventory(type, arrayOfNulls(type.size)).also { player.invMap["inv.bank"] = it }
            }

        init {
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            val objRepo = ObjRepository(clock, objRegistry)
            val shops = Shops(events)
            with(FritzTheGlassblower(objRepo)) { scripts.startup() }
            with(MarimShops(shops, objRepo)) { scripts.startup() }
            with(MarimMonkeys(objRepo)) { scripts.startup() }
            with(PirateShops(shops, objRepo)) { scripts.startup() }
            with(HarmonyIsland(objRepo)) { scripts.startup() }
            with(WaterbirthIsland()) { scripts.startup() }
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
            repeat(200) {
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
        private const val BUCKET = "obj.bucket_empty"
        private const val FRITZ = "npc.entrana_glassblower"
        private const val DAGA = "npc.mm_daga"
        private const val DRAGON_SCIMITAR = "obj.dragon_scimitar"
        private const val MONKEY_CHILD = "npc.mm_monkey_child"
        private const val MONKEY_TALISMAN = "obj.mm_monkey_talisman"
        private const val SMITH = "npc.fever_smithing_smith"
        private const val BOOK_O_PIRACY = "obj.fever_piracy_book"
        private const val HARRYS_CUTLASS = "obj.fever_cutlass_brasshand_harry"
        private const val LUCKY_CUTLASS = "obj.fever_cutlass_unlucky_jenkins_lucky_cutlass"
        private const val MALLARDS_RAPIER = "obj.fever_rapier_mallard"
        private const val BROKEN_ANCHOR = "obj.brain_broken_anchor"
        private const val ANCHOR = "obj.brain_anchor"
        private const val TRANQUILITY = "npc.brain_island_tranquility"
        private const val ASKELADDEN = "npc.viking_askelapen_dagganoth_mountain"
        private const val KEG_OF_BEER = "obj.keg_of_beer"
        private val BLADES =
            listOf(
                listOf(3, 1) to HARRYS_CUTLASS,
                listOf(3, 2) to LUCKY_CUTLASS,
                listOf(3, 3) to MALLARDS_RAPIER,
            )
        private val BLADE_PRICES =
            mapOf(HARRYS_CUTLASS to 1_040, LUCKY_CUTLASS to 2_560, MALLARDS_RAPIER to 25_600)
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
