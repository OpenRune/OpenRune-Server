package org.rsmod.content.interfaces.skill.guides

import dev.openrune.ServerCacheManager
import dev.openrune.types.aconverted.interf.IfButtonOp
import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.annotations.InternalApi
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.IfOverlayButton
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.table.StatComponentsRow
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@OptIn(InternalApi::class)
class SkillGuideEventsTest {
    @Test
    fun `every skill opens the steel guide with all four native script arguments`() {
        val rows = StatComponentsRow.all()
        assertTrue(rows.size >= 23)
        rows.forEach { row ->
            val f = Fixture()
            VarPlayerIntMapSetter.set(f.player, "varbit.option_skill_guide", 0)
            val component = row.component
            f.launcher.launchLenient(f.player) {
                f.events.publish(this, IfOverlayButton(component, -1, null, IfButtonOp.Op1))
            }
            assertTrue(f.player.ui.containsOverlay("interface.skill_guide_v2"), row.component.toString())
            assertFalse(f.player.ui.containsOverlay("interface.skill_guide"), row.component.toString())
            val script = f.client.messages.filterIsInstance<RunClientScript>().last { it.id == 1902 }
            assertEquals(listOf(row.bit, 0, 0, 0), script.values, row.component.toString())
            assertEquals(1, f.player.vars["varbit.option_skill_guide"])
        }
    }

    @Test
    fun `login upgrades a saved parchment guide preference`() {
        val f = Fixture()
        VarPlayerIntMapSetter.set(f.player, "varbit.option_skill_guide", 0)
        f.events.publish(SessionStateEvent.Login(f.player))
        assertEquals(1, f.player.vars["varbit.option_skill_guide"])
    }

    private class Fixture {
        val events = EventBus()
        val client = RecordingClient()
        val player = Player(client)
        val launcher: ProtectedAccessLauncher
        init {
            val factory = mock(ProtectedAccessContextFactory::class.java)
            `when`(factory.create()).thenReturn(ProtectedAccessContextFactory.empty().copy(getEventBus = { events }))
            launcher = ProtectedAccessLauncher(factory)
            with(SkillGuideEvents(events, launcher)) {
                ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup()
            }
        }
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
