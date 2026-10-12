package org.rsmod.content.areas.misc.islands.braindeath.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:rum_deal] the brewers' and protesters' lines during the quest
class BraindeathBrewers : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.deal_brewer_1") { startDialogue(it.npc) { brewer1() } }
        onOpNpc1("npc.deal_brewer_2") { startDialogue(it.npc) { brewer2() } }
        onOpNpc1("npc.deal_brewer_3") { startDialogue(it.npc) { brewer3() } }
        onOpNpc1("npc.deal_brewer_4") { startDialogue(it.npc) { brewer4() } }
        onOpNpc1("npc.deal_brewer_5") { startDialogue(it.npc) { brewer5() } }
        onOpNpc1("npc.deal_brewer_6") { startDialogue(it.npc) { brewer6() } }
        onOpNpc1("npc.deal_brewer_7") { startDialogue(it.npc) { brewer7() } }
        onOpNpc1("npc.deal_brewer_8") { startDialogue(it.npc) { brewer8() } }
        for (protester in PROTESTERS) {
            onOpNpc1(protester) { startDialogue(it.npc) { zombieProtester() } }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Brewer
    private suspend fun Dialogue.brewer1() {
        chatPlayer(happy, "Hello there!")
        chatNpc(happy, "You saved us! Huzzah!")
        chatPlayer(happy, "All in a day's work, think nothing of it.")
        chatNpc(
            sad,
            "I don't have anything to reward you with except my collection of bleak, gothic " +
                "poetry I wrote when I assumed we were all done for. Do you want it?",
        )
        chatPlayer(neutral, "I may come for it later, you hang on to it for now.")
    }

    private suspend fun Dialogue.brewer2() {
        chatPlayer(happy, "Hello there!")
        chatNpc(angry, "I don't know what your game is, but I know you're one of THEM!")
        chatPlayer(shocked, "But I just saved you!")
        chatNpc(angry, "The voices tell me different. It's all part of a plot! Confess!")
    }

    private suspend fun Dialogue.brewer3() {
        chatPlayer(happy, "Hello there!")
        chatNpc(worried, "Have they gone yet?")
        chatPlayer(neutral, "Well, no, but they are a lot calmer now.")
        chatNpc(worried, "What are we gonna do now, huh? What are we gunna do now?")
        chatPlayer(neutral, "In your case I would say relax.")
    }

    private suspend fun Dialogue.brewer4() {
        chatPlayer(happy, "Hello there!")
        chatNpc(neutral, "Well you proved that you're probably not a zombie.")
        chatNpc(quiz, "So what are you then? A ghoul? A vampyre?")
        chatPlayer(angry, "I'm not any form of undead!")
        chatNpc(worried, "Oh...oh god I'm sorry, I didn't realise it was natural...")
        chatPlayer(confused, "What?")
        chatNpc(shifty, "Nothing...")
    }

    private suspend fun Dialogue.brewer5() {
        chatPlayer(happy, "Hello there!")
        chatNpc(happy, "Hello there, brave hero, in whom I had total confidence!")
        chatPlayer(quiz, "Total confidence?")
        chatNpc(
            happy,
            "Yes! I was so confident that I would never, ever have sold your soft, edible body " +
                "to the pirates outside!",
        )
        chatPlayer(neutral, "Well, great...")
        chatNpc(
            shifty,
            "On a completely unrelated note, I would steer clear of Hungry Frank for a while.",
        )
        chatNpc(
            shifty,
            "He's a filthy liar. And a forger. It wouldn't surprise me if he has written out a " +
                "note detailing the terms of our surrender and your dismemberment and cooking in " +
                "MY handwriting.",
        )
        chatNpc(shifty, "Imagine that, the fiend.")
    }

    private suspend fun Dialogue.brewer6() {
        chatPlayer(quiz, "So...got any sleep yet?")
        chatNpc(sad, "My brain is no longer capable of sleep.")
        chatPlayer(quiz, "So...what are you going to do now?")
        chatNpc(sad, "I was gonna try and will myself dead.")
        chatPlayer(neutral, "Right...good luck with that.")
    }

    private suspend fun Dialogue.brewer7() {
        chatPlayer(happy, "Hello there!")
        chatNpc(happy, "Hello yerself Landlubber!")
        chatPlayer(quiz, "Everything ok with you now?")
        chatNpc(happy, "Hmmm...Overall everything is good!")
        chatPlayer(happy, "Great!")
    }

    private suspend fun Dialogue.brewer8() {
        chatPlayer(happy, "Hello there!")
        chatNpc(neutral, "Hello yourself!")
        chatPlayer(quiz, "How's things?")
        chatNpc(shifty, "Fine...")
        chatPlayer(
            neutral,
            "Excellent! Since I get the feeling I don't want to know why you said that so oddly " +
                "I'll just go over here!",
        )
        chatNpc(neutral, "I think that would be for the best!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Zombie_protester
    private suspend fun Dialogue.zombieProtester() {
        chatNpc(drunk, "Arrrr! Tis yerself! Have a drink!")
        chatPlayer(
            shifty,
            "Errr...Arrr! I will in a sec, I've just go to, err, plunder some landlubbers...",
        )
        chatNpc(drunk, "Good huntin'!")
    }

    private companion object {
        val PROTESTERS = (1..6).map { "npc.deal_zombie_protester_$it" }
    }
}
