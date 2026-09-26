package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

/**
 * A special attack, advanced once a tick by the room. Autos carry on meanwhile (a special can move
 * the next one through `attackCountdown`); blood magic pauses and no other special starts.
 */
internal interface ZebakSpecial {
    /** Runs the next tick (the first call is tick 0). `false` once the special is over. */
    fun step(): Boolean
}
