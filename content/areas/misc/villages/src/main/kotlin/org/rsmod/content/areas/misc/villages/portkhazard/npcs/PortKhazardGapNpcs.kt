package org.rsmod.content.areas.misc.villages.portkhazard.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class PortKhazardGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.wgs_washerman") { startDialogue(it.npc) { launderer() } }
        onOpNpc1("npc.fightslave_kelvin") { startDialogue(it.npc) { kelvin() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Khazard_Launderer (before While Guthix Sleeps)
    // TODO(content): [quest:while_guthix_sleeps] after-quest dialogue
    private suspend fun Dialogue.launderer() {
        chatNpc(happy, "Hello there. What can I do for you?")
        chatPlayer(quiz, "What do you do here?")
        chatNpc(
            neutral,
            "I mainly take in washing and make sure it's good and clean for my customers. Times " +
                "are tough though. Not everyone can afford such a service and it's affecting my " +
                "takings.",
        )
        chatPlayer(quiz, "Do you find that very fulfilling?")
        chatNpc(
            neutral,
            "Well it can be a bit monotonous and the pay isn't good, but I set myself some goals " +
                "and deliver my washing clean. My customers rarely complain, so I guess I must " +
                "be doing something right.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Kelvin
    // TODO(content): [verify:in_game] which Khazard armour pieces count as a disguise
    private suspend fun Dialogue.kelvin() {
        chatPlayer(quiz, "Are you okay?")
        val disguised =
            "obj.khazard_helmet" in player.worn && "obj.khazard_platemail" in player.worn
        when {
            disguised && access.random.of(2) == 0 ->
                chatNpc(angry, "Get away! One day we'll have our revenge on you all!")
            disguised ->
                chatNpc(
                    angry,
                    "Spare us your fake pity. I spit on Khazard, and all who do his bidding.",
                )
            access.random.of(2) == 0 ->
                chatNpc(
                    worried,
                    "You're brave coming in here. If the guards get you, you'll be the next one " +
                        "in a cell.",
                )
            else -> chatNpc(worried, "You're not safe here, traveller. Leave while you still can.")
        }
    }
}
