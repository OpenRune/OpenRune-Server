package org.rsmod.api.registry.zone

import net.rsprot.protocol.game.outgoing.util.OpFlags
import net.rsprot.protocol.game.outgoing.zone.payload.ObjCustomise
import net.rsprot.protocol.game.outgoing.zone.payload.ObjEnabledOps
import net.rsprot.protocol.game.outgoing.zone.payload.ObjUncustomise
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.rsmod.game.obj.Obj
import org.rsmod.game.obj.ObjEntity
import org.rsmod.game.obj.ObjScope
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

class ZoneItemVisualsTest {
    private val coords = CoordGrid(3205, 3202, 0)
    private val zone = ZoneKey.from(coords)

    @Test
    fun `public visual updates are queued in the ground item's zone`() {
        val updates = ZoneUpdateMap()
        val obj = groundItem(ObjScope.Temp)
        updates.objCustomise(obj, model = 20)
        updates.objUncustomise(obj)
        updates.objEnabledOps(obj, OpFlags.NONE_SHOWN)

        assertEquals(
            listOf(
                ObjCustomise(100, 2, 20, -1, 0, -1, 0, 5, 2),
                ObjUncustomise(100, 2, 5, 2),
                ObjEnabledOps(100, OpFlags.NONE_SHOWN, 5, 2),
            ),
            updates[zone]?.toList(),
        )
    }

    @Test
    fun `private ground items never queue shared visual updates`() {
        val updates = ZoneUpdateMap()
        val obj = groundItem(ObjScope.Private)
        assertThrows(IllegalArgumentException::class.java) { updates.objCustomise(obj) }
        assertThrows(IllegalArgumentException::class.java) { updates.objUncustomise(obj) }
        assertThrows(IllegalArgumentException::class.java) {
            updates.objEnabledOps(obj, OpFlags.NONE_SHOWN)
        }
        assertNull(updates[zone])
    }

    private fun groundItem(scope: ObjScope): Obj =
        Obj(coords, ObjEntity(100, 2, scope.id), creationCycle = 0, receiverId = 1)
}
