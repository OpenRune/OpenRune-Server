package org.rsmod.content.areas.city.varrock.npcs.shops

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

// https://oldschool.runescape.wiki/w/Transcript:Zaff (members world)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [diary:varrock] "Have you any extra stock of battlestaffs I can buy?"
// TODO(content): [boss:bryophyta] "Can you make me a staff?" with Bryophyta's essence
class StaffShop @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.zaff") { shopDialogue(it.npc) }
        onOpNpc3("npc.zaff") { player.openStaffShop(it.npc) }
    }

    private fun Player.openStaffShop(npc: Npc) {
        shops.open(this, npc, "Zaff's Superior Staffs!", "inv.staffshop")
    }

    private suspend fun ProtectedAccess.shopDialogue(npc: Npc) =
        startDialogue(npc) { shopKeeper(npc) }

    private suspend fun Dialogue.shopKeeper(npc: Npc) {
        chatNpc(
            happy,
            "Would you like to buy or sell some staffs? Or is there something else you need?",
        )
        val option =
            if (QuestRequirements.hasCompleted(player, WHAT_LIES_BELOW)) {
                choice3("Yes, please!", 1, "No, thank you.", 2, "Can I have another ring?", 3)
            } else {
                choice2("Yes, please!", 1, "No, thank you.", 2)
            }
        when (option) {
            1 -> player.openStaffShop(npc)
            3 -> anotherRing()
            2 -> {
                chatPlayer(neutral, "No, thank you.")
                chatNpc(happy, "Well, 'stick' your head in again if you change your mind.")
                chatPlayer(laugh, "Huh, terrible pun! You just can't get the 'staff' these days!")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Zaff (after What Lies Below)
    private suspend fun Dialogue.anotherRing() {
        chatPlayer(quiz, "Can I have another ring?")
        val name = player.displayName
        if (BEACON_RING in player.inv) {
            chatNpc(neutral, "There's still one in your inventory, $name!")
            return
        }
        if (BEACON_RING in access.bank) {
            chatNpc(neutral, "Go and get the one that's in your bank, $name!")
            return
        }
        if (access.invAdd(access.inv, BEACON_RING).failure) {
            chatNpc(
                sad,
                "I would be happy to give you another ring, but I'm afraid you don't have enough " +
                    "inventory space for it!",
            )
            return
        }
        chatNpc(
            happy,
            "Yes, of course, $name. Here you are. Please bear in mind that this ring has no " +
                "charges left with which to summon me, however.",
        )
    }

    private companion object {
        const val WHAT_LIES_BELOW = "quest_whatliesbelow"
        const val BEACON_RING = "obj.surok_ring"
    }
}
