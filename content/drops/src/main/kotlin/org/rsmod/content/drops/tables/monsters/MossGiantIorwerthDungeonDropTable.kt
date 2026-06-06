package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val mossGiantIorwerthDungeonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Moss Giant (Iorwerth Dungeon) Drops",
    npcs = npcs("npc.prif_mossgiant"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Moss Giant (Iorwerth Dungeon) Drops")
        5 weight "obj.mithril_sq_shield" count 1
        2 weight "obj.mithril_med_helm" count 1
        2 weight "obj.adamant_spear" count 1
        2 weight "obj.adamant_sword" count 1
        2 weight "obj.battlestaff" count 1
        1 weight "obj.mithril_kiteshield" count 1
        4 weight "obj.lawrune" count 10..25
        3 weight "obj.airrune" count 40..80
        3 weight "obj.earthrune" count 40..80
        3 weight "obj.chaosrune" count 10..30
        3 weight "obj.naturerune" count 6..25
        2 weight "obj.cosmicrune" count 10..25
        1 weight "obj.mithril_arrow" count 20..30
        2 weight "obj.adamant_arrow" count 10..15
        1 weight "obj.deathrune" count 10..20
        1 weight "obj.bloodrune" count 6..10
        19 weight "obj.coins" count 60
        10 weight "obj.coins" count 100..1000
        8 weight "obj.coins" count 20
        2 weight "obj.coins" count 500..1500
        6 weight "obj.mithril_bar" count 1
        1 weight "obj.coal" count 2..6
        1 weight "obj.spinach_roll" count 1

        5 weight SharedDropTables.herb
        4 weight SharedDropTables.gem
        35 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_moss_giant_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 24 weight "obj.arceuus_corpse_giant" count 1
        1 outOf 24 weight "obj.prif_crystal_shard" count 3..5
        // Drops Need Manual (rate): The mossy key drop rate is increased to 1/40 if the player is assigned moss giants as a Slayer task.
        1 outOf 120 weight "obj.mossy_key" count 1
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5000 weight "obj.champions_challenge_giant" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
        1 outOf 190 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
    },
)
