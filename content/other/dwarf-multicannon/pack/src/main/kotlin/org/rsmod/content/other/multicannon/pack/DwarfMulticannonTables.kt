package org.rsmod.content.other.multicannon.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object DwarfMulticannonTables {
    const val PARTS = 0
    const val LOCS = 1
    const val BROKEN = 2
    const val FIRE_SEQS = 3
    const val TURN_SEQS = 4
    const val PROJECTILE = 5
    const val GRANITE_PROJECTILE = 6

    const val NPCS = 0

    fun kits() =
        dbTable("dbtable.mcannon_kits", serverOnly = true) {
            column("parts", PARTS, VarType.OBJ)
            column("locs", LOCS, VarType.LOC)
            column("broken", BROKEN, VarType.LOC)
            column("fire_seqs", FIRE_SEQS, VarType.SEQ)
            column("turn_seqs", TURN_SEQS, VarType.SEQ)
            column("projectile", PROJECTILE, VarType.INT)
            column("granite_projectile", GRANITE_PROJECTILE, VarType.INT)

            row("dbrow.mcannon_kit") {
                columnRSCM(PARTS, "obj.twpart1", "obj.twpart2", "obj.twpart3", "obj.twpart4")
                columnRSCM(
                    LOCS,
                    "loc.multicannon_base",
                    "loc.multicannon_stand",
                    "loc.multicannon_barrels",
                    "loc.dwarf_multicannon1",
                )
                columnRSCM(BROKEN, "loc.dwarf_multicannon1_broken")
                columnRSCM(
                    FIRE_SEQS,
                    "seq.mcannon_n_fire",
                    "seq.mcannon_ne_fire",
                    "seq.mcannon_e_fire",
                    "seq.mcannon_se_fire",
                    "seq.mcannon_s_fire",
                    "seq.mcannon_sw_fire",
                    "seq.mcannon_w_fire",
                    "seq.mcannon_nw_fire",
                )
                columnRSCM(
                    TURN_SEQS,
                    "seq.mcannon_n_turn",
                    "seq.mcannon_ne_turn",
                    "seq.mcannon_e_turn",
                    "seq.mcannon_se_turn",
                    "seq.mcannon_s_turn",
                    "seq.mcannon_sw_turn",
                    "seq.mcannon_w_turn",
                    "seq.mcannon_nw_turn",
                )
                columnRSCM(PROJECTILE, "spotanim.cannonball_travel")
                columnRSCM(GRANITE_PROJECTILE, "spotanim.cannonball_travel_granite")
            }
            row("dbrow.mcannon_kit_league03") {
                columnRSCM(
                    PARTS,
                    "obj.league_3_multicannon_base",
                    "obj.league_3_multicannon_stand",
                    "obj.league_3_multicannon_barrels",
                    "obj.league_3_multicannon_furnace",
                )
                columnRSCM(
                    LOCS,
                    "loc.league03_multicannon_base",
                    "loc.league03_multicannon_stand",
                    "loc.league03_multicannon_barrels",
                    "loc.league03_multicannon_active",
                )
                columnRSCM(BROKEN, "loc.league03_multicannon_broken")
                columnRSCM(
                    FIRE_SEQS,
                    "seq.cannon_league03_fire_n",
                    "seq.cannon_league03_fire_ne",
                    "seq.cannon_league03_fire_e",
                    "seq.cannon_league03_fire_se",
                    "seq.cannon_league03_fire_s",
                    "seq.cannon_league03_fire_sw",
                    "seq.cannon_league03_fire_w",
                    "seq.cannon_league03_fire_nw",
                )
                columnRSCM(
                    TURN_SEQS,
                    "seq.cannon_league03_aim_n",
                    "seq.cannon_league03_aim_ne",
                    "seq.cannon_league03_aim_e",
                    "seq.cannon_league03_aim_se",
                    "seq.cannon_league03_aim_s",
                    "seq.cannon_league03_aim_sw",
                    "seq.cannon_league03_aim_w",
                    "seq.cannon_league03_aim_nw",
                )
                columnRSCM(PROJECTILE, "spotanim.cannonball_travel_league")
                columnRSCM(GRANITE_PROJECTILE, "spotanim.cannonball_travel_granite_league")
            }
        }

    fun npcs() =
        dbTable("dbtable.mcannon_npcs", serverOnly = true) {
            column("npcs", NPCS, VarType.NPC)

            row("dbrow.mcannon_destroyers") {
                columnRSCM(
                    NPCS,
                    "npc.king_dragon",
                    "npc.kalphite_queen",
                    "npc.kalphite_flyingqueen",
                    "npc.smoke_devil_boss",
                    "npc.myarm_giant_roc",
                )
            }
            row("dbrow.mcannon_untargeted") {
                columnRSCM(
                    NPCS,
                    "npc.zeah_sandcrab",
                    "npc.zeah_sandcrab_small",
                    "npc.fossil_ammonitecrab",
                    "npc.kolhuman",
                    "npc.kologre",
                    "npc.kolspider",
                    "npc.kolethereal",
                    "npc.koldemon",
                    "npc.zamorak_mage",
                    "npc.saradomin_mage",
                    "npc.guthix_mage",
                )
            }
            row("dbrow.mcannon_damage_immune") {
                columnRSCM(
                    NPCS,
                    "npc.mm2_demon_gorilla_1_melee",
                    "npc.mm2_demon_gorilla_1_ranged",
                    "npc.mm2_demon_gorilla_1_magic",
                    "npc.mm2_demon_gorilla_2_melee",
                    "npc.mm2_demon_gorilla_2_ranged",
                    "npc.mm2_demon_gorilla_2_magic",
                    "npc.slayer_kursk_1",
                    "npc.slayer_kursk_2",
                    "npc.superior_kurask",
                )
            }
        }
}
