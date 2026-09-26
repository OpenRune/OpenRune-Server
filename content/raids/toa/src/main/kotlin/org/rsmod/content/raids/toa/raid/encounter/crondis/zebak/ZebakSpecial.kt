package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

/**
 * A special attack, advanced once a tick by the room. Autos carry on meanwhile (at [attackSpeed]
 * if set); blood magic pauses and no other special starts.
 */
internal interface ZebakSpecial {
    /** Overrides Zebak's attack speed while this runs; `null` keeps it. */
    val attackSpeed: Int?

    /** Runs the next tick (the first call is tick 0). `false` once the special is over. */
    fun step(): Boolean
}
