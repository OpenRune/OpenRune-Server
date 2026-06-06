package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.shouldDropLootingBag
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.wearingRingOfWealth
import org.rsmod.api.area.checker.isInWilderness
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val spiritualMageDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Spiritual mage Drops",
    npcs = npcs("npc.godwars_spiritual_armadyl_mage", "npc.godwars_spiritual_bandos_mage", "npc.godwars_spiritual_saradomin_mage", "npc.godwars_spiritual_zamorak_mage", "npc.nex_prison_mage"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Spiritual mage Drops")
        9 weight "obj.airrune" count 30
        8 weight "obj.waterrune" count 15
        7 weight "obj.waterrune" count 50
        2 weight "obj.airrune" count 50
        2 weight "obj.firerune" count 65
        11 weight "obj.dustrune" count 15
        7 weight "obj.mudrune" count 20
        6 weight "obj.mistrune" count 20
        18 weight "obj.chaosrune" count 10
        9 weight "obj.astralrune" count 15
        8 weight "obj.deathrune" count 15
        7 weight "obj.naturerune" count 25
        4 weight "obj.mindrune" count 50
        4 weight "obj.naturerune" count 18
        3 weight "obj.bodyrune" count 35
        2 weight "obj.lawrune" count 15
        2 weight "obj.bodyrune" count 25
        2 weight "obj.bloodrune" count 10
        1 weight "obj.chaosrune" count 25
        6 weight "obj.cert_blankrune_high" count 90
        5 weight "obj.cert_3doseantipoison" count 5
        1 weight "obj.dragon_boots" count 1

        2 weight SharedDropTables.gem
        2 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_clue_master" count 1 condition { player ->
            // Drops Need Manual: Master clue scrolls and reward caskets are only dropped when completing a master clue scroll asking you to kill a spiritual mage.
             true
        }
        1 outOf 1 weight "obj.trail_reward_casket_master" count 1
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        1 outOf 121 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
        1 outOf 64 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
    },
)
