package org.rsmod.content.areas.city.keldagrim.keldagrim.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class Factory : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.dwarf_city_factory_guard") { startDialogue(it.npc) { factoryManager() } }
        for ((worker, variant) in WORKERS) {
            onOpNpc1(worker) { startDialogue(it.npc) { factoryWorker(variant) } }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Factory_Manager
    private suspend fun Dialogue.factoryManager() {
        chatNpc(angry, "Don't bother me, can't you see I'm busy?")
        chatPlayer(quiz, "No, I can't... can I go into the factory?")
        chatNpc(
            neutral,
            "You can go in. The Consortium, in their infinite wisdom, has decided to open up " +
                "the factory to all and sundry.",
        )
        chatPlayer(quiz, "How come?")
        chatNpc(
            neutral,
            "We've built a new blast furnace and always have a shortage of people to work on it.",
        )
        chatPlayer(happy, "I'll have a look, thanks.")
        chatNpc(angry, "Just don't go causing any trouble, I still run this factory!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Factory_Worker
    // TODO(content): [verify:in_game] which worker npc speaks which transcript variant
    private suspend fun Dialogue.factoryWorker(variant: Int) {
        when (variant) {
            1 -> {
                chatPlayer(quiz, "I'm sorry, I'm looking for the blast furnace?")
                chatNpc(angry, "Do I look like a guide to you?")
                chatPlayer(
                    neutral,
                    "No, you look like a hard-working dwarf, but can you please tell me where " +
                        "the blast furnace is?",
                )
                chatNpc(neutral, "Alright, just head down the stairs, it's easy to find.")
                chatPlayer(happy, "Thanks.")
            }
            2 -> {
                chatPlayer(quiz, "Are you okay?")
                chatNpc(confused, "Don't I look okay?")
                chatPlayer(laugh, "If you were any shorter you wouldn't exist.")
                chatNpc(angry, "Very funny, human.")
            }
            3 -> {
                chatPlayer(quiz, "What are you dwarves doing in this factory?")
                chatNpc(angry, "Working of course, can't you see that?")
                chatPlayer(quiz, "But working on what?")
                chatNpc(
                    neutral,
                    "Refining the ore that is being brought into the factory, of course.",
                )
                chatPlayer(quiz, "And what does that mean?")
                chatNpc(
                    angry,
                    "It means you should stop asking so many questions and get back to work!",
                )
            }
            else -> {
                chatPlayer(quiz, "Who owns this factory?")
                chatNpc(neutral, "The Consortium does and that's all you need to know.")
                chatPlayer(
                    quiz,
                    "But what company? I thought there were all these different companies?",
                )
                chatNpc(
                    neutral,
                    "Oh yes, all the major companies own this plant. It's too vital to be in the " +
                        "hands of one company alone.",
                )
                chatPlayer(quiz, "And what exactly are you doing here?")
                chatNpc(angry, "I tire of these questions. Let me get back to work!")
            }
        }
    }

    private companion object {
        val WORKERS =
            mapOf(
                "npc.dwarf_city_factory_worker1" to 1,
                "npc.dwarf_city_factory_worker2" to 2,
                "npc.dwarf_city_factory_worker3" to 3,
                "npc.dwarf_city_factory_worker4" to 4,
            )
    }
}
