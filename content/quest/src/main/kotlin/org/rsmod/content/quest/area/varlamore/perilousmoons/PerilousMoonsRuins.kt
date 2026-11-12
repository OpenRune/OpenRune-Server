package org.rsmod.content.quest.area.varlamore.perilousmoons

import dev.openrune.types.MoveRestrict
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import org.rsmod.api.config.Constants
import org.rsmod.api.player.hook.NeypotzliHook
import org.rsmod.api.player.hook.NeypotzliMoon
import org.rsmod.api.player.hook.NeypotzliPassage
import org.rsmod.api.player.hook.NeypotzliPassageKind
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid

internal enum class RuinsArea(
    private val zoneX: Int,
    private val zoneZ: Int,
    private val zonesLong: Int = ZONES,
) {
    Antechamber(176, 1200),
    AncientPrison(168, 1192),
    EarthboundCavern(168, 1208, zonesLong = ZONES * 2),
    StreamboundCavern(184, 1208),
    Vault(184, 1192);

    val southWest = CoordGrid(zoneX * ZONE_SIZE, zoneZ * ZONE_SIZE)

    val template: RegionTemplate =
        RegionTemplate.create {
            copyAllLevels(zoneX, zoneZ) {
                zoneWidth = ZONES
                zoneLength = zonesLong
            }
        }

    operator fun contains(coords: CoordGrid): Boolean =
        coords.x in southWest.x until southWest.x + ZONES * ZONE_SIZE &&
            coords.z in southWest.z until southWest.z + zonesLong * ZONE_SIZE

    companion object {
        fun of(coords: CoordGrid): RuinsArea? = entries.firstOrNull { coords in it }
    }
}

private const val ZONE_SIZE = 8
private const val ZONES = 8

/** [pieces] are the built camp's scenery tiles that an unbuilt camp does not have. */
internal enum class CampSite(
    val area: RuinsArea,
    val spot: CoordGrid,
    val stove: CoordGrid,
    val pieces: List<CoordGrid>,
) {
    AncientPrison(
        RuinsArea.AncientPrison,
        spot = CoordGrid(1348, 9580),
        stove = CoordGrid(1349, 9580),
        pieces =
            listOf(
                CoordGrid(1348, 9580),
                CoordGrid(1349, 9580),
                CoordGrid(1351, 9581),
                CoordGrid(1348, 9582),
                CoordGrid(1348, 9581),
            ),
    ),
    EarthboundCavern(
        RuinsArea.EarthboundCavern,
        spot = CoordGrid(1373, 9710),
        stove = CoordGrid(1374, 9710),
        pieces =
            listOf(
                CoordGrid(1373, 9710),
                CoordGrid(1374, 9710),
                CoordGrid(1374, 9709),
                CoordGrid(1376, 9711),
                CoordGrid(1375, 9712),
            ),
    ),
    StreamboundCavern(
        RuinsArea.StreamboundCavern,
        spot = CoordGrid(1509, 9693),
        stove = CoordGrid(1510, 9693),
        pieces =
            listOf(
                CoordGrid(1510, 9693),
                CoordGrid(1509, 9694),
                CoordGrid(1511, 9692),
                CoordGrid(1511, 9695),
                CoordGrid(1510, 9695),
            ),
    );

    fun built(player: Player): Boolean =
        when (this) {
            AncientPrison -> player.pmoonCamp1
            EarthboundCavern -> player.pmoonCamp2
            StreamboundCavern -> player.pmoonCamp3
        }

    fun markBuilt(player: Player) {
        when (this) {
            AncientPrison -> player.pmoonCamp1 = true
            EarthboundCavern -> player.pmoonCamp2 = true
            StreamboundCavern -> player.pmoonCamp3 = true
        }
    }
}

private class RuinsCopy(val area: RuinsArea, val region: Region) {
    val npcs = mutableListOf<Npc>()
    val campScenery = mutableListOf<LocInfo>()
    val campSpot = mutableListOf<LocInfo>()
    var attala: Npc? = null
    var zuma: Npc? = null

    private val origin = region.normal[area.southWest]

    fun toRegion(coords: CoordGrid): CoordGrid = region.normal[coords]

    fun toNormal(coords: CoordGrid): CoordGrid =
        CoordGrid(
            area.southWest.x + coords.x - origin.x,
            area.southWest.z + coords.z - origin.z,
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
                val leavingAntechamber = passage.origin in RuinsArea.Antechamber
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
        val area = RuinsArea.of(passage.destination)
        if (area == null || player.pmoonStage !in PmoonStage.INSTANCED) {
            leave(player)
            return false
        }
        val copy = enter(player, area) ?: return false
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

    private fun enter(player: Player, area: RuinsArea): RuinsCopy? {
        leave(player)
        val region = regionRepo.add(area.template) ?: return null
        regionRepo.protect(region)
        val copy = RuinsCopy(area, region)
        copies[player] = copy
        if (area == RuinsArea.Antechamber) {
            furnishAntechamber(player, copy)
        }
        for ((type, tile) in CREATURES[area].orEmpty()) {
            val npc = Npc(type, copy.toRegion(tile))
            npcRepo.add(npc, Int.MAX_VALUE)
            copy.npcs += npc
        }
        val site = CampSite.entries.firstOrNull { it.area == area }
        if (site != null && !site.built(player)) {
            unbuildCamp(copy, site)
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

    private fun unbuildCamp(copy: RuinsCopy, site: CampSite) {
        for (piece in site.pieces) {
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
        copy.campSpot +=
            locRepo.add(
                copy.toRegion(CoordGrid(site.spot.x, site.spot.z, level)),
                "loc.pmoon_campbox",
                Int.MAX_VALUE,
                LocAngle.West,
                LocShape.CentrepieceStraight,
            )
        val stove = copy.toRegion(CoordGrid(site.stove.x, site.stove.z, level))
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
        val campsLeft = CampSite.entries.size - player.pmoonCampsBuilt
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
        val site = CampSite.entries.firstOrNull { it.area == copy.area } ?: return
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
        if (player.pmoonCampsBuilt == CampSite.entries.size) {
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

        private fun spawns(
            type: String,
            vararg tiles: Pair<Int, Int>,
        ): List<Pair<String, CoordGrid>> = tiles.map { (x, z) -> type to CoordGrid(x, z) }

        private val CREATURES: Map<RuinsArea, List<Pair<String, CoordGrid>>> =
            mapOf(
                RuinsArea.AncientPrison to
                    spawns(
                        "npc.pmoon_sulphur_nagua",
                        1355 to 9563,
                        1358 to 9556,
                        1358 to 9573,
                        1359 to 9550,
                        1359 to 9567,
                        1372 to 9557,
                        1372 to 9563,
                        1375 to 9560,
                        1378 to 9557,
                        1378 to 9563,
                        1350 to 9558,
                        1351 to 9574,
                        1352 to 9552,
                        1352 to 9568,
                    ),
                RuinsArea.EarthboundCavern to
                    spawns(
                        "npc.varlamore_cockatrice_moon01",
                        1354 to 9697,
                        1355 to 9683,
                        1356 to 9677,
                        1358 to 9681,
                        1358 to 9700,
                        1359 to 9687,
                        1359 to 9694,
                        1352 to 9680,
                        1355 to 9692,
                    ) +
                        spawns(
                            "npc.moth_moonlight",
                            1364 to 9704,
                            1366 to 9695,
                            1366 to 9700,
                            1369 to 9693,
                            1370 to 9684,
                            1370 to 9697,
                            1371 to 9707,
                            1373 to 9673,
                            1373 to 9681,
                            1374 to 9671,
                            1374 to 9675,
                            1374 to 9685,
                            1375 to 9672,
                            1376 to 9677,
                            1377 to 9685,
                            1377 to 9707,
                            1379 to 9712,
                            1380 to 9696,
                            1380 to 9709,
                            1382 to 9707,
                            1382 to 9710,
                            1385 to 9708,
                            1385 to 9711,
                            1386 to 9680,
                            1390 to 9682,
                            1394 to 9680,
                            1394 to 9698,
                            1396 to 9687,
                            1396 to 9695,
                            1396 to 9701,
                            1397 to 9684,
                            1398 to 9689,
                            1398 to 9706,
                        ),
                RuinsArea.StreamboundCavern to
                    spawns(
                        "npc.moth_moonlight",
                        1481 to 9680,
                        1482 to 9699,
                        1483 to 9690,
                        1486 to 9679,
                        1497 to 9712,
                        1504 to 9679,
                        1507 to 9716,
                        1509 to 9678,
                        1513 to 9675,
                        1517 to 9710,
                        1519 to 9713,
                        1520 to 9710,
                        1521 to 9715,
                        1523 to 9671,
                        1524 to 9706,
                        1525 to 9674,
                    ) +
                        spawns(
                            "npc.grimy_lizard",
                            1483 to 9716,
                            1489 to 9704,
                            1490 to 9714,
                            1494 to 9711,
                            1498 to 9709,
                            1499 to 9718,
                            1504 to 9714,
                            1510 to 9715,
                        ) +
                        spawns(
                            "npc.pmoon_little_frog",
                            1498 to 9691,
                            1501 to 9682,
                            1504 to 9692,
                            1506 to 9684,
                            1508 to 9688,
                            1517 to 9688,
                            1521 to 9696,
                            1524 to 9687,
                        ),
            )
    }
}

private suspend fun org.rsmod.api.player.dialogue.Dialogue.needSupplies() {
    chatPlayer(
        neutral,
        "I should grab the camp building supplies before I do any more exploration.",
    )
}
