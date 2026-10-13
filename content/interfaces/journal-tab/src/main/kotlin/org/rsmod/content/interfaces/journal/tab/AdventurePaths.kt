package org.rsmod.content.interfaces.journal.tab

import dev.openrune.definition.type.widget.IfEvent
import org.rsmod.api.player.ui.ifOpenMainModal
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.resyncVar
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player

internal const val MAIN_IF_TAB = "varbit.adventurepath_main_if_tab"
internal const val SELECTED_PATH = "varp.adventurepath_selected_path"
internal const val SELECTED_TASK = "varp.adventurepath_selected_task"
private const val DISABLE_TASK_FOCUS_MESSAGE = "varbit.adventurepath_disable_task_focus_message"
private const val DISABLE_PATH_FOCUS_MESSAGE = "varbit.adventurepath_disable_path_focus_message"

internal const val TAB_PATH_LIST = 0
internal const val TAB_PATH_DETAIL = 1
internal const val TAB_TASK_VIEW = 2

internal const val TASK_FIRST = 11
internal const val TASK_STRIDE = 12
internal const val TASK_ROWS = 15
internal const val PATH_STRIDE = 39
internal const val PATH_ROWS = 5
internal const val PATH_SELECT_OFFSET = 10
internal const val PATH_FOCUS_OFFSET = 38

/**
 * Opens the full-screen Adventure Paths interface on [view]. Adventurer Jon passes `null`, which
 * keeps the view the player last had open.
 */
public fun Player.openAdventurePaths(eventBus: EventBus, view: Int? = TAB_PATH_LIST) {
    if (view != null) {
        transmitVar(MAIN_IF_TAB, view)
    }
    transmitVar(SELECTED_PATH, 0)
    transmitVar(SELECTED_TASK, 0)
    // TODO: select the first task the player has not completed yet.
    transmitVar(SELECTED_TASK, 1)

    val taskFocusMessage = vars[DISABLE_TASK_FOCUS_MESSAGE]
    val pathFocusMessage = vars[DISABLE_PATH_FOCUS_MESSAGE]
    transmitVar(DISABLE_TASK_FOCUS_MESSAGE, 1)
    transmitVar(DISABLE_PATH_FOCUS_MESSAGE, 1)
    // TODO: recheck task progress here (in_focus, gather_retro and the "Some of your Adventure
    //  path tasks may have been updated." message) once tasks are implemented.
    transmitVar(DISABLE_TASK_FOCUS_MESSAGE, taskFocusMessage)
    transmitVar(DISABLE_PATH_FOCUS_MESSAGE, pathFocusMessage)

    ifOpenMainModal("interface.adventurepath", eventBus)
    for (n in 0 until TASK_ROWS) {
        val sub = TASK_FIRST + n * TASK_STRIDE
        ifSetEvents("component.adventurepath:tasks", sub..sub, IfEvent.Op1)
    }
    for (n in 0 until PATH_ROWS) {
        val base = n * PATH_STRIDE
        val select = base + PATH_SELECT_OFFSET
        val focus = base + PATH_FOCUS_OFFSET
        ifSetEvents("component.adventurepath:paths", base..base, IfEvent.Op1)
        ifSetEvents("component.adventurepath:paths", select..select, IfEvent.Op1)
        ifSetEvents("component.adventurepath:paths", focus..focus, IfEvent.Op4)
    }
    ifSetEvents("component.adventurepath:task_focus", 9..10, IfEvent.Op1)
    ifSetEvents("component.adventurepath:back", 9..9, IfEvent.Op1)
    ifSetEvents("component.adventurepath:extra_back_button", 9..9, IfEvent.Op1)
    ifSetEvents("component.adventurepath:task_hint", 1..2, IfEvent.Op1)
    ifSetEvents("component.adventurepath:path_reward", 9..9, IfEvent.Op1)
}

/** The live server sends these vars even when the value does not change. */
internal fun Player.transmitVar(internal: String, value: Int) {
    if (vars[internal] == value) {
        resyncVar(internal)
    } else {
        VarPlayerIntMapSetter.set(this, internal, value)
    }
}
