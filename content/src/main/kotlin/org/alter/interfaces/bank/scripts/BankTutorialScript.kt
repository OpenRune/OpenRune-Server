package org.alter.interfaces.bank.scripts

import dev.openrune.definition.type.widget.IfEvent
import org.alter.api.CommonClientScripts
import org.alter.api.ext.intVarBit
import org.alter.api.ext.runClientScript
import org.alter.constants
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onIfClose
import org.alter.game.pluginnew.event.impl.onIfModalButton
import org.alter.game.pluginnew.event.impl.onIfOverlayButton
import org.alter.interfaces.bank.configs.bank_components
import org.alter.interfaces.bank.configs.bank_interfaces
import org.alter.interfaces.bank.configs.bank_varbits
import org.alter.interfaces.bank.highlightNoClickClear
import org.alter.interfaces.bank.openBank
import org.alter.interfaces.bank.openBankWithoutEvents
import org.alter.interfaces.ifClose
import org.alter.interfaces.ifOpenOverlay
import org.alter.interfaces.ifSetEvents


class BankTutorialScript : PluginEvent() {
    private var Player.tutorialPage by intVarBit(bank_varbits.tutorial_current_page)
    private var Player.tutorialTotalPages by intVarBit(bank_varbits.tutorial_total_pages)

    override fun init() {
        onIfModalButton(bank_components.tutorial_button) { player.selectTutorial() }
        onIfOverlayButton(bank_components.tutorial_prev_page) { player.previousPage() }
        onIfOverlayButton(bank_components.tutorial_next_page) { player.nextPage() }
        onIfClose(bank_interfaces.tutorial_overlay) { player.onTutorialClose() }
    }

    suspend fun begin(player: Player) {

        player.openBankWithoutEvents()

        player.tutorialTotalPages = pages.size

        player.highlightNoClick()
        player.ifOpenOverlay(
            bank_interfaces.tutorial_overlay,
            bank_components.tutorial_overlay_target,
        )
        player.ifSetEvents(bank_components.tutorial_next_page, 9..9, IfEvent.Op1)
        player.ifSetEvents(bank_components.tutorial_prev_page, 9..9, IfEvent.Op1)
        player.highlightStart()
        player.showPage(0)
        player.ifSetEvents(bank_components.tutorial_close_button, -1..-1, IfEvent.PauseButton)

    }

    private suspend fun Player.selectTutorial() {
        ifClose()
        begin(this)
        ifClose()
        openBank()
    }

    private fun Player.showPage(pageIndex: Int) {
        val page = pages.getOrNull(pageIndex)
        requireNotNull(page)
        tutorialPage = pageIndex

        highlightScreen(
            offX = page.offX,
            offY = page.offY,
            offH = page.offH,
            offW = page.offW,
            setPosH = page.setPosH,
            setPosV = page.setPosV,
            transparency = page.transparency,
            hideCom7 = page.hideCom7,
        )
        highlightTextbox(page.text, isLastPage = pageIndex == pages.size - 1)
    }

    private fun Player.previousPage() {
        val pageIndex = tutorialPage - 1
        if (pageIndex in pages.indices) {
            showPage(pageIndex)
        } else {
            showPage(tutorialPage)
        }
    }

    private fun Player.nextPage() {
        val pageIndex = tutorialPage + 1
        if (pageIndex in pages.indices) {
            showPage(pageIndex)
        } else {
            showPage(tutorialPage)
        }
    }

    private fun Player.onTutorialClose() {
        highlightScreenHideLayer()
        highlightNoClickClear()
    }

    private fun Player.highlightNoClick() {
        runClientScript(CommonClientScripts.HIGHLIGHT_NO_CLICK_CLEAR, bank_components.bankside_highlight)
    }

    private fun Player.highlightStart() {
        runClientScript(CommonClientScripts.HIGHLIGHT_START, bank_components.tutorial_overlay_target)
    }

    private fun Player.highlightScreen(
        offX: Int,
        offY: Int,
        offH: Int,
        offW: Int,
        setPosH: Int,
        setPosV: Int,
        transparency: Int,
        hideCom7: Boolean,
    ) {
        runClientScript(
            CommonClientScripts.HIGHLIGHT_SCREEN,
            bank_components.tutorial_overlay_target,
            offX,
            offY,
            offH,
            offW,
            setPosH,
            setPosV,
            transparency,
            if (hideCom7) 1 else 0,
        )
    }

    private fun Player.highlightTextbox(text: String, isLastPage: Boolean) {
        val colour = -1
        val showCom29 = true
        runClientScript(
            CommonClientScripts.HIGHLIGHT_TEXTBOX,
            text,
            colour,
            if (isLastPage) 1 else 0,
            if (showCom29) 1 else 0,
            bank_components.tutorial_overlay_target,
        )
    }

    private fun Player.highlightScreenHideLayer() {
        runClientScript(CommonClientScripts.HIGHLIGHT_SCREEN_HIDE_LAYER, bank_components.tutorial_overlay_target)
    }
}

// Consider moving these to cache. Could use a struct, but do not want to deal with a bunch of
// parameters for now. Might be less annoying to do once we have support for db types.
private val pages =
    listOf(
        Page(
            text = """This is your bank. No one else can touch it but you.""".trimIndent(),
            hideCom7 = true,
        ),
        Page(
            text =
                """
                The bank is used to <col=ffffff>store</col> your items. You can click on
                 an item in your inventory to <col=ffffff>move it into your bank</col>.
            """
                    .trimIndent(),
            hideCom7 = true,
        ),
        Page(
            text =
                """
                If you want to move <col=ffffff>everything</col> from your
                 <col=ffffff>inventory</col> into the bank, you can use the
                 <col=ffffff>deposit all button</col>.
            """
                    .trimIndent(),
            offX = 44,
            offY = 44,
            offH = 39,
            offW = 2,
        ),
        Page(
            text =
                """
                This is the <col=ffffff>deposit worn button</col> that will move
                 <col=ffffff>all</col> your <col=ffffff>worn</col> items into the
                 bank for you.
            """
                    .trimIndent(),
            offX = 44,
            offY = 44,
            offH = 2,
            offW = 2,
        ),
        Page(
            text =
                """
                If you want to deposit or withdraw <col=ffffff>more than one item</col>,
                 you can <col=ffffff>right-click</col> on the item and select a
                 <col=ffffff>quantity</col> of the item to withdraw.
            """
                    .trimIndent(),
            hideCom7 = true,
        ),
        Page(
            text =
                """
                You can also <col=ffffff>change the amount</col> of items moved by
                 changing the <col=ffffff>quantity buttons</col>.
            """
                    .trimIndent(),
            offX = 130,
            offY = 40,
            offH = 203,
            offW = 2,
            setPosH = constants.setpos_abs_left,
        ),
        Page(
            text =
                """
                You can also withdraw items as <col=ffffff>bank notes</col>.
                 You withdraw an item as a bank note by changing the <col=ffffff>withdraw
                 option to note</col>. Items that <col=ffffff>stack and untradable items
                 can't be withdrawn as bank notes<col=ffffff>.
            """
                    .trimIndent(),
            offX = 105,
            offY = 40,
            offH = 102,
            offW = 2,
            setPosH = constants.setpos_abs_left,
        ),
        Page(
            text =
                """
                <col=ffffff>Bank notes</col> are useful when you need to <col=ffffff>transfer
                 a large amount of goods</col> around the game. You are able to sell them using
                 the Grand Exchange, to stores and to other players. <col=ffffff>To turn them
                 back into items, deposit them into the bank and withdraw them as an item</col>.
            """
                    .trimIndent(),
            offX = 105,
            offY = 40,
            offH = 102,
            offW = 2,
            setPosH = constants.setpos_abs_left,
        ),
        Page(
            text =
                """
                You can drag items around the bank to move them. There are <col=ffffff>two
                 types of item movement</col>, swap and insert. If you have <col=ffffff>swap</col>
                 selected, when you <col=ffffff>drag an item onto another</col> in the bank
                 then the <col=ffffff>items will swap position</col> with each other.
            """
                    .trimIndent(),
            offX = 105,
            offY = 40,
            offH = 102,
            offW = 2,
            setPosH = constants.setpos_abs_left,
        ),
        Page(
            text =
                """
                Here you can <col=ffffff>create or manage your bank tabs</col>. If you have
                 space, there will be a <col=ffffff>tab with a + icon</col> on it. You can
                 <col=ffffff>drag an item onto it to create a bank tab</col>. You can
                 <col=ffffff>right-click on a tab </col>and select <col=ffffff>'Collapse tab'</col>
                 to remove the tab and <col=ffffff>move the items</col> in the tab <col=ffffff>to
                 the main bank tab</col>. You can have a maximum of 9 custom tabs.
            """
                    .trimIndent(),
            offX = 409,
            offY = 46,
            offH = 40,
            offW = 32,
            setPosH = constants.setpos_abs_left,
            setPosV = constants.setpos_abs_top,
        ),
        Page(
            text =
                """
                If you want to <col=ffffff>stop items moving around your bank</col> when you
                 withdraw an item, you can <col=ffffff>create a placeholder</col> to keep an
                 unusable copy in your bank. Simply<col=ffffff> right-click on an item</col> in
                 the bank and <col=ffffff>click on the placeholder option</col>. Alternatively,
                 you can <col=ffffff>toggle</col> on the <col=ffffff>placeholder button</col> to
                 <col=ffffff>automatically create a placeholder</col> when withdrawing an item.
            """
                    .trimIndent(),
            offX = 409,
            offY = 46,
            offH = 40,
            offW = 32,
            setPosH = constants.setpos_abs_left,
            setPosV = constants.setpos_abs_top,
        ),
        Page(
            text =
                """
                You can <col=ffffff>equip items</col> while the <col=ffffff>bank is open</col>.
                 To do this,<col=ffffff> withdraw the armour and weapons</col> you want, then
                 <col=ffffff>right-click the item and click the wear option</col>. You can also
                 <col=ffffff>eat and drink</col> items in your inventory while the <col=ffffff>
                 bank is open</col> too, by <col=ffffff>selecting the eat or drink option</col>
                 instead.
            """
                    .trimIndent(),
            hideCom7 = true,
        ),
        Page(
            text =
                """
                Alternatively, you can access the <col=ffffff>equipment tab</col> here and equip
                 items in your inventory by <col=ffffff>left-clicking instead</col>.
            """
                    .trimIndent(),
            offX = 30,
            offY = 30,
            offH = 10,
            offW = 40,
            setPosH = constants.setpos_abs_left,
            setPosV = constants.setpos_abs_top,
        ),
    )

private data class Page(
    val text: String,
    val offX: Int = 0,
    val offY: Int = 0,
    val offH: Int = 0,
    val offW: Int = 0,
    val setPosH: Int = constants.setpos_abs_right,
    val setPosV: Int = constants.setpos_abs_bottom,
    val transparency: Int = 100,
    val hideCom7: Boolean = false,
)
