package org.rsmod.content.areas.misc.villages.piscatoris.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:swan_song] the colony is instanced until the quest is complete; pre-quest
//  and quest dialogue for the colonists
class PiscatorisColonists @Inject constructor(private val objRepo: ObjRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.swan_herman") { startDialogue(it.npc) { herman() } }
        onOpNpc1("npc.swan_franklin") { startDialogue(it.npc) { franklin() } }
        onOpNpc1("npc.swan_colonist_1") { startDialogue(it.npc) { devin() } }
        onOpNpc1("npc.swan_colonist_2") { startDialogue(it.npc) { george() } }
        onOpNpc1("npc.swan_colonist_3") { startDialogue(it.npc) { ramara() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Herman_Caranos (after Swan Song)
    private suspend fun Dialogue.herman() {
        chatPlayer(happy, "Hiya Hermie!")
        chatNpc(happy, "Ah, ${player.displayName}! How's the fishing going?")
        chatPlayer(neutral, "Still working on it. How's the Colony?")
        chatNpc(
            happy,
            "Flourishing, now that the colonists are coming back! Soon all the finest restaurants " +
                "will be serving monkfish!",
        )
        chatNpc(
            neutral,
            "It's a pity those skeletons you brought here haven't gone away again, though. " +
                "They're in the dormitories, which is rather annoying! So please take a little " +
                "time to kick 'em around a bit.",
        )
        chatNpc(angry, "Oh, and please don't call me Hermie.")
        chatPlayer(laugh, "Whatever you like, Hermie!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Franklin_Caranos
    private suspend fun Dialogue.franklin() {
        val sheets = player.inv.physicalCount(IRON_SHEET)
        if (sheets == 0) {
            chatNpc(happy, "Hello again, ${player.displayName}.")
            chatPlayer(quiz, "Hello. How's the repair work going?")
            chatNpc(
                neutral,
                "I'm working on it. I can always do with more iron sheets, so if you've got any " +
                    "more, I'll give you 20 gp per sheet.",
            )
            chatPlayer(happy, "Thanks, I'll remember that.")
            return
        }
        if (sheets == 1) {
            chatNpc(
                happy,
                "Hello again, ${player.displayName}. Were you wanting to sell me that iron " +
                    "sheet? I'd give you 20 gp for it.",
            )
        } else {
            chatNpc(
                happy,
                "Hello again, ${player.displayName}. Were you wanting to sell me those iron " +
                    "sheets? I'd give you ${sheets * SHEET_PRICE} gp.",
            )
        }
        when (choice2("Yes, that'll do nicely.", 1, "No, I'd rather not.", 2)) {
            1 -> {
                chatPlayer(happy, "Yes, that'll do nicely.")
                sellSheets()
            }
            2 -> {
                chatPlayer(neutral, "No, I'd rather not.")
                chatNpc(neutral, "That's ok.")
            }
        }
    }

    private fun Dialogue.sellSheets() {
        val sheets = player.inv.physicalCount(IRON_SHEET)
        if (sheets == 0) {
            return
        }
        val taken = player.invDel(player.inv, IRON_SHEET, sheets, ignoreVirtualStorage = true)
        if (!taken.success) {
            return
        }
        player.invAddOrDrop(objRepo, COINS, sheets * SHEET_PRICE)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Devin_Mendelberg (after Swan Song)
    private suspend fun Dialogue.devin() {
        chatNpc(happy, "Hello! What can I do for you?")
        val option =
            choice4(
                "How are things going for the Colony?",
                1,
                "What are you doing?",
                2,
                "You know all your doors get stuck open?",
                3,
                "Nothing, I'm fine.",
                4,
            )
        when (option) {
            1 -> {
                chatPlayer(
                    quiz,
                    "How are things going for the Colony now the trolls have all gone away?",
                )
                chatNpc(
                    neutral,
                    "Well, it's been fairly quiet around here, but there's always repair work to " +
                        "keep us busy.",
                )
                chatPlayer(quiz, "Catching plenty of fish?")
                chatNpc(
                    happy,
                    "Oh yes, lots of them. Their population's been swelling ever since you wiped " +
                        "out the trolls in the area. They've hardly got any natural predators now.",
                )
                chatPlayer(confused, "Oh... I suppose that's good...")
            }
            2 -> {
                chatPlayer(quiz, "I was just wondering what you're doing.")
                chatNpc(
                    neutral,
                    "Franklin's got me fixing this wall. You did a pretty good patch-job, but it " +
                        "could do with some work.",
                )
                chatPlayer(
                    neutral,
                    "I'm not surprised - iron sheets won't last any time at all with all that " +
                        "salt water around.",
                )
                chatNpc(
                    neutral,
                    "Yes, that's right. I'll probably get Arnold to order some fancy protective " +
                        "paint from the dwarves.",
                )
                chatPlayer(happy, "Good luck with that.")
            }
            3 -> {
                chatPlayer(quiz, "You know all your doors get stuck open?")
                chatNpc(
                    sad,
                    "Yes, the salt does terrible things to the hinges. I've been meaning to deal " +
                        "with them, but I never seem to get around to it.",
                )
                chatPlayer(neutral, "Fair enough.")
            }
            4 -> {
                chatPlayer(neutral, "Nothing, I'm fine.")
                chatNpc(happy, "Enjoy your fishing.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:George_Laxmeister
    private suspend fun Dialogue.george() {
        chatNpc(happy, "Can I help you?")
        val option =
            choice3(
                "What can you tell me about the people here?",
                1,
                "What is there for me to do here?",
                2,
                "No thanks, I'm fine.",
                3,
            )
        when (option) {
            1 -> people()
            2 -> {
                chatPlayer(quiz, "What is there for me to do here?")
                chatNpc(
                    happy,
                    "You can fish! But I'd be very glad if you could go and do something about " +
                        "the skeletal magi in the dormitories. They've kept appearing there ever " +
                        "since the trolls left.",
                )
                chatNpc(
                    neutral,
                    "Also, if you're into glassbowing, you'll probably want a bit of seaweed. It " +
                        "catches in our nets all the time, so you'll usually find some in the " +
                        "machinery down by the shore.",
                )
                chatPlayer(happy, "Thanks.")
            }
            3 -> {
                chatPlayer(neutral, "No thanks, I'm fine.")
                chatNpc(happy, "Ok, but come and talk to me anytime.")
            }
        }
    }

    private suspend fun Dialogue.people() {
        chatPlayer(quiz, "What can you tell me about the people here?")
        chatNpc(
            neutral,
            "Let's see... We currently have Herman & Franklin Caranos, Arnold Lydspor, Devin " +
                "Mendelberg and Ramara du Croissant. And me, of course.",
        )
        chatNpc(
            neutral,
            "Herman and Franklin are brothers, as you'll know. Herman's always been a fishing " +
                "nut. Franklin's not all that bothered about fishing, but he does like tinkering " +
                "with machinery. They're originally from East Ardougne.",
        )
        chatNpc(
            neutral,
            "Arnold's a bit more of a mystery. We think he might be from one of the barbarian " +
                "tribes, but he never talks about that. He's always up for a chat, if you can " +
                "understand his accent.",
        )
        chatNpc(
            neutral,
            "Devin Mendelberg comes from some town in Morytania - ghastly place! I'm not " +
                "surprised he moved about as far west as he could go! He helps Franklin with the " +
                "machinery, among other things.",
        )
        chatNpc(
            neutral,
            "Ramara's another Ardougnian. Her father's a baker there, but she didn't want to " +
                "spend her whole life baking and doing his accounts. Also, you probably know what " +
                "Ardougne's crime rate is like - it can't be easy running",
        )
        chatNpc(
            neutral,
            "a market stall in that city! I went there once, and some wannabe adventurer started " +
                "following me around trying to pick my pockets! I kept hitting him, but he kept " +
                "coming back again and again!",
        )
        chatPlayer(quiz, "You haven't told me about yourself yet.")
        chatNpc(
            happy,
            "Oh, I used to work with my sister, Emily, in the Rising Sun in Falador, but I wanted " +
                "an outdoor job. The pay's better out here too, and the work's not too bad.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ramara_du_Croissant
    private suspend fun Dialogue.ramara() {
        chatNpc(happy, "Can I help you?")
        when (choice2("Why do you work in a fishing colony?", 1, "No thanks, I'm alright.", 2)) {
            1 -> {
                chatPlayer(quiz, "Why do you work in a fishing colony?")
                chatNpc(
                    happy,
                    "Ahh, it is because I love the sea. The rushing waters, the endless horizon - " +
                        "it is beautiful! Also, I like the fresh fish.",
                )
                chatPlayer(
                    quiz,
                    "It seems like rather hard work - can't you just get a beach house on Karamja?",
                )
                chatNpc(
                    neutral,
                    "That, I am afraid, is not an option for me. Besides, I have read of the " +
                        "island of Karamja - it is full of big green spiders, and I like them not!",
                )
                chatPlayer(neutral, "Fair enough.")
            }
            2 -> {
                chatPlayer(neutral, "No thanks, I'm alright.")
                chatNpc(happy, "Ahh, farewell.")
            }
        }
    }

    private companion object {
        const val COINS = "obj.coins"
        const val IRON_SHEET = "obj.iron_sheet"
        const val SHEET_PRICE = 20
    }
}
