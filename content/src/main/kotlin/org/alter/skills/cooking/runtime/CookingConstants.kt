package org.alter.skills.cooking.runtime

/**
 * Trigger types determine how a cooking action is initiated.
 */
object Trigger {
    /** Action triggered by using item on a heat source (fire/range). */
    const val HEAT_SOURCE = 0
    /** Action triggered by using one item on another in inventory. */
    const val ITEM_ON_ITEM = 1
}

/**
 * Outcome kinds determine what type of result a cooking action produces.
 */
object OutcomeKind {
    /** Successful cooking result. */
    const val SUCCESS = 0
    /** Failed cooking result (burnt). */
    const val FAIL = 1
    /** Always produced regardless of success/failure. */
    const val ALWAYS = 2
}
