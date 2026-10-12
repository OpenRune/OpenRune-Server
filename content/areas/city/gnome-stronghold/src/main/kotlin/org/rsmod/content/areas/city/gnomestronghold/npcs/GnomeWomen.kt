package org.rsmod.content.areas.city.gnomestronghold.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class GnomeWomen @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.gnomefemale") { startDialogue(it.npc) { gnomeWoman() } }
        onOpNpc1("npc.gnomefemale_dskinned") { startDialogue(it.npc) { gnomeWoman() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gnome_woman
    private suspend fun Dialogue.gnomeWoman() {
        chatPlayer(happy, "Hello.")
        when (access.random.of(6)) {
            0 -> {
                chatNpc(happy, "Hello adventurer, here's some wise words.")
                chatPlayer(neutral, "OK.")
                chatNpc(
                    happy,
                    "Happiness is inward and not outward. So it does not depend on what we have " +
                        "but on what we are!",
                )
            }
            1 -> {
                chatNpc(
                    happy,
                    "Some people grumble because roses have thorns. I'm thankful that thorns " +
                        "have roses!",
                )
                chatPlayer(happy, "Good attitude!")
            }
            2 -> {
                chatNpc(happy, "Well good day to you. Are you new to these parts?")
                chatPlayer(neutral, "Kind of.")
                chatNpc(
                    happy,
                    "Well if you're looking for a good night out: Blurberry's cocktail bar is " +
                        "great!",
                )
            }
            3 -> {
                chatNpc(happy, "Hi. I've never seen so many humans in my life.")
                chatPlayer(happy, "I've never seen so many gnomes!")
                chatNpc(happy, "So we're both learning.")
            }
            4 -> {
                chatPlayer(quiz, "How are you?")
                chatNpc(neutral, "Not bad, a little worn out.")
                chatPlayer(neutral, "Maybe you should have a lie down.")
                chatNpc(neutral, "With three kids to feed I've no time for naps!")
                chatPlayer(neutral, "Sounds like hard work!")
                chatNpc(happy, "It is but they're worth it.")
            }
            else -> {
                chatNpc(quiz, "Hello traveller. Are you eating properly? You look tired.")
                chatPlayer(neutral, "I think so.")
                chatNpc(happy, "Here get this worm down you. It'll do you the world of good.")
                // TODO(content): [verify:in_game] what happens with a full inventory
                player.invAddOrDrop(objRepo, KING_WORM)
                objbox(KING_WORM, "The gnome gives you a worm")
                chatPlayer(happy, "Thanks!")
            }
        }
    }

    private companion object {
        const val KING_WORM = "obj.king_worm"
    }
}
