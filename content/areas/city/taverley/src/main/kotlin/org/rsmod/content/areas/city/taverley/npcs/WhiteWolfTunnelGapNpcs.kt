package org.rsmod.content.areas.city.taverley.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class WhiteWolfTunnelGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.aluft_pilot_ninto") { startDialogue(it.npc) { captainNinto() } }
        onOpNpc1("npc.dwarfrock_engineer2") { startDialogue(it.npc) { khorvak() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Captain_Ninto
    // TODO(content): [system:gnome_restaurant] food deliveries and tips
    private suspend fun Dialogue.captainNinto() {
        chatPlayer(quiz, "Hello, what are you doing here, so far from home?")
        chatNpc(drunk, "I'm enjoyn' the local hoschpitalieee. hee hee.")
        chatPlayer(neutral, "Looks like you've enjoyed more than your fair share of hospitality.")
        chatNpc(
            drunk,
            "tee hee, I probblie schoodn' 'av anuva drinkie right now. But dis dwarven beer ish " +
                "kind of moreish after the fiff pint.",
        )
        chatPlayer(neutral, "I'd go easy on the dwarven stout if I were you.")
        chatNpc(drunk, "I yousht to be a tesht pilot yoo know. I reeel hero.")
        chatNpc(drunk, "But I loscht my bottle. Scho I now ffind scholiss in the bottle.")
        chatPlayer(
            quiz,
            "What happened? Did you have a glider crash or get attacked by huge flying birds or " +
                "something?",
        )
        chatNpc(drunk, "Naaah, I realished I woz scared ov heightsh.")
        chatPlayer(
            neutral,
            "Ah, I can see that would be a problem ... You should keep an eye on your drinking, " +
                "though.",
        )
        chatNpc(drunk, "I'll try. Bofe eyez...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Khorvak,_a_dwarven_engineer
    private suspend fun Dialogue.khorvak() {
        chatPlayer(happy, "Hello there.")
        chatNpc(drunk, "Ooooh, heeellooo! Anyshing a bright dwarf like me can help you with?")
        chatPlayer(
            neutral,
            "The only thing you can help me with is finding the nearest place to get a drink, I " +
                "think...",
        )
        chatNpc(
            drunk,
            "That ish shimple! Help yourshelf to the dwarvern shtout! I mean dwarfen! I mean...",
        )
        chatNpc(drunk, "I'm not shure what I mean.")
    }
}
