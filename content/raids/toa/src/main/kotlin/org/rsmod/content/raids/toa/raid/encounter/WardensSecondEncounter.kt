package org.rsmod.content.raids.toa.raid.encounter

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.instances.InstanceSession
import org.rsmod.content.raids.toa.party.ToaInvocationKey
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.content.raids.toa.raid.encounter.wardens.WardenGod
import org.rsmod.content.raids.toa.raid.encounter.wardens.WardenSiphons
import org.rsmod.content.raids.toa.raid.encounter.wardens.WardensSlams
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid

class WardensSecondEncounter(
    raid: ToaRaid,
    room: ToaRoom,
    session: InstanceSession,
    controllerId: Int,
) : ToaBossEncounter(raid, room, session, controllerId) {

    internal var warden: Npc? = null
        private set

    private var phantom: Npc? = null

    internal val slams = WardensSlams(this)
    internal val siphons = WardenSiphons(this)

    internal var siphonDue = false
        private set

    internal val god: WardenGod
        get() = raid.wardensRestored ?: WardenGod.TUMEKEN

    override val fight =
        ToaFight(
            boss = { warden },
            combatants = WardenGod.entries.map { ToaCombatant.Spec(it.phaseThree, pathScaled = false) },
            firstAttackDelay = FIRST_SLAM_DELAY,
            attackRate = { slamRate },
        )

    internal val slamRate: Int
        get() =
            when {
                raid.isActive(ToaInvocationKey.Insanity) -> INSANITY_SLAM_RATE
                raid.isActive(ToaInvocationKey.Overclocked2) -> OVERCLOCKED_2_SLAM_RATE
                raid.isActive(ToaInvocationKey.Overclocked) -> OVERCLOCKED_SLAM_RATE
                else -> SLAM_RATE
            }

    internal val fastSlams: Boolean
        get() = slamRate <= FAST_SLAM_RATE

    override fun arrival(): Arrival = Arrival(coords(ARRIVAL_TILE), Direction.South)

    override fun onBuilt() {
        spawnRoom()
    }

    override fun onReset() {
        spawnRoom()
    }

    override fun onFightStart(boss: Npc) {
        combatantOf(boss)?.scale(teamSize, P3_BASE_HP)
    }

    override fun onFightTick(boss: Npc, targets: List<Player>) {
        if (siphonDue || siphons.active) return
        val threshold = siphonThreshold(boss) ?: return
        if (boss.hitpoints <= threshold) siphonDue = true
    }

    override fun validateAttack(player: Player, npc: Npc): NpcAttackValidateResult {
        if (npc === warden && siphons.active) return NpcAttackValidateResult.Deny(WARDEN_BUSY)
        return NpcAttackValidateResult.Pass
    }

    internal fun modifyWardenHit(npc: Npc, hit: HitBuilder) {
        if (npc !== warden || !hit.isFromPlayer) return
        if (siphons.active) {
            hit.damage = 0
            return
        }
        val threshold = siphonThreshold(npc) ?: return
        hit.damage = min(hit.damage, max(npc.hitpoints - threshold, 0))
    }

    internal fun debugSiphon(): String? {
        if (siphons.launched >= SIPHON_THRESHOLDS.size) return "All siphon sets have run."
        siphonDue = true
        return null
    }

    internal fun debugHp(percent: Int): String? {
        val boss = warden ?: return "No Warden."
        boss.hitpoints = boss.baseHitpointsLvl * percent.coerceIn(1, 100) / 100
        hpBar?.update()
        return null
    }

    internal fun siphonLaunched() {
        siphonDue = false
    }

    private fun siphonThreshold(boss: Npc): Int? {
        val fraction = SIPHON_THRESHOLDS.getOrNull(siphons.launched) ?: return null
        return floor(boss.baseHitpointsLvl * fraction).toInt()
    }

    override fun onFightComplete() {
        schedule(CRYSTAL_DELAY) {
            deps.locRepo.add(
                coords(CRYSTAL_TILE),
                CRYSTAL,
                Int.MAX_VALUE,
                LocAngle.West,
                LocShape.CentrepieceStraight,
            )
        }
    }

    private fun spawnRoom() {
        siphons.reset()
        siphonDue = false
        warden?.let(::despawn)
        phantom?.let(::despawn)
        warden =
            spawn(god.phaseThree, coords(WARDEN_TILE), Direction.North).also {
                it.ignoreCombatInteractions = true
            }
        phantom = spawn(PHANTOM, coords(PHANTOM_TILE), Direction.North)
    }

    companion object {
        const val CRYSTAL = "loc.toa_teleport_crystal_continue_wardens"
        const val PHANTOM = "npc.toa_amascut_p3"
        const val P3_BASE_HP = 880
        private val CRYSTAL_TILE = CoordGrid(3936, 5154, 1)
        private val ARRIVAL_TILE = CoordGrid(3936, 5162, 1)
        private val WARDEN_TILE = CoordGrid(3934, 5152, 1)
        private val PHANTOM_TILE = CoordGrid(3932, 5127, 1)
        private val SIPHON_THRESHOLDS = listOf(0.8, 0.6, 0.4, 0.2)
        private const val WARDEN_BUSY = "You can't attack the warden right now."
        private const val CRYSTAL_DELAY = 13
        private const val FIRST_SLAM_DELAY = 6
        private const val SLAM_RATE = 7
        private const val OVERCLOCKED_SLAM_RATE = 6
        private const val OVERCLOCKED_2_SLAM_RATE = 4
        private const val INSANITY_SLAM_RATE = 3
        private const val FAST_SLAM_RATE = 4
    }
}
