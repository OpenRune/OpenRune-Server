package dev.openrune.tables.consumables.food

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

public object FoodTable {
    private const val COL_ITEMS = 0
    private const val COL_HEAL = 1
    private const val COL_COMBO = 2
    private const val COL_EFFECT = 3
    private const val COL_OVERHEAL = 4
    private const val COL_EAT_DELAY = 5
    private const val COL_COMBAT_DELAY = 6

    public fun table(): DBTable {
        val foods = FoodTomlLoader.load()

        return dbTable(
            "dbtable.food",
            serverOnly = true,
        ) {
            column(
                "items",
                COL_ITEMS,
                VarType.OBJ,
            )
            column(
                "heal",
                COL_HEAL,
                VarType.INT,
            )
            column(
                "combo",
                COL_COMBO,
                VarType.BOOLEAN,
            )
            column(
                "effect",
                COL_EFFECT,
                VarType.STRING,
            )
            column(
                "overheal",
                COL_OVERHEAL,
                VarType.BOOLEAN,
            )
            column(
                "eat_delay",
                COL_EAT_DELAY,
                VarType.INT,
            )
            column(
                "combat_delay",
                COL_COMBAT_DELAY,
                VarType.INT,
            )

            foods.forEach { food ->
                row(food.row) {
                    columnRSCM(
                        COL_ITEMS,
                        *food.items.toTypedArray(),
                    )
                    column(
                        COL_HEAL,
                        food.heal,
                    )
                    column(
                        COL_COMBO,
                        food.combo,
                    )
                    column(
                        COL_EFFECT,
                        food.effect,
                    )
                    column(
                        COL_OVERHEAL,
                        food.overheal,
                    )

                    if (food.eatDelay.isNotEmpty()) {
                        column(
                            COL_EAT_DELAY,
                            *food.eatDelay.toTypedArray(),
                        )
                    }

                    if (food.combatDelay.isNotEmpty()) {
                        column(
                            COL_COMBAT_DELAY,
                            *food.combatDelay.toTypedArray(),
                        )
                    }
                }
            }
        }
    }
}
