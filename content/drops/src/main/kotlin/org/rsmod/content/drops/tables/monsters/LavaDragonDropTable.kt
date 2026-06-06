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
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val lavaDragonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Lava dragon Drops",
    npcs = npcs("npc.lava_dragon"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.dragonhide_black" count 1
        "obj.lava_scale" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 135) {
        name("Lava dragon Drops")
        6 weight "obj.rune_dart" count 12
        4 weight "obj.rune_knife" count 8
        3 weight "obj.lava_battlestaff" count 1
        2 weight "obj.adamant_2h_sword" count 1
        2 weight "obj.adamant_platebody" count 1
        2 weight "obj.rune_axe" count 1
        2 weight "obj.rune_kiteshield" count 1
        2 weight "obj.rune_longsword" count 1
        1 weight "obj.rune_med_helm" count 1
        1 weight "obj.rune_full_helm" count 1
        10 weight "obj.rune_javelin" count 20
        7 weight "obj.firerune" count 75
        7 weight "obj.bloodrune" count 20
        6 weight "obj.xbows_crossbow_bolts_runite" count 30
        5 weight "obj.deathrune" count 20
        5 weight "obj.lawrune" count 20
        4 weight "obj.lavarune" count 15
        4 weight "obj.lavarune" count 30
        15 weight "obj.coins" count 66
        7 weight "obj.coins" count 2960 condition {
            player -> !player.hasCompletedQuest("quest_monkeymadness2")
        }
        1 weight "obj.coins" count 690
        1 weight "obj.fire_talisman" count 1
        5 weight "obj.cert_fire_orb" count 15
        3 weight "obj.chocolate_cake" count 3
        5 weight "obj.adamantite_bar" count 2
        5 weight "obj.xbows_bolt_tips_onyx" count 12
        7 weight "obj.dragon_javelin_head" count 15 condition {
            player -> player.hasCompletedQuest("quest_monkeymadness2")
        }

        5 weight SharedDropTables.herb
        3 weight SharedDropTables.rareDrop
        5 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        1 outOf 18 weight "obj.arceuus_corpse_dragon" count 1
        1 outOf 10000 weight "obj.dragonfire_visage" count 1
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/237 after unlocking the elite Combat Achievements rewards tier.
        // Drops Need Manual (rate): The elite clue scroll rarity changes to 1/125 if a Ring of wealth (i) is worn.
        1 outOf 250 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
