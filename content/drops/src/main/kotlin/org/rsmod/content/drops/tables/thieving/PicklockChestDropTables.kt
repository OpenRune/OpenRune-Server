package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.RSWeightedTable
import dtx.rs.locs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.content.drops.tables.shared.uncommonSeedDropTable
import org.rsmod.game.entity.Player

internal val thievingChestGemTable: RSWeightedTable<Player, DropRollItem> =
    rsPlayerWeightedTable(total = 10) {
        name("Chest gems (Thieving)")
        4 weight "obj.uncut_sapphire" count 1
        3 weight "obj.uncut_emerald" count 1
        2 weight "obj.uncut_ruby" count 1
        1 weight "obj.uncut_diamond" count 1
    }

@field:RegisterDropTable
@JvmField
public val isleOfSoulsChestDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Chest, Isle of Souls Dungeon (Thieving)",
    locs = locs("loc.sw_dungeon_thieving_chest_closed"),
    mainTable = rsPlayerWeightedTable(total = 200) {
        name("Chest, Isle of Souls Dungeon (Thieving)")
        20 weight "obj.coins" count 90..190
        20 weight "obj.feather" count 30..80
        6 weight "obj.chocolate_bar" count 1
        6 weight "obj.hunting_ojibway_bird_snare" count 1..2
        6 weight "obj.hunting_box_trap" count 1..2
        20 weight "obj.unidentified_guam" count 1..2
        12 weight "obj.unidentified_lantadyme" count 1
        10 weight "obj.unidentified_ranarr" count 1
        8 weight "obj.uncut_sapphire" count 1..2
        8 weight "obj.uncut_emerald" count 1..2
        6 weight "obj.uncut_ruby" count 1..2
        4 weight "obj.uncut_diamond" count 1..2
        6 weight "obj.mithril_pickaxe" count 1
        6 weight "obj.mithril_axe" count 1
        4 weight "obj.adamant_scimitar" count 1
        15 weight "obj.mindrune" count 1..10
        20 weight "obj.naturerune" count 1..2
        20 weight "obj.deathrune" count 1..2
        2 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            player.clueScrollTransformObj("obj.trail_medium_emote_exp1")
        }
        1 weight "obj.sw_dungeon_chest_key" count 1
    },
)

@field:RegisterDropTable
@JvmField
public val aldarinVillasChestDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Chest, Aldarin Villas (Thieving)",
    locs = locs("loc.aldarin_thieving_chest"),
    mainTable = rsPlayerWeightedTable(total = 90) {
        name("Chest, Aldarin Villas pre-roll (Thieving)")
        1 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            player.clueScrollTransformObj("obj.trail_medium_emote_exp1")
        }
        89 weight rsPlayerWeightedTable(total = 60) {
            name("Chest, Aldarin Villas (Thieving)")
            20 weight "obj.coins" count 100..200
            10 weight "obj.coins" count 250..500
            4 weight "obj.coins" count 600..1200
            3 weight "obj.silver_bar" count 1
            3 weight "obj.gold_bar" count 1
            8 weight thievingChestGemTable
            4 weight "obj.spinach_roll" count 1
            2 weight "obj.blessed_bone_shard" count 10..20
            2 weight "obj.eclipse_wine" count 1
            4 weight uncommonSeedDropTable
        }
    },
)

@field:RegisterDropTable
@JvmField
public val stoneChestDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Stone chest (Thieving)",
    locs = locs("loc.molch_temple_chest_theiving"),
    mainTable = rsPlayerWeightedTable(total = 300) {
        name("Stone chest (Thieving)")
        100 weight "obj.coins" count 20..260
        85 weight "obj.lizardman_fang" count 1
        66 weight "obj.xeric_fabric" count 1
        12 weight "obj.uncut_sapphire" count 1
        8 weight "obj.uncut_ruby" count 1
        15 weight uncommonSeedDropTable
        10 weight rsPlayerWeightedTable(total = 37) {
            name("Stone chest bolt tips (Thieving)")
            6 weight "obj.opal_bolttips" count 4..12
            2 weight "obj.xbows_bolt_tips_jade" count 4..12
            6 weight "obj.pearl_bolttips" count 4..12
            2 weight "obj.xbows_bolt_tips_redtopaz" count 4..12
            2 weight "obj.xbows_bolt_tips_sapphire" count 4..12
            3 weight "obj.xbows_bolt_tips_emerald" count 4..12
            4 weight "obj.xbows_bolt_tips_ruby" count 4..12
            6 weight "obj.xbows_bolt_tips_diamond" count 4..12
            4 weight "obj.xbows_bolt_tips_dragonstone" count 4..12
            2 weight "obj.xbows_bolt_tips_onyx" count 4..12
        }
        3 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            player.clueScrollTransformObj("obj.trail_medium_emote_exp1")
        }
        1 weight "obj.xeric_talisman_empty" count 1
    },
)
