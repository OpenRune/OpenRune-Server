package org.rsmod.api.testing.factory.npc

import dev.openrune.ServerCacheManager
import dev.openrune.types.NpcMode
import dev.openrune.types.NpcServerType
import org.rsmod.api.testing.factory.TestCacheTypes

public class TestNpcTypeFactory {
    /**
     * Creates a synthetic npc type. Without an [id] it takes an id unused by the real cache and
     * becomes resolvable through [ServerCacheManager.getNpc]. With the id of a real cache entry
     * it is only constructed, so lookups by id still return the real type.
     */
    public fun create(id: Int? = null, init: NpcServerType.() -> Unit = {}): NpcServerType {
        val typeId = id ?: TestCacheTypes.npcs.nextId()
        val type = NpcServerType(id = typeId, name = "test_npc_type", defaultMode = NpcMode.None)
        type.apply(init)
        TestCacheTypes.npcs.registerIfSynthetic(type.id, type)
        return type
    }
}
