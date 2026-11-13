package org.rsmod.content.quest.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.columnCoord
import org.rsmod.map.CoordGrid

object PerilousMoonsRuinsTables {

    const val AREA = 0
    const val ZONE_X = 1
    const val ZONE_Z = 2
    const val ZONES_LONG = 3
    const val CAMP_SPOT = 4
    const val CAMP_STOVE = 5
    const val CAMP_PIECES = 6
    const val CAMP_VARBIT = 7

    const val SPAWN_PART = 0
    const val SPAWN_NPC = 1
    const val SPAWN_TILE = 2

    private const val ANTECHAMBER = "dbrow.pmoon_ruins_antechamber"
    private const val ANCIENT_PRISON = "dbrow.pmoon_ruins_ancient_prison"
    private const val EARTHBOUND_CAVERN = "dbrow.pmoon_ruins_earthbound_cavern"
    private const val STREAMBOUND_CAVERN = "dbrow.pmoon_ruins_streambound_cavern"
    private const val VAULT = "dbrow.pmoon_ruins_vault"

    private const val FIRST_SPAWN_ROW = 62900

    fun parts(): DBTable =
        dbTable("dbtable.pmoon_ruins_parts", serverOnly = true) {
            column("area", AREA, VarType.STRING)
            column("zone_x", ZONE_X, VarType.INT)
            column("zone_z", ZONE_Z, VarType.INT)
            column("zones_long", ZONES_LONG, VarType.INT)
            column("camp_spot", CAMP_SPOT, VarType.COORDGRID)
            column("camp_stove", CAMP_STOVE, VarType.COORDGRID)
            column("camp_pieces", CAMP_PIECES, VarType.COORDGRID)
            column("camp_varbit", CAMP_VARBIT, VarType.STRING)

            row(ANTECHAMBER) {
                column(AREA, "area.pmoon_ruins_antechamber")
                column(ZONE_X, 176)
                column(ZONE_Z, 1200)
                column(ZONES_LONG, 8)
            }
            row(ANCIENT_PRISON) {
                column(AREA, "area.pmoon_ruins_ancient_prison")
                column(ZONE_X, 168)
                column(ZONE_Z, 1192)
                column(ZONES_LONG, 8)
                columnCoord(CAMP_SPOT, CoordGrid(1348, 9580))
                columnCoord(CAMP_STOVE, CoordGrid(1349, 9580))
                column(
                    CAMP_PIECES,
                    listOf(
                        CoordGrid(1348, 9580).packed,
                        CoordGrid(1349, 9580).packed,
                        CoordGrid(1351, 9581).packed,
                        CoordGrid(1348, 9582).packed,
                        CoordGrid(1348, 9581).packed,
                    ),
                )
                column(CAMP_VARBIT, "varbit.pmoon_camp_1")
            }
            row(EARTHBOUND_CAVERN) {
                column(AREA, "area.pmoon_ruins_earthbound_cavern")
                column(ZONE_X, 168)
                column(ZONE_Z, 1208)
                column(ZONES_LONG, 16)
                columnCoord(CAMP_SPOT, CoordGrid(1373, 9710))
                columnCoord(CAMP_STOVE, CoordGrid(1374, 9710))
                column(
                    CAMP_PIECES,
                    listOf(
                        CoordGrid(1373, 9710).packed,
                        CoordGrid(1374, 9710).packed,
                        CoordGrid(1374, 9709).packed,
                        CoordGrid(1376, 9711).packed,
                        CoordGrid(1375, 9712).packed,
                    ),
                )
                column(CAMP_VARBIT, "varbit.pmoon_camp_2")
            }
            row(STREAMBOUND_CAVERN) {
                column(AREA, "area.pmoon_ruins_streambound_cavern")
                column(ZONE_X, 184)
                column(ZONE_Z, 1208)
                column(ZONES_LONG, 8)
                columnCoord(CAMP_SPOT, CoordGrid(1509, 9693))
                columnCoord(CAMP_STOVE, CoordGrid(1510, 9693))
                column(
                    CAMP_PIECES,
                    listOf(
                        CoordGrid(1510, 9693).packed,
                        CoordGrid(1509, 9694).packed,
                        CoordGrid(1511, 9692).packed,
                        CoordGrid(1511, 9695).packed,
                        CoordGrid(1510, 9695).packed,
                    ),
                )
                column(CAMP_VARBIT, "varbit.pmoon_camp_3")
            }
            row(VAULT) {
                column(AREA, "area.pmoon_ruins_vault")
                column(ZONE_X, 184)
                column(ZONE_Z, 1192)
                column(ZONES_LONG, 8)
            }
        }

    /**
     * Npcs for the quest's private copies of the ruins. A region copy does not bring the map's
     * npcs with it, so these repeat the spawns in `.data/raw-cache/map/npcs` for the copied zones;
     * the shared world still spawns from those tomls.
     */
    fun spawns(): DBTable =
        dbTable("dbtable.pmoon_ruins_spawns", serverOnly = true) {
            column("part", SPAWN_PART, VarType.DBROW)
            column("npc", SPAWN_NPC, VarType.NPC)
            column("tile", SPAWN_TILE, VarType.COORDGRID)

            var nextRow = FIRST_SPAWN_ROW

            fun place(part: String, npc: String, vararg tiles: Pair<Int, Int>) {
                for ((x, z) in tiles) {
                    row(nextRow++) {
                        columnRSCM(SPAWN_PART, part)
                        columnRSCM(SPAWN_NPC, npc)
                        columnCoord(SPAWN_TILE, CoordGrid(x, z))
                    }
                }
            }

            place(
                ANCIENT_PRISON,
                "npc.pmoon_sulphur_nagua",
                1355 to 9563, 1358 to 9556, 1358 to 9573,
                1359 to 9550, 1359 to 9567, 1372 to 9557,
                1372 to 9563, 1375 to 9560, 1378 to 9557,
                1378 to 9563, 1350 to 9558, 1351 to 9574,
                1352 to 9552, 1352 to 9568,
            )
            place(
                EARTHBOUND_CAVERN,
                "npc.varlamore_cockatrice_moon01",
                1354 to 9697, 1355 to 9683, 1356 to 9677,
                1358 to 9681, 1358 to 9700, 1359 to 9687,
                1359 to 9694, 1352 to 9680, 1355 to 9692,
            )
            place(
                EARTHBOUND_CAVERN,
                "npc.moth_moonlight",
                1364 to 9704, 1366 to 9695, 1366 to 9700,
                1369 to 9693, 1370 to 9684, 1370 to 9697,
                1371 to 9707, 1373 to 9673, 1373 to 9681,
                1374 to 9671, 1374 to 9675, 1374 to 9685,
                1375 to 9672, 1376 to 9677, 1377 to 9685,
                1377 to 9707, 1379 to 9712, 1380 to 9696,
                1380 to 9709, 1382 to 9707, 1382 to 9710,
                1385 to 9708, 1385 to 9711, 1386 to 9680,
                1390 to 9682, 1394 to 9680, 1394 to 9698,
                1396 to 9687, 1396 to 9695, 1396 to 9701,
                1397 to 9684, 1398 to 9689, 1398 to 9706,
            )
            place(
                STREAMBOUND_CAVERN,
                "npc.moth_moonlight",
                1481 to 9680, 1482 to 9699, 1483 to 9690,
                1486 to 9679, 1497 to 9712, 1504 to 9679,
                1507 to 9716, 1509 to 9678, 1513 to 9675,
                1517 to 9710, 1519 to 9713, 1520 to 9710,
                1521 to 9715, 1523 to 9671, 1524 to 9706,
                1525 to 9674,
            )
            place(
                STREAMBOUND_CAVERN,
                "npc.grimy_lizard",
                1483 to 9716, 1489 to 9704, 1490 to 9714,
                1494 to 9711, 1498 to 9709, 1499 to 9718,
                1504 to 9714, 1510 to 9715,
            )
            place(
                STREAMBOUND_CAVERN,
                "npc.pmoon_little_frog",
                1498 to 9691, 1501 to 9682, 1504 to 9692,
                1506 to 9684, 1508 to 9688, 1517 to 9688,
                1521 to 9696, 1524 to 9687,
            )
        }
}
