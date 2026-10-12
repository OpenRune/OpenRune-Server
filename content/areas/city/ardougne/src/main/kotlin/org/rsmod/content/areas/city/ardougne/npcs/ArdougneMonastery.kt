package org.rsmod.content.areas.city.ardougne.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class ArdougneMonastery : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.brother_omad") { startDialogue(it.npc) { brotherOmad() } }
        onOpNpc1("npc.brother_cedric") { startDialogue(it.npc) { brotherCedric() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Brother_Omad
    // TODO(content): [quest:monks_friend] in-progress dialogue
    // TODO(content): [system:treasure_trails] hard clue challenge scroll dialogue
    private suspend fun Dialogue.brotherOmad() {
        if (QuestRequirements.hasCompleted(player, MONKS_FRIEND)) {
            chatNpc(drunk, "Dum dee do la la! *hiccup* That was some party!")
            return
        }
        chatPlayer(quiz, "Hello there, What's wrong?")
        chatNpc(
            sad,
            "*yawn*...oh, hello...*yawn* I'm sorry! I'm just so tired! I haven't slept in a week!",
        )
        val choice =
            choice2(
                "Why can't you sleep, what's wrong?",
                1,
                "Sorry! I'm too busy to hear your problems!",
                2,
            )
        if (choice == 2) {
            chatPlayer(neutral, "Sorry! I'm too busy to hear your problems!")
            return
        }
        chatPlayer(quiz, "Why can't you sleep, what's wrong?")
        chatNpc(
            sad,
            "It's brother Androe's son! With his constant: Waaaaaah! Waaaaaaaaah! Androe said " +
                "it's natural, but it's so annoying!",
        )
        chatPlayer(neutral, "I suppose that's what kids do.")
        chatNpc(
            sad,
            "He was fine, up until last week! Thieves broke in! They stole his favourite " +
                "sleeping blanket!",
        )
        chatNpc(sad, "Now he won't rest until it's returned... ...and that means neither can I!")
        val help =
            choice2(
                "Can I help at all?",
                1,
                "I'm sorry to hear that! I hope you find his blanket.",
                2,
            )
        if (help == 1) {
            // TODO(content): [quest:monks_friend] quest start
            return
        }
        chatPlayer(sad, "I'm sorry to hear that! I hope you find his blanket.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Brother_Cedric
    // TODO(content): [quest:monks_friend] in-progress dialogue
    private suspend fun Dialogue.brotherCedric() {
        if (QuestRequirements.hasCompleted(player, MONKS_FRIEND)) {
            chatNpc(
                drunk,
                "Brother Omad sends you his thanks! He won't be in a fit state to thank you in " +
                    "person.",
            )
            return
        }
        chatPlayer(happy, "Hello.")
        chatNpc(drunk, "Honey, money, woman and wine!")
        chatPlayer(quiz, "Are you ok?")
        chatNpc(drunk, "Yesshh...hic up...beautiful!")
        chatPlayer(neutral, "Take care old monk.")
        chatNpc(drunk, "La..di..da..hic..up!")
    }

    private companion object {
        const val MONKS_FRIEND = "quest_monksfriend"
    }
}
