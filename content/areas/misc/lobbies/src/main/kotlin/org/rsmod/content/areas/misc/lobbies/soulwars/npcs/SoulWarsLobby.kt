package org.rsmod.content.areas.misc.lobbies.soulwars.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class SoulWarsLobby : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.soul_wars_nomad") { startDialogue(it.npc) { nomad() } }
        onOpNpc1("npc.soul_wars_imp") { startDialogue(it.npc) { zimberfizz() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Nomad (initial dialogue)
    // TODO(content): [system:soul_wars] the demonstration (tele-other after "Let's do it." with
    //  the poison, disease and restricted item checks), the pre- and post-demonstration
    //  dialogue, the Soul wars guide, rewards, ammunition and the Trade option
    private suspend fun Dialogue.nomad() {
        chatPlayer(quiz, "Excuse me, what is this place?")
        chatNpc(
            happy,
            "What is this place? This is Soul Wars, a glorious battleground where you must fight " +
                "to crush the souls of your enemies!",
        )
        chatPlayer(quiz, "Really? Are souls easy to crush?")
        chatNpc(laugh, "Easier than you'd imagine.")
        chatPlayer(quiz, "I see. So how does it all work?")
        chatNpc(neutral, "I can give you a demonstration if you're interested?")
        when (choice2("Let's do it.", 1, "Actually, I'm good.", 2)) {
            1 -> chatPlayer(happy, "Let's do it.")
            2 -> {
                chatPlayer(neutral, "Actually, I'm good.")
                chatNpc(neutral, "So be it. You know where to find me if you change your mind.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Zimberfizz (before the demonstration)
    // TODO(content): [system:soul_wars] after the demonstration: the Soul Obelisk, Nomad and
    //  hunter questions
    private suspend fun Dialogue.zimberfizz() {
        chatNpc(happy, "Wotcha guv'na!")
        chatNpcSpecific(
            NOMAD_TITLE,
            NOMAD,
            angry,
            "Hey! Talk to me if you want to know anything worth knowing.",
        )
        chatNpc(worried, "Best talk to his highness over there, 'fore he goes gettin' upset.")
    }

    private companion object {
        const val NOMAD = "npc.soul_wars_nomad_2op"
        const val NOMAD_TITLE = "Nomad"
    }
}
