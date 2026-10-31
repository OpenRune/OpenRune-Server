package org.rsmod.content.quest.area.goblinvillage

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.MesAnimType
import dev.openrune.types.hunt.HuntVis
import jakarta.inject.Inject
import org.rsmod.api.enums.NamedEnums.gobdip_mail_dyes
import org.rsmod.api.hunt.NpcSearch
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.cinematic.CameraMode
import org.rsmod.api.player.cinematic.Cinematic
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.content.quest.area.misthalin.SceneCamera
import org.rsmod.content.quest.area.misthalin.SceneRegion
import org.rsmod.content.quest.area.misthalin.lockedScene
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.content.quest.manager.startQuestPrompt
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class GoblinDiplomacy
@Inject
constructor(
    private val regionRepo: RegionRepository,
    private val npcRepo: NpcRepository,
    private val search: NpcSearch,
) :
    QuestScript(
    "quest_goblindiplomacy",
    "varp.goblinquest",
    rewards {
        xp("stat.crafting", CRAFTING_XP)
        item(GOLD_BAR, 1, label = "A gold bar")
    },
    ItemRewardDisplay(GOBLIN_MAIL, zoom = 300),
    questVarbit = "varbit.gobdip_main",
) {
    private var Player.grubfootLook by intVarBit("varbit.gobdip_grubfoot_vis")
    private var Player.sceneReturnTile by intVarp("varp.gobdip_return_tile")
    private var Player.knowsAboutArmour by intVarBit("varbit.gobdip_know_about_armour")
    private var Player.knowsAboutDye by intVarBit("varbit.gobdip_know_about_dye")
    private val Player.metAggie by intVarBit("varbit.gobdip_met_aggie")

    private val sceneRegions = HashMap<Long, Region>()

    override fun ScriptContext.init() {
        check(quest.maxSteps == COMPLETE) {
            "Goblin Diplomacy end state is ${quest.maxSteps} in the cache dbrow, but the brown " +
                "armour writes $COMPLETE."
        }
        for (general in GENERALS) {
            onOpNpc1(general) { talkToGeneral(it.npc) }
            onOpNpcU(general) { useOnGeneral(it.npc, it.objType) }
        }
        onOpNpc1(GRUBFOOT_MULTI) { startDialogue(it.npc) { grubfootDialogue() } }
        for (crate in Crate.entries) {
            onOpLoc1(crate.loc) { searchCrate(crate) }
        }
        for ((dye, dyed) in gobdip_mail_dyes.filterValuesNotNull()) {
            for (mail in ARMOURS - dyed.internalName) {
                onOpHeldU(dye, mail) { dyeMail(dye, it.firstSlot, mail, it.secondSlot, dyed) }
            }
        }
        onOpHeldU(RED_DYE, YELLOW_DYE) { mixDyes(it.firstSlot, it.secondSlot, ORANGE_DYE, "an orange") }
        onOpHeldU(YELLOW_DYE, BLUE_DYE) { mixDyes(it.firstSlot, it.secondSlot, GREEN_DYE, "a green") }
        onOpHeldU(RED_DYE, BLUE_DYE) { mixDyes(it.firstSlot, it.secondSlot, PURPLE_DYE, "a purple") }
        onEvent<SessionStateEvent.PrepareLogin> { restoreLeftScene(player) }
        onPlayerLogin { discardLeftScene(player) }
    }

    private fun restoreLeftScene(player: Player) {
        val saved = player.sceneReturnTile
        if (saved == 0) return
        player.coords = CoordGrid(saved)
        player.sceneReturnTile = 0
        Cinematic.setHideToplevel(player, hide = false)
        Cinematic.setHideHealthHud(player, hide = false)
        Cinematic.setCameraMode(player, CameraMode.Normal)
        Cinematic.setMaxDrawDistance(player, enabled = false)
        Cinematic.restoreAcceptAid(player)
        VarPlayerIntMapSetter.set(player, "varbit.minimap_state", 0)
    }

    private fun discardLeftScene(player: Player) {
        val region = player.uuid?.let(sceneRegions::remove) ?: return
        regionRepo.unprotect(region)
    }

    override fun subTitle(): String =
        "talking to the <col=800000>Goblin Generals<col=000080> in " +
            "<col=800000>Goblin Village<col=000080>."

    override fun questLog(player: ProtectedAccess) =
        questJournal(player) {
            val p = player.player
            val stage = stage(p)
            val hasMail = p.inv.count(GOBLIN_MAIL) > 0
            if (stage == STARTED) {
                when {
                    p.inv.count(ORANGE_MAIL) > 0 ->
                        line(
                            "$ASKED_FOR_ORANGE I should give them the <red>Orange Goblin " +
                                "Mail</red> that I have."
                        )
                    p.metAggie != 0 ->
                        line(if (hasMail) "$ASKED_FOR_ORANGE $AGGIE" else "$ASKED_FOR_ORANGE $AGGIE $CRATES")
                    p.knowsAboutDye != 0 || hasMail -> {
                        line(ASKED_FOR_ORANGE)
                        line(if (p.knowsAboutArmour != 0 && !hasMail) "$WITCH $CRATES" else WITCH)
                    }
                    p.knowsAboutArmour != 0 ->
                        line(
                            "$ASKED_FOR_ORANGE I should be able to find some <red>Goblin " +
                                "Mail</red> in crates around <red>Goblin Village</red>, though " +
                                "I'll need to find a way to change the colour. <red>General " +
                                "Wartface</red> and <red>General Bentnoze</red> might have an " +
                                "idea on how to do that."
                        )
                    else ->
                        line("$ASKED_FOR_ORANGE Perhaps they'll have an idea on how I can get some.")
                }
                return@questJournal
            }
            strike(DONE_ASKED)
            if (stage == BLUE_WANTED) {
                val next =
                    when {
                        p.inv.count(BLUE_MAIL) > 0 ->
                            "I should give them the <red>Blue Goblin Mail</red> that I have."
                        hasMail && p.inv.count(BLUE_DYE) > 0 ->
                            "I should use my <red>Blue Dye</red> to recolour some <red>Goblin " +
                                "Mail</red> and then give it to them."
                        hasMail -> AGGIE
                        else -> "$AGGIE $CRATES"
                    }
                line("$GAVE_ORANGE $next")
                return@questJournal
            }
            strike(DONE_GAVE_ORANGE)
            val next =
                if (hasMail) {
                    "I should give them the <red>Brown Goblin Mail</red> that I have."
                } else {
                    "I should be able to find some <red>Brown Goblin Mail</red> in crates around " +
                        "<red>Goblin Village</red>."
                }
            line("$GAVE_BLUE $next")
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line(DONE_ASKED)
            line(DONE_GAVE_ORANGE)
            line(
                "I gave some Blue Goblin Mail to General Wartface and General Bentnoze, but they " +
                    "didn't like it. They asked me to bring them some of their original Brown " +
                    "Goblin Mail to try out instead."
            )
            line(
                "I gave General Wartface and General Bentnoze some of the original Brown Goblin " +
                    "Mail, and it turned out to be their favourite. They're clearly not the " +
                    "smartest, but that's goblins for you."
            )
        }

    private fun stage(player: Player): Int = quest.getQuestStage(player)

    private enum class Crate(val loc: String, val searched: String) {
        North("loc.goblin_outpost_large_crate_armour1", "varbit.gobdip_crate1_searched"),
        West("loc.goblin_outpost_large_crate_armour2", "varbit.gobdip_crate2_searched"),
        Upstairs("loc.goblin_outpost_large_crate_armour3", "varbit.gobdip_crate3_searched"),
    }

    private suspend fun ProtectedAccess.searchCrate(crate: Crate) {
        if (vars[crate.searched] != 0) {
            mes("You search the crate, but find nothing of interest.")
            return
        }
        if (invAdd(inv, GOBLIN_MAIL).failure) {
            mes("You don't have enough inventory space.")
            return
        }
        vars[crate.searched] = 1
        soundSynth("synth.pick")
        startDialogue { objbox(GOBLIN_MAIL, "You find some goblin mail in the crate.") }
    }

    private fun ProtectedAccess.dyeMail(
        dye: ItemServerType,
        dyeSlot: Int,
        mail: String,
        mailSlot: Int,
        dyed: ItemServerType,
    ) {
        if (combine(dye.internalName, dyeSlot, mail, mailSlot, dyed.internalName)) {
            soundSynth("synth.gobdip_dye_goblin_mail")
            mes("You dye the goblin mail ${dyed.name.substringBefore(' ').lowercase()}.")
        }
    }

    private fun ProtectedAccess.mixDyes(first: Int, second: Int, result: String, colour: String) {
        val firstObj = inv[first] ?: return
        val secondObj = inv[second] ?: return
        val firstName = RSCM.getReverseMapping(RSCMType.OBJ, firstObj.id)
        val secondName = RSCM.getReverseMapping(RSCMType.OBJ, secondObj.id)
        if (combine(firstName, first, secondName, second, result)) {
            mes("You mix the two dyes and make $colour one.")
        }
    }

    private fun ProtectedAccess.combine(
        first: String,
        firstSlot: Int,
        second: String,
        secondSlot: Int,
        result: String,
    ): Boolean =
        player.invTransaction(inv) {
            val from = select(inv)
            delete {
                this.from = from
                this.obj = first.asRSCM(RSCMType.OBJ)
                this.strictCount = 1
                this.strictSlot = firstSlot
            }
            delete {
                this.from = from
                this.obj = second.asRSCM(RSCMType.OBJ)
                this.strictCount = 1
                this.strictSlot = secondSlot
            }
            insert {
                this.into = from
                this.obj = result.asRSCM(RSCMType.OBJ)
                this.strictCount = 1
            }
        }.success

    private suspend fun ProtectedAccess.useOnGeneral(npc: Npc, obj: ItemServerType) {
        val stage = stage(player)
        val wanted = wantedArmour(stage)
        if (wanted == null || obj.id !in ARMOURS.map { it.asRSCM(RSCMType.OBJ) }) {
            mes("Nothing interesting happens.")
            return
        }
        startDialogue(npc) {
            if (obj.id != wanted.asRSCM(RSCMType.OBJ)) {
                chatPlayer(quiz, "What do you think of this colour?")
                bentnoze(angry, "That wrong colour.")
                wartface(angry, "We tell you get ${wantedColour(stage)} armour.")
                return@startDialogue
            }
            handIn(stage)
        }
    }

    private suspend fun ProtectedAccess.talkToGeneral(npc: Npc) {
        val wartface = npc.id == WARTFACE.asRSCM(RSCMType.NPC)
        val other = if (wartface) BENTNOZE else WARTFACE
        if (npcFind(npc.coords, other, GENERAL_SIGHT, HuntVis.LineOfSight, search) == null) {
            startDialogue(npc) {
                if (wartface) {
                    wartface(confused, "Where's Bentnoze gone now?")
                } else {
                    bentnoze(confused, "Where's Wartface gone now?")
                }
            }
            return
        }
        startDialogue(npc) { generalsDialogue() }
    }

    private suspend fun Dialogue.generalsDialogue() {
        val stage = stage(player)
        if (stage >= COMPLETE) {
            afterQuest()
            return
        }
        when (access.random.of(maxExclusive = 5)) {
            0 -> if (!fatOpener()) return
            1 -> biggerGeneral()
            2 -> stupidGeneral()
            3 -> {
                wartface(neutral, "Green armour best.")
                bentnoze(angry, "No no red every time.")
                wartface(angry, "Go away human, we busy.")
            }
            else -> {
                bentnoze(neutral, "Red armour best.")
                wartface(angry, "No it has to be green!")
                bentnoze(angry, "Go away human, we busy.")
            }
        }
        mainMenu()
    }

    private suspend fun Dialogue.fatOpener(): Boolean {
        bentnoze(neutral, "All goblins should wear red armour!")
        wartface(angry, "Not red! Red armour make you look fat.")
        bentnoze(laugh, "Everything make YOU look fat!")
        wartface(angry, "Shut up!")
        bentnoze(laugh, "Fatty!")
        wartface(angry, "SHUT UP!")
        bentnoze(quiz, "Even this human think you look fat! Don't you, human?")
        chatPlayer(confused, "Um...")
        when (
            choice3(
                "Yes, he looks fat.",
                1,
                "No, he doesn't look fat.",
                2,
                "I'll leave you to it.",
                3,
            )
        ) {
            1 -> {
                chatPlayer(laugh, "Yes, he looks fat!")
                bentnoze(laugh, "Ha ha! See, fatty? Even human think you fat!")
                wartface(angry, "Me not care what human think! Human ugly!")
            }
            2 -> {
                chatPlayer(neutral, "No, he doesn't look fat.")
                bentnoze(angry, "Shut up human! Wartface fat and human stupid!")
                wartface(angry, "Shut up Bentnoze!")
            }
            else -> {
                chatPlayer(neutral, "I'll leave you to it.")
                return false
            }
        }
        return true
    }

    private suspend fun Dialogue.biggerGeneral() {
        wartface(neutral, "I tell all goblins in village to wear green armour now!")
        bentnoze(angry, "They not listen to you! I already tell them wear red armour!")
        wartface(angry, "They listen to me not you! They know me bigger general!")
        bentnoze(angry, "Me bigger general! They listen to me!")
        wartface(quiz, "Human! What colour armour they wearing out there?")
        chatPlayer(neutral, "Half of them are wearing red and half of them green.")
        wartface(
            angry,
            "Shut up human! They wearing green armour really! Human lying because he scared of " +
                "you!",
        )
        bentnoze(laugh, "Human scared of me not you? Then you think me bigger general!")
        wartface(confused, "What? Me mean...")
        wartface(angry, "Shut up! Me bigger general!")
    }

    private suspend fun Dialogue.stupidGeneral() {
        wartface(neutral, "We should wear green armour!")
        bentnoze(angry, "Green armour? Are you stupid?")
        wartface(angry, "You stupid! Only stupid goblins think red armour better!")
        bentnoze(angry, "No they don't! Me think red armour better!")
        wartface(angry, "That because you stupid!")
        bentnoze(angry, "Me not stupid!")
        wartface(quiz, "Then why you not like green armour?")
        bentnoze(angry, "Because red armour better!")
        wartface(angry, "Only stupid goblins think that! You stupid!")
    }

    private suspend fun Dialogue.mainMenu() {
        var askedWhy = false
        while (true) {
            val first = if (askedWhy) "What is the goblin new century?" else WHY_ARGUING
            when (
                choice4(
                    first,
                    1,
                    "Wouldn't you prefer peace?",
                    2,
                    "Do you want me to pick an armour colour for you?",
                    3,
                    "I'll leave you to it.",
                    4,
                )
            ) {
                1 ->
                    if (askedWhy) {
                        newCentury()
                    } else {
                        whyArguing()
                        askedWhy = true
                    }
                2 -> {
                    chatPlayer(quiz, "Wouldn't you prefer peace?")
                    wartface(
                        neutral,
                        "Goblins not so good at peace. Maybe peace be okay though, as long as it " +
                            "peace wearing green armour.",
                    )
                    bentnoze(angry, "But green too much like skin. Nearly make you look naked!")
                }
                3 -> {
                    pickColour()
                    return
                }
                else -> {
                    chatPlayer(neutral, "I'll leave you to it.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.whyArguing() {
        chatPlayer(quiz, WHY_ARGUING)
        bentnoze(
            neutral,
            "We decide to celebrate goblin new century by changing colour of our armour. Brown " +
                "get boring after a bit. We want change.",
        )
        wartface(
            neutral,
            "Problem is, we can't agree on new colour. We think maybe we use old goblin tribe " +
                "colours, but Bentnoze from rubbish Thorobshuun tribe and me from much better " +
                "Garagorshuun tribe.",
        )
        bentnoze(
            angry,
            "Thorobshuun tribe colour is red! Way better than silly green of Garagorshuun tribe.",
        )
        wartface(angry, "Shut up!")
    }

    private suspend fun Dialogue.newCentury() {
        chatPlayer(quiz, "What is the goblin new century?")
        bentnoze(
            neutral,
            "Goblin century mark year of battle on Plain of Mud. That when Big High War God " +
                "give goblin commandments.",
        )
        chatPlayer(quiz, "Big High War God?")
        wartface(
            neutral,
            "Big High War God is god for goblins. He take us and make us strong, not stupid " +
                "like humans.",
        )
        bentnoze(neutral, "Me bet Big High War God would pick red armour.")
        wartface(angry, "Shut up! Big High War God would hate you!")
    }

    private suspend fun Dialogue.pickColour() {
        chatPlayer(quiz, "Do you want me to pick an armour colour for you?")
        wartface(neutral, "Yes, as long as you pick green.")
        bentnoze(angry, "No you have to pick red!")
        while (true) {
            when (
                choice4(
                    "You should wear red.",
                    1,
                    "You should wear green.",
                    2,
                    "What about a different colour?",
                    3,
                    "I'll leave you to it.",
                    4,
                )
            ) {
                1 -> {
                    chatPlayer(neutral, "You should wear red.")
                    bentnoze(laugh, "See? Even stupid human think red best. Now we all wear red!")
                    wartface(
                        angry,
                        "Human not know anything! If we wear red then whole village be ugly like " +
                            "YOU!",
                    )
                    bentnoze(angry, "Go away human. You not helping.")
                }
                2 -> {
                    chatPlayer(neutral, "You should wear green.")
                    wartface(laugh, "Green! We all wear green now, human has decided!")
                    bentnoze(angry, "Why we have to do what human say? He not boss of us!")
                    wartface(neutral, "No but he agree with me!")
                    bentnoze(angry, "That prove you a filthy human-lover!")
                    wartface(angry, "Me hate humans! This human just happen to be right!")
                    bentnoze(angry, "Go away human. You not know anything.")
                }
                3 -> {
                    differentColour()
                    return
                }
                else -> {
                    chatPlayer(neutral, "I'll leave you to it.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.differentColour() {
        chatPlayer(quiz, "What about a different colour?")
        bentnoze(neutral, "That would mean me wrong... but at least Wartface not right!")
        wartface(
            neutral,
            "Well Bentnoze never been right in his life. Still, maybe new colour good, but will " +
                "have to see armour before decide.",
        )
        val stage = stage(player)
        if (stage >= STARTED) {
            wartface(neutral, "Human! Get us ${wantedColour(stage)} armour!")
            return
        }
        bentnoze(neutral, "Human! You bring us armour in new colour!")
        if (!startQuestPrompt(quest)) {
            chatPlayer(neutral, "Actually, I think I'll leave you to it.")
            return
        }
        chatPlayer(happy, "I can do that.")
        quest.setQuestStage(access, STARTED)
        wartface(quiz, "What colour we try?")
        bentnoze(neutral, "Orange armour might be good.")
        wartface(neutral, "Yep, bring us orange armour.")
        chatPlayer(quiz, "How am I meant to get orange armour?")
        bentnoze(neutral, "Well first you get goblin armour...")
        wartface(neutral, "...and then you dye it orange!")
        bentnoze(laugh, "Even human should be able to work that out!")
        whereToGet()
    }

    private suspend fun Dialogue.whereToGet() {
        while (true) {
            when (
                choice3(
                    "Where do I get goblin armour?",
                    1,
                    "Where do I get dye?",
                    2,
                    "Okay, I'll be back soon.",
                    3,
                )
            ) {
                1 -> {
                    chatPlayer(quiz, "Where do I get goblin armour?")
                    player.knowsAboutArmour = 1
                    wartface(
                        neutral,
                        "There some spare armour around village somewhere. You can take that.",
                    )
                    bentnoze(neutral, "It in crates somewhere. Can't remember which crates now.")
                }
                2 -> {
                    chatPlayer(quiz, "Where do I get dye?")
                    player.knowsAboutDye = 1
                    bentnoze(
                        neutral,
                        "You go north of here into wilderness. There you find many ways to die!",
                    )
                    chatPlayer(neutral, "No, D-Y-E, not D-I-E.")
                    wartface(laugh, "Stupid Bentnoze, you not know how to spell!")
                    bentnoze(angry, "Shut up Wartface!")
                    chatPlayer(quiz, "Do you know where I can get dye?")
                    bentnoze(confused, "Me not know where dye come from.")
                    chatPlayer(quiz, "Well where did you get your red and green dye from?")
                    wartface(
                        neutral,
                        "Some goblin or other, he steal it. Say he steal it from old witch in " +
                            "Draynor Village.",
                    )
                    bentnoze(neutral, "Maybe you can get more dye from her?")
                }
                else -> {
                    chatPlayer(neutral, "Okay, I'll be back soon.")
                    return
                }
            }
        }
    }

    private fun Dialogue.commitHandIn(stage: Int, wanted: String): Boolean {
        if (access.invDel(access.inv, wanted, 1).failure) {
            return false
        }
        when (stage) {
            STARTED -> {
                player.grubfootLook = GRUBFOOT_ORANGE
                quest.setQuestStage(access, BLUE_WANTED)
            }
            BLUE_WANTED -> {
                player.grubfootLook = GRUBFOOT_BLUE
                quest.setQuestStage(access, BROWN_WANTED)
            }
            else -> {
                if (!quest.completeQuest(access)) {
                    access.invAdd(access.inv, wanted, 1)
                    player.grubfootLook = GRUBFOOT_BLUE
                    access.mes(NO_ROOM_FOR_REWARD)
                    return false
                }
                player.grubfootLook = GRUBFOOT_BROWN
            }
        }
        return true
    }

    private suspend fun Dialogue.handIn(stage: Int) {
        val wanted = wantedArmour(stage) ?: return
        val wearing = grubfootHead(player)
        chatPlayer(happy, "I have some ${wantedColour(stage)} armour here.")
        if (tryOn(stage, wanted, wearing) && stage == BROWN_WANTED) {
            commitHandIn(stage, wanted)
        }
    }

    private suspend fun Dialogue.tryOn(stage: Int, wanted: String, wearing: String): Boolean {
        val tryOn =
            when (stage) {
                STARTED -> GRUBFOOT_ORANGE_HEAD
                BLUE_WANTED -> GRUBFOOT_BLUE_HEAD
                else -> GRUBFOOT_BROWN_HEAD
            }
        val returnTo = player.coords
        val region = regionRepo.add(HUT)
        region?.let(regionRepo::protect)
        val r = SceneRegion(region)
        player.sceneReturnTile = returnTo.packed
        player.uuid?.let { uuid -> region?.let { sceneRegions[uuid] = it } }
        val actors = ArrayList<Npc>()
        var committed = stage == BROWN_WANTED
        var verdictGiven = false
        lateinit var grubfoot: Npc
        fun spawn(type: String, coords: CoordGrid): Npc {
            val npc = Npc(type, coords)
            npcRepo.add(npc, SCENE_LIFESPAN)
            npc.noneMode()
            npc.faceSquare(r[GRUBFOOT_TILE])
            actors += npc
            return npc
        }
        try {
            access.lockedScene(
                vantage = r[PLAYER_TILE],
                faceAt = r[GRUBFOOT_TILE],
                camera = r.map(CAMERA),
                returnTo = returnTo,
                underFade = {
                    spawn(WARTFACE, r[WARTFACE_TILE])
                    spawn(BENTNOZE, r[BENTNOZE_TILE])
                    grubfoot = spawn(wearing, r[FETCH_TILE])
                    if (!committed) committed = commitHandIn(stage, wanted)
                },
                teardown = {
                    if (verdictGiven && player.inv.count(wanted) > 0) {
                        player.grubfootLook = GRUBFOOT_BROWN
                    }
                },
            ) {
                if (!committed) return@lockedScene
                grubfoot.walk(FETCH_PATH.map(r::get))
                wartface(neutral, "Grubfoot!")
                grubfoot(neutral, "Yes General Wartface?", wearing)
                wartface(neutral, "Put on this armour!")
                grubfoot.walk(r[CURTAIN_TILE])
                delay(CHANGE_TICKS)
                if (grubfoot.isSlotAssigned) npcRepo.del(grubfoot, Int.MAX_VALUE)
                delay(CHANGE_TICKS)
                grubfoot = spawn(tryOn, r[CURTAIN_TILE])
                grubfoot.walk(r[GRUBFOOT_TILE])
                delay(CHANGE_TICKS)
                grubfoot(quiz, "What do you think?", tryOn)
                when (stage) {
                    STARTED -> {
                        wartface(neutral, "No I don't like that much.")
                        bentnoze(neutral, "It clashes with skin colour.")
                        wartface(neutral, "We need darker colour, like blue.")
                        bentnoze(neutral, "Yeah blue might be good.")
                        wartface(neutral, "Human! Get us blue armour!")
                    }
                    BLUE_WANTED -> {
                        bentnoze(neutral, "That not right. Not goblin colour at all.")
                        wartface(neutral, "Goblins wear dark earthy colours like brown.")
                        bentnoze(neutral, "Yeah brown might be good.")
                        wartface(neutral, "Human! Get us brown armour!")
                        chatPlayer(
                            confused,
                            "But I thought brown was the armour you were changing from...",
                        )
                        chatPlayer(neutral, "Never mind, anything is worth a try.")
                    }
                    else -> {
                        wartface(happy, "That colour quite nice. Me can see myself wearing that.")
                        bentnoze(happy, "It a deal then. Brown armour it is.")
                        wartface(
                            happy,
                            "Thank you for sorting out argument, human. You have reward now.",
                        )
                        chatPlayer(confused, "Thanks... I think...")
                        verdictGiven = true
                    }
                }
            }
            player.sceneReturnTile = 0
        } finally {
            for (npc in actors) if (npc.isSlotAssigned) npcRepo.del(npc, Int.MAX_VALUE)
            player.uuid?.let(sceneRegions::remove)
            region?.let(regionRepo::unprotect)
        }
        return committed
    }

    private suspend fun Dialogue.grubfootDialogue() {
        when (player.grubfootLook) {
            GRUBFOOT_ORANGE -> {
                grubfoot(sad, "Me not like this orange armour. Make me look like that thing.")
                chatPlayer(quiz, "Look like what thing?")
                grubfoot(confused, "That fruit thing. The one that orange. What it called?")
                chatPlayer(quiz, "An orange?")
                grubfoot(sad, "That right. This armour make me look same colour as orange-fruit.")
            }
            GRUBFOOT_BLUE -> {
                grubfoot(sad, "Me not like this blue colour.")
                chatPlayer(quiz, "Why not?")
                grubfoot(sad, "Me not know. It just make me feel...")
                chatPlayer(quiz, "Makes you feel blue?")
                grubfoot(sad, "Makes me feel kind of sad.")
            }
            else ->
                if (stage(player) < COMPLETE) {
                    grubfoot(angry, "Grubfoot wear red armour! Grubfoot wear green armour!")
                    grubfoot(confused, "Why they not make up their minds?")
                } else {
                    grubfootChatter()
                }
        }
        val general = access.random.pick(WARTFACE, BENTNOZE)
        if (general == WARTFACE) {
            wartface(angry, "Shut up Grubfoot!")
        } else {
            bentnoze(angry, "Shut up Grubfoot!")
        }
    }

    private suspend fun Dialogue.grubfootChatter() {
        when (access.random.of(maxExclusive = 5)) {
            0 -> {
                grubfoot(sad, "Me lonely.")
                chatPlayer(quiz, "Why?")
                grubfoot(
                    sad,
                    "Other goblins in village follow either General Wartface or General " +
                        "Bentnoze. Me try to follow both but then me get left out of both groups.",
                )
            }
            1 -> {
                grubfoot(sad, "Don't talk to me!")
                chatPlayer(quiz, "Why not?")
                grubfoot(sad, "Me not allowed to talk. Generals will tell me to shut up whenever I talk.")
            }
            2 ->
                grubfoot(
                    angry,
                    "Grubfoot do this! Grubfoot do that! They always want different things and " +
                        "they never satisfied!",
                )
            3 -> {
                grubfoot(sad, "Me wish generals wouldn't tell me to shut up.")
                grubfoot(confused, "Sometimes they tell me to shut up when me not even say anything!")
            }
            else ->
                grubfoot(
                    sad,
                    "Me wish me weren't so small. Other goblins all pick on me and make me do all " +
                        "hard work for generals.",
                )
        }
    }

    private suspend fun Dialogue.afterQuest() {
        when (access.random.of(maxExclusive = 5)) {
            0 -> {
                bentnoze(angry, "Shut up Grubfoot!")
                grubfoot(sad, "But me not say anything!")
                wartface(angry, "Shut up Grubfoot!")
                bentnoze(angry, "Shut up Wartface!")
                wartface(angry, "Shut up Bentnoze!")
                grubfoot(angry, "Shut up generals!")
                bentnoze(angry, "Shut up Grubfoot!")
                wartface(angry, "Shut up Grubfoot!")
                chatPlayer(angry, "Shut up goblins!")
            }
            1 -> {
                bentnoze(quiz, "What we do now Wartface?")
                wartface(confused, "Me dunno... maybe we change armour colour again.")
                bentnoze(quiz, "Human, what colour you think best?")
                chatPlayer(quiz, "What about brown?")
                wartface(neutral, "Yeah brown is nice.")
                bentnoze(happy, "Thanks human.")
            }
            2 -> {
                bentnoze(neutral, "Our soldiers need better weapons. We give them all warhammers!")
                wartface(angry, "No no give them battleaxes!")
                bentnoze(angry, "Axes stupid! They things you cut wood with!")
                wartface(
                    angry,
                    "But hammers things you put nails in wood with! At least axes sharp!",
                )
                bentnoze(
                    angry,
                    "You not need sharp when you have strong goblin swinging big lump of iron!",
                )
                chatPlayer(neutral, "Do you want me to...")
                wartface(angry, "Shut up human, we sort this one out ourselves!")
            }
            3 -> {
                wartface(neutral, "Me tell Mudknuckles make worm stew for dinner.")
                bentnoze(neutral, "Me not like worm stew! Tell him make beetle pie!")
                wartface(happy, "Beetle pie give you wind! You smell bad enough already!")
            }
            else -> {
                wartface(
                    neutral,
                    "Now you've solved our argument, we gotta think of something else to do.",
                )
                bentnoze(neutral, "Yep, we bored now.")
            }
        }
    }

    private suspend fun Dialogue.wartface(mesanim: MesAnimType, text: String) =
        chatNpcSpecific("General Wartface", WARTFACE, mesanim, text)

    private suspend fun Dialogue.bentnoze(mesanim: MesAnimType, text: String) =
        chatNpcSpecific("General Bentnoze", BENTNOZE, mesanim, text)

    private suspend fun Dialogue.grubfoot(
        mesanim: MesAnimType,
        text: String,
        head: String = grubfootHead(player),
    ) = chatNpcSpecific("Grubfoot", head, mesanim, text)

    private fun grubfootHead(player: Player): String =
        when (player.grubfootLook) {
            GRUBFOOT_ORANGE -> GRUBFOOT_ORANGE_HEAD
            GRUBFOOT_BLUE -> GRUBFOOT_BLUE_HEAD
            else -> GRUBFOOT_BROWN_HEAD
        }

    private companion object {
        private const val STARTED = 3
        private const val BLUE_WANTED = 4
        private const val BROWN_WANTED = 5
        private const val COMPLETE = 6

        private const val WHY_ARGUING = "Why are you arguing about the colour of your armour?"

        private const val GENERALS_HUT =
            "<red>General Wartface</red> and <red>General Bentnoze</red> in <red>Goblin Village</red>"
        private const val ASKED_FOR_ORANGE =
            "I spoke to $GENERALS_HUT. They've been trying to pick a new colour of armour for " +
                "the goblins in the village, but have been unable to agree on a colour. I offered " +
                "to help, so they asked me to bring them some <red>Orange Goblin Mail</red> to " +
                "try out."
        private const val WITCH =
            "Apparently there's a <red>Witch</red> in <red>Draynor Village</red> who makes " +
                "<red>Dye</red> that can be used to recolour <red>Goblin Mail</red>."
        private const val AGGIE =
            "<red>Aggie</red> in <red>Draynor Village</red> can make some <red>Dye</red> for me " +
                "to recolour <red>Goblin Mail</red> with."
        private const val CRATES =
            "I should be able to find some <red>Goblin Mail</red> in crates around <red>Goblin " +
                "Village</red>."
        private const val GAVE_ORANGE =
            "I gave some <red>Orange Goblin Mail</red> to <red>General Wartface</red> and " +
                "<red>General Bentnoze</red>, but they didn't like it. They asked me to bring " +
                "them some <red>Blue Goblin Mail</red> to try out instead."
        private const val GAVE_BLUE =
            "I gave some <red>Blue Goblin Mail</red> to <red>General Wartface</red> and " +
                "<red>General Bentnoze</red>, but they didn't like it. They asked me to bring " +
                "them some of their original <red>Brown Goblin Mail</red> to try out instead."
        private const val DONE_ASKED =
            "I spoke to General Wartface and General Bentnoze in Goblin Village. They were " +
                "trying to pick a new colour of armour for the goblins in the village, but were " +
                "unable to agree on a colour. I offered to help, so they asked me to bring them " +
                "some Orange Goblin Mail to try out."
        private const val DONE_GAVE_ORANGE =
            "I gave some Orange Goblin Mail to General Wartface and General Bentnoze, but they " +
                "didn't like it. They asked me to bring them some Blue Goblin Mail to try out " +
                "instead."

        private const val GRUBFOOT_BROWN = 0
        private const val GRUBFOOT_ORANGE = 1
        private const val GRUBFOOT_BLUE = 2
        private const val GRUBFOOT_BROWN_HEAD = "npc.catwalk_goblin_brown"
        private const val GRUBFOOT_ORANGE_HEAD = "npc.catwalk_goblin_orange"
        private const val GRUBFOOT_BLUE_HEAD = "npc.catwalk_goblin_blue"

        private val HUT =
            RegionTemplate.create {
                copy(HUT_ZONE_X, HUT_ZONE_Z, 0) {
                    regionZoneX = HUT_REGION_ZONE
                    regionZoneZ = HUT_REGION_ZONE
                    zoneWidth = HUT_ZONES
                    zoneLength = HUT_ZONES
                }
            }
        private const val HUT_ZONE_X = 368
        private const val HUT_ZONE_Z = 437
        private const val HUT_ZONES = 3
        private const val HUT_REGION_ZONE = 6
        private val PLAYER_TILE = CoordGrid(2958, 3512)
        private val WARTFACE_TILE = CoordGrid(2958, 3510)
        private val BENTNOZE_TILE = CoordGrid(2959, 3510)
        private val GRUBFOOT_TILE = CoordGrid(2955, 3511)
        private val CURTAIN_TILE = CoordGrid(2954, 3511)
        private val FETCH_TILE = CoordGrid(2957, 3514)
        private val FETCH_PATH =
            listOf(CoordGrid(2956, 3513), CoordGrid(2956, 3511), GRUBFOOT_TILE)
        private val CAMERA =
            SceneCamera(
                eye = CoordGrid(2961, 3511),
                eyeHeight = 400,
                lookAt = CoordGrid(2956, 3511),
                lookAtHeight = 150,
            )
        private const val CHANGE_TICKS = 2
        private const val GENERAL_SIGHT = 15
        private const val SCENE_LIFESPAN = 3000

        private const val WARTFACE = "npc.general_wartface_green"
        private const val BENTNOZE = "npc.general_bentnoze_red"
        private const val GRUBFOOT_MULTI = "npc.catwalk_goblin"
        private val GENERALS = listOf(BENTNOZE, WARTFACE)

        private const val GOBLIN_MAIL = "obj.goblin_armour"
        private const val ORANGE_MAIL = "obj.goblin_armour_orange"
        private const val BLUE_MAIL = "obj.goblin_armour_darkblue"
        private val ARMOURS =
            listOf(
                GOBLIN_MAIL,
                ORANGE_MAIL,
                BLUE_MAIL,
                "obj.goblin_armour_black",
                "obj.goblin_armour_green",
                "obj.goblin_armour_pink",
                "obj.goblin_armour_purple",
                "obj.goblin_armour_red",
                "obj.goblin_armour_white",
                "obj.goblin_armour_yellow",
            )

        private const val ORANGE_DYE = "obj.orangedye"
        private const val BLUE_DYE = "obj.bluedye"
        private const val RED_DYE = "obj.reddye"
        private const val YELLOW_DYE = "obj.yellowdye"
        private const val GREEN_DYE = "obj.greendye"
        private const val PURPLE_DYE = "obj.purpledye"
        private const val GOLD_BAR = "obj.gold_bar"
        private const val CRAFTING_XP = 200.0
        private const val NO_ROOM_FOR_REWARD = "You don't have enough room for the reward."

        private fun wantedArmour(stage: Int): String? =
            when (stage) {
                STARTED -> ORANGE_MAIL
                BLUE_WANTED -> BLUE_MAIL
                BROWN_WANTED -> GOBLIN_MAIL
                else -> null
            }

        private fun wantedColour(stage: Int): String? =
            when (stage) {
                STARTED -> "orange"
                BLUE_WANTED -> "blue"
                BROWN_WANTED -> "brown"
                else -> null
            }
    }
}
