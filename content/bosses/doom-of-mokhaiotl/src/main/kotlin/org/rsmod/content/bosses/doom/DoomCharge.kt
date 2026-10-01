package org.rsmod.content.bosses.doom

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.ProjectileConfig

internal object DoomVarns {
    const val CHARGE = "varn.dom_charge"
    const val FIGHT_TICKS = "varn.dom_fight_ticks"
    const val ATTACKS = "varn.dom_attacks"
    const val SHIELD_STASHED_HP = "varn.dom_shield_stashed_hp"
    const val CHARGE_END = "varn.dom_charge_end"
    const val CHARGE_LOOP_FROM = "varn.dom_charge_loop_from"
    const val ROTATION_STEP = "varn.dom_rotation_step"
    const val STEP_ATTACKS = "varn.dom_step_attacks"
    const val IMMUNE_MELEE = "varn.immune_melee"
    const val IMMUNE_RANGED = "varn.immune_ranged"
    const val IMMUNE_MAGIC = "varn.immune_magic"
    const val GUARANTEED_HIT = "varn.guaranteed_hit"

    const val CHARGE_NONE = 0
    const val CHARGE_MELEE = 1
    const val CHARGE_PUNISHED = 2
    const val CHARGE_SHIELD = 3
    const val CHARGE_SHIELD_BROKEN = 4
    const val CHARGE_BURROW = 5
}

internal object DoomCharge {
    const val PUNISH_BONUS_EXT = "doom.punish_bonus"

    const val CHARGE_BAR = "headbar.standard_charge_100"
    const val CHARGE_SEQ = "seq.dom_beam_charge"
    const val CHARGE_LOOP_SEQ = "seq.dom_beam_charge_loop"
    const val CANCEL_SEQ = "seq.dom_beam_cancel"
    const val CHARGE_UP_SPOTANIM = "spotanim.vfx_beam_charge_up_01"
    const val CHARGE_UP_SLOT = 2
    const val CYCLES_PER_TICK = 30
    private const val MELEE_CHARGE_TICKS = 13
    private const val MELEE_CHARGE_CYCLES = MELEE_CHARGE_TICKS * CYCLES_PER_TICK

    private const val HEAD_ICON_SLOT = 0
    private const val HEAD_ICON_GRAPHIC = 440
    private const val PROTECT_MISSILES_AND_MAGIC = 6

    private const val FIRE_SEQ = "seq.dom_beam_fire"

    private const val BEAM_HEAD = "spotanim.vfx_beam_attack_head_01"
    private const val BEAM_MIDDLE = "spotanim.vfx_beam_middle_segment_01"
    private const val BEAM_END = "spotanim.vfx_beam_end_segment_01"
    private const val BEAM_IMPACT = "spotanim.vfx_beam_impact_01"
    private const val BEAM_HEIGHT = 100
    private const val BEAM_IMPACT_HEIGHT = 50
    private const val BEAM_HIT_DELAY = 2
    private const val BEAM_IMPACT_DELAY = 30
    private const val BEAM_RECOVERY_TICKS = 2

    private val clearCharge: Effect =
        sequence(
            setVarn(DoomVarns.CHARGE, DoomVarns.CHARGE_NONE),
            setVarn(DoomVarns.IMMUNE_RANGED, 0),
            setVarn(DoomVarns.IMMUNE_MAGIC, 0),
            setVarn(DoomVarns.GUARANTEED_HIT, 0),
            clearHeadIcon(HEAD_ICON_SLOT),
            clearHeadbar(CHARGE_BAR),
        )

    fun meleeCharge(delve: DoomDelve): Effect =
        sequence(
            setVarn(DoomVarns.CHARGE, DoomVarns.CHARGE_MELEE),
            setVarn(DoomVarns.IMMUNE_RANGED, 1),
            setVarn(DoomVarns.IMMUNE_MAGIC, 1),
            setVarn(DoomVarns.GUARANTEED_HIT, 1),
            headIcon(HEAD_ICON_SLOT, HEAD_ICON_GRAPHIC, PROTECT_MISSILES_AND_MAGIC),
            anim(CHARGE_SEQ),
            headbar(CHARGE_BAR, fromPercent = 0, toPercent = 100, cycles = MELEE_CHARGE_CYCLES),
            repeat(
                MELEE_CHARGE_TICKS - 1,
                effect = sequence(wait(1), anim(CHARGE_LOOP_SEQ), spotanim(CHARGE_UP_SPOTANIM, slot = CHARGE_UP_SLOT)),
            ),
            wait(1),
            clearCharge,
            beam(delve),
            wait(BEAM_RECOVERY_TICKS),
        )

    fun punish(delve: DoomDelve): Effect =
        sequence(
            interrupt(),
            clearCharge,
            setVarn(DoomVarns.CHARGE, DoomVarns.CHARGE_PUNISHED),
            anim(CANCEL_SEQ),
            external(PUNISH_BONUS_EXT),
            nextAttackIn(delve.punishDelay),
            after(1, sequence(anim(CANCEL_SEQ), setVarn(DoomVarns.CHARGE, DoomVarns.CHARGE_NONE))),
        )

    fun beam(delve: DoomDelve): Effect =
        sequence(
            anim(FIRE_SEQ),
            beamSegment(BEAM_HEAD, delay = 0, travel = 25),
            beamSegment(BEAM_MIDDLE, delay = 1, travel = 25),
            beamSegment(BEAM_MIDDLE, delay = 1, travel = 26),
            beamSegment(BEAM_MIDDLE, delay = 1, travel = 27),
            beamSegment(BEAM_END, delay = 2, travel = 27),
            hit {
                delay = BEAM_HIT_DELAY
                damage(Fixed(delve.beamDamage))
                type(Typeless)
                spotanim(BEAM_IMPACT, height = BEAM_IMPACT_HEIGHT, delay = BEAM_IMPACT_DELAY)
            },
        )

    private fun beamSegment(spotanim: String, delay: Int, travel: Int): Effect =
        projectile(spotanim, config = ProjectileConfig.fixed(BEAM_HEIGHT, BEAM_HEIGHT, delay, travel, angle = 0))
}
