package org.rsmod.content.quest.area.falador.knightssword

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
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
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.area.falador.knightssword.KnightsSwordQuest.Companion.BLURITE_ORE
import org.rsmod.content.quest.area.falador.knightssword.KnightsSwordQuest.Companion.BLURITE_SWORD
import org.rsmod.content.quest.area.falador.knightssword.KnightsSwordQuest.Companion.IRON_BAR
import org.rsmod.content.quest.area.falador.knightssword.KnightsSwordQuest.Companion.PORTRAIT
import org.rsmod.content.quest.area.falador.knightssword.KnightsSwordQuest.Companion.REDBERRY_PIE
import org.rsmod.content.quest.area.falador.knightssword.KnightsSwordQuest.Companion.STAGE_COMPLETE
import org.rsmod.content.quest.area.falador.knightssword.KnightsSwordQuest.Companion.STAGE_GAVE_PORTRAIT
import org.rsmod.content.quest.area.falador.knightssword.KnightsSwordQuest.Companion.STAGE_LOOKING_FOR_PORTRAIT
import org.rsmod.content.quest.area.falador.knightssword.KnightsSwordQuest.Companion.STAGE_STARTED
import org.rsmod.content.quest.area.falador.knightssword.KnightsSwordQuest.Companion.STAGE_THURGO_NEEDS_PICTURE
import org.rsmod.content.quest.area.falador.knightssword.KnightsSwordQuest.Companion.STAGE_TOLD_BY_RELDO
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
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class KnightsSwordInteractionTest {
    @Test fun `giving Thurgo the pie takes exactly one pie and reaches the picture request`() {
        val f = Fixture(STAGE_TOLD_BY_RELDO)
        f.player.inv[0] = InvObj(REDBERRY_PIE, 1)
        f.player.inv[1] = InvObj(REDBERRY_PIE, 1)
        f.talk(THURGO)
        f.finish(listOf(PIE_OPTION, 1))
        assertEquals(1, f.player.inv.count(REDBERRY_PIE))
        assertEquals(STAGE_THURGO_NEEDS_PICTURE, f.stage())
    }

    @Test fun `a pie that vanishes mid-dialogue does not advance the quest`() {
        val f = Fixture(STAGE_TOLD_BY_RELDO)
        f.player.inv[0] = InvObj(REDBERRY_PIE, 1)
        f.talk(THURGO)
        f.until { f.output().contains("eyes light up") }
        f.player.inv[0] = null
        f.finish(listOf(PIE_OPTION))
        assertEquals(STAGE_TOLD_BY_RELDO, f.stage())
    }

    @Test fun `handing over the portrait takes every copy`() {
        val f = Fixture(STAGE_LOOKING_FOR_PORTRAIT)
        f.player.inv[0] = InvObj(PORTRAIT, 1)
        f.player.inv[5] = InvObj(PORTRAIT, 1)
        f.talk(THURGO)
        f.finish(listOf(1))
        assertEquals(0, f.player.inv.count(PORTRAIT))
        assertEquals(STAGE_GAVE_PORTRAIT, f.stage())
    }

    @Test fun `no portrait leaves the stage alone`() {
        val f = Fixture(STAGE_LOOKING_FOR_PORTRAIT)
        f.talk(THURGO)
        f.finish(listOf(1))
        assertEquals(STAGE_LOOKING_FOR_PORTRAIT, f.stage())
    }

    @Test fun `Thurgo swaps one ore and two bars for one sword`() {
        val f = Fixture(STAGE_GAVE_PORTRAIT)
        f.materials(ores = 2, bars = 3)
        f.talk(THURGO)
        f.finish(listOf(1))
        assertEquals(1, f.player.inv.count(BLURITE_ORE))
        assertEquals(1, f.player.inv.count(IRON_BAR))
        assertEquals(1, f.player.inv.count(BLURITE_SWORD))
        assertEquals(STAGE_GAVE_PORTRAIT, f.stage())
    }

    @Test fun `missing materials take nothing`() {
        for ((ores, bars) in listOf(0 to 0, 1 to 1, 0 to 2)) {
            val f = Fixture(STAGE_GAVE_PORTRAIT)
            f.materials(ores, bars)
            f.talk(THURGO)
            f.finish(listOf(1))
            assertEquals(ores, f.player.inv.count(BLURITE_ORE))
            assertEquals(bars, f.player.inv.count(IRON_BAR))
            assertEquals(0, f.player.inv.count(BLURITE_SWORD))
        }
    }

    @Test fun `a full inventory still has room for the sword`() {
        val f = Fixture(STAGE_GAVE_PORTRAIT)
        f.materials(ores = 1, bars = 2)
        for (slot in 3 until 28) f.player.inv[slot] = InvObj("obj.coins", 1)
        f.talk(THURGO)
        f.finish(listOf(1))
        assertEquals(1, f.player.inv.count(BLURITE_SWORD))
        assertEquals(0, f.player.inv.count(BLURITE_ORE))
        assertEquals(0, f.player.inv.count(IRON_BAR))
    }

    @Test fun `a bar that vanishes before the swap leaves the rest untouched`() {
        val f = Fixture(STAGE_GAVE_PORTRAIT)
        f.materials(ores = 1, bars = 2)
        f.talk(THURGO)
        f.until { f.output().contains("I have them right here.") }
        f.player.inv[1] = null
        f.finish(listOf(1))
        assertEquals(1, f.player.inv.count(BLURITE_ORE))
        assertEquals(1, f.player.inv.count(IRON_BAR))
        assertEquals(0, f.player.inv.count(BLURITE_SWORD))
    }

    @Test fun `handing in the sword completes the quest with the wiki rewards`() {
        val f = Fixture(STAGE_GAVE_PORTRAIT)
        f.player.inv[0] = InvObj(BLURITE_SWORD, 1)
        f.talk(SQUIRE)
        f.finish()
        assertEquals(STAGE_COMPLETE, f.stage())
        assertEquals(STAGE_COMPLETE, f.player.vars["varp.squire"])
        assertEquals(0, f.player.inv.count(BLURITE_SWORD))
        assertEquals(1, f.player.vars["varp.qp"])
        assertEquals(12_725, f.player.statMap.getXP("stat.smithing"))
        assertTrue(f.player.ui.containsModal("interface.questscroll"))
    }

    @Test fun `a worn sword is not taken`() {
        val f = Fixture(STAGE_GAVE_PORTRAIT)
        f.player.worn[3] = InvObj(BLURITE_SWORD, 1)
        f.talk(SQUIRE)
        f.finish()
        assertEquals(STAGE_GAVE_PORTRAIT, f.stage())
        assertEquals(1, f.player.worn.count(BLURITE_SWORD))
        assertTrue(f.output().contains("un-equip it"))
    }

    @Test fun `a sword that vanishes mid hand-in does not complete the quest`() {
        val f = Fixture(STAGE_GAVE_PORTRAIT)
        f.player.inv[0] = InvObj(BLURITE_SWORD, 1)
        f.talk(SQUIRE)
        f.until { f.output().contains("Thank you, thank you, thank you!") }
        f.player.inv[0] = null
        f.finish()
        assertEquals(STAGE_GAVE_PORTRAIT, f.stage())
        assertEquals(0, f.player.vars["varp.qp"])
    }

    @Test fun `Reldo only advances the quest from its first stage`() {
        for ((from, to) in listOf(0 to 0, STAGE_STARTED to STAGE_TOLD_BY_RELDO, 4 to 4)) {
            val f = Fixture(from)
            with(f.script) { f.access().learnAboutImcando() }
            assertEquals(to, f.stage())
        }
    }

    private class Harness(private val quest: KnightsSwordQuest) : PluginScript() {
        override fun ScriptContext.startup() {
            onOpNpc1(SQUIRE) { startDialogue(it.npc) { with(quest) { squireDialogue() } } }
            onOpNpc1(THURGO) {
                startDialogue(it.npc) {
                    with(quest) { thurgoDialogue(pie = { mesbox("pie") }, cape = { mesbox("cape") }) }
                }
            }
        }
    }

    private class Fixture(stage: Int) {
        val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("knights-sword-test")
        private var result: Result<Unit>? = null
        private val context = ProtectedAccessContextFactory.empty().copy(
            getEventBus = { events }, getAlignment = { TextAlignment() },
            getNpcInteractions = { NpcInteractions(events) },
        )

        @OptIn(InternalApi::class)
        val player = Player().apply {
            this.client = this@Fixture.client
            uuid = 790L
            slotId = 1
            assignUid()
            coords = CoordGrid(2999, 3144, 0)
            currentMapClock = 100
            processedMapClock = 100
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
        }

        val script = KnightsSwordQuest()

        init {
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            with(script) { scripts.startup() }
            with(Harness(script)) { scripts.startup() }
            VarPlayerIntMapSetter.set(player, "varbit.knights_sword_progress", stage)
        }

        fun stage() = script.stage(player)

        fun access() = ProtectedAccess(player, coroutine, context)

        fun materials(ores: Int, bars: Int) {
            var slot = 0
            repeat(ores) { player.inv[slot++] = InvObj(BLURITE_ORE, 1) }
            repeat(bars) { player.inv[slot++] = InvObj(IRON_BAR, 1) }
        }

        fun talk(npcType: String) {
            while (player.isDelayed) {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
            }
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            val npc = Npc(npcType, player.coords.translateZ(1))
            val block: suspend () -> Unit = { assertTrue(events.publish(access(), NpcEvents.Op1(npc))) }
            block.startCoroutine(object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) { this@Fixture.result = result }
            })
            result?.getOrThrow()
        }

        fun until(predicate: () -> Boolean) {
            repeat(150) {
                if (predicate()) return
                advance()
            }
            fail<Unit>("Condition was not reached: ${output()}")
        }

        fun finish(options: List<Int> = emptyList()) {
            val selections = options.iterator()
            repeat(150) {
                if (coroutine.isIdle) return
                advance(selections)
            }
            fail<Unit>("Interaction did not finish: ${output()}")
        }

        private fun advance(options: Iterator<Int> = emptyList<Int>().iterator()) {
            if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
                val parent = listOf("chat_left", "chat_right", "messagebox", "objectbox", "chatmenu")
                    .firstOrNull { player.ui.containsModal("interface.$it") }
                    ?: error("Unknown dialogue: ${output()}")
                val input = when (parent) {
                    "chatmenu" ->
                        ResumePauseButtonInput("component.chatmenu:options", if (options.hasNext()) options.next() else 1)
                    "objectbox" -> ResumePauseButtonInput("component.objectbox:universe", 0)
                    else -> ResumePauseButtonInput("component.$parent:continue", -1)
                }
                coroutine.resumeWith(input)
            } else {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
                coroutine.advance()
            }
            result?.getOrThrow()
        }

        fun output() = client.messages.joinToString("\n")
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
        private const val SQUIRE = "npc.squire"
        private const val THURGO = "npc.thurgo"
        private const val PIE_OPTION = 2
        private val restored = mutableListOf<() -> Unit>()

        @OptIn(InternalApi::class)
        @JvmStatic @BeforeAll fun cache() {
            ServerCacheManager.init(240).close()
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

        @JvmStatic @AfterAll fun restore() {
            restored.asReversed().forEach { it() }
            restored.clear()
        }
    }
}
