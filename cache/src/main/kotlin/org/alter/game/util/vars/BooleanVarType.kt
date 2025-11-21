package org.alter.game.util.vars

import dev.openrune.definition.util.BaseVarType
import dev.openrune.definition.util.VarType

abstract class BooleanVarType(
    protected val varType: VarType
) : IntegerVarType<Boolean> {

    override val type: VarType get() = varType
    override val baseVarType: BaseVarType get() = BaseVarType.INTEGER
    override val char: Char get() = varType.ch
    override val id: Int get() = varType.id

    override fun convertTo(variable: Int): Boolean = variable != 0

    fun convertToAny(variable: Any): Boolean = when (variable) {
        is Number -> variable.toInt() != 0
        is String -> variable.toIntOrNull()?.let { it != 0 } ?: false
        else -> false
    }

    override fun convertFrom(variable: Boolean): Int = if (variable) 1 else 0
}