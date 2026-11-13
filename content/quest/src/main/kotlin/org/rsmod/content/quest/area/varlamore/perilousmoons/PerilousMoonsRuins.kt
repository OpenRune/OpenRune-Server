package org.rsmod.content.quest.area.varlamore.perilousmoons

import dev.openrune.types.MoveRestrict
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.config.Constants
import org.rsmod.api.player.hook.NeypotzliHook
import org.rsmod.api.player.hook.NeypotzliMoon
import org.rsmod.api.player.hook.NeypotzliPassage
import org.rsmod.api.player.hook.NeypotzliPassageKind
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.api.table.PmoonRuinsPartsRow
import org.rsmod.api.table.PmoonRuinsSpawnsRow
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid

private const val ZONE_SIZE = 8
private const val ZONES = 8

private object RuinsParts {
    val ANTECHAMBER = PmoonRuinsPartsRow.getRow("dbrow.pmoon_ruins_antechamber")
    val ALL: List<PmoonRuinsPartsRow> = PmoonRuinsPartsRow.all()
    val CAMPS: List<PmoonRuinsPartsRow> = ALL.filter { it.campVarbit != null }
    val SPAWNS: List<PmoonRuinsSpawnsRow> = PmoonRuinsSpawnsRow.all()
}

private fun PmoonRuinsPartsRow.southWest() = CoordGrid(zoneX * ZONE_SIZE, zoneZ * ZONE_SIZE)

private fun PmoonRuinsPartsRow.template(): RegionTemplate =
    RegionTemplate.create {
        copyAllLevels(zoneX, zoneZ) {
            zoneWidth = ZONES
            zoneLength = zonesLong
        }
    }

private fun PmoonRuinsPartsRow.built(player: Player): Boolean =
    player.vars[checkNotNull(campVarbit)] == 1

private fun PmoonRuinsPartsRow.markBuilt(player: Player) =
    VarPlayerIntMapSetter.set(player, checkNotNull(campVarbit), 1)

private class RuinsCopy(val part: PmoonRuinsPartsRow, val region: Region) {
    val npcs = mutableListOf<Npc>()
    val campScenery = mutableListOf<LocInfo>()
    val campSpot = mutableListOf<LocInfo>()
    var attala: Npc? = null
    var zuma: Npc? = null

    private val southWest = part.southWest()
    private val origin = region.normal[southWest]

    fun toRegion(coords: CoordGrid): CoordGrid = region.normal[coords]

    fun toNormal(coords: CoordGrid): CoordGrid =
        CoordGrid(
            southWest.x + coords.x - origin.x,
            southWest.z + coords.z - origin.z,
            coords.level,
        )
}

/**
 * Neypotzli as the quest sees it. From the first entry until the talismans are enchanted each
 * part of the ruins is a private copy, which is what lets the three camps start out unbuilt for
 * one player while the shared map has them standing.
 */
@Singleton
class PerilousMoonsRuins
@Inject
constructor(
    private val regionRepo: RegionRepository,
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val scenes: PerilousMoonsScenes,
    private val areas: AreaChecker,
) : NeypotzliHook {
    private val copies = IdentityHashMap<Player, RuinsCopy>()

    override fun normalCoords(player: Player, coords: CoordGrid): CoordGrid {
        val copy = copies[player] ?: return coords
        return if (RegionRegistry.inWorkingArea(coords)) copy.toNormal(coords) else coords
    }

    override suspend fun ProtectedAccess.mayPass(passage: NeypotzliPassage): Boolean {
        val stage = player.pmoonStage
        when (passage.kind) {
            NeypotzliPassageKind.CityGate -> {
                if (stage < PmoonStage.CITY_OPEN) {
                    startDialogue { gateRefusal() }
                    return false
                }
                if (stage == PmoonStage.CITY_OPEN) {
                    player.pmoonStage = PmoonStage.IN_CITY
                }
            }
            NeypotzliPassageKind.RuinsEntrance ->
                if (stage < PmoonStage.RUINS_OPEN) {
                    startDialogue { ruinsRefusal() }
                    return false
                }
            NeypotzliPassageKind.Cavern -> {
                val leavingAntechamber = areas.inArea(RuinsParts.ANTECHAMBER.area, passage.origin)
                if (leavingAntechamber && stage < PmoonStage.BUILD_CAMPS) {
                    startDialogue {
                        chatPlayer(
                            neutral,
                            "I should probably speak to the others before I go exploring.",
                        )
                    }
                    return false
                }
                if (leavingAntechamber && stage < PmoonStage.CAMPS_BUILT && !hasSupplies()) {
                    startDialogue { needSupplies() }
                    return false
                }
            }
            NeypotzliPassageKind.MoonChamber -> {
                if (stage < PmoonStage.FIGHT_MOONS) {
                    startDialogue {
                        chatPlayer(
                            worried,
                            "This entrance looks dangerous. I probably shouldn't go in here " +
                                "without a good reason.",
                        )
                    }
                    return false
                }
                if (stage < PmoonStage.COMPLETE && player.pmoonMoonsSubdued) {
                    startDialogue {
                        chatPlayer(
                            neutral,
                            "I've done everything I need to here. I should check in with " +
                                "Eyatlalli.",
                        )
                    }
                    return false
                }
            }
            NeypotzliPassageKind.Vault ->
                if (stage < PmoonStage.COMPLETE) {
                    mesbox(
                        "You look through the passage and see that it's blocked. There won't " +
                            "be any way for you to get through."
                    )
                    return false
                }
            NeypotzliPassageKind.CityExit,
            NeypotzliPassageKind.RuinsExit -> {}
        }
        return true
    }

    override suspend fun ProtectedAccess.arrive(passage: NeypotzliPassage): Boolean {
        if (
            passage.kind == NeypotzliPassageKind.RuinsEntrance &&
                player.pmoonStage == PmoonStage.RUINS_OPEN
        ) {
            player.pmoonStage = PmoonStage.MURALS
        }
        val part = partAt(passage.destination)
        if (part == null || player.pmoonStage !in PmoonStage.INSTANCED) {
            leave(player)
            return false
        }
        val copy = enter(player, part) ?: return false
        telejump(copy.toRegion(passage.destination), TeleportType.Exempt)
        return true
    }

    override suspend fun ProtectedAccess.mayGather(): Boolean {
        if (player.pmoonStage >= PmoonStage.CAMPS_BUILT) return true
        startDialogue {
            chatPlayer(
                neutral,
                "I probably shouldn't touch anything until the others have had a chance to look " +
                    "around.",
            )
        }
        return false
    }

    override suspend fun ProtectedAccess.beforeFight(moon: NeypotzliMoon) {
        if (moon == NeypotzliMoon.Eclipse && player.pmoonStage == PmoonStage.FIGHT_MOONS) {
            scenes.eclipseMoon(this)
            player.pmoonStage = PmoonStage.ECLIPSE_SEEN
        }
    }

    fun leave(player: Player) {
        val copy = copies.remove(player) ?: return
        for (npc in copy.npcs) {
            if (npc.isSlotAssigned) npcRepo.del(npc, Int.MAX_VALUE)
        }
        regionRepo.unprotect(copy.region)
    }

    /** A player saved inside a private copy comes back at the ruins entrance. */
    fun restoreLogin(player: Player) {
        if (player.pmoonStage !in PmoonStage.INSTANCED) return
        if (!RegionRegistry.inWorkingArea(player.coords)) return
        player.coords = PmoonPlace.RUINS_ENTRANCE
    }

    fun attalaJoinsZuma(player: Player) {
        val copy = copies[player] ?: return
        copy.attala?.walk(copy.toRegion(PmoonPlace.ZUMA_MURAL.translateX(-1)))
    }

    fun pairJoinsJessamine(player: Player) {
        val copy = copies[player] ?: return
        copy.attala?.walk(copy.toRegion(PmoonPlace.ATTALA_REST))
        copy.zuma?.walk(copy.toRegion(PmoonPlace.ZUMA_REST))
    }

    private fun partAt(coords: CoordGrid): PmoonRuinsPartsRow? =
        RuinsParts.ALL.firstOrNull { areas.inArea(it.area, coords) }

    private fun enter(player: Player, part: PmoonRuinsPartsRow): RuinsCopy? {
        leave(player)
        val region = regionRepo.add(part.template()) ?: return null
        regionRepo.protect(region)
        val copy = RuinsCopy(part, region)
        copies[player] = copy
        if (part.rowId == RuinsParts.ANTECHAMBER.rowId) {
            furnishAntechamber(player, copy)
        }
        for (spawn in RuinsParts.SPAWNS.filter { it.part.rowId == part.rowId }) {
            val npc = Npc(spawn.npc, copy.toRegion(spawn.tile))
            npcRepo.add(npc, Int.MAX_VALUE)
            copy.npcs += npc
        }
        if (part.campVarbit != null && !part.built(player)) {
            unbuildCamp(copy, part)
        }
        return copy
    }

    private fun furnishAntechamber(player: Player, copy: RuinsCopy) {
        val exploring = player.pmoonStage == PmoonStage.MURALS
        val attalaTile =
            when {
                exploring && player.pmoonMurals < 1 -> PmoonPlace.ATTALA_MURAL
                exploring && player.pmoonMurals < 2 -> PmoonPlace.ZUMA_MURAL.translateX(-1)
                else -> PmoonPlace.ATTALA_REST
            }
        val zumaTile =
            if (exploring && player.pmoonMurals < 2) PmoonPlace.ZUMA_MURAL else PmoonPlace.ZUMA_REST
        copy.attala = spawn(copy, PmoonNpc.ATTALA_RUINS, attalaTile)
        copy.zuma = spawn(copy, PmoonNpc.ZUMA_RUINS, zumaTile)
        spawn(copy, PmoonNpc.JESSAMINE_RUINS, PmoonPlace.JESSAMINE)
        spawn(copy, PmoonNpc.BUILDERS_RUINS, PmoonPlace.BUILDERS)
        for ((loc, tile) in SUPPLIES) {
            locRepo.add(
                copy.toRegion(tile),
                loc,
                Int.MAX_VALUE,
                LocAngle.West,
                LocShape.CentrepieceStraight,
            )
        }
    }

    private fun spawn(copy: RuinsCopy, type: String, tile: CoordGrid): Npc {
        val npc = Npc(type, copy.toRegion(tile))
        npcRepo.add(npc, Int.MAX_VALUE)
        npc.noneMode()
        npc.moveRestrict = MoveRestrict.PassThru
        copy.npcs += npc
        return npc
    }

    private fun unbuildCamp(copy: RuinsCopy, site: PmoonRuinsPartsRow) {
        for (piece in site.campPieces) {
            for (level in CAMP_LEVELS) {
                val tile = copy.toRegion(CoordGrid(piece.x, piece.z, level))
                val scenery =
                    locRepo.findAll(tile).filter { it.coords == tile && it.shape in CAMP_SHAPES }
                for (loc in scenery.toList()) {
                    locRepo.del(loc, Int.MAX_VALUE)
                    copy.campScenery += loc
                }
            }
        }
        val level = copy.campScenery.firstOrNull()?.coords?.level ?: 0
        val spot = checkNotNull(site.campSpot)
        val stoveTile = checkNotNull(site.campStove)
        copy.campSpot +=
            locRepo.add(
                copy.toRegion(CoordGrid(spot.x, spot.z, level)),
                "loc.pmoon_campbox",
                Int.MAX_VALUE,
                LocAngle.West,
                LocShape.CentrepieceStraight,
            )
        val stove = copy.toRegion(CoordGrid(stoveTile.x, stoveTile.z, level))
        copy.campSpot +=
            locRepo.add(
                stove,
                "loc.pmoon_range_notbuilt",
                Int.MAX_VALUE,
                copy.campScenery.firstOrNull { it.coords == stove }?.angle ?: LocAngle.West,
                LocShape.CentrepieceStraight,
            )
    }

    suspend fun ProtectedAccess.takeSupplies() {
        if (player.pmoonStage < PmoonStage.BUILD_CAMPS) {
            startDialogue {
                chatPlayer(
                    neutral,
                    "I probably shouldn't touch anything until the others have had a chance to " +
                        "look around.",
                )
            }
            return
        }
        val campsLeft = RuinsParts.CAMPS.size - player.pmoonCampsBuilt
        val supplies = (campsLeft - inv.count(PmoonObj.BUILDING_SUPPLIES)).coerceAtLeast(0)
        val tools = listOf(PmoonObj.HAMMER, PmoonObj.SAW).filter { inv.count(it) == 0 }
        val needed = supplies + tools.size
        if (needed == 0) {
            mes("You already have everything you need from these crates.")
            return
        }
        if (inv.freeSpace() < needed) {
            val spaces = if (needed == 1) "one free space" else "${SPACE_WORDS[needed]} free spaces"
            mes(
                "You don't have enough room to take everything you need from the crates. You " +
                    "need $spaces."
            )
            return
        }
        if (supplies > 0) invAdd(inv, PmoonObj.BUILDING_SUPPLIES, supplies)
        for (tool in tools) invAdd(inv, tool)
        doubleobjbox(
            PmoonObj.BUILDING_SUPPLIES,
            PmoonObj.BUILDING_SUPPLIES,
            "You take some supplies from the crates.",
        )
    }

    suspend fun ProtectedAccess.buildCamp() {
        val copy = copies[player] ?: return
        val site = copy.part.takeIf { it.campVarbit != null } ?: return
        if (site.built(player)) return
        if (!hasSupplies() || inv.count(PmoonObj.HAMMER) == 0 || inv.count(PmoonObj.SAW) == 0) {
            startDialogue { needSupplies() }
            return
        }
        if (invDel(inv, PmoonObj.BUILDING_SUPPLIES, 1).failure) return
        anim("seq.human_poh_build_wall")
        soundSynth("synth.poh_build_wood")
        copy.campSpot.firstOrNull()?.let { locRepo.del(it, Int.MAX_VALUE) }
        fadeOverlay(0, 255, 0, 0, FADE_DURATION)
        delay(1)
        minimapHideMap()
        delay(1)
        for (loc in copy.campSpot.drop(1)) locRepo.del(loc, Int.MAX_VALUE)
        for (loc in copy.campScenery) locRepo.add(loc, Int.MAX_VALUE)
        copy.campSpot.clear()
        copy.campScenery.clear()
        delay(1)
        resetAnim()
        fadeOverlay(0, 0, 0, 255, FADE_DURATION)
        minimapReset()
        delay(1)
        anim("seq.human_pickuptable_walkmerge")
        delay(2)
        closeFadeOverlayNow()
        mes("You drink some tea from the newly built camp and find yourself feeling energised!")
        say("Aaah, nothing like a nice cuppa tea!")
        anim("seq.human_eat")
        soundSynth("synth.liquid")
        player.runEnergy = Constants.run_max_energy
        UpdateRun.energy(player, player.runEnergy)
        site.markBuilt(player)
        if (player.pmoonCampsBuilt == RuinsParts.CAMPS.size) {
            player.pmoonStage = PmoonStage.CAMPS_BUILT
        }
    }

    private fun ProtectedAccess.hasSupplies(): Boolean = inv.count(PmoonObj.BUILDING_SUPPLIES) > 0

    private companion object {
        private const val FADE_DURATION = 50
        private val CAMP_LEVELS = 0..1
        private val CAMP_SHAPES = setOf(LocShape.CentrepieceStraight)
        private val SPACE_WORDS = listOf("zero", "one", "two", "three", "four", "five")
        private val SUPPLIES =
            listOf(
                "loc.pmoon_camp_supplies_1" to CoordGrid(1435, 9636, 1),
                "loc.pmoon_camp_supplies_2" to CoordGrid(1436, 9636, 1),
                "loc.pmoon_camp_supplies_3" to CoordGrid(1436, 9637, 1),
            )
    }
}

private suspend fun org.rsmod.api.player.dialogue.Dialogue.needSupplies() {
    chatPlayer(
        neutral,
        "I should grab the camp building supplies before I do any more exploration.",
    )
}
