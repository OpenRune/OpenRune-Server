package org.rsmod.content.areas.city.lumbridge.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class HamHideoutGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.favour_jimmy") {
            startDialogue(it.npc) { chatNpc(happy, "Hello mate!") }
        }
        onOpNpc1("npc.favour_johanhus_ulsbrecht") { startDialogue(it.npc) { johanhus() } }
        onOpNpc1("npc.favour_npc_floor") { startDialogue(it.npc) { seatedMember() } }
        onOpNpc1("npc.favour_npc_chair") { startDialogue(it.npc) { seatedMember() } }
        onOpNpc1("npc.lost_tribe_sigmund_there") { startDialogue(it.npc) { sigmund() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Johanhus_Ulsbrecht
    private suspend fun Dialogue.johanhus() {
        val address = if (access.isBodyTypeA()) "brother" else "sister"
        chatNpc(
            neutral,
            "Welcome $address... to our organisation... Ours is a fight for humanity in all its " +
                "shapes and forms... and to rid mankind of the monsters that exist in our world.",
        )
        while (true) {
            val option =
                choice3(
                    "What kind of organisation is this?",
                    1,
                    "Who are you and what do you do here?",
                    2,
                    "Ok, thanks.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What kind of organisation is this?")
                    chatNpc(
                        neutral,
                        "We're a proactive organisation working towards the cessation of " +
                            "monsters in normal civilised human society. There seems to be no " +
                            "backbone in this land so we're stepping up to the challenge.",
                    )
                    chatPlayer(
                        quiz,
                        "Hmm, how do you propose to do that? I mean, I see people everyday " +
                            "killing goblins around here!",
                    )
                    chatNpc(
                        neutral,
                        "We're mobilising people and we're starting our own society...cleaning " +
                            "out the caves once inhabited by these foul creatures and defending " +
                            "them so that they can never again shelter sub human species.",
                    )
                    chatPlayer(neutral, "That sounds kind of strange, but hey it's your choice.")
                }
                2 -> {
                    chatPlayer(quiz, "Who are you and what do you do here?")
                    chatNpc(
                        neutral,
                        "My name is Johanhus and I lead these glorious people on a courageous " +
                            "mission called, 'Humans against Monsters', we mean to make this " +
                            "land free of monsters so we can all live in peace.",
                    )
                }
                else -> {
                    chatPlayer(neutral, "Okay, thanks.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:H.A.M._Member_(seated)
    private suspend fun Dialogue.seatedMember() {
        while (true) {
            val option =
                choice5(
                    "What are all you people doing here?",
                    1,
                    "Who are you and what do you do here?",
                    2,
                    "What do you think you're going to achieve?",
                    3,
                    "Where did all you people come from?",
                    4,
                    "Ok, thanks.",
                    5,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What are all you people doing here?")
                    chatNpc(neutral, MEMBER_REASONS[access.random.of(MEMBER_REASONS.size)])
                }
                2 -> {
                    chatPlayer(quiz, "Who are you and what do you do here?")
                    chatNpc(
                        neutral,
                        "I'm a strong believer in the non-monsters policy...we should really get " +
                            "rid of them...and if that means I have to live in a cave like a " +
                            "monster, so be it!",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "What do you think you're going to achieve?")
                    chatNpc(
                        neutral,
                        "We want a world without monsters, to live in safety and without fear of " +
                            "being attacked by these ferocious beasts.",
                    )
                    chatPlayer(
                        neutral,
                        "But there aren't that many ferocious beasts in the towns and cities.",
                    )
                    chatNpc(
                        angry,
                        "That's not enough, we want to get rid of them totally, we want to enjoy " +
                            "the surrounding lands and not worry about our children playing in " +
                            "caves and so on.",
                    )
                }
                4 -> {
                    chatPlayer(quiz, "Where did all you people come from?")
                    chatNpc(
                        neutral,
                        "Most of us came from small towns that had been attacked by monsters. We " +
                            "all got fed up with it and so decided to join this movement. We're " +
                            "hoping to return to the towns and cities when we've cleaned up the " +
                            "areas",
                    )
                    chatNpc(neutral, "that these monsters live in.")
                    return
                }
                else -> {
                    chatPlayer(neutral, "Ok, thanks.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sigmund (after The Lost Tribe)
    private suspend fun Dialogue.sigmund() {
        chatNpc(
            angry,
            "...And I would have gotten away with it too, if it wasn't for that pesky adventurer!",
        )
    }

    private companion object {
        val MEMBER_REASONS =
            listOf(
                "Many of us disagree with the king about what freedoms the local monster " +
                    "population should have. We're taking a stand and mobilising our forces " +
                    "against the monstrous hordes.",
                "I'm totally in awe of Johanhus, he really knows what's what. I know he keeps " +
                    "going on about monsters and it's clear there are too many of them, so hey, " +
                    "I agree with whatever Johanhus says.",
                "We're against the monsters..like Johanhus says...we don't like them...you know.",
            )
    }
}
