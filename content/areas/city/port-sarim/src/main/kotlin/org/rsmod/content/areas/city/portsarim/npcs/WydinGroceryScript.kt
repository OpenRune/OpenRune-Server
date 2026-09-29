package org.rsmod.content.areas.city.portsarim.npcs

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpObj3
import org.rsmod.content.generic.locs.doors.DoorTranslations
import org.rsmod.content.quest.manager.Quest
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.obj.Obj
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal var Player.wydinJob by boolVarBit("varbit.pirates_treasure_wydin_job")
private var Player.rumShipped by boolVarBit("varbit.pirates_treasure_rum_shipped")

internal fun Player.onPiratesTreasure(): Boolean =
    Quest.get("quest_piratestreasure")?.isQuestInProgress(this) == true

private fun Player.wearingApron(): Boolean = worn.count(WHITE_APRON) > 0

private fun Player.hasApron(): Boolean = wearingApron() || inv.count(WHITE_APRON) > 0

internal suspend fun Dialogue.askForJob() {
    chatPlayer(quiz, "Can I get a job here?")
    chatNpc(
        neutral,
        "Well, you're keen, I'll give you that. Okay, I'll give you a go. Have you got your own " +
            "white apron?",
    )
    if (!player.hasApron()) {
        chatPlayer(sad, "No, I haven't.")
        chatNpc(
            neutral,
            "Well, you can't work here unless you have a white apron. Health and safety " +
                "regulations, you understand.",
        )
        chatPlayer(quiz, "Where can I get one of those?")
        chatNpc(
            neutral,
            "Well, I get all of mine over at the clothing shop in Varrock. They sell them cheap " +
                "there.",
        )
        chatNpc(
            neutral,
            "Oh, and I'm sure that I've seen a spare one over in Gerrant's fish store somewhere. " +
                "It's the little place just north of here.",
        )
        return
    }
    chatPlayer(happy, "Yes, I have one right here.")
    player.wydinJob = true
    chatNpc(
        happy,
        "Wow - you are well prepared! You're hired. Go through to the back and tidy up for me, " +
            "please.",
    )
    if (!player.wearingApron()) {
        chatNpc(neutral, "You need to put your white apron on first though.")
    }
}

internal suspend fun Dialogue.wydinEmployee(openShop: () -> Unit) {
    chatNpc(quiz, "Is it nice and tidy round the back now?")
    when (
        menu(
            listOf(
                "Yes, can I work out front now?" to 1,
                "Yes, are you going to pay me yet?" to 2,
                "No, it's a complete mess" to 3,
                "Can I buy something please?" to 4,
            )
        )
    ) {
        1 -> {
            chatPlayer(quiz, "Yes, can I work out front now?")
            chatNpc(neutral, "No, I'm the one who works here.")
        }
        2 -> {
            chatPlayer(quiz, "Yes, are you going to pay me yet?")
            chatNpc(shifty, "Umm... No, not yet.")
        }
        3 -> {
            chatPlayer(sad, "No, it's a complete mess.")
            chatNpc(neutral, "Ah well, it'll give you something to do, won't it.")
        }
        4 -> {
            chatPlayer(quiz, "Can I buy something please?")
            chatNpc(happy, "Yes, of course.")
            openShop()
        }
    }
}

class WydinGroceryScript
@Inject
constructor(
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val objRepo: ObjRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.wydindoor") { openBackDoor(it.loc) }
        onOpLoc1("loc.grocerycrate") { searchGroceryCrate() }
        val apron = ServerCacheManager.getItem(SPARE_APRON.asRSCM(RSCMType.OBJ)) ?: error("Missing $SPARE_APRON")
        onOpObj3(apron) { takeSpareApron(it.obj) }
    }

    private suspend fun ProtectedAccess.takeSpareApron(obj: Obj) {
        if (inv.isFull()) {
            mes(Constants.dm_take_invspace)
            return
        }
        if (coords != obj.coords) {
            delay(1)
        }
        anim("seq.human_pickuptable")
        if (!objRepo.del(obj)) {
            mes(Constants.dm_take_taken)
            return
        }
        delay(2)
        invAdd(inv, WHITE_APRON)
        mes("You take an apron. It feels freshly starched and smells of laundry.")
    }

    private suspend fun ProtectedAccess.openBackDoor(door: BoundLocInfo) {
        arriveDelay()
        val insideStoreroom = coords.x < door.coords.x
        if (!insideStoreroom) {
            if (!player.wydinJob) {
                stopAtDoor()
                return
            }
            if (!player.wearingApron()) {
                val wydin = findWydin() ?: return
                startDialogue(wydin) {
                    chatNpc(neutral, "Can you put your white apron on before going in there, please?")
                }
                return
            }
        }
        val dest = if (insideStoreroom) door.coords else door.coords.translateX(-1)
        val openCoords = DoorTranslations.translateOpen(door.coords, door.shape, door.angle)
        locRepo.del(door, DOOR_OPEN_TICKS)
        locRepo.add(
            openCoords,
            "loc.wydindooropen",
            DOOR_OPEN_TICKS,
            door.turnAngle(rotations = 1),
            door.shape,
        )
        soundSynth("synth.door_open")
        playerMove(dest)
    }

    private suspend fun ProtectedAccess.stopAtDoor() {
        val wydin = findWydin() ?: return
        startDialogue(wydin) {
            chatNpc(
                neutral,
                "Hey, you can't go in there. Only employees of the grocery store can go in.",
            )
            val job = menu(listOf("Well, can I get a job here?" to true, "Sorry, I didn't realise." to false))
            if (job) {
                askForJob()
            } else {
                chatPlayer(neutral, "Sorry, I didn't realise.")
            }
        }
    }

    private fun ProtectedAccess.findWydin() =
        npcRepo.findAll(ZoneKey.from(player.coords), zoneRadius = 1).firstOrNull { it.isType(WYDIN) }

    private suspend fun ProtectedAccess.searchGroceryCrate() {
        arriveDelay()
        mes("There are a lot of bananas in the crate.")
        if (player.rumShipped) {
            anim("seq.human_pickuptable")
            delay(2)
            player.rumShipped = false
            invAdd(inv, "obj.karamja_rum")
            mes("You find your bottle of rum in amongst the bananas.")
        }
        startDialogue {
            val take = choice2("Yes.", true, "No.", false, title = "Do you want to take a banana?")
            if (take) {
                access.anim("seq.human_pickuptable")
                access.invAdd(access.inv, "obj.banana")
                access.mes("You take a banana.")
            }
        }
    }

    private companion object {
        const val WYDIN = "npc.wydin"
        const val DOOR_OPEN_TICKS = 3
    }
}

private const val WHITE_APRON = "obj.white_apron"
private const val SPARE_APRON = "obj.piratetreasure_apron"
