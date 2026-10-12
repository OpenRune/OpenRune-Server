package org.rsmod.content.areas.city.portsarim

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
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
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.areas.city.portsarim.npcs.RustyAnchorScript
import org.rsmod.content.areas.city.portsarim.npcs.SecurityGuard
import org.rsmod.content.areas.city.portsarim.travel.KaramjaFerryScript
import org.rsmod.content.quest.manager.QuestRequirementMode
import org.rsmod.content.quest.manager.QuestRequirementPolicy
import org.rsmod.content.quest.manager.QuestRequirements
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
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class PortSarimPurchasesInteractionTest {
    private lateinit var questPolicy: QuestRequirementPolicy

    @BeforeEach
    fun respectQuestProgress() {
        questPolicy = QuestRequirements.activePolicy()
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.RespectProgress))
    }

    @AfterEach
    fun restoreQuestPolicy() {
        QuestRequirements.install(questPolicy)
    }

    @Test fun `the Rusty Anchor bartender sells a beer for 2 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 5)
        f.talkTo(BARTENDER)
        f.finish(listOf(1))
        assertEquals(3, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(BEER))
    }

    @Test fun `the Rusty Anchor bartender keeps the beer from a player short of coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 1)
        f.talkTo(BARTENDER)
        f.finish(listOf(1))
        assertEquals(1, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(BEER))
    }

    @Test fun `a full pack drops the Rusty Anchor beer instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 3)
        f.talkTo(BARTENDER)
        f.finish(listOf(1))
        assertEquals(1, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(BEER))
        assertEquals(1, f.groundCount(BEER))
    }

    @Test fun `spending the last coins frees a slot for the Rusty Anchor beer`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 2)
        f.talkTo(BARTENDER)
        f.finish(listOf(1))
        assertEquals(0, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(BEER))
        assertEquals(0, f.groundCount(BEER))
    }

    @Test fun `the Security Guard hands out one security book`() {
        val f = Fixture()
        f.talkTo(SECURITY_GUARD)
        f.finish(listOf(2, 2))
        assertEquals(1, f.player.inv.count(SECURITY_BOOK))
    }

    @Test fun `the Security Guard gives nothing to a player who has the book`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(SECURITY_BOOK, 1)
        f.talkTo(SECURITY_GUARD)
        f.finish(listOf(2))
        assertEquals(1, f.player.inv.count(SECURITY_BOOK))
        assertEquals(0, f.groundCount(SECURITY_BOOK))
    }

    @Test fun `a full pack drops the security book instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.talkTo(SECURITY_GUARD)
        f.finish(listOf(2, 3))
        assertEquals(0, f.player.inv.count(SECURITY_BOOK))
        assertEquals(1, f.groundCount(SECURITY_BOOK))
    }

    @Test fun `the customs officer confiscates every Karamja rum and nothing else`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 50)
        for (slot in 1..3) f.player.inv[slot] = InvObj(RUM, 1)
        f.talkTo(CUSTOMS_OFFICER)
        f.finish(listOf(1, 2))
        assertEquals(0, f.player.inv.count(RUM))
        assertEquals(50, f.player.inv.count(COINS))
    }

    @Test fun `refusing the customs search keeps the rum`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(RUM, 1)
        f.talkTo(CUSTOMS_OFFICER)
        f.finish(listOf(1, 3))
        assertEquals(1, f.player.inv.count(RUM))
    }

    @Test fun `the customs officer takes no fare from a player who cannot pay`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 29)
        f.talkTo(CUSTOMS_OFFICER)
        f.finish(listOf(1, 2, 1))
        assertEquals(29, f.player.inv.count(COINS))
    }

    @Test fun `declining the customs boarding charge costs nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 50)
        f.talkTo(CUSTOMS_OFFICER)
        f.finish(listOf(1, 2, 2))
        assertEquals(50, f.player.inv.count(COINS))
    }

    @Test fun `the Karamja seamen take no fare from a player who cannot pay or declines`() {
        for ((coins, options) in listOf(29 to listOf(1), 50 to listOf(2))) {
            for (sailor in SEAMEN) {
                val f = Fixture()
                f.player.inv[0] = InvObj(COINS, coins)
                f.talkTo(sailor)
                f.finish(options)
                assertEquals(coins, f.player.inv.count(COINS), sailor)
            }
        }
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("port-sarim-purchases-test")
        private var result: Result<Unit>? = null
        private val clock = MapClock(100)
        private val objRegistry = ObjRegistry(ZoneUpdateMap())
        private val context =
            ProtectedAccessContextFactory.empty()
                .copy(
                    getEventBus = { events },
                    getAlignment = { TextAlignment() },
                    getNpcInteractions = { NpcInteractions(events) },
                )

        @OptIn(InternalApi::class)
        val player =
            Player().apply {
                this.client = this@Fixture.client
                uuid = 794L
                observerUUID = 794L
                slotId = 1
                assignUid()
                coords = CoordGrid(3213, 3428, 0)
                currentMapClock = 100
                processedMapClock = 100
                inv =
                    Inventory(
                        checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())),
                        arrayOfNulls(28),
                    )
                worn =
                    Inventory(
                        checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())),
                        arrayOfNulls(14),
                    )
            }

        init {
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            val objRepo = ObjRepository(clock, objRegistry)
            with(RustyAnchorScript(objRepo)) { scripts.startup() }
            with(SecurityGuard(objRepo)) { scripts.startup() }
            with(KaramjaFerryScript()) { scripts.startup() }
        }

        fun fillInventory() {
            for (slot in 0 until 28) player.inv[slot] = InvObj("obj.bronze_dagger", 1)
        }

        fun groundCount(obj: String): Int =
            objRegistry.findAll(player.coords).filter { it.type == obj.asRSCM() }.sumOf { it.count }

        fun talkTo(npc: String) {
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            val target = Npc(npc, player.coords.translateZ(1))
            val body: suspend () -> Unit = {
                val access = ProtectedAccess(player, coroutine, context)
                assertTrue(events.publish(access, NpcEvents.Op1(target)))
            }
            body.startCoroutine(
                object : Continuation<Unit> {
                    override val context = EmptyCoroutineContext

                    override fun resumeWith(result: Result<Unit>) {
                        this@Fixture.result = result
                    }
                }
            )
            result?.getOrThrow()
        }

        fun finish(options: List<Int>) {
            val selections = options.iterator()
            repeat(100) {
                if (coroutine.isIdle) return
                advance(selections)
            }
            fail<Unit>("Interaction did not finish: ${client.messages.joinToString("\n")}")
        }

        private fun advance(options: Iterator<Int>) {
            if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
                val input =
                    if (player.ui.containsModal("interface.chatmenu")) {
                        ResumePauseButtonInput(
                            "component.chatmenu:options",
                            if (options.hasNext()) options.next() else 1,
                        )
                    } else {
                        val component =
                            CONTINUE_COMPONENTS.entries
                                .firstOrNull { player.ui.containsModal(it.key) }
                                ?.value
                                ?: error("Unknown dialogue: ${client.messages.joinToString("\n")}")
                        ResumePauseButtonInput(component, -1)
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
        private const val COINS = "obj.coins"
        private const val BEER = "obj.beer"
        private const val RUM = "obj.karamja_rum"
        private const val SECURITY_BOOK = "obj.sos_security_book"
        private const val BARTENDER = "npc.rustyanchor_bartender"
        private const val SECURITY_GUARD = "npc.sos_guard"
        private const val CUSTOMS_OFFICER = "npc.customs_officer"
        private val SEAMEN =
            listOf("npc.seaman_lorris", "npc.seaman_thresnor", "npc.captain_tobias")
        private val CONTINUE_COMPONENTS =
            mapOf(
                "interface.chat_left" to "component.chat_left:continue",
                "interface.chat_right" to "component.chat_right:continue",
                "interface.messagebox" to "component.messagebox:continue",
                "interface.objectbox" to "component.objectbox:universe",
            )
        private val restored = mutableListOf<() -> Unit>()

        @OptIn(InternalApi::class)
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
            for ((owner, name) in
                listOf(
                    "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                    "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
                )) {
                val field =
                    Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                restored += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            restored += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic
        @AfterAll
        fun restore() {
            restored.asReversed().forEach { it() }
            restored.clear()
        }
    }
}
