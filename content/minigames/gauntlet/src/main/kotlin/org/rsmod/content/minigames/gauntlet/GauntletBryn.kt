package org.rsmod.content.minigames.gauntlet

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.dialogue.mesanims
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.other.pets.PetFollowers
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal var Player.gauntletSpokenToBryn by boolVarBit("varbit.gauntlet_spoken_to_bryn")

@Singleton
class GauntletBryn @Inject constructor(private val followers: PetFollowers) {
    suspend fun ProtectedAccess.talk(npc: Npc) {
        startDialogue(npc) {
            chatNpc(neutral, "What are you doing down here?")
            if (player.gauntletSpokenToBryn) returningDialogue() else firstDialogue()
        }
    }

    /** Returns true when the player may enter; otherwise Bryn explains why not. */
    suspend fun ProtectedAccess.canEnter(): Boolean {
        val refusal =
            when {
                !player.gauntletSpokenToBryn ->
                    "Don't think you want to be heading down there without knowing what you're " +
                        "getting into! Come and see me, if you really want to go down there."
                followers.hasFollower(player) ->
                    "Don't think you can be taking any plus ones down there with you."
                player.gauntletRewardAvailable ->
                    "There's something in that there chest waiting for you. Don't leave it for " +
                        "too long."
                else -> return true
            }
        chatNpcSpecific(BRYN_TITLE, BRYN_TYPE, mesanims.neutral, refusal)
        return false
    }

    suspend fun ProtectedAccess.corruptedUnlock() {
        chatNpcSpecific(
            BRYN_TITLE,
            BRYN_TYPE,
            mesanims.neutral,
            "You know, if things start getting comfortable down there, things can be a little " +
                "unstable at times.",
        )
        chatNpcSpecific(
            BRYN_TITLE,
            BRYN_TYPE,
            mesanims.happy,
            "It might be a little frightening, but it makes for a good challenge!",
        )
        chatPlayer(mesanims.happy, "Maybe I'll check it out at some point!")
        mesbox("The Corrupted Gauntlet can be accessed by right-clicking the entrance.")
    }

    private suspend fun Dialogue.firstDialogue() {
        val explain =
            choice2("What is this place?", true, "Just passing by.", false)
        if (explain) {
            chatPlayer(quiz, "What is this place?")
            explanation()
            player.gauntletSpokenToBryn = true
        } else {
            chatPlayer(neutral, "Just passing by.")
            chatNpc(neutral, "Alright... Well be careful down here. I've got my eye on you.")
        }
    }

    private suspend fun Dialogue.returningDialogue() {
        val explain =
            choice2(
                "Could you explain this place to me again?",
                true,
                "I'm thinking of giving the Gauntlet a go.",
                false,
            )
        if (explain) {
            chatPlayer(quiz, "Could you explain this place to me again?")
            explanation()
            chatNpc(happy, "You can always come back and ask me to go over things again.")
        } else {
            chatPlayer(neutral, "I'm thinking of giving the Gauntlet a go.")
            chatNpc(laugh, "You? Attempting the Gauntlet? If you say so!")
        }
    }

    private suspend fun Dialogue.explanation() {
        chatNpc(
            neutral,
            "This here is the Gauntlet, the finest creation of the Amlodd clan. It was built " +
                "years ago to help train Prifddinas' finest warriors and survivalists.",
        )
        chatPlayer(quiz, "Train?")
        chatNpc(
            neutral,
            "That's right. The Gauntlet contains a range of deadly creatures formed from " +
                "crystal. Defeating them is the key to victory, but it is no easy task.",
        )
        chatNpc(
            neutral,
            "This is because all participants start with nothing at hand beyond a few basic " +
                "tools.",
        )
        chatPlayer(quiz, "But how do people fight without their gear?")
        chatNpc(
            neutral,
            "Resources can be gathered throughout the dungeon. These can be used to create the " +
                "weapons, armour, food and potions needed to survive.",
        )
        chatNpc(
            neutral,
            "Without any armour, those crystalline creatures can be real nasty. Any armour that " +
                "you make down there will greatly reduce the damage they do to you.",
        )
        chatPlayer(
            confused,
            "Okay, I think I follow... But I thought this was all about combat, not just " +
                "gathering my own equipment.",
        )
        chatNpc(
            neutral,
            "That's right. Once you feel you're ready, or once we decide that you've spent long " +
                "enough down there, you'll have to face the fearsome Crystalline Hunllef.",
        )
        chatPlayer(quiz, "And what makes it so dangerous, if it's just for training?")
        chatNpc(
            neutral,
            "Once you go down there, there are only two ways out... You can make a break for the " +
                "exit, or if you're bested in combat we'll send someone in for you. The " +
                "Gauntlet is not a safe environment, and teleports are blocked inside! If you " +
                "die in there, any items stored at a gravestone will be lost!",
        )
        chatNpc(
            neutral,
            "Things can be a little unstable down there at times, too... Everything gets " +
                "corrupted, and it might be a little frightening, but it makes for a good " +
                "challenge. The Corrupted Gauntlet can be accessed by right-clicking the " +
                "entrance.",
        )
        chatNpc(
            neutral,
            "I think that covers it... Everything else, you'll have to work out for yourself " +
                "while you're in there.",
        )
        chatPlayer(neutral, "Okay... I think I've got it.")
    }

    private companion object {
        const val BRYN_TITLE = "Bryn"
        const val BRYN_TYPE = "npc.gauntlet_instructor"
    }
}

class GauntletBrynScript @Inject constructor(private val bryn: GauntletBryn) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.gauntlet_instructor") { with(bryn) { talk(it.npc) } }
    }
}
