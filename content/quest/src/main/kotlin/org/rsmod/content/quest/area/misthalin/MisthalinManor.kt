package org.rsmod.content.quest.area.misthalin

import kotlin.math.abs
import org.rsmod.api.config.constants
import org.rsmod.api.player.output.CamShakeAxis
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLocU
import org.rsmod.content.quest.manager.Quest
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

internal class MisthalinManor(
    private val quest: Quest,
    private val locRepo: LocRepository,
    private val cutscenes: MisthalinCutscenes,
    private val puzzles: MisthalinPuzzles,
) {

    fun register(ctx: ScriptContext): Unit =
        with(ctx) {
            onOpLoc1("loc.mistmyst_stairs_up") {
                mes("The staircase looks too rickety to climb.")
            }
            onOpLoc1("loc.mistmyst_empty_bucket") {
                takeTool("obj.bucket_empty", PickupFloor, "You take the bucket.")
            }
            onOpLoc1("loc.mistmyst_table_knife") {
                takeTool("obj.knife", PickupTable, "You take the knife from the table.")
            }
            onOpLoc1("loc.mistmyst_shelves_tinderbox") {
                takeTool("obj.tinderbox", PickupTable, "You take a tinderbox from the shelves.")
            }

            onOpLoc1("loc.mistmyst_barrel") { searchBarrel() }
            onOpLocU("loc.mistmyst_barrel", "obj.bucket_empty") { fillBucket(it.invSlot) }

            for (leaf in FrontLeaves) {
                onOpLoc1(leaf.closed) { enterFront() }
            }

            onOpLoc1("loc.mistmyst_door_redtopaz") { taytenDoor() }
            onOpLoc1("loc.mistmyst_clue_library") { takeNote(Note1Obj) }
            onOpHeld1(Note1Obj) {
                readNote(Note1Lines, MisthalinStage.TaytenDead, MisthalinStage.Note1Read)
            }
            onOpLocU("loc.mistmyst_painting", "obj.knife") { slashPainting(it.loc) }
            onOpLoc1("loc.mistmyst_painting") {
                searchContainer(
                    revealedAt = MisthalinStage.PaintingSlashed,
                    key = "obj.mistmyst_ruby_key",
                    text = "You find a key hidden inside the slashed painting.",
                )
            }

            onOpLoc1("loc.mistmyst_door_ruby") { rubyDoor(it.loc) }
            for ((index, candle) in CandleLocs.withIndex()) {
                onOpLocU(candle, "obj.tinderbox") { lightCandle(index) }
            }
            onOpLocU("loc.mistmyst_explosive_barrel", "obj.tinderbox") { lightFuse() }
            onOpLoc1("loc.mistmyst_destructable_wall_climbable") { climbBrokenWall() }

            onOpLoc1("loc.mistmyst_tree") { observeTree() }
            onOpLoc1("loc.mistmyst_clue_outside") { takeNote(Note2Obj) }
            onOpHeld1(Note2Obj) {
                readNote(Note2Lines, MisthalinStage.LaceyDead, MisthalinStage.Note2Read)
            }
            onOpLoc3("loc.mistmyst_piano") {
                searchContainer(
                    revealedAt = MisthalinStage.PianoSolved,
                    key = "obj.mistmyst_emerald_key",
                    text = "You find a key hidden inside the piano's compartment.",
                )
            }
            onOpLoc1("loc.mistmyst_door_emerald") {
                gemDoor(
                    door = it.loc,
                    leaf = EmeraldLeaf,
                    key = "obj.mistmyst_emerald_key",
                    unlockText = "You use the emerald key to unlock the door.",
                    unlockedAt = MisthalinStage.EmeraldRoomOpen,
                )
            }

            onOpLoc1("loc.mistmyst_door_diamond") { diamondDoor() }
            onOpLoc1("loc.mistmyst_clue_kitchen") { takeNote(Note3Obj) }
            onOpHeld1(Note3Obj) {
                readNote(Note3Lines, MisthalinStage.MandyDead, MisthalinStage.Note3Read)
            }
            onOpLocU("loc.mistmyst_fireplace", "obj.knife") { pryBrick() }
            onOpLoc1("loc.mistmyst_fireplace") { searchFireplace() }

            for (locked in LockedDoors) {
                onOpLoc1(locked) { lockedDoor() }
            }
        }

    private fun ProtectedAccess.takeTool(obj: String, seq: String, text: String) {
        if (invAdd(inv, obj, 1).failure) {
            return
        }
        anim(seq)
        soundSynth("synth.pick")
        mes(text)
    }

    private suspend fun ProtectedAccess.searchBarrel() {
        val stage = quest.getQuestStage(player)
        when {
            stage in MisthalinStage.Accepted..MisthalinStage.SeenShadyFigure -> {
                if (stage < MisthalinStage.SeenShadyFigure) {
                    quest.setQuestStage(this, MisthalinStage.SeenShadyFigure)
                }
                cutscenes.sid(this)
            }
            stage == MisthalinStage.SidDead -> startDialogue {
                chatPlayer(neutral, "The barrel is full of rainwater, I can't see what's in there.")
            }
            stage in MisthalinStage.BucketFilled..MisthalinStage.Complete ->
                searchContainer(
                    revealedAt = MisthalinStage.BucketFilled,
                    key = "obj.mistmyst_frontdoor_key",
                    text = "You find a key at the bottom of the barrel.",
                )
            else -> mes(constants.dm_default)
        }
    }

    private suspend fun ProtectedAccess.fillBucket(invSlot: Int) {
        val stage = quest.getQuestStage(player)
        if (stage in MisthalinStage.Accepted..MisthalinStage.SeenShadyFigure) {
            if (stage < MisthalinStage.SeenShadyFigure) {
                quest.setQuestStage(this, MisthalinStage.SeenShadyFigure)
            }
            cutscenes.sid(this)
        }

        if (quest.getQuestStage(player) != MisthalinStage.SidDead) {
            mes(constants.dm_default)
            return
        }

        anim("seq.human_pickuptable")

        if (invDel(inv, "obj.bucket_empty", 1, slot = invSlot).failure) {
            return
        }
        invAdd(inv, "obj.bucket_water", 1, slot = invSlot)

        quest.setQuestStage(this, MisthalinStage.BucketFilled)
        objbox("obj.bucket_water", "You fill the bucket from the barrel.")
    }

    private suspend fun ProtectedAccess.enterFront() {
        val stage = quest.getQuestStage(player)
        if (stage < MisthalinStage.InsideManor) {
            if (invTotal(inv, ManorKey) <= 0) {
                mes("The door is locked.")
                return
            }
            if (invDel(inv, ManorKey, 1).failure) {
                return
            }
            mes("You use the manor key to unlock the door.")
            soundSynth("synth.ds_big_door_unlock")
            quest.setQuestStage(this, MisthalinStage.InsideManor)
        }

        val nearest = FrontLeaves.minBy { abs(it.tile.x - player.coords.x) }
        val inside = player.coords.z > MisthalinCoords.FrontDoorZ
        val exitZ = if (inside) MisthalinCoords.FrontDoorZ - 1 else MisthalinCoords.FrontDoorZ + 1
        val route =
            listOf(
                CoordGrid(nearest.tile.x, MisthalinCoords.FrontDoorZ, 0),
                CoordGrid(nearest.tile.x, exitZ, 0),
            )
        val openTicks = misthalinCrossingOpenTicks(misthalinCrossingTiles(route))

        for (leaf in FrontLeaves) {
            openMisthalinLeaf(locRepo, leaf, openTicks)
        }
        misthalinDoorCreak()
        misthalinCrossDoorway(route)
    }

    private suspend fun ProtectedAccess.rubyDoor(door: BoundLocInfo) {
        val unlocked =
            gemDoor(
                door = door,
                leaf = RubyLeaf,
                key = "obj.mistmyst_ruby_key",
                unlockText = "You use the ruby key to unlock the door.",
                unlockedAt = MisthalinStage.RubyRoomOpen,
            )
        if (!unlocked) {
            return
        }

        val leftTheRoom = player.coords.x <= door.coords.x
        if (quest.getQuestStage(player) != MisthalinStage.FuseLit || !leftTheRoom) {
            return
        }

        delay(2)
        camShake(axis = CamShakeAxis.PAN_UP_DOWN, random = 4, amplitude = 0, rate = 1)
        delay(2)
        soundSynth("synth.explosion")
        quest.setQuestStage(this, MisthalinStage.WallBlown)
        delay(1)
        camShakeReset(axis = CamShakeAxis.PAN_UP_DOWN)
    }

    private suspend fun ProtectedAccess.gemDoor(
        door: BoundLocInfo,
        leaf: MisthalinDoorLeaf,
        key: String,
        unlockText: String,
        unlockedAt: Int,
    ): Boolean {
        if (quest.getQuestStage(player) < unlockedAt) {
            if (invTotal(inv, key) <= 0) {
                mesbox("The door is securely locked.")
                soundSynth("synth.locked")
                return false
            }
            if (invDel(inv, key, 1).failure) {
                return false
            }
            mes(unlockText)
            soundSynth("synth.unlock")
            quest.setQuestStage(this, unlockedAt)
        }

        val route = misthalinCrossingRoute(door)
        openMisthalinLeaf(locRepo, leaf, misthalinCrossingOpenTicks(misthalinCrossingTiles(route)))
        misthalinDoorCreak()
        misthalinCrossDoorway(route)
        return true
    }

    private suspend fun ProtectedAccess.lockedDoor() {
        mesbox("The door is securely locked.")
        soundSynth("synth.locked")
    }

    private suspend fun ProtectedAccess.taytenDoor() {
        if (quest.getQuestStage(player) == MisthalinStage.InsideManor) {
            cutscenes.tayten(this)
            return
        }
        lockedDoor()
    }

    private suspend fun ProtectedAccess.diamondDoor() {
        if (quest.getQuestStage(player) == MisthalinStage.EmeraldRoomOpen) {
            cutscenes.mandy(this)
            return
        }
        lockedDoor()
    }

    private suspend fun ProtectedAccess.observeTree() {
        if (quest.getQuestStage(player) == MisthalinStage.WallBlown) {
            cutscenes.lacey(this)
            return
        }
        mes(constants.dm_default)
    }

    private suspend fun ProtectedAccess.takeNote(obj: String) {
        if (invTotal(inv, obj) > 0) {
            mes(constants.dm_default)
            return
        }
        if (invAdd(inv, obj, 1).failure) {
            return
        }
        objbox(obj, "You take a copy of the note.")
    }

    private fun ProtectedAccess.readNote(lines: List<String>, readableAt: Int, readStage: Int) {
        ifOpenMainModal("interface.letterscroll")
        for (index in 0 until LetterScrollLines) {
            ifSetText("component.letterscroll:lj${index + 1}", lines.getOrElse(index) { "" })
        }
        if (quest.getQuestStage(player) == readableAt) {
            quest.setQuestStage(this, readStage)
        }
    }

    private suspend fun ProtectedAccess.slashPainting(painting: BoundLocInfo) {
        if (quest.getQuestStage(player) != MisthalinStage.Note1Read) {
            mes(constants.dm_default)
            return
        }
        if (player.hasMovedThisCycle) delay(1)
        // The painting occupies the player's tile, so face its mounted wall.
        faceSquare(when (painting.angle) {
            LocAngle.West -> painting.coords.translateX(-1)
            LocAngle.North -> painting.coords.translateZ(1)
            LocAngle.East -> painting.coords.translateX(1)
            LocAngle.South -> painting.coords.translateZ(-1)
        })
        delay(2)
        anim("seq.human_knife_chop")
        quest.setQuestStage(this, MisthalinStage.PaintingSlashed)
        mesbox("You slash open the painting, revealing a hidden storage compartment.")
    }

    private suspend fun ProtectedAccess.lightCandle(index: Int) {
        val varbit = MisthalinCandleVarbits[index]
        val alreadyLit = vars[varbit] != 0
        if (!alreadyLit) {
            vars[varbit] = 1
        }

        val allLit = mistmystCandles == AllCandlesLit
        if (allLit && quest.getQuestStage(player) == MisthalinStage.RubyRoomOpen) {
            quest.setQuestStage(this, MisthalinStage.CandlesLit)
            mesbox(
                "You light the candle. The room is sufficiently warmed, the fuse should now have " +
                    "dried out."
            )
            return
        }
        if (alreadyLit) {
            mes(constants.dm_default)
            return
        }
        mesbox("You light the candle.")
    }

    private suspend fun ProtectedAccess.lightFuse() {
        if (quest.getQuestStage(player) != MisthalinStage.CandlesLit) {
            mes(constants.dm_default)
            return
        }
        mes("You attempt to light the fuse...")
        anim("seq.human_createfire")

        delay(3)
        resetAnim()
        startDialogue {
            chatPlayer(neutral, "The fuse is lit, I should get out of here before it blows!")
        }
        quest.setQuestStage(this, MisthalinStage.FuseLit)
    }

    private suspend fun ProtectedAccess.climbBrokenWall() {
        if (quest.getQuestStage(player) < MisthalinStage.WallBlown) {
            mes(constants.dm_default)
            return
        }

        val goingEast = player.coords.x <= MisthalinCoords.BrokenWallWest.x
        val start =
            if (goingEast) MisthalinCoords.BrokenWallWest else MisthalinCoords.BrokenWallEast
        val end = if (goingEast) MisthalinCoords.BrokenWallEast else MisthalinCoords.BrokenWallWest
        val facing = if (goingEast) constants.em_face_east else constants.em_face_west

        anim("seq.human_walk_crumbledwall", delay = WallClimbDelay)
        soundSynth("synth.climb_wall", delay = WallClimbDelay)
        exactMove(start, end, delay1 = WallClimbDelay, delay2 = WallClimbArrival, dir = facing)
        delay(2)
    }

    private suspend fun ProtectedAccess.pryBrick() {
        if (quest.getQuestStage(player) != MisthalinStage.Note3Read) {
            mes(constants.dm_default)
            return
        }
        mesbox(
            "You use your knife to pry open a loose brick in the fireplace, revealing a secret " +
                "panel."
        )
        quest.setQuestStage(this, MisthalinStage.PanelRevealed)
    }

    private suspend fun ProtectedAccess.searchFireplace() {
        when {
            quest.getQuestStage(player) == MisthalinStage.PanelRevealed -> {
                mesbox(
                    "You find a panel of switches inside the hidden compartment. Each switch is " +
                        "crafted from a different gemstone."
                )
                puzzles.openGemPanel(this)
            }
            quest.getQuestStage(player) >= MisthalinStage.GemPanelSolved ->
                searchContainer(
                    revealedAt = MisthalinStage.GemPanelSolved,
                    key = "obj.mistmyst_sapphire_key",
                    text = "You find a key hidden inside the fireplace.",
                )
            else -> mes(constants.dm_default)
        }
    }

    private suspend fun ProtectedAccess.searchContainer(
        revealedAt: Int,
        key: String,
        text: String,
    ) {
        if (quest.getQuestStage(player) < revealedAt) {
            mes(constants.dm_default)
            return
        }
        if (invTotal(inv, key) > 0) {
            mes(constants.dm_default)
            return
        }
        if (invAdd(inv, key, 1).failure) {
            return
        }
        anim(PickupTable)
        soundSynth("synth.pick")
        objbox(key, text)
    }

    private companion object {
        private const val ManorKey = "obj.mistmyst_frontdoor_key"

        private const val PickupFloor = "seq.human_pickupfloor"
        private const val PickupTable = "seq.human_pickuptable"

        private const val Note1Obj = "obj.mistmyst_clue_library"

        private const val Note2Obj = "obj.mistmyst_clue_outside"
        private const val Note3Obj = "obj.mistmyst_clue_kitchen"

        private const val AllCandlesLit = 15

        private const val LetterScrollLines = 8

        private const val WallClimbDelay = 30
        private const val WallClimbArrival = 100

        private val LockedDoors =
            listOf(
                "loc.mistmyst_door_opal",
                "loc.mistmyst_door_jade",
                "loc.mistmyst_door_dragonstone",
            )

        private val CandleLocs =
            listOf(
                "loc.mistmyst_candle1",
                "loc.mistmyst_candle2",
                "loc.mistmyst_candle3",
                "loc.mistmyst_candle4",
            )

        private val Note1Lines =
            listOf(
                "Isn't murder just a work of art?",
                "Beautiful, yet haunting, like the blade of a knife.",
                "As we wander through the valley of death.",
            )

        private val Note2Lines =
            listOf("It's like music to my ears!", "The glorious sounds, spelling out your fate!")

        private val Note3Lines =
            listOf(
                "Hear at first these words.",
                "Each murder you witness helplessly from start to end.",
                "As you fail to solve the final letters of this quiz.",
                "Razor sharp like a gemstone is the blade of my knife.",
                "The last sound you hear will be your scream's echo.",
                "Heed that I will have the final word in this thriller.",
            )

        private val FrontLeaves =
            listOf(
                MisthalinDoorLeaf(
                    tile = CoordGrid(1636, MisthalinCoords.FrontDoorZ, 0),
                    closed = "loc.mistmyst_front_doorl",
                    openLeaf = "loc.haunteddoorl_inactive",
                    openAngle = LocAngle.West,
                    closedAngle = LocAngle.North,
                ),
                MisthalinDoorLeaf(
                    tile = CoordGrid(1637, MisthalinCoords.FrontDoorZ, 0),
                    closed = "loc.mistmyst_front_doorr",
                    openLeaf = "loc.haunteddoorr_inactive",
                    openAngle = LocAngle.East,
                    closedAngle = LocAngle.North,
                ),
            )

        private val RubyLeaf =
            MisthalinDoorLeaf(
                tile = CoordGrid(1640, 4828, 0),
                closed = "loc.mistmyst_door_ruby",
                openLeaf = "loc.mistmyst_door_ruby_inactive",
                openAngle = LocAngle.South,
                closedAngle = LocAngle.East,
            )

        private val EmeraldLeaf =
            MisthalinDoorLeaf(
                tile = CoordGrid(1633, 4837, 0),
                closed = "loc.mistmyst_door_emerald",
                openLeaf = "loc.mistmyst_door_emerald_inactive",
                openAngle = LocAngle.North,
                closedAngle = LocAngle.West,
            )
    }
}
