package org.rsmod.content.areas.misc.lobbies.pestcontrol

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.interfaces.collectionlog.collectionTransmit
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
@OptIn(InternalApi::class)
class PestControlRefundsTest {
    @TestFactory
    fun `each accepted item refunds its fixed value exactly once without parchment or log credit`() =
        REFUNDS.map { (item, points) ->
            DynamicTest.dynamicTest("$item refunds $points points") {
                val f = Fixture(points = 17)
                val slot = f.give(item)
                f.give(item)
                VarPlayerIntMapSetter.set(f.player, "varp.if1", 3999)
                f.offer(slot)
                f.awaitOffer()
                assertEquals(mapOf(item to 2), f.totals())
                assertEquals(17, f.player.pestControlPoints)
                f.choose(1)
                f.finish()
                assertEquals(17 + points, f.player.pestControlPoints)
                assertEquals(mapOf(item to 1), f.totals())
                assertEquals(emptyMap<String, Int>(), f.totals(f.player.collectionTransmit))
            }
        }

    @Test
    fun `declining leaves the source item and persistent balance untouched`() {
        val f = Fixture(points = 731)
        val slot = f.give(GLOVES)
        val before = f.totals()
        f.offer(slot)
        f.awaitOffer()
        f.choose(2)
        f.finish()
        assertEquals(before, f.totals())
        assertEquals(731, f.player.pestControlPoints)
        assertEquals(emptyMap<String, Int>(), f.totals(f.player.collectionTransmit))
    }

    @Test
    fun `vanished source cannot consume an identical item in another slot or credit points`() {
        val f = Fixture(points = 19)
        val slot = f.give(GLOVES)
        f.give(GLOVES)
        f.offer(slot)
        f.awaitOffer()
        f.player.inv[slot] = null
        f.choose(1)
        f.finish()
        assertEquals(mapOf(GLOVES to 1), f.totals())
        assertEquals(19, f.player.pestControlPoints)
    }

    @Test
    fun `swapped source cannot remove the replacement or credit the original quote`() {
        for (replacement in listOf(GLOVES, TOP)) {
            val f = Fixture(points = 19)
            val slot = f.give(GLOVES)
            f.offer(slot)
            f.awaitOffer()
            f.player.inv[slot] = InvObj(replacement, 1)
            f.choose(1)
            f.finish()
            assertEquals(mapOf(replacement to 1), f.totals())
            assertEquals(19, f.player.pestControlPoints)
        }
    }

    @Test
    fun `replayed accept and a stale source slot cannot refund the next identical item`() {
        val f = Fixture(points = 19)
        val slot = f.give(GLOVES)
        f.give(GLOVES)
        f.offer(slot)
        f.awaitOffer()
        f.choose(1)
        f.finish()
        repeat(2) { f.replayAccept() }
        f.offer(slot)
        f.finish()
        assertEquals(139, f.player.pestControlPoints)
        assertEquals(mapOf(GLOVES to 1), f.totals())
    }

    @Test
    fun `full inventory still consumes only the refunded item and credits points`() {
        val f = Fixture(points = 80)
        val slot = f.give(TOP)
        f.fill()
        assertEquals(0, f.player.inv.freeSpace())
        f.offer(slot)
        f.awaitOffer()
        f.choose(1)
        f.finish()
        assertEquals(280, f.player.pestControlPoints)
        assertEquals(mapOf("obj.bronze_dagger" to 27), f.totals())
        assertEquals(emptyMap<String, Int>(), f.totals(f.player.collectionTransmit))
    }

    @Test
    fun `3999 and 4000 balances refund only remaining capacity and never exceed the cap`() {
        for (balance in listOf(3999, 4000)) {
            val f = Fixture(points = balance)
            val slot = f.give(TOP)
            f.offer(slot)
            f.awaitOffer()
            assertEquals(balance, f.player.pestControlPoints)
            assertEquals(mapOf(TOP to 1), f.totals())
            f.choose(1)
            f.finish()
            assertEquals(4000, f.player.pestControlPoints)
            assertEquals(emptyMap<String, Int>(), f.totals())
        }
    }

    @Test
    fun `quote that no longer fits requires a fresh offer without consuming the item`() {
        for ((balance, changed) in listOf(
            0 to 3999, 0 to 4000, 3999 to 4000,
        )) {
            val f = Fixture(points = balance)
            val slot = f.give(TOP)
            f.offer(slot)
            f.awaitOffer()
            f.player.pestControlPoints = changed
            f.choose(1)
            f.finish()
            assertEquals(changed, f.player.pestControlPoints)
            assertEquals(mapOf(TOP to 1), f.totals())
            f.offer(slot)
            f.awaitOffer()
            f.choose(1)
            f.finish()
            assertEquals(4000, f.player.pestControlPoints)
            assertEquals(emptyMap<String, Int>(), f.totals())
        }
    }

    @Test
    fun `balance changes with sufficient room honour the accepted quote without silent repricing`() {
        for ((balance, changed, expected) in listOf(
            Triple(3999, 0, 1), Triple(0, 17, 217), Triple(4000, 0, 0),
        )) {
            val f = Fixture(points = balance)
            val slot = f.give(TOP)
            f.offer(slot)
            f.awaitOffer()
            f.player.pestControlPoints = changed
            f.choose(1)
            f.finish()
            assertEquals(expected, f.player.pestControlPoints)
            assertEquals(emptyMap<String, Int>(), f.totals())
        }
    }

    @Test
    fun `seals broken equipment and ornamented equipment are refused without mutation`() {
        for (item in REFUSED) {
            val f = Fixture(points = 731)
            val slot = f.give(item)
            f.offer(slot)
            f.finish()
            assertEquals(mapOf(item to 1), f.totals(), item)
            assertEquals(731, f.player.pestControlPoints, item)
            assertEquals(emptyMap<String, Int>(), f.totals(f.player.collectionTransmit), item)
        }
    }

    private class Fixture(points: Int) {
        private val events = EventBus()
        private val client = RecordingClient()
        val player = Player(client).apply {
            uuid = 796L
            observerUUID = 796L
            slotId = 1
            assignUid()
            coords = CoordGrid(2663, 2649, 0)
            currentMapClock = 100
            processedMapClock = 100
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
            invMap["inv.inv"] = inv
            invMap["inv.worn"] = worn
        }
        private val context = ProtectedAccessContextFactory.empty().copy(
            getEventBus = { events }, getAlignment = { TextAlignment() },
        )
        private var coroutine = GameCoroutine("pest-refund-test")
        private var result: Result<Unit>? = null

        init {
            player.pestControlPoints = points
        }

        fun offer(slot: Int) {
            assertTrue(coroutine.isIdle, "Previous refund interaction is still suspended")
            coroutine = GameCoroutine("pest-refund-test")
            player.activeCoroutine = coroutine
            result = null
            client.messages.clear()
            val npc = Npc("npc.pest_squire_refunds", player.coords.translateZ(1))
            val action: suspend () -> Unit = {
                ProtectedAccess(player, coroutine, context).offerPestControlRefund(npc, slot)
            }
            action.startCoroutine(object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) { this@Fixture.result = result }
            })
            result?.getOrThrow()
        }

        fun awaitOffer() {
            repeat(100) {
                if (player.ui.containsModal("interface.chatmenu")) {
                    assertTrue(coroutine.isAwaiting(ResumePauseButtonInput::class))
                    assertTrue(result == null, "Offer completed without waiting for consent")
                    return
                }
                assertFalse(coroutine.isIdle, "Refund ended before confirmation")
                advance()
            }
            error("Refund confirmation did not appear")
        }

        fun choose(option: Int) {
            assertTrue(player.ui.containsModal("interface.chatmenu"))
            player.resumeActiveCoroutine(ResumePauseButtonInput("component.chatmenu:options", option))
            result?.getOrThrow()
        }

        fun replayAccept() {
            player.resumeActiveCoroutine(ResumePauseButtonInput("component.chatmenu:options", 1))
            result?.getOrThrow()
        }

        fun finish() {
            repeat(100) {
                if (coroutine.isIdle) {
                    result?.getOrThrow()
                    assertTrue(result?.isSuccess == true, "Refund coroutine did not complete")
                    player.advanceActiveCoroutine()
                    return
                }
                assertFalse(player.ui.containsModal("interface.chatmenu"), "Unexpected confirmation")
                advance()
            }
            error("Refund interaction did not finish")
        }

        private fun advance() {
            if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
                val component = CONTINUE_COMPONENTS.entries
                    .firstOrNull { player.ui.containsModal(it.key) }?.value
                    ?: error("Unknown refund dialogue")
                player.resumeActiveCoroutine(ResumePauseButtonInput(component, -1))
            } else {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
                player.advanceActiveCoroutine()
            }
            result?.getOrThrow()
        }

        fun give(item: String): Int {
            val slot = (0 until 28).first { player.inv[it] == null }
            player.inv[slot] = InvObj(item, 1)
            return slot
        }

        fun fill() = repeat(player.inv.freeSpace()) { give("obj.bronze_dagger") }

        fun totals(inventory: Inventory = player.inv): Map<String, Int> = buildMap {
            for (obj in inventory.objs) {
                if (obj == null) continue
                val name = checkNotNull(ServerCacheManager.getItem(obj.id)).internalName
                put(name, (get(name) ?: 0) + obj.count)
            }
        }
    }

    private class RecordingClient : Client<Any, Any> {
        val messages = mutableListOf<Any>()
        override fun write(message: Any) { messages += message }
        override fun close() {}
        override fun read(player: Player) {}
        override fun flush() {}
        override fun flushHighPriority() {}
        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
        private const val GLOVES = "obj.pest_void_knight_gloves"
        private const val TOP = "obj.pest_void_knight_top"
        private val REFUNDS = mapOf(
            "obj.pest_void_knight_mace" to 200,
            TOP to 200,
            "obj.pest_void_knight_robes" to 200,
            GLOVES to 120,
            "obj.game_pest_mage_helm" to 160,
            "obj.game_pest_archer_helm" to 160,
            "obj.game_pest_melee_helm" to 160,
            "obj.elite_void_knight_top" to 360,
            "obj.elite_void_knight_robes" to 360,
            "obj.pest_void_knight_mace_trouver" to 200,
            "obj.pest_void_knight_top_trouver" to 200,
            "obj.pest_void_knight_robes_trouver" to 200,
            "obj.pest_void_knight_gloves_trouver" to 120,
            "obj.game_pest_mage_helm_trouver" to 160,
            "obj.game_pest_archer_helm_trouver" to 160,
            "obj.game_pest_melee_helm_trouver" to 160,
            "obj.elite_void_knight_top_trouver" to 360,
            "obj.elite_void_knight_robes_trouver" to 360,
        )
        private val REFUSED = (1..8).map { "obj.pest_seal_$it" } + listOf(
            "obj.pest_void_knight_top_broken",
            "obj.elite_void_knight_top_broken",
            "obj.game_pest_mage_helm_broken",
            "obj.game_pest_melee_helm_trouver_broken",
            "obj.game_pest_melee_helm_trouver_mangled",
            "obj.league_3_void_knight_top",
            "obj.league_3_void_knight_top_trouver",
            "obj.league_3_void_knight_top_elite",
            "obj.league_3_void_knight_top_elite_trouver",
            "obj.league_3_void_mage_helm",
            "obj.league_3_void_mage_helm_trouver",
        )
        private val CONTINUE_COMPONENTS = mapOf(
            "interface.chat_left" to "component.chat_left:continue",
            "interface.chat_right" to "component.chat_right:continue",
            "interface.messagebox" to "component.messagebox:continue",
            "interface.objectbox" to "component.objectbox:universe",
        )
        private val RESTORE = mutableListOf<() -> Unit>()

        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(241).close()
            for ((owner, name) in listOf(
                "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
            )) {
                val field = Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                RESTORE += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            RESTORE += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic
        @AfterAll
        fun restore() {
            RESTORE.asReversed().forEach { it() }
            RESTORE.clear()
        }
    }
}
