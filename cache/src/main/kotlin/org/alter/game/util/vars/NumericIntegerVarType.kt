package org.alter.game.util.vars

import dev.openrune.definition.util.BaseVarType
import dev.openrune.definition.util.VarType

abstract class NumericIntegerVarType(
    protected val varType: VarType
) : IntegerVarType<Int> {
    override val type: VarType get() = varType
    override val char: Char get() = varType.ch
    override val id: Int get() = varType.id



    override fun convertTo(variable: Int) = variable
    override fun convertFrom(variable: Int) = variable
}

internal interface IntegerVarType<V> : VarTypeImpl<Int, V> {
    override val baseVarType: BaseVarType get() = BaseVarType.INTEGER
}