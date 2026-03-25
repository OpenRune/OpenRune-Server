package org.alter.game.pluginnew.event

import kotlin.test.Test
import kotlin.test.assertEquals

class EventListenerPriorityTest {

    // A minimal no-op event for testing purposes
    class TestEvent : Event

    @Test
    fun `EventListener has default priority of 500`() {
        val listener = EventListener(TestEvent::class)
        assertEquals(500, listener.priority)
    }

    @Test
    fun `EventListener priority builder sets value and returns this`() {
        val listener = EventListener(TestEvent::class)
        val returned = listener.priority(100)
        assertEquals(100, listener.priority)
        assertEquals(listener, returned)
    }

    @Test
    fun `ReturnableEventListener has default priority of 500`() {
        val listener = ReturnableEventListener<TestEvent, Int>(TestEvent::class)
        assertEquals(500, listener.priority)
    }

    @Test
    fun `ReturnableEventListener priority builder sets value and returns this`() {
        val listener = ReturnableEventListener<TestEvent, Int>(TestEvent::class)
        val returned = listener.priority(200)
        assertEquals(200, listener.priority)
        assertEquals(listener, returned)
    }

    @Test
    fun `listeners execute in priority order`() {
        val executionOrder = mutableListOf<String>()
        val em = TestEventManager()

        val listenerC = EventListener(TestEvent::class).apply {
            priority = 300
            then { executionOrder.add("C-300") }
        }
        val listenerA = EventListener(TestEvent::class).apply {
            priority = 100
            then { executionOrder.add("A-100") }
        }
        val listenerB = EventListener(TestEvent::class).apply {
            priority = 200
            then { executionOrder.add("B-200") }
        }

        em.listen(TestEvent::class.java, listenerC)
        em.listen(TestEvent::class.java, listenerA)
        em.listen(TestEvent::class.java, listenerB)

        em.postAndWait(TestEvent())

        assertEquals(listOf("A-100", "B-200", "C-300"), executionOrder)
    }
}
