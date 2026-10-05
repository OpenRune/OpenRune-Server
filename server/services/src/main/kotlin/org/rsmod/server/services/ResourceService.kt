package org.rsmod.server.services

/**
 * A [Service] that owns a resource other services still use during their own [Service.shutdown],
 * such as the game database that the account saver writes the final player saves to.
 *
 * [ServiceManager] shuts every other service down first and only then shuts down the
 * [ResourceService]s, so that work drained at shutdown can still reach the resource.
 */
public interface ResourceService : Service
