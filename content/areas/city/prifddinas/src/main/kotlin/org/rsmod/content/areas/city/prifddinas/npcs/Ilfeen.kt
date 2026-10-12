package org.rsmod.content.areas.city.prifddinas.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Ilfeen (after Song of the Elves)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [verify:in_game] chanting a crystal seed and replacing defunct crystal
// equipment (the wiki transcript is incomplete), and the Enchant option
class Ilfeen @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.ilfeen_prif") { startDialogue(it.npc) { ilfeen() } }
    }

    private suspend fun Dialogue.ilfeen() {
        chatPlayer(happy, "Hello again.")
        chatNpc(happy, "Hey.")
        chatPlayer(quiz, "Are you still offering to chant seeds?")
        chatNpc(neutral, "I am, but you'll need to supply the seed.")
        if (QuestRequirements.hasCompleted(player, EYES_OF_GLOUPHRIE)) {
            oaknock()
        }
        if (access.playerContainsObj(BOOK)) {
            return
        }
        chatNpc(happy, "If it helps I have a book here that will tell you about crystal chanting.")
        when (choice2("That sounds great.", 1, "I don't need it, thanks.", 2)) {
            1 -> {
                chatPlayer(happy, "That sounds great.")
                player.invAddOrDrop(objRepo, BOOK)
                objbox(BOOK, "You're handed a book.")
                chatPlayer(happy, "Thanks, I'll read all about it.")
            }
            2 -> chatPlayer(neutral, "I don't need it, thanks.")
        }
        chatNpc(happy, "Well, good luck.")
        chatPlayer(happy, "See you later.")
    }

    // TODO(content): [verify:in_game] whether the book offer still follows "Ilfeen smiles
    // serenely."; the transcript's conditions don't say
    private suspend fun Dialogue.oaknock() {
        chatPlayer(neutral, "While I'm here, I have a question for you.")
        chatNpc(happy, "Well ask away.")
        chatPlayer(quiz, "Do you remember Oaknock? He was a gnome inventor.")
        chatNpc(
            happy,
            "Of course! How is the young gnome? I rather liked him he was rather loud, but he " +
                "had a good soul, and was very interested in elven magic.",
        )
        chatPlayer(
            neutral,
            "I think Oaknock died of old age some time ago. He must have visited you a few " +
                "hundred years ago though...",
        )
        chatNpc(
            neutral,
            "I keep forgetting, a hundred years is a long time for you. He was very interested " +
                "in illusion magic, and how to prevent it. I gave him some advice and a few bits " +
                "of equipment to help him.",
        )
        chatPlayer(quiz, "A seed?")
        chatNpc(happy, "Of course. It grew into an axe. I hope he found it useful.")
        chatPlayer(happy, "I think he did! Thanks.")
        mesbox("Ilfeen smiles serenely.")
    }

    private companion object {
        const val BOOK = "obj.roving_crystal_book"
        const val EYES_OF_GLOUPHRIE = "quest_eyesofglouphrie"
    }
}
