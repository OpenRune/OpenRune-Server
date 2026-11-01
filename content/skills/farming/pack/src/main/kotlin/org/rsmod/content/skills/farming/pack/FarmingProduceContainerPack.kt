package org.rsmod.content.skills.farming.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.PluginPack
import dev.openrune.rscm.RSCM.asRSCM

class FarmingProduceContainerPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> = listOf(FarmingProduceContainerTable.containers())
}

object FarmingProduceContainerTable {
    private const val COL_FORMS = 0

    private const val SACK_CAPACITY = 10
    private const val BASKET_CAPACITY = 5

    fun containers() =
        dbTable("dbtable.farming_produce_containers", serverOnly = true) {
            column("forms", COL_FORMS, VarType.OBJ, VarType.OBJ, VarType.INT)

            val forms = mutableListOf<Int>()
            fun family(empty: String, prefix: String, capacity: Int, vararg produce: Pair<String, String>) {
                for ((_, obj) in produce) forms += listOf(empty.asRSCM(), obj.asRSCM(), 0)
                for ((name, obj) in produce) {
                    for (count in 1..capacity) {
                        forms += listOf("obj.${prefix}_${name}_$count".asRSCM(), obj.asRSCM(), count)
                    }
                }
            }

            family(
                "obj.sack_empty", "sack", SACK_CAPACITY,
                "cabbage" to "obj.cabbage", "onion" to "obj.onion", "potato" to "obj.potato",
            )
            family(
                "obj.basket_empty", "basket", BASKET_CAPACITY,
                "apple" to "obj.cooking_apple", "banana" to "obj.banana", "orange" to "obj.orange",
                "strawberry" to "obj.strawberry", "tomato" to "obj.tomato",
            )

            row("dbrow.farming_produce_containers") { column(COL_FORMS, *forms.toTypedArray()) }
        }
}
