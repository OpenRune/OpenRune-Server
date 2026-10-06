package org.rsmod.content.quest.area.baxtorianfalls.waterfall

import jakarta.inject.Inject
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.content.quest.area.ardougne.QuestDoors
import org.rsmod.content.quest.area.ardougne.fadeFromBlack
import org.rsmod.content.quest.area.ardougne.fadeToBlack
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.Amulet
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.BaxtorianKey
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.FloorRisen
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.PillarCount
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.RunesPerPillar
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.RunesPlaced
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.UrnEmpty
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.UrnFull
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The caves inside Baxtorian Falls.
 *
 * Baxtorian's tomb is mapped twice. The room the player walks into (2561..2570 x 9902..9917) has
 * the six pillars, the statues of Baxtorian and Glarial, and the chalice floating out of reach on
 * level 1. An identical copy 38 tiles east and one tile south has the chalice on the floor.
 * Placing the amulet "raises the floor" by moving the player into the copy, and leaving the copy
 * through its doors puts them back at the matching spot in the real corridor.
 */
class WaterfallDungeon
@Inject
constructor(
    private val waterfall: WaterfallQuest,
    private val objRepo: ObjRepository,
    private val worldRepo: WorldRepository,
    private val doors: QuestDoors,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1(ExitDoor) { exitFalls() }
        onOpLoc1(Crate) { searchCrate() }
        onOpLoc1(TombDoor) { openTombDoor(it.loc) }
        for (pillar in PillarForms) {
            for (rune in PillarRunes.keys) {
                onOpLocU(pillar, rune) { placeRune(it.loc, rune) }
            }
        }
        onOpLocU(StatueGlarial, Amulet) { placeAmulet() }
        onOpLoc1(Chalice) { takeTreasure() }
        onOpLoc1(ChaliceAsh) { takeTreasure() }
        onOpLocU(Chalice, UrnFull) { pourAshes() }
    }

    private suspend fun ProtectedAccess.exitFalls() {
        soundSynth(DoorSound)
        mes("You leave the waterfall.")
        telejump(WaterfallCoords.LEDGE, TeleportType.Exempt)
    }

    private suspend fun ProtectedAccess.searchCrate() {
        anim(SearchSeq)
        delay(1)
        if (BaxtorianKey in player.inv) {
            mes("You search the crate but find nothing of interest.")
            return
        }
        invAddOrDrop(objRepo, BaxtorianKey)
        objbox(BaxtorianKey, "You find a key in the crate.")
    }

    /** Both tomb doors sit on the north edge of their tile and are locked to the south. */
    private suspend fun ProtectedAccess.openTombDoor(door: BoundLocInfo) {
        if (door.playerIsSouth(player.coords)) {
            if (BaxtorianKey !in player.inv) {
                soundSynth(LockedSound)
                mes("The door is locked.")
                return
            }
            mes("You use the key to unlock the door.")
            doors.open(this, door, TombDoorOpen)
            return
        }
        if (door.coords.inRaisedCopy()) {
            soundSynth(DoorSound)
            arriveDelay()
            telejump(door.coords.toRealRoom(), TeleportType.Exempt)
            return
        }
        doors.open(this, door, TombDoorOpen)
    }

    private suspend fun ProtectedAccess.placeRune(pillar: BoundLocInfo, rune: String) {
        val runeIndex = PillarRunes.getValue(rune)
        val pillarIndex = pillarIndex(pillar.coords) ?: return
        val bit = 1 shl (pillarIndex * RunesPerPillar + runeIndex)
        val placed = player.pillarRunes
        if ((placed and bit) != 0) {
            mes("You have already placed that kind of rune on this pillar.")
            return
        }
        if (invDel(inv, rune).failure) {
            return
        }
        anim(PlaceSeq)
        soundSynth(PlaceSound)
        spotanimMap(worldRepo, SmokeSpotanim, pillar.coords)
        player.pillarRunes = placed or bit
        objbox(rune, "You place the rune on the pillar and it disappears in a puff of smoke.")
        if (waterfall.allRunesPlaced(player)) {
            waterfall.advanceTo(this, RunesPlaced)
        }
    }

    private suspend fun ProtectedAccess.placeAmulet() {
        if (player.coords.inRaisedCopy()) {
            mes("Glarial's statue already wears her amulet.")
            return
        }
        if (!waterfall.allRunesPlaced(player)) {
            invDel(inv, Amulet)
            mesbox(
                "You go to place the amulet on the statue, but the pillars are not ready and " +
                    "water floods the chamber..."
            )
            mesbox("...and washes you out of the cave and down the river.")
            washDownstream(bruised = true)
            return
        }
        if (invDel(inv, Amulet).failure) {
            return
        }
        mesbox(
            "You place the amulet around the statue's neck. You hear a loud rumble as the " +
                "floor rises."
        )
        soundSynth(RumbleSound)
        fadeToBlack()
        telejump(player.coords.toRaisedCopy(), TeleportType.Exempt)
        delay(1)
        fadeFromBlack()
        waterfall.advanceTo(this, FloorRisen)
    }

    private suspend fun ProtectedAccess.takeTreasure() {
        if (waterfall.isComplete(player)) {
            mes("The chalice only contains some old ashes.")
            return
        }
        mesbox("You go to take the treasure from the chalice, but water floods the chamber...")
        mesbox("...and washes you out of the cave and down the river.")
        washDownstream(bruised = true)
    }

    private suspend fun ProtectedAccess.pourAshes() {
        if (waterfall.isComplete(player)) {
            mes("The chalice only contains some old ashes.")
            return
        }
        if (waterfall.stage(player) < FloorRisen) {
            mes("You can't reach the chalice from here.")
            return
        }
        if (inv.freeSpace() < RewardSlots) {
            mesbox("You need at least $RewardSlots free inventory spaces to carry the treasure.")
            return
        }
        if (invDel(inv, UrnFull).failure) {
            return
        }
        invAddOrDrop(objRepo, UrnEmpty)
        anim(PourSeq)
        soundSynth(PourSound)
        mesbox("You carefully pour Glarial's ashes into the chalice and take Baxtorian's treasure.")
        waterfall.quest.completeQuest(this)
    }

    private companion object {
        const val ExitDoor = "loc.baxtorian_door_waterfall_quest"
        const val Crate = "loc.baxtorian_crate_waterfall_quest"
        const val TombDoor = "loc.baxtorian_door_2_waterfall_quest"
        const val TombDoorOpen = "loc.baxtorian_door_2_open_waterfall_quest"

        /**
         * The pillar is a multiloc on `varbit.sote`, and the used-item bridge hands the resolved
         * form to `onOpLocU`, so every form is registered.
         */
        val PillarForms =
            listOf(
                "loc.stonepillar_small_waterfall_quest",
                "loc.stonepillar_small_waterfall_quest_noop",
                "loc.stonepillar_small_waterfall_quest_op",
            )
        const val StatueGlarial = "loc.statue_queen_waterfall_quest"
        const val Chalice = "loc.baxtorian_chalice_waterfall_quest"
        const val ChaliceAsh = "loc.baxtorian_chalice_waterfall_quest_ash"

        /** Rune obj to its bit within a pillar's three. */
        val PillarRunes = mapOf("obj.airrune" to 0, "obj.waterrune" to 1, "obj.earthrune" to 2)

        const val PillarWestX = 2562
        const val PillarEastX = 2569
        const val PillarSouthZ = 9910
        const val PillarRowGap = 2

        const val CopyOffsetX = 38
        const val CopyOffsetZ = -1
        const val CopyMinX = 2590

        const val RewardSlots = 5

        const val SearchSeq = "seq.human_pickuptable"
        const val PlaceSeq = "seq.human_pickuptable"
        const val PourSeq = "seq.human_pickuptable"
        const val SmokeSpotanim = "spotanim.smokepuff"
        const val DoorSound = "synth.door_open"
        const val LockedSound = "synth.irondoor_locked"
        const val PlaceSound = "synth.smokepuff"
        const val RumbleSound = "synth.contact_rumble"
        const val PourSound = "synth.vial_pour"

        fun CoordGrid.inRaisedCopy(): Boolean = x >= CopyMinX

        fun CoordGrid.toRaisedCopy(): CoordGrid = translate(CopyOffsetX, CopyOffsetZ)

        fun CoordGrid.toRealRoom(): CoordGrid = translate(-CopyOffsetX, -CopyOffsetZ)

        fun pillarIndex(coords: CoordGrid): Int? {
            val real = if (coords.inRaisedCopy()) coords.toRealRoom() else coords
            val column =
                when (real.x) {
                    PillarWestX -> 0
                    PillarEastX -> 1
                    else -> return null
                }
            val offset = real.z - PillarSouthZ
            val row = offset / PillarRowGap
            if (row !in 0 until PillarCount / 2 || offset % PillarRowGap != 0) {
                return null
            }
            return column * (PillarCount / 2) + row
        }
    }
}
