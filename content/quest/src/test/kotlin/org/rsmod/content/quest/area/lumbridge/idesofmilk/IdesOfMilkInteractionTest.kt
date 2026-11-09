package org.rsmod.content.quest.area.lumbridge.idesofmilk

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.random.Random
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
import org.rsmod.api.player.hit.processor.DamageOnlyPlayerHitProcessor
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.DefaultGameRandom
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.area.lumbridge.idesofmilk.IdesOfMilkQuest.Companion.BOOK
import org.rsmod.content.quest.area.lumbridge.idesofmilk.IdesOfMilkQuest.Companion.CASSIUS
import org.rsmod.content.quest.area.lumbridge.idesofmilk.IdesOfMilkQuest.Companion.COWBELL_AMULET_EMPTY
import org.rsmod.content.quest.area.lumbridge.idesofmilk.IdesOfMilkQuest.Companion.GILLIE
import org.rsmod.content.quest.area.lumbridge.idesofmilk.IdesOfMilkQuest.Companion.LAMP
import org.rsmod.content.quest.area.lumbridge.idesofmilk.IdesOfMilkQuest.Companion.MILK_SAMPLE_1
import org.rsmod.content.quest.area.lumbridge.idesofmilk.IdesOfMilkQuest.Companion.MILK_SAMPLE_2
import org.rsmod.content.quest.area.lumbridge.idesofmilk.IdesOfMilkQuest.Companion.STAGE_BOOK_GIVEN
import org.rsmod.content.quest.area.lumbridge.idesofmilk.IdesOfMilkQuest.Companion.STAGE_COMPLETE
import org.rsmod.content.quest.area.lumbridge.idesofmilk.IdesOfMilkQuest.Companion.STAGE_FIRST_SAMPLE
import org.rsmod.content.quest.area.lumbridge.idesofmilk.IdesOfMilkQuest.Companion.STAGE_FIRST_SAMPLE_DRUNK
import org.rsmod.content.quest.area.lumbridge.idesofmilk.IdesOfMilkQuest.Companion.STAGE_RESEARCHED
import org.rsmod.content.quest.area.lumbridge.idesofmilk.IdesOfMilkQuest.Companion.STAGE_SECOND_SAMPLE
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class IdesOfMilkInteractionTest {
    @Test fun `the shelf respects capacity and does not duplicate or replace a surrendered book`() {
        val f = Fixture(STAGE_RESEARCHED, sethInformation = true)
        f.fill(28)
        f.run { with(f.script) { searchShelf() } }
        assertEquals(0, f.player.inv.count(BOOK))

        f.player.inv[0] = null
        repeat(2) { f.run { with(f.script) { searchShelf() } } }
        assertEquals(1, f.player.inv.count(BOOK))

        f.player.inv[0] = null
        VarPlayerIntMapSetter.set(f.player, "varbit.cowquest", STAGE_BOOK_GIVEN)
        f.run { with(f.script) { searchShelf() } }
        assertEquals(0, f.player.inv.count(BOOK))
    }

    @Test fun `Cassius takes every copy of the book for one sample`() {
        val f = Fixture(STAGE_RESEARCHED)
        f.player.inv[0] = InvObj(BOOK, 1)
        f.player.inv[4] = InvObj(BOOK, 1)
        f.talk(CASSIUS)
        assertEquals(0, f.player.inv.count(BOOK))
        assertEquals(1, f.player.inv.count(MILK_SAMPLE_1))
        assertEquals(STAGE_FIRST_SAMPLE, f.stage())
    }

    @Test fun `a book that vanishes mid-dialogue does not advance the quest`() {
        val f = Fixture(STAGE_RESEARCHED)
        f.player.inv[0] = InvObj(BOOK, 1)
        f.talk(CASSIUS) {
            f.until { f.client.messages.any { it.toString().contains("Hand it here, quickly!") } }
            f.player.inv[0] = null
        }
        assertEquals(STAGE_RESEARCHED, f.stage())
        assertEquals(0, f.player.inv.count(MILK_SAMPLE_1))
    }

    @Test fun `lost samples are replaced only when missing and there is room`() {
        for ((stage, sample) in listOf(
            STAGE_FIRST_SAMPLE to MILK_SAMPLE_1,
            STAGE_SECOND_SAMPLE to MILK_SAMPLE_2,
        )) {
            val f = Fixture(stage)
            f.fill(28)
            f.talk(CASSIUS)
            assertEquals(0, f.player.inv.count(sample))
            assertEquals(stage, f.stage())

            f.player.inv[0] = null
            repeat(2) { f.talk(CASSIUS) }
            assertEquals(1, f.player.inv.count(sample))
            assertEquals(stage, f.stage())
        }
    }

    @Test fun `drinking the first sample uses it up once`() {
        val f = Fixture(STAGE_FIRST_SAMPLE)
        f.player.inv[0] = InvObj(MILK_SAMPLE_1, 1)
        f.player.inv[1] = InvObj(MILK_SAMPLE_1, 1)
        repeat(2) { f.run { with(f.script) { drinkFirstSample() } } }
        assertEquals(1, f.player.inv.count(MILK_SAMPLE_1))
        assertEquals(STAGE_FIRST_SAMPLE_DRUNK, f.stage())
    }

    @Test fun `the second sample needs room before advancing the quest`() {
        val f = Fixture(STAGE_FIRST_SAMPLE_DRUNK)
        f.fill(28)
        f.talk(CASSIUS)
        assertEquals(0, f.player.inv.count(MILK_SAMPLE_2))
        assertEquals(STAGE_FIRST_SAMPLE_DRUNK, f.stage())
    }

    @Test fun `Gillie defers rewards until space is available without duplicating them`() {
        val f = Fixture(STAGE_COMPLETE)
        f.fill(28)
        f.talk(GILLIE)
        assertEquals(0, f.player.inv.count(COWBELL_AMULET_EMPTY))
        assertEquals(0, f.player.inv.count(LAMP))
        assertEquals(0, f.player.vars["varbit.cowquest_reward"])

        f.player.inv[0] = null
        f.talk(GILLIE)
        assertEquals(1, f.player.inv.count(COWBELL_AMULET_EMPTY))
        assertEquals(0, f.player.inv.count(LAMP))

        f.player.inv[1] = null
        repeat(2) { f.talk(GILLIE) }
        assertEquals(1, f.player.inv.count(LAMP))
        assertEquals(1, f.player.inv.count(COWBELL_AMULET_EMPTY))
    }

    @Test fun `the lamp is conserved when declined and consumed once for its reward`() {
        val f = Fixture(STAGE_COMPLETE)
        VarPlayerIntMapSetter.set(f.player, "varbit.cowquest_reward", 1)
        f.player.inv[0] = InvObj(LAMP, 1)
        f.run(listOf(ATTACK_OPTION, DECLINE_OPTION)) { with(f.script) { rubLamp() } }
        assertEquals(1, f.player.inv.count(LAMP))
        assertEquals(0, f.player.statMap.getXP("stat.attack"))

        f.run(listOf(ATTACK_OPTION, CONFIRM_OPTION)) { with(f.script) { rubLamp() } }
        assertEquals(0, f.player.inv.count(LAMP))
        assertEquals(1_000, f.player.statMap.getXP("stat.attack"))

        f.talk(GILLIE)
        assertEquals(0, f.player.inv.count(LAMP))
        assertEquals(1_000, f.player.statMap.getXP("stat.attack"))
    }

    private class Harness(private val quest: IdesOfMilkQuest) : PluginScript() {
        override fun ScriptContext.startup() {
            onOpNpc1(GILLIE) {
                startDialogue(it.npc) {
                    if (!with(quest) { gillieDialogue() }) mesbox("gillie")
                }
            }
        }
    }

    private class Fixture(stage: Int, sethInformation: Boolean = false) {
        val events = EventBus()
        val client = TestClient()
        private val coroutine = GameCoroutine("ides-of-milk-test")
        private var result: Result<Unit>? = null
        private val context = ProtectedAccessContextFactory.empty().copy(
            getEventBus = { events }, getAlignment = { TextAlignment() },
            getNpcInteractions = { NpcInteractions(events) },
            getRandom = { DefaultGameRandom(Random(0)) },
            getInstantHitProcessor = { DamageOnlyPlayerHitProcessor(events, NpcList(), PlayerList()) },
        )

        @OptIn(InternalApi::class)
        val player = Player().apply {
            this.client = this@Fixture.client
            uuid = 791L
            slotId = 1
            assignUid()
            coords = CoordGrid(3256, 3283, 0)
            currentMapClock = 100
            processedMapClock = 100
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
        }

        val script: IdesOfMilkQuest

        init {
            val npcs = NpcList()
            val npcRepo = NpcRepository(MapClock(), NpcRegistry(npcs, CollisionFlagMap(), events), npcs)
            script = IdesOfMilkQuest(npcRepo, DefaultGameRandom(Random(0)))
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            with(script) { scripts.startup() }
            with(Harness(script)) { scripts.startup() }
            VarPlayerIntMapSetter.set(player, "varbit.cowquest", stage)
            if (sethInformation) {
                VarPlayerIntMapSetter.set(player, "varbit.cowquest_seth_information", 1)
            }
        }

        fun stage() = script.stage(player)

        fun fill(slots: Int) {
            for (slot in 0 until slots) player.inv[slot] = InvObj("obj.coins", 1)
        }

        fun talk(npcType: String, beforeFinish: () -> Unit = {}) {
            start {
                val npc = Npc(npcType, player.coords.translateZ(1))
                events.publish(it, NpcEvents.Op1(npc))
            }
            beforeFinish()
            finish()
        }

        fun run(options: List<Int> = emptyList(), block: suspend ProtectedAccess.() -> Unit) {
            start { it.block() }
            finish(options)
        }

        private fun start(block: suspend (ProtectedAccess) -> Unit) {
            while (player.isDelayed) {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
            }
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            val access = ProtectedAccess(player, coroutine, context)
            val body: suspend () -> Unit = { block(access) }
            body.startCoroutine(object : Continuation<Unit> {
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
            fail<Unit>("Interaction did not reach the inventory handover")
        }

        private fun finish(options: List<Int> = emptyList()) {
            val selections = options.iterator()
            repeat(150) {
                if (coroutine.isIdle) return
                advance(selections)
            }
            fail<Unit>("Interaction did not finish")
        }

        private fun advance(options: Iterator<Int> = emptyList<Int>().iterator()) {
            if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
                val parent = listOf("chat_left", "chat_right", "messagebox", "objectbox", "chatmenu")
                    .firstOrNull { player.ui.containsModal("interface.$it") }
                    ?: error("Unknown dialogue")
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
    }

    private class TestClient : Client<Any, Any> {
        val messages = mutableListOf<Any>()
        override fun write(message: Any) { messages += message }
        override fun close() {}
        override fun read(player: Player) {}
        override fun flush() {}
        override fun flushHighPriority() {}
        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
        private const val ATTACK_OPTION = 1
        private const val CONFIRM_OPTION = 1
        private const val DECLINE_OPTION = 2
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
