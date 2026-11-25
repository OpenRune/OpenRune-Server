package org.rsmod.content.quest.mudfoot

import org.rsmod.api.combat.commons.npc.NpcAttackHook
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.npc.owner.isSpawnOwnedByOther
import org.rsmod.api.player.hook.PlayerPostTickHook
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.module.PluginModule

class MudfootModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerPostTickHook>(MudfootLeash::class.java)
        addSetBinding<NpcAttackValidateHook>(MudfootOutlawAttackHook::class.java)
        addSetBinding<NpcAttackHook>(MudfootOutlawSpotanims::class.java)
    }
}

class MudfootOutlawAttackHook : NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        if (OUTLAWS.none(npc::isType) || !npc.isSpawnOwnedByOther(player)) {
            return NpcAttackValidateResult.Pass
        }
        return NpcAttackValidateResult.Deny("This is not your target.")
    }

    private companion object {
        val OUTLAWS =
            setOf(
                "npc.mba_outlaw_ranged_combat",
                "npc.mba_outlaw_magic_combat",
                "npc.mba_outlaw_melee_combat",
            )
    }
}

class MudfootOutlawSpotanims : NpcAttackHook {
    override fun onAttack(npc: Npc, target: Player) {
        when {
            npc.isType("npc.mba_outlaw_ranged_combat") ->
                npc.spotanim("spotanim.bronze_arrow_launch", height = ARROW_LAUNCH_HEIGHT)
            npc.isType("npc.mba_outlaw_magic_combat") -> {
                npc.spotanim("spotanim.windstrike_casting", height = CAST_HEIGHT)
                target.spotanim(
                    "spotanim.windstrike_impact",
                    delay = IMPACT_DELAY,
                    height = IMPACT_HEIGHT,
                    slot = IMPACT_SLOT,
                )
            }
        }
    }

    private companion object {
        const val ARROW_LAUNCH_HEIGHT = 96
        const val CAST_HEIGHT = 92
        const val IMPACT_DELAY = 90
        const val IMPACT_HEIGHT = 124
        const val IMPACT_SLOT = 2
    }
}
