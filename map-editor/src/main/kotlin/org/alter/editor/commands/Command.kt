package org.alter.editor.commands

interface Command {
    fun execute()
    fun undo()
    val description: String
}

class CompoundCommand(private val commands: List<Command>) : Command {
    override val description = commands.firstOrNull()?.description ?: "compound"
    override fun execute() = commands.forEach { it.execute() }
    override fun undo() = commands.reversed().forEach { it.undo() }
}

class CommandStack(private val maxSize: Int = 100) {
    private val undoStack = ArrayDeque<Command>()
    private val redoStack = ArrayDeque<Command>()

    fun execute(cmd: Command) {
        cmd.execute()
        undoStack.addLast(cmd)
        redoStack.clear()
        if (undoStack.size > maxSize) undoStack.removeFirst()
    }

    fun undo(): Boolean {
        val cmd = undoStack.removeLastOrNull() ?: return false
        cmd.undo()
        redoStack.addLast(cmd)
        return true
    }

    fun redo(): Boolean {
        val cmd = redoStack.removeLastOrNull() ?: return false
        cmd.execute()
        undoStack.addLast(cmd)
        return true
    }
}
