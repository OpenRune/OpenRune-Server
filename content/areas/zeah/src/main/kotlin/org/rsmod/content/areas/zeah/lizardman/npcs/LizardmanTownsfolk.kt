package org.rsmod.content.areas.zeah.lizardman.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class LizardmanTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.lizardman_cave_guard") { startDialogue(it.npc) { captainCleive() } }
        onOpNpc1("npc.raids_xerician_priest") { startDialogue(it.npc) { swampPriest() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Captain_Cleive
    // TODO(content): [skill:slayer] "Hey! These lizardmen aren't just for anyone." when attacking
    //  lizardman shamans off task
    private suspend fun Dialogue.captainCleive() {
        chatNpc(neutral, "Hello there. What do you need?")
        var topic = cleiveMenu()
        while (true) {
            when (topic) {
                CleiveTopic.Place -> {
                    chatPlayer(quiz, "What is this place?")
                    chatNpc(
                        neutral,
                        "This is one of the lairs of those foul lizardmen. We've managed to " +
                            "corner these ones so they can't escape.",
                    )
                }
                CleiveTopic.Who -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(
                        neutral,
                        "Cleive. Captain Cleive. Captain in the Shayzien Army and renowned slayer.",
                    )
                    chatPlayer(quiz, "Renowned?")
                    chatNpc(
                        happy,
                        "Absolutely. I'm part of a great family of slayers. You've probably met " +
                            "a few of them over on the mainland.",
                    )
                }
                CleiveTopic.Why -> {
                    chatPlayer(quiz, "Why are you here?")
                    chatNpc(
                        neutral,
                        "We learnt of a group of Lizardman Shamans causing trouble on the " +
                            "surface. I led a taskforce out to drive them back into their lair.",
                    )
                    chatNpc(
                        sad,
                        "We suffered many losses but we've now managed to trap them in this cave.",
                    )
                    chatNpc(
                        neutral,
                        "We've lost too many men to be able to finish them off but I'm happy for " +
                            "someone else to do it if I feel they're worthy of the challenge.",
                    )
                }
                CleiveTopic.Fight -> {
                    chatPlayer(quiz, "Can I fight these lizardmen?")
                    chatNpc(
                        neutral,
                        "Perhaps. I've lost too many men to be able to kill them myself but I'm " +
                            "happy for someone else to do it if I feel they're worthy of the " +
                            "challenge.",
                    )
                    chatPlayer(quiz, "What do you define as worthy?")
                    chatNpc(
                        neutral,
                        "You'll need to be favoured by the people of Shayzien. You'll also need " +
                            "to be on a Lizardman Slayer task.",
                    )
                }
                CleiveTopic.SlayerHelm -> slayerHelm()
            }
            topic = cleiveMenu()
        }
    }

    // TODO(content): [verify:in_game] how the menu ends; the transcript shows no goodbye option
    private suspend fun Dialogue.cleiveMenu(): CleiveTopic =
        choice5(
            "What is this place?",
            CleiveTopic.Place,
            "Who are you?",
            CleiveTopic.Who,
            "Why are you here?",
            CleiveTopic.Why,
            "Can I fight these lizardmen?",
            CleiveTopic.Fight,
            "Can I use my Slayer helm instead of a Shayzien helm?",
            CleiveTopic.SlayerHelm,
        )

    // TODO(content): [diary:kourend_and_kebos] teaching the technique after the hard diary and
    //  the tier five Shayzien helm, and "I have already shown you that secret technique."
    private suspend fun Dialogue.slayerHelm() {
        chatPlayer(quiz, "Can I use my slayer helm instead of a Shayzien helm?")
        chatNpc(
            neutral,
            "Yes that is possible, however I only show the required technique to those who have " +
                "proved themselves a friend to Kourend.",
        )
        chatPlayer(quiz, "How do I prove that I am a friend to Kourend?")
        chatNpc(
            neutral,
            "You'll need to complete the Hard tier of the Kourend & Kebos Achievement Diary and " +
                "have earned the tier five Shayzien helm.",
        )
        chatPlayer(neutral, "I don't meet those requirements. I'll be back when I do though.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Swamp_Priest
    // TODO(content): [system:kharedsts_memoirs] using the memoirs with The Fisher's Flute on him
    private suspend fun Dialogue.swampPriest() {
        chatNpc(neutral, "Yessss, adventurer? You smell funky.")
        while (true) {
            val option =
                choice5(
                    "You smell funky.",
                    1,
                    "How come you live with the lizardmen?",
                    2,
                    "What is that you're wearing?",
                    3,
                    "What is the symbol on the shrine?",
                    4,
                    "Goodbye, swampy man.",
                    5,
                )
            when (option) {
                1 -> {
                    chatPlayer(neutral, "You smell funky.")
                    chatNpc(neutral, "As do you, adventurer.")
                }
                2 -> livesWithLizardmen()
                3 -> {
                    chatPlayer(quiz, "What is that you're wearing?")
                    chatNpc(
                        neutral,
                        "I found these in thisss shrine. I guesss they belonged to sssome long " +
                            "dead priest.",
                    )
                    chatPlayer(quiz, "What would a priest be doing in this gross swamp?")
                    chatNpc(
                        angry,
                        "This grossss ssswamp is my home. You sssure do ask a lot of " +
                            "questionsss, adventurer.",
                    )
                }
                4 -> {
                    chatPlayer(quiz, "What is the symbol on the shrine?")
                    chatNpc(
                        neutral,
                        "You do not recognissse it, adventurer? It would ssseem hissstory is " +
                            "written by the winnersss, for that isss the sssymbol of the Great " +
                            "Lord Xeric.",
                    )
                    chatNpc(neutral, "Or at leassst, that isss what the lizardmen tell me...")
                }
                else -> {
                    chatPlayer(neutral, "Goodbye, swampy man.")
                    chatNpc(neutral, "Goodbye, adventurer.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.livesWithLizardmen() {
        chatPlayer(quiz, "How come you live with the lizardmen?")
        chatNpc(neutral, "They are my sssaviours, I owe them my life.")
        chatPlayer(quiz, "What did they save you from? Is it to do with your eye?")
        chatNpc(
            neutral,
            "Why, yesss it isss... Many yearsss ago now. 30? 40? I've lost count.",
        )
        chatNpc(
            neutral,
            "I wasss a Captain of the Shayzien Army, Captain Reynard Koil, ssstationed up on " +
                "Shayziens' wall.",
        )
        chatNpc(
            neutral,
            "We were told to attack the lizardmen, for they were apparently attacking usss. I " +
                "led the charge.",
        )
        chatPlayer(quiz, "Apparently? Were they attacking?")
        chatNpc(
            sad,
            "Alasss, I took an arrow to the eye and missed the battle. I woke up here, worried " +
                "for my life.",
        )
        chatPlayer(quiz, "The lizardmen took you prisoner?")
        chatNpc(
            neutral,
            "At first, that isss what I thought, but they tended my woundsss and fed me well.",
        )
        chatNpc(
            sad,
            "I assssumed my people would come for me once they sssaw my body wasn't amongssst " +
                "the dead, but they never did.",
        )
        chatPlayer(quiz, "Would the lizardmen let you leave?")
        chatNpc(
            neutral,
            "Oh yesss, once my ssstrength returned, I went and ssspoke to the Shayzien Army.",
        )
        chatPlayer(quiz, "...And?")
        chatNpc(
            sad,
            "They thought I wasss a lizardman and fired arrowsss at me, ssso I returned here.",
        )
    }

    private enum class CleiveTopic {
        Place,
        Who,
        Why,
        Fight,
        SlayerHelm,
    }
}
