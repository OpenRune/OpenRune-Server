package org.rsmod.content.skills.construction

import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.instances.InstanceAttributes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.table.FurnitureRow
import org.rsmod.game.entity.Player
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneGrid

private val HOUSE_LAYOUT: AttributeKey<String> = AttributeKey(persistenceKey = "poh_layout")
private val HOUSE_SESSION: AttributeKey<HouseSession> = AttributeKey(temp = true)

var Player.buildMode by boolVarBit(VARBIT_BUILD_MODE)

val Player.houseSession: HouseSession?
    get() = attr[HOUSE_SESSION]

fun Player.startHouseSession(session: HouseSession, exit: CoordGrid) {
    attr[HOUSE_SESSION] = session
    attr[InstanceAttributes.LOGIN_EXIT_COORD] = exit.packed
}

fun Player.layout(): HouseLayout = HouseLayout.decode(attr[HOUSE_LAYOUT])

fun Player.storeLayout(layout: HouseLayout) {
    attr[HOUSE_LAYOUT] = layout.encode()
}

/** Every way out of a house ends the visit, however it was left. */
fun ProtectedAccess.leaveHouse(destination: CoordGrid) {
    player.attr.remove(HOUSE_SESSION)
    player.attr.remove(InstanceAttributes.LOGIN_EXIT_COORD)
    player.buildMode = false
    telejump(destination)
}

class HotspotTarget(val slot: RoomSlot, val rotation: Int, val hotspot: HotspotDef)

class DoorTarget(val slot: RoomSlot, val direction: Direction)

class HouseSession(val region: Region, val layout: HouseLayout) {
    fun slotAt(coords: CoordGrid): RoomSlot? {
        val x = (coords.x - region.southWest.x) / ZoneGrid.LENGTH - HOUSE_GRID_OFFSET
        val z = (coords.z - region.southWest.z) / ZoneGrid.LENGTH - HOUSE_GRID_OFFSET
        return RoomSlot.of(coords.level, x, z)
    }
}

fun FurnitureRow.buildLevel(): Int = levelRequirement.firstOrNull()?.t1 ?: 1

fun FurnitureRow.materials(): List<Pair<String, Int>> =
    materialCost.map { it.t0.internalName to it.t1 }

fun FurnitureRow.materialText(): String =
    materialCost.joinToString(", ") { "${it.t1} x ${it.t0.name}" }
