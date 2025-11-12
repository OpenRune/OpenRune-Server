package org.alter.game.model.queue

import net.rsprot.protocol.game.outgoing.interfaces.IfCloseSub
import net.rsprot.protocol.game.outgoing.misc.player.TriggerOnDialogAbort
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.coroutine.suspension.GameCoroutineSimpleCompletion
import java.util.*
import kotlin.coroutines.startCoroutine

abstract class GameQueueSet : QueueStack {

    private var currQueue: GameCoroutine? = null
    private val pendQueue: Queue<GameQueueBlock> = LinkedList()
    protected var currPriority: QueueType = QueueType.Weak

    override val size: Int
        get() = pendQueue.size + (if (currQueue == null) 1 else 0)

    override fun cycle() {
        currQueue?.let {
            if(processScript(it))
                return
        }
        //TODO ADVO: we need to reset pawn, terminate action here or just have it passed to queueset.
        currQueue = null
        /* only reset priority if no other queue is pending */
        if (pendQueue.isEmpty()) {
            currPriority = QueueType.Weak
        }
        while (pendQueue.isNotEmpty()) {
            val ctx = pendQueue.poll() ?: break
            if(pollPending(ctx)) return
        }
    }

    /**
     * this will return true if we are done processing
     * or false if we need to grab another script
     */
    private fun pollPending(ctx: GameQueueBlock): Boolean {
        val coroutine = GameCoroutine()
        val completion = GameCoroutineSimpleCompletion
        ctx.block.startCoroutine(coroutine, completion)
        if (processScript(coroutine)) {
            currQueue = coroutine
            return true
        }
        return false
    }

    /**
     * this will return true if we are done processing
     * or false if we need to grab another script
     */
    abstract fun processScript(task: GameCoroutine): Boolean

    override fun queue(priority: QueueType, block: suspend GameCoroutine.() -> Unit) {
        if (!overtakeQueues(priority)) {
            return
        }
        val queueBlock = GameQueueBlock(block)
        pendQueue.add(queueBlock)
    }

    /**
     * In-game events sometimes must return a value to a plugin. An example are
     * dialogs which must return values such as input, button click, etc.
     *
     * @param value
     * The return value that the plugin has asked for.
     */
    override fun submitReturnValue(value: Any) {//TODO ADVO: change this from Any->Event later
        // we use this currently for also holding on appearance.
        // Shouldn't call this method without a queued task.
        currQueue?.resumeWith(value)
    }

    override fun terminateTasks() {
//        currQueue?.terminate()
//        currQueue = null
//        currPriority = QueueType.Weak
//        pendQueue.clear()
    }

    private fun overtakeQueues(priority: QueueType): Boolean {
        if (priority == currPriority) {
            return true
        }
        if (!priority.overtake(currPriority)) {
            return false
        }
        if (priority != currPriority) {
            terminateTasks()
            currPriority = priority
        }
        return true
    }

    private fun QueueType.overtake(other: QueueType): Boolean = when (this) {
        QueueType.Normal -> other == QueueType.Weak
        QueueType.Strong -> true
        else -> false
    }

    /**
     * Terminate any further execution of this task, during any state,
     * and invoke [terminateAction] if applicable (not null).
     */
    public fun terminate(pawn: Pawn) {
        if(currQueue != null) {
            pawn.terminateAction?.invoke(currQueue!!)
        }
        handleModals(pawn)
        currQueue?.stop()
        currQueue = null
        currPriority = QueueType.Weak
        pendQueue.clear()
    }

    protected fun GameCoroutine.requiresPauseDialogAbort(): Boolean =
        isAwaiting(ContinueDialogue::class)

    protected fun GameCoroutine.requiresInputDialogAbort(): Boolean {
        return isAwaitingAny(
            IntChatInput::class,
            NameChatInput::class,
            StringChatInput::class,
            ItemSearchInput::class,
        )
    }

    protected fun handleModals(pawn: Pawn) {
        if(currQueue != null && pawn is Player) {
            if (currQueue!!.requiresPauseDialogAbort()) {
                pawn.closeComponent(parent = 162, child = 561)
            }
            if (currQueue!!.requiresInputDialogAbort()) {
                pawn.write(TriggerOnDialogAbort)
            }
        }
    }

    private fun Player.closeComponent(parent: Int, child: Int) {
        interfaces.close(parent, child)
        write(IfCloseSub(parent,child))
    }
}