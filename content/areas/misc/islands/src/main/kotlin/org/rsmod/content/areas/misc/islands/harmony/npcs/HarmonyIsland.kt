package org.rsmod.content.areas.misc.islands.harmony.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class HarmonyIsland @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.brain_tranquility") { startDialogue(it.npc) { tranquilityOnMos() } }
        onOpNpc1("npc.brain_island_tranquility") {
            startDialogue(it.npc) { tranquilityOnHarmony() }
        }
        onOpNpc1("npc.brain_island_fenkenstrain") { startDialogue(it.npc) { fenkenstrain() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Brother_Tranquility
    // TODO(content): [system:harmony_transport] "Would you like me to take you to Harmony?" and
    // the Transport option after the quest
    private suspend fun Dialogue.tranquilityOnMos() {
        if (QuestRequirements.hasCompleted(player, GREAT_BRAIN_ROBBERY)) {
            tranquilityAfterQuest()
            return
        }
        // TODO(content): [quest:the_great_brain_robbery] quest start for a player with the
        // requirements
        chatNpc(neutral, "Excuse me...")
        chatPlayer(quiz, "Yes? Can I help you?")
        chatNpc(
            neutral,
            "Sorry to have bothered you; I'm looking for a more experienced adventurer.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Brother_Tranquility (after The Great Brain
    // Robbery)
    // TODO(content): [system:harmony_transport] "Do you want me to take you back to Mos
    // Le'Harmless?" and the Transport option
    private suspend fun Dialogue.tranquilityOnHarmony() {
        tranquilityAfterQuest()
    }

    private suspend fun Dialogue.tranquilityAfterQuest() {
        chatPlayer(quiz, "How's your head feeling?")
        chatNpc(happy, "A lot better, thanks. Can I help you with anything?")
        if (!access.playerContainsAnyObj(BROKEN_ANCHOR, ANCHOR)) {
            chatNpc(
                neutral,
                "This washed up on the beach earlier. I thought you could get it fixed by " +
                    "Smithing Smith in Mos Le'Harmless. It could be valuable.",
            )
            player.invAddOrDrop(objRepo, BROKEN_ANCHOR)
            chatPlayer(happy, "Thanks.")
            return
        }
        chatPlayer(
            neutral,
            "Not really. I was just popping by to see how you were all getting on and possibly " +
                "help cull some of the zombies.",
        )
        chatNpc(happy, "Your concern is most gratifying. Saradomin be with you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dr_Fenkenstrain (after The Great Brain Robbery)
    // TODO(content): [skill:thieving] pickpocketing the ring of Charos
    private suspend fun Dialogue.fenkenstrain() {
        chatPlayer(quiz, "How are things going, doctor?")
        chatNpc(
            sad,
            "After talking with these monks and seeing your efforts to save them...I'm starting " +
                "to regret my past actions.",
        )
        chatPlayer(neutral, "Well, we all make mistakes.")
        chatPlayer(
            neutral,
            "Admittedly, most of us don't accidentally trip and sew a bunch of dead bodies " +
                "together.",
        )
        chatNpc(
            sad,
            "I know...I think that my time here will be well spent trying to atone for what I did.",
        )
        chatNpc(
            happy,
            "Thank you for helping me see the error of my ways, ${player.displayName}.",
        )
    }

    private companion object {
        const val BROKEN_ANCHOR = "obj.brain_broken_anchor"
        const val ANCHOR = "obj.brain_anchor"
        const val GREAT_BRAIN_ROBBERY = "quest_greatbrainrobbery"
    }
}
