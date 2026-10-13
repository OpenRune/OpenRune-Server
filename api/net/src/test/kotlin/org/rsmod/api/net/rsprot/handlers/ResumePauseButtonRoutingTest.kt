package org.rsmod.api.net.rsprot.handlers

import com.google.inject.AbstractModule
import com.google.inject.Guice
import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import net.rsprot.protocol.game.incoming.resumed.ResumePauseButton
import net.rsprot.protocol.util.CombinedId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.IfModalPauseButton
import org.rsmod.api.player.ui.ifOpenMainModal
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player

@ResourceLock("ServerCacheManager")
class ResumePauseButtonRoutingTest {
    @Test
    fun `idle native browser pause selection reaches keyed subscriber without closing modal`() {
        val bus = EventBus()
        val player = Player()
        var selected = -1
        bus.subscribeKeyed(IfModalPauseButton::class.java, BOSS_BUTTON.asRSCM(RSCMType.COMPONENT).toLong()) {
            selected = comsub
        }
        player.ifOpenMainModal("interface.ca_bosses", bus)
        player.ifSetEvents(BOSS_BUTTON, 5..5, IfEvent.PauseButton)

        val injector = Guice.createInjector(object : AbstractModule() {
            override fun configure() {
                bind(EventBus::class.java).toInstance(bus)
            }
        })
        injector.getInstance(ResumePauseButtonHandler::class.java)
            .handle(player, packet(BOSS_BUTTON, 5))

        assertEquals(5, selected)
        assertTrue(player.ui.containsModal("interface.ca_bosses"))
        assertTrue(player.ui.closeQueue.isEmpty())
        assertNull(player.activeCoroutine)
    }

    @Test
    fun `suspended ordinary dialogue retains pause input even with keyed subscriber`() {
        val bus = EventBus()
        val player = Player()
        var statelessCalled = false
        var input: ResumePauseButtonInput? = null
        val options = "component.chatmenu:options"
        bus.subscribeKeyed(IfModalPauseButton::class.java, options.asRSCM(RSCMType.COMPONENT).toLong()) {
            statelessCalled = true
        }
        val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { bus })
        assertTrue(ProtectedAccessLauncher.withProtectedAccess(player, context) {
            ifOpenMainModal("interface.chatmenu")
            player.ifSetEvents(options, 1..2, IfEvent.PauseButton)
            input = pauseButton()
        })
        assertTrue(player.activeCoroutine?.isAwaiting(ResumePauseButtonInput::class) == true)

        ResumePauseButtonHandler(bus).handle(player, packet(options, 2))

        assertFalse(statelessCalled)
        assertEquals(ResumePauseButtonInput(options, 2), input)
        assertFalse(player.ui.closeQueue.isEmpty())
    }

    @Test
    fun `unregistered pause selection follows existing coroutine fallback`() {
        val bus = EventBus()
        val player = Player()
        var input: ResumePauseButtonInput? = null
        val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { bus })
        assertTrue(ProtectedAccessLauncher.withProtectedAccess(player, context) {
            ifOpenMainModal("interface.ca_bosses")
            player.ifSetEvents(BOSS_BUTTON, 33..33, IfEvent.PauseButton)
            input = pauseButton()
        })

        ResumePauseButtonHandler(bus).handle(player, packet(BOSS_BUTTON, 33))

        assertNotNull(input)
        assertEquals(33, input?.subcomponent)
        assertFalse(player.ui.closeQueue.isEmpty())
    }

    @Test
    fun `closed interface and disabled child cannot publish stateless selection`() {
        val bus = EventBus()
        val player = Player()
        var calls = 0
        bus.subscribeKeyed(IfModalPauseButton::class.java, BOSS_BUTTON.asRSCM(RSCMType.COMPONENT).toLong()) {
            calls++
        }
        player.ifSetEvents(BOSS_BUTTON, 5..5, IfEvent.PauseButton)
        val handler = ResumePauseButtonHandler(bus)
        handler.handle(player, packet(BOSS_BUTTON, 5))
        player.ifOpenMainModal("interface.ca_bosses", bus)
        handler.handle(player, packet(BOSS_BUTTON, 0))
        handler.handle(player, packet(BOSS_BUTTON, 65535))

        assertEquals(0, calls)
        assertTrue(player.ui.closeQueue.isEmpty())
    }

    @Test
    fun `delayed browser cannot invoke idle selection handler`() {
        val bus = EventBus()
        val player = Player()
        var calls = 0
        bus.subscribeKeyed(IfModalPauseButton::class.java, BOSS_BUTTON.asRSCM(RSCMType.COMPONENT).toLong()) {
            calls++
        }
        player.ifOpenMainModal("interface.ca_bosses", bus)
        player.ifSetEvents(BOSS_BUTTON, 5..5, IfEvent.PauseButton)
        player.delay(1)

        ResumePauseButtonHandler(bus).handle(player, packet(BOSS_BUTTON, 5))

        assertEquals(0, calls)
        assertTrue(player.ui.closeQueue.isEmpty())
    }

    private fun packet(component: String, sub: Int) =
        ResumePauseButton(CombinedId(component.asRSCM(RSCMType.COMPONENT)), sub)

    companion object {
        private const val BOSS_BUTTON = "component.ca_bosses:bosses_button_click_pause"

        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
