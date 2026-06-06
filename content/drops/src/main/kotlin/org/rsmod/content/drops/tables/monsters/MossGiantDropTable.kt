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
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val mossGiantDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Moss giant Drops",
    npcs = npcs("npc.crypt_of_tonali_mossgiant", "npc.kourend_mossgiant", "npc.lunar_mossgiant", "npc.lunar_mossgiant2", "npc.mossgiant", "npc.mossgiant2", "npc.mossgiant3", "npc.mossgiant4", "npc.tlati_mossgiant01", "npc.tlati_mossgiant02", "npc.tlati_mossgiant03", "npc.tlati_mossgiant04"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Moss giant Drops")
        5 weight "obj.black_sq_shield" count 1
        2 weight "obj.magic_staff" count 1
        2 weight "obj.steel_med_helm" count 1
        2 weight "obj.mithril_sword" count 1
        2 weight "obj.mithril_spear" count 1
        1 weight "obj.steel_kiteshield" count 1
        4 weight "obj.lawrune" count 3
        3 weight "obj.airrune" count 18
        3 weight "obj.earthrune" count 27
        3 weight "obj.chaosrune" count 7
        3 weight "obj.naturerune" count 6
        2 weight "obj.cosmicrune" count 3
        2 weight "obj.iron_arrow" count 15
        1 weight "obj.steel_arrow" count 30
        1 weight "obj.deathrune" count 3
        1 weight "obj.bloodrune" count 1
        19 weight "obj.coins" count 37
        11 weight "obj.coins" count 2
        10 weight "obj.coins" count 119
        2 weight "obj.coins" count 300
        6 weight "obj.steel_bar" count 1
        1 weight "obj.coal" count 1
        1 weight "obj.spinach_roll" count 1

        5 weight SharedDropTables.herb
        4 weight SharedDropTables.gem
        32 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        1 outOf 4 weight "obj.rag_moss_giant_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 24 weight "obj.arceuus_corpse_giant" count 1
        1 outOf 150 weight "obj.mossy_key" count 1
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5000 weight "obj.champions_challenge_giant" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
        1 outOf 45 weight "obj.trail_clue_beginner" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_beginner")
        }
    },
)
