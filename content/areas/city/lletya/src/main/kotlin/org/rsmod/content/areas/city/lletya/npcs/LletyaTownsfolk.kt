package org.rsmod.content.areas.city.lletya.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class LletyaTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.mourning_town_elf_3") { startDialogue(it.npc) { arvel() } }
        onOpNpc1("npc.mourning_town_elf_1") { startDialogue(it.npc) { goreu() } }
        onOpNpc1("npc.mourning_town_elf_4") { startDialogue(it.npc) { mawrth() } }
        onOpNpc1("npc.mourning_town_elf_2") { startDialogue(it.npc) { ysgawyn() } }
        onOpNpc1("npc.mourning_town_elf_5") { startDialogue(it.npc) { kelyn() } }
        onOpNpc1("npc.mourning_elf_kid_1") { startDialogue(it.npc) { eoin() } }
        onOpNpc1("npc.mourning_elf_kid_2") { startDialogue(it.npc) { iona() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Arvel
    // TODO(content): [quest:song_of_the_elves] dialogue after the attack on Lletya
    private suspend fun Dialogue.arvel() {
        chatNpc(happy, "Good day traveller. You are far from home, what brings you here?")
        chatPlayer(happy, "I am a wandering hero, I come here in search of adventure.")
        chatNpc(neutral, "Sounds ghastly, I just want to live in peace.")
        chatPlayer(neutral, "Unfortunately without people like me, peace doesn't last for long.")
        chatNpc(
            neutral,
            "True, but then again most adventurers cause as much trouble as they put right.",
        )
        chatPlayer(confused, "You've got a point there... Hmm...")
        chatNpc(happy, "Well, good day traveller. And always do the right thing.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Goreu
    // TODO(content): [quest:song_of_the_elves] dialogue after the attack on Lletya
    // TODO(content): [system:treasure_trails] master light or puzzle box
    private suspend fun Dialogue.goreu() {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "Good day, can I help you?")
        chatPlayer(neutral, "No thanks I'm just watching the world go by.")
        chatNpc(
            happy,
            "Well I can think of no better place to do it, it is beautiful here is it not?",
        )
        chatPlayer(happy, "Indeed it is.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Mawrth
    // TODO(content): [system:treasure_trails] elite clue step
    private suspend fun Dialogue.mawrth() {
        chatNpc(
            worried,
            "Those children are nothing but trouble - if I did not watch them all the time, " +
                "Seren knows what trouble they would get in to!",
        )
        chatPlayer(neutral, "They look old enough to look after themselves.")
        chatNpc(neutral, "They are only 34 and 38, far too young to be left unsupervised.")
        chatPlayer(shocked, "Only!?! How old does that make you?")
        chatNpc(angry, "Has no one told you it is rude to ask a lady her age?")
        chatPlayer(sad, "Sorry, I wasn't thinking. Anyway... I'd better stop distracting you.")
        chatNpc(neutral, "Okay, See you some other time.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ysgawyn
    // TODO(content): [system:treasure_trails] clue step (missing from the wiki transcript)
    private suspend fun Dialogue.ysgawyn() {
        chatPlayer(happy, "Hey.")
        chatNpc(neutral, "Can I help you?")
        chatPlayer(neutral, "Not really I just wanted to chat.")
        chatNpc(angry, "Sorry, I do not have time for idle gossip. Good day.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Kelyn (Lletya)
    // TODO(content): [quest:song_of_the_elves] "I hope that once this war is over, I'll finally be
    // able to." (the wiki does not name the condition)
    private suspend fun Dialogue.kelyn() {
        chatPlayer(happy, "Hello.")
        chatNpc(shocked, "Huh... Oh sorry, you made me jump. I was miles away, day dreaming.")
        chatPlayer(quiz, "About what may I ask?")
        chatNpc(happy, "I was thinking about the crystal spires of Prifddinas.")
        chatPlayer(neutral, "It must be beautiful, I've only seen the city walls.")
        chatNpc(
            sad,
            "I have never seen it, all I know are the stories. I hope that changes one day.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Eoin
    // TODO(content): [quest:song_of_the_elves] dialogue after the attack on Lletya
    private suspend fun Dialogue.eoin() {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "Sorry, I cannot stop or Iona will catch me, we are playing tag!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Iona
    // TODO(content): [quest:song_of_the_elves] dialogue after the attack on Lletya
    private suspend fun Dialogue.iona() {
        chatPlayer(happy, "Hello.")
        chatNpc(sad, "I can never catch Eoin, he is just too fast, I am always 'It'.")
    }
}
