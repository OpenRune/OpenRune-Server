package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val scurriusDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Scurrius Drops",
    npcs = npcs("npc.rat_boss_instance", "npc.rat_boss_normal"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.big_bones" count 1
        "obj.raw_rat_meat" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 100) {
        name("Scurrius Drops")
        6 weight "obj.adamant_platebody" count 1
        6 weight "obj.rune_med_helm" count 1
        6 weight "obj.rune_full_helm" count 1
        6 weight "obj.rune_sq_shield" count 1
        6 weight "obj.rune_chainbody" count 1
        6 weight "obj.rune_battleaxe" count 1
        6 weight "obj.adamant_arrow" count 20..50
        6 weight "obj.rune_arrow" count 20..50
        6 weight "obj.chaosrune" count 70..125
        3 weight "obj.deathrune" count 40..90
        3 weight "obj.lawrune" count 10..30
        6 weight "obj.trout" count 1..3
        6 weight "obj.tuna" count 1..3
        3 weight "obj.shark" count 1
        6 weight "obj.lobster" count 1..3
        3 weight "obj.1doserangerspotion" count 1
        3 weight "obj.1dose1strength" count 1
        1 weight "obj.cheese" count 1
        6 weight "obj.4doseprayerrestore" count 1
        6 weight "obj.coins" count 1000..9000
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 25 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_medium_emote_exp1")
        }
        1 outOf 33 weight "obj.rat_boss_spine" count 1
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
        1 outOf 3000 weight "obj.scurriuspet" count 1
    },
)
