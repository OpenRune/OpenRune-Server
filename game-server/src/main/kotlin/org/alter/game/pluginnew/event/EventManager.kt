@file:Suppress("UNCHECKED_CAST")

package org.alter.game.pluginnew.event

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import org.alter.game.pluginnew.event.impl.WorldTickEvent
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Predicate

interface IEventManager {
    fun <E : Event> post(event: E)
    fun <E : Event> postWithResult(event: E): Boolean
    fun <E : Event> postAndWait(event: E)
    fun <E : Event> postAndCall(event: E, completion: Runnable)
    fun <E : Event, K> postAndReturn(event: E): List<K>
    fun <E : Event> listen(event: Class<out Event>, listener: EventListener<E>)
    fun <E : Event, K> listenReturnable(event: Class<out Event>, listener: ReturnableEventListener<E, K>)
    fun <E : Event> addFilter(clazz: Class<E>, filter: Predicate<E>)
}

object EventManager : IEventManager {

    val DEBUG_EVENTS = false
    val DT_FORMAT = DateTimeFormat.forPattern("MM/dd/yyyy HH:mm:ss")

    private val eventChannels = ConcurrentHashMap<Class<out Event>, MutableSharedFlow<out Event>>()
    private val eventFilters = ConcurrentHashMap<Class<out Event>, LinkedList<Predicate<out Event>>>()
    private val listeners = ConcurrentHashMap<Class<out Event>, List<EventListener<out Event>>>()
    val returnableListeners = ConcurrentHashMap<Class<out Event>, List<ReturnableEventListener<out Event, Any>>>()

    @OptIn(DelicateCoroutinesApi::class)
    private val context = newFixedThreadPoolContext(
        (Runtime.getRuntime().availableProcessors() - 1).coerceAtLeast(1),
        "EventContext"
    )

    override fun <E : Event> post(event: E) {
        debugLog(event)
        CoroutineScope(context).launch {
            if (getFilters<E>(event.javaClass).all { it.test(event) }) {
                getChannel<E>(event.javaClass).emit(event)
            }
        }
    }

    override fun <E : Event> postWithResult(event: E): Boolean {
        debugLog(event)
        var handled = false

        runBlocking {
            processListeners(event, listeners[event.javaClass]) { handled = true }
            processListeners(event, returnableListeners[event.javaClass]) { handled = true }
        }

        return handled
    }

    override fun <E : Event> postAndWait(event: E) = runBlocking {
        processListeners(event, listeners[event.javaClass])
    }

    override fun <E : Event> postAndCall(event: E, completion: Runnable) {
        CoroutineScope(context).launch { postAndWait(event) }.invokeOnCompletion { completion.run() }
    }

    override fun <E : Event, K> postAndReturn(event: E): List<K> {
        post(event)
        val returns = mutableListOf<K>()

        returnableListeners[event.javaClass]?.forEach { listener ->
            runBlocking {
                try {
                    @Suppress("UNCHECKED_CAST")
                    val l = listener as ReturnableEventListener<E, K>
                    if (getFilters<E>(event.javaClass).all { it.test(event) } && l.condition(event)) {
                        l.action(event)?.let { returns.add(it) }
                    } else l.otherwiseAction(event)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }

        return returns
    }

    override fun <E : Event> addFilter(clazz: Class<E>, filter: Predicate<E>) {
        getFilters<E>(clazz).addLast(filter)
    }

    override fun <E : Event> listen(event: Class<out Event>, listener: EventListener<E>) {
        listeners.compute(event) { _, old -> (old ?: emptyList()) + listener }

        CoroutineScope(context).launch {
            getChannel<E>(event).collect {
                try {
                    if (listener.condition(it)) {
                        listener.action(it)
                        if (listener.singleUse) {
                            listeners.compute(event) { _, old -> old?.filter { it !== listener } }
                            cancel(CancellationException())
                        }
                    } else listener.otherwiseAction(it)
                } catch (_: CancellationException) {
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <E : Event, K> listenReturnable(event: Class<out Event>, listener: ReturnableEventListener<E, K>) {
        returnableListeners.compute(event) { _, old ->
            (old ?: emptyList()) + (listener as ReturnableEventListener<out Event, Any>)
        }
    }

    private fun debugLog(event: Event) {
        if (DEBUG_EVENTS && event::class != WorldTickEvent::class && event is PlayerEvent) {
            println("[${DT_FORMAT.print(DateTime.now())}] [${event.player.username}] ${event::class.simpleName}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <E : Event> processListeners(
        event: E,
        list: List<out Any>?,
        markHandled: (Boolean) -> Unit = {}
    ) {
        if (list.isNullOrEmpty()) return

        val sorted = list.sortedBy {
            when (it) {
                is EventListener<*> -> it.priority
                is ReturnableEventListener<*, *> -> it.priority
                else -> 500
            }
        }

        for (listener in sorted) {
            try {
                when (listener) {
                    is EventListener<*> -> {
                        val l = listener as EventListener<E>
                        if (getFilters<E>(event.javaClass).all { it.test(event) }) {
                            if (l.condition(event)) { l.action(event); markHandled(true) }
                            else l.otherwiseAction(event)
                        }
                    }
                    is ReturnableEventListener<*, *> -> {
                        val l = listener as ReturnableEventListener<E, Any>
                        if (getFilters<E>(event.javaClass).all { it.test(event) }) {
                            if (l.condition(event)) { l.action(event); markHandled(true) }
                            else l.otherwiseAction(event)
                        }
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun <E : Event> getChannel(clazz: Class<out Event>): MutableSharedFlow<E> {
        return eventChannels.computeIfAbsent(clazz) { MutableSharedFlow<E>() } as MutableSharedFlow<E>
    }

    private fun <E : Event> getFilters(clazz: Class<out Event>): LinkedList<Predicate<E>> {
        return eventFilters.computeIfAbsent(clazz) { LinkedList<Predicate<out Event>>() } as LinkedList<Predicate<E>>
    }
}
