package org.rsmod.content.minigames.giantsfoundry

import org.rsmod.game.entity.Player

/**
 * Sleeping Giants stage values. The quest varbit (`sleeping_giants`, end state 30 in the quest
 * dbrow) and the tutorial varbit (`sleeping_giants_tutorial`) use the values RuneLite's Quest
 * Helper checks (`SleepingGiants.java`, github.com/Zoinkwiz/quest-helper); the outside Kovac's
 * multinpc (strike 0-4, talk 5-9) and the crate's multiloc (full 0-10, empty 15+) agree.
 * https://oldschool.runescape.wiki/w/Sleeping_Giants
 */
internal object SleepingGiants {
    const val QUEST = "quest_sleepinggiants"

    const val NOT_STARTED = 0

    /** Agreed to help Kovac outside the cave. */
    const val STARTED = 5

    /** Followed Kovac into the foundry. */
    const val ENTERED = 10

    /** Kovac asked for the tools to be fixed. */
    const val REPAIRING = 15

    /** Tools repaired; the sword is made in the real foundry from here on. */
    const val REPAIRED = 20

    /** Kovac has set the sword to make. */
    const val MAKING_SWORD = 25

    const val COMPLETE = 30

    const val TUTORIAL_COMMISSION = 10
    const val TUTORIAL_CRATE_SEARCHED = 15
    const val TUTORIAL_CRUCIBLE_FULL = 25
    const val TUTORIAL_MOULD_EXPLAINED = 30
    const val TUTORIAL_MOULD_SET = 35
    const val TUTORIAL_POUR_EXPLAINED = 40
    const val TUTORIAL_POURED = 45
    const val TUTORIAL_PREFORM_TAKEN = 50
    const val TUTORIAL_HANDED_IN = 55

    const val REPAIR_BROKEN = 0
    const val REPAIR_INSPECTED = 1
    const val REPAIR_DONE = 2
}

/** True while the quest's tutorial sword is being made (stages 20-25). */
internal fun Player.makingQuestSword(): Boolean =
    sleepingGiants in SleepingGiants.REPAIRED until SleepingGiants.COMPLETE

/** Moves the tutorial forward to [value]; never back, so a step can't be repeated. */
internal fun Player.advanceTutorial(value: Int) {
    if (sleepingGiantsTutorial < value) {
        sleepingGiantsTutorial = value
    }
}
