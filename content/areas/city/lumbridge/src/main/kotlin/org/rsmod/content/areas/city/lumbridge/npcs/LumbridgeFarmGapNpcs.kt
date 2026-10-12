package org.rsmod.content.areas.city.lumbridge.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class LumbridgeFarmGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.fred_the_farmer") { startDialogue(it.npc) { fredTheFarmer() } }
        onOpNpc1("npc.cave_goblin_guard1") { startDialogue(it.npc) { caveGoblinGuard() } }
        onOpNpc1("npc.cave_goblin_guard2") { startDialogue(it.npc) { caveGoblinGuard() } }
    }

    private fun Dialogue.completed(quest: String): Boolean =
        QuestRequirements.hasCompleted(player, quest)

    // https://oldschool.runescape.wiki/w/Transcript:Fred_the_Farmer
    // https://oldschool.runescape.wiki/w/Transcript:Sheep_Shearer
    // TODO(content): [quest:sheep_shearer] the quest from "Start the Sheep Shearer quest?" on, the
    //  20 balls of wool shortcut and the in-quest dialogue
    // TODO(content): [quest:sheep_shearer] "Fred! Fred! I've seen The Thing!" after the Thing
    private suspend fun Dialogue.fredTheFarmer() {
        if (completed(SHEEP_SHEARER)) {
            chatNpc(angry, "What are you doing on my land?")
            val option = choice2("I'm looking for something to kill.", 1, "I'm lost.", 2)
            if (option == 1) fredSomethingToKill() else fredLost()
            return
        }
        chatNpc(
            angry,
            "What are you doing on my land? You're not the one who keeps leaving all my gates " +
                "open and letting out all my sheep, are you?",
        )
        val option =
            choice3(
                "I'm looking for a quest.",
                1,
                "I'm looking for something to kill.",
                2,
                "I'm lost.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "I'm looking for a quest.")
                chatNpc(
                    neutral,
                    "You're after a quest, you say? Actually, I could do with a bit of help.",
                )
                chatNpc(
                    neutral,
                    "My sheep are getting mighty woolly. I'd be much obliged if you could shear " +
                        "them. And while you're at it, spin the wool for me too.",
                )
                chatNpc(
                    neutral,
                    "Yes, that's it. Bring me 20 balls of wool. And I'm sure I could sort out " +
                        "some sort of payment. Of course, there's the small matter of The Thing.",
                )
                chatPlayer(confused, "What do you mean, The Thing?")
                chatNpc(
                    neutral,
                    "Well now, no one has ever seen The Thing. That's why we call it The Thing, " +
                        "'cos we don't know what it is.",
                )
                chatNpc(
                    worried,
                    "Some say it's a black hearted shapeshifter, hungering for the souls of hard " +
                        "working decent folk like me. Others say it's just a sheep.",
                )
                chatNpc(
                    angry,
                    "Well I don't have all day to stand around and gossip. Are you going to " +
                        "shear my sheep or what!",
                )
            }
            2 -> fredSomethingToKill()
            3 -> fredLost()
        }
    }

    private suspend fun Dialogue.fredSomethingToKill() {
        chatPlayer(neutral, "I'm looking for something to kill.")
        chatNpc(angry, "What, on my land? Leave my livestock alone you scoundrel!")
    }

    private suspend fun Dialogue.fredLost() {
        chatPlayer(confused, "I'm lost.")
        chatNpc(
            neutral,
            "How can you be lost? Just follow the road east and south. You'll end up in " +
                "Lumbridge fairly quickly.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Cave_goblin_guard
    private suspend fun Dialogue.caveGoblinGuard() {
        if (!completed(LOST_TRIBE)) {
            chatNpc(angry, "Surface-dweller! You should leave the mines now!")
            return
        }
        when (access.random.of(3)) {
            0 -> chatNpc(happy, "Have a nice day!")
            1 -> chatNpc(neutral, "I'm keeping an eye on you, surface-dweller.")
            else -> {
                chatNpc(
                    angry,
                    "Surface-dweller! You will never find your way into the city of Dorgesh-Kaan!",
                )
                if (!completed(DEATH_TO_THE_DORGESHUUN)) {
                    chatPlayer(confused, "Isn't it through that big door?")
                    chatNpc(angry, "Well, yes, but you'll never find your way in!")
                    return
                }
                chatPlayer(confused, "But I thought I was allowed into the city now.")
                chatNpc(shocked, "Oh yes, so you are. I'd better say something different.")
                chatNpc(neutral, "*Ahem*")
                chatNpc(
                    happy,
                    "Surface-dweller! You will easily find your way into the city of Dorgesh-Kaan!",
                )
                chatNpc(happy, "How about that?")
                chatPlayer(happy, "Yes, that's much more appropiate.")
            }
        }
    }

    private companion object {
        const val SHEEP_SHEARER = "quest_sheepshearer"
        const val LOST_TRIBE = "quest_losttribe"
        const val DEATH_TO_THE_DORGESHUUN = "quest_deathtothedorgeshuun"
    }
}
