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
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.wearingRingOfWealth
import org.rsmod.api.area.checker.isInWilderness
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val bloodveldDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bloodveld Drops",
    npcs = npcs("npc.godwars_bloodveld", "npc.slayer_bloodveld", "npc.slayer_bloodveld_baby", "npc.slayer_bloodveld_baby_strongholdcave", "npc.slayer_bloodveld_strongholdcave"),
    mainTable = rsPlayerWeightedTable(total = 135) {
        name("Bloodveld Drops")
        4 weight "obj.steel_axe" count 1
        4 weight "obj.steel_full_helm" count 1
        2 weight "obj.steel_scimitar" count 1
        1 weight "obj.black_armoured_boots" count 1
        1 weight "obj.mithril_sq_shield" count 1
        1 weight "obj.mithril_chainbody" count 1
        1 weight "obj.rune_med_helm" count 1
        8 weight "obj.firerune" count 60
        3 weight "obj.bloodrune" count 3
        5 weight "obj.bloodrune" count 10
        1 weight "obj.bloodrune" count 30
        7 weight "obj.coins" count 10
        29 weight "obj.coins" count 40
        30 weight "obj.coins" count 120
        10 weight "obj.coins" count 200
        1 weight "obj.coins" count 450
        10 weight "obj.bones" count 1
        7 weight "obj.big_bones" count 1
        3 weight "obj.big_bones" count 3
        2 weight "obj.gold_ore" count 1
        3 weight "obj.meat_pizza" count 1

        1 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_elite_emote_exp1" count 1 condition { player ->
            // Drops Need Manual: The elite clue scroll and casket are only dropped when completing an elite clue scroll asking you to kill a bloodveld
             true
        }
        1 outOf 1 weight "obj.trail_reward_casket_elite" count 1
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        1 outOf 35 weight "obj.arceuus_corpse_bloodveld" count 1
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        1 outOf 243 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
        1 outOf 128 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
    },
)
