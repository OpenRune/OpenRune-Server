package org.rsmod.content.skills.agility

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.enums.DorgeshAgilityDelicateEnums.dorgesh_agility_delicate_breaks
import org.rsmod.api.enums.DorgeshAgilityDelicateEnums.dorgesh_agility_delicate_parts
import org.rsmod.api.enums.DorgeshAgilityHeavyEnums.dorgesh_agility_heavy_breaks
import org.rsmod.api.enums.DorgeshAgilityHeavyEnums.dorgesh_agility_heavy_parts
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.table.agility.AgilityCoursesRow
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

/**
 * Turgall's job on the Dorgesh-Kaan Agility Course
 * (https://oldschool.runescape.wiki/w/Dorgesh-Kaan_Agility_Course). He names one heavy and one
 * delicate part; the spanner takes the heavy one from the old generator's boiler and the delicate
 * one from its console, and only one part at a time. Heavy parts cross back on the Agility route,
 * delicate ones on the pylons, and the obstacles of the other route break them. Handing a part in
 * ends a lap: it pays the course's bonus for each way across by the route taken there and back.
 */
class DorgeshKaanParts
@Inject
constructor(private val marks: MarksOfGrace, private val pets: AgilityPets) {
    private val ItemServerType.partName: String
        get() = name.lowercase()

    fun ProtectedAccess.takePart(heavy: Boolean) {
        val part =
            if (heavy) {
                dorgesh_agility_heavy_parts[player.partHeavy]
            } else {
                dorgesh_agility_delicate_parts[player.partDelicate]
            }
        if (part == null) {
            // UNVERIFIED: wording guessed.
            mes("You don't know which part Turgall needs.")
            return
        }
        if (INTACT_PARTS.any { invTotal(inv, it.internalName) > 0 }) {
            // UNVERIFIED: wording guessed.
            mes("You should take the part you have to Turgall first.")
            return
        }
        if (invAdd(inv, part.internalName).failure) {
            mes("You don't have enough inventory space.")
            return
        }
        player.returning = true
        player.outboundRoute = player.lastRoute
        // UNVERIFIED: wording guessed.
        mes("You take a ${part.partName} from the old generator.")
    }

    suspend fun ProtectedAccess.talk(npc: Npc) {
        startDialogue(npc) {
            takeBrokenParts()
            val part = access.handInPart()
            if (part != null) {
                handIn(part)
            } else {
                greet()
            }
        }
    }

    suspend fun ProtectedAccess.useOn(npc: Npc, obj: ItemServerType) {
        when {
            BROKEN_PARTS.any { it.id == obj.id } -> startDialogue(npc) { takeBrokenParts() }
            obj.id == handInPart()?.id -> talk(npc)
            INTACT_PARTS.any { it.id == obj.id } && request(player) != null ->
                startDialogue(npc) {
                    chatNpc(
                        neutral,
                        "Thanks, but I've got plenty of those. At the moment I need " +
                            "${request(player)}.",
                    )
                }
            else -> mes("Nothing interesting happens.")
        }
    }

    private fun ProtectedAccess.handInPart(): ItemServerType? {
        if (!player.returning) {
            return null
        }
        val requested =
            listOfNotNull(
                dorgesh_agility_heavy_parts[player.partHeavy],
                dorgesh_agility_delicate_parts[player.partDelicate],
            )
        return requested.firstOrNull { invTotal(inv, it.internalName) > 0 }
    }

    private fun ProtectedAccess.deliver(part: ItemServerType): Boolean {
        if (!player.returning || invDel(inv, part.internalName).failure) {
            return false
        }
        player.returning = false
        val course = AgilityCoursesRow.getRow(COURSE)
        val varp = RSCM.getReverseMapping(RSCMType.VARP, course.lapCount)
        val laps = player.vars[varp] + 1
        VarPlayerIntMapSetter.set(player, varp, laps)
        player.helperHighlightedCourse = 0
        marks.rollLap(this, course)
        LapCounter.announce(this, course, laps)
        for (route in listOf(player.outboundRoute, routeOf(part))) {
            val (stat, xp) = course.legBonusXp[route]
            statAdvance(RSCM.getReverseMapping(RSCMType.STAT, stat.id), xp / 10.0)
        }
        assignParts()
        pets.roll(this, course)
        return true
    }

    private fun ProtectedAccess.assignParts() {
        player.partHeavy = random.pick(dorgesh_agility_heavy_parts.keys)
        player.partDelicate = random.pick(dorgesh_agility_delicate_parts.keys)
    }

    private suspend fun Dialogue.takeBrokenParts() {
        val carried = BROKEN_PARTS.sumOf { invTotal(access.inv, it.internalName) }
        if (carried == 0) {
            return
        }
        if (carried == 1) {
            chatNpc(neutral, "Oh, look at that broken part. Let me take it off you.")
        } else {
            chatNpc(neutral, "Oh, look at those broken parts. Let me take them off you.")
        }
        for (part in BROKEN_PARTS) {
            val count = invTotal(access.inv, part.internalName)
            if (count > 0) {
                access.invDel(access.inv, part.internalName, count)
            }
        }
    }

    // UNVERIFIED: every chathead anim, and which line answers a lap out one way and back the other.
    private suspend fun Dialogue.handIn(part: ItemServerType) {
        chatPlayer(happy, "I've got the ${part.partName}!")
        chatNpc(quiz, "Oh? How did you get across the cavern?")
        if (routeOf(part) == GRAPPLE_ROUTE) {
            chatPlayer(
                neutral,
                "I got there and back by swinging across the cavern with a crossbow.",
            )
        } else {
            chatPlayer(neutral, "I got there and back by balancing on cables and things.")
        }
        chatNpc(happy, "Oh, well, I'll give you a few tips about how to do that more easily.")
        if (!access.deliver(part)) {
            return
        }
        when (
            choice4(
                "Do you need any more help?",
                1,
                "What is this place?",
                2,
                "Why are there cables all over the cavern?",
                3,
                "Goodbye.",
                4,
            )
        ) {
            1 -> moreHelp()
            2 -> whatIsThisPlace()
            3 -> cables()
            4 -> goodbye()
        }
    }

    private suspend fun Dialogue.greet() {
        chatNpc(quiz, "Oh! Hello, surface-dweller. Do you think you could help me?")
        when (
            choice4(
                "What help?",
                1,
                "What is this place?",
                2,
                "Why are there cables all over the cavern?",
                3,
                "Goodbye.",
                4,
            )
        ) {
            1 -> whatHelp()
            2 -> whatIsThisPlace()
            3 -> cables()
            4 -> goodbye()
        }
    }

    private suspend fun Dialogue.whatHelp() {
        chatPlayer(quiz, "What help?")
        chatNpc(
            neutral,
            "This power station keeps breaking. I can fix it easily with parts from the old " +
                "power generator on the other side of the cavern, but that's not easy to get to.",
        )
        chatNpc(quiz, "Do you think you could get something from there for me?")
        when (choice2("Okay", 1, "Get it yourself!", 2)) {
            1 -> {
                chatPlayer(happy, "Okay!")
                acceptJob()
            }
            2 -> {
                chatPlayer(angry, "Get it youself!")
                chatNpc(sad, "Alright, I'm sure someone else will help me.")
            }
        }
    }

    // UNVERIFIED: when and how the spanner is handed over.
    private suspend fun Dialogue.acceptJob() {
        if (player.partHeavy == 0 || player.partDelicate == 0) {
            access.assignParts()
        }
        if (invTotal(access.inv, SPANNER) == 0 && access.invAdd(access.inv, SPANNER).failure) {
            access.mes("You don't have enough inventory space.")
        }
        chatNpc(
            happy,
            "Excellent! Now, at the moment, I need ${request(player)}. If you could get either " +
                "one I'd appreciate it.",
        )
    }

    private suspend fun Dialogue.moreHelp() {
        chatPlayer(quiz, "Do you need any more help?")
        chatNpc(neutral, "I always need more help. This power station is always breaking.")
        chatNpc(
            neutral,
            "At the moment, I need ${request(player)}. If you could get either one I'd " +
                "appreciate it.",
        )
    }

    private suspend fun Dialogue.whatIsThisPlace() {
        chatPlayer(quiz, "What is this place?")
        chatNpc(
            neutral,
            "Oh, this is, um, the power station. It converts heat from the lava into magical " +
                "energy, which powers the lights in the city.",
        )
        when (
            choice4(
                "How does it work?",
                1,
                "Why is it outside the city?",
                2,
                "Why are there cables all over the cavern?",
                3,
                "Goodbye.",
                4,
            )
        ) {
            1 -> howDoesItWork()
            2 -> outsideTheCity()
            3 -> cables()
            4 -> goodbye()
        }
    }

    private suspend fun Dialogue.howDoesItWork() {
        chatPlayer(quiz, "How does it work?")
        chatNpc(happy, "By magic!")
        chatNpc(
            neutral,
            "The lava heats water into steam, which drives a turbine around. The turbine " +
                "contains magical stones.",
        )
        chatNpc(
            neutral,
            "As the magical stones rotate they generate a magical current in the conductor " +
                "cables, and that powers the light!",
        )
        when (
            choice3(
                "Why is it outside the city?",
                1,
                "Why are there cables all over the cavern?",
                2,
                "Goodbye.",
                3,
            )
        ) {
            1 -> outsideTheCity()
            2 -> cables()
            3 -> goodbye()
        }
    }

    private suspend fun Dialogue.outsideTheCity() {
        chatPlayer(quiz, "Why is it outside the city?")
        chatNpc(neutral, "We're hardly going to let flowing lava inside the city, are we?")
        chatNpc(
            neutral,
            "But don't worry, it's quite safe. The creatures on the cavern floor can't get up " +
                "here.",
        )
        when (
            choice3(
                "How does it work?",
                1,
                "Why are there cables all over the cavern?",
                2,
                "Goodbye.",
                3,
            )
        ) {
            1 -> howDoesItWork()
            2 -> cables()
            3 -> goodbye()
        }
    }

    private suspend fun Dialogue.cables() {
        chatPlayer(quiz, "Why are there cables all over the cavern?")
        chatNpc(neutral, "Oh...they deliver power from the old power station.")
        chatNpc(
            neutral,
            "There was an accident with that power station and it stopped working, so we set " +
                "this one up instead.",
        )
        when (choice2("What is this place?", 1, "Goodbye.", 2)) {
            1 -> whatIsThisPlace()
            2 -> goodbye()
        }
    }

    private suspend fun Dialogue.goodbye() {
        chatPlayer(neutral, "Goodbye.")
        chatNpc(neutral, "Oh, er, bye.")
    }

    private fun request(player: Player): String? {
        val heavy = dorgesh_agility_heavy_parts[player.partHeavy] ?: return null
        val delicate = dorgesh_agility_delicate_parts[player.partDelicate] ?: return null
        return "a ${heavy.partName} and a ${delicate.partName}"
    }

    private fun routeOf(part: ItemServerType): Int =
        if (HEAVY_PARTS.any { it.id == part.id }) AGILITY_ROUTE else GRAPPLE_ROUTE

    private companion object {
        const val COURSE = "dbrow.agility_course_dorgesh_kaan"
        const val SPANNER = "obj.dorgesh_spanner"
        const val AGILITY_ROUTE = 0
        const val GRAPPLE_ROUTE = 1

        val HEAVY_PARTS by lazy { dorgesh_agility_heavy_parts.values.filterNotNull() }
        val INTACT_PARTS by lazy {
            HEAVY_PARTS + dorgesh_agility_delicate_parts.values.filterNotNull()
        }
        val BROKEN_PARTS by lazy {
            (dorgesh_agility_heavy_breaks.values + dorgesh_agility_delicate_breaks.values)
                .filterNotNull()
        }

        var Player.partHeavy by intVarBit("varbit.dorgesh_agility_part_heavy")
        var Player.partDelicate by intVarBit("varbit.dorgesh_agility_part_delicate")
        var Player.returning by boolVarBit("varbit.dorgesh_agility_course_direction")
        var Player.lastRoute by intVarBit("varbit.dorgesh_last_route")
        var Player.outboundRoute by intVarBit("varbit.dorgesh_agility_outbound_route")
        var Player.helperHighlightedCourse by intVarBit("varbit.helper_agility_highlighted_course")
    }
}
