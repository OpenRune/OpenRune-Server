package org.rsmod.content.skills.construction

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.instances.InstanceAttributes
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.constructionLvl
import org.rsmod.api.player.ui.ifOpenMainModal
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc5
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.table.FurnitureRow
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ConstructionScript
@Inject
constructor(
    private val catalogue: ConstructionCatalogue,
    private val houses: HouseRegions,
    private val locRepo: LocRepository,
    private val xpMods: XpModifiers,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    private val logger = InlineLogger()

    private var Player.buildMode by boolVarBit(VARBIT_BUILD_MODE)

    override fun ScriptContext.startup() {
        for (portal in TOWN_PORTALS) {
            onOpLoc1(portal) { enterHouse(buildMode = false) }
            onOpLoc2(portal) { enterHouse(buildMode = true) }
        }
        onOpLoc1(EXIT_PORTAL) { leaveHouse() }

        for (locId in catalogue.hotspotLocIds()) {
            val type = ServerCacheManager.getObject(locId) ?: continue
            onOpLoc1(type) { openBuildMenu(it.loc.coords) }
            onOpLoc5(type) { removeFurniture(it.loc.coords) }
        }

        for (locId in catalogue.doorLocIds()) {
            val type = ServerCacheManager.getObject(locId) ?: continue
            onOpLoc1(type) { openRoomMenu(it.loc.coords) }
        }

        onCommand("house") {
            desc = "Enter your player-owned house"
            cheat { protectedAccess.launch(player) { enterHouse(buildMode = false) } }
        }
        onCommand("buildmode") {
            desc = "Enter your player-owned house in building mode"
            cheat { protectedAccess.launch(player) { enterHouse(buildMode = true) } }
        }
        onCommand("pohrooms") {
            desc = "Dump what the cache says about house rooms and their hotspots"
            cheat { dumpRooms(args.getOrNull(0)) }
        }
    }

    private fun Cheat.dumpRooms(filter: String?) {
        val rooms =
            catalogue.all().filter { filter == null || it.row.name.contains(filter, true) }
        player.mes("${rooms.size} room(s) loaded from dbtable.poh_room:")
        for (room in rooms.sortedBy { it.row.name }) {
            player.mes(
                "${room.row.name} lvl=${room.levelRequirement} cost=${room.cost} " +
                    "zone=${room.sourceZone.x},${room.sourceZone.z} " +
                    "doors=${room.doorsAfter(0)} hotspots=${room.hotspots.size}/${room.row.hotspot.size}"
            )
            for (hotspot in room.hotspots) {
                val part = hotspot.primary
                val loc = RSCM.getReverseMapping(RSCMType.LOC, part.locId)
                player.mes(
                    "  [${hotspot.index + 1}] ${part.localX},${part.localZ} $loc " +
                        "x${hotspot.parts.size} builds=${hotspot.builds.joinToString { it.name }}"
                )
            }
        }
    }

    private fun ProtectedAccess.enterHouse(buildMode: Boolean) {
        val layout = player.layout()
        if (layout.rooms.isEmpty()) {
            starterLayout(layout)
            player.storeLayout(layout)
        }
        val region = houses.allocate(layout)
        if (region == null) {
            mes("There is no space for your house right now. Try again shortly.")
            return
        }
        val session = HouseSession(region, layout)
        player.attr[SESSION] = session
        player.attr[InstanceAttributes.LOGIN_EXIT_COORD] = EXIT_COORDS.packed
        player.buildMode = buildMode
        applyFurniture(session)
        player.coords = entranceCoords(session)
    }

    private fun ProtectedAccess.leaveHouse() {
        player.attr.remove(SESSION)
        player.attr.remove(InstanceAttributes.LOGIN_EXIT_COORD)
        player.buildMode = false
        player.coords = EXIT_COORDS
    }

    private fun starterLayout(layout: HouseLayout) {
        val garden = catalogue.all().firstOrNull { it.row.name == "garden" } ?: return
        layout.place(slotKey(LEVEL_GROUND, 4, 4), garden.id, rotation = 0)
    }

    private fun entranceCoords(session: HouseSession): CoordGrid {
        val slot =
            session.layout.rooms.keys.firstOrNull { slotLevel(it) == LEVEL_GROUND }
                ?: session.layout.rooms.keys.first()
        return houses.roomBase(session.region, slot).translate(3, 3)
    }

    private suspend fun ProtectedAccess.openBuildMenu(coords: CoordGrid) {
        val session = player.attr[SESSION] ?: return
        if (!player.buildMode) {
            mes("You need to be in building mode to do that.")
            return
        }
        val target = session.resolve(catalogue, houses, coords) ?: return
        if (session.layout.built(target.slot, target.hotspot.index) != null) {
            mes("You need to remove the existing furniture first.")
            return
        }
        if (!inv.contains(OBJ_SAW) || !inv.contains(OBJ_HAMMER)) {
            mes("You need a hammer and a saw to build furniture.")
            return
        }

        val builds = target.hotspot.builds.filter { it.hiddenInBuildMenu != 1 }
        if (builds.isEmpty()) {
            return
        }

        ifOpenMainModal(INTERFACE_FURNITURE)
        ifSetEvents(COMPONENT_FURNITURE_CONTENTS, 0 until FURNITURE_SLOTS, IfEvent.PauseButton)
        for (slot in 0 until FURNITURE_SLOTS) {
            val furniture = builds.getOrNull(slot)
            if (furniture == null) {
                player.runClientScript(entryScript, slot + 1, 0, -1, "", 0)
                continue
            }
            player.runClientScript(
                entryScript,
                slot + 1,
                furniture.rowId,
                furniture.buildLevel(),
                furniture.materialText(),
                if (canBuild(furniture)) 1 else 0,
            )
        }

        val furniture = builds.getOrNull(pauseButton().subcomponent - 1) ?: return
        buildFurniture(session, target, furniture)
    }

    private suspend fun ProtectedAccess.buildFurniture(
        session: HouseSession,
        target: HotspotTarget,
        furniture: FurnitureRow,
    ) {
        val required = furniture.buildLevel()
        if (player.constructionLvl < required) {
            mes("You need a Construction level of $required to build that.")
            return
        }
        val materials = furniture.materials()
        if (materials.any { inv.count(it.first) < it.second }) {
            mes("You don't have the materials to build that.")
            return
        }

        anim(SEQ_BUILD)
        delay(BUILD_TICKS)
        for ((obj, count) in materials) {
            if (invDel(inv, obj, count).failure) {
                resetAnim()
                return
            }
        }
        resetAnim()

        session.layout.build(target.slot, target.hotspot.index, furniture.rowId)
        player.storeLayout(session.layout)
        spawnFurniture(session, target.slot, target.rotation, target.hotspot, furniture.rowId)
        statAdvance(STAT_CONSTRUCTION, furniture.xp() * xpMods.get(player, STAT_CONSTRUCTION))
        spam("You build a ${furniture.name}.")
    }

    private suspend fun ProtectedAccess.removeFurniture(coords: CoordGrid) {
        val session = player.attr[SESSION] ?: return
        if (!player.buildMode) {
            mes("You need to be in building mode to do that.")
            return
        }
        val target = session.resolve(catalogue, houses, coords) ?: return
        val built = session.layout.built(target.slot, target.hotspot.index) ?: return
        if (menu("Really remove it?", hotkeys = false, choices = listOf("Yes", "No")) != 0) {
            return
        }
        anim(SEQ_BUILD)
        delay(BUILD_TICKS)
        resetAnim()
        session.layout.demolish(target.slot, target.hotspot.index)
        player.storeLayout(session.layout)
        despawnFurniture(session, target, built)
        spam("You remove the furniture.")
    }

    private suspend fun ProtectedAccess.openRoomMenu(coords: CoordGrid) {
        val session = player.attr[SESSION] ?: return
        if (!player.buildMode) {
            mes("You need to be in building mode to do that.")
            return
        }
        val door = session.resolveDoor(catalogue, houses, coords) ?: return
        val destination = neighbour(door.slot, door.direction)
        if (destination == null || destination in session.layout.rooms) {
            mes("You can't build a room there.")
            return
        }

        val facing = (door.direction + 2) and 3
        val options =
            catalogue
                .all()
                .filter { it.levelRequirement <= player.constructionLvl }
                .filter { def -> (0..3).any { facing in def.doorsAfter(it) } }
                .sortedBy { it.cost }
        if (options.isEmpty()) {
            mes("You don't have the Construction level to build any rooms yet.")
            return
        }

        // ponytail: a chat menu stands in for interface.poh_add_room, whose room grid is driven by
        // client-side varcs the server does not set yet. Swap it out once that contract is known.
        val labels = options.map { "${it.name} (${it.cost} coins)" } + "Cancel"
        val room = options.getOrNull(menu("Build a room", hotkeys = false, choices = labels)) ?: return

        if (inv.count(OBJ_COINS) < room.cost) {
            mes("You need ${room.cost} coins to build a ${room.name}.")
            return
        }
        val rotation = (0..3).firstOrNull { facing in room.doorsAfter(it) } ?: return
        if (invDel(inv, OBJ_COINS, room.cost).failure) {
            return
        }

        session.layout.place(destination, room.id, rotation)
        player.storeLayout(session.layout)
        mes("You build a ${room.name}. Re-enter your house to walk into it.")
    }

    private fun applyFurniture(session: HouseSession) {
        for ((slot, placed) in session.layout.rooms) {
            val def = catalogue.room(placed.room) ?: continue
            for (hotspot in def.hotspots) {
                val row = session.layout.built(slot, hotspot.index) ?: continue
                spawnFurniture(session, slot, placed.rotation, hotspot, row)
            }
        }
    }

    private fun spawnFurniture(
        session: HouseSession,
        slot: Int,
        rotation: Int,
        hotspot: HotspotDef,
        furnitureRow: Int,
    ) {
        for (loc in builtLocs(session, slot, rotation, hotspot, furnitureRow)) {
            locRepo.add(loc, duration = HOUSE_LOC_DURATION)
        }
    }

    private fun despawnFurniture(session: HouseSession, target: HotspotTarget, furnitureRow: Int) {
        val locs =
            builtLocs(session, target.slot, target.rotation, target.hotspot, furnitureRow)
        for (loc in locs) {
            locRepo.del(loc, duration = HOUSE_LOC_DURATION)
        }
    }

    /**
     * A multi-tile piece covers every part of its hotspot, each with the loc for that part, so a rug
     * lays its corners and sides rather than one tile at the hotspot's anchor.
     */
    private fun builtLocs(
        session: HouseSession,
        slot: Int,
        rotation: Int,
        hotspot: HotspotDef,
        furnitureRow: Int,
    ): List<LocInfo> {
        val furniture = hotspot.builds.firstOrNull { it.rowId == furnitureRow } ?: return emptyList()
        if (catalogue.builtLocIds(furniture).isEmpty()) {
            logger.warn { "Furniture '${furniture.name}' has no matching loc to place." }
            return emptyList()
        }
        return hotspot.parts.mapNotNull { part ->
            val locId = catalogue.builtLocFor(furniture, part) ?: return@mapNotNull null
            val coords = houses.partCoords(session.region, slot, rotation, part)
            LocInfo(
                part.layer,
                coords,
                LocEntity(locId, part.shapeId, (part.angleId + rotation) and 3),
            )
        }
    }

    private fun ProtectedAccess.canBuild(furniture: FurnitureRow): Boolean {
        if (player.constructionLvl < furniture.buildLevel()) {
            return false
        }
        return furniture.materials().all { (obj, count) -> inv.count(obj) >= count }
    }

    private fun Player.layout(): HouseLayout = HouseLayout.decode(attr[LAYOUT])

    private fun Player.storeLayout(layout: HouseLayout) {
        attr[LAYOUT] = layout.encode()
    }

    private companion object {
        const val BUILD_TICKS = 3
        const val HOUSE_LOC_DURATION = Int.MAX_VALUE

        val EXIT_COORDS = CoordGrid(2953, 3224, 0)

        val LAYOUT: AttributeKey<String> = AttributeKey(persistenceKey = "poh_layout")
        val SESSION: AttributeKey<HouseSession> = AttributeKey(temp = true)
    }
}
