package org.rsmod.content.quest.area.varrock.dragonslayer

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.forcedWalk
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onNpcHit
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.CRANDOR_MAP
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.MAZE_KEY
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.MELZAR_PIECE
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.ownsAny
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Melzar's Maze: the key doors, Melzar's chest and the maze's scenery. */
class DragonSlayerMaze
@Inject
constructor(
    private val quest: DragonSlayerQuest,
    private val locRepo: LocRepository,
    private val gameRandom: GameRandom,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1(FRONT_DOOR) { frontDoor(it.loc) }
        for ((door, key) in KEY_DOORS) {
            onOpLoc1(door) { keyDoor(it.loc, key) }
        }
        onOpLoc1(CHEST_SHUT) { openChest(it.loc) }
        onOpLoc1(CHEST_OPEN) { searchChest(it.loc) }
        onOpLoc2(CHEST_OPEN) { closeChest(it.loc) }
        onOpLoc1(EXIT_TRAPDOOR) {
            arriveDelay()
            mes("That trapdoor can only be opened from below.")
        }
        onOpLoc1(CELLAR_LADDER) {
            arriveDelay()
            climbOutOfCellar()
        }
        onOpLoc1(WARDROBE) { searchWardrobe() }
        onOpLoc1(BOOKSHELVES) { searchBookshelves() }

        val melzar = ServerCacheManager.getNpc(MELZAR.asRSCM(RSCMType.NPC))!!
        onNpcHit(melzar) {
            if (gameRandom.of(MELZAR_SHOUT_CHANCE) == 0) {
                npc.say(MELZAR_SHOUTS[gameRandom.of(MELZAR_SHOUTS.size)])
            }
        }
    }

    private suspend fun ProtectedAccess.climbOutOfCellar() {
        anim(CLIMB_UP_SEQ)
        delay(CLIMB_TICKS)
        telejump(CELLAR_EXIT)
    }

    private suspend fun ProtectedAccess.frontDoor(door: BoundLocInfo) {
        arriveDelay()
        val leaving = coords.x < door.coords.x
        if (!leaving && MAZE_KEY !in inv) {
            mes("This door is securely locked.")
            soundSynth(LOCKED_SOUND)
            return
        }
        passDoor(door)
    }

    private suspend fun ProtectedAccess.keyDoor(door: BoundLocInfo, key: String) {
        arriveDelay()
        if (key !in inv) {
            mes("This door is securely locked.")
            soundSynth(LOCKED_SOUND)
            return
        }
        if (invDel(inv, key, 1).failure) {
            return
        }
        mes("The key disintegrates as it unlocks the door.")
        passDoor(door)
    }

    /** Pass-through door: the leaf is swapped for an invisible wall while the player steps across. */
    private suspend fun ProtectedAccess.passDoor(door: BoundLocInfo) {
        val across = otherSide(door, coords)
        soundSynth(DOOR_SOUND)
        locRepo.change(door, INVISIBLE_WALL, DOOR_TICKS)
        forcedWalk(across, 1)
    }

    private fun otherSide(door: BoundLocInfo, from: CoordGrid): CoordGrid {
        val tile = door.coords
        val (near, far) =
            when (door.angle) {
                LocAngle.West -> tile.translateX(-1) to tile
                LocAngle.North -> tile to tile.translateZ(1)
                LocAngle.East -> tile to tile.translateX(1)
                LocAngle.South -> tile.translateZ(-1) to tile
            }
        val onNearSide =
            when (door.angle) {
                LocAngle.West,
                LocAngle.East -> from.x <= near.x
                LocAngle.North,
                LocAngle.South -> from.z <= near.z
            }
        return if (onNearSide) far else near
    }

    private suspend fun ProtectedAccess.openChest(chest: BoundLocInfo) {
        arriveDelay()
        anim(OPEN_CHEST_SEQ)
        soundSynth(CHEST_OPEN_SOUND)
        delay(1)
        locRepo.change(chest, CHEST_OPEN, CHEST_TICKS)
    }

    private suspend fun ProtectedAccess.searchChest(chest: BoundLocInfo) {
        arriveDelay()
        if (!quest.isCollectingMap(player) || player.ownsAny(MELZAR_PIECE, CRANDOR_MAP)) {
            mes("You find nothing in the chest.")
            return
        }
        if (invAdd(inv, MELZAR_PIECE, 1).failure) {
            mes("You don't have enough inventory space.")
            return
        }
        startDialogue { objbox(MELZAR_PIECE, "You find a map piece in the chest.") }
    }

    private suspend fun ProtectedAccess.closeChest(chest: BoundLocInfo) {
        arriveDelay()
        mes("You shut the chest.")
        locRepo.change(chest, CHEST_SHUT, CHEST_TICKS)
        soundSynth(CHEST_CLOSE_SOUND)
    }

    private suspend fun ProtectedAccess.searchWardrobe() {
        arriveDelay()
        val text =
            when (coords.level) {
                0 -> "The wardrobe is full of heaps of dead rats!"
                1 ->
                    "The wardrobe is full of scorched and broken objects: clothes, tools, and " +
                        "children's toys. Each is labelled with a scrawled number. You don't see " +
                        "anything of value or use."
                else ->
                    "The wardrobe is full of heaps of bones and incomplete skeletons, each one " +
                        "labelled with a number."
            }
        startDialogue { mesbox(text) }
    }

    private suspend fun ProtectedAccess.searchBookshelves() {
        arriveDelay()
        val note = MELZARS_NOTES[random.of(MELZARS_NOTES.size)]
        startDialogue {
            mesbox("These books are full of scrawled notes. You open one at a random page...")
            mesbox(note)
        }
    }

    private companion object {
        const val FRONT_DOOR = "loc.melzardoor"
        const val CHEST_SHUT = "loc.funchestshut"
        const val CHEST_OPEN = "loc.funchestopen"
        const val EXIT_TRAPDOOR = "loc.dragon_slayer_qip_trapdoor_closed"
        const val CELLAR_LADDER = "loc.dragon_slayer_ladder_from_cellar"
        const val WARDROBE = "loc.dragonslayer_spookywardrobe"
        const val BOOKSHELVES = "loc.dragon_slayer_qip_spookybookshelves"
        const val MELZAR = "npc.melzar_the_mad"

        val KEY_DOORS =
            listOf(
                "loc.reddoor" to "obj.redkey",
                "loc.orangedoor" to "obj.orangekey",
                "loc.yellowdoor" to "obj.yellowkey",
                "loc.bluedoor" to "obj.bluekey",
                "loc.magentadoor" to "obj.magentakey",
                "loc.greendoor" to "obj.greenkey",
            )

        val CELLAR_EXIT = CoordGrid(2929, 3258, 0)

        const val INVISIBLE_WALL = "loc.inviswall"
        const val DOOR_TICKS = 3
        const val DOOR_SOUND = "synth.nicedoor_open"
        const val LOCKED_SOUND = "synth.locked"
        const val CHEST_OPEN_SOUND = "synth.chest_open"
        const val CHEST_CLOSE_SOUND = "synth.chest_close"
        const val CHEST_TICKS = 500
        const val OPEN_CHEST_SEQ = "seq.human_openchest"
        const val CLIMB_UP_SEQ = "seq.human_reachforladder"
        const val CLIMB_TICKS = 2

        const val MELZAR_SHOUT_CHANCE = 4
        val MELZAR_SHOUTS =
            listOf(
                "By the power of custard!",
                "Feel the wrath of my feet!",
                "Let me drink my tea in peace.",
                "Leave me alone, I need to feed my pet rock.",
            )

        val MELZARS_NOTES =
            listOf(
                "...My ritual was a success! I have managed to subdue a lesser demon. I can channel " +
                    "its magic to master the forces of life and death - as long as its chains hold...",
                "...On my fortieth attempt, the process was partially successful. The rat's flesh " +
                    "continues to decay, but it regains the faculty for movement that it had in " +
                    "life. It is too early to say how many of its mental faculties have survived...",
                "...I have searched through the ruins of the refugee camp and found many personal " +
                    "items. I can use these to conjure the spirits of my deceased countrymen...",
                "...I have nearly perfected the formula: I can now conjure ghosts that persist for " +
                    "several hours. I am sure I can soon make them permanent...",
                "...The ghosts are still screaming. They seem to be in pain, and want to be returned " +
                    "to true death. But when I have perfected my techniques they will thank me...",
                "...I searched the ruins of the camp again and have assembled a number of complete " +
                    "skeletons. As the flesh is all gone it will be more difficult to animate them " +
                    "than the rats...",
                "...Note to self: must stop thinking about cabbages...",
                "...Have made flesh grow on bones like cabbage from seed. All power come from " +
                    "cabbage. Zombies not useful...",
                "...Cabbage cabbage cabcab cabbabbabbage egabac CABBAGE cbaaeg CCCC...",
                "...hate dragons hate dragons hate dragons...",
            )
    }
}
