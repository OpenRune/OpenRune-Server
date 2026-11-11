package org.rsmod.api.death

import org.rsmod.api.player.protect.ProtectedAccess

public interface PlayerDeathOverrideHook {
    public suspend fun ProtectedAccess.override(): Boolean
}
