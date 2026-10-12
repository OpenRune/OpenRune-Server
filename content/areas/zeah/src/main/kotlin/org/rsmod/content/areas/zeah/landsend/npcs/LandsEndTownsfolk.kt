package org.rsmod.content.areas.zeah.landsend.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:floki_offerings] giving Floki bones and meat
//  (https://oldschool.runescape.wiki/w/Transcript:Floki)
class LandsEndTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.zeah_magoro") { startDialogue(it.npc) { captainMagoro() } }
        onOpNpc1("npc.kw_hugor") { startDialogue(it.npc) { hugor() } }
        onOpNpc1("npc.kw_butcher_lan") { startDialogue(it.npc) { lanTheButcher() } }
        onOpNpc1("npc.kw_artist") { startDialogue(it.npc) { robertBoss() } }
        onOpNpc1("npc.kw_dog_floki") { startDialogue(it.npc) { mesbox("You pet the dog...") } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Captain_Magoro
    // TODO(content): [system:ship_travel] sail to Port Piscarilius or Port Sarim, from the
    //  dialogue and the Port Sarim / Port Piscarilius ops (Veos's voyage helper is internal to the
    //  port-sarim module)
    private suspend fun Dialogue.captainMagoro() {
        chatNpc(happy, "Hello! What can I do for you?")
        val option =
            choice4(
                "Where am I exactly?",
                1,
                "Can you take me to Port Piscarilius please?",
                2,
                "Can you take me to Port Sarim please?",
                3,
                "Nothing.",
                4,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Where am I exactly?")
                chatNpc(happy, "This is the port of Land's End!")
                chatPlayer(quiz, "Can you tell me about the local area?")
                chatNpc(
                    neutral,
                    "Certainly. We're on the southern edge of the Kingdom of Great Kourend.",
                )
                chatNpc(
                    neutral,
                    "The kingdom is made up of five main cities. You'll find Shayzien and " +
                        "Lovakengj to the north, and the remaining cities to the north east.",
                )
                chatPlayer(happy, "Thanks.")
            }
            2 -> {
                chatPlayer(neutral, "I'd like to travel to Port Piscarilius, please.")
                chatNpc(neutral, "As you wish.")
            }
            3 -> {
                chatPlayer(neutral, "I'd like to travel to Port Sarim, please.")
                chatNpc(
                    neutral,
                    "As you wish, I hope you don't get seasick, it is a long voyage.",
                )
            }
            else -> chatPlayer(neutral, "Nothing.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hugor
    // TODO(content): [quest:the_depths_of_despair] the Crabclaw Caves dialogue during the quest
    private suspend fun Dialogue.hugor() {
        if (access.random.of(2) == 0) {
            chatNpc(drunk, "Got anymore *hic* wine?")
            chatPlayer(neutral, "Ummm... No, sorry. Looks like you've had enough anyway.")
            chatNpc(drunk, "Well, I *hic* ...disagree.")
            chatPlayer(quiz, "Why are you drinking so much? To forget?")
            chatNpc(drunk, "Nope, tastes mighty fine! Any chance you *hic* know where Horses go?")
            chatPlayer(confused, "The what?")
            chatNpc(drunk, "The Horses!")
            chatPlayer(
                neutral,
                "Ah, the 'Horses'... How unfortunate, the wine has made you delirious. I've no " +
                    "idea what a Horse is my friend.",
            )
            chatNpc(drunk, "Oh.")
        } else {
            chatNpc(drunk, "You know this *hic* place was built twice?")
            chatPlayer(quiz, "Ummm... How come?")
            chatNpc(drunk, "Storm swept in and erased it, had to be built again from scratch.")
            chatPlayer(sad, "Huh, what a shame.")
            chatNpc(
                drunk,
                "They did a better job second time round, but the builder sure was annoyed.",
            )
            chatPlayer(quiz, "Who was the builder?")
            chatNpc(drunk, "Some *hic* Westener.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lan_the_Butcher
    private suspend fun Dialogue.lanTheButcher() {
        chatNpc(angry, "What d'ya want?")
        chatPlayer(happy, "Just exploring the far reaches of this wonderful land.")
        chatNpc(quiz, "Why?")
        chatPlayer(happy, "For gold! For glory!")
        chatNpc(shifty, "You got gold?")
        chatPlayer(shifty, "Maybe a little...")
        chatNpc(
            shifty,
            "Well, I'm a butcher, I'll be 'appy to perhaps trade you, that I would.",
        )
        chatPlayer(confused, "I don't see any product around here...")
        chatNpc(shifty, "Heh, o' course not. I keep it down in the basement, that I do.")
        chatPlayer(confused, "What basement?")
        chatNpc(silent, "...")
        chatPlayer(silent, "...")
        chatNpc(silent, "...")
        chatPlayer(worried, "I think I'll be leaving now.")
        chatNpc(madlaugh, "Heh heh heh, that you should.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Robert_Boss
    private suspend fun Dialogue.robertBoss() {
        when (access.random.of(3)) {
            0 -> {
                chatNpc(happy, "Talk to the tree, make friends with it.")
                chatPlayer(confused, "But I can't reach it...")
                chatNpc(happy, "You can do anything you want to do. This is your world.")
            }
            1 -> {
                chatPlayer(sad, "That tree looks lonely...")
                chatNpc(
                    happy,
                    "I'll give him a friend! Like I always say, everyone needs a friend!",
                )
                chatPlayer(quiz, "Will you be my friend?")
                chatNpc(happy, "Well of course! Right after I finish this painting.")
            }
            else -> {
                chatNpc(happy, "I don't make mistakes, just happy accidents.")
                chatPlayer(happy, "How very positive!")
            }
        }
    }
}
