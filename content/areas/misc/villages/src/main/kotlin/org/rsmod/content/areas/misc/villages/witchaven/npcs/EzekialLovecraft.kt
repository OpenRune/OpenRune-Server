package org.rsmod.content.areas.misc.villages.witchaven.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Ezekial_Lovecraft
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:shops] Lovecraft's Tackle has no shop inv in the cache data yet; open it
//  from the Shop op and from "Can I buy some fishing supplies please?"
class EzekialLovecraft : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.slug2_lovecraft") { startDialogue(it.npc) { lovecraft() } }
    }

    private suspend fun Dialogue.lovecraft() {
        chatNpc(happy, "Well hello there; welcome to our little village. Pray, stay awhile.")
        while (true) {
            val option =
                choice4(
                    "You know, you sound just like the mayor?",
                    1,
                    "What do you do here?",
                    2,
                    "Can I buy some fishing supplies please?",
                    3,
                    "You are a bit too strange for me. Bye.",
                    4,
                )
            when (option) {
                1 -> soundLikeTheMayor()
                2 -> {
                    whatDoYouDo()
                    return
                }
                3 -> {
                    chatPlayer(quiz, "Can I buy some fishing supplies please?")
                    return
                }
                4 -> {
                    chatPlayer(neutral, "You are a bit too strange for me. Bye.")
                    chatNpc(sad, "Sniff. Yes, everyone says that.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.soundLikeTheMayor() {
        chatPlayer(quiz, "You know, you sound just like the mayor?")
        chatNpc(happy, "Really? That's the nicest thing anyone has ever said.")
        chatNpc(happy, "I admire him a great deal. He works so hard for this town.")
        chatPlayer(quiz, "You look like him as well?")
        chatNpc(confused, "Hmm, I don't think I do.")
        chatPlayer(
            neutral,
            "No really, what with the unhealthy skin tone and bulging eyes. The resemblance is " +
                "uncanny.",
        )
        chatNpc(neutral, "Well, I haven't spoken to him in a while.  I don't get out much.")
        chatPlayer(neutral, "I can see why.")
        chatNpc(
            happy,
            "Maybe the Mayor has finally seen how much I admire him and has grown to admire me?",
        )
        chatPlayer(
            confused,
            "Anything's possible I suppose. Wait a minute, you mean you've always looked that odd?",
        )
        chatNpc(angry, "Hey, I resent that!")
        chatPlayer(worried, "I'm sorry, it's just...  Nevermind.")
    }

    private suspend fun Dialogue.whatDoYouDo() {
        chatPlayer(quiz, "What do you do here?")
        chatNpc(
            neutral,
            "I supply the local fishermen with the tools and bait they need to do their job.",
        )
        chatPlayer(quiz, "Interesting. Have you been doing it long?")
        chatNpc(
            happy,
            "Why yes, all of my life.  I took over from my father, who inherited the business " +
                "from his father and so on.",
        )
        chatNpc(
            happy,
            "In fact, there have been Lovecraft's selling bait for over ten generations.",
        )
        chatPlayer(neutral, "Wow, that's some lineage.")
        chatNpc(
            happy,
            "Oh yes, we have a long and interesting family history. For one reason or another the " +
                "Lovecraft's have always been bait sellers or writers.",
        )
        chatPlayer(neutral, "Hmm, yes well. I'm sure it's all fascinating, but I...")
        chatNpc(happy, "Oh very, very fascinating, for instance my great grandfather Howard...")
        chatPlayer(
            neutral,
            "Yawn. Oh, I'm sorry but I really must be getting on. I think my giraffe needs " +
                "feeding.",
        )
        chatNpc(confused, "Your?")
        chatPlayer(
            neutral,
            "Giraffe. Sorry, but he gets cranky without enough sugar. Bye now!",
        )
        chatNpc(neutral, "Oh right, goodbye.")
    }
}
