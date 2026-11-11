package org.rsmod.content.interfaces.screenhighlight

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.ui.UserInterface

@Singleton
public class ScreenHighlightTours @Inject constructor(private val eventBus: EventBus) {
    private val tours = HashMap<Int, ScreenHighlightTour>()

    public fun start(player: Player, tour: ScreenHighlightTour, page: Int = 0) {
        val registered = tours.getOrPut(tour.host) { tour }
        check(registered === tour) { "Two screenhighlight tours share host ${tour.host}." }
        tour.hint?.let { VarPlayerIntMapSetter.set(player, "varbit.hnt_selected_hint", it) }
        VarPlayerIntMapSetter.set(player, "varbit.hnt_hint_max_step", tour.pages.size)
        val host = RSCM.getReverseMapping(RSCMType.COMPONENT, tour.host)
        player.ifOpenOverlay("interface.screenhighlight", host, eventBus)
        player.ifSetEvents("component.screenhighlight:continue", 9..9, IfEvent.Op1)
        player.ifSetEvents("component.screenhighlight:previous", 9..9, IfEvent.Op1)
        player.runClientScript(
            "clientscript.[clientscript,highlight_start]".asRSCM(RSCMType.CLIENTSCRIPT),
            tour.host,
        )
        show(player, tour, page)
        player.ifSetEvents("component.screenhighlight:pausebutton", -1..-1, IfEvent.PauseButton)
    }

    /** [step] is the `hnt_hint_step` the page is shown under, normally the page itself. */
    public fun show(player: Player, tour: ScreenHighlightTour, page: Int, step: Int = page) {
        val shown = tour.pages[page]
        tour.onPage(player, page)
        VarPlayerIntMapSetter.set(player, "varbit.hnt_hint_step", step)
        player.sendFrame(tour.host, shown.frame)
        val lastPage = if (shown.lastPage ?: (page == tour.pages.lastIndex)) 1 else 0
        val showClose = if (shown.showClose) 1 else 0
        if (shown.defaultTextbox) {
            player.runClientScript(
                "clientscript.[clientscript,highlight_textbox_default]".asRSCM(RSCMType.CLIENTSCRIPT),
                shown.text,
                shown.textColour,
                lastPage,
                showClose,
                tour.host,
                DEFAULT_TEXTBOX_MODE,
            )
        } else {
            player.runClientScript(
                "clientscript.[clientscript,highlight_textbox]".asRSCM(RSCMType.CLIENTSCRIPT),
                shown.text,
                shown.textColour,
                lastPage,
                showClose,
                tour.host,
            )
        }
        if (shown.previousButton) {
            player.runClientScript(
                "clientscript.highlight_button".asRSCM(RSCMType.CLIENTSCRIPT),
                "component.screenhighlight:previous".asRSCM(RSCMType.COMPONENT),
                PREVIOUS_BUTTON_MODE,
                "Previous",
                0,
            )
        }
    }

    public fun hide(player: Player, tour: ScreenHighlightTour) {
        player.runClientScript(
            "clientscript.[clientscript,highlight_screen_hide_layer]".asRSCM(RSCMType.CLIENTSCRIPT),
            tour.host,
        )
    }

    internal fun active(player: Player): ScreenHighlightTour? {
        val screenhighlight = UserInterface("interface.screenhighlight".asRSCM(RSCMType.INTERFACE))
        val host = player.ui.overlays.getComponent(screenhighlight) ?: return null
        return tours[host.packed]
    }

    internal fun tourAt(host: Int): ScreenHighlightTour? = tours[host]

    private fun Player.sendFrame(host: Int, frame: HighlightFrame) {
        when (frame) {
            is HighlightFrame.Screen ->
                runClientScript(
                    "clientscript.[clientscript,highlight_screen]".asRSCM(RSCMType.CLIENTSCRIPT),
                    host,
                    frame.offX,
                    frame.offY,
                    frame.offH,
                    frame.offW,
                    frame.setPosH,
                    frame.setPosV,
                    frame.transparency,
                    if (frame.hideCom7) 1 else 0,
                )
            is HighlightFrame.Component ->
                runClientScript(
                    "clientscript.[clientscript,highlight_screen_component]"
                        .asRSCM(RSCMType.CLIENTSCRIPT),
                    host,
                    frame.outer,
                    frame.focus,
                    frame.padding,
                    frame.setPosH,
                    frame.setPosV,
                    frame.transparency,
                    if (frame.flag) 1 else 0,
                )
        }
    }

    private companion object {
        const val DEFAULT_TEXTBOX_MODE = 1
        const val PREVIOUS_BUTTON_MODE = 1
    }
}
