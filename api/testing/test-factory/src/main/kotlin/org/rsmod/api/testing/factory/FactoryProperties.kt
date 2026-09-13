package org.rsmod.api.testing.factory

import org.rsmod.api.testing.factory.entity.TestPathingEntityFactory
import org.rsmod.api.testing.factory.map.TestCollisionFactory
import org.rsmod.api.testing.factory.npc.TestNpcFactory
import org.rsmod.api.testing.factory.npc.TestNpcTypeFactory
import org.rsmod.api.testing.factory.player.TestPlayerFactory

// Being reconstructed incrementally against dev.openrune.* types; ported so far are the
// factories that either have no old-type dependency, or (npc) are needed by the first
// restored integration test module (combat-formulas). The remaining old factories (loc,
// obj, timer, controller, region, inv, font, varbit, stat) are ported per-module as each
// is restored, since several depend on classes (e.g. TimerType) that no longer exist and
// need a real consuming test to design against.

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
