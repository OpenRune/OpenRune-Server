package org.rsmod.content.skills.construction

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.constructionLvl
import org.rsmod.api.player.ui.ifOpenMainModal
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.ui.ifSetHide
import org.rsmod.api.player.ui.ifSetPosition
import org.rsmod.api.player.ui.ifSetText
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc5
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.table.FurnitureRow
import org.rsmod.game.cheat.Cheat
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
    private val portals: PohPortals,
    private val locRepo: LocRepository,
    private val xpMods: XpModifiers,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    private val logger = InlineLogger()

    override fun ScriptContext.startup() {
        catalogue.load()

        for (portal in TOWN_PORTALS) {
            onOpLoc1(portal) { enterHouse(buildMode = vars[VARBIT_DEFAULT_BUILD_MODE] == 1) }
            onOpLoc2(portal) { enterHouse(buildMode = false) }
            onOpLoc3(portal) { enterHouse(buildMode = true) }
        }
        onOpLoc1(EXIT_PORTAL) { leaveHouse(EXIT_COORDS) }

        onIfOpen(INTERFACE_HOUSE_OPTIONS) {
            player.ifSetText(COMPONENT_ROOM_COUNT, "Number of rooms: ${player.layout().rooms.size}")
        }
        onIfModalButton(COMPONENT_BUILD_MODE_ON) { setBuildMode(on = true) }
        onIfModalButton(COMPONENT_BUILD_MODE_OFF) { setBuildMode(on = false) }
        onIfModalButton(COMPONENT_LEAVE_HOUSE) { leaveFromOptions() }
        onIfModalButton(COMPONENT_TELE_INSIDE) { vars[VARBIT_TELE_OUTSIDE] = 0 }
        onIfModalButton(COMPONENT_TELE_OUTSIDE) { vars[VARBIT_TELE_OUTSIDE] = 1 }
        onIfModalButton(COMPONENT_DEFAULT_BUILD_ON) { vars[VARBIT_DEFAULT_BUILD_MODE] = 1 }
        onIfModalButton(COMPONENT_DEFAULT_BUILD_OFF) { vars[VARBIT_DEFAULT_BUILD_MODE] = 0 }

        // Every house hotspot, doorway and built piece carries its option at op5: a hotspot is
        // `ops=[4=Build]` and a built piece `ops=[.., 4=Remove]`. Binding op1 never fires, and
        // binding Remove to the hotspot's op5 eats the Build click instead.
        for (locId in catalogue.hotspotLocIds()) {
            val type = ServerCacheManager.getObject(locId) ?: continue
            onOpLoc5(type) { openBuildMenu(it.loc.coords, it.loc.id) }
        }

        for (locId in catalogue.furnitureLocIds()) {
            val type = ServerCacheManager.getObject(locId) ?: continue
            onOpLoc5(type) { removeFurniture(it.loc.coords, it.loc.id) }
        }

        for (destination in portals.destinations()) {
            val coord = destination.coord ?: continue
            for (locId in destination.portalLocs) {
                val type = ServerCacheManager.getObject(locId) ?: continue
                onOpLoc1(type) { enterPortal(coord) }
            }
        }

        for (locId in catalogue.doorLocIds()) {
            val type = ServerCacheManager.getObject(locId) ?: continue
            onOpLoc5(type) { openRoomMenu(it.loc.coords) }
        }

        onCommand("house") {
            desc = "Enter your player-owned house"
            cheat { protectedAccess.launch(player) { enterHouse(buildMode = false) } }
        }
        onCommand("buildmode") {
            desc = "Enter your player-owned house in building mode"
            cheat { protectedAccess.launch(player) { enterHouse(buildMode = true) } }
        }
        onPlayerQueueWithArgs<BuildTask>(QUEUE_BUILD) { finishBuild(it.args) }
        onPlayerQueueWithArgs<RemoveTask>(QUEUE_REMOVE) { finishRemove(it.args) }

        onCommand("pohwhere") {
            desc = "Report how the tile under you resolves to a room slot and hotspot"
            cheat { dumpWhere() }
        }
        onCommand("pohrooms") {
            desc = "Dump what the cache says about house rooms and their hotspots"
            cheat { dumpRooms(args.getOrNull(0)) }
        }
    }

    /**
     * Every refusal in [openBuildMenu] is a silent return, so a hotspot that will not open gives
     * nothing to go on. This walks the same chain out loud.
     */
    private fun Cheat.dumpWhere() {
        val coords = player.coords
        val session = player.houseSession
        if (session == null) {
            player.mes("No house session: you are not inside your house.")
            return
        }
        player.mes("at $coords  region sw=${session.region.southWest} buildMode=${player.buildMode}")
        val slot = session.slotAt(coords)
        if (slot == null) {
            player.mes("slotAt: no grid slot for this tile.")
            return
        }
        val placed = session.layout.placed(slot)
        player.mes("slot=${slot.packed} placed=${placed?.room} rotation=${placed?.rotation}")
        val room = placed?.room?.let(catalogue::room)
        if (room == null) {
            player.mes("No room placed in this slot.")
            return
        }
        player.mes("room='${room.row.name}' hotspots=${room.hotspots.size}")
        val target = houses.hotspotAt(session, coords)
        if (target == null) {
            player.mes("resolve: this tile is not on any hotspot of that room.")
        } else {
            val spot = target.hotspot
            val visible = spot.builds.filter { it.hiddenInBuildMenu != 1 }
            player.mes(
                "resolve -> slot=${target.slot.packed} hotspot=${spot.index + 1} " +
                    "builds=${spot.builds.size} visibleInMenu=${visible.size} " +
                    "alreadyBuilt=${session.layout.built(target.slot, spot.index)} " +
                    "saw=${player.inv.contains(OBJ_SAW)} hammer=${player.inv.contains(OBJ_HAMMER)}"
            )
        }
        for (spot in room.hotspots) {
            val where =
                spot.parts.joinToString {
                    houses.localCoords(session.region, slot, placed.rotation, it.localX, it.localZ)
                        .toString()
                }
            player.mes("  [${spot.index + 1}] $where")
        }
        val door = houses.doorAt(session, coords)
        if (door == null) {
            player.mes("door: this tile is not a doorway of that room.")
        } else {
            val target = door.slot.neighbour(door.direction)
            player.mes(
                "door -> dir=${door.direction.value} neighbour=${target?.packed} " +
                    "occupiedBy=${target?.let { session.layout.placed(it)?.room }}"
            )
        }
        for (entry in room.doors) {
            val at =
                houses.localCoords(session.region, slot, placed.rotation, entry.localX, entry.localZ)
            player.mes("  door dir=${entry.direction.value} ${entry.localX},${entry.localZ} -> $at")
        }
    }

    private fun Cheat.dumpRooms(filter: String?) {
        val rooms =
            catalogue.all().filter { filter == null || it.row.name.contains(filter, true) }
        player.mes("${rooms.size} room(s) loaded from dbtable.poh_room:")
        for (room in rooms.sortedBy { it.row.name }) {
            val doors = room.doorsAfter(0).map { it.value }
            player.mes(
                "${room.row.name} lvl=${room.levelRequirement} cost=${room.cost} " +
                    "zone=${room.sourceZone.x},${room.sourceZone.z} doors=$doors " +
                    "hotspots=${room.hotspots.size}/${room.row.hotspot.size}"
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
        val starting = layout.rooms.isEmpty()
        if (starting) {
            starterLayout(layout)
        }
        if (ensureExitPortal(layout) || starting) {
            player.storeLayout(layout)
        }
        val session = allocate(layout) ?: return
        player.buildMode = buildMode
        player.coords = entranceCoords(session)
    }

    private suspend fun ProtectedAccess.setBuildMode(on: Boolean) {
        val session = player.houseSession
        if (session == null) {
            mes("You must be inside your own house to do that.")
            return
        }
        if (player.buildMode == on) {
            return
        }
        player.buildMode = on
        mes(if (on) "Building mode is now on." else "Building mode is now off.")
        ifClose()
        rebuildHouse(session, toEntrance = true)
    }

    private fun ProtectedAccess.leaveFromOptions() {
        if (player.houseSession == null) {
            mes("You are not in a house.")
            return
        }
        ifClose()
        leaveHouse(EXIT_COORDS)
    }

    private fun exitPortalLoc(): Int = EXIT_PORTAL.asRSCM(RSCMType.LOC)

    private fun isExitPortal(furniture: FurnitureRow): Boolean =
        exitPortalLoc() in catalogue.builtLocIds(furniture)

    /** Every hotspot in the house with an exit portal built on it. */
    private fun exitPortals(layout: HouseLayout): List<HotspotTarget> =
        layout.rooms.flatMap { (slot, placed) ->
            val def = catalogue.room(placed.room) ?: return@flatMap emptyList()
            def.hotspots
                .filter { spot ->
                    val row = layout.built(slot, spot.index)
                    spot.builds.any { it.rowId == row && isExitPortal(it) }
                }
                .map { HotspotTarget(slot, placed.rotation, it) }
        }

    /**
     * A house always has a way out: one with no exit portal - a new house, or one saved before the
     * starting portal was placed - gets one on the first garden centrepiece. Returns whether it did.
     */
    private fun ensureExitPortal(layout: HouseLayout): Boolean {
        if (exitPortals(layout).isNotEmpty()) {
            return false
        }
        for ((slot, placed) in layout.rooms) {
            val def = catalogue.room(placed.room) ?: continue
            for (spot in def.hotspots) {
                val portal = spot.builds.firstOrNull(::isExitPortal) ?: continue
                if (layout.built(slot, spot.index) == null) {
                    layout.build(slot, spot.index, portal.rowId)
                    return true
                }
            }
        }
        return false
    }

    /**
     * Builds [layout] into a fresh region and makes it the player's house. Entering and every room
     * change go through here, so a house is always shown as its layout currently stands.
     */
    private fun ProtectedAccess.allocate(layout: HouseLayout): HouseSession? {
        val region = houses.allocate(layout)
        if (region == null) {
            mes("There is no space for your house right now. Try again shortly.")
            return null
        }
        val session = HouseSession(region, layout)
        player.startHouseSession(session, EXIT_COORDS)
        applyFurniture(session)
        return session
    }

    /**
     * Redraws the house after a room changed, keeping the player on the same tile of the same room;
     * one whose room is gone is put back at the entrance.
     */
    private fun ProtectedAccess.rebuildHouse(session: HouseSession, toEntrance: Boolean = false) {
        val from = player.coords
        val slot = session.slotAt(from)
        val offsetX = from.x - session.region.southWest.x
        val offsetZ = from.z - session.region.southWest.z
        val rebuilt = allocate(session.layout) ?: return
        player.coords =
            if (!toEntrance && slot != null && slot in rebuilt.layout.rooms) {
                rebuilt.region.southWest.translate(offsetX, offsetZ, from.level)
            } else {
                entranceCoords(rebuilt)
            }
    }

    /** A new house is a garden with its exit portal and a parlour north of it. */
    private fun starterLayout(layout: HouseLayout) {
        val garden = catalogue.all().firstOrNull { it.row.name == "garden" } ?: return
        val gardenSlot = RoomSlot.of(LEVEL_GROUND, 4, 4) ?: return
        layout.place(gardenSlot, garden.id, rotation = 0)
        val parlour = catalogue.all().firstOrNull { it.row.name == "parlour" } ?: return
        val parlourSlot = gardenSlot.neighbour(NORTH) ?: return
        val rotation = (0..3).firstOrNull { NORTH.opposite in parlour.doorsAfter(it) } ?: return
        layout.place(parlourSlot, parlour.id, rotation)
    }

    /** In front of the exit portal, or the middle of a ground floor room in a house without one. */
    private fun entranceCoords(session: HouseSession): CoordGrid {
        val portal = exitPortals(session.layout).firstOrNull()
        if (portal != null) {
            val part = portal.hotspot.primary
            return houses.localCoords(
                session.region,
                portal.slot,
                portal.rotation,
                part.localX + EXIT_PORTAL_STEP_X,
                part.localZ,
            )
        }
        val slot =
            session.layout.rooms.keys.firstOrNull { it.level == LEVEL_GROUND }
                ?: session.layout.rooms.keys.first()
        return houses.roomBase(session.region, slot).translate(3, 3)
    }

    /** The player's house, provided they are in it in building mode. */
    private fun ProtectedAccess.buildingSession(): HouseSession? {
        val session = player.houseSession ?: return null
        if (!player.buildMode) {
            mes("You need to be in building mode to do that.")
            return null
        }
        return session
    }

    /** The house [target] was taken from, provided its room still stands as it did then. */
    private fun ProtectedAccess.sessionStillHolding(target: HotspotTarget): HouseSession? {
        val session = player.houseSession ?: return null
        val placed = session.layout.placed(target.slot) ?: return null
        if (placed.rotation != target.rotation) {
            return null
        }
        val room = catalogue.room(placed.room) ?: return null
        return session.takeIf { room.hotspot(target.hotspot.index) != null }
    }

    private fun ProtectedAccess.hasTools(): Boolean =
        inv.contains(OBJ_SAW) && inv.contains(OBJ_HAMMER)

    private suspend fun ProtectedAccess.openBuildMenu(coords: CoordGrid, locId: Int? = null) {
        val session = buildingSession() ?: return
        val target = houses.hotspotAt(session, coords, locId) ?: return
        if (session.layout.built(target.slot, target.hotspot.index) != null) {
            mes("You need to remove the existing furniture first.")
            return
        }
        if (!hasTools()) {
            mes("You need a hammer and a saw to build furniture.")
            return
        }

        val builds = target.hotspot.builds.filter { it.hiddenInBuildMenu != 1 }
        if (builds.isEmpty()) {
            return
        }

        ifOpenMainModal(INTERFACE_FURNITURE)
        val entryScript = CLIENTSCRIPT_FURNITURE_ENTRY.asRSCM(RSCMType.CLIENTSCRIPT)
        for (slot in 0 until FURNITURE_SLOTS) {
            val furniture = builds.getOrNull(slot)
            val entry = furnitureEntryComponent(slot)
            player.ifSetHide(entry, furniture == null)
            player.ifSetPosition(
                entry,
                (slot % FURNITURE_ENTRY_COLUMNS) * FURNITURE_ENTRY_WIDTH,
                (slot / FURNITURE_ENTRY_COLUMNS) * FURNITURE_ENTRY_HEIGHT,
            )
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

        // The entry script clears its component and rebuilds the children, which drops any event
        // set on them, so the Build click is enabled only once an entry has been filled. Its op runs
        // `[clientscript,poh_furniture_creation_op]`, which resumes from whichever child it built.
        for (slot in builds.indices) {
            ifSetEvents(
                furnitureEntryComponent(slot),
                0 until FURNITURE_ENTRY_CHILDREN,
                IfEvent.PauseButton,
            )
        }

        // Each entry is its own component, so the chosen slot is in the name rather than in a
        // subcomponent index.
        val input = pauseButton()
        val chosen = builds.indices.firstOrNull { input.isComponentType(furnitureEntryComponent(it)) }
        if (chosen == null) {
            logger.warn { "Build menu click on an unknown component: '${input.component}'." }
            return
        }
        startBuild(target, builds[chosen])
    }

    private suspend fun ProtectedAccess.startBuild(target: HotspotTarget, furniture: FurnitureRow) {
        val required = furniture.buildLevel()
        if (player.constructionLvl < required) {
            mes("You need a Construction level of $required to build that.")
            return
        }
        if (spendPlan(inv, furniture.materials()) == null) {
            mes("You don't have the materials to build that.")
            return
        }
        val portal = portalFrame(furniture)
        val variant =
            if (portal != null) {
                directPortal(portal, furniture) ?: return
            } else {
                null
            }
        anim(SEQ_BUILD)
        weakQueue(QUEUE_BUILD, BUILD_TICKS, BuildTask(target, furniture.rowId, variant))
    }

    /**
     * A portal is one furniture row per material whichever destination it leads to, so the
     * destination is picked when it is built and kept as the layout's variant loc. Returns null
     * when the menu was cancelled or the player cannot direct it there.
     */
    private suspend fun ProtectedAccess.directPortal(frame: Int, furniture: FurnitureRow): Int? {
        val options = portals.destinations().filter { it.portalFor(frame) != null }
        val labels = options.map { it.name } + "Cancel"
        val chosen = options.getOrNull(menu("Where should it lead?", hotkeys = false, choices = labels))
        if (chosen == null) {
            return null
        }
        portals.refusal(player, chosen, boostable = true)?.let {
            mes(it)
            return null
        }
        val runes = portals.runeCost(chosen, PohPortals.PORTAL_RUNE_MULTIPLIER)
        if (spendPlan(inv, furniture.materials() + runes) == null) {
            mes("You do not have all the required runes in your inventory.")
            return null
        }
        return chosen.portalFor(frame)
    }

    private fun ProtectedAccess.finishBuild(task: BuildTask) {
        resetAnim()
        val target = task.target
        val session = sessionStillHolding(target) ?: return
        val furniture = target.hotspot.builds.firstOrNull { it.rowId == task.furniture } ?: return
        if (session.layout.built(target.slot, target.hotspot.index) != null) {
            return
        }
        if (!hasTools()) {
            mes("You need a hammer and a saw to build furniture.")
            return
        }
        val destination = task.variant?.let(portals::byPortalLoc)
        if (destination != null) {
            portals.refusal(player, destination, boostable = true)?.let {
                mes(it)
                return
            }
        }
        val runes =
            destination?.let { portals.runeCost(it, PohPortals.PORTAL_RUNE_MULTIPLIER) }.orEmpty()
        if (!player.spend(furniture.materials() + runes)) {
            mes("You don't have the materials to build that.")
            return
        }

        session.layout.build(target.slot, target.hotspot.index, furniture.rowId, task.variant)
        player.storeLayout(session.layout)
        spawnFurniture(session, target, furniture.rowId)
        val xp = catalogue.xp(furniture) * xpMods.get(player, STAT_CONSTRUCTION)
        statAdvance(STAT_CONSTRUCTION, xp)
        if (destination != null) {
            statAdvance(STAT_MAGIC, destination.spell.castXp * PohPortals.PORTAL_XP_MULTIPLIER)
        }
        spam("You build a ${furniture.name}.")
    }

    private data class BuildTask(val target: HotspotTarget, val furniture: Int, val variant: Int?)

    private suspend fun ProtectedAccess.removeFurniture(coords: CoordGrid, locId: Int? = null) {
        val session = buildingSession() ?: return
        val target = houses.hotspotAt(session, coords, locId) ?: return
        val built = session.layout.built(target.slot, target.hotspot.index) ?: return
        lastExitRefusal(session.layout, target.slot, target.hotspot.index)?.let {
            mes(it)
            return
        }
        if (menu("Really remove it?", hotkeys = false, choices = listOf("Yes", "No")) != 0) {
            return
        }
        anim(SEQ_BUILD)
        weakQueue(QUEUE_REMOVE, BUILD_TICKS, RemoveTask(target, built))
    }

    private fun ProtectedAccess.finishRemove(task: RemoveTask) {
        resetAnim()
        val target = task.target
        val session = sessionStillHolding(target) ?: return
        if (session.layout.built(target.slot, target.hotspot.index) != task.furniture) {
            return
        }
        despawnFurniture(session, target, task.furniture)
        session.layout.demolish(target.slot, target.hotspot.index)
        player.storeLayout(session.layout)
        spam("You remove the furniture.")
    }

    private data class RemoveTask(val target: HotspotTarget, val furniture: Int)

    private suspend fun ProtectedAccess.openRoomMenu(coords: CoordGrid) {
        val session = buildingSession() ?: return
        val door = houses.doorAt(session, coords)
        if (door == null) {
            mes("This doorway is not one the room knows about. Try the other side of it.")
            return
        }
        val destination = door.slot.neighbour(door.direction)
        if (destination == null) {
            mes("You can't build a room there.")
            return
        }
        if (destination in session.layout.rooms) {
            offerRoomRemoval(session, destination)
            return
        }

        if (destination.level == LEVEL_UPSTAIRS && !supportsAbove(session.layout, destination)) {
            mes("You can't build a room that would have no room to support it.")
            return
        }

        val facing = door.direction.opposite
        val options =
            catalogue
                .all()
                .filter { it.levelRequirement <= player.constructionLvl }
                .filter { it.fitsOn(destination.level) }
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
        val rotation = (0..3).firstOrNull { facing in room.doorsAfter(it) } ?: return
        if (destination in session.layout.rooms) {
            return
        }
        if (!player.spend(listOf(OBJ_COINS to room.cost))) {
            mes("You need ${room.cost} coins to build a ${room.name}.")
            return
        }

        session.layout.place(destination, room.id, rotation)
        player.storeLayout(session.layout)
        rebuildHouse(session)
        mes("You build a ${room.name}.")
    }

    /** Whether the room under [slot] is one an upper floor room can stand on. */
    private fun supportsAbove(layout: HouseLayout, slot: RoomSlot): Boolean {
        val below = RoomSlot.of(slot.level - 1, slot.x, slot.z) ?: return false
        val room = layout.placed(below)?.room?.let(catalogue::room) ?: return false
        return !room.outdoors
    }

    /** Why removing what is at [slot] would leave the house without an exit portal, or null. */
    private fun lastExitRefusal(layout: HouseLayout, slot: RoomSlot, hotspot: Int? = null): String? {
        val portals = exitPortals(layout)
        val removed = portals.filter { it.slot == slot && (hotspot == null || it.hotspot.index == hotspot) }
        return if (removed.isNotEmpty() && removed.size == portals.size) {
            "Your house must have at least one exit portal."
        } else {
            null
        }
    }

    private suspend fun ProtectedAccess.offerRoomRemoval(session: HouseSession, slot: RoomSlot) {
        val room = session.layout.placed(slot)?.room?.let(catalogue::room) ?: return
        (session.layout.removalRefusal(slot) ?: lastExitRefusal(session.layout, slot))?.let {
            mes(it)
            return
        }
        if (session.slotAt(player.coords) == slot) {
            mes("You can't remove a room while you are standing in it.")
            return
        }
        val title = "Remove the ${room.name}? Anything built in it is lost."
        if (menu(title, "Remove it", "Keep it") != 0) {
            return
        }
        if (session.layout.removalRefusal(slot) != null) {
            return
        }
        session.layout.remove(slot)
        player.storeLayout(session.layout)
        rebuildHouse(session)
        mes("You remove the ${room.name}.")
    }

    private fun applyFurniture(session: HouseSession) {
        for ((slot, placed) in session.layout.rooms) {
            val def = catalogue.room(placed.room) ?: continue
            for (hotspot in def.hotspots) {
                val row = session.layout.built(slot, hotspot.index) ?: continue
                spawnFurniture(session, HotspotTarget(slot, placed.rotation, hotspot), row)
            }
        }
    }

    private fun spawnFurniture(session: HouseSession, target: HotspotTarget, furnitureRow: Int) {
        for (loc in builtLocs(session, target, furnitureRow)) {
            locRepo.add(loc, duration = HOUSE_LOC_DURATION)
        }
    }

    private fun despawnFurniture(session: HouseSession, target: HotspotTarget, furnitureRow: Int) {
        for (loc in builtLocs(session, target, furnitureRow)) {
            locRepo.del(loc, duration = HOUSE_LOC_DURATION)
        }
    }

    /**
     * A multi-tile piece covers every part of its hotspot, each with the loc for that part, so a rug
     * lays its corners and sides rather than one tile at the hotspot's anchor.
     */
    private fun builtLocs(
        session: HouseSession,
        target: HotspotTarget,
        furnitureRow: Int,
    ): List<LocInfo> {
        val hotspot = target.hotspot
        val furniture = hotspot.builds.firstOrNull { it.rowId == furnitureRow } ?: return emptyList()
        if (catalogue.builtLocIds(furniture).isEmpty()) {
            logger.warn { "Furniture '${furniture.name}' has no matching loc to place." }
            return emptyList()
        }
        val variant = session.layout.variant(target.slot, hotspot.index)
        return hotspot.parts.mapNotNull { part ->
            val locId = variant ?: catalogue.builtLocFor(furniture, part) ?: return@mapNotNull null
            val coords = houses.partCoords(session.region, target.slot, target.rotation, part)
            LocInfo(
                part.layer,
                coords,
                LocEntity(locId, part.shapeId, quarterTurns(part.angleId, target.rotation)),
            )
        }
    }

    private fun ProtectedAccess.canBuild(furniture: FurnitureRow): Boolean {
        if (player.constructionLvl < furniture.buildLevel()) {
            return false
        }
        return spendPlan(inv, furniture.materials()) != null
    }

    private fun portalFrame(furniture: FurnitureRow): Int? =
        catalogue.builtLocIds(furniture).firstOrNull(portals::isPortalFrame)

    private fun ProtectedAccess.enterPortal(destination: CoordGrid) {
        leaveHouse(destination)
        spam("You step through the portal.")
    }

    private companion object {
        const val BUILD_TICKS = 3
        const val HOUSE_LOC_DURATION = Int.MAX_VALUE

        /** Live puts an arriving player two tiles east of the exit portal's corner, clear of it. */
        const val EXIT_PORTAL_STEP_X = 2

        val NORTH = Direction(0)
        val EXIT_COORDS = CoordGrid(2953, 3224, 0)
    }
}
