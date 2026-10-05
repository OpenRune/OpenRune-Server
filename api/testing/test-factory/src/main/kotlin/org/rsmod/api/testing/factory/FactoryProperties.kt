package org.rsmod.api.testing.factory

import org.rsmod.api.testing.factory.entity.TestPathingEntityFactory
import org.rsmod.api.testing.factory.map.TestCollisionFactory
import org.rsmod.api.testing.factory.npc.TestNpcFactory
import org.rsmod.api.testing.factory.npc.TestNpcTypeFactory
import org.rsmod.api.testing.factory.player.TestPlayerFactory

/* Entity factory properties */
public val entityFactory: TestPathingEntityFactory
    get() = TestPathingEntityFactory()

/* Map factory properties */
public val collisionFactory: TestCollisionFactory
    get() = TestCollisionFactory()

/* Npc factory properties */
public val npcFactory: TestNpcFactory
    get() = TestNpcFactory()

public val npcTypeFactory: TestNpcTypeFactory
    get() = TestNpcTypeFactory()

/* Player factory properties */
public val playerFactory: TestPlayerFactory
    get() = TestPlayerFactory()
