package org.rsmod.content.areas.city.edgeville.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Oziach, who only sells rune platebodies to those who have slain Elvarg. */
class Oziach @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.oziach") { talk(it.npc) }
        onOpNpc3("npc.oziach") { trade(it.npc) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) {
        val slayer = QuestRequirements.hasCompleted(player, DRAGON_SLAYER)
        startDialogue(npc) { if (slayer) dragonSlayer(npc) else fairDay() }
    }

    private suspend fun ProtectedAccess.trade(npc: Npc) {
        if (QuestRequirements.hasCompleted(player, DRAGON_SLAYER)) {
            player.openRunePlateShop(npc)
            return
        }
        startDialogue(npc) {
            chatNpc(angry, "I ain't got nothing to sell ye, adventurer. Leave me be!")
        }
    }

    private suspend fun Dialogue.fairDay() {
        chatNpc(shifty, "Aye, 'tis a fair day my friend.")
        if (choice2("I'm not your friend.", true, "Yes, it's a very nice day.", false)) {
            notYourFriend()
        } else {
            niceDay()
        }
    }

    private suspend fun Dialogue.dragonSlayer(npc: Npc) {
        chatNpc(shifty, "Aye, 'tis a fair day, my mighty dragon-slaying friend.")
        when (
            choice4(
                "Can I buy a rune platebody now, please?",
                1,
                "I'm not your friend.",
                2,
                "Yes, it's a very nice day.",
                3,
                "Can I have another key to Melzar's Maze?",
                4,
            )
        ) {
            1 -> {
                chatPlayer(neutral, "Can I buy a rune platebody now please?")
                player.openRunePlateShop(npc)
            }
            2 -> notYourFriend()
            3 -> niceDay()
            else -> {
                chatPlayer(quiz, "Can I have another key to Melzar's Maze?")
                chatNpc(
                    neutral,
                    "It's the Guildmaster in the Champions' Guild who hands those keys out now. " +
                        "Go talk to him. No need to bother me if you don't need armour.",
                )
            }
        }
    }

    private suspend fun Dialogue.notYourFriend() {
        chatPlayer(angry, "I'm not your friend.")
        chatNpc(angry, "I'm surprised if you're anyone's friend with those kind of manners.")
    }

    private suspend fun Dialogue.niceDay() {
        chatPlayer(neutral, "Yes, it's a very nice day.")
        chatNpc(neutral, "Aye, may the gods walk by yer side. Now leave me alone.")
    }

    private fun Player.openRunePlateShop(npc: Npc) {
        shops.open(this, npc, "Oziach", "inv.runeplateshop")
    }

    private companion object {
        private const val DRAGON_SLAYER = "quest_dragonslayer1"
    }
}
