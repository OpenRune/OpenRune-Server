package org.rsmod.content.areas.misc.dwarvenmine.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DwarvenMineLocScript
@Inject
constructor(private val locRepo: LocRepository, private val worldRepo: WorldRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.stairs_falador") { useStairs(FALADOR_HOUSE) }
        onOpLoc1("loc.stairs_cellar") {
            if (it.loc.coords == FALADOR_CELLAR_STAIRS) {
                useStairs(MINE_STAIRS_LANDING)
            }
        }

        onOpLoc1("loc.dwarf_mines_sc_wall_crack") { squeezeThroughCrevice(it.loc) }

        onOpLoc1("loc.dragon_slayer_qip_magic_door") { openMagicDoor(it.loc) }

        onOpLoc1("loc.oraclechestshut") { openOracleChest(it.loc) }
        onOpLoc1("loc.oraclechestopen") { mes("You search the chest but find nothing.") }
        onOpLoc2("loc.oraclechestopen") { locRepo.change(it.loc, "loc.oraclechestshut", CHEST_OPEN_CYCLES) }
    }

    private suspend fun ProtectedAccess.useStairs(dest: CoordGrid) {
        arriveDelay()
        delay(1)
        telejump(dest)
    }

    /** Three forced steps through the wall: into the crack, along the tunnel, and back out. */
    private suspend fun ProtectedAccess.squeezeThroughCrevice(crevice: BoundLocInfo) {
        arriveDelay()
        if (player.agilityLvl < CREVICE_AGILITY) {
            mes("You need an Agility level of $CREVICE_AGILITY to negotiate this obstacle.")
            return
        }
        val westbound = crevice.coords == CREVICE_EAST
        val step = if (westbound) -1 else 1
        val facing = if (westbound) FACE_WEST else FACE_EAST
        val tunnelEnd = if (westbound) CREVICE_WEST else CREVICE_EAST

        faceSquare(crevice.coords)
        exactMove(coords, crevice.coords, delay1 = 0, delay2 = STEP_CLIENT_CYCLES, dir = facing)
        anim("seq.agility_shortcut_crack_enter")
        soundSynth(CREVICE_ENTER_SOUND)
        delay(1)

        exactMove(coords, tunnelEnd, delay1 = 0, delay2 = STEP_CLIENT_CYCLES, dir = facing)
        anim("seq.agilty_shortcut_tunnel_walk")
        delay(1)

        exactMove(coords, tunnelEnd.translateX(step), delay1 = 0, delay2 = STEP_CLIENT_CYCLES, dir = facing)
        anim("seq.agility_shortcut_crack_leave")
        soundSynth(CREVICE_LEAVE_SOUND)
        spam("You climb your way through the narrow crevice.")
    }

    private suspend fun ProtectedAccess.openMagicDoor(door: BoundLocInfo) {
        arriveDelay()
        if (!QuestRequirements.hasCompleted(player, DRAGON_SLAYER)) {
            mes("There is something in this door for me to put objects in.")
            return
        }
        val fromWest = coords.x < door.coords.x
        val centre = door.coords.translateZ(1)
        val approach = centre.translateX(if (fromWest) -1 else 1)
        val exit = centre.translateX(if (fromWest) 1 else -1)

        if (coords != approach) {
            playerWalk(approach)
            delay(1)
        }
        mes("The door opens...")
        worldRepo.locAnim(door, "seq.dragon_slayer_qip_magic_door_open")
        delay(MAGIC_DOOR_OPEN_CYCLES)

        val closedDoor = LocInfo(door.layer, door.coords, door.entity)
        val leftPanel = spawn(door.coords.translateZ(2), "loc.dragon_slayer_qip_magic_door_left", LocAngle.North, LocShape.CentrepieceStraight)
        val middlePanel = spawn(centre, "loc.dragon_slayer_qip_magic_door2", LocAngle.South, LocShape.WallStraight)
        val eastWall = spawn(centre.translateX(1), INVISIBLE_WALL, LocAngle.West, LocShape.WallL)
        val westWall = spawn(centre.translateX(-1), INVISIBLE_WALL, LocAngle.East, LocShape.WallL)
        locRepo.del(closedDoor, Int.MAX_VALUE)
        spawn(door.coords, "loc.dragon_slayer_qip_magic_door_right", LocAngle.North, LocShape.CentrepieceStraight)
        teleport(exit)
        delay(PANEL_CYCLES)

        locRepo.del(leftPanel, Int.MAX_VALUE)
        locRepo.del(middlePanel, Int.MAX_VALUE)
        spawn(door.coords, "loc.dragon_slayer_qip_magic_door_open", LocAngle.North, LocShape.CentrepieceStraight)
        delay(1)

        locRepo.del(eastWall, Int.MAX_VALUE)
        locRepo.del(westWall, Int.MAX_VALUE)
        delay(1)

        locRepo.add(closedDoor, Int.MAX_VALUE)
    }

    private fun spawn(coords: CoordGrid, loc: String, angle: LocAngle, shape: LocShape): LocInfo =
        locRepo.add(coords, loc, Int.MAX_VALUE, angle, shape)

    private suspend fun ProtectedAccess.openOracleChest(chest: BoundLocInfo) {
        arriveDelay()
        mesbox("As you open the chest, you notice an inscription on the lid:")
        mesbox(
            "Here I rest the map to my beloved home. To whoever finds it, I beg of you, let it be. I was " +
                "honour-bound not to destroy the map piece, but I have used all my magical skill to keep it " +
                "from being recovered.",
        )
        mesbox(
            "This map leads to the lair of the beast that destroyed my home, devoured my family, and burned " +
                "to a cinder all that I love. But revenge would not benefit me now, and to disturb this beast " +
                "is to risk bringing its wrath down upon another land.",
        )
        mesbox(
            "I cannot stop you from taking this map piece now, but think on this: if you can slay the Dragon " +
                "of Crandor, you are a greater hero than my land ever produced. There is no shame in backing " +
                "out now.",
        )
        anim("seq.human_openchest")
        soundSynth(CHEST_OPEN_SOUND)
        locRepo.change(chest, "loc.oraclechestopen", CHEST_OPEN_CYCLES)
    }

    private companion object {
        val FALADOR_HOUSE = CoordGrid(3061, 3377, 0)
        val MINE_STAIRS_LANDING = CoordGrid(3058, 9776, 0)
        val FALADOR_CELLAR_STAIRS = CoordGrid(3058, 3376, 0)

        val CREVICE_EAST = CoordGrid(3034, 9806, 0)
        val CREVICE_WEST = CoordGrid(3029, 9806, 0)
        const val CREVICE_AGILITY = 42
        const val CREVICE_ENTER_SOUND = 2489
        const val CREVICE_LEAVE_SOUND = 2490
        const val STEP_CLIENT_CYCLES = 30
        const val FACE_WEST = 512
        const val FACE_EAST = 1536

        const val DRAGON_SLAYER = "quest_dragonslayer1"
        const val MAGIC_DOOR_OPEN_CYCLES = 6
        const val PANEL_CYCLES = 3
        const val INVISIBLE_WALL = "loc.inviswall"

        const val CHEST_OPEN_SOUND = 52
        const val CHEST_OPEN_CYCLES = 500
    }
}
