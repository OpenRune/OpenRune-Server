package org.rsmod.content.areas.city.falador.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The four thugs loitering on the road between Falador and Doric's hut. */
class DoricAreaThugs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.falador_doric_area_man1") { startDialogue(it.npc) { cuffs() } }
        onOpNpc1("npc.falador_doric_area_man2") { startDialogue(it.npc) { narf() } }
        onOpNpc1("npc.falador_doric_area_man3") { startDialogue(it.npc) { rusty() } }
        onOpNpc1("npc.falador_doric_area_man4") { startDialogue(it.npc) { jeff() } }
    }

    private suspend fun Dialogue.cuffs() {
        chatPlayer(quiz, "Hello. Nice day for a walk, isn't it?")
        chatNpc(shifty, "A walk? Oh, yes, that's what we're doing. We're just out here for a walk.")
        chatPlayer(
            shifty,
            "I'm glad you're just out here for a walk. A more suspicious person would think you " +
                "were waiting here to attack weak-looking travellers.",
        )
        chatNpc(
            shifty,
            "Nope, we'd never do anything like that. Just a band of innocent walkers, that's us.",
        )
        chatPlayer(shifty, "Alright, have a nice walk.")
    }

    private suspend fun Dialogue.narf() {
        chatPlayer(happy, "That's a funny name you've got.")
        chatNpc(
            angry,
            "'Narf'? You think that's funny? At least I don't call myself " +
                "'${player.displayName}'! Where did you get a name like that?",
        )
        chatPlayer(sad, "It seemed like a good idea at the time!")
        chatNpc(angry, "Bah!")
    }

    private suspend fun Dialogue.rusty() {
        chatNpc(happy, "Hiya. Are you carrying anything valuable?")
        chatPlayer(confused, "Why are you asking?")
        chatNpc(
            shifty,
            "Um... It's a quiz. I'm asking everyone I meet if they're carrying anything valuable.",
        )
        chatPlayer(neutral, "What would you do if I said I had loads of expensive items with me?")
        chatNpc(
            happy,
            "Ooh, do you? It's been ages since anyone said they'd got anything worth stealing.",
        )
        chatPlayer(angry, "'Anything worth stealing'?")
        chatNpc(confused, "Um... Not that I'd dream of stealing anything!")
        chatPlayer(shifty, "Well, I'll say I'm not carrying anything valuable at all.")
        chatNpc(sad, "Oh, what a shame.")
    }

    private suspend fun Dialogue.jeff() {
        chatNpc(quiz, "Tell me, is the guard still watching us?")
        chatPlayer(quiz, "Why would you care if there's a guard watching you?")
        chatNpc(shifty, "Oh, forget it.")
    }
}
