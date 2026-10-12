package org.rsmod.content.areas.misc.islands.pandemonium.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class PandemoniumTownsfolk @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.pandemonium_fiora") { startDialogue(it.npc) { fiora() } }
        onOpNpc1(HENDERSON) { startDialogue(it.npc) { henderson(it.npc) } }
        onOpNpc3(HENDERSON) { player.openCatchOfTheDay(it.npc) }
        onOpNpc1("npc.pandemonium_polly") { it.npc.say(random.pick(POLLY_LINES)) }
        onOpNpc1("npc.pandemonium_rosalee") { startDialogue(it.npc) { rosalee() } }
        onOpNpc1("npc.tortugan_kebba") { startDialogue(it.npc) { kebba() } }
        onOpNpc1("npc.brass_hand_harry") { startDialogue(it.npc) { brassHandHarry() } }
    }

    private fun Player.openCatchOfTheDay(npc: Npc) {
        shops.open(this, npc, "Henderson's Catch of the Day", "inv.pandemonium_fishmonger")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fiora
    private suspend fun Dialogue.fiora() {
        chatPlayer(happy, "Hello!")
        chatNpc(
            happy,
            "Ahoy there! Here for a drink? You've picked the right place for it. Steve is " +
                "thankfully a much better bartender than he was a sailor.",
        )
        chatPlayer(quiz, "Sounds like there's a story here?")
        chatNpc(
            laugh,
            "Indeed there is. Steve fancied himself a pirate, but on his first proper voyage he " +
                "managed to crash his ship, the Pandemonium, right here.",
        )
        chatPlayer(shocked, "He crashed? How?")
        chatNpc(
            neutral,
            "He claims it was an unavoidable accident caused by the Storm Tempor, but I heard it " +
                "was actually because he got distracted by a large flock of angry parrots.",
        )
        chatPlayer(quiz, "So he crashed the boat and then decided to open a bar?")
        chatNpc(neutral, "Pretty much.")
        chatPlayer(neutral, "I see. Fair enough.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Henderson
    private suspend fun Dialogue.henderson(npc: Npc) {
        chatNpc(
            happy,
            "Hello there. Want to sample my catch of the day? Or maybe you'd prefer to do some " +
                "fishing yourself? If so, I have all the supplies you'll need.",
        )
        if (choice2("Let's see what you have.", true, "Nothing for me, thanks.", false)) {
            chatPlayer(happy, "Let's see what you have.")
            player.openCatchOfTheDay(npc)
            return
        }
        chatPlayer(neutral, "Nothing for me, thanks.")
        chatNpc(neutral, "As you wish.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:One_Eyed_Rosalee
    private suspend fun Dialogue.rosalee() {
        chatNpc(angry, "You're one to talk! I paid plenty for that little encounter!")
        dawson("One eye doesn't equal two of my best men!")
        chatNpc(angry, "We gave you plenty of chances to flee!")
        dawson("If I ran away from you, I'd never hear the end of it!")
        chatNpc(laugh, "Really? Then what about that fight in the Turtle Belt?")
        dawson(
            "That wasn't fleeing! It was a tactical repositioning! Besides, you're conveniently " +
                "forgetting what happened at Brimhaven!"
        )
        chatNpc(bored, "A boring fight! I had better things to do.")
        dawson("Well, we'll have to make things more entertaining next time then!")
        chatNpc(happy, "I look forward to it, but for now, let's drink!")
        dawson("Indeed! Cheers!")
    }

    private suspend fun Dialogue.dawson(text: String) {
        chatNpcSpecific("Captain Dawson", "npc.pandemonium_dawson", neutral, text)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Kebba (before Troubled Tortugans)
    // TODO(content): [quest:troubled_tortugans] closing lines after the quest
    private suspend fun Dialogue.kebba() {
        chatNpc(
            happy,
            "Hello! Nice day we're having today. You here for business, or just a drink?",
        )
        chatPlayer(neutral, "Well, actually I just sort of washed up here.")
        chatNpc(
            happy,
            "Ah, it was the same for me. After I left the Great Conch I didn't really have too " +
                "much of a goal in mind to be honest. I decided to just explore the world. I " +
                "started swimming and ended up here!",
        )
        chatPlayer(quiz, "The Great Conch?")
        chatNpc(
            neutral,
            "It's where the tortugans, my people, live. Well, it's where the juveniles and " +
                "elders live at least. Us adults leave the island to find our calling in the " +
                "wider world.",
        )
        chatNpc(
            neutral,
            "I'd tell you to visit, but you don't get many humans allowed on the island. My " +
                "people prefer their privacy. Who knows though, maybe one day you'll be one of " +
                "the lucky few who get to go there!",
        )
        chatPlayer(neutral, "Interesting...")
        chatNpc(
            happy,
            "Anyway, I shouldn't keep you. I'm sure you've got plenty to be doing. See you later!",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Brass_Hand_Harry
    // TODO(content): [skill:sailing] level 99 greeting and buying the Sailing cape for 99,000
    //  coins
    private suspend fun Dialogue.brassHandHarry() {
        chatPlayer(happy, "Hello there!")
        chatNpc(neutral, "Do I know you?")
        chatPlayer(neutral, "I don't think so.")
        chatNpc(neutral, "Then I will have to ask you to move on.")
        chatPlayer(confused, "But...")
        chatNpc(
            neutral,
            "Here's a word of advice: Don't take things personally. I get a lot of wannabes " +
                "bothering me and wanting to join my crew, but I'm not here to offer out cheap " +
                "thrills.",
        )
        val option =
            choice3(
                "So what are you here for?",
                1,
                "Can you tell me about your cape?",
                2,
                "Fair enough.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "So what are you here for?")
                chatNpc(
                    neutral,
                    "To enjoy a drink and not be bothered. Now, if you'll excuse me...",
                )
            }
            2 -> {
                chatPlayer(quiz, "Can you tell me about your cape?")
                chatNpc(
                    neutral,
                    "Fine. This cape marks me as someone who has mastered the art of sailing. " +
                        "Only those who are skilled enough and brave enough to face anything the " +
                        "seas throw at them are allowed to wear one.",
                )
                chatPlayer(quiz, "So could I have one?")
                chatNpc(neutral, "No. Now, if you'll excuse me...")
                mesbox(
                    "You need to have fully mastered the Sailing skill by reaching level 99 to " +
                        "be able to buy and wear the Sailing Skillcape."
                )
            }
            3 -> chatPlayer(neutral, "Fair enough.")
        }
    }

    private companion object {
        const val HENDERSON = "npc.pandemonium_fishmonger"

        val POLLY_LINES =
            listOf(
                "Squawk! Who's a pretty bird?",
                "Squawk! Leave me alone, bird! I'm trying to have a drink!",
                "Squawk! She's pecking my peg leg!",
                "Squawk! Ow! That was my finger! Stupid bird!",
                "Squawk! Steve! That bird is trying to drink the grog again!",
                "Squawk! Please stop! I'm sorry I called you a pigeon!",
            )
    }
}
