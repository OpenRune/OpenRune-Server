package org.rsmod.content.quest.area.asgarnia.blackknights

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.table.BlackKnightsRoutesRow
import org.rsmod.content.quest.manager.*
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class BlackKnightsFortress :
    QuestScript(
        BlackKnightsProgress.QUEST,
        "varp.spy",
        rewards { item("obj.coins", 2500) },
        ItemRewardDisplay(BlackKnightsProgress.DOSSIER),
        questVarbit = BlackKnightsProgress.STAGE,
    ) {
    private val p
        get() = BlackKnightsProgress

    override fun ScriptContext.init() {
        check(quest.maxSteps == 4 && quest.questPoints == 3)
        onOpNpc1("npc.sir_amik_varze") {
            startDialogue(it.npc) {
                when (p.stage(player)) {
                    0 -> {
                        if (player.vars["varp.qp"] < 12) {
                            chatNpc(
                                neutral,
                                "You need at least twelve quest points before I can entrust you with this mission.",
                            )
                            return@startDialogue
                        }
                        chatNpc(
                            neutral,
                            "The Black Knights threaten Falador with a secret weapon. Infiltrate their fortress and sabotage it. Will you accept?",
                        )
                        if (startQuestPrompt(quest)) {
                            if (p.dossier(access))
                                chatNpc(
                                    neutral,
                                    "Here is your dossier. Enter the fortress as a guard, wearing an iron chainbody and bronze med helm. Report back after destroying their weapon.",
                                )
                            else
                                chatNpc(
                                    neutral,
                                    "Read your dossier, or make room for it. I can replace a lost copy.",
                                )
                        }
                    }
                    1 -> {
                        chatNpc(
                            neutral,
                            "Discover the secret weapon. Push the wall beyond the guards' entrance and follow the ladders until you can listen at the witch's grill.",
                        )
                        if (!p.owns(player, p.DOSSIER)) p.dossier(access)
                    }
                    2 ->
                        chatNpc(
                            neutral,
                            "An invincibility potion? Sabotage it. Any ordinary cabbage should ruin it; Draynor Manor cabbage would help the witch. Enter the meeting room, use its ladder and the upper secret wall to reach the hole above the cauldron.",
                        )
                    3 -> {
                        chatNpc(
                            happy,
                            "Our enemies have withdrawn their demands. Please accept 2,500 coins and my thanks.",
                        )
                        if (!p.finish(access))
                            chatNpc(
                                neutral,
                                "Please make room for the reward and talk to me again.",
                            )
                    }
                    else -> {
                        chatNpc(happy, "Thank you for protecting Falador.")
                    }
                }
            }
        }
        onOpHeld1(p.DOSSIER) {
            startDialogue {
                mesbox(
                    "Infiltrate the fortress. Discover and sabotage its secret weapon. This dossier will self-destruct in 3...2...1..."
                )
                if (p.read(access))
                    mesbox(
                        "The dossier self-destructs. I should infiltrate the Black Knights' Fortress."
                    )
                else mesbox("I need the actual dossier to read it.")
            }
        }
        for (type in
            listOf(
                "loc.dk_ladder",
                "loc.dk_laddertop",
                "loc.dk_meeting_ladder",
                "loc.dk_meeting_laddertop",
            )) onOpLoc1(type) {
            val row =
                BlackKnightsRoutesRow.all().singleOrNull { r ->
                    r.loc == type && it.loc.coords == CoordGrid(r.x, r.z, r.level)
                }
            if (row == null) {
                mes("This ladder has no accessible landing here.")
                return@onOpLoc1
            }
            if (coords.level != row.level || coords.chebyshevDistance(it.loc.coords) > 2)
                return@onOpLoc1
            arriveDelay()
            if (coords.level == row.level && coords.chebyshevDistance(it.loc.coords) <= 2)
                telejump(CoordGrid(row.destX, row.destZ, row.destLevel))
        }
        onOpLoc1("loc.witchgrill") {
            if (it.loc.coords != CoordGrid(3026, 3507)) return@onOpLoc1
            startDialogue {
                if (p.stage(player) == 1) {
                    mesbox(
                        "You overhear the witch discussing an invincibility potion. She needs a magical Draynor Manor cabbage; any ordinary cabbage would ruin it."
                    )
                    if (p.hear(access))
                        mesbox(
                            "I should find a way to drop an ordinary cabbage into the cauldron from above."
                        )
                } else
                    mesbox(
                        if (p.stage(player) >= 3) "The witch is furious. Her potion is ruined."
                        else
                            "I have no reason to listen here yet, or already know how to sabotage the potion."
                    )
            }
        }
        onOpLocU("loc.blackknighthole") {
            if (it.loc.coords != CoordGrid(3031, 3507, 1)) return@onOpLocU
            if (p.sabotage(this, it.objType.internalName)) {
                fadeOverlay(0, 0, 0, 255, 30)
                closeFadeOverlay()
                mes(
                    "You drop the cabbage through the hole. The potion is ruined! Return to Sir Amik Varze in Falador."
                )
            } else
                mes(
                    if (it.objType.internalName != "obj.cabbage")
                        "That is the wrong sort of cabbage or item. I must not help the witch."
                    else
                        "I need to overhear the witch's plan and reach the hole above the cauldron first."
                )
        }
    }

    override fun subTitle() =
        "talking to <col=800000>Sir Amik Varze</col> on the second floor of <col=800000>White Knights' Castle</col> in Falador, with 12 quest points."

    override fun questLog(player: ProtectedAccess) =
        questJournal(player) {
            line(
                when (p.stage(player.player)) {
                    1 ->
                        "Enter the Black Knights' Fortress in an iron chainbody and bronze med helm. Push the wall, follow the ladder route and listen at the witch's grill."
                    2 ->
                        "An ordinary cabbage will spoil the potion. Return to the entrance, accept the meeting-room guard's warning, climb the dining ladder, push the upper secret wall and use the cabbage on the hole. Draynor Manor cabbage will not work."
                    3 ->
                        "I have sabotaged the potion. Return to Sir Amik Varze in White Knights' Castle for the reward."
                    else ->
                        "Sir Amik Varze has a dangerous mission. I need 12 quest points and room for his dossier."
                }
            )
        }

    override fun completedLog(player: ProtectedAccess) =
        completionJournal(player) {
            line("I infiltrated the Black Knights' Fortress and ruined their invincibility potion.")
        }
}
