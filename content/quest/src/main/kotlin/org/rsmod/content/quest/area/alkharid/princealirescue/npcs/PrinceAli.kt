package org.rsmod.content.quest.area.alkharid.princealirescue.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.BLOND_WIG
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.NPC_PRINCE_CELL
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.NPC_PRINCE_PALACE
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.PINK_SKIRT
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.SKIN_PASTE
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.STAGE_ALI_ESCAPED
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PrinceAli @Inject constructor(private val princeAli: PrinceAliRescueQuest) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpNpc1(NPC_PRINCE_CELL) { startDialogue(it.npc) { inCell() } }
        onOpNpc1(NPC_PRINCE_PALACE) { startDialogue(it.npc) { atHome() } }
    }

    private suspend fun Dialogue.inCell() {
        val stage = princeAli.stage(player)
        if (stage >= STAGE_ALI_ESCAPED) {
            atHome()
            return
        }
        chatPlayer(neutral, "Prince Ali? I'm here to rescue you.")
        chatNpc(happy, "Oh thank goodness! What's your plan?")
        if (!princeAli.hasDisguise(player) || !princeAli.hasKey(player)) {
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
        if (access.invDel(inv, BLOND_WIG).failure) return
        if (access.invDel(inv, SKIN_PASTE).failure) return
        if (access.invDel(inv, PINK_SKIRT).failure) return
        princeAli.setStage(access, STAGE_ALI_ESCAPED)
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
