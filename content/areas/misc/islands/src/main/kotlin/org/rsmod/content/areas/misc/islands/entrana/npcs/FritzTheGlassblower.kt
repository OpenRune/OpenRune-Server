package org.rsmod.content.areas.misc.islands.entrana.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Fritz_the_Glassblower
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:met_flags] the later-visit dialogue after he has explained glassmaking:
// buying molten glass at 20 coins each, re-explaining glassmaking and only giving the bucket once
class FritzTheGlassblower @Inject constructor(private val objRepo: ObjRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.entrana_glassblower") { startDialogue(it.npc) { fritz() } }
    }

    private suspend fun Dialogue.fritz() {
        chatNpc(happy, "Hello adventurer, welcome to the Entrana furnace.")
        chatNpc(quiz, "Would you like me to explain my craft to you?")
        val option =
            choice2(
                "Yes please. I'd be fascinated to hear what you do.",
                1,
                "No thanks, I doubt I'll ever turn my hand to glassblowing.",
                2,
            )
        when (option) {
            1 -> explainCraft()
            2 -> {
                chatPlayer(
                    neutral,
                    "No thanks, I doubt I'll ever turn my hand to glassblowing.",
                )
                chatNpc(
                    sad,
                    "Ok, suit yourself. Nobody seems to be interested in the skilled crafts " +
                        "these days.",
                )
                chatNpc(neutral, "If you should change your mind, come back and talk to me.")
            }
        }
    }

    private suspend fun Dialogue.explainCraft() {
        chatPlayer(happy, "Yes please. I'd be fascinated to hear what you do.")
        chatNpc(
            happy,
            "I'm extremely pleased to hear that! I've always wanted an apprentice. Let me talk " +
                "you through the secrets of glassblowing.",
        )
        chatNpc(
            neutral,
            "Glass is made from soda ash and silica. We get our soda ash by collecting seaweed " +
                "from the rocks - the prevailing currents make the north-west corner of the " +
                "island the best place to find it, it can also be found in",
        )
        chatNpc(
            neutral,
            "your nets sometimes when you're fishing, on Karamja island or at the Piscatoris " +
                "Fishing Colony in the nets there. To turn seaweed into soda ash, all you need " +
                "to do is burn it on a fire. Feel free to use the range in",
        )
        chatNpc(
            neutral,
            "my house for that; it's the one directly west of here. Next we collect sand from " +
                "the sandpit that you'll also find just west of here, there are others located " +
                "in Yanille and Shilo Village.",
        )
        chatNpc(
            happy,
            "You'll need a bucket to carry it in. Tell you what, you can have this old one of " +
                "mine.",
        )
        player.invAddOrDrop(objRepo, BUCKET)
        chatNpc(
            neutral,
            "Bring the sand and the soda ash back here and melt them together in the furnace, " +
                "and there you have it - molten glass!",
        )
        chatNpc(
            neutral,
            "There are many things you can use the molten glass for once you have made it. " +
                "Depending on how talented you are, you could try turning it into something, " +
                "like a fishbowl, for example. If you'd like to try your hand at",
        )
        chatNpc(
            neutral,
            "the fine art of glassblowing you can use my spare glassblowing pipe. I think I " +
                "left it on the chest of drawers in my house this morning.",
        )
        chatNpc(
            happy,
            "Alternatively I am always happy to buy the molten glass from you, saves me running " +
                "about making it for myself.",
        )
        chatPlayer(quiz, "That sounds good. How much will you pay me?")
        chatNpc(
            happy,
            "Tell you what, because you've been interested in my art, I'll pay you the premium " +
                "price of 20 gold pieces for each piece of molten glass you bring me",
        )
    }

    private companion object {
        const val BUCKET = "obj.bucket_empty"
    }
}
