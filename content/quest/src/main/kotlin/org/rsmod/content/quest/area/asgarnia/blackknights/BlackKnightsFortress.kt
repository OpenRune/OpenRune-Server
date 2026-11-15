package org.rsmod.content.quest.area.asgarnia.blackknights

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.table.BlackKnightsRoutesRow
import org.rsmod.content.quest.manager.*
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class BlackKnightsFortress @Inject constructor(private val scenes: BlackKnightsScene) :
    QuestScript(
        BlackKnightsProgress.QUEST,
        "varp.spy",
        rewards { item("obj.coins", 2500) },
        ItemRewardDisplay("obj.bkfortress_cauldron_broken", zoom = 400, x = 78, y = 128),
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
                    access.anim("seq.bkf_bent_to_listen_at_door_idle")
                    mesbox(
                        "You overhear the witch discussing an invincibility potion. She needs a magical Draynor Manor cabbage; any ordinary cabbage would ruin it."
                    )
                    access.resetAnim()
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
            if (!scenes.sabotage(this, it.objType.internalName))
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

    override fun questLog(player: ProtectedAccess) = questJournal(player) {
        when (p.stage(player.player)) {
            1 -> {
                line("Sir Amik Varze asked me to infiltrate the Black Knights' Fortress and destroy their secret weapon.")
                line("I need to discover what the Black Knights are planning.")
            }
            2 -> listOf(
                "<str>Sir Amik Varze asked me to investigate the Black Knights'",
                "<str>Fortress. I sneaked inside disguised as a Guard.",
                "<col=000080>I eavesdropped on a Witch and the Black Knight Captain and",
                "<col=000080>discovered that their <col=800000>invincibility potion<col=000080> can be destroyed",
                "<col=000080>with a normal <col=800000>cabbage<col=000080>.",
            ).forEach { addRawLine(it) }
            3 -> listOf(
                "<str>Sir Amik Varze asked me to investigate the Black Knights'",
                "<str>Fortress. I sneaked inside disguised as a Guard.",
                "<str>I eavesdropped on a Witch and the Black Knight Captain and",
                "<str>discovered that their invincibility potion could be destroyed",
                "<str>with a normal cabbage.",
                "<col=000080>Now that I have sabotaged the witch's potion, I can claim my",
                "<col=000080><col=800000>reward<col=000080> from <col=800000>Sir Amik Varze<col=000080> in <col=800000>Falador Castle<col=000080>.",
            ).forEach { addRawLine(it) }
        }
    }

    override fun completedLog(player: ProtectedAccess) =
        completionJournal(player) {
            line("Sir Amik Varze asked me to destroy the Black Knights' secret weapon.")
            line(
                "I infiltrated their fortress disguised as a guard and overheard the witch's plan."
            )
            line("I dropped an ordinary cabbage into her invincibility potion and ruined it.")
            line("I reported back to Sir Amik Varze and received 2,500 coins and 3 quest points.")
        }
}
