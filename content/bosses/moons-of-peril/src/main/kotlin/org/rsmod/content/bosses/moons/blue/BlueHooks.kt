package org.rsmod.content.bosses.moons.blue

import dev.openrune.types.ItemServerType
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.death.PlayerDeathPreDropHook
import org.rsmod.api.player.hook.HeldEquipValidateHook
import org.rsmod.api.player.output.soundSynth
import org.rsmod.content.bosses.moons.Moon
import org.rsmod.content.bosses.moons.MoonsFights
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

class BlueWearPreventHook @Inject constructor(private val frozenWeapons: FrozenWeapons) :
    HeldEquipValidateHook {
    override fun validate(player: Player, type: ItemServerType): String? {
        val held = frozenWeapons.held(player)
        val frozen = held != null || player.vars[WEAR_PREVENT_VARBIT] == 1
        if (!frozen) return null
        val slots = setOf(type.wearpos1, type.wearpos2, type.wearpos3)
        val blocked =
            Wearpos.RightHand.slot in slots ||
                (held != null && held.isTwoHanded() && Wearpos.LeftHand.slot in slots)
        return if (blocked) FROZEN_MESSAGE else null
    }

    private fun ItemServerType.isTwoHanded(): Boolean =
        wearpos2 == Wearpos.LeftHand.slot || wearpos3 == Wearpos.LeftHand.slot

    companion object {
        const val FROZEN_MESSAGE = "Your weapon is still frozen in the ice."
        private const val WEAR_PREVENT_VARBIT = "varbit.pmoon_boss_blue_wearprevent"
    }
}

class IcicleAttackHook
@Inject
constructor(private val fights: MoonsFights, private val blue: BlueFights) :
    NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        if (npc.visType.id !in ICICLE_IDS) return NpcAttackValidateResult.Pass
        if (fights.of(player)?.moon != Moon.Blue) return NpcAttackValidateResult.Pass
        if (player.vars[BlueFights.ICICLE_STATE] == 0) {
            val unfrozen = blue.icicleLocked() || player.worn[Wearpos.RightHand.slot] != null
            return if (unfrozen) NpcAttackValidateResult.Deny(NOT_FROZEN_MESSAGE) else NpcAttackValidateResult.Pass
        }
        player.soundSynth(PUNCH_SYNTH)
        return NpcAttackValidateResult.Deny(RETRIEVED_MESSAGE)
    }

    private companion object {
        private const val NOT_FROZEN_MESSAGE = "Your weapon hasn't frozen over yet."
        private const val RETRIEVED_MESSAGE = "You've already retrieved your weapon!"
        private const val PUNCH_SYNTH = "synth.varl_pm_boss_blue_weapon_ice_block_cracking_01"
        private val ICICLE_IDS =
            MoonsFights.ICICLE_NPCS
                .map { it.asRSCM(RSCMType.NPC) }
                .toSet()
    }
}

class FrozenWeaponDeathHook @Inject constructor(private val blue: BlueFights) :
    PlayerDeathPreDropHook {
    override fun beforeDrops(player: Player) {
        blue.onDeathRelease(player)
    }
}
