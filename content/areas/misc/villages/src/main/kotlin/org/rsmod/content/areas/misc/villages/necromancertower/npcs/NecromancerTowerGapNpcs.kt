package org.rsmod.content.areas.misc.villages.necromancertower.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class NecromancerTowerGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.zogre_necromancers_assistant") { startDialogue(it.npc) { irwin() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Irwin_Feaselbaum
    // TODO(content): [quest:zogre_flesh_eaters] using the torn page and the necromancy book on him
    private suspend fun Dialogue.irwin() {
        chatPlayer(quiz, "Hey, what do you do here?")
        chatNpc(
            neutral,
            "I help the main man over there. He can get a bit cantankerous. Every now and then " +
                "he'll just start attacking people without warning. I act as a sort of home " +
                "help...",
        )
        chatPlayer(quiz, "How do you mean, home help?")
        chatNpc(
            neutral,
            "Well, sometimes he'll fly right off the handle and just start, you know, 'raising " +
                "the dead' to do his 'bidding'. That kind of activity is just so anti-social, so " +
                "it tends to attract a lot of hero types, you know!",
        )
        chatPlayer(neutral, "Yes, yes, I know the sort...full of themselves aren't they.")
        chatNpc(
            neutral,
            "Exactly! They come in all, 'Have at thee! ' and 'Death to you evil one', it's all a " +
                "bit too dramatic for me to be honest.",
        )
        chatNpc(
            neutral,
            "Anyway, he'll often end up getting killed and nothing get's done around the house " +
                "while he's awaiting his mortal return.",
        )
        chatPlayer(quiz, "Mortal return? Sounds interesting.")
        chatNpc(
            neutral,
            "Well, it's kind of interesting the first few times, but it's like falling off a " +
                "bike...",
        )
        chatPlayer(quiz, "What, you graze your knees a lot?")
        chatNpc(
            neutral,
            "I was going to say that you soon get fed-up with it. I guess it's one of those " +
                "perks of the job though, ability to raise yourself from the dead.",
        )
        chatNpc(
            neutral,
            "You'd think he'd do better things with a gift like that, but all he tends to do is " +
                "get angry at people who come into his house without being invited and then he's " +
                "off again 'raising the dead'.",
        )
    }
}
