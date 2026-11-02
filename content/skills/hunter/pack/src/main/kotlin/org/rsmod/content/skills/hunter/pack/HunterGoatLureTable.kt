package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object HunterGoatLureTable {
    const val COMPONENT = 0
    const val CAST_SEQ = 1
    const val CAST_SPOTANIM = 2
    const val TRAVEL = 3
    const val IMPACT = 4
    const val IMPACT_HEIGHT = 5
    const val SYNTH = 6

    fun goatLures() =
        dbTable("dbtable.hunter_goat_lures", serverOnly = true) {
            column("component", COMPONENT, VarType.COMPONENT)
            column("cast_seq", CAST_SEQ, VarType.STRING)
            column("cast_spotanim", CAST_SPOTANIM, VarType.STRING)
            column("travel", TRAVEL, VarType.STRING)
            column("impact", IMPACT, VarType.STRING)
            column("impact_height", IMPACT_HEIGHT, VarType.INT)
            column("synth", SYNTH, VarType.STRING)

            row("dbrow.hunter_goat_lure_telegrab") {
                columnRSCM(COMPONENT, "component.magic_spellbook:telegrab")
                column(CAST_SEQ, "seq.human_casttelegrab")
                column(CAST_SPOTANIM, "spotanim.telegrab_casting")
                column(TRAVEL, "spotanim.telegrab_travel")
                column(IMPACT, "spotanim.telegrab_impact")
                column(IMPACT_HEIGHT, 0)
                column(SYNTH, "synth.telegrab_all")
            }
            row("dbrow.hunter_goat_lure_dark_lure") {
                columnRSCM(COMPONENT, "component.magic_spellbook:dark_lure")
                column(CAST_SEQ, "seq.human_cast_vicious_strike")
                column(CAST_SPOTANIM, "spotanim.dark_lure_cast_spotanim")
                column(TRAVEL, "spotanim.dark_lure_travel_projanim")
                column(IMPACT, "spotanim.dark_lure_hit_spotanim")
                column(IMPACT_HEIGHT, 124)
            }
        }
}
