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
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.BOOK
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.MET_HUDON
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.READ_BOOK
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BookOnBaxtorian
@Inject
constructor(private val waterfall: WaterfallQuest, private val objRepo: ObjRepository) :
    PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1(BOOKCASE) { searchBookcase() }
        onOpHeld1(BOOK) { readBook() }
    }

    private suspend fun ProtectedAccess.searchBookcase() {
        if (waterfall.stage(player) < MET_HUDON) {
            searchUninterestingBooks()
            return
        }
        anim(SEARCH_SEQ)
        if (player.inv.contains(BOOK)) {
            mes("You search the bookcase but find nothing of interest")
            return
        }
        invAddOrDrop(objRepo, BOOK)
        objbox(BOOK, "You find a book named 'Book on Baxtorian' on the bookcase.")
    }

    /**
     * The book's page arrows are pause buttons. The server queues the book to close when one is
     * pressed, and the client ignores further presses until the interface is sent again, so every
     * turn re-opens the book at the new spread.
     */
    private suspend fun ProtectedAccess.readBook() {
        var spread = 0
        openSpread(spread)
        if (waterfall.stage(player) == MET_HUDON) {
            waterfall.advanceTo(this, READ_BOOK)
        }
        while (true) {
            val input = pauseButton()
            val turned =
                when (input.component) {
                    PAGE_LEFT -> spread - 1
                    PAGE_RIGHT -> spread + 1
                    else -> spread
                }
            if (turned in SPREADS.indices) {
                spread = turned
            }
            openSpread(spread)
        }
    }

    private fun ProtectedAccess.openSpread(spread: Int) {
        ifOpenMainModal(BOOK_INTERFACE)
        player.runClientScript(
            BOOK_INIT_SCRIPT,
            RSCM.getRSCM("component.book:close_button"),
            RSCM.getRSCM("component.book:close_graphic"),
            RSCM.getRSCM(PAGE_LEFT),
            RSCM.getRSCM("component.book:page_left_graphic"),
            RSCM.getRSCM(PAGE_RIGHT),
            RSCM.getRSCM("component.book:page_right_graphic"),
        )
        ifSetText("component.book:title", TITLE)
        ifSetEvents(PAGE_LEFT, -1..-1, IfEvent.PauseButton)
        ifSetEvents(PAGE_RIGHT, -1..-1, IfEvent.PauseButton)
        showSpread(spread)
        soundSynth(PAGE_SOUND)
    }

    private fun ProtectedAccess.showSpread(spread: Int) {
        val (left, right) = SPREADS[spread]
        for (line in 1..LINES_PER_PAGE) {
            ifSetText("component.book:page_left_text_$line", left.getOrElse(line - 1) { "" })
            ifSetText("component.book:page_right_text_$line", right.getOrElse(line - 1) { "" })
        }
        ifSetText("component.book:page_left_number", (spread * 2 + 1).toString())
        ifSetText("component.book:page_right_number", (spread * 2 + 2).toString())
        ifSetHide(PAGE_LEFT, spread == 0)
        ifSetHide(PAGE_RIGHT, spread == SPREADS.lastIndex)
    }

    private companion object {
        const val BOOKCASE = "loc.bookcase_waterfall_quest"
        const val BOOK_INTERFACE = "interface.book"
        val BOOK_INIT_SCRIPT = "clientscript.[clientscript,book_init]".asRSCM(RSCMType.CLIENTSCRIPT)
        const val TITLE = "Book on Baxtorian"
        const val SEARCH_SEQ = "seq.human_pickuptable"
        const val PAGE_SOUND = "synth.paper_move"
        const val LINES_PER_PAGE = 15

        const val PAGE_LEFT = "component.book:page_left_button"
        const val PAGE_RIGHT = "component.book:page_right_button"

        val PAGES =
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

        val SPREADS = PAGES.chunked(2).map { it[0] to it[1] }
    }
}
