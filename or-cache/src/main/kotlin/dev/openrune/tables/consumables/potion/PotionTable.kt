package dev.openrune.tables.consumables.potion

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

public object PotionTable {
    private const val KEY = 0
    private const val NAME = 1
    private const val ITEMS = 2
    private const val EMPTY = 3
    private const val EFFECT = 4
    private const val CATEGORY = 5
    private const val WILDERNESS_ONLY = 6
    private const val MIX = 7
    private const val HEAL = 8
    private const val DRINK_DELAY = 9
    private const val COMBAT_DELAY = 10
    private const val MINIGAME_ONLY = 11
    private const val RAID_ONLY = 12

    public fun table() =
        PotionTomlLoader.potions().let { potions ->

            dbTable(
                "dbtable.potion",
                serverOnly = true,
            ) {
                column("key", KEY, VarType.STRING)
                column("name", NAME, VarType.STRING)
                column(
                    "items",
                    ITEMS,
                    VarType.OBJ,
                )
                column("empty", EMPTY, VarType.OBJ)
                column("effect", EFFECT, VarType.DBROW)
                column("category", CATEGORY, VarType.STRING)
                column(
                    "wilderness_only",
                    WILDERNESS_ONLY,
                    VarType.BOOLEAN,
                )
                column("mix", MIX, VarType.BOOLEAN)
                column("heal", HEAL, VarType.INT)
                column(
                    "drink_delay",
                    DRINK_DELAY,
                    VarType.INT,
                )
                column(
                    "combat_delay",
                    COMBAT_DELAY,
                    VarType.INT,
                )
                column(
                    "minigame_only",
                    MINIGAME_ONLY,
                    VarType.STRING,
                )
                column(
                    "raid_only",
                    RAID_ONLY,
                    VarType.STRING,
                )

                potions.forEach { potion ->
                    row(potion.row) {
                        column(KEY, potion.key)
                        column(NAME, potion.name)
                        columnRSCM(
                            ITEMS,
                            *potion.items.toTypedArray(),
                        )
                        columnRSCM(EMPTY, potion.empty)
                        columnRSCM(EFFECT, potion.effect)
                        column(CATEGORY, potion.category)
                        column(
                            WILDERNESS_ONLY,
                            potion.wildernessOnly,
                        )
                        column(MIX, potion.mix)
                        column(HEAL, potion.heal)
                        column(
                            DRINK_DELAY,
                            potion.drinkDelay,
                        )
                        column(
                            COMBAT_DELAY,
                            potion.combatDelay,
                        )
                        column(
                            MINIGAME_ONLY,
                            potion.minigameOnly,
                        )
                        column(
                            RAID_ONLY,
                            potion.raidOnly,
                        )
                    }
                }
            }
        }
}
