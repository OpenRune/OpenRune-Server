package org.rsmod.content.raids.toa.raid.encounter

import org.rsmod.api.instances.InstanceSession
import org.rsmod.content.raids.toa.party.ToaInvocationKey
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.content.raids.toa.raid.encounter.wardens.WardenGod
import org.rsmod.content.raids.toa.raid.encounter.wardens.WardensSlams
import org.rsmod.game.entity.Npc
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

    private var warden: Npc? = null
    private var phantom: Npc? = null

    internal val slams = WardensSlams(this)

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
        warden?.let(::despawn)
        phantom?.let(::despawn)
        val god = raid.wardensRestored ?: WardenGod.TUMEKEN
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
        private const val CRYSTAL_DELAY = 13
        private const val FIRST_SLAM_DELAY = 6
        private const val SLAM_RATE = 7
        private const val OVERCLOCKED_SLAM_RATE = 6
        private const val OVERCLOCKED_2_SLAM_RATE = 4
        private const val INSANITY_SLAM_RATE = 3
        private const val FAST_SLAM_RATE = 4
    }
}
