package org.rsmod.api.death

import org.rsmod.api.death.PlayerDeathDrops.DeathDropResult

public interface PlayerDeathDropHook {
    public fun processDrops(
        context: PlayerDeathContext,
        handling: PlayerDeathHandling,
        result: DeathDropResult,
    ): DeathDropResult
}
