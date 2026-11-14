package org.rsmod.content.bosses.bryophyta

import jakarta.inject.Inject
import org.rsmod.api.combat.manager.PlayerAttackManager
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.combat.weapon.types.AttackTypes
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.player.righthand
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getInvObj

internal class BryophytaPruneHook
@Inject
constructor(
    private val attackStyles: AttackStyles,
    private val attackTypes: AttackTypes,
    private val attackManager: PlayerAttackManager,
) : NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        if (!npc.isType(BryophytaGrowthling.NPC) || npc.hitpoints <= 0) {
            return NpcAttackValidateResult.Pass
        }
        val weapon = player.righthand?.let(::getInvObj) ?: return NpcAttackValidateResult.Pass
        if (!BryophytaGrowthling.isOneClick(weapon)) {
            return NpcAttackValidateResult.Pass
        }
        BryophytaGrowthling.prune(
            player = player,
            npc = npc,
            awardXp = true,
            attackStyles = attackStyles,
            attackTypes = attackTypes,
            attackManager = attackManager,
        )
        return NpcAttackValidateResult.Deny(BryophytaGrowthling.PRUNE_MESSAGE)
    }
}
