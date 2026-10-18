package org.rsmod.content.quest.area.lumbridge.idesofmilk

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onOpHeld1
import org.rsmod.content.quest.area.lumbridge.idesofmilk.IdesOfMilkQuest.Companion.BOOK
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GroatsPrinciplesBook : PluginScript() {
    private var Player.pagePos by intVarp("varp.pagepos")

    override fun ScriptContext.startup() {
        onOpHeld1(BOOK) { readBook() }
    }

    // Page arrows close the book client-side, so reopen it on the new spread.
    private suspend fun ProtectedAccess.readBook() {
        player.pagePos = 0
        anim("seq.human_readbook")
        showSpread()
        while (true) {
            val input = pauseButton()
            when {
                input.isComponentType("component.book:page_right_button") -> turnPage(SPREAD_PAGES)
                input.isComponentType("component.book:page_left_button") -> turnPage(-SPREAD_PAGES)
                else -> return
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

    private companion object {
        private const val TITLE = "The Groats Principles"
        private const val LINES_PER_PAGE = 15
        private const val SPREAD_PAGES = 2
        private const val SPOTANIM_HEIGHT = 92

        private val PAGES =
            listOf(
                listOf(
                    "The Groats Principles.",
                    "",
                    "Principle One.",
                    "",
                    "Happy animals make happy",
                    "farmers. It is crucial to",
                    "ensure your animals have",
                    "everything they could need:",
                    "space, food, water. Without",
                    "those essentials met, you'll",
                    "spend your time running",
                    "around trying to make them",
                    "happy and healthy. Not to",
                    "mention, unhappy animals",
                    "produce worse quality",
                ),
                listOf(
                    "produce!",
                    "",
                    "The Groats name lives and",
                    "dies on its reputation, and",
                    "that reputation relies on",
                    "producing the highest quality",
                    "milk and eggs.",
                    "",
                    "Principle Two.",
                    "",
                    "Never take more than you",
                    "need. Greed will rot not only",
                    "your soul, but your land.",
                    "Your animals will remain",
                    "healthier when not pushed to",
                ),
                listOf(
                    "their limits.",
                    "",
                    "Principle Three.",
                    "",
                    "Always squirrel money away",
                    "from the tax collector. It's",
                    "not a good citizen that avoids",
                    "all of their taxes, but a smart",
                    "one keeps a little back. The",
                    "more you can invest into",
                    "your farm and your animals,",
                    "the more you can give back",
                    "later on in your life.",
                ),
                emptyList(),
            )
    }
}
