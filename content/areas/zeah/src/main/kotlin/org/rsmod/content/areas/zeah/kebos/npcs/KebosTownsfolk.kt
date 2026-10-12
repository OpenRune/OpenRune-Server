package org.rsmod.content.areas.zeah.kebos.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class KebosTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        for (type in SERGEANTS) {
            onOpNpc1(type) { startDialogue(it.npc) { sergeant() } }
        }
        onOpNpc1("npc.molch_shay_general") { startDialogue(it.npc) { generalVir() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sergeant_(Kebos_Lowlands)
    // TODO(content): [quest:getting_ahead] the sergeant's quest dialogue
    private suspend fun Dialogue.sergeant() {
        chatPlayer(quiz, "Hello. What are you doing here?")
        chatNpc(neutral, "We're here to keep the farmers safe from the lizardmen.")
        chatPlayer(quiz, "There's not many of you though.")
        chatNpc(
            neutral,
            "Luckily the lizardmen have been focusing their efforts elsewhere. They haven't " +
                "tried to come up here yet.",
        )
        chatNpc(neutral, "If they do, we can quickly call in reinforcements if required.")
        chatPlayer(neutral, "I see. Well I'd better let you get on then.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:General_Vir
    private suspend fun Dialogue.generalVir() {
        chatPlayer(quiz, "Are you in charge here?")
        chatNpc(
            neutral,
            "Yes. Is there something important you need? We're currently quite occupied.",
        )
        chatPlayer(quiz, "Oh yeah?")
        chatNpc(angry, "Yes... We're currently attacking the Lizardmen, are you blind?!")
        chatPlayer(neutral, "I just wanted a chat.")
        chatNpc(verymad, "A chat? Who let you into this camp? GET OUT!")
        chatPlayer(sad, "No need to shout. I guess I'm not wanted here.")
        chatNpc(
            angry,
            "You are correct! Unless you plan to help with the war effort, you should leave. Now.",
        )
        chatPlayer(angry, "Hmph, you need to work on your people skills.")
        chatNpc(angry, "You need to leave.")
        chatPlayer(neutral, "Bye.")
    }

    private companion object {
        val SERGEANTS =
            listOf(
                "npc.farming_guild_guard_2",
                "npc.farming_guild_guard_2_alt",
                "npc.ga_sergeant",
            )
    }
}
