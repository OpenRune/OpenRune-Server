package org.rsmod.content.quest.area.asgarnia.icemountain

import dev.openrune.types.NpcMode
import dev.openrune.types.aconverted.SynthType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.rsprot.protocol.game.outgoing.camera.CamTargetV4
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.music.MusicPlayer
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.table.MusicRow
import org.rsmod.game.entity.Npc
import org.rsmod.game.map.Direction
import org.rsmod.game.movement.MoveSpeed
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.collision.CollisionFlagMap

@Singleton
class BimWorkout
@Inject
constructor(
    private val regionRepo: RegionRepository,
    private val npcRepo: NpcRepository,
    private val worldRepo: WorldRepository,
    private val musicPlayer: MusicPlayer,
    private val collision: CollisionFlagMap,
    private val zoneUpdates: ZoneUpdateMap,
) {
    suspend fun play(access: ProtectedAccess) {
        allocateWorkoutCollision()
        val openingRegion =
            regionRepo.add(BimRegions.VarrockSquare)
                ?: run {
                    failWorkout(access)
                    return
                }
        regionRepo.protect(openingRegion)
        val palaceRegion =
            regionRepo.add(BimRegions.VarrockPalace)
                ?: run {
                    failWorkout(access)
                    return
                }
        regionRepo.protect(palaceRegion)
        val barbarianRegion =
            regionRepo.add(BimRegions.BarbarianUnion)
                ?: run {
                    failWorkout(access)
                    return
                }
        regionRepo.protect(barbarianRegion)
        val openingScene = CutsceneScene(openingRegion, 1, CoordGrid(3200, 3344))
        val palaceScene = CutsceneScene(palaceRegion, 1, CoordGrid(3160, 3344))
        val pushupScene =
            CutsceneScene(
                barbarianRegion,
                rotation = 0,
                sourceBase = CoordGrid(3024, 3368),
                centerCellX = 17,
                centerCellZ = 12,
            )
        val westScene =
            CutsceneScene(
                barbarianRegion,
                rotation = 0,
                sourceBase = CoordGrid(2984, 3368),
                centerCellX = 12,
                centerCellZ = 12,
            )
        val actors = mutableListOf<Npc>()
        val firstTraining = access.player.bimCheckal == BimCheckal.TRAINING
        val restoreModalHalt = access.player.ignoreModalMovementHalt
        access.player.ignoreModalMovementHalt = true
        val restoreNpcViewDistance = access.player.npcViewDistance
        access.player.npcViewDistance = 32

        try {
            access.beginWorkoutScene()
            playBarbarianWorkoutMusic(access.player)

            // Varrock square, opening shot.
            access.telejump(openingScene[CoordGrid(3253, 3393)])
            access.player.regionRebuildPending = true
            cinematicDelay(access, 1)
            val tramp = access.spawn(TRAMP, openingScene[CoordGrid(3255, 3394)], Direction.South)
            val rat = access.spawn(RAT, openingScene[CoordGrid(3251, 3395)], Direction.South)
            val aubury = access.spawn(AUBURY, openingScene[CoordGrid(3250, 3391)], Direction.South)
            val man = access.spawn(MAN, openingScene[CoordGrid(3262, 3385)], Direction.South)
            actors += listOf(tramp, rat, aubury, man)
            access.camMoveAt(48, 53, 500, SNAP, SNAP)
            access.camLookAt(54, 53, 350, SNAP, SNAP)
            access.faceAngle(512)
            access.fadeFromBlackNow(50)
            cinematicDelay(access, 1)
            man.faceSquare(openingScene[CoordGrid(3257, 3385)])
            tramp.faceSquare(openingScene[CoordGrid(3250, 3394)])
            cinematicDelay(access, 1)
            access.startDialogue {
                chatNpcSpecificNp(
                    "Atlas",
                    ATLAS_CUTSCENE,
                    happy,
                    "If you want to become a strongman, you need to get into shape!",
                )
            }
            cinematicDelay(access, 2)
            access.anim("seq.emote_run_on_spot")
            cinematicDelay(access, 4)
            access.anim("seq.emote_starjump_5")
            cinematicDelay(access, 8)
            access.ifCloseChat()
            access.anim("seq.human_running")
            rat.walk(openingScene[CoordGrid(3251, 3394)])
            cinematicDelay(access, 1)

            var woman: Npc? = null
            var man2: Npc? = null
            var baraek: Npc? = null
            var man3: Npc? = null
            for ((index, coords) in OPENING_RUN_PATH.withIndex()) {
                when (index) {
                    0 -> {
                        access.camLookAt(56, 55, 280, 1, 1)
                        rat.walk(openingScene[CoordGrid(3251, 3393)])
                    }
                    2 -> {
                        man.faceSquare(openingScene[CoordGrid(3255, 3391)])
                        access.camMoveAt(69, 60, 670, SNAP, SNAP)
                        access.camLookAt(58, 60, 250, SNAP, SNAP)
                    }
                    5 -> aubury.walk(openingScene[CoordGrid(3251, 3390)])
                    6 ->
                        access.startDialogue {
                            chatNpcSpecificNp(
                                "Atlas",
                                ATLAS_CUTSCENE,
                                neutral,
                                "It's not all about strength! You need to get some cardio in, too.",
                            )
                        }
                    9 -> aubury.walk(openingScene[CoordGrid(3250, 3391)])
                    10 -> {
                        access.faceAngle(1536)
                        rat.walk(openingScene[CoordGrid(3251, 3394)])
                    }
                    11 -> {
                        man2 =
                            access.spawn(
                                MAN_2,
                                openingScene[CoordGrid(3258, 3374)],
                                Direction.South,
                            )
                                .also { actors += it }
                        rat.walk(openingScene[CoordGrid(3251, 3395)])
                        man2?.walk(openingScene[CoordGrid(3258, 3375)])
                    }
                    12 -> {
                        rat.walk(openingScene[CoordGrid(3251, 3396)])
                        man2?.walk(openingScene[CoordGrid(3258, 3376)])
                    }
                    13 -> {
                        rat.walk(openingScene[CoordGrid(3251, 3397)])
                        man2?.walk(openingScene[CoordGrid(3258, 3377)])
                    }
                    14 -> {
                        man.faceSquare(openingScene[CoordGrid(3260, 3389)])
                        man2?.walk(openingScene[CoordGrid(3258, 3378)])
                        aubury.walk(openingScene[CoordGrid(3251, 3392)])
                    }
                    15 -> man2?.walk(openingScene[CoordGrid(3258, 3379)])
                    16 -> man2?.walk(openingScene[CoordGrid(3258, 3380)])
                    17 -> {
                        man.faceSquare(openingScene[CoordGrid(3260, 3383)])
                        despawn(listOf(rat))
                        access.ifCloseChat()
                        access.camMoveAt(91, 60, 430, 3, 3)
                        man2?.walk(openingScene[CoordGrid(3258, 3381)])
                    }
                    18 -> man2?.walk(openingScene[CoordGrid(3258, 3382)])
                    19 -> {
                        despawn(listOf(tramp))
                        man2?.walk(openingScene[CoordGrid(3258, 3383)])
                    }
                    20 -> {
                        despawn(listOf(aubury))
                        woman =
                            access.spawn(
                                WOMAN,
                                openingScene[CoordGrid(3275, 3360)],
                                Direction.South,
                            )
                                .also { actors += it }
                        man2?.walk(openingScene[CoordGrid(3258, 3384)])
                    }
                    21 -> man2?.walk(openingScene[CoordGrid(3258, 3385)])
                    23 -> despawn(listOf(man))
                    26 -> {
                        access.camLookAt(85, 60, 400, SNAP, SNAP)
                        access.camMoveAt(85, 53, 430, SNAP, SNAP)
                    }
                    27 -> {
                        access.camLookAt(85, 81, 400, 3, 3)
                        access.camMoveAt(85, 74, 430, 3, 3)
                        woman?.walk(openingScene[CoordGrid(3273, 3360)])
                    }
                    30 -> {
                        val thrower = woman
                        if (thrower != null) {
                            val splash = openingScene[CoordGrid(3266, 3362)]
                            thrower.anim("seq.human_throw")
                            thrower.faceSquare(splash)
                            thrower.spotanim(
                                "spotanim.rotten_tomato_launch",
                                delay = 0,
                                height = 96,
                                slot = 0,
                            )
                            thrower.say("Catch!")
                            access.spotanimMap(
                                worldRepo,
                                "spotanim.rotten_splash",
                                splash,
                                delay = 81,
                            )
                            worldRepo.soundArea(splash, "synth.thrown", radius = 5)
                            zoneUpdates.soundArea(
                                splash,
                                2698,
                                delay = 81,
                                loops = 1,
                                radius = 5,
                                size = 0,
                            )
                            worldRepo.projAnim(
                                ProjAnim(
                                    spotanim = 29,
                                    startHeight = 40,
                                    endHeight = 0,
                                    startTime = 28,
                                    endTime = 81,
                                    angle = 15,
                                    progress = 32,
                                    sourceIndex = 0,
                                    targetIndex = 0,
                                    startCoord = thrower.coords,
                                    endCoord = splash,
                                )
                            )
                        }
                    }
                    31 -> {
                        man3 =
                            access.spawn(
                                MAN,
                                openingScene[CoordGrid(3284, 3359)],
                                Direction.South,
                            )
                                .also { actors += it }
                        baraek =
                            access.spawn(
                                BARAEK,
                                openingScene[CoordGrid(3285, 3354)],
                                Direction.East,
                            )
                                .also { actors += it }
                    }
                    36 -> access.fadeToBlackNow(15)
                }
                if (coords == null) {
                    cinematicDelay(access, 1)
                } else {
                    access.step(openingScene[coords])
                }
            }

            despawn(
                listOfNotNull(tramp, rat, aubury, man, man2, woman, man3, baraek),
            )
            access.telejump(pushupScene[CoordGrid(3076, 3423)])
            access.player.regionRebuildPending = true

            // Barbarian Village, first pushup set.
            cinematicDelay(access, 1)
            val atlas =
                access.spawn(ATLAS_CUTSCENE, pushupScene[CoordGrid(3074, 3424)], Direction.SouthEast)
            actors += atlas
            val firstBarbarian =
                access.spawn(BARBARIAN_FEMALE, pushupScene[CoordGrid(3074, 3420)], Direction.NorthEast)
            actors += firstBarbarian
            atlas.faceSquare(access.coords)
            firstBarbarian.faceSquare(access.coords)
            access.camMoveToV3(pushupScene[CoordGrid(3079, 3425)], 300, SNAP, SNAP)
            access.camLookAtV3(pushupScene[CoordGrid(3075, 3423)], 280, SNAP, SNAP)
            access.fadeFromBlackNow(50)
            access.anim("seq.bim_pushups")
            cinematicDelay(access, 2)
            for (count in 1..9) {
                atlas.say("$count...")
                if (count % 3 == 0) {
                    atlas.anim("seq.emote_cheer")
                }
                if (count < 9) {
                    cinematicDelay(access, 2)
                }
            }
            access.fadeToBlackNow(15)
            cinematicDelay(access, 2)

            // Sparring.
            access.telejump(pushupScene[CoordGrid(3078, 3435)])
            access.resetAnim()
            access.faceAngle(1536)
            atlas.telejump(collision, pushupScene[CoordGrid(3079, 3435)])
            atlas.say("")
            atlas.faceSquare(pushupScene[CoordGrid(3078, 3435)])
            access.camMoveToV3(pushupScene[CoordGrid(3078, 3431)], 290, SNAP, SNAP)
            access.camLookAtV3(pushupScene[CoordGrid(3078, 3438)], 280, SNAP, SNAP)
            access.fadeFromBlackNow(50)
            access.startDialogue {
                chatNpcSpecificNp(
                    "Atlas",
                    ATLAS_CUTSCENE,
                    happy,
                    "You need to be tough to be a strongman! Show me what you've got! Come on!",
                )
            }
            cinematicDelay(access, 1)
            repeat(10) { round ->
                access.anim("seq.human_unarmedblock")
                atlas.anim("seq.human_unarmedpunch")
                access.soundSynth(SynthType(2503))
                if ((round + 1) % 2 == 0) {
                    access.say("Eugh...")
                }
                if (round == 4) {
                    access.soundSynth(SynthType(3120), loops = 3)
                }
                if (round == 9) {
                    access.ifCloseChat()
                    access.fadeToBlackNow(15)
                } else {
                    cinematicDelay(access, 2)
                }
            }
            cinematicDelay(access, 2)

            access.telejump(pushupScene[CoordGrid(3074, 3417)])
            access.player.buildAreaOverride = pushupScene.buildAreaBase()
            access.player.buildArea = access.player.buildAreaOverride!!
            despawn(actors.filter { it.isType(BARBARIAN_FEMALE) || it.isType(ATLAS_CUTSCENE) })
            actors.removeAll { it.isType(BARBARIAN_FEMALE) || it.isType(ATLAS_CUTSCENE) }
            access.camReset()
            access.camLookAt(45, 54, 280, SNAP, SNAP)
            access.camMoveAt(45, 43, 600, SNAP, SNAP)
            access.fadeFromBlackNow(50)
            access.anim("seq.human_running")
            cinematicDelay(access, 1)

            for (x in 3072 downTo 3040 step 2) {
                if (x == 3066) {
                    access.startDialogue {
                        chatNpcSpecificNp(
                            "Atlas",
                            ATLAS_CUTSCENE,
                            neutral,
                            "Every strongman was once a beginner who refused to give up.",
                        )
                    }
                }
                if (x == 3064) {
                    access.camLookAt(42, 49, 280, 2, 2)
                    access.camMoveAt(19, 43, 400, 3, 3)
                    access.camLookAt(42, 49, 0, 1, 1)
                } else if (x < 3064 && x > 3040) {
                    access.camLookAt(42 - (3064 - x), 49, 0, 1, 1)
                }
                if (x == 3058) {
                    access.soundSynth(SynthType(3424))
                }
                if (x == 3040) {
                    access.ifCloseChat()
                    access.camLookAt(15, 49, 0, 1, 0)
                }
                access.step(pushupScene[CoordGrid(x, 3417)])
            }
            cinematicDelay(access, 1)

            access.player.buildAreaOverride = westScene.buildAreaBase()
            access.player.buildArea = access.player.buildAreaOverride!!
            access.player.regionRebuildPending = true
            access.camMoveAt(9, 59, 400, 1, 1)
            access.step(westScene[CoordGrid(3038, 3419)])
            access.step(westScene[CoordGrid(3036, 3421)])
            access.step(westScene[CoordGrid(3034, 3423)])
            access.camMoveToV3(westScene[CoordGrid(3033, 3432)], 600, 3, 3)
            access.camLookAtV3(westScene[CoordGrid(3013, 3428)], 0, 1, 0)
            access.step(westScene[CoordGrid(3032, 3425)])
            access.step(westScene[CoordGrid(3030, 3427)])
            access.step(westScene[CoordGrid(3028, 3428)])
            access.camLookAtV3(westScene[CoordGrid(3000, 3432)], 0, 1, 0)
            access.step(westScene[CoordGrid(3026, 3428)])
            access.step(westScene[CoordGrid(3024, 3429)])
            access.step(westScene[CoordGrid(3022, 3429)])
            access.step(westScene[CoordGrid(3020, 3429)])
            access.step(westScene[CoordGrid(3018, 3429)])
            access.fadeToBlackNow(50)
            access.step(westScene[CoordGrid(3016, 3429)])
            access.step(westScene[CoordGrid(3014, 3429)])

            // Second pushup set.
            access.player.buildAreaOverride = null
            access.telejump(pushupScene[CoordGrid(3076, 3423)])
            access.faceAngle(1024)
            access.player.regionRebuildPending = true
            cinematicDelay(access, 1)
            val secondBarbarianOne =
                access.spawn(BARBARIAN_MALE, pushupScene[CoordGrid(3073, 3421)], Direction.NorthEast)
            actors += secondBarbarianOne
            val secondBarbarianTwo =
                access.spawn(BARBARIAN_MALE_2, pushupScene[CoordGrid(3073, 3422)], Direction.NorthEast)
            actors += secondBarbarianTwo
            val secondBarbarianThree =
                access.spawn(BARBARIAN_FEMALE, pushupScene[CoordGrid(3074, 3420)], Direction.NorthEast)
            actors += secondBarbarianThree
            val secondBarbarianFour =
                access.spawn(BARBARIAN_FEMALE_2, pushupScene[CoordGrid(3073, 3420)], Direction.NorthEast)
            actors += secondBarbarianFour
            val finalAtlas =
                access.spawn(ATLAS_CUTSCENE, pushupScene[CoordGrid(3074, 3424)], Direction.SouthEast)
            actors += finalAtlas
            for (npc in
                listOf(
                    secondBarbarianOne,
                    secondBarbarianTwo,
                    secondBarbarianThree,
                    secondBarbarianFour,
                    finalAtlas,
                )) {
                npc.faceSquare(access.coords)
            }
            access.camMoveToV3(pushupScene[CoordGrid(3079, 3425)], 300, SNAP, SNAP)
            access.camLookAtV3(pushupScene[CoordGrid(3075, 3423)], 280, SNAP, SNAP)
            access.fadeFromBlackNow(50)
            access.startDialogue {
                chatNpcSpecificNp(
                    "Atlas",
                    ATLAS_CUTSCENE,
                    neutral,
                    "You need to push yourself!",
                )
            }
            access.anim("seq.bim_pushups")
            cinematicDelay(access, 3)
            for (count in 91..100) {
                finalAtlas.say("$count...")
                if ((count - 91) % 3 == 0) {
                    finalAtlas.anim("seq.emote_yes")
                }
                if (count < 100) {
                    cinematicDelay(access, 2)
                }
            }
            access.ifCloseChat()
            access.anim("seq.bim_pushups_stand")
            cinematicDelay(access, 2)
            access.faceAngle(663)
            cinematicDelay(access, 3)
            access.fadeToBlackNow(15)
            cinematicDelay(access, 2)

            // Varrock palace, final sprint.
            access.player.buildAreaOverride = null
            val palaceStart = palaceScene[CoordGrid(3212, 3392)]
            collision.allocateIfAbsent(palaceStart.x, palaceStart.z, palaceStart.level)
            access.telejump(palaceStart, collision, TeleportType.Exempt)
            access.faceAngle(1536)
            access.player.regionRebuildPending = true
            despawn(
                actors.filter {
                    it.isType(BARBARIAN_MALE) ||
                        it.isType(BARBARIAN_MALE_2) ||
                        it.isType(BARBARIAN_FEMALE) ||
                        it.isType(BARBARIAN_FEMALE_2) ||
                        it.isType(ATLAS_CUTSCENE)
                }
            )
            actors.removeAll {
                it.isType(BARBARIAN_MALE) ||
                    it.isType(BARBARIAN_MALE_2) ||
                    it.isType(BARBARIAN_FEMALE) ||
                    it.isType(BARBARIAN_FEMALE_2) ||
                    it.isType(ATLAS_CUTSCENE)
            }
            val dog = access.spawn(STRAY_DOG, palaceScene[CoordGrid(3216, 3386)], Direction.South)
            actors += dog
            dog.walk(palaceScene[CoordGrid(3216, 3387)])
            cinematicDelay(access, 1)
            access.fadeFromBlackNow(50)
            access.startDialogue {
                chatNpcSpecificNp(
                    "Atlas",
                    ATLAS_CUTSCENE,
                    happy,
                    "Always strive to be the best that you can be! That is the strongman way.",
                )
            }
            access.camMoveAt(50, 52, 300, SNAP, SNAP)
            access.camLookAt(55, 52, 280, SNAP, SNAP)
            access.camMoveAt(78, 52, 300, 3, 3)
            access.camLookAt(88, 52, 280, 3, 0)
            access.anim("seq.human_running")
            var sprintMan: Npc? = null
            var sprintBaraek: Npc? = null
            var guardF: Npc? = null
            var guardVariant: Npc? = null
            var guard: Npc? = null
            for ((index, coords) in FINAL_SPRINT_ROUTE.withIndex()) {
                when (index) {
                    0 -> dog.walk(palaceScene[CoordGrid(3216, 3388)])
                    1 -> dog.walk(palaceScene[CoordGrid(3216, 3389)])
                    3 -> dog.walk(palaceScene[CoordGrid(3216, 3390)])
                    6 -> dog.walk(palaceScene[CoordGrid(3213, 3387)])
                    7 -> dog.walk(palaceScene[CoordGrid(3213, 3386)])
                    8 -> {
                        dog.walk(palaceScene[CoordGrid(3214, 3387)])
                        sprintMan =
                            access.spawn(MAN, palaceScene[CoordGrid(3204, 3359)], Direction.South)
                                .also { actors += it }
                    }
                    9 -> access.ifCloseChat()
                    10 -> {
                        despawn(listOf(dog))
                        actors.remove(dog)
                    }
                    11 -> {
                        access.player.buildAreaOverride = palaceScene.buildAreaBase(17, 12)
                        access.player.buildArea = access.player.buildAreaOverride!!
                        access.player.regionRebuildPending = true
                        access.camLookAt(103, 51, 280, 1, 0)
                        access.camMoveAt(78, 52, 600, 3, 3)
                        sprintBaraek =
                            access.spawn(BARAEK, palaceScene[CoordGrid(3205, 3354)], Direction.East)
                                .also { actors += it }
                    }
                    16 -> {
                        access.camLookAt(103, 51, 280, 1, 0)
                        access.camMoveAt(78, 52, 600, 3, 3)
                    }
                    18 -> {
                        access.camMoveAt(58, 51, 500, SNAP, SNAP)
                        access.camLookAt(48, 51, 280, SNAP, SNAP)
                        sprintBaraek?.walk(palaceScene[CoordGrid(3205, 3355)])
                    }
                    19 -> sprintBaraek?.walk(palaceScene[CoordGrid(3205, 3356)])
                    20 -> sprintBaraek?.walk(palaceScene[CoordGrid(3205, 3357)])
                    21 -> sprintBaraek?.walk(palaceScene[CoordGrid(3204, 3358)])
                    22 -> {
                        guardF =
                            access.spawn(GUARD_F, palaceScene[CoordGrid(3208, 3331)], Direction.West)
                                .also { actors += it }
                        guardF?.walk(palaceScene[CoordGrid(3208, 3332)])
                    }
                    23 -> {
                        access.camMoveAt(63, 51, 500, 2, 2)
                        access.camLookAt(78, 51, 400, SNAP, SNAP)
                    }
                    24 -> {
                        despawn(listOfNotNull(sprintMan, sprintBaraek))
                        actors.removeAll { it === sprintMan || it === sprintBaraek }
                        guardVariant =
                            access.spawn(
                                GUARD_VARIANT,
                                palaceScene[CoordGrid(3216, 3327)],
                                Direction.South,
                            )
                                .also { actors += it }
                        guardVariant?.walk(palaceScene[CoordGrid(3215, 3327)])
                    }
                    25 -> {
                        guard =
                            access.spawn(GUARD, palaceScene[CoordGrid(3218, 3326)], Direction.North)
                                .also { actors += it }
                    }
                    27 -> guard?.walk(palaceScene[CoordGrid(3217, 3327)])
                    28 -> {
                        access.camMoveAt(77, 46, 700, 3, 3)
                        access.camLookAt(80, 51, 650, 3, 3)
                        guard?.walk(palaceScene[CoordGrid(3217, 3328)])
                    }
                    29 -> guard?.walk(palaceScene[CoordGrid(3217, 3327)])
                    30 -> guard?.walk(palaceScene[CoordGrid(3216, 3327)])
                    34 -> access.faceAngle(512)
                    37 -> access.anim("seq.emote_cheer", delay = 2)
                    38 -> guardVariant?.walk(palaceScene[CoordGrid(3215, 3326)])
                    39 -> {
                        guardVariant?.walk(palaceScene[CoordGrid(3215, 3325)])
                        guard?.walk(palaceScene[CoordGrid(3216, 3326)])
                    }
                    40 -> {
                        guardVariant?.walk(palaceScene[CoordGrid(3215, 3324)])
                        guard?.walk(palaceScene[CoordGrid(3216, 3325)])
                    }
                    41 -> {
                        access.fadeToWhiteNow(50)
                        guardVariant?.walk(palaceScene[CoordGrid(3215, 3323)])
                        guard?.walk(palaceScene[CoordGrid(3215, 3325)])
                    }
                }
                if (coords == null) {
                    cinematicDelay(access, 1)
                } else {
                    access.step(palaceScene[coords])
                }
            }

            actors +=
                access.spawn(BARBARIAN_MALE, pushupScene[CoordGrid(3073, 3421)], Direction.NorthEast)
            actors +=
                access.spawn(BARBARIAN_MALE_2, pushupScene[CoordGrid(3073, 3422)], Direction.NorthEast)
            actors +=
                access.spawn(BARBARIAN_FEMALE, pushupScene[CoordGrid(3074, 3420)], Direction.NorthEast)
            actors +=
                access.spawn(BARBARIAN_FEMALE_2, pushupScene[CoordGrid(3073, 3420)], Direction.NorthEast)
            access.player.buildAreaOverride = null
            access.telejump(pushupScene[CoordGrid(3076, 3423)])
            access.faceAngle(663)
            access.player.regionRebuildPending = true
            cinematicDelay(access, 1)

            // Final dialogue at Barbarian Village.
            val lastAtlas =
                access.spawn(ATLAS_CUTSCENE, pushupScene[CoordGrid(3074, 3424)], Direction.SouthEast)
            actors += lastAtlas
            lastAtlas.faceSquare(access.coords)
            access.camMoveToV3(pushupScene[CoordGrid(3079, 3425)], 300, SNAP, SNAP)
            access.camLookAtV3(pushupScene[CoordGrid(3075, 3423)], 280, SNAP, SNAP)
            access.fadeFromWhiteNow(50)
            cinematicDelay(access, 2)
            access.faceAngle(663)
            access.startDialogue {
                chatNpcSpecificNp(
                    "Atlas",
                    ATLAS_CUTSCENE,
                    happy,
                    "I'm very proud of you. You've shown grit, determination and an iron will. " +
                        "You are a true strongman at heart.",
                )
            }
            cinematicDelay(access, 2)
            access.startDialogue {
                access.chatPlayerNoContinue(
                    neutral,
                    "That was exhausting... I've never worked so hard in my life!",
                )
            }
            cinematicDelay(access, 1)
            access.faceAngle(663)
            access.startDialogue {
                chatNpcSpecificNp(
                    "Atlas",
                    ATLAS_CUTSCENE,
                    happy,
                    "And for that, I am proud. Now, show us what you're made of and flex those " +
                        "muscles!",
                )
            }
            cinematicDelay(access, 1)
            access.ifCloseChat()
            access.faceAngle(1345)
            access.anim("seq.emote_flex")
            if (firstTraining) {
                access.player.bimCheckal = BimCheckal.TRAINED
            }
            cinematicDelay(access, 6)
            access.player.emoteFlex = 1
            access.mesboxNp("You have unlocked the <col=ef1020>flex</col> emote!")
            cinematicDelay(access, 1)
            access.ifCloseChat()
            access.faceAngle(663)
            access.startDialogue {
                chatNpcSpecificNp(
                    "Atlas",
                    ATLAS_CUTSCENE,
                    neutral,
                    "My work is done. Return to Checkal and show him your new-found muscles.",
                )
            }
            cinematicDelay(access, 1)
            access.ifCloseChat()
        } finally {
            despawn(actors)
            regionRepo.unprotect(openingRegion)
            regionRepo.unprotect(palaceRegion)
            regionRepo.unprotect(barbarianRegion)
            access.player.buildAreaOverride = null
            access.player.ignoreModalMovementHalt = restoreModalHalt
            access.player.npcViewDistance = restoreNpcViewDistance
            access.fadeToBlack(50)
            access.telejump(ATLAS_TILE)
            access.endWorkoutScene()
        }
    }

    private suspend fun cinematicDelay(access: ProtectedAccess, cycles: Int) {
        repeat(cycles) {
            access.camTarget(CamTargetV4.PlayerCamTarget(access.player.slotId))
            access.delay(1)
        }
    }

    private fun ProtectedAccess.camMoveAt(x: Int, z: Int, height: Int, rate: Int, rate2: Int) {
        camMoveToV3(player.buildArea.translate(x, z), height, rate, rate2)
    }

    private fun ProtectedAccess.camLookAt(x: Int, z: Int, height: Int, rate: Int, rate2: Int) {
        camLookAtV3(player.buildArea.translate(x, z), height, rate, rate2)
    }

    private fun failWorkout(access: ProtectedAccess) {
        access.mes("The training area is unavailable at the moment. Please try again shortly.")
        access.player.bimCheckal = BimCheckal.SENT_TO_ATLAS
    }
    private fun allocateWorkoutCollision() {
        for (x in 374..384) {
            for (z in 425..430) {
                allocateCollisionZone(x, z)
            }
        }
        for (x in 394..412) {
            for (z in 412..435) {
                allocateCollisionZone(x, z)
            }
        }
        for (x in 389..412) {
            for (z in 412..441) {
                allocateCollisionZone(x, z)
            }
        }
    }

    private fun allocateCollisionZone(zoneX: Int, zoneZ: Int) {
        val coords = ZoneKey(zoneX, zoneZ, 0).toCoords()
        collision.allocateIfAbsent(coords.x, coords.z, coords.level)
    }

    private suspend fun ProtectedAccess.beginWorkoutScene() {
        fadeToBlack()
        hideEntityOps()
        hideTopLevel()
        hideHealthHud()
        clearHealthHud()
        minimapHideMap()
        camModeClose()
        maxDrawDistance(true)
        tempDisableAcceptAid()
        closeTopLevelTabsLenient()
        syncDrawDistance()
    }

    private suspend fun ProtectedAccess.endWorkoutScene() {
        fadeFromBlackNow()
        showEntityOps()
        restoreLastAcceptAid()
        maxDrawDistance(false)
        minimapReset()
        camModeReset()
        showTopLevel()
        showHealthHud()
        camReset()
        openTopLevelTabs()
        syncDrawDistance()
    }

    private suspend fun ProtectedAccess.fadeToBlack(duration: Int = 50) {
        fadeOverlay(0, FADE_CLEAR, 0, FADE_BLACK, duration)
        delay(2)
    }

    private suspend fun ProtectedAccess.fadeFromBlack(duration: Int = 50) {
        fadeOverlay(0, FADE_BLACK, 0, FADE_CLEAR, duration)
        delay(2)
        closeFadeOverlay(cycles = 2)
    }

    private suspend fun ProtectedAccess.fadeToWhite(duration: Int = 50) {
        fadeOverlay(WHITE, FADE_CLEAR, WHITE, FADE_BLACK, duration)
        delay(2)
    }

    private suspend fun ProtectedAccess.fadeFromWhite(duration: Int = 50) {
        fadeOverlay(WHITE, FADE_BLACK, WHITE, FADE_CLEAR, duration)
        delay(2)
        closeFadeOverlay(cycles = 2)
    }

    private fun ProtectedAccess.fadeToBlackNow(duration: Int = 50) {
        fadeOverlay(0, FADE_CLEAR, 0, FADE_BLACK, duration)
    }

    private fun ProtectedAccess.fadeFromBlackNow(duration: Int = 50) {
        fadeOverlay(0, FADE_BLACK, 0, FADE_CLEAR, duration)
        closeFadeOverlay(cycles = 2)
    }

    private fun ProtectedAccess.fadeToWhiteNow(duration: Int = 50) {
        fadeOverlay(WHITE, FADE_CLEAR, WHITE, FADE_BLACK, duration)
    }

    private fun ProtectedAccess.fadeFromWhiteNow(duration: Int = 50) {
        fadeOverlay(WHITE, FADE_BLACK, WHITE, FADE_CLEAR, duration)
        closeFadeOverlay(cycles = 2)
    }

    private suspend fun ProtectedAccess.step(dest: CoordGrid) {
        walk(dest)
        player.moveSpeed = MoveSpeed.Run
        player.tempMoveSpeed = MoveSpeed.Run
        camTarget(CamTargetV4.PlayerCamTarget(player.slotId))
        delay(1)
    }

    private fun ProtectedAccess.spawn(type: String, coords: CoordGrid, facing: Direction): Npc {
        val npc = Npc(type, coords)
        npc.respawnDir = facing
        npc.mode = NpcMode.None
        npcRepo.add(npc, Int.MAX_VALUE)
        return npc
    }

    private fun despawn(npcs: List<Npc>) {
        for (npc in npcs) {
            if (npc.isSlotAssigned) {
                npcRepo.del(npc, Int.MAX_VALUE)
            }
        }
    }

    private fun playBarbarianWorkoutMusic(player: org.rsmod.game.entity.Player) {
        val row = MusicRow.all().firstOrNull { it.displayname == BARBARIAN_WORKOUT_MUSIC } ?: return
        musicPlayer.unlockAndPlay(player, row)
    }

    private companion object {
        const val SNAP = 232
        const val FADE_CLEAR = 255
        const val FADE_BLACK = 0
        const val WHITE = 0xFFFFFF
        const val BARBARIAN_WORKOUT_MUSIC = "Barbarian Workout"

        const val ATLAS_CUTSCENE = "npc.bim_cutscene_atlas"
        const val CHECKAL_BARB = "npc.bim_checkal_barb"
        const val TRAMP = "npc.bim_hobo_cutscene"
        const val RAT = "npc.bim_rat"
        const val AUBURY = "npc.bim_aubury"
        const val MAN = "npc.bim_man"
        const val MAN_2 = "npc.bim_man_2"
        const val WOMAN = "npc.bim_woman"
        const val BARAEK = "npc.bim_baraek"
        const val STRAY_DOG = "npc.bim_dog_stray"
        const val BARBARIAN_MALE = "npc.bim_barbarian_male"
        const val BARBARIAN_MALE_2 = "npc.bim_barbarian_male_2"
        const val BARBARIAN_FEMALE = "npc.bim_barbarian_female"
        const val BARBARIAN_FEMALE_2 = "npc.bim_barbarian_female_2"
        const val GUARD = "npc.bim_fai_varrock_guard02"
        const val GUARD_F = "npc.bim_fai_varrock_guard02_f"
        const val GUARD_VARIANT = "npc.bim_fai_varrock_guard02_variant02"

        val OPENING_RUN_PATH: List<CoordGrid?> =
            listOf(
                CoordGrid(3254, 3392),
                CoordGrid(3255, 3391),
                CoordGrid(3256, 3390),
                CoordGrid(3257, 3389),
                CoordGrid(3258, 3389),
                CoordGrid(3259, 3389),
                CoordGrid(3260, 3389),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                CoordGrid(3260, 3387),
                CoordGrid(3260, 3385),
                CoordGrid(3260, 3383),
                CoordGrid(3260, 3381),
                CoordGrid(3260, 3379),
                CoordGrid(3260, 3377),
                CoordGrid(3260, 3375),
                CoordGrid(3260, 3373),
                CoordGrid(3260, 3371),
                CoordGrid(3260, 3369),
                CoordGrid(3260, 3367),
                CoordGrid(3260, 3365),
                CoordGrid(3260, 3363),
                CoordGrid(3262, 3362),
                CoordGrid(3264, 3362),
                CoordGrid(3266, 3362),
                CoordGrid(3268, 3362),
                CoordGrid(3270, 3362),
                CoordGrid(3272, 3362),
                CoordGrid(3274, 3362),
                CoordGrid(3276, 3362),
                CoordGrid(3278, 3362),
                CoordGrid(3280, 3362),
                CoordGrid(3281, 3362),
            )
        val FINAL_SPRINT_ROUTE: List<CoordGrid?> =
            listOf(
                CoordGrid(3212, 3390), CoordGrid(3212, 3388), CoordGrid(3212, 3386), CoordGrid(3212, 3384),
                CoordGrid(3212, 3382), CoordGrid(3212, 3380), CoordGrid(3212, 3378), CoordGrid(3212, 3376),
                CoordGrid(3212, 3374), CoordGrid(3211, 3372), CoordGrid(3211, 3370), CoordGrid(3211, 3368),
                CoordGrid(3211, 3366), CoordGrid(3211, 3364), CoordGrid(3211, 3362), CoordGrid(3211, 3360),
                CoordGrid(3211, 3358), CoordGrid(3211, 3356), CoordGrid(3211, 3354), CoordGrid(3211, 3352),
                CoordGrid(3211, 3350), CoordGrid(3211, 3348), CoordGrid(3211, 3346), CoordGrid(3211, 3344),
                CoordGrid(3211, 3342), CoordGrid(3211, 3340), CoordGrid(3211, 3338), CoordGrid(3211, 3336),
                CoordGrid(3211, 3334), CoordGrid(3211, 3332), CoordGrid(3211, 3330), CoordGrid(3211, 3328),
                CoordGrid(3211, 3326), CoordGrid(3211, 3324),
                null, null, null, null, null, null, null, null, null,
            )
        val ATLAS_TILE = CoordGrid(3075, 3440)
    }
}
