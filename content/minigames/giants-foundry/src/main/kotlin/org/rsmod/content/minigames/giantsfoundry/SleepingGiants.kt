package org.rsmod.content.minigames.giantsfoundry

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.table.QuestRow
import org.rsmod.game.entity.Player

/**
 * Stage values of `sleeping_giants` and `sleeping_giants_tutorial`. The cache switches on them
 * too: the outside Kovac's multinpc (strike 0-4, talk 5-9) and the crate's multiloc (full 0-10,
 * empty 15+).
 */
internal object SleepingGiants {
    const val QUEST = "quest_sleepinggiants"

    val requiredSmithing: Int by lazy {
        val smithing = "stat.smithing".asRSCM(RSCMType.STAT)
        QuestRow.getRow("dbrow.$QUEST").requirementStats.first { it.t0.id == smithing }.t1
    }

    const val STARTED = 5
    const val ENTERED = 10
    const val REPAIRING = 15
    const val REPAIRED = 20
    const val MAKING_SWORD = 25
    const val COMPLETE = 30

    const val TUTORIAL_COMMISSION = 5
    const val TUTORIAL_CRATE_SHOWN = 10
    const val TUTORIAL_CRATE_SEARCHED = 15
    const val TUTORIAL_METAL_ADDED = 20
    const val TUTORIAL_CRUCIBLE_FULL = 25
    const val TUTORIAL_MOULD_EXPLAINED = 30
    const val TUTORIAL_MOULD_SET = 35
    const val TUTORIAL_POUR_EXPLAINED = 40
    const val TUTORIAL_POURED = 45
    const val TUTORIAL_PREFORM_TAKEN = 50
    const val TUTORIAL_HANDED_IN = 55
    const val TUTORIAL_SWORD_KEPT = 60
    const val TUTORIAL_COMPLETE = 65

    const val REPAIR_BROKEN = 0
    const val REPAIR_INSPECTED = 1
    const val REPAIR_DONE = 2
}

internal fun Player.makingQuestSword(): Boolean =
    sleepingGiants in SleepingGiants.REPAIRED until SleepingGiants.COMPLETE

internal fun Player.advanceTutorial(value: Int) {
    if (sleepingGiantsTutorial < value) {
        sleepingGiantsTutorial = value
    }
}
