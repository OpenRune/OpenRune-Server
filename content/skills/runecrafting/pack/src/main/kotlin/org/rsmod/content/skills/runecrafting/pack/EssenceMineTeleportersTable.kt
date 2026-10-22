package org.rsmod.content.skills.runecrafting.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.columnCoord
import org.rsmod.map.CoordGrid

object EssenceMineTeleportersTable {
    const val NPC = 0
    const val EXIT = 1

    fun teleporters() =
        dbTable("dbtable.essence_mine_teleporters", serverOnly = true) {
            column("npc", NPC, VarType.NPC)
            column("exit", EXIT, VarType.COORDGRID)

            row("dbrow.essence_mine_teleporter_aubury") {
                columnRSCM(NPC, "npc.aubury")
                columnCoord(EXIT, CoordGrid(3253, 3401, 0))
            }

            row("dbrow.essence_mine_teleporter_sedridor") {
                columnRSCM(NPC, "npc.head_wizard")
                columnCoord(EXIT, CoordGrid(3105, 9572, 0))
            }
        }
}
