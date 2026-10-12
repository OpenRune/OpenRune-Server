package org.rsmod.content.areas.city.keldagrim.keldagrim.npcs

import dev.openrune.types.MesAnimType
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Miodvetnir,_Derni,_and_Dernu
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:met_flags] "Hello again, [player name]!" greeting after the first talk
// TODO(content): [quest:the_giant_dwarf] the statue options during the quest and the one-time
//  "I have rebuilt the statue" option after it
// TODO(content): [quest:between_a_rock] the rock options during the quest and the one-time
//  "I defeated the spirit of the rock!" option after it
// TODO(content): [quest:forgettable_tale] the drunken dwarf option during the
//  quest and the one-time "unforgettable tale" option after it
class KeldagrimEntranceBrothers : PluginScript() {
    override fun ScriptContext.startup() {
        for (brother in BROTHERS) {
            onOpNpc1(brother) { startDialogue(it.npc) { brothers() } }
        }
    }

    private suspend fun Dialogue.brothers() {
        val name = player.displayName
        miodvetnir(happy, "Oh look, a human!")
        dernu(quiz, "A human? Can we eat it?")
        derni(angry, "No, you rock brain! Humans are our friends!")
        chatPlayer(neutral, "Hello, excuse me...")
        miodvetnir(
            neutral,
            "Not sure if I would call them our friends... Not enemies either, of course.",
        )
        dernu(angry, "Not like the trolls. We hate trolls.")
        chatPlayer(neutral, "Excuse me, I just want to-")
        derni(angry, "Nothing's worse than a troll.")
        dernu(sad, "Except discovering that your pickaxe's gone blunt just as you-")
        chatPlayer(angry, "Excuse me!!!")
        miodvetnir(happy, "Why, hello, yes, what is it?")
        chatPlayer(quiz, "I er... well, who are you?")
        derni(happy, "I'm Derni.")
        dernu(happy, "I'm Dernu.")
        miodvetnir(happy, "And I'm Miodvetnir. These are my brothers. And who might you be?")
        chatPlayer(happy, "My name is $name.")
        derni(happy, "Pleased to meet you, $name!")
        dernu(happy, "I say, we don't get to see a hummon down here very often!")
        miodvetnir(angry, "It's a human, you metal head! You should pay more attention!")
        chatPlayer(quiz, "So, can I ask you dwarves a few questions?")
        miodvetnir(happy, "Of course, we love answering questions!")
        dernu(happy, "We're very good at answering questions!")
        derni(happy, "Fire away!")
        while (true) {
            val option =
                if (beforeGiantDwarf()) {
                    choice4(
                        "What's down this river?",
                        1,
                        "Any good spots to fish around here?",
                        2,
                        "What are these mines used for?",
                        3,
                        "I must be off now.",
                        4,
                    )
                } else {
                    choice3(
                        "Any good spots to fish around here?",
                        2,
                        "What are these mines used for?",
                        3,
                        "I must be off now.",
                        4,
                    )
                }
            when (option) {
                1 -> downTheRiver()
                2 -> fishingSpots()
                3 -> mines()
                else -> {
                    chatPlayer(happy, "I must be off now, thanks for the chat.")
                    miodvetnir(happy, "No problem!")
                    dernu(happy, "Glad to be of service!")
                    derni(happy, "Come back any time!")
                    return
                }
            }
        }
    }

    private fun Dialogue.beforeGiantDwarf(): Boolean =
        QuestRequirements.hasNotCompleted(player, GIANT_DWARF) &&
            !QuestRequirements.isOnQuest(player, GIANT_DWARF)

    private suspend fun Dialogue.downTheRiver() {
        val he = if (access.isBodyTypeA()) "He" else "She"
        val him = if (access.isBodyTypeA()) "him" else "her"
        val his = if (access.isBodyTypeA()) "his" else "her"
        val heLower = he.lowercase()
        chatPlayer(quiz, "What's down this river?")
        miodvetnir(quiz, "Oh, you don't know?")
        dernu(shocked, "$he doesn't know?")
        derni(shocked, "$he doesn't know!")
        chatPlayer(angry, "Can't you just tell me?")
        miodvetnir(happy, "It is our glorious city!")
        dernu(happy, "The grand city of the dwarves!")
        derni(happy, "Sparkling jewel of the mountain!")
        miodvetnir(happy, "Keldagrim, our home!")
        chatPlayer(quiz, "Can you take me there?")
        derni(neutral, "Oh no, we can't do that.")
        miodvetnir(neutral, "We're far too busy.")
        dernu(neutral, "You have to keep busy, you see.")
        derni(neutral, "Always keep busy, is what our good father used to say.")
        dernu(worried, "Besides, there's been some weird going ons in the city.")
        derni(angry, "Don't tell $him that! None of $his business!")
        miodvetnir(neutral, "Only fair if $heLower wants to go into the city.")
        dernu(neutral, "Oh yes, they only just cleaned up the mess from the last little incident.")
        miodvetnir(
            worried,
            "So watch out if you ever do manage to travel to the city! Weird things could happen!",
        )
    }

    private suspend fun Dialogue.fishingSpots() {
        chatPlayer(quiz, "Any good spots to fish around here?")
        miodvetnir(confused, "Fish?")
        derni(confused, "Fish?")
        dernu(confused, "What are fish?")
        miodvetnir(neutral, "No fish around here.")
        val champion = QuestRequirements.hasCompleted(player, FISHING_CONTEST)
        if (champion) {
            miodvetnir(quiz, "Say, you're the Fishing Champion, aren't you?")
            derni(neutral, "That must be why you were talking about fish then...")
            dernu(confused, "What are fish?")
        }
        derni(neutral, "But this water here, you see, it comes dripping down from above.")
        miodvetnir(neutral, "Molten snow, you see.")
        dernu(confused, "What are fish?")
        derni(neutral, "Oh, they're these small wriggly things that live in the water.")
        miodvetnir(happy, "But they're not all small! Some can be large!")
        derni(neutral, "And they don't breathe air.")
        if (champion) {
            val name = player.displayName
            val heIs = if (access.isBodyTypeA()) "he's" else "she's"
            miodvetnir(
                happy,
                "But $name could tell you all about it! After all, $heIs the Fishing Champion!",
            )
            chatPlayer(neutral, "Maybe later...")
        }
    }

    // TODO(content): [verify:in_game] whether the answer stops after the Consortium line once The
    //  Giant Dwarf has started (the transcript only marks the rest "Before The Giant Dwarf")
    private suspend fun Dialogue.mines() {
        val him = if (access.isBodyTypeA()) "him" else "her"
        chatPlayer(quiz, "What are these mines used for?")
        miodvetnir(quiz, "What does it look like they're used for?")
        derni(quiz, "Isn't it obvious?")
        dernu(happy, "For mining of course!")
        miodvetnir(
            neutral,
            "These mines are actually the territory of one of the eight major companies of the " +
                "mining Consortium.",
        )
        if (!beforeGiantDwarf()) {
            return
        }
        chatPlayer(quiz, "Mining companies? Where are they at then?")
        derni(neutral, "Oh, they all have their headquarters in the city.")
        dernu(neutral, "First floor of the palace.")
        derni(angry, "No need to give $him directions.")
        miodvetnir(neutral, "We haul all our goods to the city by ship, you see.")
        dernu(neutral, "Since the mine carts don't come here.")
        derni(neutral, "But because the river goes right to the city, this is just as easy.")
    }

    private suspend fun Dialogue.miodvetnir(mesanim: MesAnimType, text: String) =
        chatNpcSpecific("Miodvetnir", MIODVETNIR, mesanim, text)

    private suspend fun Dialogue.dernu(mesanim: MesAnimType, text: String) =
        chatNpcSpecific("Dernu", DERNU, mesanim, text)

    private suspend fun Dialogue.derni(mesanim: MesAnimType, text: String) =
        chatNpcSpecific("Derni", DERNI, mesanim, text)

    private companion object {
        const val MIODVETNIR = "npc.dwarfrock_boatman_bruv1"
        const val DERNU = "npc.dwarfrock_boatman_bruv2"
        const val DERNI = "npc.dwarfrock_boatman_bruv3"
        const val GIANT_DWARF = "quest_giantdwarf"
        const val FISHING_CONTEST = "quest_fishingcontest"
        val BROTHERS = listOf(MIODVETNIR, DERNU, DERNI)
    }
}
