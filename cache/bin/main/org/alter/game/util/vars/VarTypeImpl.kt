package org.alter.game.util.vars

import dev.openrune.definition.util.BaseVarType
import dev.openrune.definition.util.VarType

interface VarTypeImpl<K, V> {
    val char: Char
    val id: Int
    val type: VarType
    val baseVarType: BaseVarType?
        get() = type.baseType

    fun convertTo(variable: K): V
    fun convertFrom(variable: V): K
}
