package org.rsmod.content.events.leaguetasks.tutorial

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.cinematic.Cinematic
import org.rsmod.api.player.cinematic.MinimapState
import org.rsmod.api.player.midiSong
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.ui.ifCloseModal
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenFullOverlay
import org.rsmod.api.player.ui.ifOpenMainModal
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.ui.ifSetHide
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.table.LeagueTutorialHintRow
import org.rsmod.content.events.leaguetasks.LeagueTasks
import org.rsmod.content.events.leaguetasks.pacts.LeaguePacts
import org.rsmod.content.interfaces.journal.tab.SideJournalTab
import org.rsmod.content.interfaces.journal.tab.switchJournalTab
import org.rsmod.content.interfaces.screenhighlight.HighlightFrame
import org.rsmod.content.interfaces.screenhighlight.HighlightPage
import org.rsmod.content.interfaces.screenhighlight.ScreenHighlightTour
import org.rsmod.content.interfaces.screenhighlight.ScreenHighlightTours
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player

/**
 * The step is `varbit.league_tutorial_completed`. The client draws the welcome modal's text for each
 * step from the cache, so the server only moves the step on, opens the menus with their tutorial
 * events and runs the four `screenhighlight` tours whose pages are `dbtable.league_tutorial_hint`.
 *
 * Steps 0, 3, 4, 5, 7, 9, 11 and 13 wait on the modal; 6, 8, 10 and 12 are the tours. A relog
 * resumes at the last modal step, so the player repeats the click that opens the tour.
 */
@Singleton
public class LeagueTutorial
@Inject
constructor(
    private val tours: ScreenHighlightTours,
    private val pacts: LeaguePacts,
    tasks: LeagueTasks,
    private val eventBus: EventBus,
) {
    private val colour = tasks.league(DEMONIC_PACTS)?.notificationColour ?: -1
    private val tasksTour = tour(TASKS_HINT) { player, _ -> finishTasksTour(player) }
    private val areasTour =
        tour(AREAS_HINT, onPage = ::flashArea) { player, _ -> finishAreasTour(player) }
    private val relicsTour = tour(RELICS_HINT) { player, _ -> finishRelicsTour(player) }
    private val pactsTour = tour(PACTS_HINT, onFinish = ::finishPactsTour)

    public fun isActive(player: Player): Boolean = step(player) < COMPLETE

    public fun takesMenuButton(player: Player, menu: LeagueMenu): Boolean {
        if (!isActive(player)) {
            return false
        }
        val step = step(player)
        when {
            menu == LeagueMenu.Tasks && step == TASKS_BUTTON -> startTasksTour(player)
            menu == LeagueMenu.Areas && step == AREAS_BUTTON -> startAreasTour(player)
            menu == LeagueMenu.Relics && step == RELICS_BUTTON -> startRelicsTour(player)
            menu == LeagueMenu.Relics && step == RELICS_TOUR -> reopenRelics(player)
            menu == LeagueMenu.Pacts && (step == PACTS_BUTTON || step == PACTS_TOUR) ->
                startPactsTour(player)
        }
        return true
    }

    internal fun resume(player: Player) {
        VarPlayerIntMapSetter.set(player, "varbit.orbs_disabled", 1)
        VarPlayerIntMapSetter.set(player, "varbit.chatoverlay_allowmesoverlap", 1)
        player.ifCloseOverlay("interface.orbs", eventBus)
        VarPlayerIntMapSetter.set(player, BUSY, 1)
        val step = resumeStep(player)
        if (step == WELCOME) {
            setStep(player, WELCOME)
            showModal(player, fade = false)
        } else {
            enterStep(player, step)
        }
    }

    internal fun getStarted(player: Player) {
        if (step(player) != WELCOME) {
            return
        }
        VarPlayerIntMapSetter.set(player, "varbit.league_3_has_seen_cutscene", 1)
        player.ifCloseModal(MODAL, eventBus)
        player.switchJournalTab(SideJournalTab.Quests, eventBus)
        enterStep(player, JOURNAL)
    }

    internal fun onJournalStone(player: Player) {
        if (step(player) != JOURNAL) {
            return
        }
        VarPlayerIntMapSetter.set(player, FLASHSIDE, 0)
        player.ifCloseModal(MODAL, eventBus)
        enterStep(player, LEAGUE_TAB)
    }

    internal fun onSidePanelOpened(player: Player) {
        if (step(player) != LEAGUE_TAB) {
            return
        }
        player.ifCloseModal(MODAL, eventBus)
        enterStep(player, TASKS_BUTTON)
    }

    internal fun onRelicUnlocked(player: Player) {
        if (step(player) == RELICS_TOUR) {
            enterStep(player, PACTS_BUTTON)
        }
    }

    internal fun onPactsSpent(player: Player) {
        if (step(player) != PACTS_TOUR) {
            return
        }
        showOverlay(player, fade = false)
        pacts.openTree(player)
        tours.start(player, pactsTour, page = PACTS_AFTER_SPEND_PAGE)
        Cinematic.setMinimapState(player, MinimapState.MinimapNoOp)
    }

    internal fun finish(player: Player) {
        if (step(player) != FINISHING) {
            return
        }
        setStep(player, COMPLETE)
        VarPlayerIntMapSetter.set(player, "varbit.orbs_disabled", 0)
        VarPlayerIntMapSetter.set(player, "varbit.chatoverlay_allowmesoverlap", 0)
        VarPlayerIntMapSetter.set(player, BUSY, 0)
        player.ifCloseModal(MODAL, eventBus)
        player.ifSetHide("component.league_tutorial:noclick", true)
        player.ifCloseOverlay(OVERLAY, eventBus)
        player.ifOpenOverlay("interface.orbs", "component.toplevel_osrs_stretch:orbs", eventBus)
        pulse(player, "mastery_highlight", start = true)
        Cinematic.setMinimapState(player, MinimapState.Normal)
    }

    private fun startTasksTour(player: Player) {
        setStep(player, TASKS_TOUR)
        player.ifCloseModal(MODAL, eventBus)
        pulse(player, "tasks_highlight", start = false)
        player.runClientScript("clientscript.[clientscript,script2446]".asRSCM(RSCMType.CLIENTSCRIPT))
        player.ifOpenOverlay("interface.league_tasks", eventBus)
        player.ifSetEvents("component.league_tasks:dropdown_tier", 1..7, IfEvent.Op1)
        player.ifSetEvents("component.league_tasks:dropdown_completed", 1..4, IfEvent.Op1)
        player.ifSetEvents("component.league_tasks:dropdown_mastery", 1..4, IfEvent.Op1)
        player.ifSetEvents("component.league_tasks:dropdown_type", 1..8, IfEvent.Op1)
        player.ifSetEvents("component.league_tasks:dropdown_area", 1..15, IfEvent.Op1)
        player.ifSetEvents("component.league_tasks:league_menu_frame", 10..14, IfEvent.Op1)
        tours.start(player, tasksTour)
    }

    private fun finishTasksTour(player: Player) {
        player.ifCloseOverlay("interface.screenhighlight", eventBus)
        player.ifCloseOverlay("interface.league_tasks", eventBus)
        player.runClientScript("clientscript.script9431".asRSCM(RSCMType.CLIENTSCRIPT))
        tours.hide(player, tasksTour)
        enterStep(player, AREAS_BUTTON)
    }

    private fun startAreasTour(player: Player) {
        setStep(player, AREAS_TOUR)
        player.ifCloseModal(MODAL, eventBus)
        pulse(player, "areas_highlight", start = false)
        player.ifOpenMainModal("interface.trailblazer_areas", eventBus, -1, -3)
        player.ifSetEvents("component.trailblazer_areas:league_menu_frame", 10..16, IfEvent.Op1)
        tours.start(player, areasTour)
    }

    private fun flashArea(player: Player, page: Int) {
        when (page) {
            VARLAMORE_PAGE -> {
                flash(player, "karamja", start = false)
                flash(player, "varlamore", start = true)
            }
            KARAMJA_PAGE -> {
                flash(player, "varlamore", start = false)
                flash(player, "karamja", start = true)
            }
            VARLAMORE_PAGE - 1,
            KARAMJA_PAGE + 1 -> {
                flash(player, "varlamore", start = false)
                flash(player, "karamja", start = false)
            }
        }
    }

    private fun finishAreasTour(player: Player) {
        player.ifCloseModal("interface.trailblazer_areas", eventBus)
        enterStep(player, RELICS_BUTTON)
    }

    private fun startRelicsTour(player: Player) {
        setStep(player, RELICS_TOUR)
        player.ifCloseModal(MODAL, eventBus)
        pulse(player, "relics_highlight", start = false)
        player.ifOpenMainModal("interface.league_relics", eventBus, -1, -3)
        player.ifSetEvents("component.league_relics:clickzones", 0..23, IfEvent.Op1)
        player.ifSetEvents("component.league_relics:league_menu_frame", 10..12, IfEvent.Op1)
        tours.start(player, relicsTour)
    }

    /**
     * The tour stays on its last page with the menu fully usable until a relic is picked; closing
     * the menu does not move the tutorial on.
     */
    private fun finishRelicsTour(player: Player) {
        player.ifSetEvents("component.league_relics:league_menu_frame", 10..20, IfEvent.Op1)
        player.ifSetEvents("component.league_relics:toggles", -1..-1, IfEvent.ScriptTrigger)
        val last = relicsTour.pages.lastIndex
        tours.show(player, relicsTour, last, step = relicsTour.pages.size)
    }

    /** UNVERIFIED: how the menu reopens after a close without a pick. */
    private fun reopenRelics(player: Player) {
        player.ifOpenMainModal("interface.league_relics", eventBus, -1, -3)
        player.ifSetEvents("component.league_relics:clickzones", 0..23, IfEvent.Op1)
        player.ifSetEvents("component.league_relics:league_menu_frame", 10..20, IfEvent.Op1)
        player.ifSetEvents("component.league_relics:toggles", -1..-1, IfEvent.ScriptTrigger)
        tours.start(player, relicsTour, page = relicsTour.pages.lastIndex)
    }

    private fun startPactsTour(player: Player) {
        setStep(player, PACTS_TOUR)
        player.ifCloseModal(MODAL, eventBus)
        pulse(player, "mastery_highlight", start = false)
        pacts.openTree(player)
        val page = if (pacts.isUnlocked(player, ROOT_PACT)) PACTS_AFTER_SPEND_PAGE else 0
        tours.start(player, pactsTour, page)
    }

    /** Page 2 hands over to the spend, which reopens the tour at page 3; page 4 ends it. */
    private fun finishPactsTour(player: Player, page: Int) {
        player.ifCloseOverlay("interface.talent_tree", eventBus)
        if (page < PACTS_AFTER_SPEND_PAGE) {
            pacts.openTree(player)
            player.runClientScript("clientscript.script9397".asRSCM(RSCMType.CLIENTSCRIPT))
            return
        }
        enterStep(player, FINISHING)
    }

    private fun enterStep(player: Player, step: Int) {
        setStep(player, step)
        when (step) {
            JOURNAL -> VarPlayerIntMapSetter.set(player, FLASHSIDE, JOURNAL_FLASHSIDE)
            LEAGUE_TAB -> {
                ClientScripts.toplevelSidebuttonSwitch(player, JOURNAL_SIDE)
                player.runClientScript(
                    "clientscript.[clientscript,league_icon_flash]".asRSCM(RSCMType.CLIENTSCRIPT),
                    "component.side_journal:tab_icon_5".asRSCM(RSCMType.COMPONENT),
                )
            }
            TASKS_BUTTON -> pulse(player, "tasks_highlight", start = true)
            AREAS_BUTTON -> pulse(player, "areas_highlight", start = true)
            RELICS_BUTTON -> pulse(player, "relics_highlight", start = true)
            PACTS_BUTTON -> pulse(player, "mastery_highlight", start = true)
        }
        showModal(player, fade = true)
    }

    private fun resumeStep(player: Player): Int =
        when (val step = step(player)) {
            in WELCOME + 1..JOURNAL -> JOURNAL
            TASKS_TOUR,
            AREAS_TOUR -> step - 1
            RELICS_TOUR -> if (player.vars[FIRST_RELIC] != 0) PACTS_BUTTON else RELICS_BUTTON
            PACTS_TOUR -> if (pacts.isUnlocked(player, ROOT_PACT)) FINISHING else PACTS_BUTTON
            else -> step
        }

    private fun showModal(player: Player, fade: Boolean) {
        showOverlay(player, fade)
        player.ifOpenMainModal(MODAL, eventBus, -1, -1)
        Cinematic.setMinimapState(player, MinimapState.MinimapNoOp)
    }

    private fun showOverlay(player: Player, fade: Boolean) {
        player.ifOpenFullOverlay(OVERLAY, eventBus)
        player.ifSetHide("component.league_tutorial:noclick", false)
        val speed = if (fade) MUSIC_FADE else 0
        player.midiSong("midi.yama_combat", fadeOutSpeed = speed, fadeInDelay = speed)
    }

    private fun pulse(player: Player, highlight: String, start: Boolean) {
        val script = if (start) "league_border_pulse_start" else "league_border_pulse_stop"
        player.runClientScript(
            "clientscript.[clientscript,$script]".asRSCM(RSCMType.CLIENTSCRIPT),
            "component.league_side_panel:$highlight".asRSCM(RSCMType.COMPONENT),
        )
    }

    private fun flash(player: Player, region: String, start: Boolean) {
        val script = if (start) "league_area_flash_start" else "league_area_flash_stop"
        player.runClientScript(
            "clientscript.[clientscript,$script]".asRSCM(RSCMType.CLIENTSCRIPT),
            "component.trailblazer_areas:league_shield_${region}_highlight"
                .asRSCM(RSCMType.COMPONENT),
            "component.trailblazer_areas:league_name_${region}_highlight"
                .asRSCM(RSCMType.COMPONENT),
        )
    }

    internal fun step(player: Player): Int = player.vars[STEP]

    private fun setStep(player: Player, step: Int) {
        VarPlayerIntMapSetter.set(player, STEP, step)
    }

    private fun tour(
        hint: Int,
        onPage: (Player, Int) -> Unit = { _, _ -> },
        onFinish: (Player, Int) -> Unit,
    ): ScreenHighlightTour {
        val rows = LeagueTutorialHintRow.all().filter { it.hint == hint }.sortedBy { it.page }
        check(rows.isNotEmpty()) { "dbtable.league_tutorial_hint has no pages for hint $hint." }
        return ScreenHighlightTour(
            host = rows.first().host,
            pages = rows.map { it.toPage() },
            hint = hint,
            onFinish = onFinish,
            onPage = onPage,
        )
    }

    private fun LeagueTutorialHintRow.toPage(): HighlightPage {
        val frame =
            if (focus != NO_COMPONENT) {
                HighlightFrame.Component(
                    outer = focusOuter,
                    focus = focus,
                    padding = focusSub,
                    setPosH = setposH,
                    setPosV = setposV,
                )
            } else {
                HighlightFrame.Screen(offX, offY, offH, offW, setposH, setposV, hideCom7 = hideCom7)
            }
        return HighlightPage(
            frame = frame,
            text = text,
            textColour = colour,
            defaultTextbox = defaultTextbox,
            lastPage = lastPage,
            pause = pause,
            previousButton = previousButton,
        )
    }

    internal companion object {
        const val DEMONIC_PACTS = 6

        const val WELCOME = 0
        const val JOURNAL = 3
        const val LEAGUE_TAB = 4
        const val TASKS_BUTTON = 5
        const val TASKS_TOUR = 6
        const val AREAS_BUTTON = 7
        const val AREAS_TOUR = 8
        const val RELICS_BUTTON = 9
        const val RELICS_TOUR = 10
        const val PACTS_BUTTON = 11
        const val PACTS_TOUR = 12
        const val FINISHING = 13
        const val COMPLETE = 14

        const val TASKS_HINT = 1
        const val RELICS_HINT = 2
        const val AREAS_HINT = 3
        const val PACTS_HINT = 7

        const val VARLAMORE_PAGE = 2
        const val KARAMJA_PAGE = 3
        const val PACTS_AFTER_SPEND_PAGE = 3
        const val ROOT_PACT = 0
        const val NO_COMPONENT = -1
        const val JOURNAL_FLASHSIDE = 3
        const val JOURNAL_SIDE = 2
        const val MUSIC_FADE = 60

        const val STEP = "varbit.league_tutorial_completed"
        const val FLASHSIDE = "varbit.flashside"
        const val FIRST_RELIC = "varbit.league_relic_selection_0"
        const val BUSY = "varbit.busy"
        const val MODAL = "interface.league_tutorial_main"
        const val OVERLAY = "interface.league_tutorial"
    }
}
