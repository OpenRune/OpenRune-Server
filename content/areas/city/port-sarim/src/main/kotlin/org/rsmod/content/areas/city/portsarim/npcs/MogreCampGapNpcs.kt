package org.rsmod.content.areas.city.portsarim.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class MogreCampGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.100_pirate_mogre_nung") { startDialogue(it.npc) { nung() } }
        onOpNpc1("npc.100_pirate_mogre_guard") { startDialogue(it.npc) { mogreGuard() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Nung
    private suspend fun Dialogue.nung() {
        chatNpc(
            neutral,
            "Hey fishy. If you's a good fishy I might sell yous bitses of skippy skin clothes.",
        )
        chatNpc(neutral, "But Nung needs time to makes them first.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Mogre_Guard
    // TODO(content): [quest:recipe_for_disaster] entering the crab pen after speaking to Nung
    private suspend fun Dialogue.mogreGuard() {
        chatNpc(
            laugh,
            "Silly fishy! You lookin' like likkle Mogre wid your likkle arms an' wid your likkle " +
                "legs!",
        )
        chatNpc(laugh, "Swim along silly round head fishy!")
        chatNpc(neutral, "Dont let dem big skippies eat you!")
    }
}
