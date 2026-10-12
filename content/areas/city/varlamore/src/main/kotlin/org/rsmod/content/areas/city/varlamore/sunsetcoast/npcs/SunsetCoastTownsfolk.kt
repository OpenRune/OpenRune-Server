package org.rsmod.content.areas.city.varlamore.sunsetcoast.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class SunsetCoastTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.sunset_coast_fremennik_1") { startDialogue(it.npc) { reinn() } }
        onOpNpc1("npc.sunset_coast_fremennik_2") { startDialogue(it.npc) { inga() } }
        onOpNpc1("npc.sunset_coast_fremennik_3") { startDialogue(it.npc) { arnkel() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Reinn_(Sunset_Coast)
    // TODO(content): [verify:in_game] varp.colosseum_glory holds the glory that sets the
    //  player's rank (Brawler at 2,000, https://oldschool.runescape.wiki/w/Fortis_Colosseum)
    private suspend fun Dialogue.reinn() {
        if (!isFremennik) {
            chatNpc(happy, "Good day, traveller.")
            chatPlayer(happy, "And to you.")
            return
        }
        chatNpc(happy, "Good to see you, ${fremennikName()}!")
        chatPlayer(happy, "Hello there. You're a long way from home.")
        chatNpc(
            laugh,
            "Well what sort of Fremennik are you if you don't travel! Besides, we heard this was " +
                "the land to visit for breaking skulls and spilling blood!",
        )
        chatPlayer(quiz, "Is it?")
        if (vars[COLOSSEUM_GLORY] >= BRAWLER_GLORY) {
            chatNpc(
                laugh,
                "Ha! As if you didn't know! We've heard about your exploits in the Colosseum! " +
                    "Maybe you'll see us next time you're there!",
            )
            return
        }
        chatNpc(
            happy,
            "Of course! Have you not visited the Colosseum? You won't regret it if you do.",
        )
        chatPlayer(neutral, "Hmm... I might have to check it out.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Inga_(Sunset_Coast)
    private suspend fun Dialogue.inga() {
        chatNpc(
            happy,
            "I think the locals are taking a liking to us! One of them hasn't stopped wearing " +
                "those clothes Arnkel sold her. Much better than those piddly little things she " +
                "was wearing before!",
        )
        chatPlayer(neutral, "I think they're piddly because of how warm it is here...")
        chatNpc(laugh, "Bah! A bit of heat never hurt anyone.")
        chatPlayer(neutral, "I don't think that's true.")
        val name = if (isFremennik) fremennikName() else "traveller"
        chatNpc(angry, "Oh stop your whining, $name! Now, I think it's time for a drink.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Arnkel
    private suspend fun Dialogue.arnkel() {
        chatNpc(
            happy,
            "Today is a fine day! First, I intend to drink. Next, I intend to fight. Finally, I " +
                "intend to contemplate our existence in this confusing world.",
        )
        chatPlayer(neutral, "That's quite the day.")
        chatNpc(happy, "And a fine day it will be!")
    }

    private companion object {
        const val COLOSSEUM_GLORY = "varp.colosseum_glory"
        const val BRAWLER_GLORY = 2_000
    }
}
