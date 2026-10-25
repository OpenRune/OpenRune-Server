package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object BirdHouseTypesTable {
    const val TIER = 0
    const val ITEM = 1
    const val HUNTER_LEVEL = 2
    const val HUNTER_XP = 3
    const val NEST_CHANCE = 4

    private data class Row(
        val name: String,
        val item: String,
        val hunterLevel: Int,
        val hunterXp: Int,
        val nestChance: Int,
    )

    private val ROWS =
        listOf(
            Row("normal", "obj.birdhouse_normal", 5, 112, 300),
            Row("oak", "obj.birdhouse_oak", 14, 168, 375),
            Row("willow", "obj.birdhouse_willow", 24, 224, 384),
            Row("teak", "obj.birdhouse_teak", 34, 280, 390),
            Row("maple", "obj.birdhouse_maple", 44, 369, 420),
            Row("mahogany", "obj.birdhouse_mahogany", 49, 480, 450),
            Row("yew", "obj.birdhouse_yew", 59, 612, 480),
            Row("magic", "obj.birdhouse_magic", 74, 969, 510),
            Row("redwood", "obj.birdhouse_redwood", 89, 1200, 525),
        )

    fun birdHouseTypes() =
        dbTable("dbtable.birdhouse_types", serverOnly = true) {
            column("tier", TIER, VarType.INT)
            column("item", ITEM, VarType.OBJ)
            column("hunter_level", HUNTER_LEVEL, VarType.INT)
            column("hunter_xp", HUNTER_XP, VarType.INT)
            column("nest_chance", NEST_CHANCE, VarType.INT)

            for ((tier, r) in ROWS.withIndex()) {
                row("dbrow.birdhouse_type_${r.name}") {
                    column(TIER, tier)
                    columnRSCM(ITEM, r.item)
                    column(HUNTER_LEVEL, r.hunterLevel)
                    column(HUNTER_XP, r.hunterXp)
                    column(NEST_CHANCE, r.nestChance)
                }
            }
        }
}
