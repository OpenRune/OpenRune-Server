package org.rsmod.api.testing.factory.npc

import dev.openrune.ServerCacheManager
import dev.openrune.types.NpcMode
import dev.openrune.types.NpcServerType

public class TestNpcTypeFactory {
    public fun create(id: Int = 0, init: NpcServerType.() -> Unit = {}): NpcServerType {
        val type = NpcServerType(id = id, name = "test_npc_type", defaultMode = NpcMode.None)
        type.apply(init)
        ServerCacheManager.registerTestNpc(type)
        return type
    }
}
