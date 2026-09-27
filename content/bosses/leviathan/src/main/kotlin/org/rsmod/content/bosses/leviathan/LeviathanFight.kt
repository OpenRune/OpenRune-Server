package org.rsmod.content.bosses.leviathan

import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocInfo
import org.rsmod.map.CoordGrid

internal enum class Special {
    Lightning,
    Smoke;

    val other: Special
        get() = if (this == Lightning) Smoke else Lightning
}

internal enum class OrbStyle(
    val projectile: String,
    val launchSpotanim: String,
    val followSpotanim: String,
    val impactSpotanim: String,
    val synth: String,
    val hitType: org.rsmod.game.hit.HitType,
    val maxHit: Int,
    val awakenedMaxHit: Int,
) {
    Melee(
        projectile = "spotanim.vfx_leviathan_01_projectile_melee_01",
        launchSpotanim = "spotanim.vfx_leviathan_01_projectile_spotanim_melee_02",
        followSpotanim = "spotanim.vfx_leviathan_01_projectile_spotanim_melee_01",
        impactSpotanim = "spotanim.vfx_leviathan_01_projectile_impact_melee_01",
        synth = "synth.leviathan_orb_melee",
        hitType = org.rsmod.game.hit.HitType.Melee,
        maxHit = 50,
        awakenedMaxHit = 86,
    ),
    Ranged(
        projectile = "spotanim.vfx_leviathan_01_projectile_ranged_01",
        launchSpotanim = "spotanim.vfx_leviathan_01_projectile_spotanim_ranged_02",
        followSpotanim = "spotanim.vfx_leviathan_01_projectile_spotanim_ranged_01",
        impactSpotanim = "spotanim.vfx_leviathan_01_projectile_impact_ranged_01",
        synth = "synth.leviathan_orb_ranged",
        hitType = org.rsmod.game.hit.HitType.Ranged,
        maxHit = 24,
        awakenedMaxHit = 41,
    ),
    Magic(
        projectile = "spotanim.vfx_leviathan_01_projectile_magic_01",
        launchSpotanim = "spotanim.vfx_leviathan_01_projectile_spotanim_magic_02",
        followSpotanim = "spotanim.vfx_leviathan_01_projectile_spotanim_magic_01",
        impactSpotanim = "spotanim.vfx_leviathan_01_projectile_impact_magic_01",
        synth = "synth.leviathan_orb_magic",
        hitType = org.rsmod.game.hit.HitType.Magic,
        maxHit = 32,
        awakenedMaxHit = 55,
    );

    companion object {
        val DISTANCED = listOf(Ranged, Magic)
        val ALL = listOf(Melee, Ranged, Magic)
    }
}

/**
 * One rung of the volley ladder. [orbDelay] and [orbTravel] are the projectile's own flight
 * cycles (how long before it starts moving, then how long it's in the air) - they shorten a
 * little as the ladder escalates, but [interval] (the tick gap between shots) is what actually
 * drives the ramp-up.
 */
internal data class VolleyStage(
    val interval: Int,
    val shots: Int,
    val allStyles: Boolean,
    val orbDelay: Int,
    val orbTravel: Int,
)

/**
 * Per-npc fight state that doesn't fit [org.rsmod.api.bosses.runtime.BossEncounter] (volley
 * ladder progress, specials, arena hazards) - kept alongside the DSL's own encounter state the
 * same way `MuspahFight`/`TdFight` are, per [org.rsmod.content.bosses.muspah.Muspah] and
 * `TormentedDemon`.
 */
internal class LeviathanFight(val npc: Npc, val arena: Arena, val awakened: Boolean) {
    var stage: Int = 0
    var shotsFired: Int = 0
    var volleysStarted: Int = 0

    var stunned: Boolean = false
    var stunCount: Int = 0
    var stunFacing: Int = 0
    /** Bumped whenever a stun ends early (weak spot hit) to cancel the natural stun-end callback. */
    var stunToken: Int = 0

    var inSpecial: Boolean = false
    var nextSpecial: Special? = null
    var specialAngle: Int = 0

    var enraged: Boolean = false
    var tornadoesSpawned: Boolean = false
    var absentTicks: Int = 0
    var ended: Boolean = false

    val rubble: MutableMap<CoordGrid, LocInfo> = HashMap()
    var pathfinder: Npc? = null
    var pathfinderCorner: Int = 0
    val tornadoes: MutableList<Tornado> = ArrayList()

    class Tornado(val target: Player, var npc: Npc?, var home: CoordGrid, var respawnTick: Int = 0)
}
