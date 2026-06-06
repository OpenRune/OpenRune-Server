package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val warpedTerrorbirdDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Warped Terrorbird Drops",
    npcs = npcs("npc.pog_mutated_terrorbird_1", "npc.pog_mutated_terrorbird_2", "npc.pog_mutated_terrorbird_3", "npc.pog_mutated_terrorbird_4", "npc.pog_mutated_terrorbird_5", "npc.pog_mutated_terrorbird_6", "npc.pog_mutated_terrorbird_7", "npc.pog_mutated_terrorbird_8", "npc.pog_mutated_terrorbird_boss_1", "npc.pog_mutated_terrorbird_boss_1_background_vis", "npc.pog_mutated_terrorbird_boss_2", "npc.pog_mutated_terrorbird_boss_2_background_vis", "npc.pog_mutated_terrorbird_boss_3", "npc.pog_mutated_terrorbird_boss_3_background_vis"),
    mainTable = rsPlayerWeightedTable(total = 64) {
        name("Warped Terrorbird Drops")
        2 weight "obj.adamnt_warhammer" count 1
        2 weight "obj.adamant_platebody" count 1
        1 weight "obj.rune_battleaxe" count 1
        1 weight "obj.rune_kiteshield" count 1
        1 weight "obj.rune_warhammer" count 1
        6 weight "obj.airrune" count 80..120
        6 weight "obj.earthrune" count 80..100
        3 weight "obj.deathrune" count 15..20
        3 weight "obj.lawrune" count 15..20
        3 weight "obj.soulrune" count 10..15
        8 weight "obj.coins" count 600..800
        2 weight "obj.cert_adamantite_ore" count 3..5
        3 weight "obj.weapon_poison" count 1
        3 weight "obj.xbows_bolt_tips_diamond" count 24..32
        4 weight "obj.feather" count 100..200
        5 weight "obj.cert_raw_shark" count 3..7
        5 weight "obj.swamp_tar" count 40..60
        3 weight "obj.chocolate_bomb" count 2..3
        3 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_terrorbird_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 320 weight "obj.warped_sceptre_uncharged" count 1
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Clue scroll (hard) [tertiary/Rare]
