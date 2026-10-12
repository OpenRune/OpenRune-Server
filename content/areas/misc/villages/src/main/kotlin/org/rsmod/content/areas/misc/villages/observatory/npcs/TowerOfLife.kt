package org.rsmod.content.areas.misc.villages.observatory.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:tower_of_life] start offer, quest stages and after-quest dialogue
class TowerOfLife : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.tol_npc_efergy01_multi") { startDialogue(it.npc) { effigy() } }
        onOpNpc1("npc.tol_npc_barry01") { startDialogue(it.npc) { bonafido() } }
        onOpNpc1("npc.tol_npc_builder01") { startDialogue(it.npc) { blackEye() } }
        onOpNpc1("npc.tol_npc_builder02") { startDialogue(it.npc) { noFingers() } }
        onOpNpc1("npc.tol_npc_builder03") { startDialogue(it.npc) { gummy() } }
        onOpNpc1("npc.tol_npc_builder04") { startDialogue(it.npc) { theGuns() } }
        onOpNpc1("npc.tol_npc_mage01_multi") { startDialogue(it.npc) { transmute() } }
        onOpNpc1("npc.tol_npc_mage02_multi") { startDialogue(it.npc) { currency() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Effigy (starting the quest)
    private suspend fun Dialogue.effigy() {
        chatPlayer(happy, "Hi. Interesting building you have here.")
        chatPlayer(quiz, "What's it for?")
        chatNpc(
            happy,
            "You like it? It's simply fabulous, isn't it? A real marvel of modern design made " +
                "just for my fellow alchemists and I!",
        )
        chatPlayer(
            neutral,
            "I see some builders over there. So I assume it's not yet finished.",
        )
        chatNpc(
            happy,
            "What a keen eye this chap has! But imagine this: a gargantuan cylinder of expert " +
                "design, incorporating an inventive inner spiral walkway to multiple planes of " +
                "wondrous inhabitance!",
        )
        chatPlayer(neutral, "So, it's a tower.")
        chatNpc(angry, "Oh, no, no, no, no.")
        chatNpc(happy, "It's going to have a use that will change this world!")
        chatNpc(sad, "Or at least it would if we could ever get it finished.")
        chatNpc(neutral, "See those builders?")
        chatPlayer(neutral, "They don't seem to be building much.")
        chatNpc(angry, "Exactly.")
        chatNpc(
            angry,
            "They've gone on strike for some petty reason about the tower being too weird. I " +
                "really can't understand those men.",
        )
        chatPlayer(sad, "Shame.")
        chatNpc(happy, "Hey! Maybe you could talk to them?")
        chatNpc(happy, "You look like a sturdy chap.")
        chatNpc(happy, "Maybe you could finish the work?")
        chatPlayer(confused, "Me?")
        chatNpc(happy, "Come on! This is the chance of a lifetime!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bonafido (before Tower of Life)
    private suspend fun Dialogue.bonafido() {
        chatPlayer(happy, "Hi there.")
        chatNpc(happy, "A'up, mate, how's it gowin'?")
        chatPlayer(happy, "Not bad, thank you. ")
        chatPlayer(quiz, "What are you building here?")
        chatNpc(neutral, "Nowt at moment. We're on strike!")
        chatPlayer(shocked, "You're on strike? Whatever for?")
        chatNpc(
            shifty,
            "Those strange alchemists are up ta somefin', best go ta one ov 'em.",
        )
        chatPlayer(neutral, "Oh, okay.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:%27Black-eye%27 (before Tower of Life)
    private suspend fun Dialogue.blackEye() {
        chatPlayer(quiz, "What's going on?")
        chatNpc(happy, "Ah, we be building this 'ere tower. Lookin' good ain't it?")
        chatPlayer(
            quiz,
            "It does look pretty impressive, but what are you doing hanging around?",
        )
        chatNpc(happy, "Tea break, naturally.")
        chatPlayer(neutral, "Naturally. Not sure why I asked.")
        chatNpc(happy, "Fancy some?")
        chatPlayer(happy, "Tea? Sure.")
        chatNpc(happy, "You'll find a kettle in the box to the-")
        chatPlayer(worried, "Erm...yeah...no...sorry...gotta go.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:%27No_fingers%27 (before Tower of Life)
    private suspend fun Dialogue.noFingers() {
        chatPlayer(neutral, "You seem to like playing with danger, there.")
        chatNpc(laugh, "They don't call me 'No-fingers' for nothing!")
        chatPlayer(
            confused,
            "Well, correct me if I'm wrong, but you seem to have all your fingers.",
        )
        chatNpc(
            neutral,
            "It may seem that way mate, but I think you'll find I don't have any.",
        )
        chatPlayer(confused, "What?")
        chatNpc(neutral, "These not be fingers. These 'ere be wooden fingers.")
        chatPlayer(confused, "Wooden fingers?")
        chatNpc(neutral, "Yep.")
        chatPlayer(quiz, "That's not possible, how could you move them?")
        chatNpc(neutral, "Magic wood.")
        chatPlayer(worried, "Yes, well I think I'd better be going now.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:%27Gummy%27 (before Tower of Life)
    private suspend fun Dialogue.gummy() {
        chatPlayer(quiz, "What are you up to?")
        chatNpc(neutral, "Rock, Paper, Scissors. Can't you tell?")
        chatPlayer(neutral, "It was more of a rhetorical question.")
        chatNpc(confused, "Rhetora-what?")
        chatPlayer(quiz, "Shouldn't you be working?")
        chatNpc(happy, "We are. It's team building.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:%27The_Guns%27 (before Tower of Life)
    // TODO(content): [system:npc_overhead] 'The Guns' overhead counting
    private suspend fun Dialogue.theGuns() {
        chatPlayer(shocked, "Wow, that looks pretty heavy.")
        chatNpc(worried, "Umpf! Yeah, mate...argghh...this is...umpf...hard work...")
        chatPlayer(shifty, "What would happen if I were to say...")
        chatPlayer(shifty, "135")
        chatPlayer(shifty, "25")
        chatPlayer(shifty, "234")
        chatPlayer(shifty, "2351...")
        chatNpc(angry, "Stop! Stop! Humpf...I'll beat you to...urghh...a pulp!")
        chatPlayer(laugh, "Okay, okay! Don't cry!")
        chatNpc(angry, "Why, I oughta...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:%27Transmute%27_The_Alchemist
    private suspend fun Dialogue.transmute() {
        chatPlayer(quiz, "Hi there. What is this place?")
        chatNpc(happy, "Ah, this be the Tower of Life! Beautiful, isn't it?")
        chatPlayer(neutral, "I prefer ordinary houses.")
        chatNpc(angry, "Pah, you have no taste.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:%27Currency%27_The_Alchemist
    private suspend fun Dialogue.currency() {
        chatPlayer(happy, "This is quite the tower.")
        chatNpc(happy, "Yeah, it cost a fair bit.")
        chatPlayer(quiz, "How much?")
        chatNpc(
            shifty,
            "Well, I had to 'alch' a fair few belongings of my fellow alchemists to afford it.",
        )
        chatPlayer(neutral, "I bet they love you for that.")
        chatNpc(happy, "Everyone loves someone that can create money.")
        chatPlayer(angry, "I don't.")
    }
}
