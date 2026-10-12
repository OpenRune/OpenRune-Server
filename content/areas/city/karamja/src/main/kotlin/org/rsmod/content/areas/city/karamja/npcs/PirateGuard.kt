package org.rsmod.content.areas.city.karamja.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Pirate_Guard
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:heroes_quest] lines when opening Grip's room
class PirateGuard : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.pirate_guard") { startDialogue(it.npc) { pirateGuard() } }
    }

    private suspend fun Dialogue.pirateGuard() {
        chatPlayer(happy, "Hello!")
        when (val line = access.random.of(GREETINGS.size + 4)) {
            in GREETINGS.indices -> chatNpc(happy, GREETINGS[line])
            GREETINGS.size -> {
                chatNpc(happy, "Oooh arrh!")
                chatNpc(neutral, "No wait. That's a farmer.")
            }
            GREETINGS.size + 1 -> {
                chatNpc(happy, "Avast behind!")
                chatPlayer(angry, "I'm not that fat!")
            }
            GREETINGS.size + 2 -> {
                chatNpc(happy, "Pieces of eight! pieces of eight!")
                chatNpc(neutral, "Oh wait that's the parrot's line.")
            }
            else -> {
                chatNpc(happy, "I'm the scourge of the six seas!")
                chatPlayer(confused, "Don't you mean seven seas?")
                chatNpc(happy, "I'm the scourge of the seven seas!")
            }
        }
    }

    private companion object {
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
