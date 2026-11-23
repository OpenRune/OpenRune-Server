package org.rsmod.content.quest.area.alkharid.princealirescue.npcs

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
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

    private fun obj(internal: String): ItemServerType =
        checkNotNull(ServerCacheManager.getItem(internal.asRSCM(RSCMType.OBJ)))

    private suspend fun Dialogue.inCell() {
        val stage = princeAli.stage(player)
        if (stage >= STAGE_ALI_ESCAPED) {
            atHome()
            return
        }
        chatPlayer(happy, "Prince Ali? I'm here to rescue you.")
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
        with(princeAli) {
            access.fadeScene("Prince Ali puts on the disguise and uses it to escape.") {
                if (invDel(inv, obj(BLOND_WIG), 1, obj(SKIN_PASTE), 1, obj(PINK_SKIRT), 1).failure) {
                    return@fadeScene false
                }
                setStage(this, STAGE_ALI_ESCAPED)
                true
            }
        }
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
