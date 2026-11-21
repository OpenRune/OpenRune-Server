package org.rsmod.content.minigames.sepulchre

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.shops.Shops
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The Mysterious Stranger in the Sepulchre's lobby
 * (https://oldschool.runescape.wiki/w/Mysterious_Stranger_(Hallowed_Sepulchre)). The first talk
 * explains the Sepulchre and hands over five crystal shards; until then he turns the player away
 * from the stairs. He trades the hallowed marks for his goods.
 */
class MysteriousStrangerScript
@Inject
constructor(private val shops: Shops, private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(STRANGER_NEW) { startDialogue(it.npc) { introduce() } }
        onOpNpc1(STRANGER) { startDialogue(it.npc) { greet() } }
        onOpNpc3(STRANGER) { openShop() }
        // TODO(content): Collect, the items of a player who died in the Sepulchre.
        onOpNpc4(STRANGER) { startDialogue(it.npc) { noItems() } }
    }

    private fun ProtectedAccess.openShop() {
        shops.open(
            player = player,
            title = SHOP_TITLE,
            shopInv = SHOP,
            buyPercentage = PRICE_PERCENTAGE,
            sellPercentage = PRICE_PERCENTAGE,
            changePercentage = 0.0,
            currency = MARKS,
        )
    }

    private suspend fun Dialogue.introduce() {
        chatNpc(
            neutral,
            "Welcome adventurer, to the Hallowed Sepulchre. Are you here to claim some of the " +
                "treasure within? It seems like the kind of activity one like you would " +
                "appreciate.",
        )
        chatPlayer(quiz, "Treasure? What treasure? And what do you mean by 'one like me'?")
        chatNpc(
            shifty,
            "I mean you're not one of the inhabitants of this city. We could use someone of " +
                "your talents.",
        )
        chatPlayer(quiz, "Oh? I can't say I'm not interested. What do you propose?")
        chatNpc(
            neutral,
            "This place was built long before the city of Darkmeyer existed, back when this " +
                "land was known as Hallowvale.",
        )
        chatNpc(
            neutral,
            "The Sepulchre was used as a resting place for Hallowvale's loyal followers of " +
                "Saradomin.",
        )
        chatNpc(
            neutral,
            "Before they were driven out by the vampyres, the Saradominists used their magic " +
                "to safeguard this place.",
        )
        chatPlayer(quiz, "Safeguard how?")
        chatNpc(
            shifty,
            "The dead here were buried with the wealth they had in life. To protect these " +
                "riches, the Saradominists enchanted the statues within the Sepulchre to create " +
                "various defences.",
        )
        chatNpc(shifty, "Many have tried and failed to obtain the prizes buried here.")
        chatPlayer(
            quiz,
            "Okay... So I presume you want me to get some of this treasure for you? How should " +
                "I go about doing it?",
        )
        chatNpc(
            neutral,
            "We don't care for a lot of what is kept here. We are only interested in the old " +
                "currency of this land.",
        )
        objbox(MARK, "The Mysterious Stranger pulls a coin out of his pocket and shows it to you.")
        chatNpc(
            neutral,
            "If you find any of these Hallowed marks, I'd be willing to take them off your " +
                "hands. In exchange, I can provide tools that can help you within the Hallowed " +
                "Sepulchre.",
        )
        chatNpc(neutral, "You can keep whatever else you find for yourself.")
        chatNpc(
            neutral,
            "You should know, only the most agile of adventurers will have success down " +
                "there. You'll need to use your reflexes to overcome the traps within.",
        )
        chatNpc(
            neutral,
            "The Sepulchre has five floors in total, each more dangerous than the last. I " +
                "hope you prove competent enough to make it all the way down.",
        )
        chatPlayer(quiz, "Do you have any information about what kind of traps I can expect?")
        chatNpc(
            quiz,
            "The statues have been enchanted to shoot fire, lightning and arrows. Some also " +
                "throw swords. There are also tiles that when stepped on, will teleport you " +
                "somewhere else.",
        )
        chatPlayer(
            happy,
            "Sounds like the perfect challenge for me! Is there anything else I should know?",
        )
        chatNpc(
            shifty,
            "Yes... It seems that there is another safeguard which prevents anyone from " +
                "spending too long within the Sepulchre.",
        )
        chatNpc(
            shifty,
            "You'll only have so much time before the magic within seals both the coffins and " +
                "the stairs leading to the next floor.",
        )
        access.player.tutorialDone = true
        chatPlayer(
            quiz,
            "Hang on a second... If the doors to the next floor close, then how will I escape?",
        )
        chatNpc(
            shifty,
            "The priests who built this place had a teleportation system. The magical obelisks " +
                "they used can be found at the end of each floor. These obelisks can be used to " +
                "transport you back here.",
        )
        chatNpc(
            shifty,
            "However, we have found that the obelisks only respond to those they perceive to be " +
                "followers of Saradomin. I'd strongly advise against wearing items of any other " +
                "god.",
        )
        chatPlayer(
            quiz,
            "I see, but what if I'm in the middle of a floor and can't make it to an obelisk?",
        )
        chatNpc(
            shifty,
            "I see you are cautious adventurer. That is wise. You can use any teleport spell to " +
                "escape from the Hallowed Sepulchre. However, you may also find these of use.",
        )
        if (!access.player.teleportsReceived) {
            access.player.teleportsReceived = true
            access.invAddOrDrop(objRepo, SHARDS, SHARD_GIFT)
        }
        objbox(SHARDS, "The Mysterious Stranger hands you some strange crystal shards.")
        chatNpc(
            shifty,
            "These teleport crystals were enchanted using the magic of the obelisks. They can be " +
                "used to teleport to this lobby. I can sell you more when you run out.",
        )
        chatPlayer(happy, "Thanks! I'll make sure to put them to good use.")
        chatPlayer(
            neutral,
            "Thanks for the information. I'll keep an eye out for some of those Hallowed marks.",
        )
        chatNpc(happy, "That would be... appreciated.")
        options()
    }

    // UNVERIFIED: the greeting on later talks, from the wiki's transcript.
    private suspend fun Dialogue.greet() {
        chatNpc(
            neutral,
            "I see you're still alive adventurer. Do you have any Hallowed marks for me?",
        )
        options()
    }

    private suspend fun Dialogue.options() {
        when (
            choice4(
                "Tell me about yourself.",
                ABOUT,
                "I'd like to trade in some Hallowed marks.",
                TRADE,
                "Do you have any of my items?",
                ITEMS,
                "More options...",
                MORE,
                title = "Select an option",
            )
        ) {
            // TODO(content): his story, from the wiki's transcript.
            ABOUT -> chatPlayer(quiz, "Tell me about yourself.")
            TRADE -> {
                chatPlayer(neutral, "I'd like to trade in some Hallowed marks.")
                access.openShop()
            }
            ITEMS -> {
                chatPlayer(quiz, "Do you have any of my items?")
                noItems()
            }
            // TODO(content): the second page: tool storage, private access and leaving.
            MORE -> Unit
        }
    }

    private suspend fun Dialogue.noItems() {
        chatNpc(
            neutral,
            "I do not have anything for you. If I did have any of your items, but you died " +
                "before collecting them, they'll now be lost.",
        )
    }

    private companion object {
        const val STRANGER = "npc.hallowed_lobby_npc_3op"
        const val STRANGER_NEW = "npc.hallowed_lobby_npc_1op"
        const val SHOP = "inv.hallowed_rewardshop"
        const val SHOP_TITLE = "Mysterious Hallowed Goods"
        const val MARKS = "currency.hallowed_marks"
        const val MARK = "obj.hallowed_mark"
        const val SHARDS = "obj.hallowed_teleport"
        const val SHARD_GIFT = 5
        const val PRICE_PERCENTAGE = 100.0
        const val ABOUT = 1
        const val TRADE = 2
        const val ITEMS = 3
        const val MORE = 4
    }
}
