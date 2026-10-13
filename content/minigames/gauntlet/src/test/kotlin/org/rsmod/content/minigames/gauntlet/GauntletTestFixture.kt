package org.rsmod.content.minigames.gauntlet

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class GauntletTestFixture {
    val events = EventBus()
    val player = Player(RecordingClient()).apply {
        observerUUID = 1L
        inv = Inventory.create("inv.inv")
        worn = Inventory.create("inv.worn")
        for (stat in ServerCacheManager.getStats().values) {
            val key = RSCM.getReverseMapping(RSCMType.STAT, stat.id)
            statMap.setBaseLevel(key, 99)
            statMap.setCurrentLevel(key, 99)
        }
    }
    val coroutine = GameCoroutine("gauntlet-native-interaction")
    var result: Result<Unit>? = null
    val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { events }, getAlignment = { TextAlignment() })
    val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
    init { register(InvTransactionsScript(PlayerItemStorage(emptySet()))) }
    fun register(script: PluginScript) { with(script) { scripts.startup() } }
    fun put(slot: Int, obj: String, count: Int = 1, vars: Int = 0) { player.inv[slot] = InvObj(obj, count, vars) }
    fun use(first: Int, second: Int) = launch {
        HeldUInteractions(events).interact(this, player.inv,
            ServerCacheManager.getItem(player.inv[first]!!.id)!!, first,
            ServerCacheManager.getItem(player.inv[second]!!.id)!!, second)
    }
    fun launch(action: suspend ProtectedAccess.() -> Unit) {
        result = null
        player.activeCoroutine = coroutine
        val block: suspend () -> Unit = { action(ProtectedAccess(player, coroutine, context)) }
        block.startCoroutine(object : Continuation<Unit> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Unit>) { this@GauntletTestFixture.result = result }
        })
        result?.getOrThrow()
    }
    fun advance() {
        player.currentMapClock++
        player.processedMapClock = player.currentMapClock
        coroutine.advance()
        result?.getOrThrow()
    }
    private class RecordingClient : Client<Any, Any> {
        override fun write(message: Any) = Unit
        override fun close() = Unit
        override fun read(player: Player) = Unit
        override fun flush() = Unit
        override fun flushHighPriority() = Unit
        override fun unregister(service: Any, player: Player) = Unit
    }
}
