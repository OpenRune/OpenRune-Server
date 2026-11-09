package org.rsmod.content.other.sawmill.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object SawmillOperatorsTable {
    const val NPC = 0
    const val GREETING = 1
    const val EXPLAINS_PLANKS = 2
    const val ACKNOWLEDGES_SUPPLIES = 3
    const val DECLINE_OPTION = 4
    const val FAREWELL = 5

    private const val PLANK_OFFER =
        "Do you want me to make some planks for you? I can make planks from wood, oak, teak and " +
            "mahogany logs. Or would you like to buy some other housing supplies?"

    fun sawmillOperators() =
        dbTable("dbtable.sawmill_operators", serverOnly = true) {
            column("npc", NPC, VarType.NPC)
            column("greeting", GREETING, VarType.STRING)
            column("explains_planks", EXPLAINS_PLANKS, VarType.BOOLEAN)
            column("acknowledges_supplies", ACKNOWLEDGES_SUPPLIES, VarType.BOOLEAN)
            column("decline_option", DECLINE_OPTION, VarType.STRING)
            column("farewell", FAREWELL, VarType.STRING)

            row("dbrow.sawmill_operator_lumber_yard") {
                columnRSCM(NPC, "npc.poh_sawmill_opp")
                column(GREETING, "Hello there. $PLANK_OFFER")
                column(EXPLAINS_PLANKS, false)
                column(ACKNOWLEDGES_SUPPLIES, true)
                column(DECLINE_OPTION, "I'm good, thanks.")
                column(
                    FAREWELL,
                    "Well come back when you want some. You'll struggle to find quality planks " +
                        "anywhere but here!",
                )
            }

            row("dbrow.sawmill_operator_prifddinas") {
                columnRSCM(NPC, "npc.prif_sawmill_operator")
                column(GREETING, PLANK_OFFER)
                column(EXPLAINS_PLANKS, true)
                column(ACKNOWLEDGES_SUPPLIES, false)
                column(DECLINE_OPTION, "Nothing, thanks")
                column(
                    FAREWELL,
                    "Well come back when you want some. You can't get good quality planks " +
                        "anywhere but here!",
                )
            }

            row("dbrow.sawmill_operator_auburnvale") {
                columnRSCM(NPC, "npc.auburn_sawmill_operator")
                column(GREETING, "Nilsal. $PLANK_OFFER")
                column(EXPLAINS_PLANKS, false)
                column(ACKNOWLEDGES_SUPPLIES, true)
                column(DECLINE_OPTION, "I'm good, thanks")
                column(
                    FAREWELL,
                    "Well come back when you want some. You'll struggle to find quality planks " +
                        "anywhere but here!",
                )
            }
        }
}
