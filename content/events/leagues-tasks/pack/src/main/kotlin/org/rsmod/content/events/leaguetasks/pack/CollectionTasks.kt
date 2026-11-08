package org.rsmod.content.events.leaguetasks.pack

import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.COLLECTION
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.VALUE

internal fun TriggerRows.collectionTasks() {
    for (slots in listOf(5, 15, 30, 50, 100, 200, 350, 500, 750)) {
        slots("league_task_${slots}_collection_log_slots", slots)
    }

    clueSlots("easy", 5, 20, 50)
    clueSlots("medium", 5, 20, 40)
    clueSlots("hard", 3, 15, 30)
    clueSlots("elite", 3, 10, 25)
    clueSlots("master", 5, 25)
}

/**
 * The clue tasks predate the "(Rare)" pages that Jagex later split out of the hard, elite and master
 * pages, so a tier's rare page counts toward its section.
 */
private fun TriggerRows.clueSlots(tier: String, vararg counts: Int) {
    val pages =
        listOfNotNull(
            "dbrow.collection_log_category_${tier}_treasure_trails",
            "dbrow.collection_log_category_${tier}_treasure_trails_rare".takeIf {
                tier in RARE_TIERS
            },
        )
    for (count in counts) {
        slots("league_task_fill_${count}_${tier}_clue_collection_log_slots", count, pages)
    }
}

private val RARE_TIERS = setOf("hard", "elite", "master")

private fun TriggerRows.slots(task: String, slots: Int, pages: List<String> = emptyList()) {
    trigger("dbrow.$task", LeagueTaskTrigger.CollectionSlots) {
        column(VALUE, slots)
        if (pages.isNotEmpty()) {
            columnRSCM(COLLECTION, pages)
        }
    }
}
