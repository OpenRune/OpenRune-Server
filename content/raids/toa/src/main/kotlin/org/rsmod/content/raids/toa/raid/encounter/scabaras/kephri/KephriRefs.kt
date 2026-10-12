package org.rsmod.content.raids.toa.raid.encounter.scabaras.kephri

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.game.entity.Npc
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid

internal object KephriNpcs {
    const val SHIELDED = "npc.toa_kephri_boss_shielded"
    const val WEAK = "npc.toa_kephri_boss_weak"
    const val ENRAGE = "npc.toa_kephri_boss_enrage"
    const val DEAD = "npc.toa_kephri_boss_dead"
    const val SHIELD_SCARAB = "npc.toa_kephri_shield_scarab"
    const val AGILE_SCARAB = "npc.toa_kephri_scarab_rangekite"
    const val EGG_EXPLODE = "npc.kephri_egg_explode"
    const val EGG_HATCH = "npc.kephri_egg_hatch"
    const val GUARDIAN_MELEE = "npc.toa_kephri_guardian_melee"
    const val GUARDIAN_RANGED = "npc.toa_kephri_guardian_ranged"
    const val GUARDIAN_MAGE = "npc.toa_kephri_guardian_mage"
}

internal object KephriObjs {
    const val DUNG = "obj.toa_kephri_poo"
    const val BOOK = "obj.toa_book_kephri"
}

internal object KephriVarbits {
    const val BOOK_OWNED = "varbit.toa_book_kephri"
}

internal object KephriLocs {
    const val BLOCKER = "loc.invisible_type8_blocking_size5"
    const val DUNG = "loc.kephri_dung"
    const val INVISIBLE = "loc.invisible_type0_nonblocking"
}

internal object KephriSeqs {
    const val AUTO = "seq.npc_kephri_attack_magic_radial"
    const val THROW = "seq.npc_kephri_attack_special_eggs"
    const val STUNNED = "seq.npc_kephri_stunned"
    const val WAKE = "seq.npc_kephri_stunned_wakeup"
    const val DEATH = "seq.npc_kephri_death"
    const val THROWN_PLAYER = "seq.human_troll_flyback_merge"
    const val EGG_SPAWN = "seq.nightmare_infection_spotanim_spawn"
    const val SCARAB_SPAWN = "seq.npc_scarab_spawn"
    const val SCARAB_ATTACK = "seq.npc_scarab_flying_attack"
    const val SCARAB_RANGED = "seq.npc_scarab_ranged_attack"
    const val SCARAB_MELEE = "seq.npc_scarab_attack"
    const val FLYING_SPAWN = "seq.npc_scarab_flying_spawn"
    const val SWARM_SPAWN = "seq.npc_swarm01_scarab01_spawn01"
    const val SWARM_INTEGRATE = "seq.npc_swarm01_scarab01_integrate01"
    const val SWARM_DEATH = "seq.npc_swarm01_scarab01_death01"
}

internal object KephriSpots {
    const val DUNG_INDICATOR = "spotanim.kephri_indicate_player_for_dung"
    const val DUNG_DROP = "spotanim.kephri_dung_drop"
    const val EGG_DARK = "spotanim.projectile_dungball01"
    const val EGG_SMALL = "spotanim.projectile_dungball02"
    const val EGG_BREAK = "spotanim.spotanim_dungball_explosion01"
    const val SCARAB_SHOT = "spotanim.toa_scarab_ranged_projectile01"
    const val MELEE_HEAL = "spotanim.toa_kephri_melee_heal"
    const val MAGE_CHARGE = "spotanim.toa_kephri_mage_charge"
    const val THROWN_PLAYER = "spotanim.stunned_thieving"
    const val SHIELD_DEATH = "spotanim.kephri_shield_death"
    const val BOMB = "spotanim.kephri_kamikaze_scarab"
    const val BOMB_EXPLODE = "spotanim.kephri_kamikaze_scarab_explode"
    const val SHIELD_REFORM = "spotanim.kephri_shield_reform"
    const val AUTO_APEX = "spotanim.vorkath_area_travel"
    const val AUTO_DROP = "spotanim.vorkath_area_travel_small"

    val EXPLOSIONS =
        listOf(
            "spotanim.vfx_scarab_explosion01",
            "spotanim.vfx_scarab_explosion02",
            "spotanim.vfx_scarab_explosion03",
            "spotanim.vfx_scarab_explosion04",
        )
}

internal object KephriCoords {
    val KEPHRI = CoordGrid(3549, 5406, 0)
    const val CENTRE_OFFSET = 2
}

internal object KephriAbilities {
    const val AUTO = "auto"
    const val SPECIAL = "special"
    const val EGGS = "eggs"
    const val DUNG = "dung"
    const val FINAL_DUNG = "final_dung"
    const val BOMBS = "bombs"
}

internal object KephriPhases {
    const val OPENING = "opening"
    const val STUN_1 = "stun_1"
    const val SHIELD_2 = "shield_2"
    const val STUN_2 = "stun_2"
    const val SHIELD_3 = "shield_3"
    const val FINAL = "final"

    val STUNS = listOf(STUN_1, STUN_2)

    val AFTER_BREAK = mapOf(OPENING to STUN_1, SHIELD_2 to STUN_2, SHIELD_3 to FINAL)

    val AFTER_STUN = mapOf(STUN_1 to SHIELD_2, STUN_2 to SHIELD_3)
}

internal object KephriVarns {
    const val EGGS_NEXT = "varn.toa_kephri_eggs_next"
}

internal object KephriMidis {
    const val FIGHT = "midi.toa_boss_kephri"
}

internal val Npc.kephriCentre: CoordGrid
    get() = coords.translate(KephriCoords.CENTRE_OFFSET, KephriCoords.CENTRE_OFFSET)

internal fun projAnim(
    spot: String,
    from: CoordGrid,
    to: CoordGrid,
    startHeight: Int,
    endHeight: Int,
    startTime: Int,
    endTime: Int,
    angle: Int,
    progress: Int,
): ProjAnim =
    ProjAnim(
        spotanim = spot.asRSCM(RSCMType.SPOTANIM),
        startHeight = startHeight,
        endHeight = endHeight,
        startTime = startTime,
        endTime = endTime,
        angle = angle,
        progress = progress,
        sourceIndex = 0,
        targetIndex = 0,
        startCoord = from,
        endCoord = to,
    )
