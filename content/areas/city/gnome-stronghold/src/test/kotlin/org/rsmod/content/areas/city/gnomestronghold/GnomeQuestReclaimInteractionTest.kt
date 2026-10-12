package org.rsmod.content.areas.city.gnomestronghold

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
import org.rsmod.content.areas.city.gnomestronghold.npcs.Brimstail
import org.rsmod.content.areas.city.gnomestronghold.village.npcs.VillageTownsfolk
import org.rsmod.content.quest.area.lumbridge.RuneMysteriesQuest
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
class GnomeQuestReclaimInteractionTest {
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

    @Test
    fun `Brimstail returns one crystal saw seed after The Eyes of Glouphrie`() {
        completed(EYES_OF_GLOUPHRIE)
        val f = Fixture()
        f.talkTo(BRIMSTAIL)
        f.finish(emptyList())
        assertEquals(1, f.player.inv.count(CRYSTAL_SAW_SEED))
        f.talkTo(BRIMSTAIL)
        f.finish(emptyList())
        assertEquals(1, f.player.inv.count(CRYSTAL_SAW_SEED))
    }

    @Test
    fun `Brimstail gives no seed to a player who banked a crystal saw`() {
        completed(EYES_OF_GLOUPHRIE)
        val f = Fixture()
        f.bank[0] = InvObj(CRYSTAL_SAW, 1)
        f.talkTo(BRIMSTAIL)
        f.finish(emptyList())
        assertEquals(0, f.player.inv.count(CRYSTAL_SAW_SEED))
        assertEquals(1, f.bank.count(CRYSTAL_SAW))
    }

    @Test
    fun `a full pack drops Brimstail's seed instead of losing it`() {
        completed(EYES_OF_GLOUPHRIE)
        val f = Fixture()
        f.fillInventory()
        f.talkTo(BRIMSTAIL)
        f.finish(emptyList())
        assertEquals(0, f.player.inv.count(CRYSTAL_SAW_SEED))
        assertEquals(1, f.groundCount(CRYSTAL_SAW_SEED))
    }

    @Test
    fun `Brimstail gives no seed before The Eyes of Glouphrie`() {
        val f = Fixture()
        f.talkTo(BRIMSTAIL)
        f.finish(listOf(2))
        assertEquals(0, f.player.inv.count(CRYSTAL_SAW_SEED))
        assertEquals(0, f.groundCount(CRYSTAL_SAW_SEED))
    }

    @Test
    fun `King Bolren returns one gnome amulet after Tree Gnome Village`() {
        completed(TREE_GNOME_VILLAGE)
        val f = Fixture()
        f.talkTo(BOLREN)
        f.finish(emptyList())
        assertEquals(1, f.player.inv.count(GNOME_AMULET))
        f.talkTo(BOLREN)
        f.finish(emptyList())
        assertEquals(1, f.player.inv.count(GNOME_AMULET))
    }

    @Test
    fun `King Bolren gives no amulet to a player wearing one`() {
        completed(TREE_GNOME_VILLAGE)
        val f = Fixture()
        f.player.worn[2] = InvObj(GNOME_AMULET, 1)
        f.talkTo(BOLREN)
        f.finish(emptyList())
        assertEquals(0, f.player.inv.count(GNOME_AMULET))
        assertEquals(1, f.player.worn.count(GNOME_AMULET))
    }

    @Test
    fun `a full pack drops King Bolren's amulet instead of losing it`() {
        completed(TREE_GNOME_VILLAGE)
        val f = Fixture()
        f.fillInventory()
        f.talkTo(BOLREN)
        f.finish(emptyList())
        assertEquals(0, f.player.inv.count(GNOME_AMULET))
        assertEquals(1, f.groundCount(GNOME_AMULET))
    }

    @Test
    fun `King Bolren gives no amulet before Tree Gnome Village`() {
        val f = Fixture()
        f.talkTo(BOLREN)
        f.finish(listOf(2))
        assertEquals(0, f.player.inv.count(GNOME_AMULET))
        assertEquals(0, f.groundCount(GNOME_AMULET))
    }

    private fun completed(vararg quests: String) {
        QuestRequirements.install(
            QuestRequirementPolicy(
                QuestRequirementMode.VirtualCompletions,
                virtualCompletions = quests.toSet(),
            )
        )
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("gnome-quest-reclaim-test")
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
                coords = CoordGrid(2465, 3495, 1)
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
                invMap["inv.inv"] = inv
                invMap["inv.worn"] = worn
            }

        val bank: Inventory =
            checkNotNull(ServerCacheManager.getInventory("inv.bank".asRSCM())).let { type ->
                Inventory(type, arrayOfNulls(type.size)).also { player.invMap["inv.bank"] = it }
            }

        init {
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            val objRepo = ObjRepository(clock, objRegistry)
            with(Brimstail(RuneMysteriesQuest(), objRepo)) { scripts.startup() }
            with(VillageTownsfolk(objRepo)) { scripts.startup() }
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
        private val CONTINUE_COMPONENTS =
            mapOf(
                "interface.chat_left" to "component.chat_left:continue",
                "interface.chat_right" to "component.chat_right:continue",
                "interface.messagebox" to "component.messagebox:continue",
                "interface.objectbox" to "component.objectbox:universe",
            )
        private const val BRIMSTAIL = "npc.gnome_brimstail"
        private const val BOLREN = "npc.king_bolren"
        private const val CRYSTAL_SAW = "obj.eyeglo_crystal_saw"
        private const val CRYSTAL_SAW_SEED = "obj.crystal_seed_old_small"
        private const val GNOME_AMULET = "obj.gnome_amulet"
        private const val EYES_OF_GLOUPHRIE = "quest_eyesofglouphrie"
        private const val TREE_GNOME_VILLAGE = "quest_treegnomevillage"
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
