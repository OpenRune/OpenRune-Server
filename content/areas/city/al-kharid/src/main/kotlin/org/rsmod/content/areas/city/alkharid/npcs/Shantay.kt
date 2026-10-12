package org.rsmod.content.areas.city.alkharid.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.shops.Shops
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Shantay
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:shantay_pass] first-talk greeting ("I see you're new.") needs a varbit
// TODO(content): [system:treasure_trails] beginner clue reward
class Shantay
@Inject
constructor(private val shops: Shops, private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(SHANTAY) { talk(it.npc) }
        onOpNpc3(SHANTAY) { player.openShantayShop(it.npc) }
        onOpNpc4(SHANTAY) { startDialogue(it.npc) { buyPass() } }
    }

    private fun Player.openShantayShop(npc: Npc) {
        shops.open(this, npc, "Shantay Pass Shop", "inv.shantayshop")
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            chatNpc(
                happy,
                "Hello again friend. Please read the billboard poster before going into the " +
                    "desert. It'll give yer details on the dangers you can face.",
            )
            if (QuestRequirements.isOnQuest(player, TOURIST_TRAP)) {
                chatNpc(
                    happy,
                    "Hope the quest is going well friend. Irena told me you were looking for Ana. " +
                        "Very brave of you!",
                )
            } else if (QuestRequirements.hasNotCompleted(player, TOURIST_TRAP)) {
                chatNpc(
                    sad,
                    "There is a heartbroken mother just past the gates and in the desert. Her " +
                        "name is Irena and she mourns her lost daughter. Such a shame.",
                )
            }
            mainMenu(npc)
        }

    private suspend fun Dialogue.mainMenu(npc: Npc) {
        val option =
            choice4(
                "What is this place?",
                1,
                "Can I see what you have to sell please?",
                2,
                "I must be going.",
                3,
                "I want to buy a Shantay pass for 5 gold coins.",
                4,
            )
        when (option) {
            1 -> whatIsThisPlace(npc)
            2 -> seeShop(npc)
            3 -> mustBeGoing()
            4 -> {
                chatPlayer(quiz, "I want to buy a Shantay pass for 5 gold coins.")
                buyPass()
            }
        }
    }

    private suspend fun Dialogue.whatIsThisPlace(npc: Npc) {
        chatPlayer(quiz, "What is this place?")
        chatNpc(
            neutral,
            "This is the pass of Shantay. I guard this area with my men. I am responsible for " +
                "keeping this pass open and repaired.",
        )
        chatNpc(
            neutral,
            "My men and I prevent outlaws from getting out of the desert. And we stop the " +
                "inexperienced from a dry death in the sands. Which would you say you were?",
        )
        val option =
            choice3(
                "I am definitely an outlaw, prepare to die!",
                1,
                "I am a little inexperienced.",
                2,
                "Er, neither, I'm an adventurer.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(angry, "I am definitely an outlaw, prepare to die!")
                // TODO(content): [system:shantay_jail] arrest, five gold fine or Port Sarim prison
                chatNpc(laugh, "Ha, very funny.....")
            }
            2 -> {
                chatPlayer(worried, "I am a little inexperienced.")
                chatNpc(
                    neutral,
                    "Can I recommend that you purchase a full waterskin and a knife! These items " +
                        "will no doubt save your life. A waterskin will keep water from " +
                        "evaporating in the desert.",
                )
                chatNpc(
                    neutral,
                    "And a keen woodsman with a knife can extract the juice from a cactus. " +
                        "Before you go into the desert, it's advisable to wear desert clothes. " +
                        "It's very hot in the desert and you'll surely cook if you wear armour.",
                )
                chatNpc(
                    neutral,
                    "To keep the pass bandit free, we charge a small toll of five gold pieces. " +
                        "You can buy a desert pass from me, just ask me to open the shop. You " +
                        "can also use our free banking services by clicking on the chest.",
                )
                tollMenu(npc)
            }
            3 -> {
                chatPlayer(happy, "Er, neither, I'm an adventurer.")
                chatNpc(
                    happy,
                    "Great, I have just the thing for the desert adventurer. I sell desert " +
                        "clothes which will keep you cool in the heat of the desert. I also " +
                        "sell waterskins so that you won't die in the desert.",
                )
                chatNpc(
                    neutral,
                    "A waterskin and a knife help you survive from the juice of a cactus. Use " +
                        "the chest to store your items, we'll take them to the bank. It's hot " +
                        "in the desert, you'll bake in all that armour.",
                )
                chatNpc(
                    neutral,
                    "To keep the pass open we ask for 5 gold pieces. And we give you a Shantay " +
                        "Pass, just ask to see what I sell to buy one.",
                )
                tollMenu(npc)
            }
        }
    }

    private suspend fun Dialogue.tollMenu(npc: Npc) {
        val option =
            choice3(
                "Can I see what you have to sell please?",
                1,
                "I must be going.",
                2,
                "Why do I have to pay to go into the desert?",
                3,
            )
        when (option) {
            1 -> seeShop(npc)
            2 -> mustBeGoing()
            3 -> {
                chatPlayer(quiz, "Why do I have to pay to go into the desert?")
                chatNpc(
                    angry,
                    "Effendi, you insult me! I am not interested in making a profit from you! I " +
                        "merely seek to cover my expenses in keeping this pass open.",
                )
                chatNpc(
                    neutral,
                    "There is repair work to carry out and also the men's wages to consider. For " +
                        "the paltry sum of 5 Gold pieces, I think we offer a great service.",
                )
                mainMenu(npc)
            }
        }
    }

    private suspend fun Dialogue.seeShop(npc: Npc) {
        chatPlayer(quiz, "Can I see what you have to sell please?")
        chatNpc(happy, "Absolutely Effendi!")
        player.openShantayShop(npc)
    }

    private suspend fun Dialogue.mustBeGoing() {
        chatPlayer(neutral, "I must be going.")
        chatNpc(neutral, "So long...")
    }

    private suspend fun Dialogue.buyPass() {
        if (access.invTotal(access.inv, COINS) < PASS_PRICE) {
            chatNpc(
                sad,
                "Sorry friend, the Shantay Pass is 5 gold coins. You don't seem to have enough " +
                    "money!",
            )
            return
        }
        if (player.inv.isFull()) {
            chatNpc(sad, "Sorry friend, you'll need more inventory space to buy a pass.")
            return
        }
        if (player.invTakeFee(PASS_PRICE)) {
            player.invAddOrDrop(objRepo, "obj.shantay_pass")
            player.mes("You purchase a Shantay Pass.")
        }
    }

    private companion object {
        const val SHANTAY = "npc.shantay"
        const val COINS = "obj.coins"
        const val PASS_PRICE = 5
        const val TOURIST_TRAP = "quest_touristtrap"
    }
}
