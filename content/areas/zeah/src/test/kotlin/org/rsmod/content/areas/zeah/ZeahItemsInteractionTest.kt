package org.rsmod.content.areas.zeah

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.util.Wearpos
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
import org.rsmod.api.combat.commons.magic.Spellbook
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
import org.rsmod.api.spells.autocast.AutocastSpells
import org.rsmod.api.spells.autocast.AutocastWeapons
import org.rsmod.api.spells.autocast.MagicSpellbookManager
import org.rsmod.content.areas.zeah.arceuus.npcs.Tyss
import org.rsmod.content.areas.zeah.hosidius.npcs.Konoo
import org.rsmod.content.areas.zeah.hosidius.npcs.Sandicrahb
import org.rsmod.content.areas.zeah.hosidius.npcs.Smoggy
import org.rsmod.content.areas.zeah.molch.npcs.AlryTheAngler
import org.rsmod.content.areas.zeah.piscarilius.npcs.Bast
import org.rsmod.content.areas.zeah.wintertodt.npcs.WintertodtCat
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
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
import org.rsmod.routefinder.collision.CollisionFlagMap

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class ZeahItemsInteractionTest {
    @Test fun `Konoo gives one spade to a player without one`() {
        val f = Fixture()
        f.talkTo(KONOO)
        f.finish(listOf(3, 4))
        assertEquals(1, f.player.inv.count(SPADE))
    }

    @Test fun `Konoo keeps his spade from a player who has one`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(SPADE, 1)
        f.talkTo(KONOO)
        f.finish(listOf(3, 3, 4))
        assertEquals(1, f.player.inv.count(SPADE))
    }

    @Test fun `Konoo gives nothing to a full pack`() {
        val f = Fixture()
        f.fillInventory()
        f.talkTo(KONOO)
        f.finish(listOf(3, 4))
        assertEquals(0, f.player.inv.count(SPADE))
        assertEquals(0, f.groundCount(SPADE))
    }

    @Test fun `Smoggy gives one tinderbox to a player without one`() {
        val f = Fixture()
        f.talkTo(SMOGGY)
        f.finish(listOf(4, 4, 5))
        assertEquals(1, f.player.inv.count(TINDERBOX))
    }

    @Test fun `Smoggy gives nothing to a full pack`() {
        val f = Fixture()
        f.fillInventory()
        f.talkTo(SMOGGY)
        f.finish(listOf(4, 5))
        assertEquals(0, f.player.inv.count(TINDERBOX))
        assertEquals(0, f.groundCount(TINDERBOX))
    }

    @Test fun `Sandicrahb takes 10,000 coins and ferries the player to Crabclaw Isle`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 12_000)
        f.talkTo(SANDICRAHB)
        f.finish(listOf(1, 1))
        assertEquals(2_000, f.player.inv.count(COINS))
        assertEquals(ISLAND, f.player.coords)
    }

    @Test fun `asking why the fare is so high still charges it once`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 10_000)
        f.talkTo(SANDICRAHB)
        f.finish(listOf(1, 3, 1, 1))
        assertEquals(0, f.player.inv.count(COINS))
        assertEquals(ISLAND, f.player.coords)
    }

    @Test fun `Sandicrahb ferries nobody short of the fare`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 9_999)
        f.talkTo(SANDICRAHB)
        f.finish(listOf(1))
        assertEquals(9_999, f.player.inv.count(COINS))
        assertEquals(START, f.player.coords)
    }

    @Test fun `declining Sandicrahb's fare costs nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 10_000)
        f.talkTo(SANDICRAHB)
        f.finish(listOf(1, 2))
        assertEquals(10_000, f.player.inv.count(COINS))
        assertEquals(START, f.player.coords)
    }

    @Test fun `the return trip from Crabclaw Isle is free`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 100)
        f.talkTo(SANDICRAHB_ISLAND)
        f.finish(listOf(1))
        assertEquals(100, f.player.inv.count(COINS))
        assertEquals(BEACH, f.player.coords)
    }

    @Test fun `Bast sells a pint for 4 coins`() {
        for (options in listOf(listOf(1), listOf(2, 1))) {
            val f = Fixture()
            f.player.inv[0] = InvObj(COINS, 10)
            f.talkTo(BAST)
            f.finish(options)
            assertEquals(6, f.player.inv.count(COINS), options.toString())
            assertEquals(1, f.player.inv.count(BEER), options.toString())
        }
    }

    @Test fun `Bast sells nothing to a player short of coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 3)
        f.talkTo(BAST)
        f.finish(listOf(1))
        assertEquals(3, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(BEER))
    }

    @Test fun `Bast charges nothing when the pack is full`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 10)
        f.talkTo(BAST)
        f.finish(listOf(1))
        assertEquals(10, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(BEER))
        assertEquals(0, f.groundCount(BEER))
    }

    @Test fun `declining Bast's pint costs nothing`() {
        for (options in listOf(listOf(4), listOf(2, 2))) {
            val f = Fixture()
            f.player.inv[0] = InvObj(COINS, 10)
            f.talkTo(BAST)
            f.finish(options)
            assertEquals(10, f.player.inv.count(COINS), options.toString())
            assertEquals(0, f.player.inv.count(BEER), options.toString())
        }
    }

    @Test fun `Tyss gives an Arceuus signet`() {
        val f = Fixture()
        f.talkTo(TYSS)
        f.finish(listOf(3))
        assertEquals(1, f.player.inv.count(SIGNET))
    }

    @Test fun `Tyss gives no signet to a full pack`() {
        val f = Fixture()
        f.fillInventory()
        f.talkTo(TYSS)
        f.finish(listOf(3))
        assertEquals(0, f.player.inv.count(SIGNET))
        assertEquals(0, f.groundCount(SIGNET))
    }

    @Test fun `Tyss swaps the player onto the Arceuus spellbook and back`() {
        val f = Fixture()
        f.talkTo(TYSS)
        f.finish(listOf(2))
        assertEquals(Spellbook.Arceuus, f.spellbooks.activeSpellbook(f.player))
        f.talkTo(TYSS)
        f.finish(listOf(2))
        assertEquals(Spellbook.Standard, f.spellbooks.activeSpellbook(f.player))
    }

    @Test fun `the Wintertodt cat takes exactly one tuna`() {
        val f = Fixture()
        f.player.worn[Wearpos.Front.slot] = InvObj(CATSPEAK_AMULET, 1)
        f.player.inv[0] = InvObj(TUNA, 1)
        f.player.inv[1] = InvObj(TUNA, 1)
        f.talkTo(WINTERTODT_CAT)
        f.finish(listOf(1))
        assertEquals(1, f.player.inv.count(TUNA))
    }

    @Test fun `keeping the tuna from the Wintertodt cat costs nothing`() {
        val f = Fixture()
        f.player.worn[Wearpos.Front.slot] = InvObj(CATSPEAK_AMULET, 1)
        f.player.inv[0] = InvObj(TUNA, 1)
        f.talkTo(WINTERTODT_CAT)
        f.finish(listOf(2))
        assertEquals(1, f.player.inv.count(TUNA))
    }

    @Test fun `the Wintertodt cat takes no tuna without a catspeak amulet`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(TUNA, 1)
        f.talkTo(WINTERTODT_CAT)
        f.finish(emptyList())
        assertEquals(1, f.player.inv.count(TUNA))
    }

    @Test fun `Alry swaps one golden tench for 100 molch pearls`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(GOLDEN_TENCH, 1)
        f.player.inv[1] = InvObj(MOLCH_PEARL, 7)
        f.talkTo(ALRY)
        f.finish(listOf(4, 1, 2))
        assertEquals(0, f.player.inv.count(GOLDEN_TENCH))
        assertEquals(107, f.player.inv.count(MOLCH_PEARL))
    }

    @Test fun `Alry swaps the tench in a full pack`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(GOLDEN_TENCH, 1)
        f.talkTo(ALRY)
        f.finish(listOf(4, 1, 2))
        assertEquals(0, f.player.inv.count(GOLDEN_TENCH))
        assertEquals(100, f.player.inv.count(MOLCH_PEARL))
        assertEquals(0, f.groundCount(MOLCH_PEARL))
    }

    @Test fun `declining Alry's offer keeps the tench`() {
        for (options in listOf(listOf(4, 2), listOf(4, 1, 1))) {
            val f = Fixture()
            f.player.inv[0] = InvObj(GOLDEN_TENCH, 1)
            f.talkTo(ALRY)
            f.finish(options)
            assertEquals(1, f.player.inv.count(GOLDEN_TENCH), options.toString())
            assertEquals(0, f.player.inv.count(MOLCH_PEARL), options.toString())
        }
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("zeah-items-test")
        private var result: Result<Unit>? = null
        private val objRegistry = ObjRegistry(ZoneUpdateMap())
        val spellbooks = MagicSpellbookManager(AutocastWeapons(AutocastSpells()))
        private val collision =
            CollisionFlagMap().apply {
                for (coord in listOf(START, ISLAND, BEACH)) {
                    allocateIfAbsent(coord.x, coord.z, coord.level)
                }
            }
        private val context =
            ProtectedAccessContextFactory.empty()
                .copy(
                    getEventBus = { events },
                    getAlignment = { TextAlignment() },
                    getNpcInteractions = { NpcInteractions(events) },
                    getCollision = { collision },
                )

        @OptIn(InternalApi::class)
        val player =
            Player().apply {
                this.client = this@Fixture.client
                uuid = 795L
                observerUUID = 795L
                slotId = 1
                assignUid()
                coords = START
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
            with(Konoo()) { scripts.startup() }
            with(Smoggy()) { scripts.startup() }
            with(Sandicrahb()) { scripts.startup() }
            with(Bast()) { scripts.startup() }
            with(Tyss(spellbooks)) { scripts.startup() }
            with(WintertodtCat()) { scripts.startup() }
            with(AlryTheAngler()) { scripts.startup() }
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
        private const val SPADE = "obj.spade"
        private const val TINDERBOX = "obj.tinderbox"
        private const val BEER = "obj.beer"
        private const val BAST = "npc.kourend_entrance_bartender"
        private const val TYSS = "npc.arceuus_darkguardian"
        private const val SIGNET = "obj.poh_arceuus_signet"
        private const val KONOO = "npc.hosidius_saltpetre_miner"
        private const val SMOGGY = "npc.lovakengj_charcoal_keeper"
        private const val SANDICRAHB = "npc.tribes_boatman"
        private const val SANDICRAHB_ISLAND = "npc.tribes_boatman_island"
        private const val WINTERTODT_CAT = "npc.wint_troublecat"
        private const val CATSPEAK_AMULET = "obj.twocats_amuletofcatspeak"
        private const val TUNA = "obj.tuna"
        private const val ALRY = "npc.fishing_npc_angler"
        private const val GOLDEN_TENCH = "obj.golden_tench"
        private const val MOLCH_PEARL = "obj.aerial_fishing_pearl"
        private val START = CoordGrid(1782, 3457, 0)
        private val ISLAND = CoordGrid(1781, 3412, 0)
        private val BEACH = CoordGrid(1783, 3459, 0)
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
