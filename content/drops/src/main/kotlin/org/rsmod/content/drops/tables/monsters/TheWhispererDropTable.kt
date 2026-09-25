package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.content.drops.dt2Drop
import org.rsmod.content.drops.shouldDropSanguineTorvaKit
import org.rsmod.content.drops.vestigeProgressRoll
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val theWhispererDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "The Whisperer Drops",
    npcs = npcs("npc.whisperer", "npc.whisperer_spawn", "npc.whisperer_melee", "npc.whisperer_melee_quest", "npc.whisperer_quest"),
    mainTable = rsPlayerWeightedTable(total = 100) {
        name("The Whisperer Drops")
        1 weight dt2Drop("obj.cert_bronze_longsword", 16, 24)
        1 weight dt2Drop("obj.cert_mithril_longsword", 7, 10)
        1 weight dt2Drop("obj.cert_adamant_longsword", 9, 14)
        2 weight dt2Drop("obj.cert_battlestaff", 70, 105)
        1 weight dt2Drop("obj.dragon_plateskirt", 7, 10)
        1 weight dt2Drop("obj.cert_blankrune_high", 280, 420)
        1 weight dt2Drop("obj.cert_iron_ore", 88, 133)
        8 weight dt2Drop("obj.cert_coal", 303, 455)
        1 weight dt2Drop("obj.cert_gold_ore", 88, 133)
        1 weight dt2Drop("obj.cert_mithril_ore", 88, 133)
        8 weight dt2Drop("obj.cert_adamantite_ore", 105, 157)
        2 weight dt2Drop("obj.cert_runite_ore", 42, 63)
        1 weight dt2Drop("obj.cert_sapphire", 39, 59)
        1 weight dt2Drop("obj.cert_emerald", 39, 59)
        1 weight dt2Drop("obj.cert_ruby", 39, 59)
        5 weight dt2Drop("obj.cert_uncut_ruby", 58, 87)
        5 weight dt2Drop("obj.cert_uncut_diamond", 58, 87)
        8 weight dt2Drop("obj.dragon_javelin_head", 84, 126)
        8 weight dt2Drop("obj.xbows_crossbow_bolts_runite_unfeathered", 84, 126)
        1 weight dt2Drop("obj.cert_raw_monkfish", 700, 1050)
        1 weight dt2Drop("obj.waterrune", 280, 420)
        8 weight dt2Drop("obj.steamrune", 466, 700)
        1 weight dt2Drop("obj.chaosrune", 140, 210)
        8 weight dt2Drop("obj.deathrune", 466, 700)
        2 weight dt2Drop("obj.soulrune", 933, 1400)
        1 outOf 512 separate "obj.soulreaper_axe_staff" count 1
        3 outOf 512 separate vestigeProgressRoll("varp.whisperer_vestige_progress", "obj.bellator_vestige")
        3 outOf 512 separate "obj.chromium_ingot" count 1
        1 outOf 1536 separate rsPlayerWeightedTable {
            1 weight "obj.virtus_mask" count 1
            1 weight "obj.virtus_top" count 1
            1 weight "obj.virtus_legs" count 1
        }
        1 outOf 26 separate "obj.whisperer_tablet" count 1 condition { player ->
            // Drops Need Manual: The sirenic tablet will become more common at an unknown rate as kill count increases until the first drop is received. Afterwards, the item will no longer be dropped.
             true
        }
        1 outOf 35 separate "obj.dt2_awakeners_orb" count 1
        1 outOf 209 separate "obj.shadow_quartz" count 1
        1 outOf 6 separate rsPlayerWeightedTable {
            1 weight "obj.2doseancientbrew" count 1
            1 weight "obj.3doseprayerrestore" count 1
            1 weight "obj.mantaray" count 3..4
        }
        22 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll() }
        1 outOf 1 weight "obj.dt2_sanguine_torva_kit" count 1 killCondition { player, npc, _ ->
            player.shouldDropSanguineTorvaKit(npc, "obj.dt2_sanguine_torva_kit")
        }
        1 outOf 2000 weight "obj.whispererpet" count 1
        1 outOf 160 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_easy_simple001")
        }
        1 outOf 160 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_medium_emote_exp1")
        }
        1 outOf 160 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
        1 outOf 160 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_elite_emote_exp1")
        }
    },
)
