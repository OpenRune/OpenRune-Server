package org.rsmod.content.quest.area.varrock.shieldofarrav

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

class JonnyTheBeardAttackHook : NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        if (!npc.isJonny() || player.phoenixGang == PHOENIX_TASK) {
            return NpcAttackValidateResult.Pass
        }
        return NpcAttackValidateResult.Deny(NOT_YOUR_TARGET_MESSAGE)
    }

    private companion object {
        const val NOT_YOUR_TARGET_MESSAGE = "You can't attack Jonny the Beard."
    }
}

class JonnyTheBeardKillHook @Inject constructor(private val objRepo: ObjRepository) :
    NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        val hero = context.hero
        if (!context.npc.isJonny() || hero.phoenixGang != PHOENIX_TASK || hero.owns(INTEL_REPORT)) {
            return
        }
        objRepo.add(INTEL_REPORT, context.dropCoords, constants.lootdrop_duration, hero)
    }
}

class WeaponsmasterKillHook : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        if (context.npc.isType(ShieldOfArravBlackArm.WEAPONSMASTER)) {
            context.hero.weaponsmasterDead = 1
        }
    }
}

private fun Npc.isJonny(): Boolean =
    isType(ShieldOfArravPhoenix.JONNY) ||
        isType(ShieldOfArravPhoenix.JONNY_TALK) ||
        isType(ShieldOfArravPhoenix.JONNY_ATTACKABLE)
