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
public val vardorvisDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Vardorvis Drops",
    npcs = npcs("npc.vardorvis", "npc.vardorvis_base_postquest", "npc.vardorvis_base_quest", "npc.vardorvis_cutscene", "npc.vardorvis_quest"),
    mainTable = rsPlayerWeightedTable(total = 100) {
        name("Vardorvis Drops")
        1 weight "obj.cert_blankrune_high" count 1
        1 weight "obj.cert_iron_ore" count 1
        1 weight "obj.cert_silver_ore" count 1
        8 weight "obj.cert_coal" count 1
        1 weight "obj.cert_mithril_ore" count 1
        8 weight "obj.cert_adamantite_ore" count 1
        2 weight "obj.cert_runite_ore" count 1
        1 weight "obj.cert_sapphire" count 1
        1 weight "obj.cert_emerald" count 1
        1 weight "obj.cert_ruby" count 1
        5 weight "obj.cert_uncut_ruby" count 1
        5 weight "obj.cert_uncut_diamond" count 1
        8 weight "obj.rune_javelin_head" count 1
        8 weight "obj.dragon_javelin_head" count 1
        2 weight "obj.dragon_dart_tip" count 1
        1 weight "obj.cert_raw_shark" count 1
        1 weight "obj.bronze_javelin" count 1
        1 weight "obj.mithril_javelin" count 1
        1 weight "obj.adamant_javelin" count 1
        1 weight "obj.xbows_crossbow_bolts_runite_tipped_onyx_enchanted" count 1
        1 weight "obj.mindrune" count 1
        1 weight "obj.firerune" count 1
        8 weight "obj.lavarune" count 1
        8 weight "obj.bloodrune" count 1
        2 weight "obj.soulrune" count 1
        1 outOf 1088 separate rsPlayerWeightedTable {
            1 weight "obj.ultor_vestige" count 1
            1 weight "obj.soulreaper_axe_head" count 1
        }
        3 outOf 1088 separate "obj.chromium_ingot" count 1
        1 outOf 3264 separate rsPlayerWeightedTable {
            1 weight "obj.virtus_mask" count 1
            1 weight "obj.virtus_top" count 1
            1 weight "obj.virtus_legs" count 1
        }
        1 outOf 26 separate "obj.vardorvis_tablet" count 1 condition { player ->
            // Drops Need Manual: The strangled tablet will become more common at an unknown rate as kill count increases until the first drop is received. Afterwards, the item will no longer be dropped.
             true
        }
        1 outOf 81 separate "obj.dt2_awakeners_orb" count 1
        1 outOf 204 separate "obj.blood_quartz" count 1
        1 outOf 5 separate rsPlayerWeightedTable {
            1 weight "obj.potato_tuna+sweetcorn" count 3..4
            1 weight "obj.3doseprayerrestore" count 1
            1 weight "obj.2dose2combat" count 1
        }
        22 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.dt2_stranglewood_key" count 1 condition { player ->
            // Drops Need Manual: Only dropped by the quest variant.
             true
        }
        onBuilder { brimstoneKeyRoll() }
        1 outOf 1 weight "obj.dt2_sanguine_torva_kit" count 1 condition { player ->
            // Drops Need Manual: Only when defeated in the awakened encounter as the '''last''' of the four.
             true
        }
        1 outOf 3000 weight "obj.vardorvispet" count 1
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
