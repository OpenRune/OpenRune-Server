package org.alter.game.pluginnew.event

import kotlin.collections.get
import kotlin.reflect.KClass

class ReturnableEventListener<E : Event,  K>(val type: KClass<E>)  {

    var condition: E.() -> Boolean = { true }

    var action: suspend E.() -> K? = { null }

    var otherwiseAction: E.() -> K? = { null }

    var priority: Int = 500

    var stack : Array<StackTraceElement> = emptyArray()

    fun where(condition: E.() -> Boolean): ReturnableEventListener<E, K> {
        this.condition = condition
        return this
    }

    fun then(plugin: suspend E.() -> K?): ReturnableEventListener<E, K> {
        this.action = plugin
        return this
    }

    fun otherwise(plugin: E.() -> K?): ReturnableEventListener<E, K> {
        this.otherwiseAction = plugin
        return this
    }

    fun priority(p: Int): ReturnableEventListener<E, K> {
        this.priority = p
        return this
    }

    @Suppress("UNCHECKED_CAST")
    fun submit() : ReturnableEventListener<E, K> {
        this.stack = Thread.currentThread().stackTrace
        val toAdd = listOf(this as ReturnableEventListener<out Event, Any>)

        val existing = EventManager.returnableListeners[type.java]
        if(existing == null) {
            EventManager.returnableListeners[type.java] = toAdd
        } else {
            EventManager.returnableListeners[type.java] = toAdd + existing
        }


        return this
    }

    companion object {

        inline fun <reified E : Event, K> on(config : ReturnableEventListener<E, K>.() -> ReturnableEventListener<E, K>) : ReturnableEventListener<E, K> {

            return config.invoke(ReturnableEventListener(E::class)).submit()
        }

    }
}