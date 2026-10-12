package org.rsmod.content.areas.city.falador.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:The_Knight%27s_Sword (before the quest)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:the_knights_sword] quest start and every later stage
class Squire : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.squire") { startDialogue(it.npc) { squire() } }
    }

    private suspend fun Dialogue.squire() {
        chatNpc(neutral, "Hello. I am the squire to Sir Vyvin.")
        val life =
            choice2(
                "And how is life as a squire?",
                true,
                "Wouldn't you prefer to be a squire for me?",
                false,
            )
        if (!life) {
            chatPlayer(quiz, "Wouldn't you prefer to be a squire for me?")
            chatNpc(neutral, "No, sorry, I'm loyal to Sir Vyvin.")
            return
        }
        chatPlayer(quiz, "And how is life as a squire?")
        chatNpc(
            sad,
            "Well, Sir Vyvin is a good guy to work for, however, I'm in a spot of trouble today. " +
                "I've gone and lost Sir Vyvin's sword!",
        )
        when (
            choice3(
                "Do you know where you lost it?",
                LOST_IT,
                "I can make a new sword if you like...",
                MAKE,
                "Is he angry?",
                ANGRY,
            )
        ) {
            LOST_IT -> lostIt()
            MAKE -> makeSword()
            ANGRY -> isHeAngry()
        }
    }

    private suspend fun Dialogue.lostIt() {
        chatPlayer(quiz, "Do you know where you lost it?")
        chatNpc(angry, "Well now, if I knew THAT it wouldn't be lost, now would it?")
        when (
            choice4(
                "Well, do you know the VAGUE AREA you lost it?",
                VAGUE,
                "I can make a new sword if you like...",
                MAKE,
                "Well, the kingdom is fairly abundant with swords...",
                KINGDOM,
                "Is he angry",
                ANGRY,
            )
        ) {
            VAGUE -> vagueArea()
            MAKE -> makeSword()
            KINGDOM -> abundantSwords()
            ANGRY -> isHeAngry()
        }
    }

    private suspend fun Dialogue.vagueArea() {
        chatPlayer(quiz, "Well, do you know the VAGUE AREA you lost it in?")
        chatNpc(
            sad,
            "No. I was carrying it for him all the way from where he had it stored in Lumbridge. " +
                "It must have slipped from my pack during the trip, and you know what people are " +
                "like these days...",
        )
        chatNpc(sad, "Someone will have just picked it up and kept it for themselves.")
        when (
            choice3(
                "I can make a new sword if you like...",
                MAKE,
                "Well, the kingdom is fairly abundant with swords...",
                KINGDOM,
                "Well, I hope you find it soon.",
                HOPE,
            )
        ) {
            MAKE -> makeSword()
            KINGDOM -> abundantSwords()
            HOPE -> hopeYouFindIt()
        }
    }

    private suspend fun Dialogue.isHeAngry() {
        chatPlayer(quiz, "Is he angry?")
        chatNpc(
            worried,
            "He doesn't know yet. I was hoping I could think of something to do before he does " +
                "find out, But I find myself at a loss.",
        )
        when (
            choice4(
                "Well, do you know the VAGUE AREA you lost it?",
                VAGUE,
                "I can make a new sword if you like...",
                MAKE,
                "Well, the kingdom is fairly abundant with swords...",
                KINGDOM,
                "Well, I hope you find it soon.",
                HOPE,
            )
        ) {
            VAGUE -> vagueArea()
            MAKE -> makeSword()
            KINGDOM -> abundantSwords()
            HOPE -> hopeYouFindIt()
        }
    }

    private suspend fun Dialogue.makeSword() {
        chatPlayer(neutral, "I can make a new sword if you like...")
        chatNpc(neutral, "Thanks for the offer. I'd be surprised if you could though.")
        heirloom()
    }

    private suspend fun Dialogue.abundantSwords() {
        chatPlayer(neutral, "Well, the kingdom is fairly abundant with swords...")
        chatNpc(neutral, "Yes. You can get bronze swords anywhere. But THIS isn't any old sword.")
        heirloom()
    }

    private suspend fun Dialogue.heirloom() {
        chatNpc(
            neutral,
            "The thing is, this sword is a family heirloom. It has been passed down through " +
                "Vyvin's family for five generations! It was originally made by the Imcando " +
                "dwarves, who were",
        )
        chatNpc(
            neutral,
            "a particularly skilled tribe of dwarven smiths. I doubt anyone could make it in the " +
                "style they do.",
        )
        val dwarves =
            choice2(
                "So would these dwarves make another one?",
                true,
                "Well I hope you find it soon.",
                false,
            )
        if (!dwarves) {
            hopeYouFindIt()
            return
        }
        chatPlayer(quiz, "So would these dwarves make another one?")
        chatNpc(
            neutral,
            "I'm not a hundred percent sure the Imcando tribe exists anymore. I should " +
                "think Reldo, the palace librarian in Varrock, will know; he has done a lot of " +
                "research on the races of Gielinor.",
        )
        chatNpc(
            quiz,
            "I don't suppose you could try and track down the Imcando dwarves for me? I've got " +
                "so much work to do...",
        )
        // TODO(content): [quest:the_knights_sword] "Start The Knight's Sword quest?" offer
    }

    private suspend fun Dialogue.hopeYouFindIt() {
        chatPlayer(neutral, "Well, I hope you find it soon.")
        chatNpc(
            sad,
            "Yes, me too. I'm not looking forward to telling Vyvin I've lost it. He's going to " +
                "want it for the parade next week as well.",
        )
    }

    private companion object {
        const val LOST_IT = 1
        const val MAKE = 2
        const val KINGDOM = 3
        const val HOPE = 4
        const val ANGRY = 5
        const val VAGUE = 6
    }
}
