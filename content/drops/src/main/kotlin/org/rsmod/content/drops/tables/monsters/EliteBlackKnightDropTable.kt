package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val eliteBlackKnightDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Elite Black Knight Drops",
    npcs = npcs("npc.elite_black_knight_1", "npc.elite_black_knight_1_cutscene", "npc.elite_black_knight_2", "npc.elite_black_knight_2_cutscene"),
    mainTable = rsPlayerWeightedTable(total = 122) {
        name("Elite Black Knight Drops")
        7 weight "obj.cert_opal_necklace" count 3..5
        7 weight "obj.cert_sapphire_necklace" count 6..10
        7 weight "obj.cert_emerald_ring" count 6..10
        4 weight "obj.cert_jade_ring" count 1..3
        4 weight "obj.cert_jade_necklace" count 2..5
        3 weight "obj.cert_ruby" count 3..5
        3 weight "obj.cert_sapphire" count 8..12
        3 weight "obj.cert_emerald" count 5..8
        3 weight "obj.cert_ruby_ring" count 3..5
        3 weight "obj.cert_diamond_ring" count 2..5
        2 weight "obj.cert_unstrung_topaz_amulet" count 2..3
        1 weight "obj.dragonstone" count 1
        1 weight "obj.dragonstone_ring" count 1
        14 weight "obj.1doseprayerrestore" count 1..3
        4 weight "obj.fish_pie" count 1
        4 weight "obj.meat_pie" count 1
        3 weight "obj.3dose1magic" count 1
        3 weight "obj.1doserangerspotion" count 1..2
        3 weight "obj.2dose2strength" count 1..2
        3 weight "obj.2dose2attack" count 1..2
        2 weight "obj.lobster" count 2..4
        1 weight "obj.cooked_chompy" count 28
        19 weight "obj.coins" count 545..2500
        4 weight "obj.xbows_bolt_tips_sapphire" count 45..60
        4 weight "obj.opal_bolttips" count 30..40
        4 weight "obj.xbows_bolt_tips_emerald" count 30..45
        3 weight "obj.cert_silver_bar" count 20..30
        2 weight "obj.xbows_bolt_tips_dragonstone" count 25..75
        1 weight "obj.xbows_bolt_tips_onyx" count 15..60
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Elite black full helm [main/Varies]
//   - Elite black platebody [main/Varies]
//   - Elite black platelegs [main/Varies]
//   - Clue scroll (hard) [tertiary/Rare]
