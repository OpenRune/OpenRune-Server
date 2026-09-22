package org.rsmod.content.areas.city.lumbridge.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.menu
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MillieMiller : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.millie_the_miller") { startDialogue(it.npc) { millie() } }
    }

    private suspend fun Dialogue.millie() {
        chatNpc(happy, "Hello Adventurer. Welcome to Mill Lane Mill. Can I help you?")
        when (
            menu(
                "Who are you?" to 1,
                "What is this place?" to 2,
                "How do I mill flour?" to 3,
                "I'm fine, thanks." to 4,
            )
        ) {
            1 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(
                    happy,
                    "I'm Miss Millicent Miller the Miller of Mill Lane Mill. Our family have been " +
                        "milling flour for generations.",
                )
                chatPlayer(neutral, "It's a good business to be in. People will always need flour.")
                howToMill()
            }
            2 -> {
                chatPlayer(quiz, "What is this place?")
                chatNpc(
                    happy,
                    "This is Mill Lane Mill. Millers of the finest flour in Gielinor, and home to the " +
                        "Miller family for many generations",
                )
                chatNpc(happy, "We take grain from the field nearby and mill into flour.")
                howToMill()
            }
            3 -> howToMill()
            else -> chatPlayer(neutral, "I'm fine, thanks.")
        }
    }

    private suspend fun Dialogue.howToMill() {
        chatPlayer(quiz, "How do I mill flour?")
        chatNpc(
            happy,
            "Making flour is pretty easy. First of all you need to get some grain. You can pick some " +
                "from wheat fields. There is one just outside the Mill, but there are many others " +
                "scattered across Gielinor. Feel free to pick wheat",
        )
        chatNpc(happy, "from our field! There always seems to be plenty of wheat there.")
        chatPlayer(quiz, "Then I bring my wheat here?")
        chatNpc(
            happy,
            "Yes, or one of the other mills in Gielinor. They all work the same way. Just take your " +
                "grain to the top floor of the mill (up two ladders, there are three floors including " +
                "this one) and then place some grain into the hopper.",
        )
        chatNpc(
            happy,
            "Then you need to start the grinding process by pulling the hopper lever. You can add " +
                "more grain, but each time you add grain you have to pull the hopper lever again.",
        )
        chatPlayer(quiz, "So where does the flour go then?")
        chatNpc(
            happy,
            "The flour appears in this room here, you'll need a pot to put the flour into. One pot " +
                "will hold the flour made by one load of grain",
        )
        chatNpc(
            happy,
            "And that's it! You now have some pots of finely ground flour of the highest quality. " +
                "Ideal for making tasty cakes or delicous bread. I'm not a cook so you'll have to ask " +
                "a cook to find out how to bake things.",
        )
        chatPlayer(happy, "Great! Thanks for your help.")
    }
}
