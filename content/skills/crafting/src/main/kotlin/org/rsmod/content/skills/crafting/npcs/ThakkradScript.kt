package org.rsmod.content.skills.crafting.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpcU
import org.rsmod.content.skills.crafting.interfaces.TanningRecipe
import org.rsmod.content.skills.crafting.interfaces.tanningRecipes
import org.rsmod.content.skills.crafting.util.CraftingConstants
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ThakkradScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(CraftingConstants.YAK_CURER) { serviceMenu(it.npc, greet = true) }
        onOpNpc3(CraftingConstants.YAK_CURER) { serviceMenu(it.npc, greet = false) }
        onOpNpcU(CraftingConstants.YAK_CURER) { event ->
            val row = yakRow() ?: return@onOpNpcU
            if (event.objType.internalName == row.input) {
                startDialogue(event.npc) { cureBranch(row) }
            }
        }
    }

    private suspend fun ProtectedAccess.serviceMenu(npc: Npc, greet: Boolean) {
        val row = yakRow() ?: return
        startDialogue(npc) {
            if (greet) {
                chatNpc(happy, "What can I help you with?")
            }
            val cure = choice2(
                "Cure my yak-hide, please.", true,
                "Nothing, thanks.", false,
                title = "What can I help you with?",
            )
            if (cure) {
                cureBranch(row)
            } else {
                declineService()
            }
        }
    }

    private suspend fun Dialogue.declineService() {
        chatPlayer(neutral, "Nothing, thanks.")
        chatNpc(neutral, "See you later.")
        chatNpc(neutral, "You won't find anyone else who can cure yak-hide.")
    }

    private suspend fun Dialogue.cureBranch(row: TanningRecipe) {
        val hide = row.input
        val held = access.inv.count(hide)

        chatPlayer(happy, "Cure my yak-hide, please.")
        chatNpc(neutral, "I will cure yak-hide for a fee of ${row.cost} gp per hide.")

        if (held == 0) {
            chatNpc(neutral, "You have no yak-hide to cure.")
            return
        }
        if (access.inv.count(CraftingConstants.COINS) < row.cost * held) {
            chatNpc(neutral, "You don't have enough gold to pay me!")
            return
        }

        val choice = choice4(
            "Cure all my hides.", CureChoice.All,
            "Cure one hide.", CureChoice.One,
            "Cure no hide.", CureChoice.None,
            "Can you cure any other type of leather?", CureChoice.OtherLeather,
            title = "How many hides do you want cured?",
        )
        when (choice) {
            CureChoice.All -> cure(row, held)
            CureChoice.One -> cure(row, 1)
            CureChoice.None -> chatNpc(neutral, "Bye.")
            CureChoice.OtherLeather -> otherLeather()
        }
    }

    private suspend fun Dialogue.otherLeather() {
        chatPlayer(quiz, "Can you cure any other type of leather?")
        chatNpc(confused, "Other types of leather? Why would you need any other type of leather?")
        chatPlayer(neutral, "I'll take that as a no then.")
    }

    private suspend fun Dialogue.cure(row: TanningRecipe, amount: Int) {
        if (!access.cureYakHides(row, amount)) {
            chatNpc(neutral, "You don't have enough gold to pay me!")
            return
        }
        chatNpc(happy, "There you go.")
    }

    private fun ProtectedAccess.cureYakHides(row: TanningRecipe, amount: Int): Boolean {
        val hide = row.input
        val cure = minOf(amount, inv.count(hide))
        if (cure <= 0) {
            return false
        }

        val totalCost = row.cost * cure
        if (inv.count(CraftingConstants.COINS) < totalCost) {
            return false
        }
        if (!invDel(inv, CraftingConstants.COINS, totalCost).success) {
            return false
        }
        if (!invDel(inv, hide, cure).success) {
            invAdd(inv, CraftingConstants.COINS, totalCost)
            return false
        }
        invAdd(inv, row.output, cure)
        return true
    }

    private fun yakRow(): TanningRecipe? = tanningRecipes.firstOrNull { it.output == "obj.yak_hide_cured" }

    private enum class CureChoice { All, One, None, OtherLeather }
}
