package org.rsmod.content.quest.area.alkharid.princealirescue.npcs

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.KEY
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.KEY_PRINT
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.NPC_KELI
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.ROPE
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.SHIELD_OF_ARRAV
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.SOFT_CLAY
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.STAGE_BRIEFED
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.STAGE_JOE_DRUNK
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.STAGE_KELI_TIED
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.quest.manager.menu
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Lady Keli, leader of the bandits holding the Prince. Flattery and an interest in joining her
 * gang get her to show off the cell key, which soft clay can take an imprint of. Once Joe is
 * drunk and the player has the key and disguise, a rope sees her tied up in a cupboard; the
 * `varp.princequest` multinpc then hides her.
 */
class LadyKeli @Inject constructor(private val princeAli: PrinceAliRescueQuest) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpNpc1(NPC_KELI) { talk(it.npc) }
        onOpNpcU(NPC_KELI) {
            if (it.objType.id == ROPE.asRSCM(RSCMType.OBJ)) {
                useRope()
            } else {
                mes("Nothing interesting happens.")
            }
        }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) {
        val stage = princeAli.stage(player)
        if (stage == STAGE_JOE_DRUNK && readyToTie()) {
            startDialogue(npc) {
                chatPlayer(angry, "Hello! I'm here to tie you up!")
                chatNpc(shocked, "What?")
            }
            tieUp()
            return
        }
        startDialogue(npc) {
            if (stage in STAGE_BRIEFED until STAGE_KELI_TIED) {
                keli()
            } else {
                chatNpc(angry, "What do you want?")
                chatPlayer(confused, "Nothing?")
                chatNpc(angry, "Clear off then.")
            }
        }
    }

    private suspend fun ProtectedAccess.useRope() {
        if (princeAli.stage(player) != STAGE_JOE_DRUNK || !readyToTie()) {
            mesbox("You cannot tie Keli up until you have all equipment and disabled the guard!")
            return
        }
        tieUp()
    }

    private fun ProtectedAccess.readyToTie(): Boolean =
        ROPE in player.inv && princeAli.hasKey(player) && princeAli.hasDisguise(player)

    private suspend fun ProtectedAccess.tieUp() {
        if (invDel(inv, ROPE).failure) {
            return
        }
        princeAli.setStage(this, STAGE_KELI_TIED)
        mesbox("You overpower Keli, tie her up, and put her in a cupboard.")
    }

    private suspend fun Dialogue.keli() {
        if (princeAli.keliAsked(player)) {
            chatPlayer(happy, "Hello again!")
            chatNpc(neutral, "Oh, it's you. What do you want?")
            chatPlayer(neutral, "I was hoping we could talk more about your prisoner.")
            chatNpc(
                neutral,
                "Until you join us, I can say little. What I can tell you is that he is very " +
                    "valuable. If all goes well, he will make us very rich.",
            )
            prisoner()
            return
        }
        chatPlayer(quiz, "Are you the famous Lady Keli? Leader of the toughest gang of bandits around?")
        chatNpc(neutral, "Yes, I am Keli. You've heard of me then?")
        val katrine = QuestRequirements.hasCompleted(player, SHIELD_OF_ARRAV)
        val opening =
            menu(
                buildList {
                    add("Heard of you? You're famous in Gielinor!" to Opening.Famous)
                    if (katrine) add("I've heard a little, but I think Katrine is tougher." to Opening.Katrine)
                    add("I've heard rumours that you kill people." to Opening.Rumours)
                    add("No, I've never really heard of you." to Opening.NeverHeard)
                },
            )
        when (opening) {
            Opening.Famous -> {
                chatPlayer(happy, "Heard of you? You're famous in Gielinor!")
                famous(katrine)
            }
            Opening.Katrine -> {
                chatPlayer(neutral, "I've heard a little, but I think Katrine is tougher.")
                katrineIsTougher()
            }
            Opening.Rumours -> {
                chatPlayer(shifty, "I've heard rumours that you kill people.")
                chatNpc(
                    neutral,
                    "There's always someone ready to spread rumours. I hear all sort of ridiculous " +
                        "things these days.",
                )
                smallTalk()
            }
            Opening.NeverHeard -> {
                chatPlayer(neutral, "No, I've never really heard of you.")
                chatNpc(
                    angry,
                    "You must be new around here then. Everyone knows of Lady Keli and her prowess " +
                        "with a sword.",
                )
                neverHeard()
            }
        }
    }

    private suspend fun Dialogue.famous(katrine: Boolean) {
        chatNpc(
            happy,
            "That's very kind of you to say. Reputations are not easily earned. I have managed to " +
                "succeed where many fail.",
        )
        val choice =
            menu(
                buildList {
                    if (katrine) add("I think Katrine is still tougher." to Talk.KATRINE)
                    add("What's your latest plan then?" to Talk.PLAN)
                    add("You must have trained a lot for this work." to Talk.TRAINED)
                    add("I shouldn't disturb someone as tough as you." to Talk.LEAVE)
                },
            )
        when (choice) {
            Talk.KATRINE -> {
                chatPlayer(neutral, "I think Katrine is still tougher.")
                katrineIsTougher()
            }
            Talk.PLAN -> plan()
            Talk.TRAINED -> trained()
            Talk.LEAVE -> leave()
        }
    }

    private suspend fun Dialogue.katrineIsTougher() {
        chatNpc(
            angry,
            "Well you can think that all you like! Those cowards dare not leave Varrock. Out here, " +
                "I'm toughest. You can tell them that! Now get out of my sight, before I call my " +
                "guards.",
        )
    }

    private suspend fun Dialogue.smallTalk() {
        when (
            choice3(
                "What's your latest plan then?", Talk.PLAN,
                "You must have trained a lot for this work.", Talk.TRAINED,
                "I shouldn't disturb someone as tough as you.", Talk.LEAVE,
            )
        ) {
            Talk.PLAN -> plan()
            Talk.TRAINED -> trained()
            else -> leave()
        }
    }

    private suspend fun Dialogue.neverHeard() {
        when (
            choice4(
                "No, still doesn't ring a bell.", 1,
                "Actually, I have heard of you. You're famous in Gielinor!", 2,
                "You must have trained a lot for this work.", 3,
                "I shouldn't disturb someone as tough as you.", 4,
            )
        ) {
            1 -> {
                chatPlayer(neutral, "No, still doesn't ring a bell.")
                chatNpc(
                    angry,
                    "Well, you know of me now. You should also know that I will wring your neck if " +
                        "you don't show some respect.",
                )
                when (
                    choice4(
                        "I don't show respect to killers and hoodlums.", 1,
                        "What's your latest plan then?", 2,
                        "You must have trained a lot for this work.", 3,
                        "I shouldn't disturb someone as tough as you.", 4,
                    )
                ) {
                    1 -> {
                        chatPlayer(angry, "I don't show respect to killers and hoodlums.")
                        chatNpc(
                            angry,
                            "You should, you really should. I am wealthy enough to place a bounty " +
                                "on your head, or I could just remove your head myself. Luckily, I " +
                                "am too busy to deal with the likes of you, so clear off!",
                        )
                    }
                    2 -> plan()
                    3 -> trained()
                    else -> leave()
                }
            }
            2 -> {
                chatPlayer(happy, "Actually, I have heard of you. You're famous in Gielinor!")
                famous(QuestRequirements.hasCompleted(player, SHIELD_OF_ARRAV))
            }
            3 -> trained()
            else -> leave()
        }
    }

    private suspend fun Dialogue.trained() {
        chatPlayer(neutral, "You must have trained a lot for this work.")
        chatNpc(
            happy,
            "I have used a sword since I was a girl. My first kill was before I was even six years old.",
        )
        when (
            choice2(
                "What's your latest plan then?", Talk.PLAN,
                "I shouldn't disturb someone as tough as you.", Talk.LEAVE,
            )
        ) {
            Talk.PLAN -> plan()
            else -> leave()
        }
    }

    private suspend fun Dialogue.leave() {
        chatPlayer(neutral, "I shouldn't disturb someone as tough as you.")
        chatNpc(neutral, "Yes, I am very busy. Goodbye.")
    }

    private suspend fun Dialogue.plan() {
        chatPlayer(quiz, "What's your latest plan then?")
        chatNpc(shifty, "Why do you want to know?")
        chatPlayer(neutral, "Well I was actually hoping to join your group.")
        chatNpc(neutral, "Join us? Interesting... I suppose you do look the type.")
        princeAli.setKeliAsked(player)
        chatNpc(
            neutral,
            "You'll of course need to properly prove yourself before we let you join us. However, " +
                "what I can tell you is that we currently have a very valuable prisoner. If all " +
                "goes well, he will make us very rich.",
        )
        prisoner()
    }

    private suspend fun Dialogue.prisoner() {
        when (
            choice3(
                "Ah, I see. You must have been very skillful.", 1,
                "How do you know someone won't try to free him?", 2,
                "Well good luck with it.", 3,
            )
        ) {
            1 -> {
                chatPlayer(happy, "Ah, I see. You must have been very skillful.")
                chatNpc(
                    happy,
                    "To catch him? Oh yes. We had to grab him without his bodyguards noticing. It " +
                        "was a stroke of genius. Mostly my doing, of course.",
                )
                when (
                    choice2(
                        "How do you know someone won't try to free him?", 1,
                        "Well good luck with it.", 2,
                    )
                ) {
                    1 -> freeHim()
                    else -> chatPlayer(neutral, "Well good luck with it.")
                }
            }
            2 -> freeHim()
            else -> chatPlayer(neutral, "Well good luck with it.")
        }
    }

    private suspend fun Dialogue.freeHim() {
        chatPlayer(quiz, "How do you know someone won't try to free him?")
        chatNpc(
            neutral,
            "There is no way to release him. The only key to his cell is on a chain around my neck, " +
                "and the locksmith who made it died very suddenly.",
        )
        chatNpc(neutral, "There isn't another key like this in the world.")
        while (true) {
            when (
                choice3(
                    "Could I see the key please?", 1,
                    "That is a good way to keep secrets.", 2,
                    "Well I'll be off. Good luck.", 3,
                )
            ) {
                1 -> {
                    seeKey()
                    return
                }
                2 -> {
                    chatPlayer(neutral, "That is a good way to keep secrets.")
                    chatNpc(neutral, "It is the best way I know. Dead men tell no tales.")
                }
                else -> {
                    chatPlayer(neutral, "Well I'll be off. Good luck.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.seeKey() {
        chatPlayer(quiz, "Could I see the key please?")
        chatNpc(quiz, "Why?")
        chatPlayer(
            happy,
            "It will be something I can tell my grandchildren, when you are even more famous than " +
                "you are now.",
        )
        chatNpc(
            neutral,
            "Well I suppose there's no harm in letting you see it. After all, you have no hope of " +
                "stealing it.",
        )
        objbox(KEY, "Keli shows you a small key on a strong looking chain.")
        if (SOFT_CLAY !in player.inv || !princeAli.needsImprint(player)) {
            chatPlayer(happy, "Thank you. I'd better go now.")
            return
        }
        when (
            choice2(
                "Could I touch the key for a moment please?", 1,
                "Thank you. I'd better go now.", 2,
            )
        ) {
            1 -> {
                chatPlayer(quiz, "Could I touch the key for a moment please?")
                chatNpc(neutral, "Well... only for a moment then.")
                if (access.invDel(access.inv, SOFT_CLAY).failure) {
                    return
                }
                access.invAdd(access.inv, KEY_PRINT)
                objbox(
                    KEY_PRINT,
                    "As you touch the key, you take an imprint of it using your soft clay.",
                )
                chatPlayer(happy, "Thank you so much! You are too kind.")
                chatNpc(
                    neutral,
                    "You are welcome, but run along now. I will need some time to consider your " +
                        "request to join us.",
                )
            }
            else -> chatPlayer(happy, "Thank you. I'd better go now.")
        }
    }

    private enum class Opening {
        Famous,
        Katrine,
        Rumours,
        NeverHeard,
    }

    private object Talk {
        const val KATRINE = 0
        const val PLAN = 1
        const val TRAINED = 2
        const val LEAVE = 3
    }
}
