package org.rsmod.content.areas.city.lumbridge.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.quest.manager.menu
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The adventurer camp in Lumbridge Swamp. Lost City itself isn't implemented yet. */
class LostCityAdventurers : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.warrioradventurerpg") { startDialogue(it.npc) { warrior() } }
        onOpNpc1("npc.archeradventurerpg") { startDialogue(it.npc) { archer() } }
        onOpNpc1("npc.wizardadventuterpg") { startDialogue(it.npc) { wizard() } }
        onOpNpc1("npc.monkadventurerpg") { startDialogue(it.npc) { monk() } }
    }

    private fun Dialogue.foundZanaris(): Boolean =
        QuestRequirements.hasCompleted(player, LOST_CITY)

    private suspend fun Dialogue.warrior() {
        if (foundZanaris()) {
            chatPlayer(
                happy,
                "Hey, thanks for all the information. It REALLY helped me out in finding the lost city " +
                    "of Zanaris and all.",
            )
            chatNpc(
                sad,
                "Oh please don't say that anymore! If the rest of my party knew I'd helped you they'd " +
                    "probably throw me out and make me walk home by myself!",
            )
            chatNpc(
                quiz,
                "So anyway, what have you found out? Where is the fabled Zanaris? Is it all the " +
                    "legends say it is?",
            )
            chatPlayer(happy, "You know.... I think I'll keep that to myself.")
            return
        }
        chatNpc(neutral, "Hello there traveller.")
        val camped =
            menu(
                "What are you camped out here for?" to true,
                "Do you know any good adventures I can go on?" to false,
            )
        if (camped) {
            chatPlayer(quiz, "What are you camped here for?")
            lookingForZanaris()
            return
        }
        chatPlayer(quiz, "Do you know any good adventures I can go on?")
        chatNpc(
            neutral,
            "Well we're on an adventure right now. Mind you, this is OUR adventure and we don't want " +
                "to share it - find your own!",
        )
        val beg =
            menu(
                "Please tell me." to true,
                "I don't think you've found a good adventure at all!" to false,
            )
        if (beg) return pleaseTellMe()
        chatPlayer(neutral, "I don't think you've found a good adventure at all!")
        chatNpc(
            angry,
            "Hah! Adventurers of our calibre don't just hang around in forests for fun, whelp!",
        )
        chatPlayer(quiz, "Oh really?")
        chatPlayer(quiz, "What are you camped here for?")
        lookingForZanaris()
    }

    private suspend fun Dialogue.lookingForZanaris() {
        chatNpc(
            shocked,
            "We're looking for Zanaris...GAH! I mean we're not here for any particular reason at all.",
        )
        var topic =
            menu(
                "Who's Zanaris?" to 1,
                "What's Zanaris?" to 2,
                "What makes you think it's out here?" to 3,
            )
        while (true) {
            topic =
                when (topic) {
                    1 -> return whoIsZanaris()
                    2 -> {
                        chatPlayer(quiz, "What's Zanaris?")
                        chatNpc(
                            neutral,
                            "I don't think we want other people competing with us to find it. Forget " +
                                "I said anything.",
                        )
                        if (menu("Please tell me." to true, "Oh well. Never mind." to false)) {
                            return pleaseTellMe()
                        }
                        chatPlayer(neutral, "Oh well. Never mind.")
                        return
                    }
                    3 -> {
                        chatPlayer(quiz, "What makes you think it's out here?")
                        chatNpc(
                            neutral,
                            "Don't you know of the legends that tell of the magical city, hidden in " +
                                "the swam... Uh, no, you're right, we're wasting our time here.",
                        )
                        menu(
                            "If it's hidden how are you planning to find it?" to 4,
                            "There's no such thing!" to 5,
                        )
                    }
                    4 -> return hiddenCity()
                    else -> {
                        chatPlayer(neutral, "There's no such thing!")
                        chatNpc(
                            shocked,
                            "When we've found Zanaris you'll... GAH! I mean, we're not here for any " +
                                "particular reason at all.",
                        )
                        menu(
                            "Who's Zanaris?" to 1,
                            "What's Zanaris?" to 2,
                            "What makes you think it's out here?" to 3,
                        )
                    }
                }
        }
    }

    private suspend fun Dialogue.whoIsZanaris() {
        chatPlayer(quiz, "Who's Zanaris?")
        chatNpc(
            laugh,
            "Ahahahaha! Zanaris isn't a person! It's a magical hidden city filled with treasures and " +
                "rich.. uh, nothing. It's nothing.",
        )
        hiddenCity()
    }

    private suspend fun Dialogue.hiddenCity() {
        chatPlayer(quiz, "If it's hidden how are you planning to find it?")
        chatNpc(
            neutral,
            "Well, we don't want to tell anyone else about that, because we don't want anyone else " +
                "sharing in all that glory and treasure.",
        )
        if (menu("Please tell me." to true, "Looks like you don't know either." to false)) {
            pleaseTellMe()
            return
        }
        chatPlayer(
            neutral,
            "Well, it looks to me like YOU don't know EITHER seeing as you're all just sat around here.",
        )
        chatNpc(
            angry,
            "Of course we know! We just haven't found which tree the stupid leprechaun's hiding in yet!",
        )
    }

    private suspend fun Dialogue.pleaseTellMe() {
        chatPlayer(quiz, "Please tell me?")
        chatNpc(angry, "No.")
        chatPlayer(quiz, "Please?")
        chatNpc(angry, "No!")
        chatPlayer(quiz, "PLEEEEEEEEEEEEEEEEEEEEEASE?")
        chatNpc(angry, "NO!")
    }

    private suspend fun Dialogue.archer() {
        if (foundZanaris()) {
            chatPlayer(quiz, "So you didn't find the entrance to Zanaris yet, huh?")
            chatNpc(angry, "Don't tell me a novice like YOU has found it!")
            chatPlayer(neutral, "Yep. Found it REALLY easily too.")
            chatNpc(
                confused,
                "...I cannot believe that someone like YOU could find the portal where experienced " +
                    "adventurers such as ourselves could not.",
            )
            chatPlayer(happy, "Believe what you want. Enjoy your little camp fire.")
            return
        }
        chatPlayer(quiz, "Why are you guys hanging around here?")
        chatNpc(angry, "(ahem)...'Guys'?")
        chatPlayer(worried, "Uh... yeah, sorry about that. Why are you all standing around out here?")
        chatNpc(neutral, "Well, that's really none of your business.")
    }

    private suspend fun Dialogue.wizard() {
        if (foundZanaris()) {
            chatNpc(happy, "Hahaha you're such an amateur!")
            chatNpc(happy, "Go away and play with some cabbage amateur!")
            chatPlayer(neutral, "...right.")
            return
        }
        chatPlayer(quiz, "Why are all of you standing around here?")
        chatNpc(
            laugh,
            "Hahaha you dare talk to a mighty wizard such as myself? I bet you can't even cast " +
                "windstrike yet amateur!",
        )
        chatPlayer(angry, "...You're an idiot.")
    }

    private suspend fun Dialogue.monk() {
        if (foundZanaris()) {
            chatNpc(neutral, "I already told you. I'm not talking to you anymore.")
            return
        }
        chatPlayer(quiz, "Why are all of you standing around here?")
        chatNpc(neutral, "None of your business. Get lost.")
    }

    private companion object {
        const val LOST_CITY = "quest_lostcity"
    }
}
