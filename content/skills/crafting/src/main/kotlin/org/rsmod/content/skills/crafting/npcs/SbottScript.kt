package org.rsmod.content.skills.crafting.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
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

class SbottScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(TANNER_SBOTT) { greet(it.npc) }
        onOpNpc3(TANNER_SBOTT) { openTanner(TannerPrices.Sbott) }
        onOpNpcU(TANNER_SBOTT) { usedItemOnSbott(it.npc, it.objType.internalName) }
    }

    private suspend fun ProtectedAccess.greet(npc: Npc) {
        startDialogue(npc) {
            chatNpc(happy, "Hello stranger. Would you like me to tan any hides for you?")
            chatNpcNoAnim(
                "Soft leather - 2 gp per hide<br>" +
                    "Hard leather - 5 gp per hide<br>" +
                    "Snakeskins - 25 gp per hide<br>" +
                    "Dragon leather - 45 gp per hide.",
            )
            if (access.heldTannableHides() > 0) {
                offerWithHides()
            } else {
                offerEmptyHanded()
            }
        }
    }

    private suspend fun Dialogue.offerWithHides() {
        val pick = choice3(
            "Yes please.", OfferChoice.Yes,
            "Why are you so expensive?", OfferChoice.Why,
            "No thanks, I'm not interested.", OfferChoice.No,
        )
        when (pick) {
            OfferChoice.Yes -> access.openTanner(TannerPrices.Sbott)
            OfferChoice.Why -> {
                explainPricing()
                if (choice2("Yes please.", true, "No thanks, I'm not interested.", false)) {
                    access.openTanner(TannerPrices.Sbott)
                } else {
                    notInterested()
                }
            }
            OfferChoice.No -> notInterested()
        }
    }

    private suspend fun Dialogue.offerEmptyHanded() {
        val expensive = choice2(
            "Why are you so expensive?", true,
            "No thanks, I haven't any hides.", false,
        )
        if (expensive) {
            explainPricing()
        }
        noHides()
    }

    private suspend fun Dialogue.explainPricing() {
        chatPlayer(quiz, "Why are you so expensive? The tanner in Al-Kharid is almost half the price!")
        chatNpc(
            happy,
            "Hey, I charge more because I'm worth it! I deal in bulk, and I work extremely " +
                "quickly. You'll see for yourself!",
        )
        chatNpc(happy, "You got a lot of hides you want tanning quickly? I'm your guy!")
        chatNpc(quiz, "So you got hides for me to tan, or are you just gonna bust my chops about prices all day?")
    }

    private suspend fun Dialogue.notInterested() {
        chatPlayer(neutral, "No thanks, I'm not interested.")
        chatNpc(neutral, "Okay; you change your mind, you come see me. I'm your guy!")
    }

    private suspend fun Dialogue.noHides() {
        chatPlayer(neutral, "No thanks, I haven't any hides.")
        chatNpc(neutral, "Fair enough. I can't tan what you don't bring me.")
    }

    private suspend fun ProtectedAccess.usedItemOnSbott(npc: Npc, obj: String) {
        when {
            obj in tannableHideObjs -> openTanner(TannerPrices.Sbott)
            obj in tannedLeatherObjs ->
                startDialogue(npc) { chatNpc(neutral, "Er... I have no use for that, I make the stuff!") }
            else -> startDialogue(npc) { chatNpc(neutral, "Er... Thanks, but no thanks!") }
        }
    }

    private enum class OfferChoice { Yes, Why, No }
}

private const val TANNER_SBOTT = "npc.werewolftanner"
