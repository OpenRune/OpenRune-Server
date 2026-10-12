package org.rsmod.content.areas.misc.lobbies.troublebrewing.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Honest_Jimmy
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:shops] Honest Jimmy's House of Stuff (inv.brew_reward_shop) needs a
//  pieces of eight shop currency: Trade op and every "here for the stuff" option
// TODO(content): [system:treasure_trails] selling a medium clue scroll for 5000 Pieces of
//  Eight after "You sure you want in on this?"
// TODO(content): [system:trouble_brewing] Join-Team option and the lobby overheads
class HonestJimmy : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.honest_jimmy") { startDialogue(it.npc) { jimmy() } }
    }

    private suspend fun Dialogue.jimmy() {
        chatNpc(shifty, "You here for the...stuff?")
        chatPlayer(confused, "What stuff?")
        val title = if (access.isBodyTypeA()) "pal" else "miss"
        chatNpc(shifty, "Look $title I got the goods if you have the cash.")
        chatNpc(shifty, "We talkin' the same language yet?")
        val option =
            choice4(
                "Yes, I am here for the stuff.",
                1,
                "What stuff is this again?",
                2,
                "I hear you are the man to talk to about maps.",
                3,
                "I have no idea what you mean.",
                4,
            )
        when (option) {
            1 -> return
            2 -> whatStuff()
            3 -> maps()
            4 -> {
                chatPlayer(confused, "I have no idea what you are talking about.")
                slingYerHook()
            }
        }
    }

    private suspend fun Dialogue.whatStuff() {
        chatPlayer(confused, "What stuff is this again?")
        chatNpc(shifty, "The sort of stuff you use in brewing.")
        chatNpc(shifty, "The sort of stuff the Cooking Guild banned last year.")
        chatNpc(
            shifty,
            "The sort of stuff that (if added to a vat of brewing ingredients just before the " +
                "hops) has a higher chance of producin' a mature batch of whatever ale you " +
                "wanted to make.",
        )
        chatNpc(shifty, "That sort of stuff.")
        chatPlayer(quiz, "What was that bit about adding it to something?")
        chatNpc(
            neutral,
            "I said, it's the sort of Stuff you add to a brewing vat after you have put the " +
                "water and barley malt in.",
        )
        chatNpc(
            neutral,
            "THEN you add the sort of stuff I'm talking about, followed by the hops and the yeast.",
        )
        chatNpc(
            neutral,
            "Then, when whatever you are making brews it will have a higher chance of being a " +
                "mature batch of ale.",
        )
        chatNpc(
            angry,
            "That clear enough for you or am I going to have to start drawin' pictures on the " +
                "wall of the hut?",
        )
        if (!choice2("Yes, I am here for the stuff.", true, "No.", false)) {
            chatPlayer(neutral, "No.")
            slingYerHook()
        }
    }

    private suspend fun Dialogue.maps() {
        chatPlayer(quiz, "I hear you are the man to talk to about maps.")
        chatNpc(shifty, "I might be, you got something for me?")
        chatPlayer(confused, "Like what?")
        chatNpc(shifty, "Like...5000 Pieces of Eight?")
        if (access.invTotal(player.inv, PIECES_OF_EIGHT) >= MAP_PRICE) {
            chatPlayer(
                shifty,
                "I might have 5000 Pieces of Eight with your name on them if you have a map for " +
                    "me.",
            )
            chatNpc(
                shifty,
                "Look, I might have a little something for you for that amount of cash, but I'll " +
                    "have to check.",
            )
            chatNpc(shifty, "You sure you want in on this?")
            return
        }
        chatPlayer(worried, "Well, no but...")
        chatNpc(angry, "Look, you want something or not?")
        if (!choice2("Yes, what are you selling?", true, "No.", false)) {
            chatPlayer(neutral, "No.")
            slingYerHook()
        }
    }

    private suspend fun Dialogue.slingYerHook() {
        chatNpc(angry, "Then sling yer 'ook!")
    }

    private companion object {
        const val PIECES_OF_EIGHT = "obj.brew_piece_of_eight"
        const val MAP_PRICE = 5000
    }
}
