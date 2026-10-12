package org.rsmod.content.areas.misc.islands.lunarisle.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class MoonClan : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.lunar_moonclan_monk2") { talk(it.npc) { meteora() } }
        onOpNpc1("npc.lunar_moonclan_monk1") { talk(it.npc) { paulinePolaris() } }
        onOpNpc1("npc.lunar_moonclan_monk4") { talk(it.npc) { selene() } }
        onOpNpc1("npc.lunar_moonclan_monk_watering") { talk(it.npc) { bouquetMacHyacinth() } }
        onOpNpc1("npc.lunar_oneiromancer") { talk(it.npc) { oneiromancer() } }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc, dialogue: suspend Dialogue.() -> Unit) {
        if (!hasSealOfPassage()) {
            startDialogue(npc) { refuseWithoutSeal() }
            return
        }
        startDialogue(npc) { dialogue() }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Meteora
    private suspend fun Dialogue.meteora() {
        chatPlayer(happy, "Take me to your leader.")
        chatNpc(confused, "Wot?")
        chatPlayer(
            neutral,
            "Isn't that what you're supposed to say when you meet a new race of people?",
        )
        chatNpc(
            neutral,
            "Well, mate, we don't av no leader, so I ain't gonna take ya to um. We auw av eqwuw " +
                "right's un stuff, yeah? But if ya wont an intro go live it up wiv da " +
                "Onerimancer.",
        )
        chatPlayer(quiz, "OK. Is he rich and powerful?")
        chatNpc(
            neutral,
            "No man, SHE, is dead-wise yeah? She is responsible for initiation wituals an stuff.",
        )
        chatPlayer(quiz, "Great. Thanks. And where is she?")
        chatNpc(neutral, "She angs out int souf east of ouw island.")
        chatPlayer(confused, "Okey dokey....yeah.")
        chatNpc(confused, "Huh?")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Pauline_Polaris
    private suspend fun Dialogue.paulinePolaris() {
        chatPlayer(happy, "Hello there.")
        chatNpc(neutral, "Ah, a stranger to our island. What brings you here?")
        chatPlayer(
            neutral,
            "Well, I've actually come here on a diplomatic mission. I want to try and settle " +
                "some of the disputes between the Fremenniks and your clan.",
        )
        chatNpc(
            angry,
            "The Fremenniks? Pah! They are just too ignorant and stubborn to listen to anything " +
                "we have to say - how can we possibly associate with a race that won't listen?",
        )
        chatPlayer(
            neutral,
            "I think they are very eager to listen, but they feel like you are keeping secrets " +
                "from them.",
        )
        chatNpc(confused, "Secrets?")
        chatPlayer(
            neutral,
            "Yes, as in your magic ways. To be honest, I think it is fear of the unknown.",
        )
        chatNpc(
            neutral,
            "Well, when the day comes that a Fremennik can prove they have the patience and " +
                "interest in learning our ways, then we will perhaps share our secrets.",
        )
        chatPlayer(happy, "I'm hoping I might be able to achieve that.")
        chatNpc(neutral, "Good luck. It's far from simple.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Selene
    private suspend fun Dialogue.selene() {
        chatPlayer(quiz, "Can you tell me a bit about your people?")
        chatNpc(neutral, "Ok. Like what?")
        chatPlayer(quiz, "How about the values of the Moon clan!")
        chatNpc(
            happy,
            "Let me see... We value knowledge of self because it is this that gives us our " +
                "strength! It is most important!",
        )
        chatPlayer(happy, "Well... I know things about myself. I know I like hot chocolate!")
        chatNpc(
            neutral,
            "I was meaning something a little deeper than that. We also like to see someone " +
                "listen. You know how they say a wise man listens?",
        )
        chatPlayer(silent, "....")
        chatNpc(quiz, "Did you hear me?")
        chatPlayer(silent, ".... I'm listening.")
        chatNpc(happy, "Most wise.")
        chatPlayer(confused, "Huh?")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bouquet_Mac_Hyacinth
    private suspend fun Dialogue.bouquetMacHyacinth() {
        chatPlayer(quiz, "Hi! What are you up to?")
        chatNpc(happy, "Watering the pretty flowers, you want to help?")
        chatPlayer(neutral, "I don't have time to water flowers, I have people to save!")
        chatNpc(neutral, "Pft, you should take time to enjoy the simple things.")
        chatPlayer(neutral, "I'm not a simple person.")
        chatNpc(neutral, "So it seems.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Oneiromancer
    // TODO(content): [system:lunar_equipment_replacement] replacing a lost lunar staff (30,000
    // coins) or other lost lunar equipment after "The state of the island."
    // TODO(content): [quest:dream_mentor] the "Cyrisus." option
    // TODO(content): [quest:while_guthix_sleeps] Cyrisus' fate in the "Cyrisus." option
    private suspend fun Dialogue.oneiromancer() {
        chatNpc(happy, "Hello there. What do you want to talk about?")
        if (choice2("The state of the island.", true, "Nothing.", false)) {
            chatPlayer(quiz, "Hi, how are things going?")
            chatNpc(
                happy,
                "Well hopefully a lot better now that you've initiated the calm between the Moon " +
                    "Clan and the Fremenniks. Remember, if you want to use our Lunar Spells at " +
                    "any time, pray at the altar beside me and you can modify",
            )
            chatNpc(happy, "your knowledge!")
        }
    }
}
