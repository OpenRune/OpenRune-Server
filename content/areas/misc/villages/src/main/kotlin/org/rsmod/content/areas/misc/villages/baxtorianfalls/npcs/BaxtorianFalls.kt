package org.rsmod.content.areas.misc.villages.baxtorianfalls.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class BaxtorianFalls @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.almera_waterfall_quest") { startDialogue(it.npc) { almera() } }
        onOpNpc1("npc.hudon_waterfall_quest") { startDialogue(it.npc) { hudon() } }
        onOpNpc1("npc.gerald_waterfall_quest") { startDialogue(it.npc) { gerald() } }
        onOpNpc1("npc.hadley_waterfall_quest") { startDialogue(it.npc) { hadley() } }
        onOpNpc1("npc.shadow_warrior_rasool") { startDialogue(it.npc) { rasolo(it.npc) } }
        onOpNpc3("npc.shadow_warrior_rasool") { player.openRasoloShop(it.npc) }
    }

    private fun Player.openRasoloShop(npc: Npc) {
        shops.open(this, npc, "Rasolo the Wandering Merchant", "inv.rasoolshop1")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Almera
    private suspend fun Dialogue.almera() {
        if (QuestRequirements.hasCompleted(player, WATERFALL_QUEST)) {
            chatPlayer(happy, "Hello Almera.")
            chatNpc(happy, "Hello adventurer, how's your treasure hunt going?")
            chatPlayer(neutral, "Oh, I'm just sight seeing.")
            chatNpc(
                neutral,
                "No adventurer stays here this long just to sight see. But your business is " +
                    "yours alone, if you need to use the raft go ahead. But please try not crash " +
                    "it this time!",
            )
            chatPlayer(happy, "Thanks Almera.")
            return
        }
        // TODO(content): [quest:waterfall_quest] quest start and in-progress dialogue
        chatPlayer(happy, "Hello.")
        chatNpc(
            happy,
            "Ah, hello there. Nice to see an outsider for a change. Are you busy? I have a " +
                "problem.",
        )
        if (choice2("Yes.", 1, "No.", 2, title = "Start Waterfall Quest?") == 1) {
            return
        }
        chatPlayer(neutral, "I'm afraid I'm in a rush.")
        chatNpc(sad, "Oh okay, never mind.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hudon
    private suspend fun Dialogue.hudon() {
        if (QuestRequirements.hasCompleted(player, WATERFALL_QUEST)) {
            chatPlayer(happy, "Hello again.")
            chatNpc(angry, "You stole my treasure. I saw you!")
            chatPlayer(neutral, "I'll make sure it goes to a good cause.")
            chatNpc(angry, "Hmmmm!")
            return
        }
        // TODO(content): [quest:waterfall_quest] in-progress dialogue
        mesbox(
            "Hudon can't hear you because of the noise of the waterfall. Perhaps the acoustics " +
                "would be better from that island?"
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gerald (before Waterfall Quest)
    // TODO(content): [quest:waterfall_quest] treasure hunter line once the treasure is known
    private suspend fun Dialogue.gerald() {
        chatPlayer(happy, "Hello there.")
        chatNpc(
            happy,
            "Good day to you traveller, are you here to fish or just looking around? I've caught " +
                "some beauties down here.",
        )
        chatPlayer(quiz, "Really?")
        chatNpc(happy, "The last one was this big!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hadley
    // TODO(content): [quest:waterfall_quest] dialogue after learning about the treasure
    private suspend fun Dialogue.hadley() {
        val title = if (access.isBodyTypeA()) "sir." else "lady."
        chatPlayer(happy, "Hello there.")
        chatNpc(
            happy,
            "Well hello, come in, come in, my name's Hadley, I'm head of tourism here. There's " +
                "some of the most unspoilt wildlife and scenery in Gielinor here. People come " +
                "from miles around to fish in the clear lakes or to wander the",
        )
        chatNpc(happy, "beautiful hillsides.")
        chatPlayer(neutral, "It is quite pretty.")
        chatNpc(
            happy,
            "Surely pretty is an understatement, $title Beautiful, amazing or possibly " +
                "life-changing would be more suitable wording. Have you seen Baxtorian Falls? " +
                "Named after the elven king who was buried beneath them.",
        )
        while (true) {
            val choice =
                choice4(
                    "What happened to the elven king?",
                    1,
                    "Where else is worth visiting around here?",
                    2,
                    "I don't like nature, it gives me a rash!",
                    3,
                    "Thanks, goodbye.",
                    4,
                )
            when (choice) {
                1 -> {
                    chatPlayer(quiz, "What happened to the elven king?")
                    chatNpc(
                        sad,
                        "Baxtorian? I guess he died a long long time ago, it's quite sad really. " +
                            "After leaving his kingdom to deal with invading forces, Baxtorian " +
                            "returned to find his wife Glarial had been captured by the enemy.",
                    )
                    chatNpc(
                        sad,
                        "This destroyed Baxtorian, after years of searching he became a recluse. " +
                            "He went into the secret home he had made for Glarial under the " +
                            "waterfall and sealed himself in. To this day, no one has managed " +
                            "to enter.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Where else is worth visiting around here?")
                    chatNpc(
                        neutral,
                        "Well, there's a wide variety of wildlife, although unfortunately most " +
                            "of it's quite dangerous. Please don't feed the goblins.",
                    )
                    chatPlayer(neutral, "Okay.")
                    chatNpc(
                        happy,
                        "There is a lovely spot for a picnic on the hill to the north east, " +
                            "there's a monument to the elven queen Glarial. It really is quite " +
                            "pretty.",
                    )
                }
                3 -> {
                    chatPlayer(angry, "I don't like nature, it gives me a rash!")
                    chatNpc(neutral, "That's just silly talk.")
                }
                else -> {
                    chatPlayer(happy, "Thanks, goodbye.")
                    chatNpc(happy, "Enjoy your visit.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Rasolo
    private suspend fun Dialogue.rasolo(npc: Npc) {
        if (QuestRequirements.hasCompleted(player, DESERT_TREASURE)) {
            rasoloAfterQuest(npc)
            return
        }
        chatNpc(
            happy,
            "Greetings friend. I am Rasolo, the famous merchant. Would you care to see my wares?",
        )
        if (choice2("Yes", 1, "No", 2) == 1) {
            player.openRasoloShop(npc)
            return
        }
        chatPlayer(neutral, "No, not really.")
        chatNpc(neutral, "As you wish. I will travel wherever the business takes me.")
    }

    private suspend fun Dialogue.rasoloAfterQuest(npc: Npc) {
        chatNpc(
            happy,
            "Many thanks for returning my heirloom to me, adventurer. Would you like to buy " +
                "something?",
        )
        when (choice3("Yes", 1, "No", 2, "I lost that ring you gave me...", 3)) {
            1 -> player.openRasoloShop(npc)
            2 -> {
                chatPlayer(neutral, "No, not really.")
                chatNpc(neutral, "As you wish. I will travel wherever the business takes me.")
            }
            3 -> lostRing()
        }
    }

    private suspend fun Dialogue.lostRing() {
        chatPlayer(sad, "I lost that ring you gave me...")
        val bank = player.invMap["inv.bank"]
        val hasRing =
            RING_OF_VISIBILITY in player.inv || (bank != null && RING_OF_VISIBILITY in bank)
        if (QuestRequirements.hasCompleted(player, DESERT_TREASURE_II) || hasRing) {
            chatNpc(confused, "Are you sure? I don't have it anymore...")
            return
        }
        if (player.inv.isFull()) {
            chatNpc(
                neutral,
                "Well, should you need a replacement, I suggest you free up some inventory room " +
                    "first...",
            )
            return
        }
        chatNpc(
            happy,
            "Then by all means, take another. Only a foolish merchant would give away his only " +
                "stock!",
        )
        player.invAdd(player.inv, RING_OF_VISIBILITY)
    }

    private companion object {
        const val WATERFALL_QUEST = "quest_waterfall"
        const val DESERT_TREASURE = "quest_deserttreasure"
        const val DESERT_TREASURE_II = "quest_deserttreasure2"
        const val RING_OF_VISIBILITY = "obj.fd_ring_visibility"
    }
}
