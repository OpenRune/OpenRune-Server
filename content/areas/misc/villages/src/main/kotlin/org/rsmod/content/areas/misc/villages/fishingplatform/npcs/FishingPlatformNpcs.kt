package org.rsmod.content.areas.misc.villages.fishingplatform.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class FishingPlatformNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.bailey") { startDialogue(it.npc) { bailey() } }
        for (fisherman in FISHERMEN) {
            onOpNpc1(fisherman) { startDialogue(it.npc) { fisherman() } }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bailey (after Sea Slug)
    // TODO(content): [quest:sea_slug] Bailey's quest dialogue
    private suspend fun Dialogue.bailey() {
        chatPlayer(happy, "Hello Bailey.")
        chatNpc(
            happy,
            "Well, hello again, ${player.displayName}. What brings you back out here?",
        )
        chatPlayer(neutral, "Just looking around.")
        chatNpc(
            worried,
            "Well, be very careful out there! Those dratted slugs have been joined by giant " +
                "lobsters!",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fisherman_(Fishing_Platform)
    private suspend fun Dialogue.fisherman() {
        chatPlayer(happy, "Hello there.")
        mesbox("His eyes are staring vacantly into space.")
        when (access.random.of(6)) {
            0 -> {
                chatNpc(neutral, "You are not part of our family...")
                chatPlayer(confused, "Umm. Not last time I checked.")
                chatNpc(neutral, "Soon you will be... Soon you will...")
            }
            1 -> {
                chatNpc(sad, "Lost to us.. She is Lost to us...")
                chatPlayer(quiz, "Who is lost?")
                chatNpc(sad, "Trapped by the light... Lost and trapped...")
                chatPlayer(confused, "Ermm... So you don't want to tell me then?")
                chatNpc(sad, "Trapped... In stone and darkness...")
            }
            2 -> {
                chatNpc(neutral, "Must find family...")
                chatPlayer(confused, "What?")
                chatNpc(neutral, "Soon we will all be together...")
                chatPlayer(quiz, "Are you ok?")
                chatNpc(
                    neutral,
                    "Must find family... They are all under the blue... Deep deep under the " +
                        "blue...",
                )
                chatPlayer(confused, "Ermm... I'll leave you to it then.")
            }
            3 -> {
                chatNpc(neutral, "Free of the deep blue we are...")
                chatNpc(neutral, "We must find...")
                chatPlayer(quiz, "Yes?")
                chatNpc(neutral, "a new home...")
                chatNpc(neutral, "We must leave this place...")
                chatPlayer(quiz, "Where will you go?")
                chatNpc(neutral, "Away.. Away to her...")
                chatPlayer(confused, "Riiight.")
            }
            4 -> {
                chatNpc(neutral, "Below the deep, deep blue she waits...")
                chatPlayer(quiz, "Who waits?")
                chatNpc(neutral, "They came to her with fire and faith...")
                chatPlayer(quiz, "Who? Who came to who?")
                chatNpc(neutral, "Too many... Too many...")
                chatPlayer(angry, "Too many what? Make sense!")
                chatNpc(neutral, "Locked away for all eternity...")
                chatPlayer(angry, "You'd better start making sense Sonny Jim or I'll...")
                chatNpc(neutral, "Free... Soon to be free...")
            }
            else -> {
                chatNpc(neutral, "Must escape the blue.. Deep deep blue")
                chatPlayer(confused, "Pardon?")
                chatNpc(neutral, "Family... Under the blue... Must escape the blue...")
            }
        }
    }

    private companion object {
        val FISHERMEN =
            listOf(
                "npc.fishplatform1",
                "npc.fishplatform2",
                "npc.fishplatform3",
                "npc.fishplatform4",
            )
    }
}
