package org.rsmod.content.areas.misc.stronghold_of_security

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private var Player.metSolztun by boolVarBit("varbit.sos_brother_found")

class StrongholdNotes @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.sos_skelly_bag") { searchExplorer() }
        onOpHeld1(Notes) { readNotes() }
        onOpNpc1("npc.sos_barb_spirit") { startDialogue(it.npc) { solztun() } }
    }

    private suspend fun ProtectedAccess.searchExplorer() {
        arriveDelay()
        if (inv.count(Notes) > 0 || bank.count(Notes) > 0) {
            mes("You search the dead explorer, but there is nothing else of use on him.")
            return
        }
        if (inv.isFull()) {
            mes("You don't have enough inventory space to take the explorer's notes.")
            return
        }
        invAddOrDrop(objRepo, Notes)
        startDialogue { objbox(Notes, "You find a book of notes on the dead explorer's body.") }
    }

    private suspend fun ProtectedAccess.readNotes() {
        var spread = 0
        openSpread(spread)
        while (true) {
            spread = StrongholdNotesBook.turn(spread, pauseButton().component)
            openSpread(spread)
        }
    }

    private fun ProtectedAccess.openSpread(spread: Int) {
        ifOpenMainModal(BookInterface)
        player.runClientScript(
            BookInitScript,
            RSCM.getRSCM("component.indexed_book:close_button"),
            RSCM.getRSCM("component.indexed_book:close_graphic"),
            RSCM.getRSCM(StrongholdNotesBook.PageLeft),
            RSCM.getRSCM("component.indexed_book:page_left_graphic"),
            RSCM.getRSCM(StrongholdNotesBook.PageRight),
            RSCM.getRSCM("component.indexed_book:page_right_graphic"),
            RSCM.getRSCM(StrongholdNotesBook.FirstPage),
        )
        ifSetText("component.indexed_book:title", StrongholdNotesBook.Title)
        val (left, right) = StrongholdNotesBook.spread(spread)
        for (line in 1..StrongholdNotesBook.LinesPerPage) {
            val index = line - 1
            ifSetText("component.indexed_book:page_left_text_$line", left.getOrElse(index) { "" })
            ifSetText("component.indexed_book:page_right_text_$line", right.getOrElse(index) { "" })
        }
        ifSetText("component.indexed_book:page_left_number", (spread * 2 + 1).toString())
        ifSetText("component.indexed_book:page_right_number", (spread * 2 + 2).toString())
        for (button in listOf(StrongholdNotesBook.PageLeft, StrongholdNotesBook.PageRight)) {
            ifSetEvents(button, -1..-1, IfEvent.PauseButton)
        }
        ifSetEvents(StrongholdNotesBook.FirstPage, -1..-1, IfEvent.PauseButton)
        ifSetHide(StrongholdNotesBook.PageLeft, spread == 0)
        ifSetHide(StrongholdNotesBook.PageRight, spread == StrongholdNotesBook.lastSpread)
        ifSetHide(StrongholdNotesBook.FirstPage, spread == 0)
        if (spread == 0) {
            showChapterLinks()
        }
    }

    private fun ProtectedAccess.showChapterLinks() {
        for ((i, chapter) in StrongholdNotesBook.Chapters.withIndex()) {
            val link = StrongholdNotesBook.chapterLink(StrongholdNotesBook.FirstChapterLine + i)
            ifSetText(link, chapter.first)
            ifSetEvents(link, -1..-1, IfEvent.PauseButton)
            ifSetHide(link, false)
        }
    }

    private suspend fun Dialogue.solztun() {
        chatNpc(neutral, "Can you hear me?")
        if (player.worn.count(Ghostspeak) > 0) {
            chatPlayer(happy, "Of course! I'm wearing an amulet of ghostspeak.")
        } else {
            chatPlayer(
                worried,
                "...I can, but I'm not wearing an amulet of ghostspeak... A-am I dead?",
            )
        }
        chatNpc(
            angry,
            "I'm no ghost! I'm a spirit, I'm injecting my thoughts into your head.",
        )
        chatPlayer(confused, "Oh, that's... interesting. Also rather disturbing...")
        chatNpc(neutral, "Try not to think about it.")
        while (true) {
            when (
                choice3(
                    "Who are you?",
                    SolztunOption.Who,
                    "Why are you here?",
                    SolztunOption.Why,
                    "Bye!",
                    SolztunOption.Bye,
                )
            ) {
                SolztunOption.Who -> whoAreYou()
                SolztunOption.Why -> whyAreYouHere()
                SolztunOption.Bye -> {
                    chatPlayer(happy, "Bye!")
                    chatNpc(happy, "Stay safe, adventurer.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.whoAreYou() {
        chatPlayer(quiz, "Who are you?")
        if (!player.metSolztun) {
            player.metSolztun = true
            chatNpc(happy, "I am Solztun, the greatest barbarian explorer!")
            chatPlayer(happy, "It's very nice to meet you, Solztun! I'm ${player.displayName}.")
            chatNpc(neutral, "I know.")
            chatPlayer(quiz, "... so, you were exploring this place?")
            chatNpc(
                sad,
                "Yes, my sister told me not to go, she said it was dangerous and that I'm no " +
                    "fighter. Unfortunately... she was correct.",
            )
            chatPlayer(
                quiz,
                "Oh! Your sister is Litara, the woman out on the surface? How come she was " +
                    "correct?",
            )
            chatNpc(
                sad,
                "I died... Of course, I tried escaping, long before I'd made it to the " +
                    "treasure, but I was shot with arrows just as I approached the exit.",
            )
        } else {
            chatNpc(
                happy,
                "As I said, I am Solztun, the greatest barbarian explorer! I came to explore " +
                    "this place, but unfortunately I died long before I made it to the treasure.",
            )
        }
        chatPlayer(shocked, "Nobody came to help?!")
        chatNpc(
            sad,
            "Oh no, they had no idea I was hurt, I guess they assumed I'd found the treasure " +
                "and made a new life for myself.",
        )
        chatPlayer(quiz, "Were you not a fan of the barbarian life style?")
        chatNpc(
            neutral,
            "It was all death and pillaging, I prefer the finer things in life, like " +
                "exploration and discovery!",
        )
        chatPlayer(happy, "Ah, I too enjoy those.")
    }

    private suspend fun Dialogue.whyAreYouHere() {
        chatPlayer(quiz, "Why are you here?")
        chatNpc(happy, "I came for treasure!")
        chatPlayer(quiz, "Oh right, you were looking for what's in that cradle?")
        chatNpc(sad, "Yes, but alas, I'm dead.")
        chatPlayer(shifty, "I guess I could show you... for a price.")
        chatNpc(
            neutral,
            "Hmm... I don't have much to offer, but if you happen upon a skull sceptre, I " +
                "could make it stronger.",
        )
        chatPlayer(quiz, "Stronger you say... In what way?")
        chatNpc(
            neutral,
            "Currently the sceptre is fragile, it breaks once all charges have been used. " +
                "Show me the treasure and I could imbue it so it can be recharged with any " +
                "sceptre piece.",
        )
        chatPlayer(happy, "You, my dead friend, have a deal!")
    }

    private enum class SolztunOption {
        Who,
        Why,
        Bye,
    }

    private companion object {
        const val Notes = "obj.sos_stronghold_book"
        const val Ghostspeak = "obj.amulet_of_ghostspeak"
        const val BookInterface = "interface.indexed_book"
        val BookInitScript =
            "clientscript.[clientscript,book_indexed_init]".asRSCM(RSCMType.CLIENTSCRIPT)
    }
}
