package org.rsmod.content.interfaces.journal.tab

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
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
 * Opens the full-screen Adventure Paths interface on [view]. The side panel's buttons send the view
 * even when it is unchanged; Adventurer Jon's op only sends a change.
 */
public fun Player.openAdventurePaths(
    eventBus: EventBus,
    view: Int = TAB_PATH_LIST,
    resendView: Boolean = true,
) {
    if (resendView) {
        transmitVar(MAIN_IF_TAB, view)
    } else {
        VarPlayerIntMapSetter.set(this, MAIN_IF_TAB, view)
    }
    transmitVar(SELECTED_PATH, 0)
    // TODO: select the task in focus directly, without the 0 write, once focus is implemented.
    transmitVar(SELECTED_TASK, 0)
    transmitVar(SELECTED_TASK, adventurePathTasks(0).first())

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

/** Task ids of [path] in list order, from the path's task structs. */
internal fun adventurePathTasks(path: Int): List<Int> = PATH_TASKS.getOrElse(path) { emptyList() }

private val PATH_TASKS: List<List<Int>> by lazy {
    val paths =
        ServerCacheManager.getEnum("enum.adventurepath_path_tasks".asRSCM(RSCMType.ENUM))
            ?: error("Missing enum.adventurepath_path_tasks")
    val taskId = "param.adventurepath_task_id".asRSCM(RSCMType.PARAM)
    paths.values.entries
        .sortedBy { (it.key as Number).toInt() }
        .map { (_, tasksEnum) ->
            val tasks =
                ServerCacheManager.getEnum((tasksEnum as Number).toInt())
                    ?: error("Missing Adventure Path task enum $tasksEnum")
            tasks.values.entries
                .sortedBy { (it.key as Number).toInt() }
                .map { (_, structId) ->
                    val struct =
                        ServerCacheManager.getStruct((structId as Number).toInt())
                            ?: error("Missing Adventure Path task struct $structId")
                    struct.getInt(taskId)
                }
        }
}
