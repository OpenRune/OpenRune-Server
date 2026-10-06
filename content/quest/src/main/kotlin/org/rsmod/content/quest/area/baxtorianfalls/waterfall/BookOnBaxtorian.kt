package org.rsmod.content.quest.area.baxtorianfalls.waterfall

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.Book
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.MetHudon
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.ReadBook
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The bookcase upstairs in the tourist centre and the book it hides. The book only turns up once
 * the player has met Hudon, and reading it is what sends them after Glarial's pebble.
 */
class BookOnBaxtorian
@Inject
constructor(private val waterfall: WaterfallQuest, private val objRepo: ObjRepository) :
    PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1(Bookcase) { searchBookcase() }
        onOpHeld1(Book) { readBook() }
    }

    private suspend fun ProtectedAccess.searchBookcase() {
        if (waterfall.stage(player) < MetHudon) {
            searchUninterestingBooks()
            return
        }
        anim(SearchSeq)
        if (player.inv.contains(Book)) {
            mes("You search the bookcase but find nothing of interest")
            return
        }
        invAddOrDrop(objRepo, Book)
        objbox(Book, "You find a book named 'Book on Baxtorian' on the bookcase.")
    }

    /**
     * The book's page arrows are pause buttons. The server queues the book to close when one is
     * pressed, and the client ignores further presses until the interface is sent again, so every
     * turn re-opens the book at the new spread.
     */
    private suspend fun ProtectedAccess.readBook() {
        var spread = 0
        openSpread(spread)
        if (waterfall.stage(player) == MetHudon) {
            waterfall.advanceTo(this, ReadBook)
        }
        while (true) {
            val input = pauseButton()
            val turned =
                when (input.component) {
                    PageLeft -> spread - 1
                    PageRight -> spread + 1
                    else -> spread
                }
            if (turned in Spreads.indices) {
                spread = turned
            }
            openSpread(spread)
        }
    }

    private fun ProtectedAccess.openSpread(spread: Int) {
        ifOpenMainModal(BookInterface)
        player.runClientScript(
            BookInitScript,
            RSCM.getRSCM("component.book:close_button"),
            RSCM.getRSCM("component.book:close_graphic"),
            RSCM.getRSCM(PageLeft),
            RSCM.getRSCM("component.book:page_left_graphic"),
            RSCM.getRSCM(PageRight),
            RSCM.getRSCM("component.book:page_right_graphic"),
        )
        ifSetText("component.book:title", Title)
        ifSetEvents(PageLeft, -1..-1, IfEvent.PauseButton)
        ifSetEvents(PageRight, -1..-1, IfEvent.PauseButton)
        showSpread(spread)
        soundSynth(PageSound)
    }

    private fun ProtectedAccess.showSpread(spread: Int) {
        val (left, right) = Spreads[spread]
        for (line in 1..LinesPerPage) {
            ifSetText("component.book:page_left_text_$line", left.getOrElse(line - 1) { "" })
            ifSetText("component.book:page_right_text_$line", right.getOrElse(line - 1) { "" })
        }
        ifSetText("component.book:page_left_number", (spread * 2 + 1).toString())
        ifSetText("component.book:page_right_number", (spread * 2 + 2).toString())
        ifSetHide(PageLeft, spread == 0)
        ifSetHide(PageRight, spread == Spreads.lastIndex)
    }

    private companion object {
        const val Bookcase = "loc.bookcase_waterfall_quest"
        const val BookInterface = "interface.book"
        val BookInitScript = "clientscript.[clientscript,book_init]".asRSCM(RSCMType.CLIENTSCRIPT)
        const val Title = "Book on Baxtorian"
        const val SearchSeq = "seq.human_pickuptable"
        const val PageSound = "synth.paper_move"
        const val LinesPerPage = 15

        const val PageLeft = "component.book:page_left_button"
        const val PageRight = "component.book:page_right_button"

        val Pages =
            listOf(
                listOf(
                    "<u>The Missing Relics</u>",
                    "",
                    "Many artefacts of elven",
                    "history were lost after the",
                    "Fourth Age, following the",
                    "departure of the elves from",
                    "these lands. The greatest loss",
                    "to our collections of elf",
                    "history were the hidden",
                    "treasures of Baxtorian.",
                    "",
                    "Some believe these treasures",
                    "are still unclaimed, but it is",
                    "more commonly believed that",
                    "dwarf miners recovered them",
                ),
                listOf(
                    "early in the Fifth Age.",
                    "",
                    "Another great loss was",
                    "Glarial's pebble, a key which",
                    "allowed her family to visit her",
                    "tomb. The pebble was taken",
                    "by a gnome family over a",
                    "century ago. It is hoped that",
                    "descendants of that gnome",
                    "may still have the pebble",
                    "hidden in their cave under",
                    "the Tree Gnome Village.",
                ),
                listOf(
                    "<u>The Sonnet of Baxtorian</u>",
                    "",
                    "The love between Baxtorian",
                    "and Glarial was said to have",
                    "lasted over a century. They",
                    "lived a peaceful life learning",
                    "and teaching the laws of",
                    "nature.",
                    "",
                    "When trouble hit their home",
                    "in the west, Baxtorian left on",
                    "a great campaign. He",
                    "returned to find his people",
                    "slaughtered and his wife",
                    "taken by the enemy.",
                ),
                listOf(
                    "After years of searching for",
                    "his love, he finally gave up",
                    "and returned to the home he",
                    "had made for Glarial under",
                    "the Baxtorian Falls. Once he",
                    "entered, he never returned.",
                    "",
                    "Only he and Glarial had the",
                    "power to enter the waterfall.",
                    "Since Baxtorian entered, no",
                    "one else has been able to get",
                    "in. It's as if the powers of",
                    "nature still work to protect",
                    "him.",
                ),
                listOf(
                    "<u>The Power of Nature</u>",
                    "",
                    "Glarial and Baxtorian were",
                    "masters of nature. Trees",
                    "would grow, hills form and",
                    "rivers flood on their",
                    "command.",
                    "",
                    "Baxtorian in particular had",
                    "perfected rune lore. It was",
                    "said that he could use the",
                    "stones to control water, earth",
                    "and air.",
                ),
                listOf(
                    "<u>Ode to Eternity</u>",
                    "",
                    "A short piece written by",
                    "Baxtorian himself.",
                    "",
                    "What care I for this mortal",
                    "coil,",
                    "where treasures are yet so",
                    "frail,",
                    "for it is you that is my life",
                    "blood,",
                    "the wine to my holy grail,",
                    "and if I see the judgement",
                    "day,",
                    "when gods fill the air with",
                ),
                listOf(
                    "dust,",
                    "I'll happily choke on your",
                    "memory,",
                    "as my kingdom turns to",
                    "rust.",
                ),
                emptyList(),
            )

        val Spreads = Pages.chunked(2).map { it[0] to it[1] }
    }
}
