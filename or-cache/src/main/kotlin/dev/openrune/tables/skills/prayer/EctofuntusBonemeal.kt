package dev.openrune.tables.skills.prayer

import dev.openrune.tables.production.productionTable

object EctofuntusBonemeal {

    fun table() = productionTable("dbtable.prayer_ectofuntus_bonemeal", serverOnly = true, defaultCategory = "Bonemeal") {
        row("dbrow.prayer_ecto_alan_bones") {
            production {
                input("obj.alan_bones")
                statReq("stat.prayer", 1)
                xp(12)
                output("obj.pot_bonemeal_alan")
            }
        }
        row("dbrow.prayer_ecto_bones") {
            production {
                input("obj.bones")
                statReq("stat.prayer", 1)
                xp(18)
                output("obj.pot_bonemeal")
            }
        }
        row("dbrow.prayer_ecto_bat_bones") {
            production {
                input("obj.bat_bones")
                statReq("stat.prayer", 1)
                xp(21)
                output("obj.pot_bonemeal_bat")
            }
        }
        row("dbrow.prayer_ecto_big_bones") {
            production {
                input("obj.big_bones")
                statReq("stat.prayer", 1)
                xp(60)
                output("obj.pot_bonemeal_big")
            }
        }
        row("dbrow.prayer_ecto_long_bone") {
            production {
                input("obj.dorgesh_construction_bone")
                statReq("stat.prayer", 1)
                xp(60)
                output("obj.pot_bonemeal_big")
            }
        }
        row("dbrow.prayer_ecto_curved_bone") {
            production {
                input("obj.dorgesh_construction_bone_curved")
                statReq("stat.prayer", 1)
                xp(60)
                output("obj.pot_bonemeal_big")
            }
        }
        row("dbrow.prayer_ecto_burnt_bones") {
            production {
                input("obj.bones_burnt")
                statReq("stat.prayer", 1)
                xp(18)
                output("obj.pot_bonemeal_burnt")
            }
        }
        row("dbrow.prayer_ecto_burnt_jogre_bones") {
            production {
                input("obj.tbwt_burnt_jogre_bones")
                statReq("stat.prayer", 1)
                xp(64)
                output("obj.pot_bonemeal_burnt_jogre")
            }
        }
        row("dbrow.prayer_ecto_jogre_bones") {
            production {
                input("obj.tbwt_jogre_bones")
                statReq("stat.prayer", 1)
                xp(60)
                output("obj.pot_bonemeal_jogre")
            }
        }
        row("dbrow.prayer_ecto_zogre_bones") {
            production {
                input("obj.zogre_bones")
                statReq("stat.prayer", 1)
                xp(90)
                output("obj.pot_bonemeal_zogre")
            }
        }
        row("dbrow.prayer_ecto_babydragon_bones") {
            production {
                input("obj.babydragon_bones")
                statReq("stat.prayer", 1)
                xp(120)
                output("obj.pot_bonemeal_babydragon")
            }
        }
        row("dbrow.prayer_ecto_wyrmling_bones") {
            production {
                input("obj.babywyrm_bones")
                statReq("stat.prayer", 1)
                xp(120)
                output("obj.pot_bonemeal_babydragon")
            }
        }
        row("dbrow.prayer_ecto_dragon_bones") {
            production {
                input("obj.dragon_bones")
                statReq("stat.prayer", 1)
                xp(288)
                output("obj.pot_bonemeal_dragon")
            }
        }
        row("dbrow.prayer_ecto_wolf_bones") {
            production {
                input("obj.wolf_bones")
                statReq("stat.prayer", 1)
                xp(18)
                output("obj.pot_bonemeal_wolf")
            }
        }
        row("dbrow.prayer_ecto_monkey_bones") {
            production {
                input("obj.mm_normal_monkey_bones")
                statReq("stat.prayer", 1)
                xp(20)
                output("obj.pot_bonemeal_normal_monkey")
            }
        }
        row("dbrow.prayer_ecto_small_ninja_monkey_bones") {
            production {
                input("obj.mm_small_ninja_monkey_bones")
                statReq("stat.prayer", 1)
                xp(20)
                output("obj.pot_bonemeal_small_ninja_monkey")
            }
        }
        row("dbrow.prayer_ecto_medium_ninja_monkey_bones") {
            production {
                input("obj.mm_medium_ninja_monkey_bones")
                statReq("stat.prayer", 1)
                xp(18)
                output("obj.pot_bonemeal_medium_ninja_monkey")
            }
        }
        row("dbrow.prayer_ecto_gorilla_bones") {
            production {
                input("obj.mm_normal_gorilla_monkey_bones")
                statReq("stat.prayer", 1)
                xp(72)
                output("obj.pot_bonemeal_normal_gorilla_monkey")
            }
        }
        row("dbrow.prayer_ecto_bearded_gorilla_bones") {
            production {
                input("obj.mm_bearded_gorilla_monkey_bones")
                statReq("stat.prayer", 1)
                xp(72)
                output("obj.pot_bonemeal_bearded_gorilla_monkey")
            }
        }
        row("dbrow.prayer_ecto_small_zombie_monkey_bones") {
            production {
                input("obj.mm_small_zombie_monkey_bones")
                statReq("stat.prayer", 1)
                xp(20)
                output("obj.pot_bonemeal_small_ninja_monkey")
            }
        }
        row("dbrow.prayer_ecto_large_zombie_monkey_bones") {
            production {
                input("obj.mm_large_zombie_monkey_bones")
                statReq("stat.prayer", 1)
                xp(18)
                output("obj.pot_bonemeal_large_zombie_monkey")
            }
        }
        row("dbrow.prayer_ecto_ape_atoll_skeleton_bones") {
            production {
                input("obj.mm_skeleton_bones")
                statReq("stat.prayer", 1)
                xp(12)
                output("obj.pot_bonemeal_skeleton")
            }
        }
        row("dbrow.prayer_ecto_dagannoth_bones") {
            production {
                input("obj.dagannoth_king_bones")
                statReq("stat.prayer", 1)
                xp(500)
                output("obj.pot_bonemeal_dagannoth")
            }
        }
        row("dbrow.prayer_ecto_wyvern_bones") {
            production {
                input("obj.wyvern_bones")
                statReq("stat.prayer", 1)
                xp(288)
                output("obj.pot_bonemeal_wyvern")
            }
        }
        row("dbrow.prayer_ecto_lava_dragon_bones") {
            production {
                input("obj.lava_dragon_bones")
                statReq("stat.prayer", 1)
                xp(340)
                output("obj.pot_bonemeal_lavadragon")
            }
        }
        row("dbrow.prayer_ecto_fayrg_bones") {
            production {
                input("obj.zogre_ancestral_bones_fayg")
                statReq("stat.prayer", 1)
                xp(336)
                output("obj.pot_bonemeal_ancestral_fayg")
            }
        }
        row("dbrow.prayer_ecto_raurg_bones") {
            production {
                input("obj.zogre_ancestral_bones_raurg")
                statReq("stat.prayer", 1)
                xp(384)
                output("obj.pot_bonemeal_ancestral_raurg")
            }
        }
        row("dbrow.prayer_ecto_ourg_bones") {
            production {
                input("obj.zogre_ancestral_bones_ourg")
                statReq("stat.prayer", 1)
                xp(560)
                output("obj.pot_bonemeal_ancestral_ourg")
            }
        }
        row("dbrow.prayer_ecto_shaikahan_bones") {
            production {
                input("obj.tbwt_beast_bones")
                statReq("stat.prayer", 1)
                xp(100)
                output("obj.pot_bonemeal_beast")
            }
        }
        row("dbrow.prayer_ecto_superior_dragon_bones") {
            production {
                input("obj.dragon_bones_superior")
                statReq("stat.prayer", 70)
                xp(600)
                output("obj.pot_bonemeal_dragon_superior")
            }
        }
        row("dbrow.prayer_ecto_frost_dragon_bones") {
            production {
                input("obj.frost_dragon_bones")
                statReq("stat.prayer", 1)
                xp(400)
                output("obj.pot_bonemeal_frost_dragon")
            }
        }
        row("dbrow.prayer_ecto_wyrm_bones") {
            production {
                input("obj.wyrm_bones")
                statReq("stat.prayer", 1)
                xp(200)
                output("obj.pot_bonemeal_wyrm")
            }
        }
        row("dbrow.prayer_ecto_strykewyrm_bones") {
            production {
                input("obj.strykewyrm_bones")
                statReq("stat.prayer", 1)
                xp(200)
                output("obj.pot_bonemeal_wyrm")
            }
        }
        row("dbrow.prayer_ecto_drake_bones") {
            production {
                input("obj.drake_bones")
                statReq("stat.prayer", 1)
                xp(320)
                output("obj.pot_bonemeal_drake")
            }
        }
        row("dbrow.prayer_ecto_hydra_bones") {
            production {
                input("obj.hydra_bones")
                statReq("stat.prayer", 1)
                xp(440)
                output("obj.pot_bonemeal_hydra")
            }
        }
    }
}
