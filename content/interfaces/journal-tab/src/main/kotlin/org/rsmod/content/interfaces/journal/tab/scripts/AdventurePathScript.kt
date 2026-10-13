package org.rsmod.content.interfaces.journal.tab.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onOpNpc4
import org.rsmod.content.interfaces.journal.tab.MAIN_IF_TAB
import org.rsmod.content.interfaces.journal.tab.PATH_ROWS
import org.rsmod.content.interfaces.journal.tab.PATH_SELECT_OFFSET
import org.rsmod.content.interfaces.journal.tab.PATH_STRIDE
import org.rsmod.content.interfaces.journal.tab.SELECTED_PATH
import org.rsmod.content.interfaces.journal.tab.SELECTED_TASK
import org.rsmod.content.interfaces.journal.tab.TAB_PATH_DETAIL
import org.rsmod.content.interfaces.journal.tab.TAB_PATH_LIST
import org.rsmod.content.interfaces.journal.tab.TAB_TASK_VIEW
import org.rsmod.content.interfaces.journal.tab.TASK_FIRST
import org.rsmod.content.interfaces.journal.tab.TASK_ROWS
import org.rsmod.content.interfaces.journal.tab.TASK_STRIDE
import org.rsmod.content.interfaces.journal.tab.adventurePathTasks
import org.rsmod.content.interfaces.journal.tab.openAdventurePaths
import org.rsmod.content.interfaces.journal.tab.transmitVar
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class AdventurePathScript @Inject constructor(private val eventBus: EventBus) :
    PluginScript() {

    override fun ScriptContext.startup() {
        onIfOverlayButton("component.adventurepath_side:toggles") { player.openAdventurePaths() }
        onIfOverlayButton("component.adventurepath_side:open_interface") {
            player.openAdventurePaths()
        }
        onIfOverlayButton("component.adventurepath_side:path_name") { player.openAdventurePaths() }
        // UNVERIFIED: live also writes main_if_tab here depending on the focused task (guess)
        onIfOverlayButton("component.adventurepath_side:task_name") { player.openAdventurePaths() }
        onIfOverlayButton("component.adventurepath_side:task_description") {
            player.openAdventurePaths(eventBus, view = TAB_TASK_VIEW)
        }

        onOpNpc4("npc.ap_guide_parent") { player.openAdventurePaths(eventBus, resendView = false) }
        onOpNpc4("npc.ap_guide_active") { player.openAdventurePaths(eventBus, resendView = false) }

        onIfModalButton("component.adventurepath:paths") { player.selectPath(it.comsub) }
        onIfModalButton("component.adventurepath:tasks") { player.selectTask(it.comsub) }
        onIfModalButton("component.adventurepath:back") {
            player.transmitVar(MAIN_IF_TAB, TAB_PATH_LIST)
        }
        // UNVERIFIED: never captured; its clientscript returns to the task list (guess)
        onIfModalButton("component.adventurepath:extra_back_button") {
            player.transmitVar(MAIN_IF_TAB, TAB_PATH_DETAIL)
        }
        onIfModalButton("component.adventurepath:task_hint") {
            if (it.comsub == TASK_HINT_MORE_INFO) {
                VarPlayerIntMapSetter.set(player, MAIN_IF_TAB, TAB_TASK_VIEW)
            }
        }
        // TODO: task_focus, the path focus op (paths op4), path_reward and the task hint arrow
        //  (task_hint sub 1) need the task system.
    }

    private fun Player.openAdventurePaths() = openAdventurePaths(eventBus)

    private fun Player.selectPath(comsub: Int) {
        val path = comsub / PATH_STRIDE
        val offset = comsub % PATH_STRIDE
        if (path >= PATH_ROWS || (offset != 0 && offset != PATH_SELECT_OFFSET)) {
            return
        }
        val tasks = adventurePathTasks(path)
        val resetTask = vars[SELECTED_TASK] !in tasks
        transmitVar(SELECTED_PATH, path)
        if (resetTask) {
            transmitVar(SELECTED_TASK, 0)
        }
        transmitVar(MAIN_IF_TAB, TAB_PATH_DETAIL)
        if (resetTask && tasks.isNotEmpty()) {
            transmitVar(SELECTED_TASK, tasks.first())
        }
    }

    private fun Player.selectTask(comsub: Int) {
        val row = (comsub - TASK_FIRST) / TASK_STRIDE
        if ((comsub - TASK_FIRST) % TASK_STRIDE != 0 || row !in 0 until TASK_ROWS) {
            return
        }
        val task = adventurePathTasks(vars[SELECTED_PATH]).getOrNull(row) ?: return
        transmitVar(SELECTED_TASK, task)
    }

    private companion object {
        private const val TASK_HINT_MORE_INFO = 2
    }
}
