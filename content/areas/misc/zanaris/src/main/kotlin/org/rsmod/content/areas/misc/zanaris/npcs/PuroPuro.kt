package org.rsmod.content.areas.misc.zanaris.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class PuroPuro : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.ii_immenizz") { startDialogue(it.npc) { immenizz() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Immenizz
    // TODO(content): [system:treasure_trails] master clue puzzle box
    private suspend fun Dialogue.immenizz() {
        chatPlayer(quiz, "Hello. So, what is this place?")
        chatNpc(
            happy,
            "This is my home, mundane human! What do you have in your pockets? Something tasty?",
        )
        chatPlayer(angry, "Stay out of my pockets! I don't have anything that you want.")
        chatNpc(shifty, "Ah, but do you have anything that *you* want?")
        chatPlayer(neutral, "Of course I do!")
        chatNpc(happy, "Then you have something that implings want.")
        chatPlayer(confused, "Eh?")
        chatNpc(
            happy,
            "We want things you people want. They are tasty to us! The more you want them, the " +
                "tastier they are!",
        )
        chatPlayer(
            quiz,
            "So, you collect things that humans want? Interesting... So, what would happen if I " +
                "caught an impling in a butterfly net?",
        )
        chatNpc(
            worried,
            "Don't do that! That would be cruel. But chase us, yes! That is good. Implings are " +
                "not easy to catch. Especially ones with really tasty food.",
        )
        chatPlayer(
            neutral,
            "So, some of these implings have things that I will really want? Hmm, maybe it would " +
                "be worth my while trying to catch some.",
        )
    }
}
