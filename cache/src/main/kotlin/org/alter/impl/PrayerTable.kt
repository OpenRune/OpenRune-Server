package org.alter.impl

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.Type

object PrayerTable {

    fun skillTable() = dbTable("tables.skill_prayer") {
        column("exp", 1, arrayOf(Type.INT))

        row("dbrows.bones") {
            column(1, arrayOf(15))
        }

    }

}