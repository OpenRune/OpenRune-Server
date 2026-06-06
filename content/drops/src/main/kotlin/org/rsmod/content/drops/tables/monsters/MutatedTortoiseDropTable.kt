package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.shouldDropBrimstoneKey
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val mutatedTortoiseDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Mutated Tortoise Drops",
    npcs = npcs("npc.superior_warped_tortoise"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.konar_key" count 1 killCondition {
            player, npc, areaChecker -> player.shouldDropBrimstoneKey(npc, areaChecker)
        }
    },
    mainTable = rsPlayerWeightedTable(total = 64) {
        name("Mutated Tortoise Drops")
        2 weight "obj.adamant_axe" count 1
        2 weight "obj.adamant_platebody" count 1
        1 weight "obj.rune_pickaxe" count 1
        1 weight "obj.rune_kiteshield" count 1
        1 weight "obj.rune_warhammer" count 1
        6 weight "obj.earthrune" count 80..100
        5 weight "obj.mudrune" count 30..50
        3 weight "obj.deathrune" count 15..20
        10 weight "obj.coins" count 600..800
        2 weight "obj.cert_adamantite_ore" count 3..5
        6 weight "obj.swamp_tar" count 40..60
        3 weight "obj.cert_coal" count 6..12
        3 weight "obj.weapon_poison" count 1
        6 weight "obj.cert_cabbage" count 20..40
        3 weight "obj.pineapple" count 1
        3 weight "obj.tangled_toads_legs" count 2..3
        2 weight "obj.battle_tortoise_shell" count 1..3
        2 weight "obj.dorgesh_tortoise_shell" count 1..3
        3 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
        1 outOf 320 weight "obj.warped_sceptre_uncharged" count 1
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Clue scroll (hard) [tertiary/Rare]
