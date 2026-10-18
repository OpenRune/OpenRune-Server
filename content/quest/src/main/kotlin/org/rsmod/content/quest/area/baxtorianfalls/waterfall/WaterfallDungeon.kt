package org.rsmod.content.quest.area.baxtorianfalls.waterfall

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.CamShakeAxis
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.table.WaterfallPillarRunesRow
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.AMULET
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.BAXTORIAN_KEY
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.FLOOR_RISEN
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.FOUND_CHALICE
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.PILLAR_COUNT
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.RUNES_PER_PILLAR
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.URN_EMPTY
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.URN_FULL
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class WaterfallDungeon
@Inject
constructor(
    private val waterfall: WaterfallQuest,
    private val objRepo: ObjRepository,
    private val worldRepo: WorldRepository,
    private val locRepo: LocRepository,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1(EXIT_DOOR) { exitFalls() }
        onOpLoc1(CRATE) { searchCrate() }
        onOpLoc1(TOMB_DOOR) { openTombDoor(it.loc) }
        for (pillar in PILLAR_FORMS) {
            for (rune in PILLAR_RUNES.keys) {
                onOpLocU(pillar, rune) { placeRune(it.loc, rune) }
            }
        }
        onOpLocU(STATUE_GLARIAL, AMULET) { placeAmulet() }
        onOpLoc1(CHALICE) { takeTreasure() }
        onOpLoc1(CHALICE_ASH) { takeTreasure() }
        onOpLocU(CHALICE, URN_FULL) { pourAshes() }
    }

    private suspend fun ProtectedAccess.exitFalls() {
        soundSynth(DOOR_SOUND)
        telejump(WaterfallCoords.LEDGE, TeleportType.Exempt)
        mesbox("You exit the dungeon.")
    }

    private suspend fun ProtectedAccess.searchCrate() {
        if (BAXTORIAN_KEY in player.inv) {
            mes("You search the crate but find nothing of interest.")
            return
        }
        if (invAdd(inv, BAXTORIAN_KEY).failure) {
            objbox(
                BAXTORIAN_KEY,
                "You find a key in the crate but you don't have enough room to take it.",
            )
            return
        }
        objbox(BAXTORIAN_KEY, "You find a key in the crate.")
    }

    private suspend fun ProtectedAccess.openTombDoor(door: BoundLocInfo) {
        if (door.playerIsSouth(player.coords)) {
            if (BAXTORIAN_KEY !in player.inv) {
                soundSynth(LOCKED_SOUND)
                mes("The door is locked.")
                return
            }
            arriveDelay()
            if (door.coords == CHALICE_ROOM_DOOR) {
                enterChaliceRoom(door)
                return
            }
            mes("You use the key to unlock the door.")
            passDoor(locRepo, door, TOMB_DOOR_OPEN)
            return
        }
        if (door.coords.inRaisedCopy()) {
            soundSynth(DOOR_SOUND)
            arriveDelay()
            telejump(door.coords.toRealRoom(), TeleportType.Exempt)
            return
        }
        passDoor(locRepo, door, TOMB_DOOR_OPEN)
    }

    private suspend fun ProtectedAccess.enterChaliceRoom(door: BoundLocInfo) {
        if (waterfall.stage(player) < FLOOR_RISEN) {
            waterfall.advanceTo(this, FOUND_CHALICE)
            passDoor(locRepo, door, TOMB_DOOR_OPEN)
            return
        }
        fadeOut()
        delay(1)
        passDoor(locRepo, door, TOMB_DOOR_OPEN)
        delay(1)
        telejump(player.coords.toRaisedCopy(), TeleportType.Exempt)
        delay(1)
        fadeIn()
        delay(FADE_CLOSE_TICKS)
        closeFadeOverlayNow()
    }

    private suspend fun ProtectedAccess.placeRune(pillar: BoundLocInfo, rune: String) {
        if (pillar.coords.inRaisedCopy()) {
            mes(constants.dm_default, ChatType.Engine)
            return
        }
        val runeIndex = PILLAR_RUNES.getValue(rune)
        val pillarIndex = pillarIndex(pillar.coords) ?: return
        val bit = 1 shl (pillarIndex * RUNES_PER_PILLAR + runeIndex)
        val placed = player.pillarRunes
        if ((placed and bit) != 0) {
            mes("You've already put that type of rune on this pillar.")
            return
        }
        if (invDel(inv, rune).failure) {
            return
        }
        soundSynth(PLACE_SOUND)
        spotanimMap(worldRepo, SMOKE_SPOTANIM, pillar.coords, height = SMOKE_HEIGHT)
        player.pillarRunes = placed or bit
        objbox(rune, "You place the rune on the pillar. It disappears in a puff of smoke.")
    }

    private suspend fun ProtectedAccess.placeAmulet() {
        if (player.coords.inRaisedCopy()) {
            mes("Glarial's statue already wears her amulet.")
            return
        }
        if (!waterfall.allRunesPlaced(player)) {
            if (invDel(inv, AMULET).failure) {
                return
            }
            mesbox(
                "You go to place the amulet around the neck of the statue. However, water " +
                    "floods into the room as you do..."
            )
            mesbox("...you are washed out of the cave and down the river.")
            washDownstream()
            return
        }
        if (AMULET !in player.inv) {
            return
        }
        arriveDelay()
        mesboxNp(FLOOR_RISES)
        fadeOut()
        for (axis in SHAKE_AXES) {
            camShake(axis, SHAKE_RANDOM, 0, 0)
        }
        soundSynth(RUMBLE_SOUND)
        delay(CARRIED_TICKS)
        if (invDel(inv, AMULET).failure) {
            camReset()
            closeFadeOverlayNow()
            return
        }
        telejump(player.coords.toRaisedCopy(), TeleportType.Exempt)
        waterfall.advanceTo(this, FLOOR_RISEN)
        camReset()
        delay(1)
        fadeIn()
        mesbox(FLOOR_RISES)
        closeFadeOverlayNow()
    }

    private suspend fun ProtectedAccess.takeTreasure() {
        if (waterfall.isComplete(player)) {
            mesbox("The chalice only contains some old ashes.")
            return
        }
        mesboxNp(
            "You go to take the treasure from the chalice. However, water floods into the " +
                "room as you do..."
        )
        fadeOut()
        delay(CARRIED_TICKS)
        telejump(WaterfallCoords.WHIRLPOOL, TeleportType.Exempt)
        setBas(WHIRLPOOL_BAS)
        try {
            fadeIn()
            delay(WHIRLPOOL_TICKS)
            mesboxNp(WASHED_OUT)
            fadeOut()
            delay(CARRIED_TICKS)
            telejump(WaterfallCoords.DOWNSTREAM, TeleportType.Exempt)
        } finally {
            setBas(null)
        }
        delay(1)
        fadeIn()
        mesbox(WASHED_OUT)
        closeFadeOverlayNow()
    }

    private fun ProtectedAccess.canCarryTreasure(): Boolean =
        player
            .invTransaction(inv, autoCommit = false) {
                val target = select(inv)
                delete(target, URN_FULL.asRSCM(RSCMType.OBJ), count = 1)
                add(target, URN_EMPTY.asRSCM(RSCMType.OBJ), count = 1)
                for ((item, amount) in waterfall.quest.rewards.items) {
                    add(target, item.asRSCM(RSCMType.OBJ), count = amount)
                }
            }
            .success

    private suspend fun ProtectedAccess.pourAshes() {
        if (waterfall.isComplete(player)) {
            mesbox("The chalice only contains some old ashes.")
            return
        }
        if (waterfall.stage(player) < FLOOR_RISEN) {
            mes("You can't reach the chalice from here.")
            return
        }
        if (URN_FULL !in player.inv) {
            return
        }
        if (!canCarryTreasure()) {
            mesbox("You do not have enough space to do that. You will need five free slots.")
            return
        }
        mesbox("You carefully pour the ashes into the chalice and remove Baxtorian's treasure...")
        if (!canCarryTreasure() || invDel(inv, URN_FULL).failure) {
            return
        }
        invAddOrDrop(objRepo, URN_EMPTY)
        waterfall.quest.completeQuest(this)
    }

    private companion object {
        const val EXIT_DOOR = "loc.baxtorian_door_waterfall_quest"
        const val CRATE = "loc.baxtorian_crate_waterfall_quest"
        const val TOMB_DOOR = "loc.baxtorian_door_2_waterfall_quest"
        const val TOMB_DOOR_OPEN = "loc.baxtorian_door_2_open_waterfall_quest"

        val PILLAR_FORMS =
            listOf(
                "loc.stonepillar_small_waterfall_quest",
                "loc.stonepillar_small_waterfall_quest_noop",
                "loc.stonepillar_small_waterfall_quest_op",
            )
        const val STATUE_GLARIAL = "loc.statue_queen_waterfall_quest"
        const val CHALICE = "loc.baxtorian_chalice_waterfall_quest"
        const val CHALICE_ASH = "loc.baxtorian_chalice_waterfall_quest_ash"

        val PILLAR_RUNES: Map<String, Int> by lazy {
            WaterfallPillarRunesRow.all().associate { it.rune.internalName to it.bit }
        }

        const val PILLAR_WEST_X = 2562
        const val PILLAR_EAST_X = 2569
        const val PILLAR_SOUTH_Z = 9910
        const val PILLAR_ROW_GAP = 2

        const val COPY_OFFSET_X = 38
        const val COPY_OFFSET_Z = -1
        const val COPY_MIN_X = 2590

        val CHALICE_ROOM_DOOR = CoordGrid(2566, 9901, 0)
        val SHAKE_AXES =
            listOf(CamShakeAxis.LEFT_RIGHT, CamShakeAxis.UP_DOWN, CamShakeAxis.FORWARDS_BACKWARDS)
        const val SHAKE_RANDOM = 6
        const val CARRIED_TICKS = 2
        const val WHIRLPOOL_TICKS = 9
        const val FADE_CLOSE_TICKS = 2
        const val WHIRLPOOL_BAS = "bas.waterfall_whirlpool"

        const val FLOOR_RISES =
            "You place the amulet around the neck of the statue. You hear a loud rumble from " +
                "beneath as the floor rises."
        const val WASHED_OUT = "...you are washed out of the cave and down the river."

        const val SMOKE_SPOTANIM = "spotanim.smokepuff"
        const val SMOKE_HEIGHT = 160
        const val DOOR_SOUND = "synth.door_open"
        const val LOCKED_SOUND = "synth.irondoor_locked"
        const val PLACE_SOUND = "synth.smokepuff"
        const val RUMBLE_SOUND = "synth.leviathan_rockfall_rumble"

        fun CoordGrid.inRaisedCopy(): Boolean = x >= COPY_MIN_X

        fun CoordGrid.toRaisedCopy(): CoordGrid = translate(COPY_OFFSET_X, COPY_OFFSET_Z)

        fun CoordGrid.toRealRoom(): CoordGrid = translate(-COPY_OFFSET_X, -COPY_OFFSET_Z)

        fun pillarIndex(coords: CoordGrid): Int? {
            val real = if (coords.inRaisedCopy()) coords.toRealRoom() else coords
            val column =
                when (real.x) {
                    PILLAR_WEST_X -> 0
                    PILLAR_EAST_X -> 1
                    else -> return null
                }
            val offset = real.z - PILLAR_SOUTH_Z
            val row = offset / PILLAR_ROW_GAP
            if (row !in 0 until PILLAR_COUNT / 2 || offset % PILLAR_ROW_GAP != 0) {
                return null
            }
            return column * (PILLAR_COUNT / 2) + row
        }
    }
}
