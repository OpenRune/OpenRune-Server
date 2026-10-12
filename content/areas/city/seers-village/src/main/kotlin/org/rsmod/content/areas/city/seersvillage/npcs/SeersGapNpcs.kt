package org.rsmod.content.areas.city.seersvillage.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class SeersGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.bob_sinclair") { startDialogue(it.npc) { mesbox("He is ignoring you.") } }
        onOpNpc1("npc.pierre_the_family_dog_handler") { startDialogue(it.npc) { pierre() } }
        onOpNpc1("npc.stanford_the_gardener") { startDialogue(it.npc) { stanford() } }
        onOpNpc1("npc.gossipy_man") { startDialogue(it.npc) { gossip() } }
        onOpNpc1("npc.sherlock") { startDialogue(it.npc) { sherlock() } }
        onOpNpc1("npc.trail_falo_the_bard") { startDialogue(it.npc) { falo() } }
        onOpNpc1("npc.vt_council_workmen") { startDialogue(it.npc) { councilWorkman() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Pierre (before Murder Mystery)
    // TODO(content): [quest:murder_mystery] in-quest and after-quest dialogue
    private suspend fun Dialogue.pierre() {
        chatNpc(neutral, "The guards told me not to talk to anyone.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Stanford (before Murder Mystery)
    // TODO(content): [quest:murder_mystery] in-quest and after-quest dialogue
    private suspend fun Dialogue.stanford() {
        chatNpc(angry, "Have you no shame? We are all grieving at the moment.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gossip (before Murder Mystery)
    // TODO(content): [quest:kings_ransom] after-quest dialogue
    private suspend fun Dialogue.gossip() {
        chatNpc(
            neutral,
            "There's some kind of commotion up at the Sinclair place I hear. Not surprising all " +
                "things considered.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sherlock
    // TODO(content): [system:treasure_trails] elite and master skill challenges
    private suspend fun Dialogue.sherlock() {
        chatPlayer(quiz, "Hey, who're you?")
        chatNpc(neutral, "I'm Sherlock, master of clues and challenges.")
        chatPlayer(quiz, "Clues and challenges?")
        chatNpc(
            neutral,
            "If you find a scroll that requires my attention, bring it to me and I shall handle " +
                "it.",
        )
        chatPlayer(happy, "I will! but...")
        chatPlayer(confused, "You're not wearing a shirt?")
        chatNpc(neutral, "Yeah, no shirt Sherlock.....")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Falo_the_Bard (before The Fremennik Trials)
    // TODO(content): [quest:the_fremennik_trials] songwriting talk once the trials are started
    // TODO(content): [system:treasure_trails] master clue song dialogue
    private suspend fun Dialogue.falo() {
        chatPlayer(quiz, "Hi there! Who are you?")
        chatNpc(angry, "Get away! I do not speak to outlanders.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Council_workman
    // TODO(content): [system:item_on_npc] beer for the strange object, and the refusals
    private suspend fun Dialogue.councilWorkman() {
        chatPlayer(happy, "Hello.")
        chatNpc(
            neutral,
            "How do. You planning on crossing this here bridge and heading up to Rellekka then?",
        )
        if (choice2("Yes", 1, "No", 2) == 1) {
            chatPlayer(neutral, "Yes, actually I was.")
            chatNpc(
                neutral,
                "Aye, 'tis a good thing we fixed this 'ere bridge, I reckon. You best be careful " +
                    "up there. Them Fremenniks are odd 'uns.",
            )
            chatPlayer(quiz, "So you fixed this bridge yourself?")
            chatNpc(
                neutral,
                "Aye, that I did. 'Twas real thirsty work too. If only some kind stranger would " +
                    "buy us a bit of beer to sup, eh?",
            )
            chatNpc(neutral, "What with that inn at seers so close by and all, eh?")
        } else {
            chatPlayer(neutral, "I wasn't planning on it, no.")
            chatNpc(
                neutral,
                "Aye, that's what I told t'gaffer. Nobody wants to go up and hang out with a " +
                    "buncha dumb barbarians when there's a perfectly good inn to sup in, in " +
                    "t'seers village.",
            )
            chatNpc(
                happy,
                "Ain't much better in life than having a nice cold beer to sup on a hot day like " +
                    "this, eh? Better'n fixing bridges, eh?",
            )
        }
        mesbox("The workman winks at you")
        chatNpc(
            neutral,
            "Just make sure yer don't get me none of that non alcoholic rubbish from that poison " +
                "salesman guy at the tavern!",
        )
    }
}
