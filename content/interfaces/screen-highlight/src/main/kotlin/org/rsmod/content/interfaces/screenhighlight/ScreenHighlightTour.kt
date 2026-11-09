package org.rsmod.content.interfaces.screenhighlight

import org.rsmod.api.config.constants
import org.rsmod.game.entity.Player

/**
 * A run of `screenhighlight` hint pages shown inside the packed component [host]. The tour that
 * owns a click is found by the component `screenhighlight` is open in, so tours must not share a
 * host.
 *
 * [hint] is written to `varbit.hnt_selected_hint` on start; the client never reads it. [onFinish]
 * runs with the page index on `continue` from the last page or from a [HighlightPage.pause] page;
 * without it that click shows the page again. [onPage] runs before each page is sent. [onClose]
 * runs when `screenhighlight` closes.
 */
public class ScreenHighlightTour(
    public val host: Int,
    public val pages: List<HighlightPage>,
    public val hint: Int? = null,
    public val onFinish: ((Player, Int) -> Unit)? = null,
    public val onPage: (Player, Int) -> Unit = { _, _ -> },
    public val onClose: (Player) -> Unit = {},
)

/**
 * [showClose] shows `screenhighlight:close`; [defaultTextbox] sends `highlight_textbox_default`.
 * [lastPage] overrides the textbox's last-page flag, which otherwise marks the final page.
 * [pause] ends the tour's run at this page; [previousButton] shows `screenhighlight:previous`.
 */
public data class HighlightPage(
    val frame: HighlightFrame,
    val text: String,
    val textColour: Int = -1,
    val showClose: Boolean = false,
    val defaultTextbox: Boolean = false,
    val lastPage: Boolean? = null,
    val pause: Boolean = false,
    val previousButton: Boolean = false,
)

public sealed interface HighlightFrame {
    /** `highlight_screen` (3409): a box at fixed offsets inside the host. */
    public data class Screen(
        val offX: Int = 0,
        val offY: Int = 0,
        val offH: Int = 0,
        val offW: Int = 0,
        val setPosH: Int = constants.setpos_abs_right,
        val setPosV: Int = constants.setpos_abs_bottom,
        val transparency: Int = 100,
        val hideCom7: Boolean = false,
    ) : HighlightFrame

    /** `highlight_screen_component` (2463): a box around [focus], laid out inside [outer]. */
    public data class Component(
        val outer: Int,
        val focus: Int,
        val padding: Int = 0,
        val setPosH: Int = constants.setpos_abs_left,
        val setPosV: Int = constants.setpos_abs_top,
        val transparency: Int = 100,
        val flag: Boolean = false,
    ) : HighlightFrame
}
