package org.rsmod.api.game.process.player

import net.rsprot.protocol.game.outgoing.util.OpFlags
import net.rsprot.protocol.game.outgoing.zone.payload.LocAddChangeV2
import net.rsprot.protocol.game.outgoing.zone.payload.ObjDel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.game.obj.Obj
import org.rsmod.game.obj.ObjEntity
import org.rsmod.game.obj.ObjScope
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

class PostTickDeliveryTest {
    @Test fun privateExpiryReachesOnlyOwnerOnExactExpiryTickWithoutExtraLifetimeOrRepeat() {
        var remaining = 3
        var owned = true
        var ownerCalls = 0
        val obj = Obj(CoordGrid(2561, 3222), ObjEntity(11849, 1, ObjScope.Private.id), 0, 7L, 7L)
        val f = LateZoneUpdateTest.Fixture(count = 2, hook = { player, updates ->
            if (player.observerUUID == 7L) {
                ownerCalls++
                if (owned && --remaining == 0) {
                    owned = false
                    updates.objDel(obj)
                }
            }
        })
        f.players[0].observerUUID = 7L; f.players[1].observerUUID = 8L
        repeat(2) {
            f.tick()
            assertTrue(owned)
            assertTrue(f.clients.all { it.enclosed.isEmpty() })
        }
        f.tick()
        assertFalse(owned); assertEquals(0, remaining); assertEquals(3, ownerCalls)
        assertArrayEquals(LateZoneUpdateTest.encoded(ObjDel(11849, 1, 1, 6)), f.clients[0].enclosed.single())
        assertTrue(f.clients[1].enclosed.isEmpty())
        f.tick()
        assertEquals(4, ownerCalls); assertEquals(0, remaining)
        assertTrue(f.clients.all { it.enclosed.isEmpty() })
    }

    @Test fun sharedSceneryCreatedByHookReachesEveryExistingObserverOnce() {
        var created = false
        val f = LateZoneUpdateTest.Fixture(count = 2, hook = { _, updates ->
            if (!created) {
                created = true
                updates.locAdd(org.rsmod.game.loc.LocInfo(2, CoordGrid(2561, 3222), org.rsmod.game.loc.LocEntity(16680, 10, 0)))
            }
        })
        f.tick()
        val expected = LateZoneUpdateTest.encoded(LocAddChangeV2(16680, 1, 6, 10, 0, OpFlags.ALL_SHOWN))
        for (client in f.clients)assertArrayEquals(expected, client.enclosed.single())
        f.tick()
        assertTrue(f.clients.all { it.enclosed.isEmpty() })
    }

    @Test fun hookActorContextIsScopedAndOneFailureDoesNotSkipOtherActors() {
        val called = mutableListOf<org.rsmod.game.entity.Player>()
        val f = LateZoneUpdateTest.Fixture(count = 2, hook = { player, _ ->
            assertSame(player, org.rsmod.game.entity.PlayerPersistenceHints.activeOrNull())
            called += player
            if (player.observerUUID == 7L)throw IllegalStateException("expected hook failure")
        })
        f.players[0].observerUUID = 7L; f.players[1].observerUUID = 8L
        f.tick(expectedFailures = 1)
        assertEquals(f.players, called)
        assertTrue(f.players[0].forceDisconnect)
        assertFalse(f.players[1].forceDisconnect)
        assertNull(org.rsmod.game.entity.PlayerPersistenceHints.activeOrNull())
    }

    @Test fun expiryDuringNewZoneResetNeedsNoDuplicateTransientDelete() {
        var owned = true
        var calls = 0
        val obj = Obj(CoordGrid(2561, 3222), ObjEntity(11849, 1, ObjScope.Private.id), 0, 7L, 7L)
        val f = LateZoneUpdateTest.Fixture(hook = { _, updates ->
            calls++
            if (owned) { owned = false; updates.objDel(obj) }
        })
        val player = f.players.single()
        player.observerUUID = 7L
        player.lastProcessedZone = ZoneKey.NULL
        player.visibleZoneKeys.clear()
        f.tick()
        assertFalse(owned); assertEquals(1, calls)
        assertTrue(f.clients.single().enclosed.isEmpty())
        f.tick()
        assertEquals(2, calls); assertTrue(f.clients.single().enclosed.isEmpty())
    }
}
