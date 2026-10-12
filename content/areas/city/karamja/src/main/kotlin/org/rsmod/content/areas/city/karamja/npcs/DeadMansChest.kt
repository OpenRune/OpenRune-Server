package org.rsmod.content.areas.city.karamja.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Bartender_(Dead_Man%27s_Chest)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [miniquest:alfred_grimhands_barcrawl] barcrawl drink
class DeadMansChest @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.deadmans_bartender") { startDialogue(it.npc) { bartender() } }
    }

    private suspend fun Dialogue.bartender() {
        chatNpc(happy, "Yohoho me hearty what would you like to drink?")
        val option =
            choice3(
                "Nothing, thank you.",
                1,
                "A pint of Grog please.",
                2,
                "A bottle of rum please.",
                3,
            )
        when (option) {
            1 -> chatPlayer(neutral, "Nothing, thank you.")
            2 -> {
                chatPlayer(happy, "A pint of Grog please.")
                chatNpc(happy, "One grog coming right up, that'll be three coins.")
                buy(GROG_PRICE, "obj.grog")
            }
            3 -> {
                chatPlayer(happy, "A bottle of rum please.")
                chatNpc(happy, "That'll be 27 coins.")
                buy(RUM_PRICE, "obj.karamja_rum")
            }
        }
    }

    private suspend fun Dialogue.buy(price: Int, obj: String) {
        if (!player.invTakeFee(price)) {
            chatPlayer(sad, "Oh dear. I don't seem to have enough money.")
            return
        }
        player.invAddOrDrop(objRepo, obj)
    }

    private companion object {
        const val GROG_PRICE = 3
        const val RUM_PRICE = 27
    }
}
