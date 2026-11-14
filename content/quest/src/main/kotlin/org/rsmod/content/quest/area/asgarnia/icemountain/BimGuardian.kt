package org.rsmod.content.quest.area.asgarnia.icemountain

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.refs.params
import org.rsmod.api.npc.hit.modifier.StandardNpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.player.midiSong
import org.rsmod.api.player.output.CamShakeAxis
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.miningLvl
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.table.MusicRow
import org.rsmod.content.quest.area.misthalin.SceneCamera
import org.rsmod.content.quest.area.misthalin.SceneDriftRate
import org.rsmod.content.quest.area.misthalin.SceneRegion
import org.rsmod.content.quest.area.misthalin.SceneSnapRate
import org.rsmod.content.quest.area.misthalin.lockedScene
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.game.movement.MoveSpeed
import org.rsmod.game.movement.RouteRequestCoord
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid

@Singleton
class BimGuardian
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val locRepo: LocRepository,
    private val regionRepo: RegionRepository,
    private val worldRepo: WorldRepository,
    private val playerList: PlayerList,
    private val launcher: ProtectedAccessLauncher,
    private val hitModifier: StandardNpcHitModifier,
) {
    private var guardian: Npc? = null
    private var dead = false
    private var scene: SceneRegion? = null
    private var sceneRegion: Region? = null

    internal fun enterScene(region: Region?, view: SceneRegion) {
        if (sceneRegion !== region) {
            releaseScene()
        }
        sceneRegion = region
        scene = view
    }

    fun releaseScene() {
        sceneRegion?.let(regionRepo::unprotect)
        sceneRegion = null
        scene = null
        guardian?.let {
            if (it.isSlotAssigned) {
                npcRepo.del(it, Int.MAX_VALUE)
            }
        }
        guardian = null
        dead = false
    }

    private fun at(coords: CoordGrid): CoordGrid = scene?.let { it[coords] } ?: coords

    fun ensureFor(player: Player, stage: Int) {
        if (scene != null && !inRoom(player.coords)) {
            releaseScene()
        }
        if (stage >= BimStage.COMPLETE && inRoom(player.coords)) {
            ensureRuinsSpiders()
        }
        if (!inRoom(player.coords)) {
            return
        }
        when (stage) {
            BimStage.GUARDIAN_FIGHT -> ensureSpawned(player)
            BimStage.GUARDIAN_DEAD -> {
                ensureBag()
                openDoorsAndRamarno()
            }
        }
    }

    fun ensureSpawned(player: Player? = null) {
        if (guardian?.isSlotAssigned == true) {
            return
        }
        val npc = Npc(GUARDIAN, at(BOSS_TILE))
        npc.respawnDir = Direction.South
        player?.let { npc.facePathingEntitySquare(it) }
        npcRepo.add(npc, GUARDIAN_LIFESPAN)
        guardian = npc
        dead = false
        addPillars()
        addDoor()
        addCutsceneExit()
    }

    fun onKill(npc: Npc, hero: Player, quest: BelowIceMountain) {
        if (!npc.isType(GUARDIAN) || npc != guardian) {
            return
        }
        defeat(hero, quest)
    }

    suspend fun minePillar(access: ProtectedAccess, pillar: BoundLocInfo, quest: BelowIceMountain) {
        val player = access.player
        if (quest.stage(player) != BimStage.GUARDIAN_FIGHT) {
            return
        }
        val npc = guardian ?: return
        if (dead || !npc.isSlotAssigned || npc.hitpoints <= 0) {
            return
        }
        if (player.miningLvl < PILLAR_MINING_LEVEL) {
            access.mes("You need a Mining level of $PILLAR_MINING_LEVEL to mine this rock.")
            return
        }
        val pickaxe = access.findPickaxe()
        if (pickaxe == null) {
            access.mes("You need a pickaxe to mine this rock.")
            return
        }

        access.anim(access.pickaxeAnimation(pickaxe))
        access.spam("You swing your pick at the pillar.")
        access.delay(PILLAR_SWING_TICKS)
        val stillThere =
            locRepo.findAll(pillar.coords).any { it.id == PILLAR.asRSCM(RSCMType.LOC) }
        if (!stillThere) {
            return
        }
        if (access.random.of(maxExclusive = 2) == 0) {
            return
        }

        locRepo.change(pillar, BROKEN_PILLAR, Int.MAX_VALUE)
        access.spotanim("spotanim.rockfall", delay = 20, height = 10)
        if (npc.hitpoints <= 0) {
            return
        }
        val damage = PILLAR_MIN_DAMAGE + access.random.of(maxExclusive = PILLAR_DAMAGE_SPREAD)
        val actual = minOf(damage, npc.hitpoints)
        npc.queueHit(1, HitType.Typeless, actual, hitModifier)
        worldRepo.soundArea(
            npc.coords,
            "synth.bim_ancient_guardian_defend",
            radius = 10,
        )
        access.delay(1)
        if (npc.hitpoints <= 0) {
            defeat(player, quest)
        }
    }

    private fun defeat(hero: Player, quest: BelowIceMountain) {
        if (dead) {
            return
        }
        dead = true
        val npc = guardian
        guardian = null
        if (npc != null) {
            npc.anim("seq.ancient_guardian_death")
            worldRepo.soundArea(npc.coords, "synth.bim_ancient_guardian_death", radius = 10)
        }
        val players =
            playerList.filter { it.bimStage == BimStage.GUARDIAN_FIGHT && inRoom(it.coords) }
        for (player in players) {
            player.bimStage = BimStage.GUARDIAN_DEAD
            ServerCacheManager.getVarbit(GUARDIAN_DEFEATED_VARBIT)?.let {
                VarPlayerIntMapSetter.set(player, it, 1)
            }
            launcher.launch(player) { finishGuardian(quest, npc) }
        }
        if (hero.bimStage == BimStage.GUARDIAN_DEAD && players.none { it.uuid == hero.uuid }) {
            launcher.launch(hero) { finishGuardian(quest, npc) }
        }
    }

    private suspend fun ProtectedAccess.finishGuardian(quest: BelowIceMountain, npc: Npc?) {
        delay(4)
        if (npc?.isSlotAssigned == true) {
            npcRepo.del(npc, Int.MAX_VALUE)
        }
        val interior = scene
        if (interior == null) {
            finishGuardianInWorld()
            return
        }
        finishGuardianInScene(quest, interior)
    }

    private suspend fun ProtectedAccess.finishGuardianInScene(
        quest: BelowIceMountain,
        interior: SceneRegion,
    ) {
        val cast = mutableListOf<Npc>()
        try {
            lockedScene(
                vantage = interior[BAG_VANTAGE],
                faceAt = interior[BOSS_TILE],
                camera = interior.map(AFTERMATH_CAMERA),
                returnTo = CAMDOZAAL_ARRIVAL,
                underFade = { playRuinsMusic() },
                teardown = { cast.forEach(::despawn) },
            ) {
                startDialogue {
                    chatPlayer(
                        neutral,
                        "That monster showed up at the perfect time. I would have been toast if " +
                            "Willow and her gang had their chance to attack.",
                    )
                    chatPlayer(
                        neutral,
                        "Speaking of which, maybe they left something behind I can salvage some " +
                            "kind of reward from.",
                    )
                    anim(THINK_SEQ)
                    chatPlayer(
                        quiz,
                        "Looks like Willow left a bag behind. Maybe there's something in it I " +
                            "could take.",
                    )
                }

                walkTo(interior[BAG_WALK])
                dollyBoth(interior, BAG_CREEP, rate = 2)
                delay(BAG_WALK_TICKS)
                faceSquare(interior[BAG_TILE])

                startDialogue {
                    anim(RUMMAGE_SEQ)
                    say("Hmm...")
                    mesbox("You rummage through Willow's bag...")
                    anim(HAUL_SEQ)
                    mesbox("You find a pile of coins!")
                }
                if (player.lootBagCoins()) {
                    player.bimClaimedReward = 1
                }
                startDialogue { chatPlayer(happy, "Those will do nicely!") }

                openSceneDoors(interior)
                soundSynth(GATE_OPEN)
                soundSynth(DUNGEON_DOOR_OPEN)
                camShake(
                    axis = CamShakeAxis.LEFT_RIGHT,
                    random = DOOR_SHAKE,
                    amplitude = 0,
                    rate = 0,
                )

                startDialogue {
                    chatPlayer(
                        worried,
                        "What was that noise? Please don't be another rock monster...",
                    )
                }

                camShakeResetAll()
                camReset()
                frame(interior, BAG_CREEP)

                val ramarno = spawnSceneNpc(RAMARNO_CUTSCENE, interior[BOSS_TILE], Direction.South)
                cast += ramarno
                ramarno.faceSquare(interior[BAG_WALK])

                startDialogue { chatPlayer(shocked, "What the!") }
                dolly(interior, DOOR_REVEAL, rate = 2)
                lookAt(interior, DOOR_REVEAL, rate = 5)
                faceSquare(interior[BOSS_TILE])

                startDialogue {
                    chatNpcSpecific("Ramarno", RAMARNO_CUTSCENE, shocked, "Bless my beard...")
                }
            }
        } finally {
            releaseScene()
        }

        delay(COMPLETION_LEAD_TICKS)
        openDoorsAndRamarno()
        ensureRuinsSpiders()
        quest.complete(this)
    }

    private suspend fun ProtectedAccess.finishGuardianInWorld() {
        telejump(GUARDIAN_EXIT_TILE)
        camReset()
        camMoveToV3(GUARDIAN_CAM, height = 450, rate = SNAP, rate2 = SNAP)
        camLookAtV3(GUARDIAN_EXIT_TILE, height = 300, rate = SNAP, rate2 = SNAP)
        fadeOverlay(0, FADE_BLACK, 0, FADE_CLEAR, 50)
        delay(2)
        closeFadeOverlay(cycles = 2)
        startDialogue {
            chatPlayer(
                neutral,
                "That monster showed up at the perfect time. I would have been toast if Willow " +
                    "and her gang had their chance to attack.",
            )
            chatPlayer(
                neutral,
                "Speaking of which, maybe they left something behind I can salvage some kind " +
                    "of reward from.",
            )
        }
        ensureBag()
        startDialogue {
            chatPlayer(
                neutral,
                "Looks like Willow left a bag behind. Maybe there's something in it I could take.",
            )
        }
    }

    suspend fun onBagLooted(access: ProtectedAccess, quest: BelowIceMountain) {
        access.soundSynth(DUNGEON_DOOR_OPEN)
        access.startDialogue {
            chatPlayer(
                neutral,
                "What was that noise? Please don't be another rock monster...",
            )
        }
        openDoorsAndRamarno()
        access.startDialogue {
            chatPlayer(neutral, "What the!")
            chatNpcSpecific(
                "Ramarno",
                RAMARNO_CUTSCENE,
                shocked,
                "Bless my beard...",
            )
        }
        quest.complete(access)
        ensureRuinsSpiders()
    }

    private fun ProtectedAccess.playRuinsMusic() {
        val row = MusicRow.all().firstOrNull { it.displayname == RUINS_MUSIC } ?: return
        player.midiSong(row.midi, fadeOutSpeed = 60, fadeInDelay = 60)
    }

    private fun openSceneDoors(interior: SceneRegion) {
        locRepo.findExact(interior[RIGHT_TILE], LocShape.CentrepieceStraight)?.let {
            locRepo.del(it, SCENE_PROP_TICKS)
        }
        locRepo.findExact(interior[LEFT_TILE], LocShape.CentrepieceStraight)?.let {
            locRepo.del(it, SCENE_PROP_TICKS)
        }
        locRepo.add(
            interior[RIGHT_TILE],
            OPEN_RIGHT,
            SCENE_PROP_TICKS,
            LocAngle.West,
            LocShape.CentrepieceStraight,
        )
        locRepo.add(
            interior[LEFT_TILE],
            OPEN_LEFT,
            SCENE_PROP_TICKS,
            LocAngle.West,
            LocShape.CentrepieceStraight,
        )
    }

    private fun spawnSceneNpc(type: String, coords: CoordGrid, facing: Direction?): Npc {
        val npc = Npc(type, coords)
        facing?.let { npc.respawnDir = it }
        npc.mode = NpcMode.None
        npcRepo.add(npc, Int.MAX_VALUE)
        return npc
    }

    private fun despawn(npc: Npc) {
        if (npc.isSlotAssigned) {
            npcRepo.del(npc, Int.MAX_VALUE)
        }
    }

    private fun ProtectedAccess.frame(r: SceneRegion, camera: SceneCamera) {
        val mapped = r.map(camera)
        camMoveToV3(
            mapped.eye,
            height = mapped.eyeHeight,
            rate = SceneSnapRate,
            rate2 = SceneSnapRate,
        )
        camLookAtV3(
            mapped.lookAt,
            height = mapped.lookAtHeight,
            rate = SceneSnapRate,
            rate2 = SceneSnapRate,
        )
    }

    private fun ProtectedAccess.dolly(
        r: SceneRegion,
        camera: SceneCamera,
        rate: Int = SceneDriftRate,
    ) {
        val mapped = r.map(camera)
        camMoveToV3(mapped.eye, height = mapped.eyeHeight, rate = rate, rate2 = rate)
    }

    private fun ProtectedAccess.lookAt(r: SceneRegion, camera: SceneCamera, rate: Int) {
        val mapped = r.map(camera)
        camLookAtV3(mapped.lookAt, height = mapped.lookAtHeight, rate = rate, rate2 = rate)
    }

    private fun ProtectedAccess.dollyBoth(r: SceneRegion, camera: SceneCamera, rate: Int) {
        dolly(r, camera, rate)
        lookAt(r, camera, rate)
    }

    private fun ProtectedAccess.walkTo(dest: CoordGrid) {
        player.routeRequest = RouteRequestCoord(dest)
        player.tempMoveSpeed = MoveSpeed.Walk
    }

    private fun ensureRuinsSpiders() {
        val spider = ServerCacheManager.getNpc(RUINS_SPIDER_ID) ?: return
        for ((coords, facing) in RUINS_SPIDERS) {
            if (npcRepo.findAll(coords).any { it.type.id == RUINS_SPIDER_ID }) {
                continue
            }
            val npc = Npc(spider, coords)
            npc.respawnDir = facing
            npcRepo.add(npc, Int.MAX_VALUE)
        }
    }

    private fun ensureBag() {
        locRepo
            .findAll(BAG_TILE)
            .filter { it.id == BAG_ID || it.id == BAG_CUTSCENE_ID }
            .toList()
            .forEach { locRepo.del(it, Int.MAX_VALUE) }
        locRepo.add(
            BAG_TILE,
            BAG,
            Int.MAX_VALUE,
            LocAngle[3],
            LocShape.CentrepieceDiagonal,
        )
    }

    private fun openDoorsAndRamarno(): Npc {
        for (tile in DOOR_SCAN_TILES) {
            locRepo
                .findAll(tile)
                .filter {
                    it.id == CLOSED_DOOR_ID || it.id == OPEN_LEFT_ID || it.id == OPEN_RIGHT_ID
                }
                .toList()
                .forEach { locRepo.del(it, Int.MAX_VALUE) }
        }
        locRepo
            .findAll(EXIT_TILE)
            .filter { it.id == CUTSCENE_EXIT_ID }
            .toList()
            .forEach { locRepo.del(it, Int.MAX_VALUE) }

        ensureOpenDoor(OPEN_LEFT, LEFT_TILE)
        ensureOpenDoor(OPEN_RIGHT, RIGHT_TILE)
        ensureNormalExit()
        return spawnRamarno()
    }

    private fun ensureOpenDoor(loc: String, coords: CoordGrid) {
        if (locRepo.findAll(coords).any { it.id == loc.asRSCM(RSCMType.LOC) }) {
            return
        }
        locRepo.add(coords, loc, Int.MAX_VALUE, LocAngle[0], LocShape.CentrepieceStraight)
    }

    private fun ensureNormalExit() {
        if (locRepo.findAll(EXIT_TILE).any { it.id == EXIT.asRSCM(RSCMType.LOC) }) {
            return
        }
        locRepo.add(EXIT_TILE, EXIT, Int.MAX_VALUE, LocAngle[2], LocShape.CentrepieceStraight)
    }

    private fun spawnRamarno(): Npc {
        val existing = npcRepo.findAll(RAMARNO_TILE).firstOrNull { it.isType(RAMARNO_ENTRANCE) }
        if (existing != null) {
            return existing
        }
        val ramarno = Npc(RAMARNO_ENTRANCE, RAMARNO_TILE)
        ramarno.respawnDir = Direction.NorthEast
        npcRepo.add(ramarno, Int.MAX_VALUE)
        return ramarno
    }

    fun despawnRamarno(npc: Npc) {
        if (npc.isSlotAssigned) {
            npcRepo.del(npc, Int.MAX_VALUE)
        }
    }

    private fun addPillars() {
        for ((coords, angle) in PILLARS) {
            val tile = at(coords)
            locRepo
                .findAll(tile)
                .filter {
                    it.id == PILLAR_ID || it.id == BROKEN_PILLAR_ID || it.id == PILLAR_CUTSCENE_ID
                }
                .toList()
                .forEach { locRepo.del(it, Int.MAX_VALUE) }
            locRepo.add(
                tile,
                PILLAR,
                Int.MAX_VALUE,
                angle,
                LocShape.CentrepieceStraight,
            )
        }
    }

    private fun addDoor() {
        val tile = at(DOOR_TILE)
        if (locRepo.findAll(tile).any { it.id == CLOSED_DOOR_ID }) {
            return
        }
        locRepo.add(
            tile,
            CLOSED_DOOR,
            Int.MAX_VALUE,
            LocAngle[0],
            LocShape.CentrepieceStraight,
        )
    }

    private fun addCutsceneExit() {
        val tile = at(EXIT_TILE)
        if (locRepo.findAll(tile).any { it.id == CUTSCENE_EXIT_ID }) {
            return
        }
        locRepo.add(
            tile,
            CUTSCENE_EXIT,
            Int.MAX_VALUE,
            LocAngle[2],
            LocShape.CentrepieceStraight,
        )
    }

    private fun ProtectedAccess.findPickaxe(): InvObj? {
        val candidates =
            buildList<InvObj> {
                player.righthand?.let { add(it) }
                addAll(player.inv.filterNotNull())
            }
        return candidates.firstOrNull { pickaxe ->
            ocIsContentType(pickaxe, "content.mining_pickaxe") &&
                (ocParamOrNull(pickaxe, params.levelrequire) ?: 1) <= player.miningLvl
        }
    }

    private fun ProtectedAccess.pickaxeAnimation(pickaxe: InvObj): String {
        val seq = ocParamOrNull(pickaxe, params.skill_anim) ?: return DEFAULT_PICKAXE_ANIM
        return RSCM.getReverseMapping(RSCMType.SEQ, seq.id) ?: DEFAULT_PICKAXE_ANIM
    }

    private fun inRoom(coords: CoordGrid): Boolean {
        if (coords.level != 0) {
            return false
        }
        val view = scene
        if (view != null) {
            val southWest = view[CoordGrid(ROOM_MIN_X, ROOM_MIN_Z)]
            val northEast = view[CoordGrid(ROOM_MAX_X, ROOM_MAX_Z)]
            return coords.x in southWest.x..northEast.x && coords.z in southWest.z..northEast.z
        }
        return coords.x in ROOM_MIN_X..ROOM_MAX_X && coords.z in ROOM_MIN_Z..ROOM_MAX_Z
    }

    private companion object {
        const val GUARDIAN = "npc.bim_golem_boss"
        const val PILLAR = "loc.bim_boss_rock"
        const val BROKEN_PILLAR = "loc.bim_boss_rock_mined"
        const val PILLAR_CUTSCENE = "loc.bim_boss_rock_cutscene"
        const val CLOSED_DOOR = "loc.bim_ruins_door_closed"
        const val OPEN_LEFT = "loc.bim_ruins_door_open_left"
        const val OPEN_RIGHT = "loc.bim_ruins_door_open_right"
        const val EXIT = "loc.bim_exit"
        const val CUTSCENE_EXIT = "loc.bim_exit_cutscene"
        const val BAG = "loc.bim_willows_bag"
        const val BAG_CUTSCENE = "loc.bim_willows_bag_cutscene"
        const val RUINS_SPIDER_ID = 10652
        const val GUARDIAN_DEFEATED_VARBIT = 12393
        const val RAMARNO_ENTRANCE = "npc.camdozaal_ramarno_entrance_multi"
        const val RAMARNO_CUTSCENE = "npc.bim_camzodaal_ramarno_cutscene"
        const val DEFAULT_PICKAXE_ANIM = "seq.human_mining_bronze_pickaxe"
        const val RUINS_MUSIC = "The Ruins of Camdozaal"
        const val RUMMAGE_SEQ = "seq.generic_look_and_search"
        const val HAUL_SEQ = "seq.human_pickupfloor"
        const val THINK_SEQ = "seq.emote_think"
        const val GATE_OPEN = "synth.gate_open"
        const val DUNGEON_DOOR_OPEN = "synth.bim_dungeon_door_open"

        val PILLAR_ID = PILLAR.asRSCM(RSCMType.LOC)
        val BROKEN_PILLAR_ID = BROKEN_PILLAR.asRSCM(RSCMType.LOC)
        val PILLAR_CUTSCENE_ID = PILLAR_CUTSCENE.asRSCM(RSCMType.LOC)
        val CLOSED_DOOR_ID = CLOSED_DOOR.asRSCM(RSCMType.LOC)
        val OPEN_LEFT_ID = OPEN_LEFT.asRSCM(RSCMType.LOC)
        val OPEN_RIGHT_ID = OPEN_RIGHT.asRSCM(RSCMType.LOC)
        val CUTSCENE_EXIT_ID = CUTSCENE_EXIT.asRSCM(RSCMType.LOC)
        val BAG_ID = BAG.asRSCM(RSCMType.LOC)
        val BAG_CUTSCENE_ID = BAG_CUTSCENE.asRSCM(RSCMType.LOC)

        const val GUARDIAN_LIFESPAN = 200_000
        const val PILLAR_MINING_LEVEL = 10
        const val PILLAR_SWING_TICKS = 3
        const val PILLAR_MIN_DAMAGE = 10
        const val PILLAR_DAMAGE_SPREAD = 3
        const val SNAP = 232
        const val FADE_CLEAR = 255
        const val FADE_BLACK = 0
        const val BAG_WALK_TICKS = 5
        const val COMPLETION_LEAD_TICKS = 2
        const val DOOR_SHAKE = 15
        const val SCENE_PROP_TICKS = 6000

        val BOSS_TILE = CoordGrid(2952, 5778)
        val DOOR_TILE = CoordGrid(2951, 5780)
        val DOOR_SCAN_TILES =
            listOf(
                CoordGrid(2951, 5780),
                CoordGrid(2952, 5780),
                CoordGrid(2953, 5780),
                CoordGrid(2954, 5780),
            )
        val LEFT_TILE = CoordGrid(2951, 5780)
        val RIGHT_TILE = CoordGrid(2954, 5780)
        val EXIT_TILE = CoordGrid(2951, 5761)
        val RAMARNO_TILE = CoordGrid(2951, 5779)
        val BAG_TILE = CoordGrid(2950, 5769)
        val RUINS_SPIDERS =
            listOf(
                CoordGrid(2958, 5777) to Direction.North,
                CoordGrid(2957, 5779) to Direction.North,
            )
        val GUARDIAN_EXIT_TILE = CoordGrid(2952, 5773)
        val GUARDIAN_CAM = CoordGrid(2957, 5777)
        val BAG_VANTAGE = CoordGrid(2952, 5773)
        val BAG_WALK = CoordGrid(2951, 5769)
        val CAMDOZAAL_ARRIVAL = CoordGrid(2950, 5769)
        val AFTERMATH_CAMERA =
            SceneCamera(CoordGrid(2957, 5777), 450, CoordGrid(2952, 5773), 300)
        val BAG_CREEP = SceneCamera(CoordGrid(2955, 5772), 450, CoordGrid(2951, 5769), 300)
        val DOOR_REVEAL = SceneCamera(CoordGrid(2955, 5772), 450, CoordGrid(2952, 5778), 250)

        val PILLARS =
            listOf(
                CoordGrid(2947, 5771) to LocAngle[3],
                CoordGrid(2958, 5771) to LocAngle[1],
                CoordGrid(2947, 5778) to LocAngle[3],
                CoordGrid(2958, 5778) to LocAngle[1],
            )

        const val ROOM_MIN_X = 2944
        const val ROOM_MAX_X = 2962
        const val ROOM_MIN_Z = 5762
        const val ROOM_MAX_Z = 5784
    }
}
