package org.rsmod.content.quest.area.lumbridge


import dev.openrune.types.ItemServerType
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestProgressState
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.ScriptContext

class CooksAssistant : QuestScript("quest_cooksassistant", "varp.cookquest", rewards {
    xp("stat.cooking", 300.0)
}, ItemRewardDisplay("obj.cake")) {

    private val GIVEN_EGG = quest.attribute(name = "GIVEN_EGG", default = false)
    private val GIVEN_MILK = quest.attribute(name = "GIVEN_MILK", default = false)
    private val GIVEN_FLOUR = quest.attribute(name = "GIVEN_FLOUR", default = false)

    private val potFlour = "obj.pot_flour"

    override fun ScriptContext.init() {
        onOpNpc1("npc.cook") { startCookDialogue(it.npc) }
    }

    private suspend fun ProtectedAccess.startCookDialogue(npc: Npc) {
        startDialogue(npc) { cookDialogue(npc) }
    }

    private suspend fun Dialogue.cookDialogue(npc: Npc) {
        when {
            quest.isQuestCompleted(player) -> dialogAfterCook(npc)
            quest.questState(player) == QuestProgressState.IN_PROGRESS -> dialogDuringCook(npc)
            else -> dialogQuestNotStarted(npc)
        }
    }

    override fun subTitle(): String {
        return "talking to the <col=800000>Cook</col> in <col=800000>Lumbridge Castle</col>."
    }

    override fun questLog(player: ProtectedAccess) = questJournal(player) {
        description("It's the <red>Duke of Lumbridge's</red> birthday and I have to help his <red>Cook</red> make him a <red>birthday cake</red>. To do this I need to bring him the following ingredients:")

        objective("I need to find a <red>bucket of milk</red>. There's a cattle field east of Lumbridge, I should make sure I take an empty bucket with me.") {
            attribute(GIVEN_MILK, "I have given the cook a <red>bucket of milk</red>.").strike()
            hasItem("bucket_milk", "I have found a <red>bucket of milk</red> to give to the cook.")
        }

        objective("I need to find a <red>pot of flour</red>. There's a mill found north-west of Lumbridge, I should take an empty pot with me.") {
            attribute(GIVEN_FLOUR, "I have given the cook a <red>pot of flour</red>.").strike()
            hasItem("pot_flour", "I have found a pot of flour to give to the cook.")
        }

        objective("I need to find an <red>egg</red>. The cook normally gets from the Groats' farm, found just to the west of the cattle field.") {
            attribute(GIVEN_EGG, "I have given the cook an egg.").strike()
            hasItem("egg", "I have found an egg to give to the cook.")
        }
    }

    override fun completedLog(player: ProtectedAccess): String = completionJournal(player) {
        line("It was the Duke of Lumbridge's birthday, but his cook had forgotten to buy the ingredients he needed to make him a cake.")
        line("I brought the cook an egg, some flour and some milk and the cook made a delicious-looking cake with them.")
        line("As a reward he now lets me use his high-quality range whenever I wish to cook there.")
    }

    private fun hasMilk(player: Player): Boolean =
        player.inv.count("obj.bucket_milk") > 0 || GIVEN_MILK.get(player)

    private fun hasEgg(player: Player): Boolean =
        player.inv.count("obj.egg") > 0 || GIVEN_EGG.get(player)

    private fun hasFlour(player: Player): Boolean =
        player.inv.count(potFlour) > 0 || GIVEN_FLOUR.get(player)

    private fun allItemsDelivered(player: Player): Boolean =
        GIVEN_MILK.get(player) && GIVEN_EGG.get(player) && GIVEN_FLOUR.get(player)

    private suspend fun Dialogue.deliverItem(
        item: String,
        message: String,
        flag: () -> Unit,
    ) {
        if (player.inv.count(item) > 0) {
            access.invDel(access.inv, item)
            flag()
            chatPlayer(happy, message)
        }
    }

    private suspend fun Dialogue.dialogDuringCook(npc: Npc) {
        chatNpc(worried, "How are you getting on with finding the ingredients?")

        if (!hasMilk(player) && !hasEgg(player) && !hasFlour(player)) {
            chatPlayer(sad, "I haven't got any of them yet, I'm still looking.")
            chatNpc(verymad, "Please get the ingredients quickly. I'm running out of time! The Duke will throw me into the streets!")

            when (
                choice2(
                    "I'll get right on it.",
                    1,
                    "Can you remind me how to find these things again?",
                    2,
                )
            ) {
                1 -> chatPlayer(happy, "I'll get right on it.")
                2 -> showIngredientHelp(npc)
            }
            return
        }

        deliverItem("obj.bucket_milk", "Here's a bucket of milk.") { GIVEN_MILK.set(player, true) }
        deliverItem("obj.egg", "Here's a fresh egg.") { GIVEN_EGG.set(player, true) }
        deliverItem(potFlour, "Here's a pot of flour.") { GIVEN_FLOUR.set(player, true) }

        if (allItemsDelivered(player)) {
            questFinishing(npc)
        } else {
            chatNpc(happy, "Thanks for the ingredients you have got so far. Please get the rest quickly - I'm running out of time! The Duke will throw me into the streets!")
            when (
                choice2(
                    "I'll get right on it.",
                    1,
                    "Can you remind me how to find these things again?",
                    2,
                )
            ) {
                1 -> chatPlayer(happy, "I'll get right on it.")
                2 -> showIngredientHelp(npc)
            }
        }
    }

    private suspend fun Dialogue.showIngredientHelp(npc: Npc) {
        when (
            choice4(
                "Where do I find some flour?",
                1,
                "How about milk?",
                2,
                "And eggs? Where are they found?",
                3,
                "I've got all the information I need. Thanks.",
                4,
            )
        ) {
            1 -> {
                if (player.inv.count("obj.pot_empty") > 0) {
                    chatNpc(
                        happy,
                        "Talk to Millie, she'll help, she's a lovely girl and a fine Miller. Make sure you take a pot with you for the flour though, you've got one on you already.",
                    )
                } else {
                    chatNpc(
                        happy,
                        "Talk to Millie, she'll help, she's a lovely girl and a fine Miller. Make sure you take a pot with you for the flour though, there should be one on the table in here.",
                    )
                }
                showIngredientHelp(npc)
            }
            2 -> {
                if (player.inv.count("obj.bucket_empty") > 0) {
                    chatNpc(
                        happy,
                        "You'll need an empty bucket for the milk itself. I do see you've got a bucket with you already luckily!",
                    )
                } else {
                    chatNpc(
                        happy,
                        "You'll need an empty bucket for the milk itself. The general store just north of the castle will sell you one for a couple of coins.",
                    )
                }
                showIngredientHelp(npc)
            }
            3 -> {
                chatNpc(happy, "I normally get my eggs from the Groats' farm, on the other side of the river.")
                chatNpc(happy, "But any chicken should lay eggs.")
                showIngredientHelp(npc)
            }
            4 -> chatPlayer(happy, "I've got all the information I need. Thanks.")
        }
    }

    private suspend fun Dialogue.questFinishing(npc: Npc) {
        chatNpc(happy, "You've brought me everything I need! I am saved! Thank you!")
        chatPlayer(quiz, "So do I get to go to the Duke's Party?")
        chatNpc(sad, "I'm afraid not, only the big cheeses get to dine with the Duke.")
        chatPlayer(bored, "Well, maybe one day I'll be important enough to sit on the Duke's table.")
        chatNpc(short, "Maybe, but I won't be holding my breath.")
        quest.advanceQuestStage(access)
    }

    private suspend fun Dialogue.dialogAfterCook(npc: Npc) {
        chatNpc(happy, "How is the adventuring going, my friend?")

        when (
            choice4(
                "Do you have any other quests for me?",
                1,
                "I am getting strong and mighty.",
                2,
                "I keep on dying.",
                3,
                "Can I use your range?",
                4,
            )
        ) {
            1 -> chatNpc(sad, "I don't have anything for you to do right now, sorry.")
            2 -> {
                chatPlayer(angry, "I am getting strong and mighty. Grrr")
                chatNpc(happy, "Glad to hear it.")
            }
            3 -> {
                chatPlayer(sad, "I keep on dying.")
                chatNpc(laugh, "Ah well, at least you keep coming back to life!")
            }
            4 -> {
                chatPlayer(quiz, "Can I use your range?")
                chatNpc(happy, "Go ahead - it's a very good range. It's easier to use than most other ranges.")
                chatNpc(happy, "It's called the Cook-o-matic 100. It uses a combination of state-of-the-art temperature regulation and magic.")
                chatPlayer(quiz, "Will it mean my food will burn less often?")
                chatNpc(shifty, "Well, that's what the salesman told us anyway...")
                chatPlayer(confused, "Thanks?")
            }
        }
    }

    private suspend fun Dialogue.dialogQuestNotStarted(npc: Npc) {
        chatNpc(worried, "What am I to do?")

        when (
            choice4(
                "What's wrong?",
                1,
                "Can you make me a cake?",
                2,
                "You don't look very happy.",
                3,
                "Nice hat!",
                4,
            )
        ) {
            1 -> cooksWhatsWrong(npc)
            2 -> {
                chatPlayer(quiz, "You're a cook, why don't you bake me a cake?")
                chatNpc(sad, "*sniff* Don't talk to me about cakes...")
                cooksWhatsWrong(npc)
            }
            3 -> {
                chatPlayer(worried, "You don't look very happy.")
                chatNpc(sad, "No, I'm not. The world is caving in around me - I am overcome by dark feelings of impending doom.")
                cooksWhatsWrong(npc)
            }
            4 -> {
                chatPlayer(happy, "Nice hat!")
                chatNpc(confused, "Err thank you. It's a pretty ordinary cook's hat really.")
                chatPlayer(happy, "Still, suits you. The trousers are pretty special too.")
                chatNpc(bored, "It's all standard cook's issue uniform...")
                chatPlayer(laugh, "The whole hat, apron, stripey trousers ensemble - it works. It make you looks like a real cook.")
                chatNpc(angry, "I am a real cook! I haven't got time to be chatting about Culinary Fashion. I am in desperate need of help!")
                cooksWhatsWrong(npc)
            }
        }
    }

    private suspend fun Dialogue.cooksWhatsWrong(npc: Npc) {
        chatPlayer(worried, "What's wrong?")
        chatNpc(verymad, "Oh dear, oh dear, oh dear, I'm in a terrible terrible mess! It's the Duke's birthday and I should be making him a lovely big birthday cake.")
        chatNpc(
            sad,
            "I've forgotten to buy the ingredients. I'll never get them in time now. He'll sack me! What will I do? I have four children and a goat to look after. Would you help me? Please?",
        )

        when (
            choice2(
                "I'm always happy to help a cook in distress.",
                1,
                "I can't right now, maybe later.",
                2,
            )
        ) {
            1 -> {
                chatPlayer(happy, "Yes, I'll help you.")
                quest.advanceQuestStage(access)
                if (
                    player.inv.count("obj.bucket_milk") > 0 &&
                    player.inv.count("obj.egg") > 0 &&
                    player.inv.count(potFlour) > 0
                ) {
                    chatPlayer(happy, "I have all of those ingredients on me already!")
                    chatNpc(quiz, "That's an odd coincidence... Were you planning on making a cake too?")
                    chatPlayer(shifty, "Not exactly. I just had an odd feeling you might be needing these ingredients. If I see a cook, I presume there's food of some kind! Lucky guess I suppose.")
                    chatNpc(happy, "Well thank you! Hand them over, please.")

                    access.invDel(access.inv, "obj.bucket_milk")
                    access.invDel(access.inv, "obj.egg")
                    access.invDel(access.inv, potFlour)

                    GIVEN_FLOUR.set(player, true)
                    GIVEN_EGG.set(player, true)
                    GIVEN_MILK.set(player, true)

                    questFinishing(npc)
                    return
                }
                chatNpc(happy, "Oh thank you, thank you. I need milk, an egg and flour. I'd be very grateful if you can get them for me.")
            }
            2 -> {
                chatPlayer(sad, "No, I don't feel like it. Maybe later.")
                chatNpc(angry, "Fine. I always knew you Adventurer types were callous beasts. Go on your merry way!")
            }
        }
    }
}
