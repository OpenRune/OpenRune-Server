package org.rsmod.content.bosses.giantmole

import jakarta.inject.Inject
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.player.hook.PlayerPostTickHook
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

internal class MoleHolePostTickHook @Inject constructor(
    private val environment: MoleHoleEnvironment
) : PlayerPostTickHook {
    override fun onPostTick(player: Player) {
        environment.onTick(player)
    }
}

internal class MoleHoleAttackHook @Inject constructor(
    private val environment: MoleHoleEnvironment
) : NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        if (!environment.isDark(player)) {
            return NpcAttackValidateResult.Pass
        }
        if (environment.hasLightSource(player)) {
            return NpcAttackValidateResult.Pass
        }
        return NpcAttackValidateResult.Deny("You can't attack without a light source.")
    }
}
