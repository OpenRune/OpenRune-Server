package org.rsmod.api.instances

import jakarta.inject.Singleton

@Singleton
public class BossInstanceRegistry {
    private val specs = LinkedHashMap<String, InstanceSpec>()

    public fun register(key: String, spec: InstanceSpec) {
        specs[key] = spec
    }

    public fun get(key: String): InstanceSpec? = specs[key]

    public fun first(): InstanceSpec? = specs.values.firstOrNull()

    public fun keys(): Set<String> = specs.keys
}
