package org.rsmod.content.bosses.leviathan

import jakarta.inject.Inject
import org.rsmod.api.combat.weapon.types.AttackTypes
import org.rsmod.api.config.Constants
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.module.PluginModule

public class LeviathanModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcAttackValidateHook>(LeviathanMeleeBlockHook::class.java)
    }
}

internal class LeviathanMeleeBlockHook @Inject constructor(private val types: AttackTypes) :
    NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        if (!npc.isType(LeviathanFights.BOSS_NPC)) return NpcAttackValidateResult.Pass
        val type = types.get(player)
        if (type != null && !type.isMelee) return NpcAttackValidateResult.Pass
        return NpcAttackValidateResult.Deny(Constants.dm_reach)
    }
}
