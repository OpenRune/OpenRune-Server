package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val ogressShamanDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Ogress Shaman Drops",
    npcs = npcs("npc.ogress_shaman1", "npc.ogress_shaman2"),
    mainTable = rsPlayerWeightedTable(total = 116) {
        name("Ogress Shaman Drops")
        7 weight "obj.mithril_kiteshield" count 1
        7 weight "obj.mithril_arrow" count 5..15
        7 weight "obj.chaosrune" count 15..30
        7 weight "obj.lawrune" count 8..15
        7 weight "obj.naturerune" count 8..15
        6 weight "obj.deathrune" count 8..15
        5 weight "obj.airrune" count 10..20
        5 weight "obj.cosmicrune" count 10..15
        5 weight "obj.earthrune" count 10..20
        5 weight "obj.firerune" count 10..20
        5 weight "obj.mindrune" count 10..20
        5 weight "obj.waterrune" count 10..20
        5 weight "obj.steel_arrow" count 10..30
        5 weight "obj.iron_arrow" count 20..40
        5 weight "obj.limpwurt_root" count 1
        4 weight "obj.uncut_diamond" count 1
        4 weight "obj.uncut_emerald" count 1
        4 weight "obj.uncut_ruby" count 1
        4 weight "obj.uncut_sapphire" count 1
        9 weight "obj.coins" count 500..1000
        5 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_ogre_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 20 weight "obj.salmon" count 1..3
        1 outOf 30 weight "obj.arceuus_corpse_ogre" count 1
        1 outOf 40 weight "obj.rune_med_helm" count 1
        1 outOf 100 weight "obj.rune_full_helm" count 1
        1 outOf 100 weight "obj.rune_battleaxe" count 1
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 1200 weight "obj.ogre_helmet" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
    },
)
