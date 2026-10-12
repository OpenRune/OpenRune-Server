package org.rsmod.content.areas.misc.outskirts.goblinvillage.npcs

import dev.openrune.types.MesAnimType
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Goblin_Diplomacy
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:goblin_diplomacy] quest start, armour hand-ins, cutscenes and the
// after-quest dialogue (https://oldschool.runescape.wiki/w/Transcript:General_Wartface)
class GoblinVillage : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(BENTNOZE) { startDialogue(it.npc) { generals() } }
        onOpNpc1(WARTFACE) { startDialogue(it.npc) { generals() } }
        onOpNpc1("npc.catwalk_goblin") { startDialogue(it.npc) { grubfoot() } }
        for (soldier in RED_SOLDIERS) {
            onOpNpc1(soldier) { startDialogue(it.npc) { villageGoblin(red = true) } }
        }
        for (soldier in GREEN_SOLDIERS) {
            onOpNpc1(soldier) { startDialogue(it.npc) { villageGoblin(red = false) } }
        }
    }

    private suspend fun Dialogue.bentnoze(mesanim: MesAnimType, text: String) =
        chatNpcSpecific("General Bentnoze", BENTNOZE, mesanim, text)

    private suspend fun Dialogue.wartface(mesanim: MesAnimType, text: String) =
        chatNpcSpecific("General Wartface", WARTFACE, mesanim, text)

    private suspend fun Dialogue.generals() {
        when (access.random.of(5)) {
            0 -> {
                bentnoze(angry, "All goblins should wear red armour!")
                wartface(angry, "Not red! Red armour make you look fat.")
                bentnoze(laugh, "Everything make YOU look fat!")
                wartface(angry, "Shut up!")
                bentnoze(laugh, "Fatty!")
                wartface(verymad, "SHUT UP!")
                bentnoze(laugh, "Even this human think you look fat! Don't you, human?")
                chatPlayer(confused, "Um...")
                when (choice2("Yes, he looks fat.", 1, "No, he doesn't look fat.", 2)) {
                    1 -> {
                        chatPlayer(laugh, "Yes, he looks fat!")
                        bentnoze(laugh, "Ha ha! See, fatty? Even human think you fat!")
                        wartface(angry, "Me not care what human think! Human ugly!")
                    }
                    2 -> {
                        chatPlayer(neutral, "No, he doesn't look fat.")
                        bentnoze(angry, "Shut up human! Wartface fat and human stupid!")
                        wartface(angry, "Shut up Bentnoze!")
                    }
                }
            }
            1 -> {
                wartface(angry, "I tell all goblins in village to wear green armour now!")
                bentnoze(angry, "They not listen to you! I already tell them wear red armour!")
                wartface(angry, "They listen to me not you! They know me bigger general!")
                bentnoze(angry, "Me bigger general! They listen to me!")
                wartface(quiz, "Human! What colour armour they wearing out there?")
                chatPlayer(neutral, "Half of them are wearing red and half of them green.")
                wartface(
                    angry,
                    "Shut up human! They wearing green armour really! Human lying because he " +
                        "scared of you!",
                )
                bentnoze(laugh, "Human scared of me not you? Then you think me bigger general!")
                wartface(confused, "What? Me mean...")
                wartface(verymad, "Shut up! Me bigger general!")
            }
            2 -> {
                wartface(angry, "We should wear green armour!")
                bentnoze(angry, "Green armour? Are you stupid?")
                wartface(angry, "You stupid! Only stupid goblins think red armour better!")
                bentnoze(angry, "No they don't! Me think red armour better!")
                wartface(angry, "That because you stupid!")
                bentnoze(angry, "Me not stupid!")
                wartface(angry, "Then why you not like green armour?")
                bentnoze(angry, "Because red armour better!")
                wartface(angry, "Only stupid goblins think that! You stupid!")
            }
            3 -> {
                wartface(angry, "Green armour best.")
                bentnoze(angry, "No no red every time.")
                wartface(angry, "Go away human, we busy.")
            }
            else -> {
                bentnoze(angry, "Red armour best.")
                wartface(angry, "No it has to be green!")
                bentnoze(angry, "Go away human, we busy.")
            }
        }
        generalsTopics()
    }

    private suspend fun Dialogue.generalsTopics() {
        var menu = MAIN_MENU
        while (true) {
            val topic =
                when (menu.size) {
                    4 ->
                        choice4(
                            menu[0].text,
                            menu[0],
                            menu[1].text,
                            menu[1],
                            menu[2].text,
                            menu[2],
                            menu[3].text,
                            menu[3],
                        )
                    else -> error("Unexpected menu size: ${menu.size}")
                }
            when (topic) {
                Topic.Arguing -> {
                    whyArguing()
                    menu = ARGUING_MENU
                }
                Topic.NewCentury -> newCentury()
                Topic.Peace -> peace()
                Topic.PickColour -> {
                    pickColour()
                    menu = COLOUR_MENU
                }
                Topic.Red -> wearRed()
                Topic.Green -> wearGreen()
                Topic.Different -> return differentColour()
                Topic.Leave -> return chatPlayer(neutral, "I'll leave you to it.")
            }
        }
    }

    private suspend fun Dialogue.whyArguing() {
        chatPlayer(quiz, "Why are you arguing about the colour of your armour?")
        bentnoze(
            neutral,
            "We decide to celebrate goblin new century by changing colour of our armour. Brown " +
                "get boring after a bit. We want change.",
        )
        wartface(
            neutral,
            "Problem is, we can't agree on new colour. We think maybe we use old goblin tribe " +
                "colours, but Bentnoze from rubbish Thorobshuun tribe and me from much better " +
                "Garagorshuun tribe.",
        )
        bentnoze(
            angry,
            "Thorobshuun tribe colour is red! Way better than silly green of Garagorshuun tribe.",
        )
        wartface(angry, "Shut up!")
    }

    private suspend fun Dialogue.newCentury() {
        chatPlayer(quiz, "What is the goblin new century?")
        bentnoze(
            neutral,
            "Goblin century mark year of battle on Plain of Mud. That when Big High War God give " +
                "goblin commandments.",
        )
        chatPlayer(quiz, "Big High War God?")
        wartface(
            neutral,
            "Big High War God is god for goblins. He take us and make us strong, not stupid like " +
                "humans.",
        )
        bentnoze(happy, "Me bet Big High War God would pick red armour.")
        wartface(angry, "Shut up! Big High War God would hate you!")
    }

    private suspend fun Dialogue.peace() {
        chatPlayer(quiz, "Wouldn't you prefer peace?")
        wartface(
            neutral,
            "Goblins not so good at peace. Maybe peace be okay though, as long as it peace " +
                "wearing green armour.",
        )
        bentnoze(angry, "But green too much like skin. Nearly make you look naked!")
    }

    private suspend fun Dialogue.pickColour() {
        chatPlayer(quiz, "Do you want me to pick an armour colour for you?")
        wartface(happy, "Yes, as long as you pick green.")
        bentnoze(angry, "No you have to pick red!")
    }

    private suspend fun Dialogue.wearRed() {
        chatPlayer(neutral, "You should wear red.")
        bentnoze(laugh, "See? Even stupid human think red best. Now we all wear red!")
        wartface(
            angry,
            "Human not know anything! If we wear red then whole village be ugly like YOU!",
        )
        bentnoze(angry, "Go away human. You not helping.")
    }

    private suspend fun Dialogue.wearGreen() {
        chatPlayer(neutral, "You should wear green.")
        wartface(laugh, "Green! We all wear green now, human has decided!")
        bentnoze(angry, "Why we have to do what human say? He not boss of us!")
        wartface(happy, "No but he agree with me!")
        bentnoze(angry, "That prove you a filthy human-lover!")
        wartface(angry, "Me hate humans! This human just happen to be right!")
        bentnoze(angry, "Go away human. You not know anything.")
    }

    // TODO(content): [quest:goblin_diplomacy] "Start the Goblin Diplomacy quest?" offer and the
    // orange armour briefing
    private suspend fun Dialogue.differentColour() {
        chatPlayer(quiz, "What about a different colour?")
        bentnoze(confused, "That would mean me wrong... but at least Wartface not right!")
        wartface(
            neutral,
            "Well Bentnoze never been right in his life. Still, maybe new colour good, but will " +
                "have to see armour before decide.",
        )
        bentnoze(neutral, "Human! You bring us armour in new colour!")
    }

    private suspend fun Dialogue.grubfoot() {
        chatNpc(confused, "Grubfoot wear red armour! Grubfoot wear green armour!")
        chatNpc(confused, "Why they not make up their minds?")
        if (access.random.of(2) == 0) {
            wartface(angry, "Shut up Grubfoot!")
        } else {
            bentnoze(angry, "Shut up Grubfoot!")
        }
    }

    private suspend fun Dialogue.villageGoblin(red: Boolean) {
        val own = if (red) "red" else "green"
        val other = if (red) "green" else "red"
        val ownGeneral = if (red) "General Bentnoze" else "General Wartface"
        val otherGeneral = if (red) "General Wartface" else "General Bentnoze"
        val otherType = if (red) GREEN_SOLDIERS.first() else RED_SOLDIERS.first()
        when (access.random.of(3)) {
            0 -> {
                chatNpc(angry, "${own.replaceFirstChar(Char::uppercase)} armour best!")
                when (choice2("Why is $own best?", 1, "Err, okay.", 2)) {
                    1 -> {
                        chatPlayer(quiz, "Why is $own best?")
                        // TODO(content): [verify:in_game] the green goblin's answer; the
                        // transcript only gives the red one
                        chatNpc(neutral, "Cos $ownGeneral says so, and he bigger than me.")
                    }
                    2 -> chatPlayer(neutral, "Err, okay.")
                }
            }
            1 -> {
                if (red) {
                    chatNpcSpecific("Goblin", otherType, angry, "Green armour best.")
                    chatNpc(angry, "Red armour best!")
                } else {
                    chatNpc(angry, "Green armour best.")
                    chatNpcSpecific("Goblin", otherType, angry, "Red armour best!")
                }
            }
            else -> {
                chatPlayer(quiz, "Why are you fighting?")
                chatNpc(angry, "He wearing $other armour! $ownGeneral tell us wear $own!")
                chatNpcSpecific(
                    "Goblin",
                    otherType,
                    angry,
                    "But $otherGeneral say we must wear $other!",
                )
            }
        }
    }

    private enum class Topic(val text: String) {
        Arguing("Why are you arguing about the colour of your armour?"),
        NewCentury("What is the goblin new century?"),
        Peace("Wouldn't you prefer peace?"),
        PickColour("Do you want me to pick an armour colour for you?"),
        Red("You should wear red."),
        Green("You should wear green."),
        Different("What about a different colour?"),
        Leave("I'll leave you to it."),
    }

    private companion object {
        const val BENTNOZE = "npc.general_bentnoze_red"
        const val WARTFACE = "npc.general_wartface_green"
        val MAIN_MENU = listOf(Topic.Arguing, Topic.Peace, Topic.PickColour, Topic.Leave)
        val ARGUING_MENU = listOf(Topic.NewCentury, Topic.Peace, Topic.PickColour, Topic.Leave)
        val COLOUR_MENU = listOf(Topic.Red, Topic.Green, Topic.Different, Topic.Leave)
        val RED_SOLDIERS = (2..8).map { "npc.goblin_red_soldier_$it" }
        val GREEN_SOLDIERS = (2..8).map { "npc.goblin_green_soldier_$it" }
    }
}
