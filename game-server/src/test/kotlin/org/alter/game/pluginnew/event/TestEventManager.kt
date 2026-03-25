package org.alter.game.pluginnew.event

import java.util.function.Predicate

/**
 * A lightweight, instantiable event manager for unit tests.
 * Uses the same sequential priority-sorted dispatch logic as the real EventManager
 * so tests verify real ordering behavior.
 */
class TestEventManager : IEventManager {
    private val listeners = mutableMapOf<Class<out Event>, MutableList<EventListener<out Event>>>()

    override fun <E : Event> listen(event: Class<out Event>, listener: EventListener<E>) {
        listeners.getOrPut(event) { mutableListOf() }.add(listener)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <E : Event> postAndWait(event: E) {
        val list = listeners[event::class.java] ?: return
        val sorted = list.sortedBy { it.priority }
        for (listener in sorted) {
            val l = listener as EventListener<Event>
            try {
                if (l.condition(event)) {
                    kotlinx.coroutines.runBlocking { l.action(event) }
                } else {
                    l.otherwiseAction(event)
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }

    override fun <E : Event> post(event: E) = postAndWait(event)
    override fun <E : Event> postWithResult(event: E): Boolean { postAndWait(event); return true }
    override fun <E : Event> postAndCall(event: E, completion: Runnable) { postAndWait(event); completion.run() }
    override fun <E : Event, K> postAndReturn(event: E): List<K> = emptyList()
    override fun <E : Event, K> listenReturnable(event: Class<out Event>, listener: ReturnableEventListener<E, K>) {}
    override fun <E : Event> addFilter(clazz: Class<E>, filter: Predicate<E>) {}
}
