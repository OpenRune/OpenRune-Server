package org.rsmod.content.areas.city.rellekka.miscellania.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.shops.Shops
import org.rsmod.content.areas.city.rellekka.fremennikName
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:throne_of_miscellania] "Your Royal Highness" greetings at the
// Miscellania stalls after the quest
// TODO(content): [system:managing_miscellania] approval percentage after Royal Trouble
// TODO(content): [skill:thieving] Skraeling overhead line after failing to steal from a stall
class MarketStalls @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(MISC_FISH) { startDialogue(it.npc) { miscellaniaFishmonger(it.npc) } }
        onOpNpc3(MISC_FISH) { player.openIslandFishmonger(it.npc) }
        onOpNpc1(MISC_VEG) { startDialogue(it.npc) { miscellaniaGreengrocer(it.npc) } }
        onOpNpc3(MISC_VEG) { player.openGreengrocerOfMiscellania(it.npc) }
        onOpNpc1(ETC_FISH) { startDialogue(it.npc) { etceteriaFishmonger(it.npc) } }
        onOpNpc3(ETC_FISH) { player.openEtceteriaFish(it.npc) }
        onOpNpc4(ETC_FISH) { startDialogue(it.npc) { etceteriaApproval() } }
        onOpNpc1(ETC_VEG) { startDialogue(it.npc) { etceteriaGreengrocer(it.npc) } }
        onOpNpc3(ETC_VEG) { player.openIslandGreengrocer(it.npc) }
        onOpNpc4(ETC_VEG) { startDialogue(it.npc) { etceteriaApproval() } }
    }

    private fun Player.openIslandFishmonger(npc: Npc) {
        shops.open(this, npc, "Island Fishmonger", "inv.misc_fishmonger")
    }

    private fun Player.openGreengrocerOfMiscellania(npc: Npc) {
        shops.open(this, npc, "Greengrocer of Miscellania", "inv.misc_greengrocer")
    }

    private fun Player.openEtceteriaFish(npc: Npc) {
        shops.open(this, npc, "Etceteria Fish", "inv.etc_fishmonger")
    }

    private fun Player.openIslandGreengrocer(npc: Npc) {
        shops.open(this, npc, "Island Greengrocer", "inv.etc_greengrocer")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fishmonger
    private suspend fun Dialogue.miscellaniaFishmonger(npc: Npc) {
        chatNpc(happy, "Greetings. Get your fresh fish here!")
        chatNpc(shifty, "I've heard that the Etceterian fish is stored in a cow shed.")
        player.openIslandFishmonger(npc)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Greengrocer
    private suspend fun Dialogue.miscellaniaGreengrocer(npc: Npc) {
        val title = if (access.isBodyTypeA()) "Sir" else "Madam"
        chatNpc(happy, "Welcome, $title. I sell only the finest and freshest vegetables!")
        player.openGreengrocerOfMiscellania(npc)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fishmonger
    private suspend fun Dialogue.etceteriaFishmonger(npc: Npc) {
        chatNpc(happy, "Welcome ${fremennikName()}. My fish is fresher than any in Miscellania!")
        player.openEtceteriaFish(npc)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Greengrocer
    private suspend fun Dialogue.etceteriaGreengrocer(npc: Npc) {
        chatNpc(happy, "Hello ${fremennikName()}. You've come to the right place for yer greens!")
        player.openIslandGreengrocer(npc)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fishmonger (Checking approval)
    private suspend fun Dialogue.etceteriaApproval() {
        chatPlayer(quiz, "Can you tell me how the kingdom approves of me?")
        chatNpc(angry, "Kingdom? Etceteria is ruled by a Queen and we don't approve of you.")
    }

    private companion object {
        const val MISC_FISH = "npc.misc_fish_monger"
        const val MISC_VEG = "npc.misc_veg_monger"
        const val ETC_FISH = "npc.etc_fish_monger"
        const val ETC_VEG = "npc.etc_veg_monger"
    }
}
