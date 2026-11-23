package org.rsmod.content.areas.misc.warriorsguild.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object WarriorsGuildTables {
    const val SET_HELM = 0
    const val SET_BODY = 1
    const val SET_LEGS = 2
    const val SET_NPC = 3
    const val SET_INDEX = 4

    const val SHOT_STANCE = 0
    const val SHOT_SPOTANIM = 1
    const val SHOT_DEFEND_SEQ = 2
    const val SHOT_FAIL_SEQ = 3
    const val SHOT_NAME = 4

    const val DUMMY_HOLE = 0
    const val DUMMY_UP = 1
    const val DUMMY_STYLE = 2
    const val DUMMY_TYPE = 3

    fun animatorSets() =
        dbTable("dbtable.warguild_animator_sets", serverOnly = true) {
            column("helm", SET_HELM, VarType.OBJ)
            column("body", SET_BODY, VarType.OBJ)
            column("legs", SET_LEGS, VarType.OBJ)
            column("npc", SET_NPC, VarType.NPC)
            column("index", SET_INDEX, VarType.INT)

            val metals = listOf("bronze", "iron", "steel", "black", "mithril", "adamant", "rune")
            metals.forEachIndexed { index, metal ->
                row("dbrow.warguild_animator_$metal") {
                    columnRSCM(SET_HELM, "obj.${metal}_full_helm")
                    columnRSCM(SET_BODY, "obj.${metal}_platebody")
                    columnRSCM(SET_LEGS, "obj.${metal}_platelegs")
                    columnRSCM(SET_NPC, "npc.warguild_${metal}_armour")
                    column(SET_INDEX, index + 1)
                }
            }
        }

    fun catapultShots() =
        dbTable("dbtable.warguild_catapult_shots", serverOnly = true) {
            column("stance", SHOT_STANCE, VarType.INT)
            column("spotanim", SHOT_SPOTANIM, VarType.INT)
            column("defend_seq", SHOT_DEFEND_SEQ, VarType.SEQ)
            column("fail_seq", SHOT_FAIL_SEQ, VarType.SEQ)
            column("name", SHOT_NAME, VarType.STRING)

            fun shot(row: String, stance: Int, proj: String, seq: String, name: String) {
                row("dbrow.warguild_catapult_$row") {
                    column(SHOT_STANCE, stance)
                    columnRSCM(SHOT_SPOTANIM, "spotanim.warguild_proj_$proj")
                    columnRSCM(SHOT_DEFEND_SEQ, "seq.warguild_${seq}_def")
                    columnRSCM(SHOT_FAIL_SEQ, "seq.warguild_${seq}_fail")
                    column(SHOT_NAME, name)
                }
            }

            shot("stab", stance = 0, proj = "spike", seq = "spike", name = "spiky ball")
            shot("crush", stance = 1, proj = "blunt", seq = "blunt", name = "flung anvil")
            shot("slash", stance = 2, proj = "slash", seq = "blade", name = "slashing blades")
            shot("magic", stance = 3, proj = "mage", seq = "mage", name = "magic missile")
        }

    fun dummies() =
        dbTable("dbtable.warguild_dummies", serverOnly = true) {
            column("hole", DUMMY_HOLE, VarType.LOC)
            column("up", DUMMY_UP, VarType.LOC)
            column("attack_style", DUMMY_STYLE, VarType.INT)
            column("attack_type", DUMMY_TYPE, VarType.INT)

            fun dummy(row: String, loc: String, style: Int = 0, type: Int = 0) {
                row("dbrow.warguild_dummy_$row") {
                    columnRSCM(DUMMY_HOLE, "loc.warguild_dummy_$loc")
                    columnRSCM(DUMMY_UP, "loc.warguild_dummy_${loc}_up")
                    column(DUMMY_STYLE, style)
                    column(DUMMY_TYPE, type)
                }
            }

            // Style ids follow AttackStyle (controlled 1, accurate 2, aggressive 3, defensive 4)
            // and type ids follow AttackType (stab 1, slash 2, crush 3); 0 means "any".
            dummy("accurate", "acc", style = 2)
            dummy("slash", "slash", type = 2)
            dummy("aggressive", "agg", style = 3)
            dummy("controlled", "control", style = 1)
            dummy("crush", "crush", type = 3)
            dummy("stab", "stab", type = 1)
            dummy("defensive", "defend", style = 4)
        }
}
