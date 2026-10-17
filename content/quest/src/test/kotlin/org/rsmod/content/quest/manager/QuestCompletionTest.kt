package org.rsmod.content.quest.manager

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.table.QuestRow
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class QuestCompletionTest {
    @Test
    fun `every completion entry point refuses a partial bundle and retries exactly once`() {
        for (native in listOf(false, true)) {
            for (entry in Entry.entries) {
                val f = Fixture(native = native)
                f.fillLeaving(1)
                val before = f.snapshot()

                assertFalse(entry.complete(f), "$entry native=$native")
                assertEquals(before, f.snapshot(), "$entry native=$native")
                assertFalse(f.player.ui.containsModal("interface.questscroll"))

                f.player.inv[25] = null
                f.player.inv[26] = null
                assertTrue(entry.complete(f))
                f.assertCompleted()
                GEMS.forEach { assertEquals(1, f.player.inv.count(it)) }
                assertEquals(0, f.player.inv.freeSpace())

                val completed = f.snapshot()
                assertTrue(f.quest.completeQuest(f.access))
                assertTrue(f.quest.setQuestStage(f.access, f.quest.maxSteps))
                assertEquals(completed, f.snapshot())
            }
        }
    }

    @Test
    fun `coin overflow rolls back the whole bundle even with ample empty slots`() {
        val f = Fixture(items = GEMS.map { it to 1 } + ("obj.coins" to 50))
        f.player.inv[0] = InvObj("obj.coins", Int.MAX_VALUE - 49)
        val before = f.snapshot()

        assertFalse(f.quest.completeQuest(f.access))
        assertEquals(before, f.snapshot())

        f.player.inv[0] = InvObj("obj.coins", Int.MAX_VALUE - 50)
        assertTrue(f.quest.completeQuest(f.access))
        f.assertCompleted()
        assertEquals(Int.MAX_VALUE, f.player.inv.count("obj.coins"))
        GEMS.forEach { assertEquals(1, f.player.inv.count(it)) }
        val completed = f.snapshot()
        assertTrue(f.quest.completeQuest(f.access))
        assertEquals(completed, f.snapshot())
    }

    @Test
    fun `an existing coin stack accepts the exact limit without an empty slot`() {
        val f = Fixture(items = listOf("obj.coins" to 50))
        f.fillLeaving(0)
        f.player.inv[13] = InvObj("obj.coins", Int.MAX_VALUE - 50)

        assertTrue(f.quest.completeQuest(f.access))
        f.assertCompleted()
        assertEquals(Int.MAX_VALUE, f.player.inv.count("obj.coins"))
        assertEquals(0, f.player.inv.freeSpace())
        assertEquals(27, f.player.inv.count("obj.black_bead"))
    }

    @Test
    fun `additional items and configured rewards share the same atomic grant`() {
        val extras = listOf(InvObj("obj.kittenobject", 1), InvObj("obj.chocolate_cake", 1))
        for (useSetter in listOf(false, true)) {
            for (freeSlots in listOf(1, 4)) {
                val f = Fixture()
                f.fillLeaving(freeSlots)
                val before = f.snapshot()
                fun complete(): Boolean =
                    if (useSetter) f.quest.setQuestStage(f.access, f.quest.maxSteps, extras)
                    else f.quest.completeQuest(f.access, extras)

                assertFalse(complete())
                assertEquals(before, f.snapshot())
                for (slot in 23 until 28) f.player.inv[slot] = null
                assertTrue(complete())
                f.assertCompleted()
                GEMS.forEach { assertEquals(1, f.player.inv.count(it)) }
                assertEquals(1, f.player.inv.count("obj.kittenobject"))
                assertEquals(1, f.player.inv.count("obj.chocolate_cake"))
                assertEquals(0, f.player.inv.freeSpace())

                val completed = f.snapshot()
                assertTrue(complete())
                assertEquals(completed, f.snapshot())
            }
        }
    }

    @Test
    fun `full inventories do not block non completing transitions`() {
        for (native in listOf(false, true)) {
            val f = Fixture(native = native, initialStage = 0)
            f.fillLeaving(0)
            val inventory = f.player.inv.objs.toList()

            assertTrue(f.quest.setQuestStage(f.access, 1))
            assertEquals(1, f.quest.getQuestStage(f.player))
            assertEquals(1, f.player.vars[f.quest.questVarbit ?: f.quest.questVarp])
            assertEquals(inventory, f.player.inv.objs.toList())
            assertEquals(0, f.player.vars["varp.qp"])
            assertEquals(0, f.player.statMap.getXP("stat.crafting"))
            assertFalse(f.player.ui.containsModal("interface.questscroll"))
        }
    }

    private enum class Entry {
        SetStage,
        Complete,
        Advance;

        fun complete(f: Fixture): Boolean = when (this) {
            SetStage -> f.quest.setQuestStage(f.access, f.quest.maxSteps)
            Complete -> f.quest.completeQuest(f.access)
            Advance -> f.quest.advanceQuestStage(f.access)
        }
    }

    private class Fixture(
        native: Boolean = true,
        items: List<Pair<String, Int>> = GEMS.map { it to 1 },
        initialStage: Int? = null,
    ) {
        private val client = RecordingClient()
        private val events = EventBus()
        private val row = QuestRow.getRow("dbrow.quest_misthalinmystery")

        val quest = Quest(
            id = row.id,
            key = "quest_misthalinmystery",
            rowID = "dbrow.quest_misthalinmystery".asRSCM(),
            displayName = row.displayname,
            mapElement = row.mapelement,
            startCoord = row.startcoord,
            maxSteps = row.endstate,
            questPoints = row.questpoints,
            questVarp = "varp.mistmyst_main",
            questVarbit = if (native) "varbit.mistmyst_progress" else null,
            rewards = QuestReward(xp = mapOf("stat.crafting" to 600.0), items = items),
            itemDisplay = ItemRewardDisplay("obj.uncut_sapphire"),
        )

        @OptIn(InternalApi::class)
        val player = Player().apply {
            this.client = this@Fixture.client
            uuid = 987L
            slotId = 1
            assignUid()
            currentMapClock = 100
            processedMapClock = 100
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
        }
        val access = ProtectedAccess(
            player,
            GameCoroutine("quest-completion-test"),
            ProtectedAccessContextFactory.empty().copy(getEventBus = { events }),
        )

        init {
            val stage = initialStage ?: (quest.maxSteps - 1)
            VarPlayerIntMapSetter.set(player, quest.questVarbit ?: quest.questVarp, stage)
            player.attr[QUEST_STAGE_MAP_ATTR] = mutableMapOf(
                quest.key to if (native) 1 else stage,
                "unrelated_quest" to 7,
            )
            client.messages.clear()
        }

        fun fillLeaving(freeSlots: Int) {
            for (slot in 0 until 28 - freeSlots) player.inv[slot] = InvObj("obj.black_bead", 1)
        }

        @OptIn(InternalApi::class)
        fun snapshot(): Snapshot = Snapshot(
            inventory = player.inv.objs.toList(),
            stages = player.attr[QUEST_STAGE_MAP_ATTR]?.toMap(),
            stage = quest.getQuestStage(player),
            varp = player.vars[quest.questVarp],
            varbit = player.vars["varbit.mistmyst_progress"],
            questPoints = player.vars["varp.qp"],
            completedCount = player.vars["varbit.quests_completed_count"],
            xp = player.statMap.getXP("stat.crafting"),
            questScroll = player.ui.containsModal("interface.questscroll"),
            packets = client.messages.toList(),
        )

        @OptIn(InternalApi::class)
        fun assertCompleted() {
            assertEquals(quest.maxSteps, quest.getQuestStage(player))
            assertEquals(quest.maxSteps, player.vars[quest.questVarbit ?: quest.questVarp])
            assertEquals(quest.questPoints, player.vars["varp.qp"])
            assertEquals(1, player.vars["varbit.quests_completed_count"])
            assertEquals(600, player.statMap.getXP("stat.crafting"))
            assertTrue(player.ui.containsModal("interface.questscroll"))
        }
    }

    private data class Snapshot(
        val inventory: List<InvObj?>,
        val stages: Map<String, Int>?,
        val stage: Int,
        val varp: Int,
        val varbit: Int,
        val questPoints: Int,
        val completedCount: Int,
        val xp: Int,
        val questScroll: Boolean,
        val packets: List<Any>,
    )

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
        private val GEMS = listOf("obj.uncut_ruby", "obj.uncut_emerald", "obj.uncut_sapphire")
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
