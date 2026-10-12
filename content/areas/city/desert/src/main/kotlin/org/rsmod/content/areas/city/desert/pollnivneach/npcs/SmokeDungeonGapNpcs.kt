package org.rsmod.content.areas.city.desert.pollnivneach.npcs

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.config.refs.BaseParams
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class SmokeDungeonGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.slayer_dust_devil_guard") { startDialogue(it.npc) { dustyAliv() } }
    }

    private fun Dialogue.hasSlayerTask(npc: String): Boolean {
        val task = access.vars["varp.slayer_target"]
        if (task == 0 || access.vars["varp.slayer_count"] <= 0) {
            return false
        }
        val type = ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC)) ?: return false
        return type.paramOrNull(BaseParams.slayer_task_id) == task
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dusty_Aliv
    // TODO(content): [skill:slayer] stopping players without a dust devil task from attacking them
    private suspend fun Dialogue.dustyAliv() {
        chatNpc(neutral, "Eyup. Not too often we get visitors down here.")
        while (true) {
            val option =
                choice4(
                    "What is this place?",
                    1,
                    "Who are you?",
                    2,
                    "Nice sword.",
                    3,
                    "Goodbye.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What is this place?")
                    chatNpc(
                        neutral,
                        "Beats me. All I know is, there's dust devils in here, and I'm here to " +
                            "make sure only people with permission can kill 'em.",
                    )
                    chatPlayer(quiz, "Can I kill them?")
                    if (hasSlayerTask(DUST_DEVIL)) {
                        chatNpc(neutral, "You got a Slayer task for it, so sure, be my guest.")
                    } else {
                        chatNpc(
                            neutral,
                            "Not without a Slayer task. Go talk to a Slayer Master about it.",
                        )
                    }
                }
                2 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(
                        neutral,
                        "The name's Dusty Aliv. I guard this place for the Slayer Masters.",
                    )
                    chatPlayer(neutral, "That doesn't sound like a great job.")
                    chatNpc(
                        neutral,
                        "Eh, it has its perks. Plus it beats flying around Gielinor after " +
                            "numbskulls like you trying to deliver mail like some people I know.",
                    )
                }
                3 -> {
                    chatPlayer(happy, "Nice sword.")
                    chatNpc(
                        neutral,
                        "Thanks. Probably shouldn't tell you about the previous owner though.",
                    )
                    chatPlayer(quiz, "What happened to them?")
                    chatNpc(neutral, "Let's just say the heat got to them.")
                }
                else -> {
                    chatPlayer(neutral, "Goodbye.")
                    chatNpc(neutral, "See ya.")
                    return
                }
            }
        }
    }

    private companion object {
        const val DUST_DEVIL = "npc.slayer_dustdevil"
    }
}
