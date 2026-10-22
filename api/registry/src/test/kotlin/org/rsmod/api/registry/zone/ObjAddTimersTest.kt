package org.rsmod.api.registry.zone

import net.rsprot.protocol.game.outgoing.zone.payload.ObjAdd
import net.rsprot.protocol.message.ZoneProt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.registry.zone.ZoneUpdateTransformer.ObjPrivateZoneProt
import org.rsmod.game.obj.Obj
import org.rsmod.game.obj.ObjEntity
import org.rsmod.game.obj.ObjScope
import org.rsmod.map.CoordGrid

class ObjAddTimersTest {
    private val coords = CoordGrid(0, 50, 50, 10, 10)

    private fun ownedObj(dropCycle: Int, reveal: Int, despawn: Int): Obj {
        val entity = ObjEntity(1351, 1, ObjScope.Private.id)
        val obj = Obj(coords, entity, dropCycle, receiverId = OWNER, ownerId = OWNER)
        obj.setTimers(dropCycle, reveal, despawn)
        return obj
    }

    private fun ZoneProt.objAdd(): ObjAdd =
        when (this) {
            is ZoneUpdateTransformer.PartialFollowsZoneProt -> backing as ObjAdd
            else -> this as ObjAdd
        }

    @Test
    fun `tradeable drop is sent with reveal and despawn timers to its owner`() {
        val obj = ownedObj(dropCycle = 1000, reveal = 1100, despawn = 1300)
        val prot = ZoneUpdateTransformer.toPrivateObjAddProt(obj).objAdd()
        assertEquals(100, prot.timeUntilPublic)
        assertEquals(300, prot.timeUntilDespawn)
        assertEquals(OWNERSHIP_SELF, prot.ownershipType)
        assertFalse(prot.neverBecomesPublic)
    }

    @Test
    fun `untradeable drop never becomes public`() {
        val obj = ownedObj(dropCycle = 1000, reveal = Obj.NO_TIMER, despawn = 1300)
        val prot = ZoneUpdateTransformer.toPrivateObjAddProt(obj).objAdd()
        assertEquals(0, prot.timeUntilPublic)
        assertEquals(300, prot.timeUntilDespawn)
        assertEquals(OWNERSHIP_SELF, prot.ownershipType)
        assertTrue(prot.neverBecomesPublic)
    }

    @Test
    fun `resent private drop counts both timers down`() {
        val obj = ownedObj(dropCycle = 1000, reveal = 1100, despawn = 1300)
        val prot = ZoneUpdateTransformer.toPersistentObjAdd(obj, OWNER, cycle = 1051)!!.objAdd()
        assertEquals(49, prot.timeUntilPublic)
        assertEquals(249, prot.timeUntilDespawn)
        assertEquals(OWNERSHIP_SELF, prot.ownershipType)
    }

    @Test
    fun `revealed drop is sent as owned by another player with the remaining despawn time`() {
        val obj = ownedObj(dropCycle = 1000, reveal = 1100, despawn = 1300)
        obj.reveal()
        val reveal = ZoneUpdateTransformer.toObjRevealProt(obj, cycle = 1100).objAdd()
        assertEquals(0, reveal.timeUntilPublic)
        assertEquals(200, reveal.timeUntilDespawn)
        assertEquals(OWNERSHIP_OTHER, reveal.ownershipType)
        assertFalse(reveal.neverBecomesPublic)

        val resent = ZoneUpdateTransformer.toPersistentObjAdd(obj, OWNER, cycle = 1124)!!.objAdd()
        assertEquals(176, resent.timeUntilDespawn)
        assertEquals(OWNERSHIP_OTHER, resent.ownershipType)
    }

    @Test
    fun `server obj without timers is sent without owner or timers`() {
        val entity = ObjEntity(1351, 1, ObjScope.Perm.id)
        val obj = Obj(coords, entity, 0, Obj.NULL_OBSERVER_ID)
        val prot = ZoneUpdateTransformer.toPublicObjAddProt(obj).objAdd()
        assertEquals(0, prot.timeUntilPublic)
        assertEquals(0, prot.timeUntilDespawn)
        assertEquals(OWNERSHIP_NONE, prot.ownershipType)
        assertFalse(prot.neverBecomesPublic)
    }

    @Test
    fun `private zone prots keep the original owner of the obj`() {
        val obj = ownedObj(dropCycle = 1000, reveal = 1100, despawn = 1300)
        val copy = (ZoneUpdateTransformer.toPrivateObjAddProt(obj) as ObjPrivateZoneProt).obj
        assertEquals(OWNER, copy.ownerId)
        assertEquals(OWNER, copy.receiverId)
        assertEquals(1300, copy.despawnCycle)
        assertTrue(copy.isOriginalOwner(OWNER))
    }

    private companion object {
        const val OWNER = 1L
        const val OWNERSHIP_NONE = 0
        const val OWNERSHIP_SELF = 1
        const val OWNERSHIP_OTHER = 2
    }
}
