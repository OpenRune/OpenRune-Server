package org.rsmod.content.areas.city.prifddinas.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class ClanLeaders : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.lord_amlodd") { startDialogue(it.npc) { amlodd() } }
        onOpNpc1("npc.lord_cadarn") { startDialogue(it.npc) { cadarn() } }
        onOpNpc1("npc.lord_crwys") { startDialogue(it.npc) { crwys() } }
        onOpNpc1("npc.lady_hefin") { startDialogue(it.npc) { hefin() } }
        onOpNpc1("npc.lord_iorwerth_new") { startDialogue(it.npc) { iorwerth() } }
        onOpNpc1("npc.lady_ithell") { startDialogue(it.npc) { ithell() } }
        onOpNpc1("npc.lady_meilyr") { startDialogue(it.npc) { meilyr() } }
        onOpNpc1("npc.lady_trahaearn") { startDialogue(it.npc) { trahaearn() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lord_Ieuan_Amlodd
    private suspend fun Dialogue.amlodd() {
        chatNpc(
            happy,
            "Good day ${player.displayName}. Have you come to hear the song of the elves?",
        )
        chatPlayer(confused, "What do you mean?")
        chatNpc(
            happy,
            "Do you not hear it? The sounds of a city in harmony. It is the song of the elves, " +
                "the story of our people.",
        )
        chatNpc(
            happy,
            "The last few verses were dark and quiet, but now our city is restored, we find " +
                "ourselves singing a new verse, one of joy and light.",
        )
        chatPlayer(
            neutral,
            "I see. I guess I've never been the type to reflect on that sort of thing.",
        )
        chatNpc(
            happy,
            "More of a fighter? We have a song for that too. If you wish to sing it, head to the " +
                "Gauntlet and put yourself to the test.",
        )
        chatPlayer(happy, "I might just give it a shot. Thanks.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lord_Baxtorian_Cadarn
    private suspend fun Dialogue.cadarn() {
        chatPlayer(neutral, "Baxtorian.")
        chatNpc(neutral, "${player.displayName}.")
        chatPlayer(quiz, "How are things?")
        chatNpc(
            happy,
            "The city is flourishing, largely thanks to you. There's still some mistrust for " +
                "the Iorwerth clan, but overall I'd say it's been over a thousand years since " +
                "our people were doing this well.",
        )
        chatPlayer(quiz, "That's great to hear. What about you though?")
        chatNpc(
            sad,
            "I still mourn Glarial every day, until the end of time a part of me will be " +
                "missing, never to be replaced.",
        )
        chatNpc(
            sad,
            "But I know now that I can't let that impact the elves here. Many of them never " +
                "even knew their former queen. My grief is mine alone, they need not bear it.",
        )
        chatNpc(
            neutral,
            "To them, I'm trying my best to be a leader they deserve, to correct the mistakes " +
                "of the past. Lord Iorwerth believed we were nothing without Seren. We need to " +
                "prove that we have a future with her gone.",
        )
        chatNpc(
            neutral,
            "Thankfully I have the other elders to help this time. Ruling is not as hard when " +
                "you share out the responsiblities.",
        )
        chatPlayer(happy, "Well I'm glad to hear things are going well.")
        chatNpc(
            happy,
            "And we are glad to have you here with us. None of this would have been possible " +
                "without you.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lord_Piquan_Crwys
    private suspend fun Dialogue.crwys() {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "Hello there young one. Interested in getting your hands dirty?")
        chatPlayer(confused, "Err... maybe?")
        chatNpc(
            laugh,
            "Ah don't worry, it's nothing sinister. We leave that kind of thing to clan " +
                "Iorwerth. I speak in a more literal sense, of the ground beneath us and the " +
                "trees over us.",
        )
        chatPlayer(quiz, "Oh, I see. So it's your clan that looks after the plants of the city?")
        chatNpc(
            neutral,
            "Indeed. We maintain the farms and manage the trees. The produce we grow goes to " +
                "the Meilyr clan to feed the city and the wood we provide is used by the Ithell " +
                "clan in construction.",
        )
        chatNpc(
            happy,
            "Feel free to join us. Our farming patches are open to you, as is our sawmill.",
        )
        chatPlayer(happy, "Awesome, thank you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lady_Carys_Hefin
    private suspend fun Dialogue.hefin() {
        chatNpc(happy, "Seren be with you friend.")
        chatPlayer(quiz, "Hey there, how are things?")
        chatNpc(
            happy,
            "At long last we are back in our city, our home. Here in the light of Seren, we can " +
                "properly continue our path to enlightenment.",
        )
        chatPlayer(quiz, "How do you do that?")
        chatNpc(
            neutral,
            "We cleanse our minds in prayer, and hone our bodies through agility. You can join " +
                "us if you like. Feel free to reflect at our altar, or take to the roofs on our " +
                "agility course.",
        )
        chatPlayer(happy, "I might just do that, thanks.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lord_Iestin_Iorwerth
    private suspend fun Dialogue.iorwerth() {
        chatNpc(neutral, "Greetings ${player.displayName}.")
        chatPlayer(quiz, "Hey Iestin. So they made you the new Lord Iorwerth?")
        chatNpc(
            neutral,
            "Indeed. Apart from Morvran, I am the closest living relative of the previous Lord " +
                "Iorwerth.",
        )
        chatPlayer(quiz, "Did Morvran not want the role?")
        chatNpc(
            neutral,
            "No. That's probably for the best though. Morvran was very close to my predecessor. " +
                "It would have made many uncomfortable if he had taken on the role.",
        )
        chatPlayer(quiz, "How have things been since the city was reformed?")
        chatNpc(
            sad,
            "The others still hold a lot of mistrust for our clan. It will take a long time for " +
                "those rifts to be repaired. We are on the right path though.",
        )
        chatPlayer(happy, "That's good to hear. All the best.")
        chatNpc(happy, "Likewise.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Kelyn (Prifddinas)
    private suspend fun Dialogue.ithell() {
        chatNpc(happy, "Hey there ${player.displayName}.")
        chatPlayer(quiz, "Hey there. How is it being the new Lady Ithell?")
        chatNpc(
            neutral,
            "It's a daunting job, but I have the knowledge of my predecessor to help me.",
        )
        chatNpc(
            shocked,
            "It's still crazy how fast it all happened. One moment I was a daydreaming elf, " +
                "who'd never even seen Prifddinas. Now I'm responsible for an entire elf clan.",
        )
        chatPlayer(happy, "Well don't worry, I'm sure you're doing great.")
        chatNpc(happy, "Thank you ${player.displayName}.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lady_Ffion_Meilyr
    private suspend fun Dialogue.meilyr() {
        chatNpc(
            happy,
            "Good to see you again ${player.displayName}. Have you come to marvel at the beauty " +
                "of Prifddinas?",
        )
        chatPlayer(happy, "It's definitely a sight to behold.")
        chatNpc(
            happy,
            "Indeed it is. I love exploring, seeking out the hidden secrets of this world. But " +
                "nothing will ever compare to this place, to our home.",
        )
        chatPlayer(happy, "I'm very privileged to be able to see it.")
        chatNpc(happy, "And we are very privileged to have you here.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lady_Tangwen_Trahaearn
    private suspend fun Dialogue.trahaearn() {
        chatPlayer(happy, "Hi there.")
        chatNpc(
            neutral,
            "Good to see you ${player.displayName}. You're very brave to come see me.",
        )
        chatPlayer(confused, "How come?")
        chatNpc(
            neutral,
            "Most of the elves around here give me a wide berth. They seem to find me " +
                "intimidating. How ironic that an old woman is more of a threat to them than " +
                "the entire Iorwerth clan.",
        )
        chatPlayer(neutral, "You must have seen a fair bit in your time.")
        chatNpc(
            neutral,
            "Oh yes, I'm the oldest elf still alive. You can thank this exoskeleton for that, " +
                "one of our finer inventions.",
        )
        chatPlayer(happy, "You must have plenty of stories to tell.")
        chatNpc(
            neutral,
            "Yes, but not the time to tell them. They have plenty of books in the Tower of " +
                "Voices if you want stories, most of them are from our Grand Library.",
        )
        chatNpc(
            laugh,
            "I'd personally recommend the one about Zalcano, a fun tale about how we trapped " +
                "her beneath the city. She's still here now in fact, feel free to go give her a " +
                "poke.",
        )
        chatPlayer(happy, "I'll see about paying her a visit. See you later.")
    }
}
