package org.rsmod.content.areas.city.ardougne.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Bartender_(Flying_Horse_Inn)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [miniquest:alfred_grimhands_barcrawl] Heart Stopper and the barcrawl card
class FlyingHorseInn @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.flyinghorse_bartender") { startDialogue(it.npc) { bartender() } }
    }

    private suspend fun Dialogue.bartender() {
        chatNpc(happy, "Would you like to buy a drink?")
        chatPlayer(quiz, "What do you serve?")
        chatNpc(happy, "Beer!")
        if (!choice2("I'll have a beer then.", true, "I'll not have anything then.", false)) {
            chatPlayer(neutral, "I'll not have anything then.")
            return
        }
        chatPlayer(happy, "I'll have a beer then.")
        chatNpc(happy, "Ok, that'll be two coins.")
        if (!player.invTakeFee(BEER_PRICE)) {
            chatPlayer(sad, "Oh dear. I don't seem to have enough money.")
            return
        }
        player.invAddOrDrop(objRepo, "obj.beer")
    }

    private companion object {
        const val BEER_PRICE = 2
    }
}
