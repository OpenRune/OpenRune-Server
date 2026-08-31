package org.rsmod.content.skills.crafting.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpcU
import org.rsmod.content.skills.crafting.interfaces.TannerPrices
import org.rsmod.content.skills.crafting.interfaces.openTanner
import org.rsmod.content.skills.crafting.interfaces.tannableHideObjs
import org.rsmod.content.skills.crafting.interfaces.tannedLeatherObjs
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class EodanScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(TANNER_EODAN) { greet(it.npc) }
        onOpNpc3(TANNER_EODAN) { openTanner(TannerPrices.Eodan) }
        onOpNpcU(TANNER_EODAN) { usedItemOnEodan(it.npc, it.objType.internalName) }
    }

    private suspend fun ProtectedAccess.greet(npc: Npc) {
        startDialogue(npc) {
            chatNpc(happy, "Hello, thanks for rescuing me. Is there anything I can do for you?")
            options()
        }
    }

    private suspend fun Dialogue.options() {
        while (true) {
            val pick = choice4(
                "How did you end up down here?", EodanChoice.HowStuck,
                "Now that you're free, why don't you leave?", EodanChoice.WhyStay,
                "Can you tan some hides for me?", EodanChoice.Tan,
                "I'm good thanks.", EodanChoice.Leave,
            )
            when (pick) {
                EodanChoice.HowStuck -> howStuck()
                EodanChoice.WhyStay -> whyStay()
                EodanChoice.Tan -> {
                    access.openTanner(TannerPrices.Eodan)
                    return
                }
                EodanChoice.Leave -> return
            }
        }
    }

    private suspend fun Dialogue.howStuck() {
        chatPlayer(quiz, "How did you end up down here?")
        chatNpc(
            neutral,
            "I travelled down here with my friend Olbertus in search for treasure. When we didn't " +
                "find anything except his strange structure, he decided to prise off a coin from the " +
                "stone relief in the other room. Before I knew it the entrance had closed and I was " +
                "stuck down here.",
        )
        chatNpc(
            sad,
            "I tried calling out to Olbertus but he must not have heard me. I don't know what " +
                "would've happened if you hadn't shown up and saved me.",
        )
        chatPlayer(
            neutral,
            "Well, it turns out Olbertus was corrupted by the coin he stole, I managed to get the " +
                "coin from him and returned it to the relief, he should be fine now.",
        )
        chatNpc(happy, "That's good news at least.")
        chatNpc(
            happy,
            "I am quite the proficient tanner, for helping me escape I will offer to tan hides for " +
                "you. However, they'll be at a slightly higher cost for the convenience of being " +
                "closer to the source.",
        )
    }

    private suspend fun Dialogue.whyStay() {
        chatPlayer(quiz, "Now that you're free, why don't you leave?")
        chatNpc(
            neutral,
            "To be honest, I tried to set up a Tannery on the surface but business was poor, there " +
                "aren't a lot of dragons in Kourend.",
        )
        chatNpc(
            angry,
            "I even heard a rumour that people have learned to tan hides with magic. It's always " +
                "the same! Magic users stealing jobs from honest tradesman!",
        )
        if (access.canCastTanLeather()) {
            chatPlayer(shifty, "Erm... Yeah! Those magic users...")
        } else {
            chatPlayer(shocked, "Oh, wow! I can see why that wouldn't help business.")
        }
        chatNpc(
            neutral,
            "Anyway, I figured if I was closer to the source of the hides then I might get more " +
                "business. So I'll try setting up shop down here for a while.",
        )
    }

    private suspend fun ProtectedAccess.usedItemOnEodan(npc: Npc, obj: String) {
        when {
            obj in tannableHideObjs -> openTanner(TannerPrices.Eodan)
            obj in tannedLeatherObjs ->
                startDialogue(npc) { chatNpc(neutral, "Er... I have no use for that, I make the stuff!") }
            else -> startDialogue(npc) { chatNpc(neutral, "Er... Thanks, but no thanks!") }
        }
    }

    private enum class EodanChoice { HowStuck, WhyStay, Tan, Leave }
}

private fun ProtectedAccess.canCastTanLeather(): Boolean {
    val lunar = player.vars[VARBIT_SPELLBOOK] == SPELLBOOK_LUNAR
    return lunar && statBase("stat.magic") >= 78
}

private const val TANNER_EODAN = "npc.hosdun_eodan"

private const val VARBIT_SPELLBOOK = "varbit.spellbook"

private const val SPELLBOOK_LUNAR = 2
