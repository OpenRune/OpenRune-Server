package org.rsmod.content.other.special.weapons

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.content.other.special.weapons.scripts.charge.ZulrahParts
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class ZulrahPartsTest {
    @Test fun `all scale parts dismantle for 20000 with full inventory and cancellation is safe`() {
        for (part in ZulrahParts.scaleParts) for (accept in listOf(false, true)) {
            val f = Fixture(99); f.put(0, part)
            for (slot in 1..27) f.put(slot, "obj.abyssal_whip")
            f.dismantle(); f.finish(accept)
            assertEquals(if (accept) 20000 else 0, f.player.inv.count(ZulrahParts.SCALES))
            assertEquals(if (accept) 0 else 1, f.player.inv.count(part))
            assertEquals(27, f.player.inv.count("obj.abyssal_whip"))
        }
    }

    @Test fun `toxic weapons return fang and base and rollback when inventory is full`() {
        for ((toxic, base) in ZulrahParts.detachable + ZulrahParts.ornamentedTridents) for (full in listOf(false, true)) {
            val f = Fixture(99); f.put(0, toxic)
            if (full) for (slot in 1..27) f.put(slot, "obj.abyssal_whip")
            f.dismantle(); f.finish()
            assertEquals(if (full) 1 else 0, f.player.inv.count(toxic))
            assertEquals(if (full) 0 else 1, f.player.inv.count(base))
            assertEquals(if (full) 0 else 1, f.player.inv.count("obj.magic_fang"))
        }
    }

    @Test fun `mutagens preserve charged and empty helm state in both item orders`() {
        for (colour in listOf("cyan", "red")) for (charged in listOf(false, true)) for (reverse in listOf(false, true)) {
            val f = Fixture(99); f.put(0, "obj.serpentine_helm"); f.put(1, "obj.${colour}_mutagen")
            if (charged) {
                f.player.inv[2] = InvObj(ZulrahParts.SCALES, 1234)
                assertTrue(ZulrahParts.charge(f.player, f.player.inv, 0, 2))
            }
            f.use(if (reverse) 1 else 0, if (reverse) 0 else 1); f.finish()
            val output = "obj.serpentine_helm${if (charged) "_charged" else ""}_$colour"
            assertEquals(1, f.player.inv.count(output)); assertEquals(0, f.player.inv.count("obj.${colour}_mutagen"))
            assertEquals(if (charged) 1234 else 0, ZulrahParts.scales(f.player.inv[0]!!))
            f.result = null; f.dismantle(); f.finish()
            assertEquals(1, f.player.inv.count("obj.serpentine_helm${if (charged) "_charged" else ""}"))
            assertEquals(if (charged) 1234 else 0, ZulrahParts.scales(f.player.inv[0]!!))
        }
    }

    @Test fun `uncharge refunds exact scales and fails atomically without room`() {
        for ((empty, charged) in ZulrahParts.scaleEquipment) {
            val f = Fixture(99); f.put(0, empty); f.player.inv[1] = InvObj(ZulrahParts.SCALES, 12000)
            assertTrue(ZulrahParts.charge(f.player, f.player.inv, 0, 1))
            assertEquals(11000, ZulrahParts.scales(f.player.inv[0]!!)); assertEquals(1000, f.player.inv.count(ZulrahParts.SCALES))
            f.put(1, "obj.abyssal_whip"); for (slot in 2..27) f.put(slot, "obj.abyssal_whip")
            val original = f.player.inv[0]!!
            assertFalse(ZulrahParts.uncharge(f.player, f.player.inv, 0, original)); assertSame(original, f.player.inv[0])
            f.player.inv[1] = null
            assertTrue(ZulrahParts.uncharge(f.player, f.player.inv, 0, original))
            assertEquals(1, f.player.inv.count(empty)); assertEquals(11000, f.player.inv.count(ZulrahParts.SCALES))
            assertFalse(ZulrahParts.uncharge(f.player, f.player.inv, 0, original))
        }
    }
    private class Fixture(level: Int) {
        val events = EventBus()
        val coroutine = GameCoroutine("spirit-shield-test")
        var result: Result<Unit>? = null
        val context = ProtectedAccessContextFactory.empty().copy(
            getEventBus = { events }, getAlignment = { TextAlignment() },
        )
        val player = Player(RecordingClient()).apply {
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
            statMap.setCurrentLevel("stat.crafting", level.toByte())
            statMap.setCurrentLevel("stat.fletching", level.toByte())
        }
        init {
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }
            with(ZulrahParts()) { scripts.startup() }
        }
        fun put(slot: Int, item: String) { player.inv[slot] = InvObj(item, 1) }
        fun dismantle() = launch {
            val obj = player.inv[0]!!
            val type = ServerCacheManager.getItem(obj.id)!!
            if (type.interfaceOptions[4] == "Dismantle") assertTrue(events.publish(this, HeldObjEvents.Op5(0, obj, type, player.inv)))
            else assertTrue(events.publish(this, HeldObjEvents.Op4(0, obj, type, player.inv)))
        }
        fun use(first: Int, second: Int) = launch {
            HeldUInteractions(events).interact(this, player.inv,
                ServerCacheManager.getItem(player.inv[first]!!.id)!!, first,
                ServerCacheManager.getItem(player.inv[second]!!.id)!!, second)
        }
        fun launch(action: suspend ProtectedAccess.() -> Unit) {
            player.activeCoroutine = coroutine
            val block: suspend () -> Unit = { action(ProtectedAccess(player, coroutine, context)) }
            block.startCoroutine(object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) { this@Fixture.result = result }
            })
            result?.getOrThrow()
        }
        fun finish(accept: Boolean = true) {
            repeat(200) {
                result?.getOrThrow()
                if (result != null) return
                if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
                    val menu = player.ui.containsModal("interface.chatmenu")
                    val component = when {
                        menu -> "component.chatmenu:options"
                        player.ui.containsModal("interface.objectbox") -> "component.objectbox:universe"
                        else -> "component.messagebox:continue"
                    }
                    coroutine.resumeWith(ResumePauseButtonInput(component, if (menu) { if (accept) 1 else 2 } else -1))
                } else {
                    player.currentMapClock++
                    player.processedMapClock = player.currentMapClock
                    coroutine.advance()
                }
            }
            fail<Unit>("Crafting interaction did not finish")
        }
    }
    private class RecordingClient : Client<Any, Any> {
        override fun write(message: Any) = Unit
        override fun close() = Unit
        override fun read(player: Player) = Unit
        override fun flush() = Unit
        override fun flushHighPriority() = Unit
        override fun unregister(service: Any, player: Player) = Unit
    }
    companion object { @JvmStatic @BeforeAll fun initCache() { ServerCacheManager.init(240).close() } }
}
