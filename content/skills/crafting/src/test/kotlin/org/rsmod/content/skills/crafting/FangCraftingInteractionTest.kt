package org.rsmod.content.skills.crafting

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
import org.rsmod.content.skills.crafting.items.FangCraftingScript
import org.rsmod.content.skills.crafting.scripts.HeldCraftingScript
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
class FangCraftingInteractionTest {
    @Test fun `Etch replaces one fang in a full inventory and preserves the chisel without XP`() {
        for (fang in fangs) {
            val f = Fixture(fang.level)
            f.put(0, fang.raw); f.put(1, "obj.chisel")
            for (slot in 2..27) f.put(slot, "obj.abyssal_whip")
            f.etch(); f.finish()
            assertEquals(0, f.player.inv.count(fang.raw))
            assertEquals(1, f.player.inv.count(fang.etched))
            assertEquals(1, f.player.inv.count("obj.chisel"))
            assertEquals(26, f.player.inv.count("obj.abyssal_whip"))
            assertEquals(0, f.player.statMap.getXP("stat.crafting"))
        }
    }

    @Test fun `chisel on either fang works in both selected item orders`() {
        for (fang in fangs) for (reverse in listOf(false, true)) {
            val f = Fixture(fang.level)
            f.put(0, fang.raw); f.put(1, "obj.chisel")
            f.use(if (reverse) 1 else 0, if (reverse) 0 else 1); f.finish()
            assertEquals(1, f.player.inv.count(fang.etched))
            assertEquals(1, f.player.inv.count("obj.chisel"))
        }
    }

    @Test fun `etching rejects missing chisel and insufficient current crafting without consuming items`() {
        for (fang in fangs) for (missingChisel in listOf(false, true)) {
            val f = Fixture(if (missingChisel) fang.level else fang.level - 1)
            f.put(0, fang.raw)
            if (!missingChisel) f.put(1, "obj.chisel")
            val before = f.player.inv.objs.toList()
            f.etch(); f.finish()
            assertEquals(before, f.player.inv.objs.toList())
            assertEquals(0, f.player.statMap.getXP("stat.crafting"))
        }
    }

    @Test fun `etched fang makes rancour through real confirmation animations and inventory transactions in either order`() {
        for (reverse in listOf(false, true)) {
            val f = Fixture(86)
            f.put(0, "obj.etched_araxyte_fang"); f.put(1, "obj.zenyte_amulet_enchanted")
            f.use(if (reverse) 1 else 0, if (reverse) 0 else 1); f.finish()
            assertEquals(0, f.player.inv.count("obj.etched_araxyte_fang"))
            assertEquals(0, f.player.inv.count("obj.zenyte_amulet_enchanted"))
            assertEquals(1, f.player.inv.count("obj.amulet_of_rancour"))
            assertTrue(f.player.statMap.getXP("stat.crafting") > 0)
        }
    }

    @Test fun `declining rancour preserves both ingredients and awards no XP`() {
        val f = Fixture(86)
        f.put(0, "obj.etched_araxyte_fang"); f.put(1, "obj.zenyte_amulet_enchanted")
        val before = f.player.inv.objs.toList()
        f.use(0, 1); f.finish(accept = false)
        assertEquals(before, f.player.inv.objs.toList())
        assertEquals(0, f.player.statMap.getXP("stat.crafting"))
    }

    @Test fun `existing rupture assembly still creates exactly one necklace`() {
        val f = Fixture(84)
        f.put(0, "obj.etched_elder_venator_fang"); f.put(1, "obj.zenyte_necklace_enchanted")
        f.use(0, 1); f.finish()
        assertEquals(1, f.player.inv.count("obj.necklace_of_rupture"))
        assertEquals(0, f.player.inv.count("obj.etched_elder_venator_fang"))
        assertEquals(0, f.player.inv.count("obj.zenyte_necklace_enchanted"))
    }

    private class Fixture(level: Int) {
        val events = EventBus()
        val coroutine = GameCoroutine("fang-test")
        var result: Result<Unit>? = null
        val context = ProtectedAccessContextFactory.empty().copy(
            getEventBus = { events }, getAlignment = { TextAlignment() },
        )
        val player = Player(RecordingClient()).apply {
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
            statMap.setCurrentLevel("stat.crafting", level.toByte())
        }
        init {
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }
            with(HeldCraftingScript()) { scripts.startup() }
            with(FangCraftingScript()) { scripts.startup() }
        }
        fun put(slot: Int, item: String) { player.inv[slot] = InvObj(item, 1) }
        fun etch() = launch {
            val obj = player.inv[0]!!
            assertTrue(events.publish(this, HeldObjEvents.Op1(0, obj, ServerCacheManager.getItem(obj.id)!!, player.inv)))
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
    private data class Fang(val raw: String, val etched: String, val level: Int)
    companion object {
        private val fangs = listOf(
            Fang("obj.araxyte_fang", "obj.etched_araxyte_fang", 86),
            Fang("obj.elder_venator_fang", "obj.etched_elder_venator_fang", 84),
        )

        @JvmStatic @BeforeAll fun initCache() { ServerCacheManager.init(240).close() }
    }
}
