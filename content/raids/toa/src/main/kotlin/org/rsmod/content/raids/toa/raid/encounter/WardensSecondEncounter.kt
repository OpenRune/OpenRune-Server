package org.rsmod.content.raids.toa.raid.encounter

import org.rsmod.api.instances.InstanceSession
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.content.raids.toa.raid.encounter.wardens.WardenGod
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid

class WardensSecondEncounter(
    raid: ToaRaid,
    room: ToaRoom,
    session: InstanceSession,
    controllerId: Int,
) : ToaEncounter(raid, room, session, controllerId) {

    private var warden: Npc? = null
    private var phantom: Npc? = null
    private var hudScheduled = false

    private val hud = ToaHpBar(this, subject = { warden })

    override val combatantSpecs: List<ToaCombatant.Spec>
        get() = WardenGod.entries.map { ToaCombatant.Spec(it.phaseThree, pathScaled = false) }

    override val hpBar: ToaHpBar
        get() = hud

    override fun arrival(): Arrival = Arrival(coords(ARRIVAL_TILE), Direction.South)

    override fun onBuilt() {
        spawnRoom()
    }

    override fun onReset() {
        spawnRoom()
    }

    override fun onEnter(player: Player) {
        val boss = warden ?: return
        if (!hudScheduled) {
            hudScheduled = true
            combatantOf(boss)?.scale(teamSize, P3_BASE_HP)
        }
        schedule(HUD_DELAY) { if (player in players) hud.open(player) }
    }

    override fun onComplete() {
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
        hudScheduled = false
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
        private const val HUD_DELAY = 3
    }
}
