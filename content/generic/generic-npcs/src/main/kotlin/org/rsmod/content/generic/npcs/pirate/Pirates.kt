package org.rsmod.content.generic.npcs.pirate

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class Pirates : PluginScript() {
    override fun ScriptContext.startup() {
        for (pirate in PIRATES) {
            onOpNpc1(pirate) { startDialogue(it.npc) { pirate() } }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Pirate
    private suspend fun Dialogue.pirate() {
        chatPlayer(happy, "Hello!")
        when (val line = access.random.of(GREETINGS.size + 4)) {
            in GREETINGS.indices -> chatNpc(laugh, GREETINGS[line])
            GREETINGS.size -> {
                chatNpc(laugh, "Oooh arrh!")
                chatNpc(neutral, "No wait. That's a farmer.")
            }
            GREETINGS.size + 1 -> {
                chatNpc(laugh, "Avast behind!")
                chatPlayer(angry, "I'm not that fat!")
            }
            GREETINGS.size + 2 -> {
                chatNpc(laugh, "Pieces of eight! pieces of eight!")
                chatNpc(neutral, "Oh wait that's the parrot's line.")
            }
            else -> {
                chatNpc(laugh, "I'm the scourge of the six seas!")
                chatPlayer(quiz, "Don't you mean seven seas?")
                chatNpc(laugh, "I'm the scourge of the seven seas!")
            }
        }
    }

    private companion object {
        val PIRATES =
            listOf(
                "npc.pirate1",
                "npc.pirate2",
                "npc.zeah_pirate1",
                "npc.zeah_pirate2",
                "npc.zeah_pirate3",
            )

        val GREETINGS =
            listOf(
                "Yo ho ho and bottle of alcopop!",
                "Good day to you my dear sir!",
                "Arrh be off with ye!",
                "3 days at port for resupply then out on the high sea!",
                "Arrh arrh!",
                "Arrh ye scurvy sea dog!",
                "A pox on ye!",
                "Arrh! I be in search of buried treasure!",
                "Ahoy there!",
                "Avast me hearties!",
                "Shiver me timbers!",
                "You'll be joining Davey Jones in his locker!",
                "Arrrh ye lily livered landlubber!",
                "Arrh!",
                "Batton down the hatches there's a storm a brewin!",
                "All hands on deck!",
                "Yo ho ho me hearties!",
                "Avast behind!",
                "Man overboard!",
                "Keel haul them I say!",
                "I think ye'll be taking a long walk off a short plank!",
                "Splice the mainbrace!",
                "I'm the scourge of the seven seas!",
                "Yo ho ho and a bottle of rum!",
                "Great blackbeard's beard!",
                "Arrh I'll keel haul ye!",
            )
    }
}
