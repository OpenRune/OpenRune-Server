package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.RSWeightedTable
import dtx.rs.locs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.game.entity.Player

private val SAILING_FRAG_TABLE: RSWeightedTable<Player, DropRollItem> =
    rsPlayerWeightedTable(total = 10) {
        name("Sailing frag drop table")
        6 weight "obj.coral_elkhorn_frag" count 1
        3 weight "obj.coral_pillar_frag" count 1
        1 weight "obj.coral_umbral_frag" count 1
    }

private val RUSTY_CHEST_TABLE: RSWeightedTable<Player, DropRollItem> =
    rsPlayerWeightedTable(total = 60) {
        name("Rusty chest (Thieving)")
        3 weight "obj.steel_dagger" count 1
        3 weight "obj.steel_scimitar" count 1
        3 weight "obj.bronze_cannonball" count 5..10
        2 weight "obj.iron_cannonball" count 5..10
        1 weight "obj.mcannonball" count 5..10
        8 weight thievingChestGemTable
        3 weight "obj.coral_elkhorn_frag" count 2
        1 weight SAILING_FRAG_TABLE
        20 weight "obj.coins" count 100..200
        4 weight "obj.silver_bar" count 1
        4 weight "obj.gold_bar" count 1
        3 weight "obj.mindrune" count 5..10
        2 weight "obj.sapphire_necklace" count 1
        2 weight "obj.emerald_necklace" count 1
        1 weight nothing()
    }

private val TARNISHED_CHEST_TABLE: RSWeightedTable<Player, DropRollItem> =
    rsPlayerWeightedTable(total = 60) {
        name("Tarnished chest (Thieving)")
        3 weight "obj.mithril_dagger" count 1
        3 weight "obj.mithril_scimitar" count 1
        3 weight "obj.iron_cannonball" count 5..10
        2 weight "obj.mcannonball" count 5..10
        1 weight "obj.mithril_cannonball" count 5..10
        8 weight thievingChestGemTable
        3 weight "obj.coral_pillar_frag" count 2
        1 weight SAILING_FRAG_TABLE
        20 weight "obj.coins" count 200..400
        4 weight "obj.silver_bar" count 1
        4 weight "obj.gold_bar" count 1
        3 weight "obj.chaosrune" count 5..10
        2 weight "obj.emerald_necklace" count 1
        2 weight "obj.ruby_necklace" count 1
        1 weight nothing()
    }

private val REINFORCED_CHEST_TABLE: RSWeightedTable<Player, DropRollItem> =
    rsPlayerWeightedTable(total = 60) {
        name("Reinforced chest (Thieving)")
        3 weight "obj.adamant_dagger" count 1
        3 weight "obj.adamant_scimitar" count 1
        3 weight "obj.mcannonball" count 5..10
        2 weight "obj.mithril_cannonball" count 5..10
        1 weight "obj.adamant_cannonball" count 5..10
        8 weight thievingChestGemTable
        3 weight "obj.coral_umbral_frag" count 2
        1 weight SAILING_FRAG_TABLE
        20 weight "obj.coins" count 500..1000
        4 weight "obj.silver_bar" count 1
        4 weight "obj.gold_bar" count 1
        3 weight "obj.deathrune" count 5..10
        2 weight "obj.ruby_necklace" count 1
        2 weight "obj.diamond_necklace" count 1
        1 weight nothing()
    }

// The Dognose chest's medallion fragment pre-roll is left to the quest that needs it.
@field:RegisterDropTable
@JvmField
public val rustyChestDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Rusty chest (Thieving)",
    locs =
        locs(
            "loc.rusty_pirate_thieving_chest_dognose",
            "loc.rusty_pirate_thieving_chest_vatrachos",
        ),
    mainTable = rsPlayerWeightedTable(total = 200) {
        name("Rusty chest pre-roll (Thieving)")
        1 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            player.clueScrollTransformObj("obj.trail_medium_emote_exp1")
        }
        199 weight RUSTY_CHEST_TABLE
    },
)

@field:RegisterDropTable
@JvmField
public val tarnishedChestDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Tarnished chest (Thieving)",
    locs =
        locs(
            "loc.tarnished_pirate_thieving_chest_crown_jewel",
            "loc.tarnished_pirate_thieving_chest_shimmering_atoll",
        ),
    mainTable = rsPlayerWeightedTable(total = 150) {
        name("Tarnished chest pre-roll (Thieving)")
        1 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            player.clueScrollTransformObj("obj.trail_medium_emote_exp1")
        }
        149 weight TARNISHED_CHEST_TABLE
    },
)

@field:RegisterDropTable
@JvmField
public val reinforcedChestDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Reinforced chest (Thieving)",
    locs =
        locs(
            "loc.reinforced_pirate_thieving_chest_ynysdail",
            "loc.reinforced_pirate_thieving_chest_brittle",
        ),
    mainTable = rsPlayerWeightedTable(total = 140) {
        name("Reinforced chest pre-roll (Thieving)")
        1 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
        139 weight REINFORCED_CHEST_TABLE
    },
)
