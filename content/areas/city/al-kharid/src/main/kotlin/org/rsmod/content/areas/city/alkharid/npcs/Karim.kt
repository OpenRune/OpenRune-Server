package org.rsmod.content.areas.city.alkharid.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Karim
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [miniquest:rogue_trader] "What do you think of Ali Morrisane?"
// TODO(content): [system:treasure_trails] medium clue challenge scroll
class Karim @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.kebab_seller") { startDialogue(it.npc) { karim() } }
    }

    private suspend fun Dialogue.karim() {
        chatNpc(happy, "Would you like to buy a nice kebab? Only one gold.")
        when (choice2("I think I'll give it a miss.", 1, "Yes please.", 2)) {
            1 -> chatPlayer(neutral, "I think I'll give it a miss.")
            2 -> {
                chatPlayer(happy, "Yes please.")
                if (player.invTakeFee(KEBAB_PRICE)) {
                    player.invAddOrDrop(objRepo, "obj.kebab")
                } else {
                    chatPlayer(sad, "Oops, I forgot to bring any money with me.")
                    chatNpc(neutral, "Come back when you have some.")
                }
            }
        }
    }

    private companion object {
        const val KEBAB_PRICE = 1
    }
}
