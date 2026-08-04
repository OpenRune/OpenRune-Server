package org.rsmod.api.combat.scripts

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.combat.NvPCombat
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.AttackType
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.commons.types.RangedAttackType
import org.rsmod.api.combat.player.aggressiveNpc
import org.rsmod.api.config.refs.params
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.isInPvnCombat
import org.rsmod.api.player.isInPvpCombat
import org.rsmod.api.script.advanced.onDefaultAiApPlayer2
import org.rsmod.api.script.advanced.onDefaultAiOpPlayer2
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class NvPCombatScript
@Inject
constructor(private val combat: NvPCombat, private val areaChecker: AreaChecker) : PluginScript() {
    override fun ScriptContext.startup() {
        onDefaultAiOpPlayer2 { attemptCombat(it.target) }
        onDefaultAiApPlayer2 { attemptCombat(it.target) }
    }

    private fun StandardNpcAccess.attemptCombat(target: Player) {
        if (!canAttack(target)) {
            resetMode()
            return
        }
        val attack = npc.resolveNpcAttack()
        combat.attack(this, target, attack)
    }

    private fun StandardNpcAccess.canAttack(target: Player): Boolean {
        val singleCombat = !mapMultiway(areaChecker)
        if (singleCombat) {
            if (target.isInPvpCombat()) {
                return false
            }

            if (target.isInPvnCombat()) {
                if (target.aggressiveNpc != null && target.aggressiveNpc != npc.uid) {
                    return false
                }
            }
        }
        return true
    }

    private fun Npc.resolveNpcAttack(): CombatAttack.NpcAttack {
        val attackType = resolveAttackType()
        return when {
            attackType.isRanged ->
                CombatAttack.NpcRanged(RangedAttackType.from(attackType) ?: RangedAttackType.Standard)
            // `maxHit = 0` defers max-hit resolution to the npc's magic stats in [NvPCombat].
            attackType.isMagic -> CombatAttack.NpcMagic(maxHit = 0)
            else -> CombatAttack.NpcMelee(MeleeAttackType.from(attackType))
        }
    }

    private fun Npc.resolveAttackType(): AttackType {
        val category = visType.paramOrNull(params.npc_attack_type) ?: return AttackType.Crush

        return when {
            category.isType("category.attacktype_stab") -> AttackType.Stab
            category.isType("category.attacktype_slash") -> AttackType.Slash
            category.isType("category.attacktype_light") -> AttackType.Light
            category.isType("category.attacktype_standard") -> AttackType.Standard
            category.isType("category.attacktype_heavy") -> AttackType.Heavy
            category.isType("category.attacktype_magic") -> AttackType.Magic
            else -> AttackType.Crush
        }
    }
}
