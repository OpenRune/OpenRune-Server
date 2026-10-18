package org.rsmod.content.quest.area

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
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
import org.rsmod.api.random.GameRandom
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.obj.ObjRepository
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
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Runs quest NPC conversations headless: talk to an NPC, pick chatmenu options by number. */
class QuestTalkFixture(coords: CoordGrid, script: (ObjRepository) -> PluginScript) {
    val events = EventBus()
    private val client = RecordingClient()
    private val coroutine = GameCoroutine("quest-talk-test")
    private var result: Result<Unit>? = null
    private val context = ProtectedAccessContextFactory.empty().copy(
        getEventBus = { events }, getAlignment = { TextAlignment() },
        getNpcInteractions = { NpcInteractions(events) },
        getRandom = { fixedRandom },
    )

    /** What every random roll returns (clamped into its range). */
    var randomValue: Int = 0

    private val fixedRandom = object : GameRandom {
        override fun of(maxExclusive: Int): Int = randomValue.coerceIn(0, maxExclusive - 1)

        override fun of(minInclusive: Int, maxInclusive: Int): Int =
            randomValue.coerceIn(minInclusive, maxInclusive)

        override fun randomDouble(): Double = 0.0
    }
    val objRepo = ObjRepository(MapClock(100), ObjRegistry(ZoneUpdateMap()))

    @OptIn(InternalApi::class)
    val player = Player().apply {
        this.client = this@QuestTalkFixture.client
        uuid = 791L
        slotId = 1
        assignUid()
        this.coords = coords
        currentMapClock = 100
        processedMapClock = 100
        inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
        worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
    }

    val script: PluginScript = script(objRepo)

    init {
        with(this.script) { ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup() }
    }

    private fun access() = ProtectedAccess(player, coroutine, context)

    fun give(obj: String, count: Int = 1) {
        val slot = (0 until player.inv.size).first { player.inv[it] == null }
        player.inv[slot] = InvObj(obj, count)
    }

    fun count(obj: String): Int = player.inv.count(obj)

    /** Talks to [npc] and walks the whole conversation, answering chatmenus with [options]. */
    fun talk(npc: String, vararg options: Int) {
        val target = Npc(npc, player.coords.translateZ(1))
        interact(*options) { assertTrue(events.publish(it, NpcEvents.Op1(target))) }
    }

    /** Runs [publish] (which should publish one interaction event) and walks the conversation. */
    fun interact(vararg options: Int, publish: suspend (ProtectedAccess) -> Unit) {
        while (player.isDelayed) {
            player.currentMapClock++
            player.processedMapClock = player.currentMapClock
        }
        player.clearPendingAction(events)
        client.messages.clear()
        result = null
        player.activeCoroutine = coroutine
        val block: suspend () -> Unit = { publish(access()) }
        block.startCoroutine(object : Continuation<Unit> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Unit>) { this@QuestTalkFixture.result = result }
        })
        result?.getOrThrow()
        val selections = options.iterator()
        repeat(400) {
            if (coroutine.isIdle) {
                if (selections.hasNext()) fail<Unit>("Unused options after: ${output()}")
                return
            }
            advance(selections)
        }
        fail<Unit>("Interaction did not finish: ${output()}")
    }

    private fun advance(options: Iterator<Int>) {
        if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
            val parent = listOf("chat_left", "chat_right", "messagebox", "objectbox", "chatmenu")
                .firstOrNull { player.ui.containsModal("interface.$it") }
                ?: error("Unknown dialogue: ${output()}")
            val input = if (parent == "chatmenu") {
                if (!options.hasNext()) fail<Unit>("No option left for a chatmenu: ${output()}")
                ResumePauseButtonInput("component.chatmenu:options", options.next())
            } else if (parent == "objectbox") {
                ResumePauseButtonInput("component.objectbox:universe", 0)
            } else ResumePauseButtonInput("component.$parent:continue", -1)
            coroutine.resumeWith(input)
        } else {
            player.currentMapClock++
            player.processedMapClock = player.currentMapClock
            coroutine.advance()
        }
        result?.getOrThrow()
    }

    fun output() = client.messages.joinToString("\n")

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
        private val restored = mutableListOf<() -> Unit>()

        @OptIn(InternalApi::class)
        fun setUpCache() {
            ServerCacheManager.init(241).close()
            for ((owner, name) in listOf(
                "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
            )) {
                val field = Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                restored += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            restored += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        fun restore() {
            restored.asReversed().forEach { it() }
            restored.clear()
        }
    }
}
