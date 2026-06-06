package dtx.example.rs_tables

import dtx.rs.*
import dtx.example.Gender
import dtx.example.Item
import dtx.example.Player
import kotlin.random.Random

val dragonstoneTable = guaranteedSingle<Player, Item>(Item("uncut_dragonstone"))


val crystalChestMain = rsWeightedTable<Player, Item> {

    17 weight Item("nothing")

    34 weight rsGuaranteedTable {
        add(Item("spinach_roll"))
        add(Item("coins", 2_000))
    }

    12 weight rsGuaranteedTable {
        add(Item("air_rune", 50))
        add(Item("water_rune", 50))
        add(Item("earth_rune", 50))
        add(Item("fire_rune", 50))
        add(Item("body_rune", 50))
        add(Item("mind_rune", 50))
        add(Item("chaos_rune", 10))
        add(Item("death_rune", 10))
        add(Item("cosmic_rune", 10))
        add(Item("nature_rune", 10))
        add(Item("law_rune", 10))
    }

    12 weight rsGuaranteedTable {
        add(Item("cut_ruby", 2))
        add(Item("cut_diamond", 2))
    }

    12 weight Item("runite_bar", 3)

    10 weight rsGuaranteedTable {
        add(Item("coins", 750))
        add {
            val pickedHalf = if (Random.nextBoolean()) { "loop" } else { "tooth" }
            Item("${pickedHalf}_half_of_key")
        }
    }

    10 weight Item("noted_iron_ore", 150)

    10 weight Item("noted_coal", 100)

    8 weight rsGuaranteedTable {
        add(Item("coins", 1_000))
        add(Item("raw_swordfish", 5))
    }

    2 weight Item("adamant_sq_shield", 1)

    1 weight {
        shouldInclude { player, args -> player.gender == Gender.PlatelegEnjoyer }
        result(Item("rune_platelegs"))
    }

    1 weight {
        shouldInclude { player, args -> player.gender == Gender.PlateskirtEnthusiast }
        result(Item("rune_plateskirt"))
    }
}

val CrystalChestTable = RSDropTable(
    "Crystal Chest",
    guaranteed = dragonstoneTable,
    mainTable = crystalChestMain
)
