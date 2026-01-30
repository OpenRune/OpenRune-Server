package org.alter.impl.skills

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object Smithing {

    const val COL_OUTPUT = 0
    const val COL_LEVEL = 1
    const val COL_SMELT_XP = 2
    const val COL_SMITH_XP = 3
    const val COL_SMELT_XP_ALTERNATE = 4
    const val COL_INPUT_PRIMARY = 5
    const val COL_INPUT_SECONDARY = 6
    const val COL_INPUT_PRIMARY_AMT = 7
    const val COL_INPUT_SECONDARY_AMT = 8
    const val COL_INPUT_PREFIX = 9

    const val COL_CANNONBALL_BAR = 0
    const val COL_CANNONBALL_OUTPUT = 1
    const val COL_CANNONBALL_LEVEL = 2
    const val COL_CANNONBALL_XP = 3

    fun cannonBalls() = dbTable("tables.smithing_cannon_balls") {
        column("bar", COL_CANNONBALL_BAR, VarType.OBJ)
        column("output", COL_CANNONBALL_OUTPUT, VarType.OBJ)
        column("level", COL_CANNONBALL_LEVEL, VarType.INT)
        column("xp", COL_CANNONBALL_XP, VarType.INT)

        row("dbrows.bronze_cannon_ball") {
            columnRSCM(COL_CANNONBALL_BAR,"items.bronze_bar")
            columnRSCM(COL_CANNONBALL_OUTPUT,"items.bronze_cannonball")
            column(COL_CANNONBALL_LEVEL,5)
            column(COL_CANNONBALL_XP,9)
        }

        row("dbrows.iron_cannon_ball") {
            columnRSCM(COL_CANNONBALL_BAR,"items.bronze_bar")
            columnRSCM(COL_CANNONBALL_OUTPUT,"items.iron_cannonball")
            column(COL_CANNONBALL_LEVEL,20)
            column(COL_CANNONBALL_XP,17)
        }

        row("dbrows.steel_cannon_ball") {
            columnRSCM(COL_CANNONBALL_BAR,"items.steel_bar")
            columnRSCM(COL_CANNONBALL_OUTPUT,"items.mcannonball")
            column(COL_CANNONBALL_LEVEL,35)
            column(COL_CANNONBALL_XP,27)
        }

        row("dbrows.mithril_cannon_ball") {
            columnRSCM(COL_CANNONBALL_BAR,"items.mithril_bar")
            columnRSCM(COL_CANNONBALL_OUTPUT,"items.mithril_cannonball")
            column(COL_CANNONBALL_LEVEL,55)
            column(COL_CANNONBALL_XP,34)
        }

        row("dbrows.adamantite_cannon_ball") {
            columnRSCM(COL_CANNONBALL_BAR,"items.adamantite_bar")
            columnRSCM(COL_CANNONBALL_OUTPUT,"items.adamant_cannonball")
            column(COL_CANNONBALL_LEVEL,75)
            column(COL_CANNONBALL_XP,43)
        }

        row("dbrows.runite_cannon_ball") {
            columnRSCM(COL_CANNONBALL_BAR,"items.runite_bar")
            columnRSCM(COL_CANNONBALL_OUTPUT,"items.rune_cannonball")
            column(COL_CANNONBALL_LEVEL,90)
            column(COL_CANNONBALL_XP,51)
        }


    }

    fun bars() = dbTable("tables.smithing_bars") {

        column("output", COL_OUTPUT, VarType.OBJ)
        column("level", COL_LEVEL, VarType.INT)
        column("smeltXp", COL_SMELT_XP, VarType.INT)
        column("smithXp", COL_SMITH_XP, VarType.INT)
        column("smithXpAlternate", COL_SMELT_XP_ALTERNATE, VarType.INT)
        column("input_primary", COL_INPUT_PRIMARY, VarType.OBJ)
        column("input_secondary", COL_INPUT_SECONDARY, VarType.OBJ)
        column("input_primary_amt", COL_INPUT_PRIMARY_AMT, VarType.INT)
        column("input_secondary_amt", COL_INPUT_SECONDARY_AMT, VarType.INT)
        column("prefix", COL_INPUT_PREFIX, VarType.STRING)

        row("dbrows.bronze") {
            columnRSCM(COL_OUTPUT,"items.bronze_bar")
            column(COL_LEVEL,1)
            column(COL_SMELT_XP,6)
            column(COL_SMITH_XP,12)
            columnRSCM(COL_INPUT_PRIMARY,"items.tin_ore")
            columnRSCM(COL_INPUT_SECONDARY,"items.copper_ore")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_SECONDARY_AMT,1)
            column(COL_INPUT_PREFIX,"bronze")

        }

        row("dbrows.blurite") {
            columnRSCM(COL_OUTPUT,"items.blurite_bar")
            column(COL_LEVEL,13)
            column(COL_SMELT_XP,8)
            column(COL_SMELT_XP_ALTERNATE,10)
            column(COL_SMITH_XP,17)
            columnRSCM(COL_INPUT_PRIMARY,"items.blurite_ore")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_PREFIX,"blurite")
        }

        row("dbrows.iron") {
            columnRSCM(COL_OUTPUT,"items.iron_bar")
            column(COL_LEVEL,15)
            column(COL_SMELT_XP,12)
            column(COL_SMITH_XP,25)
            columnRSCM(COL_INPUT_PRIMARY,"items.iron_ore")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_PREFIX,"iron")
        }

        row("dbrows.silver") {
            columnRSCM(COL_OUTPUT,"items.silver_bar")
            column(COL_LEVEL,20)
            column(COL_SMELT_XP,14)
            column(COL_SMITH_XP,50)
            columnRSCM(COL_INPUT_PRIMARY,"items.silver_ore")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_PREFIX,"silver")
        }

        row("dbrows.lead") {
            columnRSCM(COL_OUTPUT,"items.lead_bar")
            column(COL_LEVEL,25)
            column(COL_SMELT_XP,15)
            column(COL_SMITH_XP,0)
            columnRSCM(COL_INPUT_PRIMARY,"items.lead_ore")
            column(COL_INPUT_PRIMARY_AMT,2)
            column(COL_INPUT_PREFIX,"lead")
        }

        row("dbrows.steel") {
            columnRSCM(COL_OUTPUT,"items.steel_bar")
            column(COL_LEVEL,30)
            column(COL_SMELT_XP,17)
            column(COL_SMITH_XP,37)
            columnRSCM(COL_INPUT_PRIMARY,"items.iron_ore")
            columnRSCM(COL_INPUT_SECONDARY,"items.coal")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_SECONDARY_AMT,2)
            column(COL_INPUT_PREFIX,"steel")
        }

        row("dbrows.gold") {
            columnRSCM(COL_OUTPUT,"items.gold_bar")
            column(COL_LEVEL,30)
            column(COL_SMELT_XP,22)
            column(COL_SMELT_XP_ALTERNATE,56)
            column(COL_SMITH_XP,90)
            columnRSCM(COL_INPUT_PRIMARY,"items.gold_ore")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_PREFIX,"gold")
        }

        row("dbrows.lovakite") {
            columnRSCM(COL_OUTPUT,"items.lovakite_bar")
            column(COL_LEVEL,45)
            column(COL_SMELT_XP,20)
            column(COL_SMITH_XP,60)
            columnRSCM(COL_INPUT_PRIMARY,"items.lovakite_ore")
            columnRSCM(COL_INPUT_SECONDARY,"items.coal")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_SECONDARY_AMT,2)
            column(COL_INPUT_PREFIX,"shayzien")
        }

        row("dbrows.mithril") {
            columnRSCM(COL_OUTPUT,"items.mithril_bar")
            column(COL_LEVEL,50)
            column(COL_SMELT_XP,30)
            column(COL_SMITH_XP,50)
            columnRSCM(COL_INPUT_PRIMARY,"items.mithril_ore")
            columnRSCM(COL_INPUT_SECONDARY,"items.coal")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_SECONDARY_AMT,4)
            column(COL_INPUT_PREFIX,"mithril")
        }

        row("dbrows.adamantite") {
            columnRSCM(COL_OUTPUT,"items.adamantite_bar")
            column(COL_LEVEL,70)
            column(COL_SMELT_XP,37)
            column(COL_SMITH_XP,62)
            columnRSCM(COL_INPUT_PRIMARY,"items.adamantite_ore")
            columnRSCM(COL_INPUT_SECONDARY,"items.coal")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_SECONDARY_AMT,6)
            column(COL_INPUT_PREFIX,"adamant")
        }

        row("dbrows.cupronickel") {
            columnRSCM(COL_OUTPUT,"items.cupronickel_bar")
            column(COL_LEVEL,74)
            column(COL_SMELT_XP,42)
            column(COL_SMITH_XP,0)
            columnRSCM(COL_INPUT_PRIMARY,"items.nickel_ore")
            columnRSCM(COL_INPUT_SECONDARY,"items.copper_ore")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_SECONDARY_AMT,2)
            column(COL_INPUT_PREFIX,"cupronickel")
        }

        row("dbrows.runite") {
            columnRSCM(COL_OUTPUT,"items.runite_bar")
            column(COL_LEVEL,85)
            column(COL_SMELT_XP,50)
            column(COL_SMITH_XP,75)
            columnRSCM(COL_INPUT_PRIMARY,"items.runite_ore")
            columnRSCM(COL_INPUT_SECONDARY,"items.coal")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_SECONDARY_AMT,8)
            column(COL_INPUT_PREFIX,"rune")
        }

    }
}