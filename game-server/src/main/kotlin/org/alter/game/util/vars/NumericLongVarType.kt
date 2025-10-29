package org.alter.game.util.vars

import dev.openrune.definition.util.BaseVarType
import dev.openrune.definition.util.VarType

abstract class NumericLongVarType(
    protected val varType: VarType
) : LongVarType<Long> {

    override val type: VarType get() = varType
    override val char: Char get() = varType.ch
    override val id: Int get() = varType.id
    override val baseVarType: BaseVarType get() = BaseVarType.LONG

    override fun convertTo(variable: Long) = variable
    override fun convertFrom(variable: Long) = variable
}

internal interface LongVarType<V> : VarTypeImpl<Long, V> {
    override val baseVarType: BaseVarType get() = BaseVarType.LONG
}