package org.rsmod.content.bosses.zulrah

import jakarta.inject.Inject
import org.rsmod.api.death.PlayerDeathContext
import org.rsmod.api.death.PlayerDeathDropHook
import org.rsmod.api.death.PlayerDeathDrops.DeathDropResult
import org.rsmod.api.death.PlayerDeathHandling
import org.rsmod.api.instances.InstanceManager

class ZulrahDeathDropHook
@Inject
constructor(
    private val instances: InstanceManager,
    private val recovery: ZulrahDeathRecovery,
) : PlayerDeathDropHook {
    override fun processDrops(
        context: PlayerDeathContext,
        handling: PlayerDeathHandling,
        result: DeathDropResult,
    ): DeathDropResult {
        return recovery.processDeath(
            context.player,
            handling,
            result,
            inZulrah = instances.sessionForPlayer(context.player)?.key == "zulrah",
        )
    }
}
