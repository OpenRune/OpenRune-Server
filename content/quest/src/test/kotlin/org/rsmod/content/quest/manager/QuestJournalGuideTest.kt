package org.rsmod.content.quest.manager

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.aconverted.interf.IfButtonOp
import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@OptIn(InternalApi::class)
class QuestJournalGuideTest {
    @Test
    fun `overview supplies real quest requirements and journal retains long progress text`() {
        val f = Fixture()
        f.open(JournalState.OVERVIEW)
        assertTrue(f.player.ui.containsModal("interface.quest_guide"))
        assertFalse(f.player.ui.containsModal("interface.questjournal_overview"))
        assertEquals(f.quest.rowID, f.draw().values[0])
        assertEquals(f.quest.displayName, f.draw().values[2])
        assertEquals("", f.draw().values[3])
        assertEquals(0, f.draw().values[5])
        f.click("journal")
        assertEquals(1, f.draw().values[5])
        val text = f.draw().values[3] as String
        assertTrue(text.contains("Progress line 199"))
        assertTrue(text.contains("<br>"))
        f.click("overview")
        assertEquals(0, f.draw().values[5])
    }

    @Test
    fun `completed journal uses completed content without changing quest state`() {
        val f = Fixture()
        f.player.attr[QUEST_STAGE_MAP_ATTR] = mutableMapOf(f.quest.key to f.quest.maxSteps)
        f.open(JournalState.LOG)
        assertEquals("Completed journal", f.draw().values[3])
        assertEquals(f.quest.maxSteps, f.quest.getQuestStage(f.player))
    }

    @Test
    fun `map action retains the quest start location`() {
        val f = Fixture()
        f.open(JournalState.OVERVIEW)
        f.click("map")
        assertTrue(f.player.ui.containsOverlay("interface.worldmap"))
        val script = f.client.messages.filterIsInstance<RunClientScript>().last { it.id == 3331 }
        assertEquals(f.quest.startCoord!!.packed, script.values[1])
        assertEquals(f.quest.mapElement, script.values[2])
    }

    private class Fixture {
        val events = EventBus()
        val client = RecordingClient()
        val player = Player(client)
        val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { events })
        val quest = Quest.register("quest_cooksassistant", "varp.cookquest", ItemRewardDisplay("obj.cake"), QuestReward())
        init {
            QuestJournalRegistry.register(quest, QuestJournalContent(
                { "talking to the cook in Lumbridge." },
                { (0..199).joinToString("\n") { "Progress line $it" } },
                { "Completed journal" },
            ))
            with(QuestJournalScript()) {
                ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup()
            }
        }
        fun access(block: suspend ProtectedAccess.() -> Unit) {
            player.launch { block(ProtectedAccess(player, this, context)) }
        }
        fun open(state: JournalState) { access { QuestJournalRegistry.openJournal(this, quest, state) } }
        fun click(name: String) {
            val component = ServerCacheManager.fromComponent("component.quest_guide:$name".asRSCM())
            access { events.publish(this, IfModalButton(component, -1, null, IfButtonOp.Op1)) }
        }
        fun draw() = client.messages.filterIsInstance<RunClientScript>().last { it.id == "clientscript.quest_guide_draw".asRSCM() }
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
