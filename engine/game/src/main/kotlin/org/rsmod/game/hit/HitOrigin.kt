package org.rsmod.game.hit

import org.rsmod.game.entity.Player

/**
 * Says where a player's hit came from when its type and objs cannot: an extra hit from a relic looks
 * the same as a normal attack otherwise. Content implements this, usually with an enum.
 */
public interface HitOrigin

/**
 * Every hit [this] player queues during [action] carries [origin], including hits queued inside a
 * weapon's own attack handler. It only covers queueing: a hit that lands later keeps its origin, but
 * a hit queued from a later callback needs its own scope.
 */
public inline fun <T> Player.withHitOrigin(origin: HitOrigin, action: () -> T): T {
    val previous = hitOrigin
    hitOrigin = origin
    try {
        return action()
    } finally {
        hitOrigin = previous
    }
}
