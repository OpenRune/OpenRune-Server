package org.rsmod.api.player.ui

import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.types.aconverted.interf.IfButtonOp
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class PlayerButtonEventTest {
    private val events = EventBus()
    private val player = Player()
    private val component = mockk<ComponentType> { every { packed } returns COMPONENT }
    private val seen = mutableListOf<PlayerButtonEvent>()

    init {
        events.subscribeUnbound(PlayerButtonEvent::class.java) { seen += this }
    }

    @Test
    fun `overlay click with a handler is published`() {
        handleOverlay()

        events.observeButton(player, IfOverlayButton(component, 4, null, IfButtonOp.Op2))

        val event = seen.single()
        assertEquals(player, event.player)
        assertEquals(4, event.comsub)
        assertEquals(IfButtonOp.Op2, event.op)
        assertFalse(event.modal)
    }

    @Test
    fun `modal click with a handler is published as modal`() {
        handleModal()

        events.observeButton(player, IfModalButton(component, -1, null, IfButtonOp.Op1))

        assertTrue(seen.single().modal)
    }

    @Test
    fun `click without a handler is not published`() {
        events.observeButton(player, IfOverlayButton(component, 0, null, IfButtonOp.Op1))
        events.observeButton(player, IfModalButton(component, 0, null, IfButtonOp.Op1))

        assertTrue(seen.isEmpty())
    }

    @Test
    fun `an overlay handler does not make a modal click observable`() {
        handleOverlay()

        events.observeButton(player, IfModalButton(component, 0, null, IfButtonOp.Op1))

        assertTrue(seen.isEmpty())
    }

    @Test
    fun `nothing happens when no one is listening`() {
        val quiet = EventBus()
        quiet.subscribeSuspend<ProtectedAccess, IfOverlayButton>(
            IfOverlayButton::class.java,
            COMPONENT.toLong(),
        ) {}

        quiet.observeButton(player, IfOverlayButton(component, 0, null, IfButtonOp.Op1))
    }

    private fun handleOverlay() {
        events.subscribeSuspend<ProtectedAccess, IfOverlayButton>(
            IfOverlayButton::class.java,
            COMPONENT.toLong(),
        ) {}
    }

    private fun handleModal() {
        events.subscribeSuspend<ProtectedAccess, IfModalButton>(
            IfModalButton::class.java,
            COMPONENT.toLong(),
        ) {}
    }

    private companion object {
        const val COMPONENT = 0x1234_0005
    }
}
