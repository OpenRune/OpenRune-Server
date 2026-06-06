package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.rsPlayerPrerollTable
import org.rsmod.api.droptable.dropRollable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val shellbaneGryphonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Shellbane gryphon Drops",
    npcs = npcs("npc.gryphon_boss"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.gryphon_feather" count 7..10
    },
    preRoll = rsPlayerPrerollTable {
        1 outOf 256 weight "obj.belles_folly_tarnished" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Shellbane gryphon Drops")
        10 weight "obj.rune_claws" count 1
        8 weight "obj.rune_scimitar" count 1
        8 weight "obj.adamant_cannonball" count 35..50
        6 weight "obj.rune_cannonball" count 20..30
        14 weight "obj.raw_swordfish" count 1
        10 weight "obj.sweetcorn" count 1
        10 weight "obj.watermelon" count 1
        4 weight "obj.raw_seaturtle" count 1
        1 weight "obj.coral_elkhorn_frag" count 1
        1 weight "obj.coral_pillar_frag" count 1
        11 weight "obj.gryphon_feather" count 35..50
        10 weight "obj.shark_lure" count 3..5
        3 weight "obj.basket_empty" count 1

        6 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
        25 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_elite_emote_exp1" count 1 condition { player ->
            // Drops Need Manual: Guaranteed elite clue scroll and reward casket drops only occur when completing an elite clue scroll asking you to kill the Shellbane gryphon.
             true
        }
        1 outOf 1 weight "obj.trail_reward_casket_elite" count 1
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 2000 weight "obj.jar_of_feathers" count 1
        1 outOf 3000 weight "obj.gryphonbosspet" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
        1 outOf 190 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_elite_emote_exp1")
        }
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Potato seed [main/1/{{#expr:1/({{#var:allotseed}}*64) round 1}}]
//   - Onion seed [main/1/{{#expr:1/({{#var:allotseed}}*32) round 1}}]
//   - Cabbage seed [main/1/{{#expr:1/({{#var:allotseed}}*16) round 1}}]
//   - Tomato seed [main/1/{{#expr:1/({{#var:allotseed}}*8 + {{#var:allotseedgood}}*8) round 1}}]
//   - Sweetcorn seed [main/1/{{#expr:1/({{#var:allotseed}}*4 + {{#var:allotseedgood}}*4) round 1}}]
//   - Strawberry seed [main/1/{{#expr:1/({{#var:allotseed}}*2 + {{#var:allotseedgood}}*2) round 1}}]
//   - Watermelon seed [main/1/{{#expr:1/({{#var:allotseed}}*1 + {{#var:allotseedgood}}*1) round 1}}]
//   - Snape grass seed [main/1/{{#expr:1/({{#var:allotseed}}*1 + {{#var:allotseedgood}}*1) round 1}}]
//   - Grimy guam leaf [main/1/{{#expr:1/({{#var:herb}}*32) round 1}}]
//   - Grimy marrentill [main/1/{{#expr:1/({{#var:herb}}*24) round 1}}]
//   - Grimy tarromin [main/1/{{#expr:1/({{#var:herb}}*18) round 1}}]
//   - Grimy harralander [main/1/{{#expr:1/({{#var:herb}}*14) round 1}}]
//   - Grimy ranarr weed [main/1/{{#expr:1/({{#var:herb}}*11) round 1}}]
//   - Grimy irit leaf [main/1/{{#expr:1/({{#var:herb}}*8) round 1}}]
//   - Grimy avantoe [main/1/{{#expr:1/({{#var:herb}}*6) round 1}}]
//   - Grimy kwuarm [main/1/{{#expr:1/({{#var:herb}}*5 + {{#var:combatherb}}*5) round 1}}]
//   - Grimy cadantine [main/1/{{#expr:1/({{#var:herb}}*4 + {{#var:combatherb}}*4) round 1}}]
//   - Grimy lantadyme [main/1/{{#expr:1/({{#var:herb}}*3 + {{#var:combatherb}}*3) round 1}}]
//   - Grimy dwarf weed [main/1/{{#expr:1/({{#var:herb}}*3 + {{#var:combatherb}}*4) round 1}}]
