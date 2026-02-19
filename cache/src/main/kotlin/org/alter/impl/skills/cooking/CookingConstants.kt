package org.alter.impl.skills.cooking

/**
 * Cooking skill constants used across recipe definitions and table builders.
 */
object CookingConstants {

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
        /** Always produced regardless of success/failure (e.g., return containers). */
        const val ALWAYS = 2
    }

    /** Default variant for single-step actions. */
    const val DEFAULT_VARIANT = 0

    /** Station mask for fire-only cooking. */
    const val STATION_FIRE = 1

    /** Station mask for range-only cooking. */
    const val STATION_RANGE = 2

    /** Station mask for cooking on both fire and range. */
    const val STATION_ANY = STATION_FIRE or STATION_RANGE

    /**
     * Modifier flags for burn chance profiles.
     * Combined as bitmasks to represent equipment/location bonuses.
     */
    object ChanceModifier {
        /** No modifier (base chance). */
        const val NONE = 0
        /** Cooking gauntlets equipped. */
        const val GAUNTLETS = 1
        /** Hosidius Kitchen range (+5% burn reduction). */
        const val HOSIDIUS_5 = 2
        /** Hosidius Kitchen range with Kourend Hard Diary (+10% burn reduction). */
        const val HOSIDIUS_10 = 4
        /** Lumbridge Castle range (reduced burn for low-level foods). */
        const val LUMBRIDGE = 8
    }
}
