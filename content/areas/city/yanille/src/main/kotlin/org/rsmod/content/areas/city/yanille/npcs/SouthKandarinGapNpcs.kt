package org.rsmod.content.areas.city.yanille.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class SouthKandarinGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.grandtree_hazelmere") { startDialogue(it.npc) { hazelmere() } }
        onOpNpc1("npc.rc_zmi_chaosmonk") { startDialogue(it.npc) { mageOfZamorak() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hazelmere
    // TODO(content): [quest:the_eyes_of_glouphrie] the mind meld after the quest
    // TODO(content): [quest:the_path_of_glouphrie] the mind meld after the quest
    private suspend fun Dialogue.hazelmere() {
        mesbox("The mage starts to speak but all you hear is:")
        chatNpc(neutral, "Blah. Blah, blah, blah, blah...blah!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Mage_of_Zamorak_(Ourania)
    private suspend fun Dialogue.mageOfZamorak() {
        chatPlayer(quiz, "What's that ladder next to the altar for?")
        chatNpc(shifty, "An...archeological dig. Yeah, a dig.")
        chatNpc(quiz, "Why?")
        chatPlayer(
            quiz,
            "Next to a Chaos Altar? That's a bit odd. Can I go down and have a look?",
        )
        chatNpc(
            neutral,
            "Well...I suppose. See, we found this ancient altar, but it was pretty broken so the " +
                "Z.M.I. sent some of its researchers to try and repair it.",
        )
        chatPlayer(quiz, "How'd that work out for them?")
        chatNpc(
            neutral,
            "Pretty well - only one died. Still, they got it working...sort of. This ancient " +
                "technology can be tricky.",
        )
        chatPlayer(quiz, "What do you mean, 'sort of'? Altars either work or don't work.")
        chatNpc(
            neutral,
            "This one works, just not as you'd expect. You put pure essence in, but get random " +
                "runes back. Some of them we don't even know how to craft!",
        )
        chatPlayer(happy, "Sounds like a pretty good deal. Can I give it a go?")
        chatNpc(
            neutral,
            "I don't think so. The mages don't like outsiders going in; they'll probably attack " +
                "you. They only patrol the short path, though, so if you used the long path, " +
                "through a tunnel near the entrance, you'd probably be safe.",
        )
        chatPlayer(happy, "Thanks.")
    }
}
