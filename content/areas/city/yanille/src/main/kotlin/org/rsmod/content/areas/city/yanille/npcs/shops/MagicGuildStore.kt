package org.rsmod.content.areas.city.yanille.npcs.shops

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
class MagicGuildStore @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(AKUTHA) { startDialogue(it.npc) { akuthaTalk(it.npc) } }
        onOpNpc3(AKUTHA) {
            if (QuestRequirements.hasCompleted(player, HAND_IN_THE_SAND)) {
                player.openRunesAndStaves(it.npc)
            } else {
                startDialogue(it.npc) { akuthaTrade() }
            }
        }
        onOpNpc1(SININA) { startDialogue(it.npc) { sinina(it.npc) } }
        onOpNpc3(SININA) { player.openMysticRobes(it.npc) }
    }

    private fun Player.openRunesAndStaves(npc: Npc) {
        shops.open(this, npc, "Magic Guild Store", "inv.magicguildshop")
    }

    private fun Player.openMysticRobes(npc: Npc) {
        shops.open(this, npc, "Magic Guild Store", "inv.magicguildshop2")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Wizard_Akutha
    private suspend fun Dialogue.akuthaTalk(npc: Npc) {
        if (QuestRequirements.hasCompleted(player, HAND_IN_THE_SAND)) {
            chatNpc(
                happy,
                "Welcome to the Magic Guild Store. Would you like to buy some magic supplies?",
            )
            if (choice2("Yes please.", true, "No thank you.", false)) {
                chatPlayer(happy, "Yes please.")
                player.openRunesAndStaves(npc)
                return
            }
            chatPlayer(neutral, "No thank you.")
            return
        }
        chatNpc(angry, "Who are you? Get out of here, I don't have anything for you.")
        mesbox("You must have completed The Hand in the Sand to use the Guild's shop.")
    }

    private suspend fun Dialogue.akuthaTrade() {
        chatNpc(neutral, "I don't know if I can trust you with any of my supplies...")
        mesbox("You must have completed The Hand in the Sand to use the Guild's shop.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Wizard_Sinina
    // TODO(content): [system:skillcapes] cape talk, hood and the 99 Magic store variants
    private suspend fun Dialogue.sinina(npc: Npc) {
        chatNpc(
            happy,
            "Welcome to the Magic Guild Store. Would you like to buy some magic supplies?",
        )
        if (choice2("Yes please.", true, "No thank you.", false)) {
            chatPlayer(happy, "Yes please.")
            player.openMysticRobes(npc)
            return
        }
        chatPlayer(neutral, "No thank you.")
    }

    private companion object {
        const val AKUTHA = "npc.magic_store_owner"
        const val SININA = "npc.magic_store_owner2"
        const val HAND_IN_THE_SAND = "quest_handinthesand"
    }
}
