package org.rsmod.content.skills.thieving

import org.rsmod.api.random.GameRandom

internal data class Loot(val obj: String, val min: Int = 1, val max: Int = min)

internal class LootTable(private val entries: List<Pair<Int, Loot>>) {
    private val total = entries.sumOf { it.first }

    fun roll(random: GameRandom): Loot {
        var pick = random.of(0, total - 1)
        for ((weight, loot) in entries) {
            if (pick < weight) return loot
            pick -= weight
        }
        return entries.last().second
    }
}

internal class Stall(
    val loc: String,
    val emptyLoc: String?,
    val level: Int,
    val xp: Double,
    val restockTicks: Int,
    val owners: List<String>,
    val guards: List<String>,
    val attemptMessage: String,
    val loot: LootTable,
)

internal class CoinPouch(val obj: String, val coins: Int)

internal class Pickpocket(
    val npcs: List<String>,
    val level: Int,
    val xp: Double,
    val lowChance: Int,
    val highChance: Int,
    val stunDamage: Int,
    val caughtShout: String,
    val loot: LootTable? = null,
    val pouch: CoinPouch? = null,
    val lowercaseName: Boolean = false,
)

internal object ThievingTables {
    private val seedStallLoot =
        LootTable(
            listOf(
                120 to Loot("obj.hammerstone_hop_seed"),
                119 to Loot("obj.potato_seed"),
                119 to Loot("obj.marigold_seed"),
                118 to Loot("obj.barley_seed"),
                89 to Loot("obj.onion_seed"),
                83 to Loot("obj.asgarnian_hop_seed"),
                71 to Loot("obj.cabbage_seed"),
                47 to Loot("obj.yanillian_hop_seed"),
                36 to Loot("obj.rosemary_seed"),
                35 to Loot("obj.nasturtium_seed"),
                35 to Loot("obj.tomato_seed"),
                35 to Loot("obj.jute_seed"),
                30 to Loot("obj.sweetcorn_seed"),
                24 to Loot("obj.krandorian_hop_seed"),
                18 to Loot("obj.strawberry_seed"),
                12 to Loot("obj.wildblood_hop_seed"),
                9 to Loot("obj.watermelon_seed"),
            )
        )

    private val wineStallLoot =
        LootTable(
            listOf(
                39 to Loot("obj.jug_empty"),
                20 to Loot("obj.jug_water"),
                17 to Loot("obj.grapes"),
                13 to Loot("obj.jug_wine"),
                11 to Loot("obj.rag_bottle_wine"),
            )
        )

    private val draynorGuards = listOf("npc.farming_market_guard")

    val stalls: List<Stall> =
        listOf(
            Stall(
                loc = "loc.seed_stall",
                emptyLoc = null,
                level = 27,
                xp = 10.0,
                restockTicks = 5,
                owners = listOf("npc.seed_merchant"),
                guards = draynorGuards,
                attemptMessage = "You attempt to steal some seeds from the seed merchant's stall.",
                loot = seedStallLoot,
            ),
            Stall(
                loc = "loc.rag_market_stall",
                emptyLoc = "loc.rag_market_stall_empty",
                level = 22,
                xp = 27.0,
                restockTicks = 8,
                owners = listOf("npc.rag_wine_merchant"),
                guards = draynorGuards,
                attemptMessage = "You attempt to steal something from the wine merchant's stall.",
                loot = wineStallLoot,
            ),
        )

    private val masterFarmerLoot =
        LootTable(
            listOf(
                17700 to Loot("obj.potato_seed", 1, 4),
                13280 to Loot("obj.onion_seed", 1, 3),
                6944 to Loot("obj.cabbage_seed", 1, 3),
                6369 to Loot("obj.tomato_seed", 1, 2),
                2212 to Loot("obj.sweetcorn_seed", 1, 2),
                1106 to Loot("obj.strawberry_seed"),
                529 to Loot("obj.watermelon_seed"),
                385 to Loot("obj.snape_grass_seed"),
                5556 to Loot("obj.barley_seed", 1, 12),
                5556 to Loot("obj.hammerstone_hop_seed", 1, 9),
                4184 to Loot("obj.asgarnian_hop_seed", 1, 6),
                4149 to Loot("obj.jute_seed", 1, 9),
                2770 to Loot("obj.yanillian_hop_seed", 1, 6),
                1385 to Loot("obj.krandorian_hop_seed", 1, 6),
                704 to Loot("obj.wildblood_hop_seed", 1, 3),
                4587 to Loot("obj.marigold_seed"),
                3040 to Loot("obj.nasturtium_seed"),
                1965 to Loot("obj.rosemary_seed"),
                1451 to Loot("obj.woad_seed"),
                1159 to Loot("obj.limpwurt_seed"),
                3876 to Loot("obj.redberry_bush_seed"),
                2717 to Loot("obj.cadavaberry_bush_seed"),
                1942 to Loot("obj.dwellberry_bush_seed"),
                775 to Loot("obj.jangerberry_bush_seed"),
                282 to Loot("obj.whiteberry_bush_seed"),
                107 to Loot("obj.poisonivy_bush_seed"),
                203 to Loot("obj.mushroom_seed"),
                122 to Loot("obj.belladonna_seed"),
                81 to Loot("obj.cactus_seed"),
                53 to Loot("obj.seaweed_seed"),
                41 to Loot("obj.potato_cactus_seed"),
                1713 to Loot("obj.guam_seed"),
                1046 to Loot("obj.marrentill_seed"),
                714 to Loot("obj.tarromin_seed"),
                485 to Loot("obj.harralander_seed"),
                372 to Loot("obj.ranarr_seed"),
                226 to Loot("obj.toadflax_seed"),
                154 to Loot("obj.irit_seed"),
                106 to Loot("obj.avantoe_seed"),
                72 to Loot("obj.kwuarm_seed"),
                54 to Loot("obj.snapdragon_seed"),
                34 to Loot("obj.cadantine_seed"),
                24 to Loot("obj.lantadyme_seed"),
                14 to Loot("obj.dwarf_weed_seed"),
                11 to Loot("obj.torstol_seed"),
            )
        )

    val pickpockets: List<Pickpocket> =
        listOf(
            Pickpocket(
                npcs =
                    listOf(
                        "npc.man",
                        "npc.man2",
                        "npc.man3",
                        "npc.woman",
                        "npc.woman2",
                        "npc.woman3",
                    ),
                level = 1,
                xp = 8.0,
                lowChance = 180,
                highChance = 240,
                stunDamage = 1,
                caughtShout = "What do you think you're doing?",
                pouch = CoinPouch("obj.pickpocket_coin_pouch_citizen", coins = 3),
                lowercaseName = true,
            ),
            Pickpocket(
                npcs = listOf("npc.master_farmer_1", "npc.martin_the_master_farmer"),
                level = 38,
                xp = 43.0,
                lowChance = 90,
                highChance = 240,
                stunDamage = 3,
                caughtShout = "Cor blimey mate, what are ye doing in me pockets?",
                loot = masterFarmerLoot,
            )
        )
}
