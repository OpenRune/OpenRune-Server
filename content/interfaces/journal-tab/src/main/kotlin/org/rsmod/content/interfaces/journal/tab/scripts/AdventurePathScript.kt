package org.rsmod.content.interfaces.journal.tab.scripts

import dev.openrune.definition.type.widget.IfEvent
import jakarta.inject.Inject
import org.rsmod.api.player.ui.ifCloseModals
import org.rsmod.api.player.ui.ifOpenMainModal
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onOpNpc4
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The Adventure Path interface — the full-screen `adventurepath` (642) modal and the click handling
 * behind it.
 *
 * Every component index and event below is transcribed from capture `20260822T133202`, ticks 35-64;
 * none of the layout is inferred. The modal is opened at capture line 1448 with
 * `runclientscript toplevel_mainmodal_open (2524) [-1, -1]` immediately before the `if_opensub`,
 * which is exactly what [ifOpenMainModal]'s default `colour`/`transparency` of `-1` emits.
 *
 * ## Layout
 *
 * Both list components are strided, and the stride is what makes the indices look arbitrary:
 *
 * - **`tasks` (642:7)** — 15 rows, `Op1` on `11 + 12n` for `n` in `0..14`, so 11, 23, 35 ... 179
 *   (capture lines 1449-1463). Clicking row `n` writes `adventurepath_selected_task` = `n + 1`;
 *   the capture walks the whole list one row at a time and the varp counts 1 → 15 in lockstep
 *   (lines 1665, 1694, 1713, 1741 ... 2071).
 * - **`paths` (642:35)** — 5 paths of 39 components. Within each path block the offsets are `+0`
 *   `Op1`, `+10` `Op1` and `+38` `Op4`, giving bases 0, 39, 78, 117, 156 (lines 1464-1478).
 *   Clicking `+10` writes `adventurepath_main_if_tab` (line 1548).
 */
public class AdventurePathScript @Inject constructor(private val eventBus: EventBus) :
    PluginScript() {

    override fun ScriptContext.startup() {
        // Side-panel entry points. None of these are armed by a packet in the capture, so they are
        // armed in the cache and only ever needed handlers — which is why every one of them was
        // silently dead. Each opens the full-screen modal (capture ticks 110-129; the paired
        // `if_closesub ... id=adventurepath (642)` at lines 3261, 3417 and 3645 is the previous
        // modal being torn down as the next one opens).
        onIfOverlayButton("component.adventurepath_side:toggles") {
            player.openAdventurePathModal()
        }

        // "Click here to see all the paths" (capture line 3648).
        onIfOverlayButton("component.adventurepath_side:open_interface") {
            player.openAdventurePathModal()
        }

        onIfOverlayButton("component.adventurepath_side:task_name") {
            player.openAdventurePathModal()
        }

        onIfOverlayButton("component.adventurepath_side:path_name") {
            player.openAdventurePathModal()
        }

        // Capture lines 3114-3116: this one also switches the modal to tab 2 and clears the
        // selected path before opening.
        onIfOverlayButton("component.adventurepath_side:task_description") {
            player.mainInterfaceTab = 2
            player.selectedPath = 0
            player.openAdventurePathModal()
        }

        // Adventurer Jon's right-click "Open Adventure Paths". The op lives on `ap_guide_active`
        // (op4) but the spawned entity is `ap_guide_parent`, so both are bound for the same reason
        // the Talk-to handler binds both.
        onOpNpc4("npc.ap_guide_parent") { player.openAdventurePathModal() }
        onOpNpc4("npc.ap_guide_active") { player.openAdventurePathModal() }

        // `adventurepath:tasks` is deliberately NOT bound. See [taskSelectionIsClientOwned].

        onIfModalButton("component.adventurepath:paths") {
            player.selectPath(it.comsub)
        }

        // Capture lines 2445-2446 and 2771-2772: `back` writes `adventurepath_main_if_tab 1 -> 0`
        // and nothing else. It is in-modal navigation from a path's detail view back to the path
        // list — it does NOT close the interface, which is what closing it here used to do.
        onIfModalButton("component.adventurepath:back") { player.mainInterfaceTab = TAB_PATH_LIST }
        // No click on `extra_back_button` is captured; it is armed identically at sub 9 and named
        // as a second back control, so it returns to the same view.
        onIfModalButton("component.adventurepath:extra_back_button") {
            player.mainInterfaceTab = TAB_PATH_LIST
        }
    }

    /** Opens the modal and arms every component the live server arms, in the captured order. */
    private fun Player.openAdventurePathModal() {
        ifOpenMainModal("interface.adventurepath", eventBus)

        for (n in 0 until TASK_ROWS) {
            val sub = TASK_FIRST + (n * TASK_STRIDE)
            ifSetEvents("component.adventurepath:tasks", sub..sub, IfEvent.Op1)
        }

        for (n in 0 until PATH_ROWS) {
            val base = n * PATH_STRIDE
            ifSetEvents("component.adventurepath:paths", base..base, IfEvent.Op1)
            val select = base + PATH_SELECT_OFFSET
            ifSetEvents("component.adventurepath:paths", select..select, IfEvent.Op1)
            val extra = base + PATH_SHOW_OFFSET
            ifSetEvents("component.adventurepath:paths", extra..extra, IfEvent.Op4)
        }

        ifSetEvents("component.adventurepath:task_focus", 9..10, IfEvent.Op1)
        ifSetEvents("component.adventurepath:back", 9..9, IfEvent.Op1)
        ifSetEvents("component.adventurepath:extra_back_button", 9..9, IfEvent.Op1)
        ifSetEvents("component.adventurepath:task_hint", 1..2, IfEvent.Op1)
        ifSetEvents("component.adventurepath:path_reward", 9..9, IfEvent.Op1)
    }

    /**
     * Why `adventurepath:tasks` has no handler.
     *
     * The capture does show the live server writing `adventurepath_selected_task` after each task
     * click (lines 1665, 1694, 1713 ... 2071), and an earlier version of this script reproduced
     * that as `row + 1`, deriving the value from the clicked row's position in the list.
     *
     * That is wrong, and the way it failed identifies the reason precisely: the Combat path
     * behaved correctly while the Gathering path flashed its rewards and blanked them. The capture
     * was recorded while browsing a single path whose tasks happen to be numbered 1-15, so "row
     * index + 1" and "task id" were the same number throughout and the distinction was invisible.
     * `adventurepath_selected_task` is a **global task id**, not a position in the visible list;
     * every path after the first has a different id range, so `row + 1` named someone else's task
     * and the reward panel rendered for it and then corrected itself.
     *
     * Producing the right value needs a path -> task-id mapping, which is Adventure Path *content*
     * and is not in any capture I have. Until that exists the client's own selection is correct on
     * its own — the same thing that made removing the `selectedPath` write fix the path buttons —
     * so the server writes nothing here rather than writing a confidently wrong id.
     */
    private val taskSelectionIsClientOwned: Unit = Unit

    /**
     * `adventurepath_main_if_tab` is a *view* selector, not a path index:
     *
     * - line 1548: clicking a path writes `main_if_tab 0 -> 1` (list -> path detail)
     * - line 2446: `back` writes `main_if_tab 1 -> 0` (path detail -> list)
     * - line 3115: `task_description` writes `main_if_tab 0 -> 2` (list -> task view)
     *
     * ## Why this writes the view and nothing else
     *
     * The captured click is two lines long and the second line is the whole server response:
     *
     * ```
     * if_buttonx com=adventurepath:paths (642:35), sub=10, op=1
     * varbit adventurepath_main_if_tab (9328), old=0, new=1
     * ```
     *
     * `adventurepath_selected_path` is **not** written here. The client already knows which path
     * was clicked — it owns that selection locally, which is why the interface's own "COMBAT PATH"
     * header navigates correctly with no server involvement at all. An earlier version of this
     * handler also assigned `selectedPath`, and that write raced the client's own value: the panel
     * rendered the previously selected path until a second click happened to agree with it.
     *
     * The only captured write to `selected_path` is `task_description` clearing it (line 3116).
     */
    private fun Player.selectPath(comsub: Int) {
        if (comsub % PATH_STRIDE != PATH_SELECT_OFFSET) return
        val path = comsub / PATH_STRIDE
        if (path >= PATH_ROWS) return
        if (mainInterfaceTab != TAB_PATH_DETAIL) {
            mainInterfaceTab = TAB_PATH_DETAIL
        }
    }

    private companion object {
        private const val TASK_FIRST = 11
        private const val TASK_STRIDE = 12
        private const val TASK_ROWS = 15

        private const val PATH_STRIDE = 39
        private const val PATH_ROWS = 5
        private const val PATH_ROW_OFFSET = 0
        private const val PATH_SELECT_OFFSET = 10
        private const val PATH_SHOW_OFFSET = 38

        /** `adventurepath_main_if_tab` views, from the captured transitions. */
        private const val TAB_PATH_LIST = 0
        private const val TAB_PATH_DETAIL = 1

        /** `varp.adventurepath_selected_task` (2358). */
        private var Player.selectedTask by intVarp("varp.adventurepath_selected_task")

        /** `varp.adventurepath_selected_path` (2357) — cleared by `task_description`, line 3116. */
        private var Player.selectedPath by intVarp("varp.adventurepath_selected_path")

        /** `varbit.adventurepath_main_if_tab` (9328). */
        private var Player.mainInterfaceTab by intVarBit("varbit.adventurepath_main_if_tab")
    }
}
