package org.rsmod.content.areas.city.alkharid.npcs

import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Ali_the_Leaflet_Dropper
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:npc_overhead] leaflet dropper overhead lines
// TODO(content): [system:treasure_trails] easy clue reward
class AliTheLeafletDropper : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.ali_leaflet_dropper") { startDialogue(it.npc) { leafletDropper() } }
    }

    private suspend fun Dialogue.leafletDropper() {
        chatNpc(
            angry,
            "I don't have time to talk right now! Ali Morrisane is paying me to hand out these " +
                "flyers.",
        )
        val option =
            choice3(
                "Who is Ali Morrisane?",
                1,
                "What are the flyers for?",
                2,
                "What is there to do round here, boy?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Who is Ali Morrisane?")
                chatNpc(happy, "Ali Morrisane is the greatest merchant in the east!")
                chatPlayer(shifty, "Were you paid to say that?")
                chatNpc(happy, "Of course I was! You can find him on the north edge of town.")
            }
            2 -> flyers()
            3 -> {
                chatPlayer(quiz, "What is there to do round here, boy?")
                chatNpc(angry, "I'm very busy, so listen carefully! I shall say this only once.")
                chatNpc(
                    neutral,
                    "Apart from a busy and wonderous market place in Al Kharid to the south, " +
                        "there is the Emir's Arena to the south-east where you can challenge " +
                        "other players to a fight.",
                )
                chatNpc(neutral, "If you're here to make money, there is a mine to the south.")
                chatNpc(
                    neutral,
                    "Watch out for scorpions though, they'll take a pop at you if you go too " +
                        "near them. To avoid them just follow the western fence as you travel " +
                        "south.",
                )
                chatNpc(
                    neutral,
                    "If you're in the mood for a little rest and relaxation, there are a couple " +
                        "of nice fishing spots south of the town.",
                )
                chatPlayer(happy, "Thanks for the help!")
            }
        }
    }

    private suspend fun Dialogue.flyers() {
        chatPlayer(quiz, "What are the flyers for?")
        chatNpc(
            neutral,
            "Well, Ali Morrisane isn't too popular with the other traders in Al Kharid, mainly " +
                "because he's from Pollnivneach and they feel he has no business trading in " +
                "their town.",
        )
        chatNpc(
            neutral,
            "I think they're just sour because he's better at making money than them.",
        )
        chatNpc(neutral, "The flyer advertises the different shops you can find in Al Kharid.")
        chatNpc(
            neutral,
            "It also entitles you to money off your next purchase in any of the shops listed on " +
                "it. It's Ali's way of getting on the good side of the traders.",
        )
        chatPlayer(quiz, "Which shops?")
        if (player.inv.count(FLYER) > 0) {
            chatNpc(
                angry,
                "Are you trying to be funny or has age turned your brain to mush? Look at the " +
                    "flyer you already have!",
            )
            return
        }
        if (!player.invAdd(player.inv, FLYER).success) {
            chatNpc(
                neutral,
                "I'd give you a flyer but it looks like your hands are full. Come back when you " +
                    "have space for my flyer.",
            )
            return
        }
        chatNpc(happy, "Here! Take one and let me get back to work.")
        chatNpc(
            shifty,
            "I still have hundreds of these flyers to hand out, I wonder if my boss would notice " +
                "if I quietly dumped them somewhere?",
        )
    }

    private companion object {
        const val FLYER = "obj.leaflet_dropper_flyer"
    }
}
