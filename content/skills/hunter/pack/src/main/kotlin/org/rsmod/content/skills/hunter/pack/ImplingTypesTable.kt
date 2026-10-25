package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import org.rsmod.map.CoordGrid

object ImplingTypesTable {
    const val NPC = 0
    const val NPC_PURO_PURO = 1
    const val JAR = 2
    const val LEVEL = 3
    const val XP_PURO_PURO = 4
    const val XP_GIELINOR = 5
    const val RESPAWN_TICKS = 6

    fun implingTypes() =
        dbTable("dbtable.impling_types", serverOnly = true) {
            column("npc", NPC, VarType.NPC)
            column("npc_puro_puro", NPC_PURO_PURO, VarType.NPC)
            column("jar", JAR, VarType.OBJ)
            column("level", LEVEL, VarType.INT)
            column("xp_puro_puro", XP_PURO_PURO, VarType.INT)
            column("xp_gielinor", XP_GIELINOR, VarType.INT)
            column("respawn_ticks", RESPAWN_TICKS, VarType.INT)

            row("dbrow.impling_type_baby") {
                columnRSCM(NPC, "npc.ii_impling_type_1")
                columnRSCM(NPC_PURO_PURO, "npc.ii_impling_type_1_maze")
                columnRSCM(JAR, "obj.ii_captured_impling_1")
                column(LEVEL, 17)
                column(XP_PURO_PURO, 18)
                column(XP_GIELINOR, 20)
                column(RESPAWN_TICKS, 7)
            }

            row("dbrow.impling_type_young") {
                columnRSCM(NPC, "npc.ii_impling_type_2")
                columnRSCM(NPC_PURO_PURO, "npc.ii_impling_type_2_maze")
                columnRSCM(JAR, "obj.ii_captured_impling_2")
                column(LEVEL, 22)
                column(XP_PURO_PURO, 20)
                column(XP_GIELINOR, 22)
                column(RESPAWN_TICKS, 7)
            }

            row("dbrow.impling_type_gourmet") {
                columnRSCM(NPC, "npc.ii_impling_type_3")
                columnRSCM(NPC_PURO_PURO, "npc.ii_impling_type_3_maze")
                columnRSCM(JAR, "obj.ii_captured_impling_3")
                column(LEVEL, 28)
                column(XP_PURO_PURO, 22)
                column(XP_GIELINOR, 24)
                column(RESPAWN_TICKS, 7)
            }

            row("dbrow.impling_type_earth") {
                columnRSCM(NPC, "npc.ii_impling_type_4")
                columnRSCM(NPC_PURO_PURO, "npc.ii_impling_type_4_maze")
                columnRSCM(JAR, "obj.ii_captured_impling_4")
                column(LEVEL, 36)
                column(XP_PURO_PURO, 25)
                column(XP_GIELINOR, 27)
                column(RESPAWN_TICKS, 50)
            }

            row("dbrow.impling_type_essence") {
                columnRSCM(NPC, "npc.ii_impling_type_5")
                columnRSCM(NPC_PURO_PURO, "npc.ii_impling_type_5_maze")
                columnRSCM(JAR, "obj.ii_captured_impling_5")
                column(LEVEL, 42)
                column(XP_PURO_PURO, 27)
                column(XP_GIELINOR, 29)
                column(RESPAWN_TICKS, 50)
            }

            row("dbrow.impling_type_eclectic") {
                columnRSCM(NPC, "npc.ii_impling_type_6")
                columnRSCM(NPC_PURO_PURO, "npc.ii_impling_type_6_maze")
                columnRSCM(JAR, "obj.ii_captured_impling_6")
                column(LEVEL, 50)
                column(XP_PURO_PURO, 30)
                column(XP_GIELINOR, 32)
                column(RESPAWN_TICKS, 7)
            }

            row("dbrow.impling_type_nature") {
                columnRSCM(NPC, "npc.ii_impling_type_7")
                columnRSCM(NPC_PURO_PURO, "npc.ii_impling_type_7_maze")
                columnRSCM(JAR, "obj.ii_captured_impling_7")
                column(LEVEL, 58)
                column(XP_PURO_PURO, 34)
                column(XP_GIELINOR, 36)
                column(RESPAWN_TICKS, 7)
            }

            row("dbrow.impling_type_magpie") {
                columnRSCM(NPC, "npc.ii_impling_type_8")
                columnRSCM(NPC_PURO_PURO, "npc.ii_impling_type_8_maze")
                columnRSCM(JAR, "obj.ii_captured_impling_8")
                column(LEVEL, 65)
                column(XP_PURO_PURO, 44)
                column(XP_GIELINOR, 216)
                column(RESPAWN_TICKS, 7)
            }

            row("dbrow.impling_type_ninja") {
                columnRSCM(NPC, "npc.ii_impling_type_9")
                columnRSCM(NPC_PURO_PURO, "npc.ii_impling_type_9_maze")
                columnRSCM(JAR, "obj.ii_captured_impling_9")
                column(LEVEL, 74)
                column(XP_PURO_PURO, 50)
                column(XP_GIELINOR, 240)
                column(RESPAWN_TICKS, 7)
            }

            row("dbrow.impling_type_dragon") {
                columnRSCM(NPC, "npc.ii_impling_type_10")
                columnRSCM(NPC_PURO_PURO, "npc.ii_impling_type_10_maze")
                columnRSCM(JAR, "obj.ii_captured_impling_10")
                column(LEVEL, 83)
                column(XP_PURO_PURO, 65)
                column(XP_GIELINOR, 300)
                column(RESPAWN_TICKS, 7)
            }

            row("dbrow.impling_type_lucky") {
                columnRSCM(NPC, "npc.ii_impling_type_11")
                columnRSCM(NPC_PURO_PURO, "npc.ii_impling_type_11_maze")
                columnRSCM(JAR, "obj.ii_captured_impling_11")
                column(LEVEL, 89)
                column(XP_PURO_PURO, 80)
                column(XP_GIELINOR, 380)
                column(RESPAWN_TICKS, 7)
            }
        }
}

object CrystalImplingTable {
    const val NPCS = 0
    const val SPOTS = 1

    private val NAMES =
        listOf(
            "johnny", "junior", "andy", "joey", "trouble", "hingy", "zolty", "neil", "yanny",
            "matty", "stace", "ian", "jamie", "damo", "xander", "steveo", "stewie",
        )

    private val SPOT_COORDS =
        listOf(
            CoordGrid(3290, 6117, 0),
            CoordGrid(3307, 6041, 0),
            CoordGrid(3210, 6110, 0),
            CoordGrid(3213, 6049, 0),
            CoordGrid(3237, 6102, 0),
            CoordGrid(3308, 6117, 0),
            CoordGrid(3240, 6046, 0),
            CoordGrid(3214, 6113, 0),
            CoordGrid(3290, 6049, 0),
            CoordGrid(3234, 6072, 0),
            CoordGrid(3240, 6120, 0),
            CoordGrid(3234, 6129, 0),
        )

    fun crystalImpling() =
        dbTable("dbtable.crystal_impling", serverOnly = true) {
            column("npcs", NPCS, VarType.NPC)
            column("spots", SPOTS, VarType.COORDGRID)

            row("dbrow.crystal_impling") {
                columnRSCM(NPCS, NAMES.map { "npc.ii_impling_type_12_$it" })
                column(SPOTS, SPOT_COORDS.map { it.packed })
            }
        }
}
