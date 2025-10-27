package org.alter.game.fs

import dev.openrune.definition.type.DBColumnType
import dev.openrune.definition.util.Type

class DBColumnWrapper(private val column: DBColumnType, private val rowId: Int, val columnId: Int) {

    val types: Array<Type> get() = column.types

    fun size(): Int = column.values?.size ?: 0

    fun get(index: Int = 0): Any {
        val values = column.values ?: throw NoSuchElementException("No values in column $columnId for row $rowId")
        if (index !in values.indices) throw IndexOutOfBoundsException("Value index $index out of bounds for column $columnId in row $rowId")
        return values[index]
    }

    private inline fun <reified T> getTyped(index: Int = 0): T {
        val value = get(index)
        return value as? T ?: throw IllegalArgumentException("Value at column $columnId index $index in row $rowId is not of type ${T::class}, actual=${value::class}")
    }

    fun getBoolean(index: Int = 0): Boolean = getInt(index) == 1
    fun getInt(index: Int = 0): Int = getTyped(index)
    fun getString(index: Int = 0): String = getTyped(index)
}