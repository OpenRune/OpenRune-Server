package org.rsmod.content.areas.city.alkharid.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Chancellor_Hassan
// https://oldschool.runescape.wiki/w/Transcript:Prince_Ali_Rescue
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:prince_ali_rescue] quest start, in-progress and post-quest dialogue
class ChancellorHassan @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.hassan") { startDialogue(it.npc) { hassan() } }
    }

    private suspend fun Dialogue.hassan() {
        chatNpc(happy, "Greetings! I am Hassan, Chancellor to the Emir of Al Kharid.")
        while (true) {
            val option =
                choice4(
                    "Is there anything I can help you with?",
                    1,
                    "It's just too hot here. How can you stand it?",
                    2,
                    "Do you mind if I just kill your warriors?",
                    3,
                    "I'd better be off.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Is there anything I can help you with?")
                    chatNpc(
                        worried,
                        "Well... we do currently have a very urgent issue we need to resolve, " +
                            "and I suppose you look like someone who knows how to get a job " +
                            "done. Are you definitely interested in helping?",
                    )
                    return
                }
                2 -> {
                    chatPlayer(sad, "It's just too hot here. How can you stand it?")
                    chatNpc(
                        happy,
                        "We manage, in our humble way. We are a wealthy town and we have water. " +
                            "It cures many thirsts.",
                    )
                    player.invAddOrDrop(objRepo, "obj.jug_water")
                    mesbox("The chancellor hands you some water.")
                }
                3 -> {
                    chatPlayer(shifty, "Do you mind if I just kill your warriors?")
                    chatNpc(confused, "Kill our warriors? I assume this is some sort of joke?")
                    chatPlayer(neutral, "I'll take that as a no. Forget I asked.")
                }
                4 -> {
                    chatPlayer(neutral, "I'd better be off.")
                    return
                }
            }
        }
    }
}
