package org.alter.quest

import org.alter.api.Skills
import org.alter.api.ext.chatNpc
import org.alter.api.ext.chatPlayer
import org.alter.api.ext.options
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.skill.Skill
import org.alter.game.pluginnew.event.impl.LoginEvent
import org.alter.game.pluginnew.event.impl.NpcClickEvent
import org.alter.quest.manager.QuestProgressState
import org.alter.quest.manager.QuestReward
import org.alter.quest.manager.QuestScript
import org.alter.quest.manager.rewards
import org.alter.rscm.RSCM.asRSCM

class CooksAssistant : QuestScript("dbrows.quest_cooksassistant", "varp.cookquest", rewards {
    xp(Skills.COOKING, 300)
}) {

    private val GIVEN_EGG = quest.attribute(name = "GIVEN_EGG", default = false)
    private val GIVEN_MILK = quest.attribute(name = "GIVEN_MILK", default = false)
    private val GIVEN_FLOUR = quest.attribute(name = "GIVEN_FLOUR", default = false)

    override fun init() {
        on<NpcClickEvent> {
            where { npc.id == "npcs.cook".asRSCM() }
            then {
                player.queue {
                    when {
                        quest.isQuestCompleted(player) -> dialogAfterCook(player)
                        quest.questState(player) == QuestProgressState.IN_PROGRESS -> dialogDuringCook(player)
                        else -> dialogQuestNotStarted(player)
                    }
                }
            }
        }
    }

    override fun subTitle(): String {
        return "talking to the <col=800000>Cook</col> in <col=800000>Lumbridge Castle</col>."
    }

    override fun questLog(player: Player) = questJournal(player) {
        description("It's the <red>Duke of Lumbridge's</red> birthday and I have to help his <red>Cook</red> make him a <red>birthday cake</red>. To do this I need to bring him the following ingredients:")

        objective("I need to find a <red>bucket of milk</red>. There's a cattle field east of Lumbridge, I should make sure I take an empty bucket with me.") {
            attribute(GIVEN_MILK, "I have given the cook a <red>bucket of milk</red>.").strike()
            hasItem("items.bucket_milk", "I have found a <red>bucket of milk</red> to give to the cook.")
        }

        objective("I need to find a <red>pot of flour</red>. There's a mill found north-west of Lumbridge, I should take an empty pot with me.") {
            attribute(GIVEN_FLOUR, "I have given the cook a <red>pot of flour</red>.").strike()
            hasItem("items.pot_flour", "I have found a pot of flour to give to the cook.")
        }

        objective("I need to find an <red>egg</red>. The cook normally gets from the Groats' farm, found just to the west of the cattle field.") {
            attribute(GIVEN_EGG, "I have given the cook an egg.").strike()
            hasItem("items.egg", "I have found an egg to give to the cook.")
        }
    }

    override fun completedLog(player: Player): String =
        completionJournal(player) {
            line("It was the Duke of Lumbridge's birthday, but his cook had forgotten to buy the ingredients he needed to make him a cake.")
            line("I brought the cook an egg, some flour and some milk and the cook made a delicious-looking cake with them.")
            line("As a reward he now lets me use his high-quality range whenever I wish to cook there.")
        }


    private suspend fun QueueTask.deliverItem(player: Player, itemId: String, message: String, flag: () -> Unit) {
        if (player.inventory.contains(itemId)) {
            player.inventory.remove(itemId)
            flag()
            chatPlayer(player, message)
        }
    }

    private fun allItemsDelivered(player: Player) =
        GIVEN_MILK.get(player) && GIVEN_EGG.get(player) && GIVEN_FLOUR.get(player)

    suspend fun QueueTask.dialogDuringCook(player: Player) {
        chatNpc(player, "How are you getting on with finding the ingredients?", "npcs.cook".asRSCM())

        val hasMilk = player.inventory.contains("items.bucket_milk") || GIVEN_MILK.get(player)
        val hasEgg = player.inventory.contains("items.egg") || GIVEN_EGG.get(player)
        val hasFlour = player.inventory.contains("items.pot_flour") || GIVEN_FLOUR.get(player)

        if (!hasMilk && !hasEgg && !hasFlour) {
            chatPlayer(player, "I haven't got any of them yet, I'm still looking.")
            chatNpc(player, "Please get the ingredients quickly. I'm running out of time! The Duke will throw me into the streets!", "npcs.cook".asRSCM())

            when (options(player, "I'll get right on it.", "Can you remind me how to find these things again?")) {
                1 -> chatPlayer(player, "I'll get right on it.")
                2 -> showIngredientHelp(player)
            }
            return
        }

        // Deliver items
        deliverItem(player, "items.bucket_milk", "Here's a bucket of milk.") { GIVEN_MILK.set(player, true) }
        deliverItem(player, "items.egg", "Here's a fresh egg.") { GIVEN_EGG.set(player, true) }
        deliverItem(player, "items.pot_flour", "Here's a pot of flour.") { GIVEN_FLOUR.set(player, true) }

        if (allItemsDelivered(player)) {
            questFinshing(player)
        } else {
            chatNpc(player, "Thanks for the ingredients you have got so far. Please get the rest quickly - I'm running out of time! The Duke will throw me into the streets!", "npcs.cook".asRSCM())
            when (options(player, "I'll get right on it.", "Can you remind me how to find these things again?")) {
                1 -> chatPlayer(player, "I'll get right on it.")
                2 -> showIngredientHelp(player)
            }
        }
    }

    private suspend fun QueueTask.showIngredientHelp(player: Player) {
        when (options(player, "Where do I find some flour?", "How about milk?", "And eggs? Where are they found?", "I've got all the information I need. Thanks.")) {
            1 -> {
                if (player.inventory.contains("items.pot_empty")) {
                    chatNpc(player, "Talk to Millie, she'll help, she's a lovely girl and a fine Miller. Make sure you take a pot with you for the flour though, you've got one on you already.", "npcs.cook".asRSCM())
                } else {
                    chatNpc(player, "Talk to Millie, she'll help, she's a lovely girl and a fine Miller. Make sure you take a pot with you for the flour though, there should be one on the table in here.", "npcs.cook".asRSCM())
                }
                showIngredientHelp(player)
            }
            2 -> {
                if (player.inventory.contains("items.bucket_empty")) {
                    chatNpc(player, "You'll need an empty bucket for the milk itself. I do see you've got a bucket with you already luckily!", "npcs.cook".asRSCM())
                } else {
                    chatNpc(player, "You'll need an empty bucket for the milk itself. The general store just north of the castle will sell you one for a couple of coins.", "npcs.cook".asRSCM())
                }
                showIngredientHelp(player)
            }
            3 -> {
                chatNpc(player, "I normally get my eggs from the Groats' farm, on the other side of the river.", "npcs.cook".asRSCM())
                chatNpc(player, "But any chicken should lay eggs.", "npcs.cook".asRSCM())
                showIngredientHelp(player)
            }
            4 -> {
                chatPlayer(player, "I've got all the information I need. Thanks.")
            }
        }
    }

    suspend fun QueueTask.questFinshing(player: Player) {
        chatNpc(player, "You've brought me everything I need! I am saved! Thank you!", "npcs.cook".asRSCM())
        chatPlayer(player, "So do I get to go to the Duke's Party?")
        chatNpc(player, "I'm afraid not, only the big cheeses get to dine with the Duke.", "npcs.cook".asRSCM())
        chatPlayer(player, "Well, maybe one day I'll be important enough to sit on the Duke's table.")
        chatNpc(player, "Maybe, but I won't be holding my breath.", "npcs.cook".asRSCM())
        quest.advanceQuestStage(player)
    }

    suspend fun QueueTask.dialogAfterCook(player: Player) {
        chatNpc(player, "How is the adventuring going, my friend?", "npcs.cook".asRSCM())

        when (options(player, "Do you have any other quests for me?", "I am getting strong and mighty.", "I keep on dying.", "Can I use your range?")) {
            1 -> chatNpc(player, "I don't have anything for you to do right now, sorry.", "npcs.cook".asRSCM())
            2 -> {
                chatPlayer(player, "I am getting strong and mighty. Grrr");
                chatNpc(player, "Glad to hear it.", "npcs.cook".asRSCM()) }
            3 -> {
                chatPlayer(player, "I keep on dying.");
                chatNpc(player, "Ah well, at least you keep coming back to life!", "npcs.cook".asRSCM()) }
            4 -> {
                chatPlayer(player, "Can I use your range?")
                chatNpc(player, "Go ahead - it's a very good range. It's easier to use than most other ranges.", "npcs.cook".asRSCM())
                chatNpc(player, "It's called the Cook-o-matic 100. It uses a combination of state-of-the-art temperature regulation and magic.", "npcs.cook".asRSCM())
                chatPlayer(player, "Will it mean my food will burn less often?")
                chatNpc(player, "Well, that's what the salesman told us anyway...", "npcs.cook".asRSCM())
                chatPlayer(player, "Thanks?")
            }
        }
    }

    suspend fun QueueTask.dialogQuestNotStarted(player: Player) {
        chatNpc(player, "What am I to do?", "npcs.cook".asRSCM())

        when (options(player, "What's wrong?", "Can you make me a cake?", "You don't look very happy.", "Nice hat!")) {
            1 -> cooksWhatsWrong(player)
            2 -> {
                chatPlayer(player, "You're a cook, why don't you bake me a cake?")
                chatNpc(player, "*sniff* Don't talk to me about cakes...", "npcs.cook".asRSCM())
                cooksWhatsWrong(player)
            }
            3 -> {
                chatPlayer(player, "You don't look very happy.")
                chatNpc(player, "No, I'm not. The world is caving in around me - I am overcome by dark feelings of impending doom.", "npcs.cook".asRSCM())
                cooksWhatsWrong(player)
            }
            4 -> {
                chatPlayer(player, "Nice hat!")
                chatNpc(player, "Err thank you. It's a pretty ordinary cook's hat really.", "npcs.cook".asRSCM())
                chatPlayer(player, "Still, suits you. The trousers are pretty special too.")
                chatNpc(player, "It's all standard cook's issue uniform...", "npcs.cook".asRSCM())
                chatPlayer(player, "The whole hat, apron, stripey trousers ensemble - it works. It make you looks like a real cook.")
                chatNpc(player, "I am a real cook! I haven't got time to be chatting about Culinary Fashion. I am in desperate need of help!", "npcs.cook".asRSCM())
                cooksWhatsWrong(player)
            }
        }
    }

    suspend fun QueueTask.cooksWhatsWrong(player: Player) {
        chatPlayer(player, "What's wrong?")
        chatNpc(player, "Oh dear, oh dear, oh dear, I'm in a terrible terrible mess! It's the Duke's birthday and I should be making him a lovely big birthday cake.", "npcs.cook".asRSCM())
        chatNpc(player, "I've forgotten to buy the ingredients. I'll never get them in time now. He'll sack me! What will I do? I have four children and a goat to look after. Would you help me? Please?", "npcs.cook".asRSCM())

        when (options(player, "I'm always happy to help a cook in distress.", "I can't right now, maybe later.")) {
            1 -> {
                chatPlayer(player, "Yes, I'll help you.")
                quest.advanceQuestStage(player)
                if (player.inventory.contains("items.bucket_milk","items.egg","items.pot_flour")) {
                    chatPlayer(player, "I have all of those ingredients on me already!")
                    chatNpc(player, "That's an odd coincidence... Were you planning on making a cake too?", "npcs.cook".asRSCM())
                    chatPlayer(player, "Not exactly. I just had an odd feeling you might be needing these ingredients. If I see a cook, I presume there's food of some kind! Lucky guess I suppose.")
                    chatNpc(player, "Well thank you! Hand them over, please.", "npcs.cook".asRSCM())

                    // Remove items and finish quest
                    player.inventory.remove("items.bucket_milk")
                    player.inventory.remove("items.egg")
                    player.inventory.remove("items.pot_flour")

                    GIVEN_FLOUR.set(player, true)
                    GIVEN_EGG.set(player, true)
                    GIVEN_MILK.set(player, true)

                    questFinshing(player)
                    return
                }
                chatNpc(player, "Oh thank you, thank you. I need milk, an egg and flour. I'd be very grateful if you can get them for me.", "npcs.cook".asRSCM())
            }
            2 -> {
                chatPlayer(player, "No, I don't feel like it. Maybe later.")
                chatNpc(player, "Fine. I always knew you Adventurer types were callous beasts. Go on your merry way!", "npcs.cook".asRSCM())
            }
        }
    }

}