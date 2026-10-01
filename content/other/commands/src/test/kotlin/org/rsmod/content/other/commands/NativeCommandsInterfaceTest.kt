package org.rsmod.content.other.commands

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfButtonOp
import dev.or2.central.account.Rights
import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import net.rsprot.protocol.game.outgoing.interfaces.IfSetText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.rsmod.annotations.InternalApi
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.script.onCommand
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@OptIn(InternalApi::class)
class NativeCommandsInterfaceTest {
    @Test
    fun `paging preserves the active search and can advance more than once`() {
        val fixture = Fixture()
        repeat(20) { fixture.command("alpha${it.toString().padStart(2, '0')}") }
        fixture.command("beta")
        fixture.open("alpha")

        fixture.click("next")
        fixture.click("next")

        assertEquals("20 results / page 3 of 3", fixture.text("page"))
        assertEquals("Search: alpha", fixture.text("search"))
        assertEquals("::alpha16", fixture.text("detail_name"))
        assertEquals(1, fixture.client.messages.filterIsInstance<IfOpenSub>().size)
    }

    @Test
    fun `category and selection survive a row redraw`() {
        val fixture = Fixture()
        fixture.command("alpha", "A utility command")
        fixture.command("travel_a", "Teleport to destination A")
        fixture.command("travel_b", "Teleport to destination B")
        fixture.open()

        fixture.click("category_travel")
        fixture.click("row_1")

        assertEquals("2 results / page 1 of 1", fixture.text("page"))
        assertEquals("::travel_b", fixture.text("detail_name"))
        assertEquals("Teleport to destination B", fixture.text("detail_desc"))
    }

    @Test
    fun `use closes the menu before executing the selected command`() {
        val fixture = Fixture()
        var executed = false
        fixture.command("alpha") {
            assertFalse(fixture.player.ui.containsModal("interface.commands_menu"))
            executed = true
        }
        fixture.open()

        fixture.click("use")

        assertTrue(executed)
    }

    @Test
    fun `ordinary players only see commands their rights allow`() {
        val fixture = Fixture()
        fixture.command("alpha")
        fixture.scripts.onCommand("restricted") {
            desc = "Administrator command"
            requiredRights = Rights.ADMINISTRATOR
            cheat { error("Must not execute") }
        }
        fixture.open()

        assertEquals("1 results / page 1 of 1", fixture.text("page"))
        assertTrue(fixture.text("row_0").contains("::alpha"))
        assertEquals("", fixture.text("row_1"))
    }

    private class Fixture {
        val events = EventBus()
        val commands = CheatCommandMap()
        val scripts = ScriptContext(events, commands, EngineQueueCache())
        val client = RecordingClient()
        val player = Player(client).apply { username = "commands-test" }
        private val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { events })
        private val launcher: ProtectedAccessLauncher

        init {
            val factory = mock(ProtectedAccessContextFactory::class.java)
            `when`(factory.create()).thenReturn(context)
            launcher = ProtectedAccessLauncher(factory)
            with(NativeCommandsInterface(launcher, commands)) {
                scripts.startup()
            }
        }

        fun command(name: String, description: String = "A command", action: () -> Unit = {}) {
            scripts.onCommand(name) {
                desc = description
                cheat { action() }
            }
        }

        fun open(query: String = "") {
            assertTrue(commands.execute(player, "commands", listOf(query)))
            assertTrue(player.ui.containsModal("interface.commands_menu"))
        }

        fun click(name: String) {
            val component = ServerCacheManager.fromComponent(component(name))
            launcher.launchLenient(player) {
                assertTrue(events.publish(this, IfModalButton(component, -1, null, IfButtonOp.Op1)))
            }
        }

        fun text(name: String): String = client.messages.filterIsInstance<IfSetText>()
            .last { it.combinedId == component(name) }.text

        private fun component(name: String) = "component.commands_menu:$name".asRSCM(RSCMType.COMPONENT)
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
        @JvmStatic
        @BeforeAll
        fun loadCache() {
            ServerCacheManager.init(240).close()
        }
    }
}
