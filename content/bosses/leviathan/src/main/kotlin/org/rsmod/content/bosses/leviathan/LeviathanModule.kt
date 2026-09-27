package org.rsmod.content.bosses.leviathan

import jakarta.inject.Inject
import org.rsmod.api.combat.weapon.types.AttackTypes
import org.rsmod.api.config.Constants
import org.rsmod.api.config.refs.BaseParams
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.module.PluginModule

public class LeviathanModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcDeathKillHook>(LeviathanAwakenedKillHook::class.java)
        addSetBinding<NpcAttackValidateHook>(LeviathanMeleeBlockHook::class.java)
    }
}

public class LeviathanAwakenedKillHook @Inject constructor() : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        if (context.npc.vars["varn.skip_killcount"] != 1) return
        if (!context.npc.isType(LeviathanEncounters.BOSS_NPC)) return
        val varp = context.npc.paramOrNull(BaseParams.killcount_varp_awakened) ?: return
        val count = context.hero.vars[varp] + 1
        VarPlayerIntMapSetter.set(context.hero, varp, count)
        context.hero.mes("Your ${context.npc.name} (Awakened) kill count is: <col=ff0000>$count</col>")
    }
}

internal class LeviathanMeleeBlockHook @Inject constructor(private val types: AttackTypes) :
    NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        if (!npc.isType(LeviathanEncounters.BOSS_NPC)) return NpcAttackValidateResult.Pass
        val type = types.get(player)
        if (type != null && !type.isMelee) return NpcAttackValidateResult.Pass
        return NpcAttackValidateResult.Deny(Constants.dm_reach)
    }
}
