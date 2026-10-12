package org.rsmod.content.areas.city.ardougne.npcs

import jakarta.inject.Inject
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
class KandarinOutposts @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.lathas_shopkeeper") { startDialogue(it.npc) { campShopKeeper(it.npc) } }
        onOpNpc3("npc.lathas_shopkeeper") { player.openArmoury(it.npc) }
        onOpNpc1("npc.makinghistory_jorral") { startDialogue(it.npc) { jorral() } }
    }

    private fun Player.openArmoury(npc: Npc) {
        shops.open(this, npc, "Armoury", "inv.lathastrainingstore")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Shop_keeper_(Combat_Training_Camp)
    private suspend fun Dialogue.campShopKeeper(npc: Npc) {
        chatPlayer(happy, "Hello.")
        if (QuestRequirements.hasCompleted(player, SONG_OF_THE_ELVES)) {
            chatNpc(
                happy,
                "So, are you looking to buy weapons? King Thoros keeps us very well stocked.",
            )
        } else {
            chatNpc(
                happy,
                "So, are you looking to buy weapons? King Lathas keeps us very well stocked.",
            )
        }
        when (choice2("What do you have?", 1, "No thanks", 2)) {
            1 -> {
                chatPlayer(quiz, "What do you have?")
                chatNpc(happy, "Take a look.")
                player.openArmoury(npc)
            }
            2 -> chatPlayer(neutral, "No thanks.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jorral
    // TODO(content): [quest:making_history] the quest offer for a player who meets its
    //  requirements
    private suspend fun Dialogue.jorral() {
        if (QuestRequirements.hasCompleted(player, MAKING_HISTORY)) {
            chatNpc(happy, "Thanks for the help again. Feel free to browse the room next door!")
            return
        }
        chatPlayer(happy, "Hi there.")
        chatNpc(sad, "All is lost!")
        chatPlayer(confused, "Sorry?")
        chatNpc(angry, "Don't bother, you're not experienced enough to help me!")
    }

    private companion object {
        const val SONG_OF_THE_ELVES = "quest_songoftheelves"
        const val MAKING_HISTORY = "quest_makinghistory"
    }
}
