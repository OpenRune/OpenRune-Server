package org.rsmod.content.areas.city.falador.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class FaladorGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.falador_doric_area_man1") { startDialogue(it.npc) { cuffs() } }
        onOpNpc1("npc.falador_doric_area_man2") { startDialogue(it.npc) { narf() } }
        onOpNpc1("npc.falador_doric_area_man3") { startDialogue(it.npc) { rusty() } }
        onOpNpc1("npc.falador_doric_area_man4") { startDialogue(it.npc) { jeff() } }
        onOpNpc1("npc.dwarfrock_rolad") { startDialogue(it.npc) { rolad() } }
        onOpNpc1("npc.grillknight") { startDialogue(it.npc) { blackKnightCaptain() } }
        onOpNpc1("npc.doric") { startDialogue(it.npc) { doric() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Cuffs
    private suspend fun Dialogue.cuffs() {
        chatPlayer(happy, "Hello. Nice day for a walk, isn't it?")
        chatNpc(
            shifty,
            "A walk? Oh yes, that's what we're doing. We're just out here for a walk.",
        )
        chatPlayer(
            neutral,
            "I'm glad you're just out here for a walk. A more suspicious person would think you " +
                "were waiting here to attack weak-looking travellers.",
        )
        chatNpc(
            shifty,
            "Nope, we'd never do anything like that. Just a band of innocent walkers, that's us.",
        )
        chatPlayer(happy, "Alright, have a nice walk.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Narf
    // TODO(content): [system:treasure_trails] Crack the Clue III dialogue
    private suspend fun Dialogue.narf() {
        chatPlayer(laugh, "That's a funny name you've got.")
        chatNpc(
            angry,
            "'Narf'? You think that's funny? At least I don't call myself " +
                "${player.displayName}! Where did you get a name like that?",
        )
        chatPlayer(happy, "It seemed like a good idea at the time!")
        chatNpc(angry, "Bah!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Rusty
    // TODO(content): [system:treasure_trails] easy clue dialogue
    private suspend fun Dialogue.rusty() {
        chatNpc(happy, "Hiya. Are you carrying anything valuable?")
        chatPlayer(quiz, "Why are you asking?")
        chatNpc(
            shifty,
            "Um... It's a quiz. I'm asking everyone I meet if they're carrying anything " +
                "valuable.",
        )
        chatPlayer(quiz, "What would you do if I said I had loads of expensive items with me?")
        chatNpc(
            happy,
            "Ooh, do you? It's been ages since anyone said they'd got anything worth stealing.",
        )
        chatPlayer(shocked, "'Anything worth stealing'?")
        chatNpc(shifty, "Um... Not that I'd dream of stealing anything!")
        chatPlayer(neutral, "Well, I'll say I'm not carrying anything valuable at all.")
        chatNpc(sad, "Oh, what a shame.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jeff
    private suspend fun Dialogue.jeff() {
        chatNpc(shifty, "Tell me, is the guard still watching us?")
        chatPlayer(quiz, "Why would you care if there's a guard watching you?")
        chatNpc(neutral, "Oh, forget it.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Rolad (before Between a Rock...)
    // TODO(content): [quest:between_a_rock] "Can you leave me alone please?" once started
    private suspend fun Dialogue.rolad() {
        chatNpc(quiz, "Oh, hello... do I know you?")
        chatPlayer(
            neutral,
            "Ehm... well... my name is ${player.displayName}, if that rings any bell?",
        )
        chatNpc(neutral, "No, never heard of you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Black_Knight_Captain
    private suspend fun Dialogue.blackKnightCaptain() {
        chatPlayer(happy, "Hello.")
        if ("obj.bronze_med_helm" in player.worn && "obj.iron_chainbody" in player.worn) {
            chatNpc(
                angry,
                "That would be Hello SIR to you guard! Get out of here and go guard something!",
            )
        } else {
            chatNpc(
                angry,
                "That would be Hello SIR to you guard! And what are you doing out of uniform? " +
                    "Get out of here and get in uniform!",
            )
        }
        chatPlayer(confused, "Errr... ok...")
        chatNpc(angry, "That's Ok SIR!")
        chatPlayer(worried, "Umm... ok...Sir.")
        mesbox("You think it best not to persue that line of questioning.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Doric (before Doric's Quest)
    // TODO(content): [quest:dorics_quest] quest start, in-progress and after-quest dialogue
    private suspend fun Dialogue.doric() {
        chatNpc(happy, "Hello traveller, what brings you to my humble smithy?")
        val choice =
            choice5(
                "I wanted to use your anvils.",
                1,
                "I want to use your whetstone.",
                2,
                "Mind your own business, shortstuff!",
                3,
                "I was just checking out the landscape.",
                4,
                "What do you make here?",
                5,
            )
        when (choice) {
            1 -> {
                chatPlayer(quiz, "I wanted to use your anvils.")
                chatNpc(
                    neutral,
                    "My anvils get enough work with my own use. I make pickaxes, and it takes a " +
                        "lot of hard work. If you could get me some more materials, then I " +
                        "could let you use them.",
                )
                doricQuestOffer()
            }
            2 -> {
                chatPlayer(quiz, "I wanted to use your whetstone.")
                chatNpc(
                    neutral,
                    "The whetstone is for more advanced smithing, but I could let you use it as " +
                        "well as my anvils if you could get me some more materials.",
                )
                doricQuestOffer()
            }
            3 -> {
                chatPlayer(angry, "Mind your own business, shortstuff!")
                chatNpc(
                    angry,
                    "How nice to meet someone with such pleasant manners. Do come again when " +
                        "you need to shout at someone smaller than you!",
                )
            }
            4 -> {
                chatPlayer(neutral, "I was just checking out the landscape.")
                chatNpc(
                    happy,
                    "Hope you like it. I do enjoy the solitude of my little home. If you get " +
                        "time, please say hi to my friends in the Dwarven Mine.",
                )
                if (choice2("Dwarven Mine?", 1, "Will do!", 2) == 1) {
                    chatPlayer(quiz, "Dwarven Mine?")
                    chatNpc(
                        happy,
                        "Yep, the entrance is in the side of Ice Mountain just to the east of " +
                            "here. They're a friendly bunch. Stop in at Nurmof's store and buy " +
                            "one of my pickaxes!",
                    )
                } else {
                    chatPlayer(happy, "Will do!")
                }
            }
            else -> {
                chatPlayer(quiz, "What do you make here?")
                chatNpc(
                    happy,
                    "I make pickaxes. I am the best maker of pickaxes in the whole of Gielinor.",
                )
                chatPlayer(quiz, "Do you have any to sell?")
                chatNpc(neutral, "Sorry, but I've got a running order with Nurmof.")
                if (choice2("Who's Nurmof?", 1, "Ah, fair enough.", 2) == 1) {
                    chatPlayer(quiz, "Who's Nurmof?")
                    chatNpc(
                        neutral,
                        "Nurmof has a store over in the Dwarven Mine. You can find the entrance " +
                            "on the side of Ice Mountain to the east of here.",
                    )
                } else {
                    chatPlayer(neutral, "Ah, fair enough.")
                }
            }
        }
    }

    private suspend fun Dialogue.doricQuestOffer() {
        if (choice2("Yes.", 1, "No.", 2, title = "Start Doric's Quest?") == 1) {
            // TODO(content): [quest:dorics_quest] continue into the quest start
            return
        }
        chatPlayer(neutral, "No, hitting rocks is for the boring people, sorry.")
        chatNpc(neutral, "That is your choice. Nice to meet you anyway.")
    }
}
