package org.rsmod.content.areas.misc.morytania.haunted

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class FenkenstrainsCastle : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.fenk_fenkenstrain_model") { startDialogue(it.npc) { fenkenstrain() } }
        onOpNpc1("npc.fenk_creature_model_2") { startDialogue(it.npc) { rologarth() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dr_Fenkenstrain (after Creature of
    // Fenkenstrain; the after-Great Brain Robbery dialogue is on Harmony Island)
    private suspend fun Dialogue.fenkenstrain() {
        if (QuestRequirements.hasCompleted(player, CREATURE_OF_FENKENSTRAIN)) {
            chatNpc(worried, "theyrecomingtogetme theyrecomingtogetme...")
            chatPlayer(
                angry,
                "It is all you deserve. Lord Rologarth is master of this castle once more. Let " +
                    "him protect you - if he wants to.",
            )
            chatNpc(worried, "theyrecomingtogetme theyrecomingtogetme...")
            return
        }
        jobInterview()
    }

    // https://oldschool.runescape.wiki/w/Transcript:Creature_of_Fenkenstrain
    // TODO(content): [quest:creature_of_fenkenstrain] the job interview that starts the quest
    //  and the in-quest dialogue
    private suspend fun Dialogue.jobInterview() {
        chatNpc(quiz, "Have you come to apply for the job?")
        if (choice2("Yes.", true, "No.", false)) {
            chatPlayer(happy, "Yes, if it pays well.")
            return
        }
        chatPlayer(angry, "No thanks, I wouldn't work here if you paid me.")
        chatNpc(neutral, "Charming.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lord_Rologarth
    private suspend fun Dialogue.rologarth() {
        chatNpc(neutral, "How goes it, friend?")
        chatPlayer(happy, "I stole the Ring of Charos from Fenkenstrain.")
        chatNpc(
            neutral,
            "I saw him climb up into the Tower to hide it. It doesn't matter - soon the " +
                "werewolves will come for him, and his experiments will be forever ceased.",
        )
        chatPlayer(quiz, "Do you want the ring back? It is yours after all.")
        chatNpc(
            neutral,
            "No, you keep it, my friend. Werewolves hunger for the scent of live flesh - I have " +
                "no need for the ring. I have my castle back, if not my soul.",
        )
    }

    private companion object {
        const val CREATURE_OF_FENKENSTRAIN = "quest_creatureoffenkenstrain"
    }
}
