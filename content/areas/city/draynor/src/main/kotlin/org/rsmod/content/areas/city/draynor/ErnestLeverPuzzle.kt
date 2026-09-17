package org.rsmod.content.areas.city.draynor

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ErnestLeverPuzzle @Inject constructor(private val locRepo: LocRepository) : PluginScript() {

    private var ProtectedAccess.leverA by boolVarBit("varbit.ernestlever_a")
    private var ProtectedAccess.leverB by boolVarBit("varbit.ernestlever_b")
    private var ProtectedAccess.leverC by boolVarBit("varbit.ernestlever_c")
    private var ProtectedAccess.leverD by boolVarBit("varbit.ernestlever_d")
    private var ProtectedAccess.leverE by boolVarBit("varbit.ernestlever_e")
    private var ProtectedAccess.leverF by boolVarBit("varbit.ernestlever_f")

    private var ProtectedAccess.door1to2 by boolVarBit("varbit.ernestdoor_1to2")
    private var ProtectedAccess.door2to3 by boolVarBit("varbit.ernestdoor_2to3")
    private var ProtectedAccess.door2to5 by boolVarBit("varbit.ernestdoor_2to5")
    private var ProtectedAccess.door3to6 by boolVarBit("varbit.ernestdoor_3to6")
    private var ProtectedAccess.door4to5 by boolVarBit("varbit.ernestdoor_4to5")
    private var ProtectedAccess.door4to7 by boolVarBit("varbit.ernestdoor_4to7")
    private var ProtectedAccess.door5to6 by boolVarBit("varbit.ernestdoor_5to6")
    private var ProtectedAccess.door5to8 by boolVarBit("varbit.ernestdoor_5to8")
    private var ProtectedAccess.door8to9 by boolVarBit("varbit.ernestdoor_8to9")

    override fun ScriptContext.startup() {
        for (lever in Lever.entries) {
            onOpLoc1(lever.upLoc) { pull(lever) }
            onOpLoc1(lever.downLoc) { pull(lever) }
            onOpLoc2(lever.upLoc) { mes("The lever is up.") }
            onOpLoc2(lever.downLoc) { mes("The lever is down.") }
        }

        onOpLoc1(AjarDoor) { passDoor(it.loc) }
    }

    private fun ProtectedAccess.isDown(lever: Lever): Boolean =
        when (lever) {
            Lever.A -> leverA
            Lever.B -> leverB
            Lever.C -> leverC
            Lever.D -> leverD
            Lever.E -> leverE
            Lever.F -> leverF
        }

    private fun ProtectedAccess.setDown(lever: Lever, down: Boolean) {
        when (lever) {
            Lever.A -> leverA = down
            Lever.B -> leverB = down
            Lever.C -> leverC = down
            Lever.D -> leverD = down
            Lever.E -> leverE = down
            Lever.F -> leverF = down
        }
    }

    private suspend fun ProtectedAccess.pull(lever: Lever) {
        arriveDelay()

        val nowDown = !isDown(lever)
        setDown(lever, nowDown)

        anim(if (nowDown) PullDownAnim else PullUpAnim)
        soundSynth(LeverSound)
        soundSynth(ClunkSound, delay = ClunkSoundDelay)
        mes("You pull lever ${lever.label} ${if (nowDown) "down" else "up"}.")
        mes("You hear a clunk.")

        applyDoors()
    }

    private suspend fun ProtectedAccess.passDoor(door: BoundLocInfo) {
        soundSynth(DoorSound, delay = DoorSoundDelay)

        val route = crossingRoute(door)
        val openTicks = crossingOpenTicks(crossingTiles(route))

        val openAngle = LocAngle[(door.angle.id + 1) and 0x3]
        locRepo.del(door, openTicks)
        locRepo.add(door.coords, OpenDoorLoc, openTicks, openAngle, LocShape.WallStraight)

        crossDoorway(route)
    }

    private fun ProtectedAccess.applyDoors() {
        val down = LeverState(leverA, leverB, leverC, leverD, leverE, leverF)
        door1to2 = Doors.oneToTwo(down)
        door2to3 = Doors.twoToThree(down)
        door2to5 = Doors.twoToFive(down)
        door3to6 = Doors.threeToSix(down)
        door4to5 = Doors.fourToFive(down)
        door4to7 = Doors.fourToSeven(down)
        door5to6 = Doors.fiveToSix(down)
        door5to8 = Doors.fiveToEight(down)
        door8to9 = Doors.eightToNine(down)
    }

    internal data class LeverState(
        val a: Boolean,
        val b: Boolean,
        val c: Boolean,
        val d: Boolean,
        val e: Boolean,
        val f: Boolean,
    )

    private enum class Lever(val label: String, val upLoc: String, val downLoc: String) {
        A("A", "loc.levera_up", "loc.levera_down"),
        B("B", "loc.leverb_up", "loc.leverb_down"),
        C("C", "loc.leverc_up", "loc.leverc_down"),
        D("D", "loc.leverd_up", "loc.leverd_down"),
        E("E", "loc.levere_up", "loc.levere_down"),
        F("F", "loc.leverf_up", "loc.leverf_down"),
    }

    internal object Doors {
        fun oneToTwo(s: LeverState) = s.d && s.e && s.f

        fun twoToThree(s: LeverState) = s.d && s.f

        fun twoToFive(s: LeverState) = s.d && s.f && !s.e

        fun threeToSix(s: LeverState) = s.d && !s.b && !s.f

        fun fourToFive(s: LeverState) = s.a && !s.c

        fun fourToSeven(s: LeverState) = s.a && s.b && !s.c

        fun fiveToSix(s: LeverState) = s.d && !s.e && !s.f

        fun fiveToEight(s: LeverState) = s.d && !s.e

        fun eightToNine(s: LeverState) = s.c && s.d && s.f
    }

    private companion object {
        private const val AjarDoor = "loc.ernest_doorajar"

        private const val PullDownAnim = "seq.macro_lever_switch_down"
        private const val PullUpAnim = "seq.macro_lever_switch_up"
        private const val LeverSound = "synth.lever"
        private const val ClunkSound = "synth.ernest_clunk"
        private const val ClunkSoundDelay = 5
        private const val DoorSound = "synth.iron_door_open"
        private const val DoorSoundDelay = 15

        private const val OpenDoorLoc = "loc.inactiveprisondoor_l"
    }
}
