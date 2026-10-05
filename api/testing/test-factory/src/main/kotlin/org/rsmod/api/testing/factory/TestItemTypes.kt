package org.rsmod.api.testing.factory

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType

public fun itemType(gameval: String): ItemServerType =
    checkNotNull(ServerCacheManager.getItem(gameval.asRSCM(RSCMType.OBJ))) {
        "No item type for `$gameval`."
    }
