package org.rsmod.content.areas.city.draynor

import jakarta.inject.Inject
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.content.quest.manager.Quest
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DraynorManorSearch @Inject constructor(private val locRepo: LocRepository) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpHeldU(Poison, FishFood) { poisonFishFood() }

        onOpLoc1(Fountain) { searchFountain() }
        onOpLocU(Fountain, PoisonedFishFood) { poisonFountain() }

        onOpLoc1(CompostHeap) { digCompost() }
        onOpLocU(CompostHeap, Spade) { digCompost() }

        onOpLoc1(ClosetDoor) { openCloset(it.loc) }
        onOpLocU(ClosetDoor, ClosetKey) { openCloset(it.loc) }
        for (shelf in Bookcases) {
            onOpLoc1(shelf) { openBookcase(it.loc) }
        }
        onOpLoc1(ExitLever) { pullExitLever() }
    }

    private suspend fun ProtectedAccess.poisonFishFood() {
        if (invReplace(inv, FishFood, 1, PoisonedFishFood).failure) {
            return
        }
        invDel(inv, Poison, 1)
        mes("You poison the fish food.")
    }

    private suspend fun ProtectedAccess.searchFountain() {
        arriveDelay()
        soundSynth(FishSwimSound)

        if (!canFindPressureGauge(player)) {
            mes("You search the fountain and find nothing of interest.")
            return
        }

        if (inv.count(PressureGauge) > 0) {
            mes("You have already taken the pressure gauge from the fountain.")
            return
        }

        startDialogue {
            chatPlayer(neutral, "There seems to be a pressure gauge in here...")

            if (!player.attr.getOrDefault(PIRANHAS_DEAD, false)) {
                access.mes("Something in the water bites you.")
                access.soundSynth(BiteSound, delay = BiteSoundDelay)
                access.queueHit(delay = 1, type = HitType.Typeless, damage = PiranhaDamage)
                chatPlayer(sad, "Ow!")
                chatPlayer(sad, "... and a lot of piranhas! I can't get the gauge out.")
                return@startDialogue
            }

            chatPlayer(neutral, "... and a lot of dead fish.")
            if (access.invAdd(access.inv, PressureGauge, 1).success) {
                access.mes("You get the pressure gauge from the fountain.")
            }
        }
    }

    private suspend fun ProtectedAccess.poisonFountain() {
        arriveDelay()

        if (player.attr.getOrDefault(PIRANHAS_DEAD, false)) {
            mes("The piranhas in the fountain are already dead.")
            return
        }

        if (invDel(inv, PoisonedFishFood, 1).failure) {
            return
        }

        player.attr[PIRANHAS_DEAD] = true
        mes("You pour the poisoned fish food into the fountain.")
        soundSynth(SprinkleSound)
        delay(2)
        mes("The piranhas start eating the food...")
        soundSynth(FishSwimSound)
        delay(3)
        mes("... then die and float to the surface.")
    }

    private suspend fun ProtectedAccess.digCompost() {
        arriveDelay()

        if (Spade !in inv) {
            // Reset on refusal too: a cancelled dig may leave its looping sequence active.
            resetAnim()
            mes("You need a spade to dig through this.")
            return
        }

        mes("You dig through the compost...")
        anim(DigAnim)
        soundSynth(DigSound)
        delay(3)
        resetAnim()

        if (!findClosetKey(this)) {
            mes("... but you find nothing of interest.")
            return
        }
        mes("... and find a small key.")
    }

    private suspend fun ProtectedAccess.openCloset(door: BoundLocInfo) {
        arriveDelay()

        if (ClosetKey !in inv) {
            mes("The door is locked.")
            return
        }

        mes("You unlock the door.")
        soundSynth(UnlockSound)
        delay(1)

        val route = crossingRoute(door)
        val openTicks = crossingOpenTicks(crossingTiles(route))

        locRepo.del(door, openTicks)
        locRepo.add(
            door.acrossTile(),
            OpenClosetLoc,
            openTicks,
            door.turnAngle(rotations = 1),
            door.shape,
        )
        soundSynth(CupboardSound)
        crossDoorway(route)
    }

    private suspend fun ProtectedAccess.openBookcase(bookcase: BoundLocInfo) {
        if (player.coords.x < BookcaseX) {
            return
        }
        val level = bookcase.coords.level
        // Still in the shelf column: step east clear of it, then along to the searched shelf's own
        // row. Two legs, never one diagonal - the diagonal cuts the corner through a closed shelf.
        if (player.coords.x == BookcaseX) {
            crossDoorway(CoordGrid(BookcaseEntryX, player.coords.z, level))
            crossDoorway(CoordGrid(BookcaseEntryX, bookcase.coords.z, level))
        }
        mes("You've found a secret door!")
        val exit = CoordGrid(BookcaseExitX, bookcase.coords.z, level)
        openBookcasePassage(crossingOpenTicks(crossingTiles(listOf(exit))))
        soundSynth(BookcaseSound)
        crossDoorway(exit)
    }

    // Use two orthogonal legs; a diagonal exit would cut through the closed shelf.
    private suspend fun ProtectedAccess.pullExitLever() {
        mes("The lever opens the secret door!")
        soundSynth(LeverSound)

        val level = player.coords.level
        val route =
            listOf(
                CoordGrid(LeverExitX, LeverExitZ, level),
                CoordGrid(BookcaseEntryX, LeverExitZ, level),
            )
        openBookcasePassage(crossingOpenTicks(crossingTiles(route)))
        soundSynth(BookcaseLeverSound)

        crossDoorway(route)
    }

    private fun ProtectedAccess.openBookcasePassage(openTicks: Int) {
        for (shelf in BookcaseShelves) {
            val existing = locRepo.findExact(shelf.at, LocShape.CentrepieceStraight)
            if (existing != null) {
                locRepo.del(existing, openTicks)
            }
            locRepo.add(
                shelf.slidesTo,
                OpenBookcaseLoc,
                openTicks,
                LocAngle.West,
                LocShape.CentrepieceStraight,
            )
        }
    }

    private class Shelf(val at: CoordGrid, val slidesTo: CoordGrid)

    internal companion object {
        private const val Fountain = "loc.hauntedfountain"
        private const val CompostHeap = "loc.hauntedcompostheap"
        private const val ClosetDoor = "loc.closet_door"
        private val Bookcases = listOf("loc.hauntedbookcasel", "loc.hauntedbookcaser")
        private const val ExitLever = "loc.hauntedleverup"
        private const val OpenClosetLoc = "loc.draynorpanelleddoor_inactive"
        private const val OpenBookcaseLoc = "loc.hauntedbookcaseopen"
        private const val LeverSound = "synth.lever"

        private const val BookcaseX = 3097
        private const val BookcaseEntryX = 3098
        private const val BookcaseExitX = 3096

        private const val LeverExitX = 3096
        private const val LeverExitZ = 3358

        private val BookcaseShelves =
            listOf(
                Shelf(CoordGrid(3097, 3358, 0), CoordGrid(3097, 3357, 0)),
                Shelf(CoordGrid(3097, 3359, 0), CoordGrid(3097, 3360, 0)),
            )

        private const val FishFood = "obj.fish_food"
        private const val Poison = "obj.poison"
        private const val PoisonedFishFood = "obj.poisoned_fish_food"
        private const val PressureGauge = "obj.pressure_gauge"

        private const val ErnestQuestKey = "quest_ernestthechicken"
        private const val ClosetKey = "obj.closet_key"
        private const val Spade = "obj.spade"

        private const val FishSwimSound = "synth.fish_swim"
        private const val BiteSound = "synth.human_hit_2"
        private const val BiteSoundDelay = 20
        private const val PiranhaDamage = 1
        private const val SprinkleSound = "synth.ernest_sprinkle"
        private const val DigAnim = "seq.human_dig"
        private const val DigSound = "synth.digspade"
        private const val UnlockSound = "synth.unlock"
        private const val CupboardSound = "synth.cupboard_open"
        private const val BookcaseSound = "synth.coffin_open"
        private const val BookcaseLeverSound = "synth.bookcase_open_and_close"

        private val PIRANHAS_DEAD = AttributeKey<Boolean>(persistenceKey = "ernest_piranhas_dead")

        internal fun canFindPressureGauge(player: Player): Boolean =
            Quest.get(ErnestQuestKey)?.isQuestInProgress(player) == true

        internal fun findClosetKey(access: ProtectedAccess): Boolean = with(access) {
            if (ClosetKey in inv || ClosetKey in bank) {
                return false
            }
            invAdd(inv, ClosetKey, 1).success
        }
    }
}
