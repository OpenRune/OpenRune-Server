package org.rsmod.content.areas.misc.zanaris.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Fairy_Aeryka (initial dialogue)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:met_flags] the "It's still here." greeting on later visits
// TODO(content): [verify:in_game] whether each topic returns to the options
class FairyAeryka : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.ii_fairy") { startDialogue(it.npc) { aeryka() } }
    }

    private suspend fun Dialogue.aeryka() {
        val name = player.displayName
        chatPlayer(happy, "Hello, my name is $name. Who are you?")
        chatNpc(happy, "Oh hello $name. I'm Aeryka. Aery for short.")
        chatPlayer(laugh, "Airy Fairy?")
        chatNpc(happy, "That's right. What can I do for you?")
        chatPlayer(quiz, "What's that crop circle thing doing here?")
        chatNpc(confused, "Crop circle? Oh, you mean the Puromantic portal?")
        chatPlayer(confused, "The pyromanic what?")
        chatNpc(
            neutral,
            "The Puromantic portal. At least that's what we call them. It's the way the implings " +
                "travel from Puro-Puro to other planes.",
        )
        chatPlayer(confused, "Puro-Puro?")
        chatNpc(
            neutral,
            "The impling home. We call it Puro-Puro. The implings just call it home, I think.",
        )
        topics()
    }

    private suspend fun Dialogue.topics() {
        while (true) {
            val option =
                choice4(
                    "What's in Puro-Puro?",
                    1,
                    "So what are these implings then?",
                    2,
                    "I've heard I may find dragon equipment in Puro-Puro.",
                    3,
                    "No, bye!",
                    4,
                )
            when (option) {
                1 -> puroPuro()
                2 -> implings()
                3 -> dragonEquipment()
                else -> {
                    chatPlayer(neutral, "No, bye!")
                    chatNpc(happy, "See you around!")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.puroPuro() {
        chatPlayer(quiz, "What's in Puro-Puro?")
        chatNpc(neutral, "Implings...and wheat.")
        chatPlayer(confused, "Erm, anything else?")
        chatNpc(
            neutral,
            "Not really. Though I have noticed quite a lot of you humans travelling through the " +
                "portal recently. I suppose you must like wheat.",
        )
        chatPlayer(
            neutral,
            "Well, most of us prefer lobsters to be perfectly honest, but there must be " +
                "something interesting there.",
        )
        chatNpc(
            neutral,
            "Oh, I did notice a very serious-looking gnome go into the portal. Maybe he knows " +
                "what's going on.",
        )
        chatPlayer(quiz, "Do you remember what his name was?")
        chatNpc(neutral, "Errm, Egg-nog, or something like that.")
        chatPlayer(
            happy,
            "Right, thanks! I'll have a chat with him when I go. Maybe he'll know what's going on.",
        )
    }

    private suspend fun Dialogue.implings() {
        chatPlayer(quiz, "So what are these implings then?")
        chatNpc(
            neutral,
            "Well, no-one knows for sure. The mischievous little creatures are probably related " +
                "to imps. And they fly as well.",
        )
        chatNpc(
            neutral,
            "Also, like imps, they love collecting things. I'm not sure why, though. They also " +
                "seem to like being chased.",
        )
        chatPlayer(quiz, "So how would I get hold of what they are carrying, then?")
        chatNpc(confused, "Catch them, I suppose. I don't know really. Why would you want to?")
        chatPlayer(
            neutral,
            "Well, if they were carrying something useful. Maybe I could catch them with a big " +
                "net - like butterflies.",
        )
        chatNpc(sad, "Sounds a bit cruel to me, but I suppose that's possible.")
    }

    private suspend fun Dialogue.dragonEquipment() {
        chatPlayer(quiz, "I've heard I may find dragon equipment in Puro-Puro.")
        chatNpc(
            neutral,
            "Really? You humans like that stuff a lot, don't you? I don't like really old stuff " +
                "myself.",
        )
        chatPlayer(confused, "Old?")
        chatNpc(neutral, "Yes, dragon stuff feels really old.")
        chatPlayer(quiz, "How can you tell that?")
        chatNpc(
            neutral,
            "From its magical aura, obviously. Oh, I forget you humans can't feel auras.",
        )
        chatPlayer(quiz, "How old is old?")
        chatNpc(neutral, "Really, really, old.")
        chatPlayer(quiz, "Can you be any more precise?")
        chatNpc(
            neutral,
            "Not really. Time doesn't really mean a lot round here. Hundreds, maybe thousands of " +
                "your human lifespans, I suppose. Anyway, it would have to be old since it all " +
                "comes from the Necrosyrtes.",
        )
        chatPlayer(quiz, "Necrosyrtes? Who are they?")
        chatNpc(
            neutral,
            "Old and powerful creatures. I don't think there have been any around here for " +
                "aeons. I haven't seen one. I don't think they are very nice. Not like me. " +
                "Anyway, this is all ancient history. Boooring!",
        )
        chatPlayer(quiz, "While I'm on the subject do you have any dragon stuff I can have?")
        chatNpc(happy, "Oh, I did have loads, but I threw it away.")
        chatPlayer(shocked, "What?")
        chatNpc(laugh, "Only joking.")
        chatNpc(
            neutral,
            "No, sorry, I can't help you there. You'll have to look for it yourself. Although, " +
                "maybe if you find Necrosyrtes then they'll give you some. I heard they give " +
                "dragon stuff away to people they like. Not fairies. We're too",
        )
        chatNpc(happy, "nice.")
    }
}
