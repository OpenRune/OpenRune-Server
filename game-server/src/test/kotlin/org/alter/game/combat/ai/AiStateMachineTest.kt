package org.alter.game.combat.ai

import org.alter.game.model.entity.Npc
import sun.misc.Unsafe
import java.lang.reflect.Field
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class AiStateMachineTest {

    // ---------------------------------------------------------------------------
    // Test helpers
    // ---------------------------------------------------------------------------

    /**
     * Creates a bare Npc instance without invoking any constructor, so no real
     * World or cache is needed. Valid only for tests that never call Npc methods.
     */
    private val testNpc: Npc by lazy {
        val field: Field = Unsafe::class.java.getDeclaredField("theUnsafe")
        field.isAccessible = true
        val unsafe = field.get(null) as Unsafe
        unsafe.allocateInstance(Npc::class.java) as Npc
    }

    /**
     * Records every lifecycle event and optionally transitions to [transitionTo]
     * on the first tick.
     */
    private class RecordingState(
        val name: String,
        private val transitionTo: AiState? = null,
    ) : AiState {
        val events = mutableListOf<String>()

        override fun onEnter(npc: Npc) { events += "enter:$name" }
        override fun onExit(npc: Npc)  { events += "exit:$name"  }
        override fun tick(npc: Npc): AiState? {
            events += "tick:$name"
            return transitionTo
        }
    }

    // ---------------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------------

    @Test
    fun `start calls onEnter on initial state`() {
        val state = RecordingState("A")
        val machine = AiStateMachine(state)

        machine.start(testNpc)

        assertEquals(listOf("enter:A"), state.events)
    }

    @Test
    fun `tick stays in current state when tick returns null`() {
        val state = RecordingState("A", transitionTo = null)
        val machine = AiStateMachine(state)
        machine.start(testNpc)
        state.events.clear()

        machine.tick(testNpc)

        assertEquals(listOf("tick:A"), state.events)
        assertSame(state, machine.currentState)
    }

    @Test
    fun `tick transitions to new state when tick returns non-null`() {
        val stateB = RecordingState("B")
        val stateA = RecordingState("A", transitionTo = stateB)
        val machine = AiStateMachine(stateA)
        machine.start(testNpc)
        stateA.events.clear()

        machine.tick(testNpc)

        // stateA: tick then exit; stateB: enter
        assertEquals(listOf("tick:A", "exit:A"), stateA.events)
        assertEquals(listOf("enter:B"), stateB.events)
        assertSame(stateB, machine.currentState)
    }

    @Test
    fun `chained transitions work across multiple ticks`() {
        val stateC = RecordingState("C")
        val stateB = RecordingState("B", transitionTo = stateC)
        val stateA = RecordingState("A", transitionTo = stateB)
        val machine = AiStateMachine(stateA)
        machine.start(testNpc)

        machine.tick(testNpc) // A -> B
        machine.tick(testNpc) // B -> C
        machine.tick(testNpc) // C stays

        assertSame(stateC, machine.currentState)
        assertEquals(listOf("enter:A", "tick:A", "exit:A"), stateA.events)
        assertEquals(listOf("enter:B", "tick:B", "exit:B"), stateB.events)
        assertEquals(listOf("enter:C", "tick:C"), stateC.events)
    }

    @Test
    fun `onEnter not called on initial state before start`() {
        val state = RecordingState("A")
        @Suppress("UNUSED_VARIABLE")
        val machine = AiStateMachine(state)

        assertEquals(emptyList(), state.events, "onEnter must not fire until start() is called")
    }
}
