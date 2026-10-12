package org.rsmod.content.areas.city.portphasmatys.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class PortPhasmatysGhosts : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.ahoy_ghost_villager") { startDialogue(it.npc) { villager() } }
        onOpNpc1("npc.ahoy_ghost_protestor") { startDialogue(it.npc) { gravingas() } }
        onOpNpc1("npc.ahoy_ghost_sailor") { startDialogue(it.npc) { sailor() } }
        onOpNpc1("npc.ahoy_ghost_captain_2") { startDialogue(it.npc) { ghostshipCaptain() } }
        onOpNpc1("npc.ahoy_necrovarus") { startDialogue(it.npc) { necrovarus() } }
        onOpNpc1("npc.makinghistory_droalak") { startDialogue(it.npc) { droalak() } }
        onOpNpc1("npc.makinghistory_melina") { startDialogue(it.npc) { melina() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ghost_villager
    // TODO(content): [quest:ghosts_ahoy] the petition signatures while collecting them
    private suspend fun Dialogue.villager() {
        if (!player.understandsGhosts()) {
            return wail()
        }
        when (access.random.of(5)) {
            0 -> chatNpc(neutral, "What do you want, mortal?")
            1 -> chatNpc(angry, "We do not talk to the warm-bloods.")
            2 -> chatNpc(sad, "Why did we have to listen to that maniacal priest?")
            3 -> chatNpc(neutral, "This cold wind blows right through you, doesn't it?")
            else ->
                chatNpc(angry, "Worship the Ectofuntus all you want, but don't bother us, human.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gravingas (before Ghosts Ahoy)
    // TODO(content): [quest:ghosts_ahoy] the petition and the after-quest answer
    // TODO(content): [system:npc_overhead] the "Down with Necrovarus!!" protest shouts
    private suspend fun Dialogue.gravingas() {
        if (!player.understandsGhosts()) {
            return wail()
        }
        chatNpc(
            angry,
            "Will you join with me and protest against the evil ban of Necrovarus and his " +
                "disciples?",
        )
        chatPlayer(neutral, "I'm sorry, I don't really think I should get involved.")
        chatNpc(sad, "Ah, the youth of today - so apathetic to politics.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ghost_sailor
    private suspend fun Dialogue.sailor() {
        if (!player.understandsGhosts()) {
            return wail(cannotUnderstand = false)
        }
        chatPlayer(
            quiz,
            "Hi there. Why do you still bother having ships here? I mean - you're dead, what use " +
                "are they to you?",
        )
        chatNpc(
            neutral,
            "We keep ships because we still need trade in Phasmatys. Every trader that comes to " +
                "Phasmatys is made to worship the Ectofuntus, so that the Ectopower doesn't run " +
                "out.",
        )
        chatPlayer(quiz, "So, without traders to worship in the Temple you're history, right?")
        chatNpc(neutral, "Aye, matey.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ghost_captain_(ghostship)
    private suspend fun Dialogue.ghostshipCaptain() {
        if (!player.understandsGhosts()) {
            return wail(cannotUnderstand = false)
        }
        chatPlayer(quiz, "Where do you sail to?")
        chatNpc(
            sad,
            "At the moment, nowhere. The mini-Ectofuntus that powers the Ghostship is in major " +
                "need of repair, and until it gets it, this ship is going nowhere.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Necrovarus (before Ghosts Ahoy)
    // https://oldschool.runescape.wiki/w/Transcript:Ghosts_Ahoy
    // TODO(content): [quest:ghosts_ahoy] quest and after-quest dialogue
    private suspend fun Dialogue.necrovarus() {
        if (!player.understandsGhosts()) {
            return wail()
        }
        val option =
            choice3(
                "What is this place?",
                1,
                "What happened to everyone here?",
                2,
                "How do I get into the town?",
                3,
            )
        when (option) {
            1 -> chatPlayer(quiz, "What is this place?")
            2 -> chatPlayer(quiz, "What happened to everyone here?")
            3 -> chatPlayer(quiz, "How do I get into the town?")
        }
        when (access.random.of(5)) {
            0 -> chatNpc(verymad, "Speak to me again and I will rend the soul from your flesh.")
            1 -> chatNpc(verymad, "Nobody speaks to me unless I speak to them!!!")
            2 -> chatNpc(verymad, "I do not answer questions, mortal fool!!")
            3 -> chatNpc(verymad, "I will answer questions when you are dead!!")
            else -> chatNpc(verymad, "You dare to speak to me??? Have you lost your wits????")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Droalak (Making History not started)
    // TODO(content): [quest:making_history] quest and after-quest dialogue
    private suspend fun Dialogue.droalak() {
        if (!player.understandsGhosts()) {
            return wail(text = "wooo wooo", cannotUnderstand = false)
        }
        chatNpc(sad, "Please, leave me alone.")
        chatPlayer(neutral, "Your loss!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Melina
    // TODO(content): [quest:making_history] quest dialogue
    // TODO(content): [verify:in_game] Melina's reply without a ghostspeak amulet
    private suspend fun Dialogue.melina() {
        if (!player.understandsGhosts()) {
            return wail(cannotUnderstand = false)
        }
        chatNpc(sad, "Leave me be.")
    }
}
