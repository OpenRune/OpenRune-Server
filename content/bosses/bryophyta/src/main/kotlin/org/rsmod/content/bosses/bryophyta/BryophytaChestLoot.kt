package org.rsmod.content.bosses.bryophyta

import org.rsmod.api.random.GameRandom

internal data class ChestDrop(val obj: String, val count: Int)

internal object BryophytaChestLoot {
    private enum class Subtable {
        Herbs,
        Seeds,
    }

    private class Entry(
        val weight: Int,
        val drops: List<ChestDrop> = emptyList(),
        val subtable: Subtable? = null,
    )

    private val MEMBERS_MAIN =
        listOf(
            Entry(6, listOf(ChestDrop("obj.rune_longsword", 1))),
            Entry(6, listOf(ChestDrop("obj.rune_med_helm", 2))),
            Entry(6, listOf(ChestDrop("obj.rune_chainbody", 1))),
            Entry(6, listOf(ChestDrop("obj.rune_plateskirt", 1))),
            Entry(6, listOf(ChestDrop("obj.rune_platelegs", 1))),
            Entry(6, listOf(ChestDrop("obj.rune_sq_shield", 1))),
            Entry(5, listOf(ChestDrop("obj.rune_sword", 2))),
            Entry(3, listOf(ChestDrop("obj.cert_adamant_platebody", 5))),
            Entry(3, listOf(ChestDrop("obj.cert_battlestaff", 3))),
            Entry(1, listOf(ChestDrop("obj.adamant_kiteshield", 3))),
            Entry(8, listOf(ChestDrop("obj.naturerune", 100))),
            Entry(6, listOf(ChestDrop("obj.cosmicrune", 100))),
            Entry(6, listOf(ChestDrop("obj.lawrune", 100))),
            Entry(5, listOf(ChestDrop("obj.deathrune", 100))),
            Entry(6, listOf(ChestDrop("obj.chaosrune", 200))),
            Entry(5, listOf(ChestDrop("obj.chaosrune", 100))),
            Entry(1, listOf(ChestDrop("obj.bloodrune", 100))),
            Entry(1, listOf(ChestDrop("obj.adamant_arrow", 100))),
            Entry(5, subtable = Subtable.Herbs),
            Entry(6, subtable = Subtable.Seeds),
            Entry(6, listOf(ChestDrop("obj.runite_bar", 2))),
            Entry(
                4,
                listOf(
                    ChestDrop("obj.cert_uncut_ruby", 5),
                    ChestDrop("obj.cert_uncut_diamond", 5),
                ),
            ),
            Entry(3, listOf(ChestDrop("obj.cert_steel_bar", 25))),
            Entry(5, listOf(ChestDrop("obj.coins", 10_000))),
            Entry(2, listOf(ChestDrop("obj.coins", 8_000))),
            Entry(1, listOf(ChestDrop("obj.gb_moss_essence", 1))),
        )

    private val FREE_MAIN =
        listOf(
            Entry(12, listOf(ChestDrop("obj.rune_platelegs", 1))),
            Entry(6, listOf(ChestDrop("obj.rune_longsword", 1))),
            Entry(5, listOf(ChestDrop("obj.rune_sword", 2))),
            Entry(6, listOf(ChestDrop("obj.rune_med_helm", 2))),
            Entry(3, listOf(ChestDrop("obj.rune_med_helm", 1))),
            Entry(6, listOf(ChestDrop("obj.rune_chainbody", 1))),
            Entry(6, listOf(ChestDrop("obj.rune_plateskirt", 1))),
            Entry(6, listOf(ChestDrop("obj.rune_sq_shield", 1))),
            Entry(1, listOf(ChestDrop("obj.adamant_kiteshield", 3))),
            Entry(3, listOf(ChestDrop("obj.cert_adamant_platebody", 5))),
            Entry(6, listOf(ChestDrop("obj.cosmicrune", 100))),
            Entry(6, listOf(ChestDrop("obj.lawrune", 100))),
            Entry(8, listOf(ChestDrop("obj.naturerune", 100))),
            Entry(6, listOf(ChestDrop("obj.deathrune", 100))),
            Entry(6, listOf(ChestDrop("obj.chaosrune", 200))),
            Entry(5, listOf(ChestDrop("obj.chaosrune", 100))),
            Entry(1, listOf(ChestDrop("obj.adamant_arrow", 100))),
            Entry(6, listOf(ChestDrop("obj.runite_bar", 2))),
            Entry(
                4,
                listOf(
                    ChestDrop("obj.cert_uncut_ruby", 5),
                    ChestDrop("obj.cert_uncut_diamond", 5),
                ),
            ),
            Entry(3, listOf(ChestDrop("obj.cert_steel_bar", 25))),
            Entry(5, listOf(ChestDrop("obj.coins", 10_000))),
            Entry(2, listOf(ChestDrop("obj.coins", 8_000))),
            Entry(5, listOf(ChestDrop("obj.cert_strength4", 15))),
            Entry(1, listOf(ChestDrop("obj.gb_moss_essence", 1))),
        )

    private val HERBS =
        listOf(
            32 to "obj.unidentified_guam",
            24 to "obj.unidentified_marentill",
            18 to "obj.unidentified_tarromin",
            14 to "obj.unidentified_harralander",
            11 to "obj.unidentified_ranarr",
            8 to "obj.unidentified_irit",
            6 to "obj.unidentified_avantoe",
            5 to "obj.unidentified_kwuarm",
            4 to "obj.unidentified_cadantine",
            3 to "obj.unidentified_lantadyme",
            3 to "obj.unidentified_dwarf_weed",
        )

    private val SEEDS =
        listOf(
            137 to "obj.limpwurt_seed",
            131 to "obj.strawberry_seed",
            125 to "obj.marrentill_seed",
            92 to "obj.jangerberry_bush_seed",
            85 to "obj.tarromin_seed",
            83 to "obj.wildblood_hop_seed",
            63 to "obj.watermelon_seed",
            56 to "obj.harralander_seed",
            40 to "obj.snape_grass_seed",
            39 to "obj.ranarr_seed",
            34 to "obj.whiteberry_bush_seed",
            29 to "obj.mushroom_seed",
            27 to "obj.toadflax_seed",
            18 to "obj.belladonna_seed",
            18 to "obj.irit_seed",
            13 to "obj.poisonivy_bush_seed",
            12 to "obj.avantoe_seed",
            12 to "obj.cactus_seed",
            9 to "obj.kwuarm_seed",
            8 to "obj.potato_cactus_seed",
            5 to "obj.snapdragon_seed",
            4 to "obj.cadantine_seed",
            3 to "obj.lantadyme_seed",
            2 to "obj.dwarf_weed_seed",
            1 to "obj.torstol_seed",
        )

    internal fun mainWeight(members: Boolean): Int =
        (if (members) MEMBERS_MAIN else FREE_MAIN).sumOf { it.weight }

    fun roll(members: Boolean, onMossGiantTask: Boolean, random: GameRandom): List<ChestDrop> {
        val drops = mutableListOf(ChestDrop("obj.trail_clue_beginner", 1))
        val table = if (members) MEMBERS_MAIN else FREE_MAIN
        val entry = pick(table, random)
        if (entry.subtable != null) {
            val subtable = if (entry.subtable == Subtable.Herbs) HERBS else SEEDS
            val (_, obj) = pick(subtable, random)
            drops += ChestDrop(obj, 1)
        } else {
            drops += entry.drops
        }
        val keyDenominator = if (onMossGiantTask) 8 else 16
        if (random.randomBoolean(keyDenominator)) {
            drops += ChestDrop("obj.mossy_key", 1)
        }
        return drops
    }

    private fun pick(table: List<Entry>, random: GameRandom): Entry {
        var roll = random.of(table.sumOf { it.weight })
        for (entry in table) {
            if (roll < entry.weight) return entry
            roll -= entry.weight
        }
        error("Bryophyta chest weight out of range")
    }

    private fun pick(table: List<Pair<Int, String>>, random: GameRandom): Pair<Int, String> {
        var roll = random.of(table.sumOf { it.first })
        for (entry in table) {
            if (roll < entry.first) return entry
            roll -= entry.first
        }
        error("Bryophyta chest subtable weight out of range")
    }
}
