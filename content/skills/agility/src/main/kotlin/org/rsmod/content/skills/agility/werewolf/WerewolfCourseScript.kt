package org.rsmod.content.skills.agility.werewolf

import jakarta.inject.Inject
import org.rsmod.api.enums.AgilityWerewolfEnums.agility_werewolf_stick_shouts
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onAiTimer
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerCoordsChanged
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftQueueWithArgs
import org.rsmod.content.skills.agility.LandSpawn
import org.rsmod.content.skills.agility.werewolf.WerewolfStick.Companion.LANDING_QUEUE
import org.rsmod.content.skills.agility.werewolf.WerewolfStick.Companion.takeSticks
import org.rsmod.content.skills.agility.werewolf.WerewolfStick.Companion.takeSticksOnLeaving
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class WerewolfCourseScript
@Inject
constructor(private val stick: WerewolfStick, private val random: GameRandom) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerSoftQueueWithArgs<LandSpawn>(LANDING_QUEUE) { stick.land(player, args) }
        onOpLoc1(EXIT_LADDER) { climbOut() }
        onPlayerCoordsChanged {
            if (lastKnownCoords.inWerewolfArea() && !player.coords.inWerewolfArea()) {
                takeSticksOnLeaving(player)
            }
        }
        onPlayerLogout { takeSticks(player) }
        val shouts = agility_werewolf_stick_shouts.values.filterNotNull()
        onAiTimer(STICK_TRAINER) { npc.say(random.pick(shouts)) }
    }

    // UNVERIFIED: the tile at the top, taken from the trapdoor the werewolf lets players down.
    private suspend fun ProtectedAccess.climbOut() {
        arriveDelay()
        anim(CLIMB)
        delay(1)
        telejump(SURFACE)
    }

    private companion object {
        const val EXIT_LADDER = "loc.waa_ladder"
        const val STICK_TRAINER = "npc.werewolf_trainer_stick"
        const val CLIMB = "seq.human_reachforladder"
        val SURFACE = CoordGrid(3543, 3463, 0)
    }
}

private const val AREA_REGION_X = 55
private const val AREA_REGION_Z = 154

internal fun CoordGrid.inWerewolfArea(): Boolean =
    level == 0 && x shr 6 == AREA_REGION_X && z shr 6 == AREA_REGION_Z
