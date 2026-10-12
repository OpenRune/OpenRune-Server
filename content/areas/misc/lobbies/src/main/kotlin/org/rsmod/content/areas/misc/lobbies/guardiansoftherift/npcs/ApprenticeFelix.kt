package org.rsmod.content.areas.misc.lobbies.guardiansoftherift.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Apprentice_Felix
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:shops] no server inv for Temple Supplies (abyssal pearl currency; the
//  stock depends on the unlocked law, death and blood altars): Trade option and "Can I see your
//  general items?"
// TODO(content): [system:guardians_of_the_rift] the dye swap and sale, lost locket and bag, and
//  selling rewards rift items, offered when the player holds them
// TODO(content): [quest:desert_treasure_ii] the greeting about Persten after the quest
class ApprenticeFelix : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.gotr_reward_trader") { startDialogue(it.npc) { felix() } }
    }

    private suspend fun Dialogue.felix() {
        chatNpc(happy, "Hey, ${player.displayName}. Got any abyssal pearls you want to trade?")
        while (true) {
            val option =
                choice5(
                    "Let's see what you have.",
                    1,
                    "What's that rift guardian doing over there?",
                    2,
                    "Why are you selling things here?",
                    3,
                    "Could you tell me more about this place.",
                    4,
                    "I'm good, thanks.",
                    5,
                )
            when (option) {
                1 -> {
                    letsSee()
                    return
                }
                2 -> riftGuardian()
                3 -> whySelling()
                4 -> aboutThisPlace()
                5 -> {
                    chatPlayer(neutral, "I'm good, thanks.")
                    chatNpc(happy, "Alright. See you later.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.letsSee() {
        chatPlayer(happy, "Let's see what you have.")
        chatNpc(
            happy,
            "I have my general store, but I also have a small collection of lanterns available, " +
                "if that interests you?",
        )
        if (!choice2("Can I see your general items?", false, "Lanterns, you say?", true)) {
            return
        }
        chatPlayer(quiz, "Lanterns, you say?")
        chatNpc(
            neutral,
            "Yes, powerful artefacts of The Abyss. We have accumulated a small collection of " +
                "them from all of the fighting.",
        )
        chatPlayer(quiz, "Do the wizards not use them in the fight against the abyss?")
        chatNpc(
            neutral,
            "Absolutely not. We know how powerful they are, yet their magic is too unknown to us " +
                "to risk using.",
        )
        chatPlayer(quiz, "Could I have one to aid in the fight?")
        chatNpc(
            neutral,
            "I'm afraid I can't just give you one. We're under strict instructions to only " +
                "provide lanterns to people who prove themselves as committed to the cause.",
        )
        chatPlayer(quiz, "How committed does one have to be?")
        chatNpc(
            neutral,
            "If you've fought the abyss enough to amass 1,500 abyssal pearls, that would be " +
                "enough.",
        )
        if (access.invTotal(player.inv, PEARLS) < LANTERN_PRICE) {
            chatPlayer(sad, "Ah, I don't have enough on me right now.")
            chatNpc(
                neutral,
                "That's alright. Feel free to come back when you have more pearls on you.",
            )
            return
        }
        chatNpc(
            neutral,
            "As long as you are aware of how powerful they are, and that I can trust you won't " +
                "lost it.",
        )
        // TODO(content): [verify:in_game] buying the abyssal lantern; the wiki transcript is
        //  missing this branch and asks whether it was removed
        choice2(
            "I understand how powerful these are, I'd like to buy one.",
            1,
            "I'm not sure I'm ready to buy one yet.",
            2,
        )
    }

    private suspend fun Dialogue.riftGuardian() {
        chatPlayer(quiz, "What's that rift guardian doing over there?")
        chatNpc(
            happy,
            "They're great, aren't they? I don't know their name, if they even have one, so I " +
                "just call them Rifty.",
        )
        chatNpc(
            happy,
            "That rift they have is a folded-space storage rift, just like the ones Traiborn " +
                "uses! Small on the outside, but big on the inside!",
        )
        chatPlayer(quiz, "But what's the rift for?")
        chatNpc(
            neutral,
            "The guardians use it to store things. If you help hold back the Abyss, they'll let " +
                "you take things from the rift in return.",
        )
        chatNpc(neutral, "Anyway, did you want to trade?")
    }

    private suspend fun Dialogue.whySelling() {
        chatPlayer(quiz, "Why are you selling things here?")
        chatNpc(
            neutral,
            "Sedridor told us to make sure there was plenty of incentive for people to help us. " +
                "He also wants us to gather abyssal pearls for research purposes. I thought " +
                "this would be a good way of doing both!",
        )
        chatPlayer(quiz, "So you sell things in exchange for abyssal pearls?")
        chatNpc(
            happy,
            "Exactly! I've got some interesting things from the Wizards' Tower, along with some " +
                "cool stuff the Guardians of the Rift gave us. Want to take a look?",
        )
    }

    private suspend fun Dialogue.aboutThisPlace() {
        chatPlayer(quiz, "Could you tell me more about this place.")
        chatNpc(
            neutral,
            "Well, I only really know what the Great Guardian told us. This is the Temple of the " +
                "Eye, or at least, what's left of it. It was built long ago to house the Eye, a " +
                "powerful Saradominist artefact.",
        )
        chatNpc(
            neutral,
            "Apparently, servants of some 'Fallen Empire' desired this artefact. However, in " +
                "their attempt to steal it, they accidently opened a rift into a deadly area of " +
                "the Abyss called the Scar.",
        )
        chatNpc(
            neutral,
            "Were it not for the Justiciar of the Lion, the rift would have endangered all of " +
                "Gielinor. She used the Eye to close it and then created the Guardians of the " +
                "Rift to protect this temple.",
        )
        chatNpc(
            neutral,
            "Of course, the rift is open again now, but with our help, the Guardians of the " +
                "Rift are keeping it under control.",
        )
        chatNpc(neutral, "Anyway, did you want to trade?")
    }

    private companion object {
        const val PEARLS = "obj.abyssal_pearl"
        const val LANTERN_PRICE = 1500
    }
}
