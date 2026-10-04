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
import org.rsmod.content.skills.crafting.items.SpiritShieldCraftingScript
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
class SpiritShieldCraftingInteractionTest {
    @Test fun `all four recipes consume one of each ingredient in either order with full inventory`() {
        for ((shield, ingredient, output) in recipes) for (reverse in listOf(false, true)) {
            val f = Fixture(1)
            f.put(0, shield); f.put(1, ingredient)
            for (slot in 2..27) f.put(slot, "obj.abyssal_whip")
            f.use(if (reverse) 1 else 0, if (reverse) 0 else 1); f.finish()
            assertEquals(0, f.player.inv.count(shield))
            assertEquals(0, f.player.inv.count(ingredient))
            assertEquals(1, f.player.inv.count(output))
            assertEquals(26, f.player.inv.count("obj.abyssal_whip"))
            assertEquals(0, f.player.statMap.getXP("stat.crafting"))
        }
    }

    @Test fun `unblessed shield cannot consume a sigil`() {
        for (sigil in listOf("spectral", "arcane", "elysian")) {
            val f = Fixture(1)
            f.put(0, "obj.spirit_shield"); f.put(1, "obj.${sigil}_sigil")
            val before = f.player.inv.objs.toList()
            f.use(0, 1); f.finish()
            assertEquals(before, f.player.inv.objs.toList())
        }
    }

    @Test fun `missing ingredients cannot create a shield through a repeated recipe request`() {
        for ((shield, ingredient, output) in recipes) {
            val f = Fixture(1)
            f.put(0, shield); f.put(1, ingredient)
            f.use(0, 1); f.finish()
            val product = CraftingRecipes.forOutput(output).single()
            f.result = null
            f.launch { craftInstantly(product) }; f.finish()
            assertEquals(1, f.player.inv.count(output))
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
        }
        init {
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }
            with(HeldCraftingScript()) { scripts.startup() }
            with(SpiritShieldCraftingScript()) { scripts.startup() }
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
    companion object {
        private val recipes = listOf(
            Triple("obj.spirit_shield", "obj.holy_elixir", "obj.blessed_spirit_shield"),
            Triple("obj.blessed_spirit_shield", "obj.spectral_sigil", "obj.spectral"),
            Triple("obj.blessed_spirit_shield", "obj.arcane_sigil", "obj.arcane"),
            Triple("obj.blessed_spirit_shield", "obj.elysian_sigil", "obj.elysian"),
        )

        @JvmStatic @BeforeAll fun initCache() { ServerCacheManager.init(240).close() }
    }
}
