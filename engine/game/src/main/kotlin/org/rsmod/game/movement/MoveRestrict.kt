package org.rsmod.game.movement

import dev.openrune.types.MoveRestrict
import org.rsmod.routefinder.collision.CollisionStrategy

public val MoveRestrict.collisionStrategy: CollisionStrategy?
    get() =
        when (this) {
            MoveRestrict.Normal -> CollisionStrategy.Normal
            MoveRestrict.Blocked -> CollisionStrategy.Blocked
            MoveRestrict.BlockedNormal -> CollisionStrategy.LineOfSight
            MoveRestrict.Indoors -> CollisionStrategy.Indoors
            MoveRestrict.Outdoors -> CollisionStrategy.Outdoors
            MoveRestrict.PassThru -> CollisionStrategy.Normal
            MoveRestrict.NoMove -> null
        }
