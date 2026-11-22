package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object HunterFalconryTable {
    const val NPC = 0
    const val FALCON = 1
    const val LEVEL = 2
    const val XP = 3
    const val LOOT = 4
    const val LOW = 5
    const val HIGH = 6

    private class Kebbit(
        val key: String,
        val npc: String,
        val falcon: String,
        val level: Int,
        val xp: Int,
        val loot: List<String>,
        val low: Int,
        val high: Int,
    )

    private val KEBBITS =
        listOf(
            Kebbit("spotted", "npc.huntingbeast_speedy", "npc.hunting_falcon_onspeedy", 43, 104, listOf("obj.bones", "obj.huntingbeast_speedy_fur"), 26, 310),
            Kebbit("dark", "npc.huntingbeast_silent", "npc.hunting_falcon_onsilent", 57, 132, listOf("obj.bones", "obj.huntingbeast_silent_fur"), 0, 253),
            Kebbit(
                "dashing",
                "npc.huntingbeast_speedy2",
                "npc.hunting_falcon_onspeedy2",
                69,
                156,
                listOf("obj.bones", "obj.huntingbeast_speedy2_fur", "obj.huntingbeast_speedy2_meat"),
                0,
                205,
            ),
        )

    fun falconry() =
        dbTable("dbtable.hunter_falconry", serverOnly = true) {
            column("npc", NPC, VarType.NPC)
            column("falcon", FALCON, VarType.NPC)
            column("level", LEVEL, VarType.INT)
            column("xp", XP, VarType.INT)
            column("loot", LOOT, VarType.OBJ)
            column("low", LOW, VarType.INT)
            column("high", HIGH, VarType.INT)

            for (k in KEBBITS) {
                row("dbrow.hunter_falconry_${k.key}") {
                    columnRSCM(NPC, k.npc)
                    columnRSCM(FALCON, k.falcon)
                    column(LEVEL, k.level)
                    column(XP, k.xp)
                    column(LOOT, k.loot.map { ConstantProvider.getMapping(it) })
                    column(LOW, k.low)
                    column(HIGH, k.high)
                }
            }
        }
}
