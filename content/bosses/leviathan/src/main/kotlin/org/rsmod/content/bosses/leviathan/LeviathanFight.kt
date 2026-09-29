package org.rsmod.content.bosses.leviathan

import org.rsmod.api.bosses.spec.HitType
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid

internal object LeviathanVarns {
    const val VOLLEY_STAGE = "varn.leviathan_volley_stage"
    const val VOLLEYS = "varn.leviathan_volleys"
    const val NEXT_SPECIAL = "varn.leviathan_next_special"
    const val STUNS = "varn.leviathan_stuns"
    const val STUNNED = "varn.leviathan_stunned"
    const val STUN_BEARING = "varn.leviathan_stun_bearing"
    const val IN_SPECIAL = "varn.leviathan_in_special"
}

internal enum class Special(val varnValue: Int, val hints: List<CoordGrid>) {
    Lightning(1, LeviathanArena.LIGHTNING_HINTS),
    Smoke(2, LeviathanArena.SMOKE_HINTS);

    val other: Special
        get() = if (this == Lightning) Smoke else Lightning

    companion object {
        const val NONE = 0

        fun fromVarn(value: Int): Special? = entries.firstOrNull { it.varnValue == value }
    }
}

internal enum class OrbStyle(
    val projectile: String,
    val launchSpotanim: String,
    val followSpotanim: String,
    val impactSpotanim: String,
    val synth: String,
    val type: HitType,
    val maxHit: Int,
) {
    Melee(
        projectile = "spotanim.vfx_leviathan_01_projectile_melee_01",
        launchSpotanim = "spotanim.vfx_leviathan_01_projectile_spotanim_melee_02",
        followSpotanim = "spotanim.vfx_leviathan_01_projectile_spotanim_melee_01",
        impactSpotanim = "spotanim.vfx_leviathan_01_projectile_impact_melee_01",
        synth = "synth.leviathan_orb_melee",
        type = HitType.Melee,
        maxHit = 50,
    ),
    Ranged(
        projectile = "spotanim.vfx_leviathan_01_projectile_ranged_01",
        launchSpotanim = "spotanim.vfx_leviathan_01_projectile_spotanim_ranged_02",
        followSpotanim = "spotanim.vfx_leviathan_01_projectile_spotanim_ranged_01",
        impactSpotanim = "spotanim.vfx_leviathan_01_projectile_impact_ranged_01",
        synth = "synth.leviathan_orb_ranged",
        type = HitType.Ranged,
        maxHit = 24,
    ),
    Magic(
        projectile = "spotanim.vfx_leviathan_01_projectile_magic_01",
        launchSpotanim = "spotanim.vfx_leviathan_01_projectile_spotanim_magic_02",
        followSpotanim = "spotanim.vfx_leviathan_01_projectile_spotanim_magic_01",
        impactSpotanim = "spotanim.vfx_leviathan_01_projectile_impact_magic_01",
        synth = "synth.leviathan_orb_magic",
        type = HitType.Magic,
        maxHit = 32,
    );

    companion object {
        val DISTANCED = listOf(Ranged, Magic)
        val ALL = listOf(Melee, Ranged, Magic)
    }
}

internal data class VolleyStage(
    val interval: Int,
    val shots: Int,
    val allStyles: Boolean,
    val orbDelay: Int,
    val orbTravel: Int,
)

internal class LeviathanFight(val npc: Npc, val arena: Arena) {
    var specialAngle: Int = 0

    var absentTicks: Int = 0
    var ended: Boolean = false

    var pathfinder: Npc? = null
    var pathfinderCorner: Int = 0
}
