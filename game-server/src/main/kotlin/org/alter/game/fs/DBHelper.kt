package org.alter.game.fs

import dev.openrune.ServerCacheManager

object DBHelper {

    fun getRow(rowId: Int): DBRowWrapper {
        val row = ServerCacheManager.getDbrow(rowId)
            ?: throw NoSuchElementException("DBRow $rowId not found")
        return DBRowWrapper(row)
    }

}