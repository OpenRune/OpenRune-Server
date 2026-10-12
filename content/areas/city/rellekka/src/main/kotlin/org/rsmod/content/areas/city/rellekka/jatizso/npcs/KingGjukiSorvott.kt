package org.rsmod.content.areas.city.rellekka.jatizso.npcs

import dev.openrune.types.MesAnimType
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.content.areas.city.rellekka.fremennikName
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:The_Fremennik_Isles (Post-quest, Talking to King
// Sorvott)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:the_fremennik_isles] pre-quest and in-quest dialogue
class KingGjukiSorvott : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.fris_r_king") { startDialogue(it.npc) { postQuest() } }
        onOpNpcU("npc.fris_r_king") { startDialogue(it.npc) { itemUsed() } }
    }

    private suspend fun Dialogue.postQuest() {
        chatNpc(angry, "What are you doing in my court? Guards!")
        chatPlayer(
            neutral,
            "I have come to inform you that you may recall your guards; the Troll King was slain " +
                "by my hand.",
        )
        chatNpc(verymad, "Preposterous. Lies, I tell you, lies!")
        thorkel(worried, "Yes, your majesty, of course your majesty.")
        chatNpc(angry, "Guards.")
        chatPlayer(quiz, "Guards! Where are your Guards?")
        chatPlayer(quiz, "Oh. Are they still recovering from their defeat against the trolls?")
        chatNpc(
            verymad,
            "Outlander, leave me in peace before I am forced to bear arms against you myself.",
        )
        chatPlayer(neutral, "Very well, your Highness, I leave you to contemplate this...")
        chatPlayer(
            neutral,
            "Should you have approached me honestly and truthfully, rather than with lies and " +
                "secrecy,",
        )
        chatPlayer(
            neutral,
            "then it could have been you holding the head of the Troll King aloft today.",
        )
        chatNpc(angry, "Be gone with you. I have heard enough of your insults for one day.")
        thorkel(
            happy,
            "Thanks, ${fremennikName()}, you are the first person to stand up to the King's " +
                "bullying in many a year. Maybe he has learnt something today?",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:King_Gjuki_Sorvott_IV
    private suspend fun Dialogue.itemUsed() {
        chatNpc(angry, "I don't dirty my hands with that sort of thing.")
    }

    private suspend fun Dialogue.thorkel(mesanim: MesAnimType, text: String) =
        chatNpcSpecific("Thorkel Silkbeard", "npc.fris_r_chancellor", mesanim, text)
}
