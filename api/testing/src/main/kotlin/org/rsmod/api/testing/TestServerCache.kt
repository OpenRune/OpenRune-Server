package org.rsmod.api.testing

import dev.openrune.ServerCacheManager
import java.nio.file.Paths
import org.rsmod.api.server.config.ServerConfigLoader

public object TestServerCache {
    private val loaded = lazy {
        val config = ServerConfigLoader().loadOrCreate(Paths.get("./", "game.yml"))
        ServerCacheManager.init(config.revision).close()
    }

    public fun load() {
        loaded.value
    }
}
