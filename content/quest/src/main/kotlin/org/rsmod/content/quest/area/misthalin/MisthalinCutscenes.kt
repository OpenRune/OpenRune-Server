package org.rsmod.content.quest.area.misthalin

import dev.openrune.types.MesAnimType
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.content.quest.manager.Quest
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

// Do not transform shared world actors for a private cutscene; use scene actors and explicit chatheads.
internal class MisthalinCutscenes(
    private val quest: Quest,
    private val regionRepo: RegionRepository,
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val objRepo: ObjRepository,
    private val collision: CollisionFlagMap,
) {

    private val sceneRegions = HashMap<Long, Region>()

    fun ScriptContext.register() {
        onPlayerLogin { discardLeakedRegion(player) }
    }

    private fun discardLeakedRegion(player: Player) {
        val region = player.uuid?.let(sceneRegions::remove) ?: return
        regionRepo.unprotect(region)
    }

    suspend fun sid(access: ProtectedAccess) {
        lateinit var killer: Npc
        lateinit var sid: Npc
        access.scene(
            vantage = SidVantage,
            faceAt = SidSpawn,
            camera = SidCamera,
            underFade = { r ->
                sid = spawn(SidVis, r[SidSpawn])
                killer = spawn(HeweyKiller, r[SidKillerStart])
            },
        ) { r ->
            sid.anim("seq.emote_dance")
            sidAlive(sid, drunk, "Woo, party on bro!")
            sid.anim("seq.emote_dance")

            killer.walk(r[SidSpawn.translateZ(1)])
            access.delay(6)
            killer.faceSquare(r[SidSpawn])
            access.delay(1)

            sidAlive(
                sid,
                confused,
                "Woah, that wind totally just felt like someone breathing on my neck!",
            )

            killer.anim("seq.hween16_slash")
            sound("synth.scythe_double")
            access.delay(1)
            sound("synth.barbarian_grunt_death")

            sidWoozy(sid, shocked, "Whoah, not cool bro...")
            sidWoozy(sid, neutral, "...urgh...")
            sidWoozy(sid, sad, "...I'm feeling woozy...")
            sidWoozy(sid, shocked, "...gargh...")
            sidWoozy(sid, shocked, "...I see the light...")
            sidWoozy(sid, shocked, "...urrggghhh...")
            sidWoozy(sid, shocked, "...it's not my time...glurgh")
            sidWoozy(sid, shocked, "...gargle...urgh...")
            heweyKiller(killer, bored, "...")
            chatPlayer(bored, "...")

            sid.walk(r[SidSpawn.translateZ(-2)])
            camera(r, SidStaggerCamera)

            sidWoozy(sid, shocked, "...glargh...help...me...")
            sidWoozy(sid, shocked, "...urgh...hergle...")
            sidWoozy(sid, neutral, "...")
            sidWoozy(sid, neutral, "...")
            sidWoozy(sid, neutral, "...flergh.")

            sid.anim("seq.human_death")
            sound("synth.human_death")

            mesbox(
                "Sid collapses to the ground in a crumpled mess. As he falls down you spot " +
                    "something shiny fall out of his shirt pocket and into the barrel of water."
            )

            chatPlayer(shocked, "Gosh, that was horrible and so drawn out!")

            sound("synth.barbarian_grunt_death")
            sidWoozy(sid, neutral, "...urgh...")
            chatPlayer(bored, "Oh wow, he's still going...")

            sidWoozy(sid, silent, "...")
            sidWoozy(sid, silent, "...")

            chatPlayer(quiz, "I think he is actually dead this time.")

            access.faceSquare(killer.coords)
            chatPlayer(confused, "Who are you? Why are you doing this!?")
            access.faceSquare(killer.coords)
            heweyKiller(killer, shifty, "...")

            killer.walk(r[SidKillerExit])
            access.delay(6)
            quest.setQuestStage(access, MisthalinStage.SidDead)
            despawn(killer)
            despawn(sid)

            chatPlayer(angry, "Gah! I've got to find a way into that house and stop this maniac!")
            chatPlayer(quiz, "I wonder what it was that I saw fall into that barrel?")
        }
    }

    suspend fun tayten(access: ProtectedAccess) {
        lateinit var tayten: Npc
        access.scene(
            vantage = TaytenVantage,
            faceAt = TaytenSpawn,
            camera = TaytenCamera,
            underFade = { r -> tayten = spawn(TaytenVis, r[TaytenSpawn]) },
        ) { r ->
            val killer = spawn(HeweyKiller, r[TaytenKillerStart])
            openWardrobe(r[TaytenWardrobe], DraynorWardrobeOpen, LocAngle.South, WardrobeSwingTicks)
            access.delay(3)

            killer.walk(r[TaytenSlashPost])
            access.delay(3)

            killer.faceSquare(r[TaytenSpawn])
            access.delay(1)

            killer.anim("seq.hween16_slash")
            sound("synth.scythe_double")
            access.delay(1)

            sound("synth.scream2")
            taytenSays(tayten, shocked, "Gurgle...")
            tayten.anim("seq.human_death")
            sound("synth.human_death")
            chatPlayer(shocked, "Nooooo!")

            killer.walk(r[TaytenNotePost])
            access.delay(3)
            killer.faceSquare(r[TaytenNoteTile])
            killer.anim("seq.human_pickupfloor")
            access.delay(2)
            sound("synth.paper_move")

            objRepo.add(LibraryNote, r[TaytenNoteTile], NoteVisibleTicks)
            access.delay(1)

            killer.walk(r[TaytenKillerStart])
            access.delay(3)
            killer.faceSquare(r[TaytenWardrobe])
            killer.anim("seq.human_openbigcupboard")
            access.delay(1)
            killer.anim("seq.human_reachforladder")
            openWardrobe(r[TaytenWardrobe], DraynorWardrobeOpen, LocAngle.South, WardrobeSwingTicks)
            access.delay(1)
            despawn(killer)
            despawn(tayten)

            quest.setQuestStage(access, MisthalinStage.TaytenDead)

            chatPlayer(confused, "What's this? A note? I guess I should read it.")
        }
    }

    suspend fun lacey(access: ProtectedAccess) {
        lateinit var killer: Npc
        lateinit var lacey: Npc
        access.scene(
            vantage = LaceyVantage,
            faceAt = LaceySpawn,
            camera = LaceyCamera,
            underFade = { r ->
                lacey = spawn(LaceyVis, r[LaceySpawn])
                killer = spawn(AbigaleKiller, r[LaceyKillerStart])
            },
        ) { r ->
            laceySays(lacey, scared, "Please, stop this, just let me go!")
            abigaleKiller(killer, neutral, "We're going to play a game. Do you like to play games?")
            laceySays(lacey, scared, "No...yes....whatever you say, just don't hurt me!")
            abigaleKiller(
                killer,
                neutral,
                "Whether you get hurt or not is entirely up to you. Your fate is in your own hands.",
            )
            laceySays(lacey, confused, "What!? What do you mean!?")
            chatPlayer(angry, "Leave her alone you monster!")
            abigaleKiller(
                killer,
                neutral,
                "Ah, ${player.displayName}, how kind of you to join us. Now if you say another " +
                    "word, I will end her right now!",
            )
            abigaleKiller(killer, neutral, "Sorry, where were we? Ah yes, of course! The game!")
            abigaleKiller(killer, neutral, "Do you like quests, Lacey?")
            laceySays(lacey, confused, "Ummm, sure, I like quests.")
            abigaleKiller(killer, neutral, "Do you like scary quests?")
            laceySays(lacey, neutral, "Uh-huh.")
            abigaleKiller(killer, neutral, "What's your favourite scary quest?")
            laceySays(lacey, quiz, "I don't know.")
            abigaleKiller(killer, neutral, "You have to have a favourite.")
            laceySays(
                lacey,
                neutral,
                "Umm.....the one with that vampyre in it who lives in a big house.",
            )
            abigaleKiller(killer, neutral, "Good, good. And now for that game I spoke about.")
            abigaleKiller(
                killer,
                neutral,
                "I am going to ask you one question. Get it wrong and you die, but get it right " +
                    "and you live.",
            )
            laceySays(lacey, scared, "Oh Guthix please no, please don't, I don't want to die!")
            abigaleKiller(
                killer,
                neutral,
                "Then think hard about your answer. You like vampyres so this should be easy for " +
                    "you. The question is this...",
            )
            abigaleKiller(killer, neutral, "Who is the Vampyre that resides in South Misthalin?")
            laceySays(lacey, quiz, "Urm...")

            val answer =
                choice4(
                    "Count Draynor",
                    "Count Draynor",
                    "Count Check",
                    "Count Check",
                    "Lord Drakan",
                    "Lord Drakan",
                    "Say nothing",
                    null,
                    title = "Interrupt with answer?",
                )

            if (answer != null) {
                chatPlayer(happy, "$answer!")
                abigaleKiller(
                    killer,
                    angry,
                    "This game didn't involve you ${player.displayName}! Now her death is on " +
                        "you! Her blood is on your hands you fool!",
                )
            }

            killer.anim("seq.hween16_slash")
            sound("synth.scythe_double")

            sound("synth.scream2")
            laceySays(lacey, sad, "Gargle...urgh...")
            lacey.anim("seq.human_death")
            sound("synth.human_death")

            access.delay(2)
            killer.walk(r[LaceyNotePost])
            access.delay(5)
            killer.faceSquare(r[LaceyNote])
            access.delay(2)

            killer.anim("seq.myarm_human_players_vile")
            quest.setQuestStage(access, MisthalinStage.LaceyDead)
            access.delay(2)
            sound("synth.paper_move")

            access.delay(1)
            killer.walk(r[LaceyKillerExit])
            access.delay(13)
            despawn(killer)
            despawn(lacey)

            chatPlayer(
                angry,
                "That monster! I have to stop them! Let's see what that new note says.",
            )
        }
    }

    suspend fun mandy(access: ProtectedAccess) {
        lateinit var mandy: Npc
        access.scene(
            vantage = MandyVantage,
            faceAt = MandySpawn,
            camera = MandyCamera,
            underFade = { r ->
                mandy = spawn(MandyVis, r[MandySpawn])

                objRepo.add(BgsProp, r[GodswordProp], ScenePropTicks)
            },
        ) { r ->
            val noise = spawn("npc.mistmyst_invisible_npc", r[MandyNoiseWest])

            mandySays(mandy, sad, "Why am I the one who is always left clearing up after everyone?")

            noise.say("*shuffle*")
            sound("synth.tree_rustle_01")
            mandySays(mandy, quiz, "Hello? Anyone there?")

            noise.telejump(r[MandyNoiseNorth])
            noise.say("*rustle*")
            sound("synth.tree_rustle_02")
            mandySays(mandy, quiz, "Hello? Lacey? Sid?")

            noise.telejump(r[MandyNoiseKitchen])
            noise.say("*scratch*")
            sound("synth.skullball_scratchy")
            noise.walk(r[MandyNoiseKitchenExit])

            mandySays(
                mandy,
                quiz,
                "Hmm, well I guess I should go check it out. No point staying in this nice, " +
                    "locked room when I could go outside into the creepy darkness.",
            )
            chatPlayer(neutral, "Seriously!?")
            mesbox("The walls and door seem too thick for Mandy to be able to hear you.")
            mandySays(
                mandy,
                neutral,
                "Oh, and I don't think there is any need to take this big, shiny weapon with me " +
                    "in order to defend myself. Nothing bad ever happens when people go check out " +
                    "noises in the dark outside of houses!",
            )
            chatPlayer(neutral, "You have got to be kidding...")
            mandySays(mandy, quiz, "Ok, well, here I go!")

            despawn(noise)
            camera(r, MandyOutsideCamera)

            mandy.walk(r[MandyDoorInside])
            access.delay(3)
            setKitchenDoor(r, open = true)
            access.delay(1)
            mandy.walk(r[MandyOutside])
            access.delay(3)

            mandy.faceSquare(r[MandyOutsideLookWest])
            mandySays(mandy, quiz, "Hello, anyone out here?")

            mandy.faceSquare(r[MandyNoiseKitchenExit])
            access.delay(2)
            mandy.faceSquare(r[MandyOutsideLookSouthWest])
            access.delay(2)
            mandySays(
                mandy,
                quiz,
                "See, I knew nothing would happen! Must have just been a squirrel or something " +
                    "running about.",
            )

            mandy.walk(listOf(r[MandyKillPost], r[MandySpawn]))
            access.delay(5)
            setKitchenDoor(r, open = false)

            if (mandy.coords != r[MandySpawn]) {
                mandy.telejump(r[MandySpawn])
            }

            mandySays(mandy, quiz, "Oh well, back to the dishes.")
            chatPlayer(neutral, "Phew, that was lucky!")

            val killer = spawn(AbigaleKiller, r[MandyKillerStart])
            openWardrobe(r[MandyWardrobe], SpookyWardrobeOpen, LocAngle.East, WardrobeSwingTicks)
            killer.faceSquare(r[MandyWardrobe])
            access.delay(3)

            killer.walk(r[MandyKillPost])
            access.delay(3)
            killer.faceSquare(r[MandySpawn])
            access.delay(1)
            killer.anim("seq.hween16_slash")
            sound("synth.scythe_double")

            sound("synth.scream2")
            mandySays(mandy, sad, "No...why...me...")
            mandy.anim("seq.human_death")
            sound("synth.human_death")

            chatPlayer(angry, "That psycho! He was just toying with her!")

            killer.walk(r[MandyNotePost])
            access.delay(3)
            killer.faceSquare(r[MandyVantage])
            killer.anim("seq.human_pickupfloor")
            access.delay(2)
            sound("synth.paper_move")

            killer.walk(r[MandyKillerStart])
            access.delay(4)
            killer.faceSquare(r[MandyWardrobe])
            killer.anim("seq.human_openbigcupboard")
            access.delay(1)
            killer.anim("seq.human_reachforladder")
            openWardrobe(r[MandyWardrobe], SpookyWardrobeOpen, LocAngle.East, WardrobeSwingTicks)
            access.delay(1)
            despawn(killer)

            camera(r, MandyCamera)

            chatPlayer(
                neutral,
                "Another note. This guy really likes playing games, but this is going to be a " +
                    "game he won't win!",
            )
            quest.setQuestStage(access, MisthalinStage.MandyDead)
        }
    }

    private suspend fun Dialogue.sidAlive(sid: Npc, mesanim: MesAnimType, text: String) {
        access.faceSquare(sid.coords)
        chatNpcSpecific("Sid", "npc.mistmyst_sid_vis", mesanim, text)
    }

    private suspend fun Dialogue.sidWoozy(sid: Npc, mesanim: MesAnimType, text: String) {
        access.faceSquare(sid.coords)
        chatNpcSpecific("Sid", "npc.mistmyst_sid_woozy", mesanim, text)
    }

    private suspend fun Dialogue.laceySays(lacey: Npc, mesanim: MesAnimType, text: String) {
        access.faceSquare(lacey.coords)
        chatNpcSpecific("Lacey", "npc.mistmyst_lacey_vis", mesanim, text)
    }

    private suspend fun Dialogue.mandySays(mandy: Npc, mesanim: MesAnimType, text: String) {
        access.faceSquare(mandy.coords)
        chatNpcSpecific("Mandy", "npc.mistmyst_mandy_vis", mesanim, text)
    }

    private suspend fun Dialogue.taytenSays(tayten: Npc, mesanim: MesAnimType, text: String) {
        access.faceSquare(tayten.coords)
        chatNpcSpecific("Tayten", TaytenHead, mesanim, text)
    }

    private suspend fun Dialogue.heweyKiller(killer: Npc, mesanim: MesAnimType, text: String) {
        access.faceSquare(killer.coords)
        chatNpcSpecific("Killer", HeweyKiller, mesanim, text)
    }

    private suspend fun Dialogue.abigaleKiller(killer: Npc, mesanim: MesAnimType, text: String) {
        access.faceSquare(killer.coords)
        chatNpcSpecific("Killer", AbigaleKiller, mesanim, text)
    }

    private fun Dialogue.sound(synth: String) {
        access.soundSynth(synth)
    }

    // Map every camera pan through the scene region, including mid-scene reframing.
    private fun Dialogue.camera(r: SceneRegion, camera: SceneCamera, rate: Int = SceneDriftRate) {
        val mapped = r.map(camera)
        access.camMoveToV3(mapped.eye, height = mapped.eyeHeight, rate = rate, rate2 = rate)
        access.camLookAtV3(mapped.lookAt, height = mapped.lookAtHeight, rate = rate, rate2 = rate)
    }

    private fun Dialogue.openWardrobe(
        tile: CoordGrid,
        openLeaf: String,
        angle: LocAngle,
        ticks: Int,
    ) {
        locRepo.add(tile, openLeaf, ticks, angle, LocShape.CentrepieceStraight)
        sound("synth.creakydoor_open")
    }

    // Player-paced dialogue can outlast timed props; keep scene dressing alive until teardown.
    private fun Dialogue.setKitchenDoor(r: SceneRegion, open: Boolean) {
        val closed = r[MandyKitchenDoor]
        val opened = r[MandyKitchenDoorOpen]

        val standing = if (open) closed else opened
        locRepo.findExact(standing, LocShape.WallStraight)?.let {
            locRepo.del(it, KitchenDoorRevert)
        }
        if (open) {
            locRepo.add(
                opened,
                PanelledDoorOpen,
                KitchenDoorRevert,
                LocAngle.East,
                LocShape.WallStraight,
            )
            sound("synth.door_open")
        } else {
            locRepo.add(
                closed,
                PanelledDoor,
                KitchenDoorRevert,
                LocAngle.North,
                LocShape.WallStraight,
            )
            sound("synth.door_close")
        }
    }

    private fun Npc.telejump(coords: CoordGrid) {
        PathingEntityCommon.telejump(this, collision, coords)
    }

    private val Dialogue.scared: MesAnimType
        get() = worried

    private suspend fun ProtectedAccess.scene(
        vantage: CoordGrid,
        faceAt: CoordGrid,
        camera: SceneCamera,
        underFade: (SceneRegion) -> Unit = {},
        body: suspend Dialogue.(SceneRegion) -> Unit,
    ) {
        val region = regionRepo.add(MisthalinIslandTemplate) ?: run {
            mes("The area is unavailable at the moment. Please try again shortly.")
            return
        }
        regionRepo.protect(region)
        val scene = SceneRegion(region)

        player.misthalinReturnTile = vantage.packed
        player.uuid?.let { uuid -> sceneRegions[uuid] = region }

        try {
            lockedScene(
                vantage = scene[vantage],
                faceAt = scene[faceAt],
                camera = scene.map(camera),
                returnTo = vantage,
                underFade = { underFade(scene) },
            ) {
                startDialogue { body(scene) }
            }
        } finally {
            player.misthalinReturnTile = 0
            player.uuid?.let(sceneRegions::remove)

            regionRepo.unprotect(region)
        }
    }

    private fun spawn(type: String, coords: CoordGrid): Npc {
        val npc = Npc(type, coords)
        npcRepo.add(npc, SceneLifespan)
        npc.noneMode()
        return npc
    }

    private fun despawn(npc: Npc) {
        if (!npc.isSlotAssigned) {
            return
        }
        npcRepo.del(npc, Int.MAX_VALUE)
    }

    fun assertSceneCoords() {
        assertMisthalinSceneCoords(
            listOf(
                "SidSpawn" to SidSpawn,
                "SidVantage" to SidVantage,
                "SidKillerStart" to SidKillerStart,
                "SidKillerExit" to SidKillerExit,
                "SidCamera.eye" to SidCamera.eye,
                "SidCamera.lookAt" to SidCamera.lookAt,
                "SidStaggerCamera.eye" to SidStaggerCamera.eye,
                "TaytenSpawn" to TaytenSpawn,
                "TaytenVantage" to TaytenVantage,
                "TaytenKillerStart" to TaytenKillerStart,
                "TaytenSlashPost" to TaytenSlashPost,
                "TaytenNotePost" to TaytenNotePost,
                "TaytenWardrobe" to TaytenWardrobe,
                "TaytenCamera.eye" to TaytenCamera.eye,
                "TaytenCamera.lookAt" to TaytenCamera.lookAt,
                "LaceySpawn" to LaceySpawn,
                "LaceyVantage" to LaceyVantage,
                "LaceyKillerStart" to LaceyKillerStart,
                "LaceyNotePost" to LaceyNotePost,
                "LaceyNote" to LaceyNote,
                "LaceyKillerExit" to LaceyKillerExit,
                "LaceyCamera.eye" to LaceyCamera.eye,
                "LaceyCamera.lookAt" to LaceyCamera.lookAt,
                "MandySpawn" to MandySpawn,
                "MandyVantage" to MandyVantage,
                "MandyNoiseWest" to MandyNoiseWest,
                "MandyNoiseNorth" to MandyNoiseNorth,
                "MandyNoiseKitchen" to MandyNoiseKitchen,
                "MandyNoiseKitchenExit" to MandyNoiseKitchenExit,
                "MandyKitchenDoor" to MandyKitchenDoor,
                "MandyKitchenDoorOpen" to MandyKitchenDoorOpen,
                "MandyDoorInside" to MandyDoorInside,
                "MandyOutside" to MandyOutside,
                "MandyOutsideLookWest" to MandyOutsideLookWest,
                "MandyOutsideLookSouthWest" to MandyOutsideLookSouthWest,
                "MandyKillerStart" to MandyKillerStart,
                "MandyWardrobe" to MandyWardrobe,
                "MandyKillPost" to MandyKillPost,
                "MandyNotePost" to MandyNotePost,
                "MandyCamera.eye" to MandyCamera.eye,
                "MandyCamera.lookAt" to MandyCamera.lookAt,
                "MandyOutsideCamera.eye" to MandyOutsideCamera.eye,
            )
        )
    }

    private companion object {

        // Spawn visible cutscene leaves; their parents may resolve to -1 outside a quest stage window.
        private const val LibraryNote = "obj.mistmyst_clue_library"

        // Region copies include locs, not ground items; add the godsword to the copied scene.
        private const val BgsProp = "obj.mistmyst_bgs_prop"
        private val GodswordProp = CoordGrid(1626, 4843, 0)

        private const val ScenePropTicks = 400

        private const val SidVis = "npc.mistmyst_sid_vis"
        private const val TaytenVis = "npc.mistmyst_tayten_vis"
        private const val LaceyVis = "npc.mistmyst_lacey_vis"
        private const val MandyVis = "npc.mistmyst_mandy_vis"

        private const val HeweyKiller = "npc.mistmyst_hewey_killer"
        private const val AbigaleKiller = "npc.mistmyst_abigale_killer"
        private const val TaytenHead = "npc.mistmyst_tayten_vis"

        private const val SceneLifespan = 3000

        private const val DraynorWardrobeOpen = "loc.draynor_wardrobe_open"
        private const val SpookyWardrobeOpen = "loc.spookywardrobe_open"

        private val SidSpawn = CoordGrid(1615, 4832, 0)

        private val SidVantage = CoordGrid(1615, 4827, 0)
        private val SidKillerStart = CoordGrid(1619, 4835, 0)

        private val SidKillerExit = CoordGrid(1615, 4837, 0)

        private val SidCamera =
            SceneCamera(
                eye = CoordGrid(1614, 4826, 0),
                eyeHeight = 300,
                lookAt = CoordGrid(1616, 4831, 0),
                lookAtHeight = 275,
            )

        private val SidStaggerCamera =
            SceneCamera(
                eye = CoordGrid(1618, 4836, 0),
                eyeHeight = 500,
                lookAt = SidSpawn,
                lookAtHeight = 500,
            )

        private val TaytenSpawn = CoordGrid(1639, 4838, 0)
        private val TaytenVantage = CoordGrid(1635, 4837, 0)

        private val TaytenKillerStart = CoordGrid(1637, 4836, 0)
        private val TaytenSlashPost = CoordGrid(1638, 4838, 0)
        private val TaytenNotePost = CoordGrid(1636, 4838, 0)

        private val TaytenWardrobe = CoordGrid(1637, 4835, 0)

        private val TaytenNoteTile = CoordGrid(1635, 4838, 0)

        private const val NoteVisibleTicks = 200

        private const val WardrobeSwingTicks = 5

        private val TaytenCamera =
            SceneCamera(
                eye = CoordGrid(1634, 4841, 0),
                eyeHeight = 1250,
                lookAt = CoordGrid(1637, 4838, 0),
                lookAtHeight = 250,
            )

        private val LaceySpawn = CoordGrid(1629, 4850, 0)
        private val LaceyVantage = CoordGrid(1631, 4849, 0)

        private val LaceyKillerStart = CoordGrid(1629, 4851, 0)

        private val LaceyNotePost = CoordGrid(1632, 4851, 0)
        private val LaceyNote = CoordGrid(1632, 4850, 0)

        private val LaceyKillerExit = CoordGrid(1620, 4851, 0)

        private val LaceyCamera =
            SceneCamera(
                eye = CoordGrid(1625, 4847, 0),
                eyeHeight = 375,
                lookAt = CoordGrid(1632, 4850, 0),
                lookAtHeight = 375,
            )

        private val MandySpawn = CoordGrid(1626, 4840, 0)
        private val MandyVantage = CoordGrid(1630, 4842, 0)

        private val MandyNoiseWest = CoordGrid(1624, 4839, 0)
        private val MandyNoiseNorth = CoordGrid(1624, 4843, 0)
        private val MandyNoiseKitchen = CoordGrid(1626, 4837, 0)
        private val MandyNoiseKitchenExit = CoordGrid(1624, 4837, 0)

        private val MandyKitchenDoor = CoordGrid(1627, 4837, 0)
        private val MandyKitchenDoorOpen = CoordGrid(1627, 4838, 0)
        private val MandyDoorInside = CoordGrid(1627, 4838, 0)
        private val MandyOutside = CoordGrid(1627, 4836, 0)

        private val MandyOutsideLookWest = CoordGrid(1626, 4836, 0)
        private val MandyOutsideLookSouthWest = CoordGrid(1624, 4835, 0)

        private const val PanelledDoor = "loc.draynor_panelled_door"
        private const val PanelledDoorOpen = "loc.draynor_panelled_door_open"

        private const val KitchenDoorRevert = 500

        private val MandyKillerStart = CoordGrid(1628, 4838, 0)
        private val MandyWardrobe = CoordGrid(1629, 4838, 0)
        private val MandyKillPost = CoordGrid(1627, 4840, 0)
        private val MandyNotePost = CoordGrid(1629, 4842, 0)

        private val MandyOutsideCamera =
            SceneCamera(
                eye = CoordGrid(1622, 4837, 0),
                eyeHeight = 1400,
                lookAt = CoordGrid(1627, 4840, 0),
                lookAtHeight = 250,
            )

        private val MandyCamera =
            SceneCamera(
                eye = CoordGrid(1634, 4848, 0),
                eyeHeight = 1400,
                lookAt = CoordGrid(1627, 4840, 0),
                lookAtHeight = 250,
            )
    }
}
