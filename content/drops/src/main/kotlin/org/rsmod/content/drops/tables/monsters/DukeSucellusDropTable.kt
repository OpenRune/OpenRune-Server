package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val dukeSucellusDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Duke Sucellus Drops",
    npcs = npcs("npc.duke_sucellus_asleep", "npc.duke_sucellus_asleep_quest", "npc.duke_sucellus_awake", "npc.duke_sucellus_awake_quest", "npc.duke_sucellus_dead", "npc.duke_sucellus_dead_quest", "npc.duke_sucellus_inactive", "npc.duke_sucellus_inactive_quest"),
    mainTable = rsPlayerWeightedTable(total = 100) {
        name("Duke Sucellus Drops")
        1 weight "obj.cert_bronze_chainbody" count 1
        1 weight "obj.cert_mithril_chainbody" count 1
        1 weight "obj.cert_adamant_chainbody" count 1
        1 weight "obj.dragon_platelegs" count 1
        1 weight "obj.cert_blankrune_high" count 1
        1 weight "obj.cert_iron_ore" count 1
        8 weight "obj.cert_coal" count 1
        1 weight "obj.cert_mithril_ore" count 1
        8 weight "obj.cert_adamantite_ore" count 1
        2 weight "obj.cert_runite_ore" count 1
        1 weight "obj.cert_sapphire" count 1
        1 weight "obj.cert_emerald" count 1
        1 weight "obj.cert_ruby" count 1
        5 weight "obj.cert_uncut_ruby" count 1
        5 weight "obj.cert_uncut_diamond" count 1
        1 weight "obj.cert_bronze_bar" count 1
        2 weight "obj.dragon_arrowheads" count 1
        8 weight "obj.rune_javelin_head" count 1
        8 weight "obj.dragon_javelin_head" count 1
        1 weight "obj.cert_raw_seaturtle" count 1
        1 weight "obj.airrune" count 1
        8 weight "obj.mistrune" count 1
        9 weight "obj.chaosrune" count 1
        2 weight "obj.soulrune" count 1
        1 outOf 720 separate rsPlayerWeightedTable {
            1 weight "obj.magus_vestige" count 1
            1 weight "obj.soulreaper_axe_eye" count 1
        }
        3 outOf 720 separate "obj.chromium_ingot" count 1
        1 outOf 2160 separate rsPlayerWeightedTable {
            1 weight "obj.virtus_mask" count 1
            1 weight "obj.virtus_top" count 1
            1 weight "obj.virtus_legs" count 1
        }
        1 outOf 26 separate "obj.duke_sucellus_tablet" count 1 condition { player ->
            // Drops Need Manual: The frozen tablet will become more common at an unknown rate as kill count increases until the first drop is received. Afterwards, the item will no longer be dropped.
             true
        }
        1 outOf 49 separate "obj.dt2_awakeners_orb" count 1
        1 outOf 207 separate "obj.ice_quartz" count 1
        1 outOf 5 separate rsPlayerWeightedTable {
            1 weight "obj.pineapple_pizza" count 3..4
            1 weight "obj.3doseprayerrestore" count 1
            1 weight "obj.2dose2combat" count 1
        }
        22 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.dt2_duke_medallion_key" count 1 condition { player ->
            // Drops Need Manual: Only dropped by the quest variant.
             true
        }
        onBuilder { brimstoneKeyRoll() }
        1 outOf 2500 weight "obj.dukesucelluspet" count 1
        1 outOf 1 weight "obj.dt2_sanguine_torva_kit" count 1 condition { player ->
            // Drops Need Manual: Only when defeated in the awakened encounter as the '''last''' of the four.
             true
        }
        1 outOf 152 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_easy_simple001")
        }
        1 outOf 152 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_medium_emote_exp1")
        }
        1 outOf 152 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
        1 outOf 152 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_elite_emote_exp1")
        }
    },
)
