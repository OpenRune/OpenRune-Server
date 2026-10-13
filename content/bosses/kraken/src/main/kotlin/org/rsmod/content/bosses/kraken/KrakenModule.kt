package org.rsmod.content.bosses.kraken

import jakarta.inject.Inject
import org.rsmod.api.combatachievements.CombatAchievements
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.module.PluginModule

class KrakenModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcAttackValidateHook>(KrakenAttackHook::class.java)
        addSetBinding<NpcDeathKillHook>(KrakenCombatAchievementHook::class.java)
    }
}

internal class KrakenCombatAchievementHook @Inject constructor(
    private val controller: KrakenController,
    private val combatAchievements: CombatAchievements,
) : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) = prepare(context).invoke()

    override fun prepare(context: NpcDeathKillContext): () -> Unit {
        val kill = controller.claimAchievementKill(context.npc, context.hero) ?: return {}
        var applied = false
        return {
            if (!applied) {
                applied = true
                combatAchievements.recordKrakenKill(context.hero, kill.eligible, kill.allTentaclesKilled)
            }
        }
    }
}

internal class KrakenAttackHook @Inject constructor(private val controller: KrakenController) : NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        if (KrakenKind.entries.none { it.activeId == npc.type.id }) return NpcAttackValidateResult.Pass
        if (!controller.allowed(player)) return NpcAttackValidateResult.Deny("You need level 87 Slayer and a cave kraken task.")
        if (controller.owner(npc)?.let { it !== player } == true)
            return NpcAttackValidateResult.Deny("Someone else is already fighting this kraken.")
        return NpcAttackValidateResult.BypassSingleWayPvnRestriction
    }
}
