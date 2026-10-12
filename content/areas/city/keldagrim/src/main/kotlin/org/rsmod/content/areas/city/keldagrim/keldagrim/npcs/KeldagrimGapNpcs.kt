package org.rsmod.content.areas.city.keldagrim.keldagrim.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class KeldagrimGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.dwarf_city_guarding_cat") { startDialogue(it.npc) { dromundsCat() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dromund%27s_cat
    // TODO(content): [quest:the_giant_dwarf] "I know what you did" after the boots go to Riki
    // TODO(content): [quest:dragon_slayer_ii] catspeak without the amulet during and after DS2
    private suspend fun Dialogue.dromundsCat() {
        if (CATSPEAK_AMULETS.none { it in player.worn }) {
            chatNpc(neutral, "Meow!")
            return
        }
        chatNpc(angry, "What do you want?")
        chatPlayer(quiz, "I didn't know dwarves kept cats.")
        chatNpc(angry, "And why wouldn't they?")
        chatNpc(
            neutral,
            "Friend of mine lives at the palace, actually. Well, I say friend. Used to be a " +
                "friend.",
        )
        chatPlayer(quiz, "What happened?")
        chatNpc(neutral, "He got adopted by Hreidmar. The director of the Red Axe.")
        chatPlayer(neutral, "Oh?")
        chatNpc(
            angry,
            "I don't like the Red Axe! They stomp around everywhere, got to be careful of your " +
                "tail with those people around.",
        )
    }

    private companion object {
        val CATSPEAK_AMULETS =
            listOf("obj.ics_little_amulet_of_catspeak", "obj.twocats_amuletofcatspeak")
    }
}
