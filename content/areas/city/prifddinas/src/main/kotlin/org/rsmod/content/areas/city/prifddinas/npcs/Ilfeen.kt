package org.rsmod.content.areas.city.prifddinas.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Ilfeen (after Song of the Elves)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [verify:in_game] chanting a crystal seed and replacing defunct crystal
// equipment (the wiki transcript is incomplete), and the Enchant option
// TODO(content): [quest:the_eyes_of_glouphrie] "Do you remember Oaknock?" after the quest
class Ilfeen @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.ilfeen_prif") { startDialogue(it.npc) { ilfeen() } }
    }

    private suspend fun Dialogue.ilfeen() {
        chatPlayer(happy, "Hello again.")
        chatNpc(happy, "Hey.")
        chatPlayer(quiz, "Are you still offering to chant seeds?")
        chatNpc(neutral, "I am, but you'll need to supply the seed.")
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

    private companion object {
        const val BOOK = "obj.roving_crystal_book"
    }
}
