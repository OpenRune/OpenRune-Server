package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object HunterLeonBoltTable {
    const val SPIKE = 0
    const val BOLTS = 1
    const val FEE = 2

    fun leonBolts() =
        dbTable("dbtable.hunter_leon_bolts", serverOnly = true) {
            column("spike", SPIKE, VarType.OBJ)
            column("bolts", BOLTS, VarType.OBJ)
            column("fee", FEE, VarType.INT)

            fun bolt(row: String, spike: String, bolts: String, fee: Int) =
                row(row) {
                    columnRSCM(SPIKE, spike)
                    columnRSCM(BOLTS, bolts)
                    column(FEE, fee)
                }

            bolt("dbrow.hunter_leon_bolts_kebbit", "obj.huntingbeast_spike", "obj.huntingbow_bolts", 20)
            bolt("dbrow.hunter_leon_bolts_long", "obj.huntingbeast_bigspike", "obj.huntingbow_bigbolts", 40)
        }

    const val MATERIAL = 0
    const val PRODUCT = 1
    const val LEVEL = 2
    const val XP_TENTHS = 3
    const val TICKS = 4

    fun boltFletching() =
        dbTable("dbtable.hunter_bolt_fletching", serverOnly = true) {
            column("material", MATERIAL, VarType.OBJ)
            column("bolts", PRODUCT, VarType.OBJ)
            column("level", LEVEL, VarType.INT)
            column("xp_tenths", XP_TENTHS, VarType.INT)
            column("ticks", TICKS, VarType.INT)

            fun recipe(row: String, material: String, bolts: String, level: Int, xpTenths: Int, ticks: Int) =
                row(row) {
                    columnRSCM(MATERIAL, material)
                    columnRSCM(PRODUCT, bolts)
                    column(LEVEL, level)
                    column(XP_TENTHS, xpTenths)
                    column(TICKS, ticks)
                }

            recipe("dbrow.hunter_bolt_fletching_kebbit", "obj.huntingbeast_spike", "obj.huntingbow_bolts", 32, 58, 3)
            recipe("dbrow.hunter_bolt_fletching_long", "obj.huntingbeast_bigspike", "obj.huntingbow_bigbolts", 42, 79, 2)
            recipe("dbrow.hunter_bolt_fletching_sunlight", "obj.sunlight_antler", "obj.sunlight_antelope_bolt", 62, 100, 3)
            recipe("dbrow.hunter_bolt_fletching_moonlight", "obj.moonlight_antler", "obj.moonlight_antelope_bolt", 72, 121, 3)
        }
}
