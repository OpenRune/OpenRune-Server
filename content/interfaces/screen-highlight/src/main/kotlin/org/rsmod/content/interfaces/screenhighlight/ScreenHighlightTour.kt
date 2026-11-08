package org.rsmod.content.interfaces.screenhighlight

import org.rsmod.api.config.constants
import org.rsmod.game.entity.Player

/**
 * A run of `screenhighlight` hint pages shown inside the packed component [host]. The tour that
 * owns a click is found by the component `screenhighlight` is open in, so tours must not share a
 * host.
 *
 * [hint] is written to `varbit.hnt_selected_hint` on start; the client never reads it. [onFinish]
 * runs on `continue` from the last page; without it that click shows the last page again.
 * [onClose] runs when `screenhighlight` closes.
 */
public class ScreenHighlightTour(
    public val host: Int,
    public val pages: List<HighlightPage>,
    public val hint: Int? = null,
    public val onFinish: ((Player) -> Unit)? = null,
    public val onClose: (Player) -> Unit = {},
)

/** [showClose] shows `screenhighlight:close`; [defaultTextbox] sends `highlight_textbox_default`. */
public data class HighlightPage(
    val frame: HighlightFrame,
    val text: String,
    val textColour: Int = -1,
    val showClose: Boolean = false,
    val defaultTextbox: Boolean = false,
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
