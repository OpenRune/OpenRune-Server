package org.rsmod.content.areas.misc.blackknights

import jakarta.inject.Inject
import org.rsmod.api.npc.aggression.AggressionTolerance
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The guard on the Black Knights' Fortress front gate, and the gate itself. */
class FortressGuard
@Inject
constructor(private val locs: LocRepository, private val tolerance: AggressionTolerance) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (knight in KNIGHTS) tolerance.enroll(knight)
        onOpLoc1("loc.bkfortressdoor1") {
            if (it.loc.coords != ENTRANCE) return@onOpLoc1
            if (coords.z < 3515 && !disguised(player)) {
                mes(
                    "You need an iron chainbody and bronze med helm, or full black knight armour, to enter as a guard."
                )
                return@onOpLoc1
            }
            open(it.loc)
        }
        onOpLoc1("loc.bkfortressdoor2") {
            if (it.loc.coords != MEETING) return@onOpLoc1
            if (coords.x >= 3020) open(it.loc)
            else
                startDialogue {
                    mesbox(
                        "The guard warns you that the Black Knights are in a meeting and will kill intruders."
                    )
                    if (
                        choice2("Okay, I won't.", false, "I don't care. I'm going in anyway.", true)
                    )
                        access.open(it.loc)
                }
        }
        onOpLoc1("loc.bkfortressdoor3") {
            if (it.loc.coords == CoordGrid(3025, 3511, 1)) open(it.loc)
        }
        onOpLoc1("loc.bksecretdoor") {
            if (it.loc.coords in listOf(CoordGrid(3016, 3517), CoordGrid(3030, 3510, 1))) {
                open(it.loc)
                mes("You push against the wall and find a secret passage.")
            }
        }
        onOpLoc1("loc.wild_door") {
            if (it.loc.x in 3008..3033 && it.loc.z in 3500..3520) open(it.loc)
        }
        for (guard in GUARDS) {
            onOpNpc1(guard) { startDialogue(it.npc) { turnedAway() } }
        }
        onOpLoc1("loc.inaclefayeunopenabledoorr") { mes("You can't open this door.") }
    }

    private fun ProtectedAccess.open(door: BoundLocInfo) {
        if (coords.level == door.level && coords.chebyshevDistance(door.coords) <= 1)
            locs.del(door, duration = 500)
    }

    private suspend fun Dialogue.turnedAway() {
        if (disguised(player)) {
            chatNpc(neutral, "Get back on duty!")
            return
        }
        chatNpc(angry, "Get lost. This is private property.")
        chatPlayer(angry, "Yes, but I work here!")
        chatNpc(
            angry,
            "I might be new here but I can tell that's not true! You're not even wearing proper " +
                "uniform!",
        )
        if (choice2("Oh pleeeaaase let me in!", true, "So what is this uniform?", false)) {
            chatPlayer(sad, "Oh pleeeaaase let me in!")
            chatNpc(angry, "Go away. You're getting annoying.")
            return
        }
        chatPlayer(quiz, "So what is this uniform?")
        chatNpc(
            neutral,
            "Well you can see me wearing it. It's an iron chainbody and a medium bronze helm.",
        )
        chatPlayer(
            neutral,
            "Hmmm... I wonder if I can make that or get some in the local towns....",
        )
        chatNpc(quiz, "What was that you muttered?")
        chatPlayer(happy, "Oh, nothing important!")
    }

    companion object {
        val ENTRANCE = CoordGrid(3016, 3514)
        val MEETING = CoordGrid(3020, 3515)
        val KNIGHTS =
            listOf(
                "npc.aggressive_black_knight",
                "npc.aggressive_black_knight_f",
                "npc.kr_aggressive_black_knight",
            )

        fun disguised(p: Player) =
            ("obj.iron_chainbody" in p.worn && "obj.bronze_med_helm" in p.worn) ||
                ("obj.black_full_helm" in p.worn &&
                    "obj.black_platebody" in p.worn &&
                    ("obj.black_platelegs" in p.worn || "obj.black_plateskirt" in p.worn))

        private val GUARDS =
            listOf(
                "npc.fortressguard_01",
                "npc.fortressguard_02",
                "npc.fortressguard_03",
                "npc.fortressguard_04",
            )
    }
}
