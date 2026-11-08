package org.rsmod.content.areas.misc.dwarvenmine.npcs

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import io.mockk.every
import io.mockk.mockk
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
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.shops.Shops
import org.rsmod.content.other.multicannon.DwarfMulticannon
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

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class NulodionLostCannonTest {
    @Test
    fun `a lost cannon at saved stage k gives exactly its first k parts`() {
        for (stage in 1..4) {
            val f = Fixture(stage = stage, decayed = true)
            val before = f.totals()
            f.lostCannon()
            assertEquals(before + PLAIN.take(stage).associateWith { 1 }, f.totals(), "stage $stage")
            f.assertReset("stage $stage")
            assertTrue(f.output().contains(OBJBOX_LINES[stage - 1]), "stage $stage: ${f.output()}")
        }
    }

    @Test
    fun `an ornamented lost cannon gives the ornamented parts`() {
        for (stage in 1..4) {
            val f = Fixture(stage = stage, ornament = true)
            val before = f.totals()
            f.lostCannon()
            assertEquals(before + ORNAMENT.take(stage).associateWith { 1 }, f.totals(), "stage $stage")
            f.assertReset("stage $stage")
        }
    }

    @Test
    fun `fewer free slots than parts gives nothing and keeps the stage`() {
        for (stage in 1..4) {
            val f = Fixture(stage = stage, freeSlots = stage - 1)
            val before = f.totals()
            f.lostCannon()
            assertEquals(before, f.totals(), "stage $stage")
            assertEquals(stage, f.player.vars["varp.dropcannon"], "stage $stage")
            assertEquals(AMMO, f.player.vars["varp.rockthrower"], "stage $stage")
            assertTrue(f.output().contains("if you free up some inventory space"), "stage $stage: ${f.output()}")
        }
    }

    @Test
    fun `exactly k free slots is enough`() {
        for (stage in 1..4) {
            val f = Fixture(stage = stage, freeSlots = stage)
            val before = f.totals()
            f.lostCannon()
            assertEquals(before + PLAIN.take(stage).associateWith { 1 }, f.totals(), "stage $stage")
            f.assertReset("stage $stage")
        }
    }

    @Test
    fun `asking again after a replacement gives nothing more`() {
        for (stage in 1..4) {
            val f = Fixture(stage = stage)
            val before = f.totals()
            f.lostCannon()
            f.lostCannon()
            assertEquals(before + PLAIN.take(stage).associateWith { 1 }, f.totals(), "stage $stage")
            assertTrue(f.output().contains(STAGE_ZERO_LINE), "stage $stage: ${f.output()}")
        }
    }

    @Test
    fun `no saved cannon gives nothing`() {
        val f = Fixture(stage = 0)
        val before = f.totals()
        f.lostCannon()
        assertEquals(before, f.totals())
        assertTrue(f.output().contains(STAGE_ZERO_LINE), f.output())
    }

    @Test
    fun `a cannon still set up gives nothing`() {
        for (stage in 1..4) {
            val f = Fixture(stage = stage, setUp = true)
            val before = f.totals()
            f.lostCannon()
            assertEquals(before, f.totals(), "stage $stage")
            assertEquals(stage, f.player.vars["varp.dropcannon"], "stage $stage")
            assertTrue(f.output().contains("still happily parked"), "stage $stage: ${f.output()}")
        }
    }

    private class Fixture(
        stage: Int,
        decayed: Boolean = false,
        ornament: Boolean = false,
        freeSlots: Int = 27,
        setUp: Boolean = false,
    ) {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("nulodion-lost-cannon-test")
        private var result: Result<Unit>? = null
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
                coords = CoordGrid(3012, 3453, 0)
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
            val multicannon = mockk<DwarfMulticannon> { every { isSetUp(any()) } returns setUp }
            with(Nulodion(Shops(EventBus()), multicannon)) {
                ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup()
            }
            player.inv[0] = InvObj("obj.coins", COINS)
            for (slot in 1 until 28 - freeSlots) {
                player.inv[slot] = InvObj("obj.bronze_dagger", 1)
            }
            VarPlayerIntMapSetter.set(player, "varp.dropcannon", stage)
            VarPlayerIntMapSetter.set(player, "varbit.mcannon_decayed", if (decayed) 1 else 0)
            VarPlayerIntMapSetter.set(player, "varp.mcannon_ornament", if (ornament) 1 else 0)
            VarPlayerIntMapSetter.set(player, "varp.rockthrower", AMMO)
        }

        fun lostCannon() {
            talk()
            finish(listOf(LOST_CANNON_OPTION))
        }

        fun assertReset(case: String) {
            assertEquals(0, player.vars["varp.dropcannon"], case)
            assertEquals(0, player.vars["varbit.mcannon_decayed"], case)
            assertEquals(0, player.vars["varp.mcannon_ornament"], case)
            assertEquals(AMMO, player.vars["varp.rockthrower"], case)
        }

        fun totals(): Map<String, Int> {
            val totals = mutableMapOf<String, Int>()
            for (slot in 0 until 28) {
                val obj = player.inv[slot] ?: continue
                val name = checkNotNull(ServerCacheManager.getItem(obj.id)).internalName
                totals.merge(name, obj.count, Int::plus)
            }
            return totals
        }

        private fun access() = ProtectedAccess(player, coroutine, context)

        private fun talk() {
            while (player.isDelayed) {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
            }
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            val npc = Npc("npc.nulodion", player.coords.translateZ(1))
            val block: suspend () -> Unit = {
                assertTrue(events.publish(access(), NpcEvents.Op1(npc)))
            }
            block.startCoroutine(
                object : Continuation<Unit> {
                    override val context = EmptyCoroutineContext

                    override fun resumeWith(result: Result<Unit>) {
                        this@Fixture.result = result
                    }
                }
            )
            result?.getOrThrow()
        }

        private fun finish(options: List<Int>) {
            val selections = options.iterator()
            repeat(150) {
                if (coroutine.isIdle) return
                advance(selections)
            }
            fail<Unit>("Interaction did not finish: ${output()}")
        }

        private fun advance(options: Iterator<Int>) {
            if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
                val component =
                    when {
                        player.ui.containsModal("interface.chatmenu") -> "component.chatmenu:options"
                        player.ui.containsModal("interface.objectbox") -> "component.objectbox:universe"
                        player.ui.containsModal("interface.objectbox_double") ->
                            "component.objectbox_double:pausebutton"
                        else ->
                            listOf("chat_left", "chat_right", "messagebox")
                                .firstOrNull { player.ui.containsModal("interface.$it") }
                                ?.let { "component.$it:continue" } ?: error("Unknown dialogue: ${output()}")
                    }
                val sub =
                    if (component == "component.chatmenu:options") {
                        if (options.hasNext()) options.next() else 1
                    } else {
                        -1
                    }
                coroutine.resumeWith(ResumePauseButtonInput(component, sub))
            } else {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
                coroutine.advance()
            }
            result?.getOrThrow()
        }

        fun output() = client.messages.joinToString("\n").replace("<br>", " ")
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
        private const val COINS = 100
        private const val AMMO = 25
        private const val LOST_CANNON_OPTION = 2
        private const val STAGE_ZERO_LINE = "only allowed to replace cannons that were stolen in action"
        private val PLAIN = listOf("obj.twpart1", "obj.twpart2", "obj.twpart3", "obj.twpart4")
        private val ORNAMENT =
            listOf(
                "obj.league_3_multicannon_base",
                "obj.league_3_multicannon_stand",
                "obj.league_3_multicannon_barrels",
                "obj.league_3_multicannon_furnace",
            )
        private val OBJBOX_LINES =
            listOf(
                "The dwarf gives you a new cannon part.",
                "The dwarf gives you new cannon parts.",
                "The dwarf gives you new cannon parts.",
                "The dwarf gives you a new cannon.",
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
