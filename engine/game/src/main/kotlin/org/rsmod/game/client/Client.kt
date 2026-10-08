package org.rsmod.game.client

import org.rsmod.game.entity.Player

public interface Client<S, T> {
    public fun close()

    public fun write(message: T)

    public fun read(player: Player)

    public fun flush()

    public fun flushHighPriority()

    public fun unregister(service: S, player: Player)
}

public interface ClientCycle {
    public fun update(player: Player)

    public fun flush(player: Player)

    public fun release()

    /**
     * Marks the client's scene stale so the next [update] resends it in full, even though the player
     * has not moved.
     *
     * The scene is normally only resent when the build area changes. A world-type switch replaces
     * the player's save in place, so the world around them can be entirely different while they
     * stand on the same tile - without this the client would keep showing the mode they left.
     */
    public fun forceSceneRebuild()
}
