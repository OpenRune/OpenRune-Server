package org.rsmod.api.instances.hook

import jakarta.inject.Singleton
import org.rsmod.api.instances.InstanceEnterTransition
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.player.protect.ProtectedAccess

public typealias InstanceEnterAction = suspend ProtectedAccess.() -> Unit

public typealias InstanceEnterPrelude =
    suspend ProtectedAccess.(InstanceManager.Result, InstanceEnterAction) -> Unit

@Singleton
internal class InstanceObjectHookRegistry {
    private val enterOverrides = HashMap<String, suspend ProtectedAccess.() -> Unit>()
    private val exitOverrides = HashMap<String, suspend ProtectedAccess.() -> Unit>()
    private val enterTransitions = HashMap<String, InstanceEnterTransition>()
    private val enterPreludes = HashMap<String, InstanceEnterPrelude>()

    fun registerEnter(key: String, action: suspend ProtectedAccess.() -> Unit) {
        enterOverrides[key] = action
    }

    fun registerExit(key: String, action: suspend ProtectedAccess.() -> Unit) {
        exitOverrides[key] = action
    }

    fun registerEnterTransition(key: String, transition: InstanceEnterTransition) {
        enterTransitions[key] = transition
    }

    fun registerEnterPrelude(key: String, action: InstanceEnterPrelude) {
        enterPreludes[key] = action
    }

    fun getEnter(key: String): (suspend ProtectedAccess.() -> Unit)? = enterOverrides[key]
    fun getExit(key: String): (suspend ProtectedAccess.() -> Unit)? = exitOverrides[key]
    fun getEnterTransition(key: String): InstanceEnterTransition? = enterTransitions[key]
    fun getEnterPrelude(key: String): InstanceEnterPrelude? = enterPreludes[key]
}
