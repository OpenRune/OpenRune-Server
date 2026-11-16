package org.rsmod.content.bosses.moons.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.columnCoord
import org.rsmod.map.CoordGrid

object MoonsTable {

    const val LABEL = 0
    const val NPC = 1
    const val STATUE = 2
    const val AREA = 3
    const val DEAD_VARBIT = 4
    const val KILLS_VARP = 5
    const val DEATH_SPOTANIM = 6
    const val SOUTH_WEST = 7
    const val FLAT_ARMOUR = 8
    const val ATTACK_ANIM = 9
    const val ATTACK_SOUNDS = 10
    const val ATTACK_MESSAGE = 11
    const val HIT_SYNTH = 12
    const val PORTAL_MODEL = 13
    const val PORTAL_COLOUR = 14

    fun moons(): DBTable =
        dbTable("dbtable.pmoon_moons", serverOnly = true) {
            column("label", LABEL, VarType.STRING)
            column("npc", NPC, VarType.STRING)
            column("statue", STATUE, VarType.STRING)
            column("area", AREA, VarType.STRING)
            column("dead_varbit", DEAD_VARBIT, VarType.STRING)
            column("kills_varp", KILLS_VARP, VarType.STRING)
            column("death_spotanim", DEATH_SPOTANIM, VarType.STRING)
            column("south_west", SOUTH_WEST, VarType.COORDGRID)
            column("flat_armour", FLAT_ARMOUR, VarType.INT)
            column("attack_anim", ATTACK_ANIM, VarType.STRING)
            column("attack_sounds", ATTACK_SOUNDS, VarType.STRING, VarType.INT)
            column("attack_message", ATTACK_MESSAGE, VarType.STRING)
            column("hit_synth", HIT_SYNTH, VarType.STRING)
            column("portal_model", PORTAL_MODEL, VarType.INT)
            column("portal_colour", PORTAL_COLOUR, VarType.INT)

            row("dbrow.pmoon_moon_blood") {
                column(LABEL, "Blood Moon")
                column(NPC, "npc.pmoon_boss_blood_moon")
                column(STATUE, "loc.pmoon_entry_statue_blood")
                column(AREA, "area.pmoon_blood_arena")
                column(DEAD_VARBIT, "varbit.pmoon_boss_blood_dead")
                column(KILLS_VARP, "varp.pmoon_blood_kills")
                column(DEATH_SPOTANIM, "spotanim.pmoon_boss_fake_blood_death")
                columnCoord(SOUTH_WEST, CoordGrid(1390, 9630))
                column(FLAT_ARMOUR, -2)
                column(ATTACK_ANIM, "seq.npc_djinn_01_combo")
                column(PORTAL_MODEL, 52840)
                column(PORTAL_COLOUR, 12584775)
            }
            row("dbrow.pmoon_moon_blue") {
                column(LABEL, "Blue Moon")
                column(NPC, "npc.pmoon_boss_blue_moon")
                column(STATUE, "loc.pmoon_entry_statue_blue")
                column(AREA, "area.pmoon_blue_arena")
                column(DEAD_VARBIT, "varbit.pmoon_boss_blue_dead")
                column(KILLS_VARP, "varp.pmoon_blue_kills")
                column(DEATH_SPOTANIM, "spotanim.pmoon_boss_fake_blue_death")
                columnCoord(SOUTH_WEST, CoordGrid(1438, 9678))
                column(FLAT_ARMOUR, -5)
                column(ATTACK_ANIM, "seq.npc_djinn_03_combo")
                column(
                    ATTACK_SOUNDS,
                    "synth.varl_pm_boss_blue_melee_swipe_01",
                    50,
                    "synth.varl_pm_boss_blue_melee_swipe_02",
                    85,
                    "synth.varl_pm_boss_blue_melee_swipe_03",
                    115,
                )
                column(ATTACK_MESSAGE, "The Blue Moon pierces your defence and chills your joints!")
                column(HIT_SYNTH, "synth.varl_pm_boss_blue_hit_01")
                column(PORTAL_MODEL, 38742)
                column(PORTAL_COLOUR, 10861264)
            }
            row("dbrow.pmoon_moon_eclipse") {
                column(LABEL, "Eclipse Moon")
                column(NPC, "npc.pmoon_boss_eclipse_moon")
                column(STATUE, "loc.pmoon_entry_statue_eclipse")
                column(AREA, "area.pmoon_eclipse_arena")
                column(DEAD_VARBIT, "varbit.pmoon_boss_eclipse_dead")
                column(KILLS_VARP, "varp.pmoon_eclipse_kills")
                column(DEATH_SPOTANIM, "spotanim.pmoon_boss_fake_eclipse_death")
                columnCoord(SOUTH_WEST, CoordGrid(1486, 9630))
                column(FLAT_ARMOUR, 6)
                column(ATTACK_ANIM, "seq.npc_djinn_02_combo")
                column(
                    ATTACK_SOUNDS,
                    "synth.varl_pm_eclipse_melee_combo_swipe_04",
                    48,
                    "synth.varl_pm_eclipse_melee_combo_swipe_02",
                    80,
                    "synth.varl_pm_eclipse_melee_combo_swipe_03",
                    103,
                )
                column(
                    ATTACK_MESSAGE,
                    "The Eclipse Moon's light pierces your defence and blinds you!",
                )
                column(HIT_SYNTH, "synth.varl_pm_eclipse_damage_impact_01")
                column(PORTAL_MODEL, 44092)
                column(PORTAL_COLOUR, 16293385)
            }
        }
}
