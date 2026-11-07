package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.content.drops.hasObjInInventoryOrBank
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val madAngelDropTable: RSDropTable<Player, DropRollItem> =
    RSDropTable(
        tableIdentifier = "Mad Angel Drops",
        npcs = npcs("npc.mad_angel", "npc.mad_angel_initial"),
        guaranteed =
            rsPlayerGuaranteedTable {
                "obj.granite_dust" count 25..35
                "obj.sunstone_crystal" count 1 condition { player ->
                    !player.hasObjInInventoryOrBank("obj.sunstone_crystal") &&
                        player.vars["varbit.amenity_bankchest_wyrmscraig"] == 0
                }
            },
        mainTable =
            rsPlayerWeightedTable(total = 150) {
                name("Mad Angel Drops")
                1 outOf 25 separate "obj.teleportscroll_ardeaglais" count 2
                boosted {
                    1 outOf 127 separate "obj.hallowfell" count 1
                    1 outOf 1000 separate "obj.jar_of_light" count 1
                }
                12 weight "obj.rune_kiteshield" count 1
                12 weight "obj.rune_full_helm" count 1
                6 weight "obj.adamant_longsword" count 1
                2 weight "obj.dragon_med_helm" count 1
                2 weight "obj.dragon_battleaxe" count 1
                16 weight "obj.mcannonball" count 90..100
                12 weight "obj.lawrune" count 28..30
                8 weight "obj.naturerune" count 20..25
                8 weight "obj.deathrune" count 40..50
                4 weight "obj.airrune" count 110..120
                16 weight
                    rsPlayerGuaranteedTable {
                        add(
                            rsPlayerWeightedTable {
                                1 weight "obj.shark" count 2
                                1 weight "obj.yellowfin" count 2
                            }
                        )
                        "obj.2doseprayerrestore" count 1
                        "obj.1dose2combat" count 1
                    }
                12 weight "obj.4doseprayerrestore" count 1
                8 weight "obj.cert_3dose2combat" count 2..3
                8 weight "obj.coins" count 8000..10000
                8 weight "obj.cert_raw_monkfish" count 18..22
                6 weight "obj.cert_emerald" count 7..8
                6 weight "obj.cert_sapphire" count 7..8
                1 weight "obj.cupronickel_bar" count 1
                1 weight "obj.papyrus" count 1
                2 weight SharedDropTables.rareDrop
            },
        tertiaries =
            rsPlayerTertiaryTable {
                1 outOf 25 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
                    player.clueScrollTransformObj("obj.trail_medium_emote_exp1")
                }
            },
    )
