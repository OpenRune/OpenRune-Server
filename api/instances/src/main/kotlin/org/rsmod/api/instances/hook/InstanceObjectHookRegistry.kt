package org.rsmod.api.instances.hook

import jakarta.inject.Singleton
import org.rsmod.api.player.protect.ProtectedAccess

@Singleton
internal class InstanceObjectHookRegistry {
    private val enterOverrides = HashMap<String, suspend ProtectedAccess.() -> Unit>()
    private val exitOverrides = HashMap<String, suspend ProtectedAccess.() -> Unit>()

    fun registerEnter(key: String, action: suspend ProtectedAccess.() -> Unit) {
        enterOverrides[key] = action
    }

    fun registerExit(key: String, action: suspend ProtectedAccess.() -> Unit) {
        exitOverrides[key] = action
    }

    fun getEnter(key: String): (suspend ProtectedAccess.() -> Unit)? = enterOverrides[key]
    fun getExit(key: String): (suspend ProtectedAccess.() -> Unit)? = exitOverrides[key]
}
