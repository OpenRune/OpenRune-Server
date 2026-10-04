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
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.content.skills.crafting.items.ZulrahCraftingScript
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
class ZulrahCraftingInteractionTest {
    @Test fun `visage and tanzanite fang craft via chisel in both directions`() {
        for ((raw, output) in listOf("obj.serpentine_visage" to "obj.serpentine_helm", "obj.blowpipe_fang" to "obj.toxic_blowpipe")) for (reverse in listOf(false, true)) {
            val f = Fixture(99); f.put(0, raw); f.put(1, "obj.chisel")
            for (slot in 2..27) f.put(slot, "obj.abyssal_whip")
            f.use(if (reverse) 1 else 0, if (reverse) 0 else 1); f.finish()
            assertEquals(1, f.player.inv.count(output)); assertEquals(0, f.player.inv.count(raw))
            assertEquals(1, f.player.inv.count("obj.chisel")); assertEquals(26, f.player.inv.count("obj.abyssal_whip"))
        }
    }

    @Test fun `magic fang combines with standard enhanced and ornamented tridents and staff`() {
        val bases = listOf("tots_uncharged", "tots_i_uncharged", "tots_uncharged_orn", "tots_i_uncharged_orn", "sotd")
        for (base in bases) for (reverse in listOf(false, true)) {
            val f = Fixture(99); f.put(0, "obj.magic_fang"); f.put(1, "obj.$base"); f.put(2, "obj.chisel")
            f.use(if (reverse) 1 else 0, if (reverse) 0 else 1); f.finish()
            assertEquals(1, f.player.inv.count("obj.toxic_$base")); assertEquals(0, f.player.inv.count("obj.magic_fang"))
            assertEquals(1, f.player.inv.count("obj.chisel"))
        }
    }

    @Test fun `insufficient skill and missing chisel preserve components`() {
        for (level in listOf(1, 99)) {
            val f = Fixture(level); f.put(0, "obj.magic_fang"); f.put(1, "obj.tots_uncharged")
            if (level == 1) f.put(2, "obj.chisel")
            val before = f.player.inv.objs.toList(); f.use(0, 1); f.finish()
            assertEquals(before, f.player.inv.objs.toList())
        }
        val f = Fixture(77); f.put(0, "obj.blowpipe_fang"); f.put(1, "obj.chisel")
        f.use(0, 1); f.finish(); assertEquals(1, f.player.inv.count("obj.blowpipe_fang"))
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
            with(HeldCraftingScript()) { scripts.startup() }
            with(ZulrahCraftingScript()) { scripts.startup() }
        }
        fun put(slot: Int, item: String) { player.inv[slot] = InvObj(item, 1) }
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
