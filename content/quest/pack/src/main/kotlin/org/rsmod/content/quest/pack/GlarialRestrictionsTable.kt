package org.rsmod.content.quest.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object GlarialRestrictionsTable {
    const val ENTRIES = 0

    fun restrictions() =
        dbTable("dbtable.glarial_restrictions", serverOnly = true) {
            column("entries", ENTRIES, VarType.STRING)

            row("dbrow.glarial_forbidden_names") {
                column(
                    ENTRIES,
                    "logs",
                    "knife",
                    "fletching knife",
                    "needle",
                    "thread",
                    "ball of wool",
                    "leather",
                    "hard leather",
                    "snakeskin",
                    "nails",
                    "feather",
                    "bow string",
                    "arrow shaft",
                    "headless arrow",
                    "looting bag",
                    "magic secateurs",
                    "mythical cape",
                    "fancy boots",
                    "fighting boots",
                    "fancier boots",
                    "spotted cape",
                    "spottier cape",
                )
            }
            row("dbrow.glarial_forbidden_parts") {
                column(
                    ENTRIES,
                    " logs",
                    "arrowtips",
                    "clue scroll",
                    "(u)",
                    "dragon leather",
                    "cannon base",
                    "cannon stand",
                    "cannon barrels",
                    "cannon furnace",
                    "dwarf cannon set",
                    "rune pack",
                    "feather pack",
                    "initiate harness",
                    "proselyte harness",
                    "ardougne cloak",
                    " cape(t)",
                    " camo top",
                    " camo legs",
                )
            }
            row("dbrow.glarial_allowed_parts") {
                column(
                    ENTRIES,
                    "tome of ",
                    "imcando hammer (off-hand)",
                    "chronicle",
                    "goblin paint cannon",
                )
            }
        }
}
