package org.rsmod.server.services.concurrent

/**
 * A [ScheduledService] whose [run] loop keeps going through the shutdown-signal phase, while
 * [ScheduledListenerService]s such as the game service do their expedited shutdown work, and stops
 * only when the cleanup phase cancels it.
 *
 * Use it for a service that the signal phase depends on: the game logs every player out during the
 * signal phase and only removes a player once the account saver has saved them.
 */
public interface ScheduledDrainService : ScheduledService
