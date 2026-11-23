package org.rsmod.content.travel.fairyrings.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

/**
 * Rings that need more than the network itself. `ring` is the `fairyring:id` of the destination. A
 * `quest` must be completed, or merely started when `quest_started` is set, and an `unlock_varbit`
 * must be non-zero. `message` is shown when the requirement is missing.
 */
object FairyRingRequirementsTable {
    const val RING = 0
    const val QUEST = 1
    const val QUEST_STARTED = 2
    const val UNLOCK_VARBIT = 3
    const val MESSAGE = 4

    private const val VISIT_KOUREND =
        "You can't use the fairy ring to go there until you have visited<br>Great Kourend."
    private const val VISIT_VARLAMORE =
        "You can't use the fairy ring to go there until you have visited<br>Varlamore."

    fun requirements() =
        dbTable("dbtable.fairyring_requirements", serverOnly = true) {
            column("ring", RING, VarType.INT)
            column("quest", QUEST, VarType.STRING)
            column("quest_started", QUEST_STARTED, VarType.BOOLEAN)
            column("unlock_varbit", UNLOCK_VARBIT, VarType.INT)
            column("message", MESSAGE, VarType.STRING)

            row("dbrow.fairyring_requirement_ajq") {
                column(RING, 33)
                column(QUEST, "quest_deathtothedorgeshuun")
                column(MESSAGE, "You need to complete Death to the Dorgeshuun to go there.")
            }
            row("dbrow.fairyring_requirement_bjr") {
                column(RING, 332)
                column(QUEST, "quest_holygrail")
                column(QUEST_STARTED, true)
                column(MESSAGE, "You need to have started Holy Grail to go there.")
            }
            row("dbrow.fairyring_requirement_bjs") {
                column(RING, 331)
                column(QUEST, "quest_regicide")
                column(MESSAGE, "You need to complete Regicide to go there.")
            }
            row("dbrow.fairyring_requirement_blq") {
                column(RING, 313)
                column(QUEST, "quest_landofthegoblins")
                column(QUEST_STARTED, true)
                column(MESSAGE, "You need to have started Land of the Goblins to go there.")
            }
            row("dbrow.fairyring_requirement_cip") {
                column(RING, 200)
                column(QUEST, "quest_fremenniktrials")
                column(MESSAGE, "You need to complete The Fremennik Trials to go there.")
            }
            row("dbrow.fairyring_requirement_clr") {
                column(RING, 212)
                column(QUEST, "quest_monkeymadness1")
                column(MESSAGE, "You need to complete Monkey Madness I to go there.")
            }
            row("dbrow.fairyring_requirement_dls") {
                column(RING, 111)
                column(QUEST, "quest_insearchofthemyreque")
                column(MESSAGE, "You need to complete In Search of the Myreque to go there.")
            }
            row("dbrow.fairyring_requirement_akp") {
                column(RING, 20)
                column(QUEST, "quest_beneathcursedsands")
                column(QUEST_STARTED, true)
                column(MESSAGE, "You need to have started Beneath Cursed Sands to go there.")
            }
            row("dbrow.fairyring_requirement_akr") {
                column(RING, 22)
                columnRSCM(UNLOCK_VARBIT, "varbit.zeah_playerhasvisited")
                column(MESSAGE, VISIT_KOUREND)
            }
            row("dbrow.fairyring_requirement_cir") {
                column(RING, 202)
                columnRSCM(UNLOCK_VARBIT, "varbit.zeah_playerhasvisited")
                column(MESSAGE, VISIT_KOUREND)
            }
            row("dbrow.fairyring_requirement_cis") {
                column(RING, 201)
                columnRSCM(UNLOCK_VARBIT, "varbit.zeah_playerhasvisited")
                column(MESSAGE, VISIT_KOUREND)
            }
            row("dbrow.fairyring_requirement_djr") {
                column(RING, 132)
                columnRSCM(UNLOCK_VARBIT, "varbit.zeah_playerhasvisited")
                column(MESSAGE, VISIT_KOUREND)
            }
            row("dbrow.fairyring_requirement_ais") {
                column(RING, 1)
                columnRSCM(UNLOCK_VARBIT, "varbit.varlamore_visited")
                column(MESSAGE, VISIT_VARLAMORE)
            }
            row("dbrow.fairyring_requirement_ajp") {
                column(RING, 30)
                columnRSCM(UNLOCK_VARBIT, "varbit.varlamore_visited")
                column(MESSAGE, VISIT_VARLAMORE)
            }
            row("dbrow.fairyring_requirement_ckq") {
                column(RING, 223)
                columnRSCM(UNLOCK_VARBIT, "varbit.varlamore_visited")
                column(MESSAGE, VISIT_VARLAMORE)
            }
            row("dbrow.fairyring_requirement_dlp") {
                column(RING, 110)
                columnRSCM(UNLOCK_VARBIT, "varbit.grimstone_visited")
                column(
                    MESSAGE,
                    "You can't use the fairy ring to go there until you have visited<br>Grimstone.",
                )
            }
        }
}
