package org.rsmod.content.slayer.requirements

import jakarta.inject.Inject
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.player.stat.slayerLvl
import org.rsmod.api.table.slayer.SlayerMonstersRow
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

internal object SlayerLevelGate {
    const val MESSAGE = "You need a higher Slayer level to know how to wound this monster."

    fun verdict(slayerLevel: Int, monster: SlayerMonstersRow?): NpcAttackValidateResult =
        if (monster == null || slayerLevel >= monster.level) {
            NpcAttackValidateResult.Pass
        } else {
            NpcAttackValidateResult.Deny(MESSAGE)
        }
}

internal class SlayerLevelAttackHook @Inject constructor() : NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult =
        SlayerLevelGate.verdict(player.slayerLvl, SlayerMonsters.find(npc))
}
