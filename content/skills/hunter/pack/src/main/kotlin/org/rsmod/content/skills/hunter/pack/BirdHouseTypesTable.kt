package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object BirdHouseTypesTable {
    const val TIER = 0
    const val ITEM = 1
    const val HUNTER_LEVEL = 2
    const val HUNTER_XP = 3
    const val NEST_CHANCE = 4
    const val LOGS = 5
    const val CRAFTING_LEVEL = 6
    const val CRAFTING_XP = 7

    private data class Row(
        val name: String,
        val item: String,
        val hunterLevel: Int,
        val hunterXp: Int,
        val nestChance: Int,
        val logs: String,
        val craftingLevel: Int,
        val craftingXp: Int,
    )

    private val ROWS =
        listOf(
            Row("normal", "obj.birdhouse_normal", 5, 112, 300, "obj.logs", 5, 15),
            Row("oak", "obj.birdhouse_oak", 14, 168, 375, "obj.oak_logs", 15, 20),
            Row("willow", "obj.birdhouse_willow", 24, 224, 384, "obj.willow_logs", 25, 25),
            Row("teak", "obj.birdhouse_teak", 34, 280, 390, "obj.teak_logs", 35, 30),
            Row("maple", "obj.birdhouse_maple", 44, 369, 420, "obj.maple_logs", 45, 35),
            Row("mahogany", "obj.birdhouse_mahogany", 49, 480, 450, "obj.mahogany_logs", 50, 40),
            Row("yew", "obj.birdhouse_yew", 59, 612, 480, "obj.yew_logs", 60, 45),
            Row("magic", "obj.birdhouse_magic", 74, 969, 510, "obj.magic_logs", 75, 50),
            Row("redwood", "obj.birdhouse_redwood", 89, 1200, 525, "obj.redwood_logs", 90, 55),
        )

    fun birdHouseTypes() =
        dbTable("dbtable.birdhouse_types", serverOnly = true) {
            column("tier", TIER, VarType.INT)
            column("item", ITEM, VarType.OBJ)
            column("hunter_level", HUNTER_LEVEL, VarType.INT)
            column("hunter_xp", HUNTER_XP, VarType.INT)
            column("nest_chance", NEST_CHANCE, VarType.INT)
            column("logs", LOGS, VarType.OBJ)
            column("crafting_level", CRAFTING_LEVEL, VarType.INT)
            column("crafting_xp", CRAFTING_XP, VarType.INT)

            for ((tier, r) in ROWS.withIndex()) {
                row("dbrow.birdhouse_type_${r.name}") {
                    column(TIER, tier)
                    columnRSCM(ITEM, r.item)
                    column(HUNTER_LEVEL, r.hunterLevel)
                    column(HUNTER_XP, r.hunterXp)
                    column(NEST_CHANCE, r.nestChance)
                    columnRSCM(LOGS, r.logs)
                    column(CRAFTING_LEVEL, r.craftingLevel)
                    column(CRAFTING_XP, r.craftingXp)
                }
            }
        }
}

object BirdHouseSeedsTable {
    const val SEEDS = 0
    const val HIGH_LEVEL_SEEDS = 1

    private val SEED_NAMES =
        listOf(
            "barley_seed", "jute_seed", "hammerstone_hop_seed", "asgarnian_hop_seed",
            "yanillian_hop_seed", "krandorian_hop_seed", "wildblood_hop_seed", "potato_seed",
            "onion_seed", "cabbage_seed", "tomato_seed", "sweetcorn_seed", "strawberry_seed",
            "watermelon_seed", "snape_grass_seed", "marigold_seed", "rosemary_seed",
            "nasturtium_seed", "woad_seed", "limpwurt_seed", "white_lily_seed",
            "redberry_bush_seed", "cadavaberry_bush_seed", "dwellberry_bush_seed",
            "jangerberry_bush_seed", "whiteberry_bush_seed", "poisonivy_bush_seed",
            "guam_seed", "marrentill_seed", "tarromin_seed", "harralander_seed",
            "ranarr_seed", "toadflax_seed", "irit_seed", "avantoe_seed", "kwuarm_seed",
            "snapdragon_seed", "cadantine_seed", "lantadyme_seed", "dwarf_weed_seed",
            "torstol_seed",
        )

    private val HIGH_LEVEL_SEED_NAMES =
        listOf(
            "wildblood_hop_seed", "ranarr_seed", "toadflax_seed", "irit_seed", "avantoe_seed",
            "kwuarm_seed", "snapdragon_seed", "cadantine_seed", "lantadyme_seed", "dwarf_weed_seed",
            "torstol_seed",
        )

    fun birdHouseSeeds() =
        dbTable("dbtable.birdhouse_seeds", serverOnly = true) {
            column("seeds", SEEDS, VarType.OBJ)
            column("high_level_seeds", HIGH_LEVEL_SEEDS, VarType.OBJ)

            row("dbrow.birdhouse_seeds") {
                columnRSCM(SEEDS, SEED_NAMES.map { "obj.$it" })
                columnRSCM(HIGH_LEVEL_SEEDS, HIGH_LEVEL_SEED_NAMES.map { "obj.$it" })
            }
        }
}
