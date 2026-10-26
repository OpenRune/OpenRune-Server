package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
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
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.server.config.GameplayConfig
import org.rsmod.api.server.config.QuestRequirementsYaml
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.table.QuestRow
import org.rsmod.content.quest.manager.QuestRequirementResolver
import org.rsmod.content.quest.manager.QuestRequirements
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
@ResourceLock("QuestRequirements")
class FarmingWhiteTreeTest {
    private val previousPolicy = QuestRequirements.activePolicy()
    private val harvests = mutableListOf<FarmingWhiteTreeHarvestEvent>()
    private val events = EventBus().apply {
        subscribeUnbound(FarmingWhiteTreeHarvestEvent::class.java) { harvests += this }
    }

    @AfterEach
    fun restorePolicy() = QuestRequirements.install(previousPolicy)

    @Test
    fun `full inventory spends no fruit or XP and repeated picks cannot exceed the tree`() {
        val tree = tree()
        val player = player()
        complete(player)
        tree.refresh(player, NOW)
        events.subscribeUnbound(FarmingWhiteTreeHarvestEvent::class.java) {
            assertEquals(player, this.player)
            assertEquals(12 - harvests.size, player.vars[FarmingWhiteTree.TREE])
            assertEquals(harvests.size, player.inv.count(FarmingWhiteTree.FRUIT))
            assertEquals(120 * harvests.size, player.statMap.getFineXP(STAT_FARMING))
        }
        for (slot in 0 until 28) player.inv[slot] = InvObj("obj.spade")
        assertFalse(tree.pick(player, NOW))
        assertEquals(12, player.vars[FarmingWhiteTree.TREE])
        assertEquals(0, player.statMap.getFineXP(STAT_FARMING))
        assertEquals(0, player.inv.count(FarmingWhiteTree.FRUIT))
        assertTrue(harvests.isEmpty())
        player.inv[0] = null
        assertTrue(tree.pick(player, NOW))
        assertEquals(11, player.vars[FarmingWhiteTree.TREE])
        assertEquals(1, player.inv.count(FarmingWhiteTree.FRUIT))
        assertEquals(120, player.statMap.getFineXP(STAT_FARMING))
        assertEquals(listOf(FarmingWhiteTreeHarvestEvent(player)), harvests)
        for (slot in 1..3) player.inv[slot] = null
        repeat(3) { assertTrue(tree.pick(player, NOW)) }
        assertFalse(tree.pick(player, NOW))
        assertEquals(8, player.vars[FarmingWhiteTree.TREE])
        assertEquals(4, player.inv.count(FarmingWhiteTree.FRUIT))
        assertEquals(480, player.statMap.getFineXP(STAT_FARMING))
        assertEquals(List(4) { FarmingWhiteTreeHarvestEvent(player) }, harvests)
    }

    @Test
    fun `saved depleted tree regrows at boundaries without refilling on reconnect`() {
        val tree = tree()
        val player = player()
        complete(player)
        set(player, FarmingWhiteTree.TREE, 8)
        set(player, FarmingWhiteTree.CLOCK, FarmingClock.cycle(player, 5, NOW).toInt())
        val restored = player().apply { vars.backing.putAll(player.vars.backing) }
        tree.refresh(restored, NOW + 299_999)
        assertEquals(8, restored.vars[FarmingWhiteTree.TREE])
        tree.refresh(restored, NOW + 300_000)
        assertEquals(9, restored.vars[FarmingWhiteTree.TREE])
        tree.refresh(restored, NOW + 300_000)
        assertEquals(9, restored.vars[FarmingWhiteTree.TREE])
        tree.refresh(restored, NOW + 20 * 60_000)
        assertEquals(12, restored.vars[FarmingWhiteTree.TREE])
        assertEquals(0, restored.statMap.getFineXP(STAT_FARMING))
        assertTrue(harvests.isEmpty())
    }

    @Test
    fun `absent real progress fails closed while sourced completion unlocks fruit`() {
        val tree = tree()
        val player = player()
        tree.refresh(player, NOW)
        assertFalse(tree.pick(player, NOW))
        assertEquals(0, player.vars[FarmingWhiteTree.TREE])
        assertEquals(0, player.vars[FarmingWhiteTree.CLOCK])
        assertEquals(0, player.inv.count(FarmingWhiteTree.FRUIT))
        assertEquals(0, player.statMap.getFineXP(STAT_FARMING))
        assertTrue(harvests.isEmpty())
        complete(player)
        assertTrue(tree.pick(player, NOW))
        assertEquals(QuestRow.getRow("dbrow.${FarmingWhiteTree.QUEST}").endstate,
            player.vars[FarmingWhiteTree.QUEST_PROGRESS])
        assertEquals(11, player.vars[FarmingWhiteTree.TREE])
        assertEquals(1, player.inv.count(FarmingWhiteTree.FRUIT))
        assertEquals(listOf(FarmingWhiteTreeHarvestEvent(player)), harvests)
    }

    @Test
    fun `configured completion initializes zero once but preserves native in-progress garden`() {
        for (mode in listOf("assume-completed", "virtual-completions")) {
            val tree = tree(mode, setOf(FarmingWhiteTree.QUEST))
            val player = player()
            tree.refresh(player, NOW)
            assertEquals(12, player.vars[FarmingWhiteTree.TREE])
            assertEquals(0, player.vars[FarmingWhiteTree.QUEST_PROGRESS])
            repeat(4) { assertTrue(tree.pick(player, NOW)) }
            tree.refresh(player, NOW)
            assertEquals(8, player.vars[FarmingWhiteTree.TREE])
            val committed = harvests.toList()
            for (art in 1..7) {
                val growing = player()
                set(growing, FarmingWhiteTree.TREE, art)
                tree.refresh(growing, NOW)
                assertFalse(tree.pick(growing, NOW))
                assertEquals(art, growing.vars[FarmingWhiteTree.TREE])
                assertEquals(0, growing.vars[FarmingWhiteTree.CLOCK])
                assertEquals(0, growing.inv.count(FarmingWhiteTree.FRUIT))
                assertEquals(0, growing.statMap.getFineXP(STAT_FARMING))
                assertEquals(committed, harvests)
            }
        }
        val unlisted = tree("virtual-completions")
        val player = player()
        unlisted.refresh(player, NOW)
        assertEquals(0, player.vars[FarmingWhiteTree.TREE])
    }

    private fun tree(mode: String = "respect-progress", completed: Set<String> = emptySet()): FarmingWhiteTree {
        val config = ServerConfig("test", 0, 240, "test", 1,
            gameplay = GameplayConfig(QuestRequirementsYaml(mode, completed)))
        return FarmingWhiteTree(QuestRequirementResolver(config), XpModifiers(emptySet()), events)
    }

    private fun player(): Player = Player().apply {
        client = SilentClient()
        inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
        set(this, "varp.farming_growth_offset", 32)
    }

    private fun complete(player: Player) = set(player, FarmingWhiteTree.QUEST_PROGRESS,
        QuestRow.getRow("dbrow.${FarmingWhiteTree.QUEST}").endstate)

    private fun set(player: Player, key: String, value: Int) = VarPlayerIntMapSetter.set(player, key, value)

    private class SilentClient : Client<Any, Any> {
        override fun write(message: Any) {}
        override fun close() {}
        override fun read(player: Player) {}
        override fun flush() {}
        override fun flushHighPriority() {}
        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
        private const val NOW = 600_000_000L
        private val RESTORED = mutableListOf<() -> Unit>()

        @OptIn(InternalApi::class)
        @JvmStatic @BeforeAll fun cache() {
            val root = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                .first { Files.isRegularFile(it.resolve("game.yml")) }
            val revision = Files.readAllLines(root.resolve("game.yml"))
                .first { it.startsWith("revision:") }.substringAfter(':').trim().substringBefore('.').toInt()
            ServerCacheManager.init(revision).close()
            for ((owner, name) in listOf(
                "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
            )) {
                val field = Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                RESTORED += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            RESTORED += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic @AfterAll fun restore() {
            RESTORED.asReversed().forEach { it() }
            RESTORED.clear()
        }
    }
}
