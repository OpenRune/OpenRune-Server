package org.alter.game.util.vars

import dev.openrune.definition.util.BaseVarType
import dev.openrune.definition.util.VarType

abstract class GenericStringVarType(
    protected val varType: VarType
) : StringVarType<String> {
    override val type: VarType get() = varType
    override val char: Char get() = varType.ch
    override val id: Int get() = varType.id

    override fun convertTo(variable: String) = variable
    override fun convertFrom(variable: String) = variable
}

internal interface StringVarType<V> : VarTypeImpl<String, V> {
    override val baseVarType: BaseVarType get() = BaseVarType.STRING
}
