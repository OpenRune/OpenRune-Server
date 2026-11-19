package org.rsmod.content.quest.area.varrock.shieldofarrav

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player

internal object ShieldOfArravBook {
    private var Player.pagePos by intVarp("varp.pagepos")

    // Page arrows close the book client-side, so reopen it on the new spread.
    suspend fun read(access: ProtectedAccess) {
        with(access) {
            player.pagePos = 0
            anim("seq.human_readbook")
            showSpread()
            while (true) {
                val input = pauseButton()
                when {
                    input.isComponentType("component.book:page_right_button") ->
                        turnPage(SPREAD_PAGES)
                    input.isComponentType("component.book:page_left_button") ->
                        turnPage(-SPREAD_PAGES)
                    else -> return
                }
            }
        }
    }

    private fun ProtectedAccess.turnPage(delta: Int) {
        val next = player.pagePos + delta
        if (next !in PAGES.indices step SPREAD_PAGES) {
            return
        }
        player.pagePos = next
        if (delta > 0) {
            spotanim("spotanim.readbook_spotanim_turnright", height = SPOTANIM_HEIGHT)
            anim("seq.human_readbook_turnright")
        } else {
            spotanim("spotanim.readbook_spotanim_turnleft", height = SPOTANIM_HEIGHT)
            anim("seq.human_readbook_turnleft")
        }
        showSpread()
    }

    private fun ProtectedAccess.showSpread() {
        val left = player.pagePos
        ifOpenMainModal("interface.book")
        ifSetText("component.book:title", TITLE)
        ifSetText("component.book:page_left_number", "${left + 1}")
        ifSetText("component.book:page_right_number", "${left + 2}")
        writePage("left", PAGES.getOrElse(left) { emptyList() })
        writePage("right", PAGES.getOrElse(left + 1) { emptyList() })
        for (line in 1..LINES_PER_PAGE) {
            ifSetHide("component.book:page_left_model_$line", true)
            ifSetHide("component.book:page_right_model_$line", true)
        }
        ifSetHide("component.book:page_right_button", left + SPREAD_PAGES >= PAGES.size)
        ifSetHide("component.book:page_left_button", left == 0)
    }

    private fun ProtectedAccess.writePage(side: String, lines: List<String>) {
        for (line in 1..LINES_PER_PAGE) {
            ifSetText("component.book:page_${side}_text_$line", lines.getOrElse(line - 1) { "" })
        }
    }

    private const val TITLE = "The Shield of Arrav"
    private const val LINES_PER_PAGE = 15
    private const val SPREAD_PAGES = 2
    private const val SPOTANIM_HEIGHT = 92

    private val PAGES =
        listOf(
            listOf(
                "The Shield of Arrav",
                "",
                "by A. R. Wright",
                "",
                "Arrav is probably the best",
                "known hero of the 4th Age.",
                "Many legends are told of his",
                "heroics, most notably his",
                "role in the defence of",
                "Avarrocka against the zombie",
                "hordes of the Mahjarrat",
                "Zemouregal.",
            ),
            listOf(
                "One surviving artefact from",
                "the 4th Age is a fabulous",
                "shield. This shield once",
                "belonged to Arrav and is now",
                "indeed known as the Shield",
                "of Arrav. For over 150",
                "years, it was the prize",
                "piece in the Varrock Museum.",
            ),
            listOf(
                "However, in the year 143 of",
                "the 5th Age, a group of",
                "thieves known as the Phoenix",
                "Gang broke into the museum",
                "and stole the shield in a",
                "daring raid. As a result,",
                "Misthalin's ruler at the",
                "time, King Roald II, put a",
                "1,200 gold bounty (a massive",
                "sum of money in those days)",
                "on the return of the shield,",
                "hoping that one of the",
                "culprits would betray their",
                "fellows out of greed. Alas,",
                "this tactic did not work.",
            ),
            listOf(
                "Many years after the",
                "original theft of the",
                "shield, a rift resulted in a",
                "splinter group of the",
                "Phoenix Gang forming their",
                "own criminal organisation,",
                "the Black Arm Gang.",
            ),
            listOf(
                "Reports on which of the two",
                "gangs ended up in possession",
                "of the Shield of Arrav were",
                "inconsistent, so the",
                "location of the shield is",
                "now sadly unknown. However,",
                "King Roald III has recently",
                "declared that the reward for",
                "its return still stands.",
            ),
            emptyList(),
        )
}
