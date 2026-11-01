package org.rsmod.content.skills.hunter

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import org.junit.jupiter.api.Assertions.fail
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.random.DefaultGameRandom
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@OptIn(InternalApi::class)
class HunterFixture(coords: CoordGrid = CoordGrid(3222, 3222, 0)) {
    private val client = RecordingClient()
    val random = DefaultGameRandom(408L)
    val coroutine = GameCoroutine("hunter-test")
    private var result: Result<Any?>? = null

    val player =
        Player().apply {
            this.client = this@HunterFixture.client
            uuid = 408L
            observerUUID = 408L
            slotId = 1
            assignUid()
            this.coords = coords
            currentMapClock = 100
            processedMapClock = 100
            inv = inventory("inv.inv", 28)
            worn = inventory("inv.worn", 14)
            invMap["inv.bank"] = inventory("inv.bank", 800)
        }

    val access =
        ProtectedAccess(
            player,
            coroutine,
            ProtectedAccessContextFactory.empty().copy(getEventBus = { EventBus() }, getAlignment = { TextAlignment() }, getRandom = { random }),
        )

    val bank: Inventory
        get() = player.invMap.getValue("inv.bank")

    fun call(owner: Any, name: String, vararg args: Any?) {
        result = null
        player.activeCoroutine = coroutine
        val method = owner.javaClass.declaredMethods.single { it.name == name || it.name.startsWith("$name-") }
        method.isAccessible = true
        val full = if (method.isSuspend()) arrayOf(access, *args, continuation()) else arrayOf(access, *args)
        val returned = invoke { method.invoke(owner, *full) }
        if (returned !== COROUTINE_SUSPENDED) result = Result.success(returned)
        result?.getOrThrow()
    }

    fun finish(choice: Int = 1) {
        repeat(200) {
            result?.let {
                it.getOrThrow()
                return
            }
            if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
                val menu = player.ui.containsModal("interface.chatmenu")
                coroutine.resumeWith(
                    if (menu) ResumePauseButtonInput("component.chatmenu:options", choice)
                    else ResumePauseButtonInput(continueComponent(), -1)
                )
            } else {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
                coroutine.advance()
            }
        }
        fail<Unit>("Interaction did not finish: ${output()}")
    }

    fun run(owner: Any, name: String, vararg args: Any?) {
        call(owner, name, *args)
        finish()
    }

    fun fill(inv: Inventory = player.inv, obj: String = "obj.bronze_dagger") {
        for (slot in inv.indices) if (inv[slot] == null) inv[slot] = InvObj(obj, 1)
    }

    fun total(obj: String): Long =
        listOf(player.inv, player.worn, bank).sumOf { it.count(obj).toLong() } +
            player.invMap.values.filter { it !== player.inv && it !== player.worn && it !== bank }.sumOf { it.count(obj).toLong() }

    fun output(): String = client.messages.joinToString("\n")

    private fun continueComponent(): String =
        CONTINUE.entries.firstOrNull { player.ui.containsModal("interface.${it.key}") }?.value
            ?: "component.chat_left:continue"

    private fun continuation() =
        object : Continuation<Any?> {
            override val context = EmptyCoroutineContext

            override fun resumeWith(result: Result<Any?>) {
                this@HunterFixture.result = result
            }
        }

    private fun Method.isSuspend(): Boolean = parameterTypes.lastOrNull() == Continuation::class.java

    private fun invoke(block: () -> Any?): Any? =
        try {
            block()
        } catch (e: InvocationTargetException) {
            throw e.targetException
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
        private val CONTINUE =
            mapOf(
                "chat_left" to "component.chat_left:continue",
                "chat_right" to "component.chat_right:continue",
                "messagebox" to "component.messagebox:continue",
                "objectbox" to "component.objectbox:universe",
                "objectbox_double" to "component.objectbox_double:pausebutton",
                "levelup_display" to "component.levelup_display:continue",
            )

        fun inventory(name: String, size: Int) =
            Inventory(checkNotNull(ServerCacheManager.getInventory(name.asRSCM())), arrayOfNulls(size))

        private var ready = false

        fun cache() {
            if (ready) return
            ServerCacheManager.init(241).close()
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
            ready = true
        }
    }
}
