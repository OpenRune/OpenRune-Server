package org.rsmod.content.areas.city.varrock.museum

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class OrlandoSmith @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.vm_nathis_apprentice") { talk(it.npc) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            when {
                player.nathisRewardGiven ->
                    chatNpc(happy, "Thanks for all your help, mate. Hope you enjoy the museum!")
                !player.nathisAgreed -> introduction()
                player.nathisDisplaysDone == NATHIS_ALL_DONE -> reward()
                player.nathisDisplaysDone != 0 -> inProgress()
                else -> notStarted()
            }
        }

    private suspend fun Dialogue.introduction() {
        chatNpc(confused, "G'day there, mate.")
        chatPlayer(happy, "Good day. Are you alright? You look a little lost.")
        chatNpc(
            confused,
            "Well, mate, to tell you the truth, I think I've come a gutser with these displays.",
        )
        chatPlayer(confused, "Come a what?")
        chatNpc(
            neutral,
            "Gutser and no mistake. Me boss asked me to put together a quiz for the visitors.",
        )
        chatNpc(
            neutral,
            "But to be deadset with you, I wasn't paying much attention to me boss over there " +
                "and I've done a bit of a rush job.",
        )
        chatPlayer(quiz, "You mean the natural historian?")
        chatNpc(neutral, "Yep, that's the bloke. Say, mate, you do me a favour?")
        chatPlayer(neutral, "Perhaps. What do you need?")
        chatNpc(
            neutral,
            "Well, you look like a pretty smart cobber. Could you take a look at the display " +
                "plaques and give 'em a runthrough?",
        )
        val agreed = choice2("Sure thing.", true, "No thanks I'm too busy.", false)
        if (!agreed) {
            chatPlayer(neutral, "No thanks. I'm too busy.")
            chatNpc(neutral, "Fair dinkum mate. I'm sure I'll get someone else to help me.")
            return
        }
        player.nathisAgreed = true
        chatPlayer(neutral, "Sure thing.")
        chatNpc(happy, "Bonza, mate! I reckon three questions per case should be bang to rights.")
        chatNpc(
            happy,
            "Take a gander at each case and I'll look over your shoulder to give some advice.",
        )
        chatPlayer(neutral, "Will do.")
    }

    private suspend fun Dialogue.notStarted() {
        chatNpc(confused, "G'day there, mate. How're the display cases coming along?")
        chatPlayer(neutral, "I haven't had the chance to take a look yet.")
        chatNpc(happy, "No worries, mate; just have a go when you can.")
        chatPlayer(neutral, "Will do.")
    }

    private suspend fun Dialogue.inProgress() {
        chatNpc(confused, "G'day there, mate. How're the display cases coming along?")
        chatPlayer(neutral, "I'm getting there. I still have a few left to do.")
        chatNpc(happy, "Bonza, mate. You're doing a great job so far.")
        chatPlayer(happy, "Righty ho! I'll get back and finish the rest.")
        chatNpc(happy, "Good luck!")
    }

    private suspend fun Dialogue.reward() {
        chatNpc(confused, "G'day there, mate. How're the display cases coming along?")
        chatPlayer(neutral, "All done. I've corrected the mistakes.")
        chatNpc(happy, "Beaut' mate! That's the best news I've had all day.")
        chatPlayer(happy, "Glad I could help.")
        chatNpc(
            happy,
            "Right, I'll have a chat to me boss over there and let him know you've helped me out.",
        )
        player.nathisRewardGiven = true
        access.statAdvance("stat.slayer", REWARD_XP)
        access.statAdvance("stat.hunter", REWARD_XP)
        access.mes("You have gained 1000xp in Slayer and Hunter.")
        chatNpc(
            happy,
            "You know, with all this new information you've absorbed, I reckon you can say " +
                "you're more experienced in Slaying and Hunting now.",
        )
        chatPlayer(happy, "Oh, thank you very much.")
        chatNpc(happy, "No worries, mate. You take care now.")
        chatPlayer(happy, "Goodbye.")
    }

    private companion object {
        const val REWARD_XP = 1000.0
    }
}
