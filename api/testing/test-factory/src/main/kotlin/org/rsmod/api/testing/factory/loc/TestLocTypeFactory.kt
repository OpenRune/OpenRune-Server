package org.rsmod.api.testing.factory.loc

import dev.openrune.types.ObjectServerType
import org.rsmod.api.testing.factory.TestCacheTypes

public class TestLocTypeFactory {
    /**
     * Creates a synthetic loc type. Without an [id] it takes an id unused by the real cache and
     * becomes resolvable through [dev.openrune.ServerCacheManager.getObject]. With the id of a real
     * cache entry it is only constructed, so lookups by id still return the real type.
     */
    public fun create(id: Int? = null, init: ObjectServerType.() -> Unit = {}): ObjectServerType {
        val typeId = id ?: TestCacheTypes.objects.nextId()
        val type = ObjectServerType(id = typeId, name = "test_loc_type")
        type.apply(init)
        TestCacheTypes.objects.registerIfSynthetic(type.id, type)
        return type
    }
}
