package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val theLeviathanDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "The Leviathan Drops",
    npcs = npcs("npc.leviathan", "npc.leviathan_cutscene", "npc.leviathan_quest"),
    mainTable = rsPlayerWeightedTable(total = 100) {
        name("The Leviathan Drops")
        1 weight "obj.cosmic_soul_catalyst" count 1
        1 weight "obj.cert_iron_ore" count 1
        1 weight "obj.cert_silver_ore" count 1
        8 weight "obj.cert_coal" count 1
        8 weight "obj.cert_gold_ore" count 1
        1 weight "obj.cert_adamantite_ore" count 1
        2 weight "obj.cert_runite_ore" count 1
        1 weight "obj.cert_sapphire" count 1
        1 weight "obj.cert_emerald" count 1
        1 weight "obj.cert_ruby" count 1
        5 weight "obj.cert_uncut_ruby" count 1
        5 weight "obj.cert_uncut_diamond" count 1
        8 weight "obj.dragon_javelin_head" count 1
        2 weight "obj.dragon_bolts_unfeathered" count 1
        1 weight "obj.xbows_bolt_tips_onyx" count 1
        1 weight "obj.cert_raw_mantaray" count 1
        8 weight "obj.anglerfish" count 1
        1 weight "obj.bronze_arrow" count 1
        1 weight "obj.mithril_arrow" count 1
        1 weight "obj.adamant_arrow" count 1
        8 weight "obj.rune_arrow" count 1
        1 weight "obj.bodyrune" count 1
        1 weight "obj.earthrune" count 1
        8 weight "obj.smokerune" count 1
        2 weight "obj.soulrune" count 1
        1 outOf 768 separate rsPlayerWeightedTable {
            1 weight "obj.venator_vestige" count 1
            1 weight "obj.soulreaper_axe_lure" count 1
        }
        3 outOf 768 separate "obj.chromium_ingot" count 1
        1 outOf 2304 separate rsPlayerWeightedTable {
            1 weight "obj.virtus_mask" count 1
            1 weight "obj.virtus_top" count 1
            1 weight "obj.virtus_legs" count 1
        }
        1 outOf 26 separate "obj.leviathan_tablet" count 1 condition { player ->
            // Drops Need Manual: The scarred tablet will become more common at an unknown rate as kill count increases until the first drop is received. Afterwards, the item will no longer be dropped.
             true
        }
        1 outOf 54 separate "obj.dt2_awakeners_orb" count 1
        1 outOf 206 separate "obj.smoke_quartz" count 1
        1 outOf 5 separate rsPlayerWeightedTable {
            1 weight "obj.3doseprayerrestore" count 1
            1 weight "obj.2doserangerspotion" count 1
            1 weight "obj.seaturtle" count 3..4
        }
        22 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll() }
        1 outOf 2500 weight "obj.leviathanpet" count 1
        1 outOf 1 weight "obj.dt2_sanguine_torva_kit" count 1 condition { player ->
            // Drops Need Manual: Only when defeated in the awakened encounter as the '''last''' of the four.
             true
        }
        // Drops Need Manual (rate): The easy clue scroll drop rate increases to 1/152 after unlocking the easy Combat Achievements rewards tier.
        1 outOf 160 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/152 after unlocking the medium Combat Achievements rewards tier.
        1 outOf 160 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/152 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 160 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/152 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 160 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
