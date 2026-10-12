package org.rsmod.content.areas.city.lletya.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class TirannwnNpcs @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.regicide_old_camp_tracker") { startDialogue(it.npc) { elfTracker() } }
        onOpNpc1("npc.regicide_old_camp_tracker_vis") { startDialogue(it.npc) { elfTracker() } }
        onOpNpc1("npc.regicide_tyras_guard") { startDialogue(it.npc) { tyrasGuard() } }
        onOpNpc1("npc.lord_iorwerth") { startDialogue(it.npc) { lordIorwerth() } }
        onOpNpc1(QUARTERMASTER) { startDialogue(it.npc) { quartermaster(it.npc) } }
        onOpNpc3(QUARTERMASTER) { player.openQuartermaster(it.npc) }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Elf_Tracker
    private suspend fun Dialogue.elfTracker() {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "How goes it?")
        chatPlayer(happy, "Well thanks.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Tyras_guard (in Tyras Camp)
    private suspend fun Dialogue.tyrasGuard() {
        chatPlayer(happy, "Hello.")
        chatNpc(
            neutral,
            "Sorry, can't stop to talk. You should go to General Hining if you need something.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lord_Iorwerth
    // TODO(content): [quest:regicide] quest dialogue
    // TODO(content): [quest:song_of_the_elves] post-Regicide and quest dialogue
    private suspend fun Dialogue.lordIorwerth() {
        chatPlayer(happy, "Good day.")
        chatNpc(neutral, "Sorry, can't stop to talk. So much to do, so little time.")
    }

    private fun Player.openQuartermaster(npc: Npc) {
        shops.open(this, npc, "Quartermaster's Stores", "inv.regicide_general_shop")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Quartermaster (before finishing Regicide)
    // TODO(content): [quest:regicide] "We have a new stock of dragon halberds." after the quest;
    // dragon halberds should only be in stock after Regicide
    private suspend fun Dialogue.quartermaster(npc: Npc) {
        val title = if (access.isBodyTypeA()) "sir" else "miss"
        chatNpc(
            happy,
            "Good day $title. I'm the quartermaster for this camp. We have a little we could " +
                "trade here.",
        )
        chatNpc(
            happy,
            "We should be getting a new stock soon. Would you like a look at what we do have?",
        )
        when (choice2("Yes please. What are you selling?", 1, "No thanks.", 2)) {
            1 -> {
                chatPlayer(quiz, "Yes please. What are you selling?")
                player.openQuartermaster(npc)
            }
            2 -> chatPlayer(neutral, "No thanks.")
        }
    }

    private companion object {
        const val QUARTERMASTER = "npc.regicidegeneralshopkeeper"
    }
}
