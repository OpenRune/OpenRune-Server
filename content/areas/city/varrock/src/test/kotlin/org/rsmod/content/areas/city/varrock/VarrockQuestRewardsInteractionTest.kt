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
import org.rsmod.api.shops.Shops
import org.rsmod.content.areas.city.varrock.npcs.Apothecary
import org.rsmod.content.areas.city.varrock.npcs.RatPits
import org.rsmod.content.areas.city.varrock.npcs.VarrockPalace
import org.rsmod.content.areas.city.varrock.npcs.VarrockTownsfolk
import org.rsmod.content.areas.city.varrock.npcs.shops.StaffShop
import org.rsmod.content.quest.manager.QuestRequirementMode
import org.rsmod.content.quest.manager.QuestRequirementPolicy
import org.rsmod.content.quest.manager.QuestRequirements
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
@ResourceLock("QuestRequirements")
class VarrockQuestRewardsInteractionTest {
    @Test fun `Zaff hands over one beacon ring`() {
        val f = Fixture()
        f.talkTo(ZAFF)
        f.finish(listOf(3))
        assertEquals(1, f.player.inv.count(BEACON_RING))
    }

    @Test fun `Zaff gives no second beacon ring`() {
        for (inBank in listOf(false, true)) {
            val f = Fixture()
            val inv = if (inBank) f.bank else f.player.inv
            inv[0] = InvObj(BEACON_RING, 1)
            f.talkTo(ZAFF)
            f.finish(listOf(3))
            assertEquals(if (inBank) 0 else 1, f.player.inv.count(BEACON_RING), "bank=$inBank")
        }
    }

    @Test fun `Zaff keeps the ring from a full pack`() {
        val f = Fixture()
        f.fillInventory()
        f.talkTo(ZAFF)
        f.finish(listOf(3))
        assertEquals(0, f.player.inv.count(BEACON_RING))
        assertEquals(0, f.groundCount(BEACON_RING))
    }

    @Test fun `the Apothecary replaces a lost nurse hat`() {
        val f = Fixture()
        f.talkTo(APOTHECARY)
        f.finish(listOf(2, 1))
        assertEquals(1, f.player.inv.count(NURSE_HAT))
        assertEquals(0, f.player.inv.count(DOCTOR_HAT))
    }

    @Test fun `the Apothecary gives no hat to a player who still has one`() {
        val f = Fixture()
        f.bank[0] = InvObj(DOCTOR_HAT, 1)
        f.talkTo(APOTHECARY)
        f.finish(listOf(1))
        assertEquals(0, f.player.inv.count(NURSE_HAT))
        assertEquals(0, f.player.inv.count(DOCTOR_HAT))
    }

    @Test fun `the Apothecary keeps the hat from a full pack`() {
        val f = Fixture()
        f.fillInventory()
        f.talkTo(APOTHECARY)
        f.finish(listOf(2, 1))
        assertEquals(0, f.player.inv.count(NURSE_HAT))
        assertEquals(0, f.groundCount(NURSE_HAT))
    }

    @Test fun `the Sisters Grime replace one lost rat pole`() {
        val f = Fixture()
        f.talkTo(GRIMESQUIT)
        f.finish(listOf(3))
        assertEquals(1, f.player.inv.count(RAT_POLE))
    }

    @Test fun `the Sisters Grime give no pole to a player who has one`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(RAT_POLE_3, 1)
        f.talkTo(GRIMESQUIT)
        f.finish(listOf(3))
        assertEquals(0, f.player.inv.count(RAT_POLE))
        assertEquals(1, f.player.inv.count(RAT_POLE_3))
    }

    @Test fun `a full pack drops the rat pole instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.talkTo(PHINGSPET)
        f.finish(listOf(3))
        assertEquals(1, f.groundCount(RAT_POLE))
    }

    @Test fun `Hooknosed Jack hands over one rat pits manual`() {
        val f = Fixture()
        f.talkTo(HOOKNOSED_JACK)
        f.finish(listOf(2, 2, 2))
        assertEquals(1, f.player.inv.count(MANUAL))

        f.talkTo(HOOKNOSED_JACK)
        f.finish(listOf(2, 2, 2))
        assertEquals(1, f.player.inv.count(MANUAL))
    }

    @Test fun `Johnathon turns cooking gauntlets into chaos gauntlets for 25,000 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 30_000)
        f.player.inv[1] = InvObj(COOKING_GAUNTLETS, 1)
        f.talkTo(JOHNATHON)
        f.finish(listOf(1, 1))
        assertEquals(5_000, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(COOKING_GAUNTLETS))
        assertEquals(1, f.player.inv.count(CHAOS_GAUNTLETS))
    }

    @Test fun `Johnathon keeps the gauntlets of a player short of coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 24_999)
        f.player.inv[1] = InvObj(COOKING_GAUNTLETS, 1)
        f.talkTo(JOHNATHON)
        f.finish(listOf(1, 1))
        assertEquals(24_999, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(COOKING_GAUNTLETS))
        assertEquals(0, f.player.inv.count(CHAOS_GAUNTLETS))
    }

    @Test fun `declining Johnathon costs nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 25_000)
        f.player.inv[1] = InvObj(COOKING_GAUNTLETS, 1)
        f.talkTo(JOHNATHON)
        f.finish(listOf(1, 2))
        assertEquals(25_000, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(COOKING_GAUNTLETS))
    }

    @Test fun `Johnathon enchants steel gauntlets for free`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(STEEL_GAUNTLETS, 1)
        f.talkTo(JOHNATHON)
        f.finish(listOf(1))
        assertEquals(0, f.player.inv.count(STEEL_GAUNTLETS))
        assertEquals(1, f.player.inv.count(CHAOS_GAUNTLETS))
    }

    @Test fun `Sir Prysin sells a Darklight for 1000 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 1_500)
        f.talkTo(SIR_PRYSIN)
        f.finish(listOf(2, 2, 2))
        assertEquals(500, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(DARKLIGHT))
    }

    @Test fun `Sir Prysin sells nothing to a player short of coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 999)
        f.talkTo(SIR_PRYSIN)
        f.finish(listOf(2, 2, 2))
        assertEquals(999, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(DARKLIGHT))
    }

    @Test fun `Sir Prysin keeps the coins of a player with a full pack`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 1_500)
        f.talkTo(SIR_PRYSIN)
        f.finish(listOf(2, 2, 2))
        assertEquals(1_500, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(DARKLIGHT))
        assertEquals(0, f.groundCount(DARKLIGHT))
    }

    @Test fun `Sir Prysin offers no Darklight to a player who owns a Silverlight`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 1_500)
        f.bank[0] = InvObj(SILVERLIGHT, 1)
        f.talkTo(SIR_PRYSIN)
        f.finish(listOf(2, 2, 2))
        assertEquals(1_500, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(DARKLIGHT))
    }

    @Test fun `Anna Jones hands over a bronze pickaxe`() {
        val f = Fixture()
        f.talkTo(ANNA_JONES)
        f.finish(listOf(5))
        assertEquals(1, f.player.inv.count(BRONZE_PICKAXE))
    }

    @Test fun `a full pack drops Anna's pickaxe instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.talkTo(ANNA_JONES)
        f.finish(listOf(5))
        assertEquals(1, f.groundCount(BRONZE_PICKAXE))
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("varrock-quest-rewards-test")
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

        val bank: Inventory = Inventory.create("inv.bank")

        @OptIn(InternalApi::class)
        val player =
            Player().apply {
                this.client = this@Fixture.client
                uuid = 796L
                observerUUID = 796L
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
                invMap["inv.inv"] = inv
                invMap["inv.worn"] = worn
                invMap["inv.bank"] = bank
            }

        init {
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            val objRepo = ObjRepository(clock, objRegistry)
            with(StaffShop(Shops(events))) { scripts.startup() }
            with(Apothecary()) { scripts.startup() }
            with(RatPits(objRepo)) { scripts.startup() }
            with(VarrockTownsfolk(objRepo)) { scripts.startup() }
            with(VarrockPalace(objRepo)) { scripts.startup() }
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
        private const val ZAFF = "npc.zaff"
        private const val BEACON_RING = "obj.surok_ring"
        private const val APOTHECARY = "npc.apothecary"
        private const val NURSE_HAT = "obj.twocats_nurses_hat"
        private const val DOCTOR_HAT = "obj.twocats_doctors_hat"
        private const val GRIMESQUIT = "npc.vc_grimesquit"
        private const val PHINGSPET = "npc.vc_phingspet"
        private const val RAT_POLE = "obj.vc_rat_pole"
        private const val RAT_POLE_3 = "obj.vc_rat_pole_3"
        private const val HOOKNOSED_JACK = "npc.vc_hooknosed_jack"
        private const val MANUAL = "obj.ratpits_manual"
        private const val JOHNATHON = "npc.johnathon_fitzharmon"
        private const val STEEL_GAUNTLETS = "obj.steel_gauntlets"
        private const val COOKING_GAUNTLETS = "obj.gauntlets_of_cooking"
        private const val CHAOS_GAUNTLETS = "obj.gauntlets_of_chaos"
        private const val SIR_PRYSIN = "npc.sir_prysin"
        private const val DARKLIGHT = "obj.darklight"
        private const val SILVERLIGHT = "obj.silverlight"
        private const val ANNA_JONES = "npc.surok_annajones"
        private const val BRONZE_PICKAXE = "obj.bronze_pickaxe"
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
            val previousPolicy = QuestRequirements.activePolicy()
            QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.AssumeCompleted))
            restored += { QuestRequirements.install(previousPolicy) }
        }

        @JvmStatic
        @AfterAll
        fun restore() {
            restored.asReversed().forEach { it() }
            restored.clear()
        }
    }
}
