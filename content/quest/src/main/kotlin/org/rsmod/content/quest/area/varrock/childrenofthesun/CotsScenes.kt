package org.rsmod.content.quest.area.varrock.childrenofthesun

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.util.OpFlags
import net.rsprot.protocol.game.outgoing.zone.header.UpdateZonePartialFollows
import net.rsprot.protocol.game.outgoing.zone.payload.LocAddChangeV2
import net.rsprot.protocol.game.outgoing.zone.payload.LocDel
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.content.quest.area.misthalin.SceneCamera
import org.rsmod.content.quest.area.misthalin.SceneFadeDuration
import org.rsmod.content.quest.area.misthalin.SceneFadeTicks
import org.rsmod.content.quest.area.misthalin.lockedScene
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.collision.CollisionFlagMap

internal interface CotsScenes {
    suspend fun delegation(access: ProtectedAccess)

    suspend fun eavesdrop(access: ProtectedAccess)

    suspend fun toRoof(access: ProtectedAccess)

    suspend fun interrogation(access: ProtectedAccess)
}

internal object CotsPlaces {
    val King = CoordGrid(3213, 3431)
    val Advisor = CoordGrid(3214, 3432)
    val Guards = listOf(CoordGrid(3212, 3433), CoordGrid(3215, 3433))
    val DelegationStart =
        listOf(
            CoordGrid(3222, 3430),
            CoordGrid(3223, 3431),
            CoordGrid(3223, 3429),
            CoordGrid(3224, 3430),
        )
    val DelegationStop =
        listOf(
            CoordGrid(3216, 3430),
            CoordGrid(3217, 3431),
            CoordGrid(3217, 3429),
            CoordGrid(3218, 3430),
        )
    val KnightsStart =
        listOf(
            CoordGrid(3224, 3432),
            CoordGrid(3224, 3428),
            CoordGrid(3225, 3431),
            CoordGrid(3225, 3429),
            CoordGrid(3226, 3430),
            CoordGrid(3226, 3429),
        )
    val KnightsStop =
        listOf(
            CoordGrid(3218, 3432),
            CoordGrid(3218, 3428),
            CoordGrid(3219, 3431),
            CoordGrid(3219, 3429),
            CoordGrid(3220, 3430),
            CoordGrid(3220, 3428),
        )
    val TobynScene = CoordGrid(3213, 3438)
    val BagGuardStart = CoordGrid(3212, 3441)
    val BagGuardStop = CoordGrid(3219, 3434)

    val DelegationCamera =
        SceneCamera(
            eye = CoordGrid(3223, 3425),
            eyeHeight = 450,
            lookAt = CoordGrid(3215, 3431),
            lookAtHeight = 100,
        )

    val EavesdropStart = CoordGrid(3254, 3397)
    val EavesdropWindow = CoordGrid(3259, 3398)
    val EavesdropCamera =
        SceneCamera(
            eye = CoordGrid(3252, 3396),
            eyeHeight = 490,
            lookAt = CoordGrid(3260, 3400),
            lookAtHeight = 290,
            rate = RecordedRate,
            rate2 = RecordedRate2,
        )
    val BanditsCamera =
        SceneCamera(
            eye = CoordGrid(3263, 3395),
            eyeHeight = 340,
            lookAt = CoordGrid(3260, 3403),
            lookAtHeight = 300,
            rate = RecordedRate,
            rate2 = RecordedRate2,
        )
    val HideoutNoOps =
        listOf(
            SceneLoc(
                CoordGrid(3258, 3397),
                LocShapeGround,
                0,
                "loc.fai_varrock_dead_tree_noop",
                "loc.fai_varrock_dead_tree",
            ),
            SceneLoc(
                CoordGrid(3259, 3400),
                LocShapeWall,
                2,
                "loc.vmq1_bandit_door_noop",
                "loc.vmq1_bandit_door",
            ),
            SceneLoc(
                CoordGrid(3262, 3403),
                LocShapeGround,
                2,
                "loc.fai_varrock_ladder_noop",
                "loc.fai_varrock_ladder",
            ),
        )
    val HideoutCutaway =
        listOf(
            SceneLoc(CoordGrid(3262, 3396), LocShapeWall, 3, InvisibleWall, PoorWallDouble),
            SceneLoc(CoordGrid(3263, 3396), LocShapeWall, 3, InvisibleWall, PoorWallDouble),
            SceneLoc(
                CoordGrid(3263, 3398),
                4,
                2,
                InvisibleDecor,
                "loc.fai_varrock_poor_wall_support2",
            ),
            SceneLoc(
                CoordGrid(3262, 3396),
                5,
                3,
                InvisibleDecor,
                "loc.fai_varrock_poor_wall_support",
            ),
            SceneLoc(
                CoordGrid(3263, 3396),
                5,
                3,
                InvisibleDecor,
                "loc.fai_varrock_poor_wall_crack",
            ),
        )

    val RoofArrival = CoordGrid(3203, 3474, 2)
    val RoofTobyn = CoordGrid(3202, 3473, 2)
    val RoofItzla = CoordGrid(3204, 3473, 2)
    val RoofCell = CoordGrid(3201, 3470, 2)
    val CellBanditFacing = CoordGrid(3203, 3470, 2)
    val CellTobyn = CoordGrid(3203, 3473, 2)
    val CellTobynFacing = CoordGrid(3203, 3471, 2)
    val CellPlayerStart = CoordGrid(3204, 3473, 2)
    val CellPlayerSteps =
        listOf(CoordGrid(3205, 3472, 2), CoordGrid(3205, 3471, 2), CoordGrid(3204, 3471, 2))
    val CellItzlaSteps =
        listOf(CoordGrid(3205, 3473, 2), CoordGrid(3205, 3471, 2), CoordGrid(3204, 3470, 2))
    val CellItzlaFacing = CoordGrid(3184, 3470, 2)
    val CellCamera =
        SceneCamera(
            eye = CoordGrid(3207, 3471, 2),
            eyeHeight = 311,
            lookAt = CoordGrid(3197, 3469, 2),
            lookAtHeight = 300,
            rate = RecordedRate,
            rate2 = RecordedRate2,
        )
    val CellGate = CoordGrid(3205, 3471, 2)
    val CellGateClosed =
        SceneLoc(CellGate, LocShapeWall, 1, "loc.vmq1_prisondoor_noop", "loc.vmq1_prisondoor")
    val CellGateGone = SceneLoc(CellGate, LocShapeWall, 1, InvisibleWall, "loc.vmq1_prisondoor")
    val CellGateOpenLeaf =
        SceneLoc(CoordGrid(3205, 3472, 2), LocShapeWall, 2, "loc.vmq1_prisondoor_noop", null)
    val CellGateLeafGone =
        SceneLoc(CoordGrid(3205, 3472, 2), LocShapeWall, 2, InvisibleWall, null)
    val CellCutaway =
        listOf(
            SceneLoc(CoordGrid(3207, 3471, 2), LocShapeWall, 0, InvisibleWall, CastleWall),
            SceneLoc(CoordGrid(3207, 3470, 2), 1, 0, InvisibleWall, CastleWall),
            SceneLoc(CoordGrid(3206, 3470, 2), 9, 0, InvisibleWall, CastleWall),
            SceneLoc(CoordGrid(3206, 3469, 2), 1, 0, InvisibleWall, CastleWall),
            SceneLoc(CoordGrid(3205, 3469, 2), 9, 0, InvisibleWall, CastleWall),
            SceneLoc(CoordGrid(3205, 3468, 2), LocShapeWall, 1, CastleWall, CastleWall, 1, 0),
        )

    private const val RecordedRate = 232
    private const val RecordedRate2 = 100
    private const val LocShapeWall = 0
    private const val LocShapeGround = 10
    private const val InvisibleWall = "loc.invisable_nonblocking_wall_noshadow"
    private const val InvisibleDecor = "loc.invisible_typeq"
    private const val PoorWallDouble = "loc.fai_varrock_poor_wall_double"
    private const val CastleWall = "loc.fai_varrock_castle_interior"
}

internal class WorldScenes
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val locRepo: LocRepository,
    private val collision: CollisionFlagMap,
) : CotsScenes {
    override suspend fun delegation(access: ProtectedAccess) {
        access.ifCloseChat()
        val actors = mutableListOf<Npc>()
        lateinit var delegates: List<Npc>
        lateinit var knights: List<Npc>
        lateinit var tobynActor: Npc
        lateinit var guard: Npc
        access.lockedScene(
            vantage = access.player.coords,
            faceAt = CotsPlaces.King,
            camera = CotsPlaces.DelegationCamera,
            underFade = {
                val east = CotsPlaces.DelegationCamera.eye
                actors += spawn("npc.vmq1_king_roald_cutscene", CotsPlaces.King, east)
                actors += spawn("npc.vmq1_aeonisig_raispher_cutscene", CotsPlaces.Advisor, east)
                for (post in CotsPlaces.Guards) {
                    actors += spawn("npc.fai_varrock_guard02", post, east)
                }
                delegates =
                    listOf(
                        spawn(CotsNpc.ItzlaCutscene, CotsPlaces.DelegationStart[0]),
                        spawn("npc.vmq1_servius_cutscene", CotsPlaces.DelegationStart[1]),
                        spawn("npc.vmq1_furia_cutscene", CotsPlaces.DelegationStart[2]),
                        spawn("npc.vmq1_ennius_cutscene", CotsPlaces.DelegationStart[3]),
                    )
                knights =
                    CotsPlaces.KnightsStart.mapIndexed { index, start ->
                        spawn("npc.vmq1_knight_${index + 1}", start)
                    }
                tobynActor = spawn("npc.vmq1_guard_sergeant_cutscene", CotsPlaces.TobynScene)
                guard = spawn("npc.vmq1_bag_guard", CotsPlaces.BagGuardStart)
                actors += delegates + knights + tobynActor + guard
            },
            teardown = { actors.forEach(::despawn) },
        ) {
            startDialogue {
                delegates.forEachIndexed { i, npc -> npc.walk(CotsPlaces.DelegationStop[i]) }
                knights.forEachIndexed { i, npc -> npc.walk(CotsPlaces.KnightsStop[i]) }
                delay(9)
                for (npc in delegates + knights) npc.faceSquare(CotsPlaces.King)
                delegationArrives()
                guard.walk(CotsPlaces.BagGuardStop)
                delay(1)
                tobynActor.faceSquare(CotsPlaces.BagGuardStart)
                guardLeaves()
                delay(1)
            }
        }
    }

    override suspend fun eavesdrop(access: ProtectedAccess) {
        access.ifCloseChat()
        val player = access.player
        access.lockedScene(
            vantage = CotsPlaces.EavesdropStart,
            faceAt = CotsPlaces.EavesdropStart.translateX(1),
            camera = CotsPlaces.EavesdropCamera,
            underFade = {
                player.showSceneLocs(CotsPlaces.HideoutNoOps)
                access.delay(1)
                access.walk(CotsPlaces.EavesdropWindow)
            },
            teardown = {
                player.restoreSceneLocs(CotsPlaces.HideoutNoOps + CotsPlaces.HideoutCutaway)
            },
        ) {
            awaitArrival(CotsPlaces.EavesdropWindow)
            startDialogue {
                eavesdropOpening()
                player.showSceneLocs(CotsPlaces.HideoutCutaway)
                access.cutTo(CotsPlaces.BanditsCamera)
                banditsTalk()
                access.cutTo(CotsPlaces.EavesdropCamera)
                eavesdropClosing()
            }
        }
    }

    override suspend fun toRoof(access: ProtectedAccess) {
        with(access) {
            fadeOverlay(0, 255, 0, 0, SceneFadeDuration)
            delay(1)
            minimapHideMap()
            delay(2)
            telejump(CotsPlaces.RoofArrival)
            faceSquare(CotsPlaces.RoofArrival.translateZ(-1))
            delay(1)
            fadeOverlay(0, 0, 0, 255, SceneFadeDuration)
            minimapReset()
            delay(SceneFadeTicks)
            closeFadeOverlayNow()
        }
    }

    override suspend fun interrogation(access: ProtectedAccess) {
        access.ifCloseChat()
        val player = access.player
        val cast = RoofCast(npcRepo, collision)
        val steps = CotsPlaces.CellItzlaSteps
        access.lockedScene(
            vantage = CotsPlaces.CellPlayerStart,
            faceAt = CotsPlaces.CellPlayerStart.translateZ(-1),
            camera = CotsPlaces.CellCamera,
            returnTo = CotsPlaces.RoofArrival,
            underFade = {
                cast.bandit?.faceSquare(CotsPlaces.CellBanditFacing)
                cast.tobyn?.let { cast.place(it, CotsPlaces.CellTobyn, CotsPlaces.CellTobynFacing) }
                cast.itzla?.let { cast.place(it, steps[0], steps[0].translateZ(-2)) }
                player.showSceneLocs(CotsPlaces.CellCutaway + CotsPlaces.CellGateClosed)
            },
            teardown = {
                player.restoreSceneLocs(
                    CotsPlaces.CellCutaway + CotsPlaces.CellGateClosed + CotsPlaces.CellGateOpenLeaf
                )
                cast.restore()
            },
        ) {
            cast.itzla?.let { cast.place(it, steps[1]) }
            openCellGate()
            player.showSceneLocs(listOf(CotsPlaces.CellGateGone, CotsPlaces.CellGateOpenLeaf))
            soundSynth(GateSound)
            delay(1)
            walk(CotsPlaces.CellPlayerSteps[0])
            cast.itzla?.let { cast.place(it, steps[2]) }
            delay(1)
            walk(CotsPlaces.CellPlayerSteps[1])
            cast.itzla?.faceSquare(CotsPlaces.CellItzlaFacing)
            delay(1)
            walk(CotsPlaces.CellPlayerSteps[2])
            delay(1)
            faceSquare(CotsPlaces.RoofCell)
            player.showSceneLocs(listOf(CotsPlaces.CellGateClosed, CotsPlaces.CellGateLeafGone))
            startDialogue {
                interrogationInCell(
                    wrapUp = { cast.itzla?.faceSquare(CotsPlaces.CellPlayerSteps[2]) }
                )
            }
        }
    }

    private fun openCellGate() {
        val type = ServerCacheManager.getObject(CellGateType.asRSCM(RSCMType.LOC)) ?: return
        val gate = locRepo.findExact(CotsPlaces.CellGate, type) ?: return
        locRepo.del(gate, CellGateOpenTicks)
    }

    private suspend fun ProtectedAccess.awaitArrival(dest: CoordGrid) {
        repeat(ArrivalTicks) {
            if (player.coords == dest) {
                return
            }
            delay(1)
        }
    }

    private fun ProtectedAccess.cutTo(camera: SceneCamera) {
        camMoveToV3(camera.eye, camera.eyeHeight, camera.rate, camera.rate2)
        camLookAtV3(camera.lookAt, camera.lookAtHeight, camera.rate, camera.rate2)
    }

    private fun spawn(type: String, coords: CoordGrid, facing: CoordGrid? = null): Npc {
        val npc = Npc(type, coords)
        npcRepo.add(npc, SceneLifespan)
        npc.noneMode()
        facing?.let(npc::faceSquare)
        return npc
    }

    private fun despawn(npc: Npc) {
        if (npc.isSlotAssigned) {
            npcRepo.del(npc, Int.MAX_VALUE)
        }
    }

    private companion object {
        const val SceneLifespan = 400
        const val ArrivalTicks = 8
        const val CellGateOpenTicks = 4
        const val CellGateType = "loc.vmq1_prisondoor"
        const val GateSound = "synth.iron_door_open"
    }
}

internal class RoofCast(npcRepo: NpcRepository, private val collision: CollisionFlagMap) {
    private val nearby =
        npcRepo.findAll(ZoneKey.from(CotsPlaces.RoofCell), zoneRadius = 1).toList()

    val bandit: Npc? = nearby.firstOrNull { it.isType(CotsNpc.CellBandit) }
    val tobyn: Npc? = nearby.firstOrNull { it.isType(CotsNpc.TobynRoofBase) }
    val itzla: Npc? = nearby.firstOrNull { it.isType(CotsNpc.ItzlaBase) }

    fun place(npc: Npc, coords: CoordGrid, facing: CoordGrid? = null) {
        if (!npc.isSlotAssigned) {
            return
        }
        npc.resetFaceEntity()
        npc.telejump(collision, coords)
        facing?.let(npc::faceSquare)
    }

    fun restore() {
        bandit?.let { place(it, CotsPlaces.RoofCell, CotsPlaces.RoofCell.translateX(-1)) }
        tobyn?.let { place(it, CotsPlaces.RoofTobyn, CotsPlaces.RoofTobyn.translateX(1)) }
        itzla?.let { place(it, CotsPlaces.RoofItzla, CotsPlaces.RoofItzla.translateX(-1)) }
    }
}

internal class SceneLoc(
    val coords: CoordGrid,
    val shape: Int,
    val angle: Int,
    val scene: String,
    val original: String?,
    val originalShape: Int = shape,
    val originalAngle: Int = angle,
)

internal fun Player.showSceneLocs(locs: Iterable<SceneLoc>) {
    for (loc in locs) sendLoc(loc.coords, loc.scene, loc.shape, loc.angle)
}

internal fun Player.restoreSceneLocs(locs: Iterable<SceneLoc>) {
    for (loc in locs) sendLoc(loc.coords, loc.original, loc.originalShape, loc.originalAngle)
}

private fun Player.sendLoc(coords: CoordGrid, loc: String?, shape: Int, angle: Int) {
    val zone = ZoneKey.from(coords).toCoords()
    val inZone = ZoneGrid.from(coords)
    client.write(UpdateZonePartialFollows(zone.x - buildArea.x, zone.z - buildArea.z, zone.level))
    val prot =
        if (loc == null) {
            LocDel(inZone.x, inZone.z, shape, angle)
        } else {
            val id = loc.asRSCM(RSCMType.LOC)
            LocAddChangeV2(id, inZone.x, inZone.z, shape, angle, OpFlags.ALL_SHOWN)
        }
    client.write(prot)
}

internal suspend fun Dialogue.delegationArrives() {
    alina(
        happy,
        "There they are: The children of the sun! That's Itzla Arkan, heir to the throne of " +
            "Varlamore! And there's Servius, the Teokan of Ralos!",
    )
    noah(confused, "Teokan? What's a Teokan?")
    alina(neutral, "It means High Priest. He's the religious leader in Varlamore.")
    noah(bored, "Huh...")
}

internal suspend fun Dialogue.guardLeaves() {
    tobyn(quiz, "What are you doing with that bag?")
    bagGuard(worried, "Oh... Er... Just delivering some supplies to the guards at the gates.")
    tobyn(neutral, "Well hurry back. We need everyone at their posts.")
    noah(quiz, "That's an unusually big bag he's carrying.")
    chatPlayer(quiz, "Hmm... Now where might you be going with that...?")
}

internal suspend fun Dialogue.eavesdropOpening() {
    chatPlayer(quiz, "What are you up to in there...?")
}

internal suspend fun Dialogue.banditsTalk() {
    bandit(CotsNpc.RedHood, angry, "You're late.")
    bagGuard(
        angry,
        "I'm here aren't I? If you're not happy, maybe you should have been the one to steal " +
            "the uniforms!",
    )
    bandit(CotsNpc.Woman, verymad, "Enough! We don't have much time. Do you have everything?")
    bagGuard(shifty, "Mostly.")
    bandit(CotsNpc.RedHood, confused, "Mostly?")
    bagGuard(worried, "I grabbed what I could!")
    bandit(
        CotsNpc.Bearded,
        neutral,
        "We're just going to have to make do. The delegation is already in the palace. We " +
            "need to be ready to strike as they leave.",
    )
    bandit(
        CotsNpc.Tanned,
        neutral,
        "Indeed. You all know the target. Get changed quick and be ready to take your " +
            "positions.",
    )
    bagGuard(shifty, "Good luck. You're going to need it.")
    bandit(
        CotsNpc.Bearded,
        laugh,
        "You lot panic too much. It's going to be fine. Now, let's get going.",
    )
}

internal suspend fun Dialogue.eavesdropClosing() {
    chatPlayer(worried, "This doesn't sound good. I'd better let that sergeant know.")
}

internal suspend fun Dialogue.interrogationInCell(wrapUp: () -> Unit = {}) {
    val name = access.player.displayName
    itzla(
        neutral,
        "Nilsal to you, iknami. I hear you managed to get yourself into a spot of bother.",
    )
    bandit(CotsNpc.Bearded, angry, "I'm not telling you anything.")
    itzla(
        happy,
        "Ah, tetamo! Such a shame. No matter though! We'll have you talking in no time.",
    )
    itzla(
        quiz,
        "Now, you'll need to forgive me, but I'm a bit new around here, so your customs are " +
            "not familiar to me. Tell me, iknami, what's the preferred method of interrogation " +
            "in these parts?",
    )
    bandit(CotsNpc.Bearded, confused, "You're asking me how you should interrogate me?")
    itzla(
        happy,
        "Well it just seemed polite to ask. We have all sorts of fun methods back home in " +
            "Varlamore. We could give one or two of them a go if you want?",
    )
    bandit(CotsNpc.Bearded, worried, "If I want?")
    itzla(happy, "Absolutely! Now, what are your thoughts on chicken?")
    bandit(CotsNpc.Bearded, confused, "Chicken?")
    itzla(
        quiz,
        "Oh, $name, do you not have chickens here? Sorry, again, all this is quite new to me.",
    )
    chatPlayer(neutral, "Yes, we have chickens. The farms around here have loads.")
    itzla(
        quiz,
        "Well then, why the confusion? Have you never seen one before? Oh you poor sheltered " +
            "thing. Did your parents never show you the wonders of the countryside?",
    )
    bandit(CotsNpc.Bearded, angry, "I know what a chicken is!")
    itzla(laugh, "Well why didn't you say so?")
    bandit(
        CotsNpc.Bearded,
        verymad,
        "I don't know what game you're playing, but it won't work on me!",
    )
    itzla(
        neutral,
        "No game at all, iknami. The reason I ask is because one of our preferred methods in " +
            "Varlamore involves just a small amount of fire, and you'd never believe it, but " +
            "the smell of human flesh once it gets going...",
    )
    bandit(CotsNpc.Bearded, shocked, "You're going to cook me?")
    itzla(
        laugh,
        "Cook? No, not at all! That would imply I'm going to eat you, and I'm not a " +
            "barbarian! Though, it would be interesting to know if it tastes like chicken as " +
            "well...",
    )
    bandit(CotsNpc.Bearded, worried, "Alright, enough! I'll tell you what you want!")
    itzla(happy, "Kuaini!")
    itzla(quiz, "So who are you working for?")
    bandit(
        CotsNpc.Bearded,
        sad,
        "I don't know their name. They just paid us to attack the delegation. They didn't look " +
            "to be from around here. I reckon it was one of your lot!",
    )
    itzla(quiz, "Why did they want you to attack the delegation?")
    bandit(CotsNpc.Bearded, sad, "They didn't say, but that priest... he was the target!")
    itzla(
        confused,
        "So a Varlamorian paid you to assassinate the Teokan? You're not giving me much to go " +
            "on.",
    )
    bandit(CotsNpc.Bearded, worried, "That's all I know!")
    wrapUp()
    itzla(neutral, "Hmm...")
    itzla(neutral, "Well I think we're done here. Come, $name.")
}
