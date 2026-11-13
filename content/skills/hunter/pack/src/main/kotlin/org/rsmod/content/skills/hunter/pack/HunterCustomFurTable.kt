package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object HunterCustomFurTable {
    const val GARMENT = 0
    const val FEE = 1
    const val COUNT = 2
    const val FURS = 3

    private const val TATTY_LARUPIA = "obj.hunting_fur_jaguar_shabby"
    private const val LARUPIA = "obj.hunting_fur_jaguar_perfect"
    private const val TATTY_GRAAHK = "obj.hunting_fur_leopard_shabby"
    private const val GRAAHK = "obj.hunting_fur_leopard_perfect"
    private const val TATTY_KYATT = "obj.hunting_fur_tiger_shabby"
    private const val KYATT = "obj.hunting_fur_tiger_perfect"

    fun customFurs() =
        dbTable("dbtable.hunter_custom_furs", serverOnly = true) {
            column("garment", GARMENT, VarType.OBJ)
            column("fee", FEE, VarType.INT)
            column("count", COUNT, VarType.INT)
            column("furs", FURS, VarType.OBJ)

            fun garment(row: String, obj: String, fee: Int, count: Int, vararg furs: String) =
                row(row) {
                    columnRSCM(GARMENT, obj)
                    column(FEE, fee)
                    column(COUNT, count)
                    column(FURS, furs.map(ConstantProvider::getMapping))
                }

            garment("dbrow.hunter_custom_fur_polar_top", "obj.hunting_camoflauge_robe_polar", 20, 2, "obj.huntingbeast_polar_fur")
            garment("dbrow.hunter_custom_fur_polar_legs", "obj.hunting_trousers_polar", 20, 2, "obj.huntingbeast_polar_fur")
            garment("dbrow.hunter_custom_fur_wood_top", "obj.hunting_camoflauge_robe_wood", 20, 2, "obj.huntingbeast_woodland_fur")
            garment("dbrow.hunter_custom_fur_wood_legs", "obj.hunting_trousers_wood", 20, 2, "obj.huntingbeast_woodland_fur")
            garment("dbrow.hunter_custom_fur_jungle_top", "obj.hunting_camoflauge_robe_jungle", 20, 2, "obj.huntingbeast_jungle_fur")
            garment("dbrow.hunter_custom_fur_jungle_legs", "obj.hunting_trousers_jungle", 20, 2, "obj.huntingbeast_jungle_fur")
            garment("dbrow.hunter_custom_fur_desert_top", "obj.hunting_camoflauge_robe_desert", 20, 2, "obj.huntingbeast_desert_fur")
            garment("dbrow.hunter_custom_fur_desert_legs", "obj.hunting_trousers_desert", 20, 2, "obj.huntingbeast_desert_fur")
            garment("dbrow.hunter_custom_fur_larupia_hat", "obj.hunting_hat_jaguar", 500, 1, LARUPIA)
            garment("dbrow.hunter_custom_fur_larupia_top", "obj.hunting_torso_jaguar", 100, 1, TATTY_LARUPIA, LARUPIA)
            garment("dbrow.hunter_custom_fur_larupia_legs", "obj.hunting_trousers_jaguar", 100, 1, TATTY_LARUPIA, LARUPIA)
            garment("dbrow.hunter_custom_fur_graahk_hat", "obj.hunting_hat_leopard", 750, 1, GRAAHK)
            garment("dbrow.hunter_custom_fur_graahk_top", "obj.hunting_torso_leopard", 150, 1, TATTY_GRAAHK, GRAAHK)
            garment("dbrow.hunter_custom_fur_graahk_legs", "obj.hunting_trousers_leopard", 150, 1, TATTY_GRAAHK, GRAAHK)
            garment("dbrow.hunter_custom_fur_kyatt_hat", "obj.hunting_hat_tiger", 1000, 1, KYATT)
            garment("dbrow.hunter_custom_fur_kyatt_top", "obj.hunting_torso_tiger", 200, 1, TATTY_KYATT, KYATT)
            garment("dbrow.hunter_custom_fur_kyatt_legs", "obj.hunting_trousers_tiger", 200, 1, TATTY_KYATT, KYATT)
            garment("dbrow.hunter_custom_fur_silent_gloves", "obj.hunting_silent_gloves", 600, 2, "obj.huntingbeast_silent_fur")
            garment("dbrow.hunter_custom_fur_spotted_cape", "obj.hunting_light_cape", 400, 2, "obj.huntingbeast_speedy_fur")
            garment("dbrow.hunter_custom_fur_spottier_cape", "obj.hunting_lighter_cape", 800, 2, "obj.huntingbeast_speedy2_fur")
        }
}
