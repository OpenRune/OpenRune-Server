package org.rsmod.content.areas.city.rellekka.miscellania.npcs.shops

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

// https://oldschool.runescape.wiki/w/Transcript:Runa
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class EsotericanArms @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(RUNA) { startDialogue(it.npc) { runa(it.npc) } }
        onOpNpc3(RUNA) { player.openBar(it.npc) }
    }

    private fun Player.openBar(npc: Npc) {
        shops.open(this, npc, "The Esoterican Arms", "inv.royal_bar")
    }

    private suspend fun Dialogue.runa(npc: Npc) {
        chatNpc(happy, "Would you like to try some fine Miscellanian ale, Your Royal Highness?")
        chatNpc(shifty, "Well, I say Miscellanian, but it's actually brewed on the mainland.")
        chatNpc(happy, "Would you like to try some anyway?")
        val option =
            choice3(
                "Yes, please.",
                1,
                "No, thank you.",
                2,
                "What's it like living down here?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "Yes, please.")
                player.openBar(npc)
            }
            2 -> chatPlayer(neutral, "No, thank you.")
            3 -> {
                chatPlayer(quiz, "What's it like living down here?")
                if (QuestRequirements.isOnQuest(player, ROYAL_TROUBLE)) {
                    chatNpc(
                        neutral,
                        "The dwarves living down here spend a lot of time and money in my inn.",
                    )
                    chatNpc(sad, "One of them's been in here all day, and only bought one pint.")
                    chatNpc(quiz, "Could you ask him to at least order another drink?")
                    chatPlayer(neutral, "I'll see what I can do.")
                    return
                }
                chatNpc(happy, "Business is booming!")
                chatNpc(
                    sad,
                    "Now, if only I hadn't taken a loss with the beer I sold to those teenagers.",
                )
            }
        }
    }

    private companion object {
        const val ROYAL_TROUBLE = "quest_royaltrouble"
        const val RUNA = "npc.royal_barmaid"
    }
}
