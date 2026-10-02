package org.rsmod.content.interfaces.monsterinfo

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.aconverted.interf.IfButtonOp
import dtx.rs.RSDropTable
import dtx.rs.rsGuaranteedTable
import net.rsprot.protocol.game.outgoing.interfaces.IfSetObject
import net.rsprot.protocol.game.outgoing.interfaces.IfSetText
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.annotations.InternalApi
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.DropTableRegistry
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.player.events.NpcExamineEvent
import org.rsmod.api.player.input.ResumePStringDialogInput
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@OptIn(InternalApi::class)
class MonsterInfoScriptTest {
    @Test
    fun `examine offers native chat choices and stats opens in the full browser`() {
        val f = Fixture()
        f.examine()
        assertTrue(f.player.ui.containsModal("interface.chatmenu"))
        f.choose(2)
        assertFalse(f.player.ui.containsModal("interface.chatmenu"))
        assertTrue(f.player.ui.containsModal("interface.monster_drops"))
        assertTrue(f.text("details").contains("Hitpoints:"))
        assertEquals("1/4", f.text("page"))
        f.click("next")
        assertTrue(f.text("details").contains("Attack speed:"))
    }

    @Test
    fun `drops can page forward backward and switch to stats without leaving a stale dialogue`() {
        val f = Fixture()
        f.examine()
        f.choose(1)
        assertTrue(f.player.ui.containsModal("interface.monster_drops"))
        assertEquals("1/2", f.text("page"))
        assertTrue(f.client.messages.any { it is IfSetObject })
        f.click("next")
        f.click("next")
        f.click("next")
        assertEquals("2/2", f.text("page"))
        f.click("previous")
        assertEquals("1/2", f.text("page"))
        f.click("stats")
        assertTrue(f.player.ui.containsModal("interface.monster_drops"))
        assertTrue(f.text("details").contains("Hitpoints:"))
    }

    @Test
    fun `item search resolves NPC sources and locations without awarding an item`() {
        val f = Fixture()
        f.examine(); f.choose(1)
        f.click("item")
        f.click("search")
        f.player.resumeActiveCoroutine(ResumePStringDialogInput("coins"))
        assertTrue(f.text("result_0").contains("Coins"))
        f.click("result_0")
        assertTrue(f.text("result_0").contains("Waterfiend"))
        assertEquals("1 NPC tables", f.text("list_status"))
        f.click("result_0")
        f.click("locations")
        assertTrue(f.text("details").contains("3200, 3201 (plane 0)"))
        assertTrue(f.player.inv.isEmpty())
    }

    @Test
    fun `empty search has no stale clickable rows and pagination is bounded`() {
        val f = Fixture()
        f.examine(); f.choose(1); f.click("search")
        f.player.resumeActiveCoroutine(ResumePStringDialogInput("no such monster 987654"))
        f.click("list_next"); f.click("result_7")
        assertEquals("No results", f.text("list_status"))
        assertEquals("1/1", f.text("list_page"))
        assertEquals("<col=ff981f></col>", f.text("result_7"))
    }

    @Test
    fun `cancel closes the dialogue and opens no panel`() {
        val f = Fixture()
        f.examine()
        f.choose(3)
        assertFalse(f.player.ui.containsModal("interface.chatmenu"))
        assertFalse(f.player.ui.containsModal("interface.monster_drops"))
        assertFalse(f.player.ui.containsModal("interface.dream_monster_stat"))
    }

    private class Fixture {
        val events = EventBus()
        val client = RecordingClient()
        val player = Player(client).apply { username = "monster-info-test"; inv = Inventory.create("inv.inv") }
        val type = ServerCacheManager.getNpcs().values.first { it.name == "Waterfiend" && MonsterInfoScript.isMonster(it) }
        val launcher: ProtectedAccessLauncher
        init {
            val factory = mock(ProtectedAccessContextFactory::class.java)
            `when`(factory.create()).thenReturn(ProtectedAccessContextFactory.empty().copy(getEventBus = { events }))
            launcher = ProtectedAccessLauncher(factory)
            val registry = mock(DropTableRegistry::class.java)
            val areas = mock(AreaChecker::class.java)
            val table = RSDropTable<Player, DropRollItem>("test", guaranteed = rsGuaranteedTable {
                repeat(8) { add(DropRollItem("obj.coins", it + 1)) }
            })
            `when`(registry.forNpcType(type.internalName, player.coords, areas)).thenReturn(table)
            `when`(registry.npcTables()).thenReturn(mapOf(type.internalName to listOf(table)))
            val npcs = NpcList().apply { this[1] = Npc(type, CoordGrid(3200, 3201)) }
            val catalogue = MonsterCatalogue(registry, npcs, BossInstanceRegistry())
            with(MonsterInfoScript(launcher, registry, areas, catalogue)) {
                ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup()
            }
        }
        fun examine() { events.publish(NpcExamineEvent(player, type)) }
        fun choose(choice: Int) { player.resumeActiveCoroutine(ResumePauseButtonInput("component.chatmenu:options", choice)) }
        fun click(name: String) {
            val component = ServerCacheManager.fromComponent("component.monster_drops:$name".asRSCM())
            launcher.launchLenient(player) { events.publish(this, IfModalButton(component, -1, null, IfButtonOp.Op1)) }
        }
        fun text(name: String) = client.messages.filterIsInstance<IfSetText>()
            .last { it.combinedId == "component.monster_drops:$name".asRSCM() }.text
    }

    private class RecordingClient : Client<Any, Any> {
        val messages = mutableListOf<Any>()
        override fun write(message: Any) { messages += message }
        override fun close() = Unit
        override fun read(player: Player) = Unit
        override fun flush() = Unit
        override fun flushHighPriority() = Unit
        override fun unregister(service: Any, player: Player) = Unit
    }

    companion object {
        @JvmStatic @BeforeAll fun loadCache() { ServerCacheManager.init(240).close() }
    }
}
