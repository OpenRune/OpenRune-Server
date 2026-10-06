package org.rsmod.content.quest.area.alkharid.princealirescue.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.BlondWig
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.NpcPrinceCell
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.NpcPrincePalace
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.PinkSkirt
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.SkinPaste
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.StageAliEscaped
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.StageKeliTied
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Prince Ali, in his cell in the Draynor jail until the disguise gets him out, and afterwards back
 * in the Al Kharid palace. Both forms are `varp.princequest` multinpcs.
 */
class PrinceAli @Inject constructor(private val princeAli: PrinceAliRescueQuest) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpNpc1(NpcPrinceCell) { startDialogue(it.npc) { inCell() } }
        onOpNpc1(NpcPrincePalace) { startDialogue(it.npc) { atHome() } }
    }

    private suspend fun Dialogue.inCell() {
        val stage = princeAli.stage(player)
        if (stage >= StageAliEscaped) {
            atHome()
            return
        }
        chatPlayer(neutral, "Prince Ali? I'm here to rescue you.")
        chatNpc(happy, "Oh thank goodness! What's your plan?")
        if (stage < StageKeliTied) {
            chatNpc(worried, "Wait! Keli is still out there. Please, deal with her first.")
            return
        }
        if (!princeAli.hasDisguise(player)) {
            chatPlayer(
                neutral,
                "I've already dealt with Lady Keli and the guard. I'm going to get you a disguise " +
                    "so the guards outside don't spot you leaving. I'll be back once I have it.",
            )
            return
        }
        chatPlayer(neutral, "Take this disguise. You can use it to get past the guards outside.")
        chatNpc(happy, "Thank you, my friend. I must leave you now, but my father will pay you well for this.")
        val inv = access.inv
        if (access.invDel(inv, BlondWig).failure) return
        if (access.invDel(inv, SkinPaste).failure) return
        if (access.invDel(inv, PinkSkirt).failure) return
        princeAli.setStage(access, StageAliEscaped)
        mesbox("Prince Ali puts on the disguise and uses it to escape.")
    }

    private suspend fun Dialogue.atHome() {
        chatNpc(
            happy,
            "Good to see you, friend. I am forever in your debt for what you did, as is all of Al Kharid.",
        )
        chatNpc(
            neutral,
            "I know my father is very grateful as well. If he were in better health, he would have " +
                "thanked you personally. However, at his age, it is best that he spends his time " +
                "resting.",
        )
        chatPlayer(happy, "I'm just happy to have helped. All the best.")
    }
}
