package org.rsmod.content.skills.crafting.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc2
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpcU
import org.rsmod.content.skills.crafting.interfaces.TannerPrices
import org.rsmod.content.skills.crafting.interfaces.heldTannableHides
import org.rsmod.content.skills.crafting.interfaces.openTanner
import org.rsmod.content.skills.crafting.interfaces.tannableHideObjs
import org.rsmod.content.skills.crafting.interfaces.tannedLeatherObjs
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class LeatherTannerScript : PluginScript() {
    private val tanners: List<Tanner> = listOf(
        Tanner(TANNER_ELLIS, LeatherManufacturerFlow, TannerPrices.Table),
        Tanner(TANNER_GUILD, LeatherManufacturerFlow, TannerPrices.Table),

        Tanner(TANNER_CHOUANI, ChouaniFlow, TannerPrices.Sbott),
    )

    override fun ScriptContext.startup() {
        for ((npc, flow, prices) in tanners) {
            onOpNpc1(npc) { flow.greet(this, it.npc, prices) }
            onOpNpc2(npc) { openTanner(prices) }
            onOpNpc3(npc) { openTanner(prices) }
            onOpNpcU(npc) { usedItemOnTanner(it.npc, it.objType.internalName, prices) }
        }
    }

    private suspend fun ProtectedAccess.usedItemOnTanner(npc: Npc, obj: String, prices: TannerPrices) {
        when {
            obj in tannableHideObjs -> openTanner(prices)
            obj in tannedLeatherObjs ->
                startDialogue(npc) { chatNpc(neutral, "Er... I have no use for that, I make the stuff!") }
            else -> startDialogue(npc) { chatNpc(neutral, "Er... Thanks, but no thanks!") }
        }
    }
}

private data class Tanner(
    val npc: String,
    val flow: DialogueFlow,
    val prices: TannerPrices,
)

private interface DialogueFlow {
    suspend fun greet(access: ProtectedAccess, npc: Npc, prices: TannerPrices)
}

private object LeatherManufacturerFlow : DialogueFlow {
    override suspend fun greet(access: ProtectedAccess, npc: Npc, prices: TannerPrices) {
        access.startDialogue(npc) {
            chatNpc(happy, "Greetings friend. I am a manufacturer of leather.")
            val hides = access.heldTannableHides()
            if (hides > 0) {
                offerTanning(hides, prices)
            } else {
                leatherSalesPitch()
            }
        }
    }

    private suspend fun Dialogue.leatherSalesPitch() {
        val buyLeather = choice2("Can I buy some leather then?", true, "Leather is rather weak stuff.", false)
        if (buyLeather) {
            chatPlayer(quiz, "Can I buy some leather then?")
            chatNpc(
                neutral,
                "I make leather from animal hides. Bring me some cowhides and one gold coin per " +
                    "hide, and I'll tan them into soft leather for you.",
            )
        } else {
            chatPlayer(neutral, "Leather is rather weak stuff.")
            chatNpc(
                neutral,
                "Normal leather may be quite weak, but it's very cheap - I make it from cowhides " +
                    "for only 1 gp per hide - and it's so easy to craft that anyone can work with it.",
            )
            chatNpc(
                neutral,
                "Alternatively you could try hard leather. It's not so easy to craft, but I only " +
                    "charge 3 gp per cowhide to prepare it, and it makes much sturdier armour.",
            )
            chatNpc(
                neutral,
                "I can also tan snake hides and dragonhides, suitable for crafting into the " +
                    "highest quality armour for rangers.",
            )
            chatPlayer(happy, "Thanks, I'll bear it in mind.")
        }
    }
}

private object ChouaniFlow : DialogueFlow {
    override suspend fun greet(access: ProtectedAccess, npc: Npc, prices: TannerPrices) {
        access.startDialogue(npc) {
            chatNpc(happy, "Nilsal, iknami. Would you like me to tan any hides for you?")
            if (access.heldTannableHides() == 0) {
                chatPlayer(neutral, "No thanks. I don't have any hides.")
                farewell()
                return@startDialogue
            }
            if (choice2("Yes please.", true, "No thanks.", false)) {
                chatPlayer(happy, "Yes please.")
                access.openTanner(prices)
            } else {
                chatPlayer(neutral, "No thanks.")
                farewell()
            }
        }
    }

    private suspend fun Dialogue.farewell() {
        chatNpc(neutral, "No problem, iknami. Come back if you need me to tan any hides for you.")
    }
}

private suspend fun Dialogue.offerTanning(hides: Int, prices: TannerPrices) {
    if (hides == 1) {
        chatNpc(quiz, "I see you have brought me a hide. Would you like me to tan it for you?")
    } else {
        chatNpc(quiz, "I see you have brought me some hides. Would you like me to tan them for you?")
    }
    if (choice2("Yes please.", true, "No thanks.", false)) {
        chatPlayer(happy, "Yes please.")
        access.openTanner(prices)
    } else {
        chatPlayer(neutral, "No thanks.")
        chatNpc(neutral, "Very well, ${access.sirMadam()}, as you wish.")
    }
}

private fun ProtectedAccess.sirMadam(): String = if (isBodyTypeA()) "sir" else "madam"

private const val TANNER_ELLIS = "npc.ellis_tanner"

private const val TANNER_GUILD = "npc.tanner"

private const val TANNER_CHOUANI = "npc.auburn_tanner"
