package org.rsmod.content.quest.area.varlamore.perilousmoons

import dev.openrune.types.MoveRestrict
import jakarta.inject.Inject
import kotlin.math.abs
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpcU
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.shops.Shops
import org.rsmod.content.quest.area.lumbridge.xmarksthespot.owns
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.menu
import org.rsmod.content.quest.manager.rewards
import org.rsmod.content.quest.manager.startQuestPrompt
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class PerilousMoonsQuest
@Inject
constructor(
    private val ruins: PerilousMoonsRuins,
    private val scenes: PerilousMoonsScenes,
    private val npcRepo: NpcRepository,
    private val shops: Shops,
) :
    QuestScript(
        "quest_perilousmoons",
        "varp.pmoon_main",
        rewards {
            extra("Access to Neypotzli")
            extra("40,000 Slayer XP")
            extra("5,000 Runecraft XP")
            extra("5,000 Hunter XP")
            extra("5,000 Fishing XP")
        },
        ItemRewardDisplay(PmoonObj.ICOSAHEDRON, zoom = 400),
        questVarbit = "varbit.pmoon_quest",
        completeJingle = "jingle.quest_complete_2",
    ) {

    override fun ScriptContext.init() {
        check(quest.maxSteps == PmoonStage.COMPLETE) {
            "Perilous Moons end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the script completes at ${PmoonStage.COMPLETE}."
        }

        for (npc in TALKERS) {
            onOpNpc1(npc) { talkTo(it.npc) }
        }
        onOpNpc1(PmoonNpc.NAHTA) { talkToNahta(it.npc, withTalisman = false) }
        onOpNpc3(PmoonNpc.NAHTA) { shops.open(player, it.npc) }
        onOpNpcU(PmoonNpc.NAHTA) { talkToNahta(it.npc, withTalisman = true) }
        onOpNpc1(PmoonNpc.BLACKSMITH) { talkToBlacksmith(it.npc, withTalisman = false) }
        onOpNpc3(PmoonNpc.BLACKSMITH) { shops.open(player, it.npc) }
        onOpNpcU(PmoonNpc.BLACKSMITH) { talkToBlacksmith(it.npc, withTalisman = true) }

        onOpLoc1("loc.dungeonkit_imcando02_door01_2x1") { squeezeThrough(it.loc) }

        onOpHeld1(PmoonObj.INFUSED_EARTH) { locate(earth = true) }
        onOpHeld1(PmoonObj.INFUSED_WATER) { locate(earth = false) }

        for (crates in SUPPLY_CRATES) {
            onOpLoc1(crates) { with(ruins) { takeSupplies() } }
        }
        onOpLoc1("loc.pmoon_campbox") { with(ruins) { buildCamp() } }

        onEvent<SessionStateEvent.PrepareLogin> { ruins.restoreLogin(player) }
        onPlayerLogout { ruins.leave(player) }
    }

    override fun subTitle(): String =
        "speaking to <col=800000>Attala</col> at the gates of <col=800000>Cam Torum</col>, in " +
            "the mountains of <col=800000>Varlamore</col>."

    override fun questLog(player: ProtectedAccess) =
        questJournal(player) {
            description(
                "A dig beneath the dwarven city of <red>Cam Torum</red> has broken into ruins " +
                    "far older than the city itself, and not everyone agrees on what should be " +
                    "done with them."
            )
            for (step in JOURNAL) {
                objective(step.text) {
                    visibleWhen { access.player.pmoonStage in step.from..step.until }
                }
            }
            objective(
                "I have built <red>${player.player.pmoonCampsBuilt}</red> of the <red>3</red> " +
                    "camps."
            ) {
                visibleWhen { access.player.pmoonStage == PmoonStage.BUILD_CAMPS }
            }
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line(
                "I dealt with a creature that escaped from the ruins beneath Cam Torum, and " +
                    "Attala let me into the city in return."
            )
            line(
                "I set up camps in the ruins for Jessamine's dig and followed a pair of " +
                    "talismans to Eyatlalli, the nagua that guards the place. It is Neypotzli, " +
                    "a prison for the Moons of Peril."
            )
            line(
                "Zuma disturbed the seal that holds the Moons. I gathered what Eyatlalli needed " +
                    "for a ritual and kept all three Moons busy while she repaired it."
            )
            line("Eyatlalli has asked me to keep distracting the Moons, and will reward me for it.")
        }

    private suspend fun ProtectedAccess.talkTo(npc: Npc) {
        when {
            npc.isType(PmoonNpc.ATTALA_GATES) || npc.isType(PmoonNpc.ZUMA_GATES) -> talkAtGate(npc)
            npc.isType(PmoonNpc.BUILDERS_GATES) ->
                startDialogue(npc) {
                    if (player.pmoonStage >= PmoonStage.CITY_OPEN) buildersHeadInside()
                    else buildersAtGate()
                }
            npc.isType(PmoonNpc.ATTALA_CITY) -> startDialogue(npc) { attalaInCity() }
            npc.isType(PmoonNpc.ZUMA_CITY) -> startDialogue(npc) { zumaInCity() }
            npc.isType(PmoonNpc.JESSAMINE_CITY) -> startDialogue(npc) { jessamineInCity() }
            npc.isType(PmoonNpc.ATTALA_RUINS) -> talkToAttala(npc)
            npc.isType(PmoonNpc.ZUMA_RUINS) -> talkToZuma(npc)
            npc.isType(PmoonNpc.JESSAMINE_RUINS) -> talkToJessamine(npc)
            npc.isType(PmoonNpc.BUILDERS_RUINS) -> talkToBuilders(npc)
            npc.isType(PmoonNpc.EYATLALLI) -> talkToEyatlalli(npc)
        }
    }

    private suspend fun ProtectedAccess.talkAtGate(npc: Npc) {
        when (player.pmoonStage) {
            PmoonStage.NOT_STARTED -> startQuest(npc)
            PmoonStage.KILL_NAGUA -> startDialogue(npc) { naguaReminder() }
            PmoonStage.NAGUA_KILLED -> {
                startDialogue(npc) { naguaReport() }
                fade(out = true)
                player.pmoonStage = PmoonStage.CITY_OPEN
                fade(out = false)
                startDialogue { buildersAfterZumaLeaves() }
            }
        }
    }

    private suspend fun ProtectedAccess.startQuest(npc: Npc) {
        startDialogue(npc) {
            gateArgument()
            if (!meetsRequirements(player)) {
                gateTurnedAway()
                access.mes(
                    "You do not meet all of the requirements to start the Perilous Moons quest."
                )
                return@startDialogue
            }
            val messenger = Npc(PmoonNpc.GUARD, GUARD_START)
            npcRepo.add(messenger, GUARD_LIFESPAN)
            messenger.noneMode()
            messenger.moveRestrict = MoveRestrict.PassThru
            messenger.walk(GUARD_STOP)
            try {
                guardReportOpening()
                guardReport()
            } finally {
                if (messenger.isSlotAssigned) npcRepo.del(messenger, Int.MAX_VALUE)
            }
            attalaOffer()
            if (!startQuestPrompt(quest)) {
                chatPlayer(angry, "On second thoughts, I don't like your attitude.")
                attala(angry, "Then go away!")
                return@startDialogue
            }
            quest.setQuestStage(access, PmoonStage.KILL_NAGUA)
            chatPlayer(happy, "Access to the city sounds nice.")
            attala(angry, "Fine, get to it!")
        }
    }

    private fun meetsRequirements(player: Player): Boolean =
        STAT_REQUIREMENTS.all { (stat, level) -> player.statBase(stat) >= level } &&
            QUEST_REQUIREMENTS.all { QuestRequirements.hasCompleted(player, it) }

    private suspend fun ProtectedAccess.talkToAttala(npc: Npc) {
        val stage = player.pmoonStage
        when {
            stage == PmoonStage.MURALS && player.pmoonMurals == 0 -> {
                startDialogue(npc) { attalaAtMural() }
                ruins.attalaJoinsZuma(player)
            }
            stage == PmoonStage.MURALS ->
                startDialogue(npc) { attala(angry, "Now where is that fool priest?") }
            stage < PmoonStage.CAMPS_BUILT -> startDialogue(npc) { campsReminderPair() }
            stage < PmoonStage.TALISMANS -> reportCamps(npc)
            stage == PmoonStage.TALISMANS ->
                startDialogue(npc) { attalaSendsToNahta(lostTalismans = !holdsAll(PLAIN)) }
            stage == PmoonStage.ENCHANTED -> startDialogue(npc) { attalaSendsBackToCity() }
            stage <= PmoonStage.EYATLALLI_FOUND -> startDialogue(npc) { attalaTalismanBriefing() }
            stage < PmoonStage.GATHER -> meetEyatlalli(npc)
            stage >= PmoonStage.FIGHT_MOONS && player.pmoonMoonsSubdued ->
                startDialogue(npc) { attalaFinish() }
            else -> startDialogue(npc) { attalaApology() }
        }
    }

    private suspend fun ProtectedAccess.talkToZuma(npc: Npc) {
        val stage = player.pmoonStage
        when {
            stage == PmoonStage.MURALS && player.pmoonMurals == 0 ->
                startDialogue(npc) {
                    zuma(confused, "The moon blotting out the sun, could this be...")
                }
            stage == PmoonStage.MURALS && player.pmoonMurals == 1 -> {
                startDialogue(npc) { zumaAtMural() }
                ruins.pairJoinsJessamine(player)
            }
            stage == PmoonStage.MURALS -> startDialogue(npc) { zuma(shocked, "Incredible...") }
            stage < PmoonStage.CAMPS_BUILT -> startDialogue(npc) { campsReminderPair() }
            stage < PmoonStage.TALISMANS -> reportCamps(npc)
            stage <= PmoonStage.EYATLALLI_FOUND -> startDialogue(npc) { zumaWaiting() }
            stage >= PmoonStage.ZUMA_DONE -> startDialogue(npc) { zumaFinishAgain() }
            player.pmoonMoonsSubdued -> startDialogue(npc) { zumaFinish() }
            else -> startDialogue(npc) { zuma(sad, "I am a fool...") }
        }
    }

    private suspend fun ProtectedAccess.talkToJessamine(npc: Npc) {
        val stage = player.pmoonStage
        when {
            stage == PmoonStage.MURALS && player.pmoonMurals < 2 ->
                startDialogue(npc) {
                    jessamine(confused, "Xali.... Ates... Hmm, no, not quite right...")
                }
            stage == PmoonStage.MURALS ->
                startDialogue(npc) {
                    jessamineAtMural(QuestRequirements.hasCompleted(player, GARDEN_OF_DEATH))
                }
            stage < PmoonStage.CAMPS_BUILT -> startDialogue(npc) { campsReminderJessamine() }
            stage < PmoonStage.TALISMANS -> reportCamps(npc)
            stage <= PmoonStage.EYATLALLI_FOUND -> startDialogue(npc) { jessamineWaiting() }
            stage < PmoonStage.GATHER -> meetEyatlalli(npc)
            stage == PmoonStage.COMPLETE -> startDialogue(npc) { jessaminePostQuest() }
            stage >= PmoonStage.FIGHT_MOONS && player.pmoonMoonsSubdued ->
                startDialogue(npc) { jessamineFinish() }
            else -> startDialogue(npc) { jessamineDilemma() }
        }
    }

    private suspend fun ProtectedAccess.talkToBuilders(npc: Npc) {
        val stage = player.pmoonStage
        startDialogue(npc) {
            when {
                stage == PmoonStage.COMPLETE -> buildersPostQuest()
                stage >= PmoonStage.ZUMA_DONE -> buildersFinishAgain()
                stage >= PmoonStage.FIGHT_MOONS && player.pmoonMoonsSubdued -> zumaFinish()
                else -> buildersInRuins()
            }
        }
    }

    private suspend fun ProtectedAccess.talkToEyatlalli(npc: Npc) {
        val stage = player.pmoonStage
        when {
            stage < PmoonStage.GATHER -> meetEyatlalli(npc)
            stage < PmoonStage.RITUAL_STARTED -> ritual(npc)
            stage == PmoonStage.ZUMA_DONE && player.pmoonMoonsSubdued -> finish(npc)
            else -> startDialogue(npc) { moonsBriefing(doneOneSmallFavour()) }
        }
    }

    private suspend fun ProtectedAccess.reportCamps(npc: Npc) {
        startDialogue(npc) {
            if (player.pmoonStage < PmoonStage.TALISMANS_SHOWN && !campsReport()) {
                return@startDialogue
            }
            if (access.inv.freeSpace() < PLAIN.size) {
                makeSpaceForTalismans()
                return@startDialogue
            }
            for (talisman in PLAIN) access.invAdd(access.inv, talisman)
            player.pmoonStage = PmoonStage.TALISMANS
            mesbox("Jessamine hands you the talismans.")
        }
    }

    private suspend fun ProtectedAccess.meetEyatlalli(npc: Npc) {
        if (player.pmoonStage < PmoonStage.ANTECHAMBER_SHAKEN) {
            with(scenes) { shake() }
        }
        startDialogue(npc) { antechamberShaken() }
    }

    private suspend fun ProtectedAccess.ritual(npc: Npc) {
        startDialogue(npc) {
            val stage = player.pmoonStage
            val owed = RITUAL_ITEMS.filter { it.placedAt > stage }
            if (owed.any { access.inv.count(it.obj) == 0 }) {
                gatherReminder(listed(owed.map { it.label }))
                return@startDialogue
            }
            if (stage == PmoonStage.GATHER) {
                chatPlayer(neutral, "I have the items.")
                eyatlalli(neutral, "Good. Arrange them as follows:")
            }
            for (item in owed) {
                eyatlalli(neutral, item.instruction)
                if (access.invDel(access.inv, item.obj, 1).failure) return@startDialogue
                player.pmoonStage = item.placedAt
                objbox(item.obj, item.placed)
            }
            player.pmoonStage = PmoonStage.RITUAL_STARTED
            with(scenes) { access.shake() }
            moonsBriefing(doneOneSmallFavour())
        }
    }

    private suspend fun ProtectedAccess.finish(npc: Npc) {
        startDialogue(npc) { eyatlalliFinish() }
        quest.completeQuest(this) {
            player.pmoonEyatlalliVis = true
            for ((stat, xp) in XP_REWARDS) statAdvance(stat, xp)
        }
    }

    private fun Dialogue.doneOneSmallFavour(): Boolean =
        QuestRequirements.hasCompleted(player, ONE_SMALL_FAVOUR)

    private enum class NahtaTopic {
        Runes,
        Source,
        Attala,
        No,
    }

    private suspend fun ProtectedAccess.talkToNahta(npc: Npc, withTalisman: Boolean) {
        var trade = false
        startDialogue(npc) {
            val stage = player.pmoonStage
            nahta(quiz, "Are you in need of some runes?")
            if (stage == PmoonStage.ENCHANTED && reclaim(ENCHANTED_SET)) {
                return@startDialogue
            }
            if (withTalisman && stage == PmoonStage.TALISMANS) {
                enchantTalismans()
                return@startDialogue
            }
            while (true) {
                val options = buildList {
                    add("Yes please." to NahtaTopic.Runes)
                    add("Where do you get your runes from?" to NahtaTopic.Source)
                    if (stage in PmoonStage.TALISMANS until PmoonStage.COMPLETE) {
                        add("Actually, I've been sent by Attala." to NahtaTopic.Attala)
                    }
                    add("No thanks." to NahtaTopic.No)
                }
                when (menu(options)) {
                    NahtaTopic.Runes -> {
                        chatPlayer(happy, "Yes please.")
                        trade = true
                        return@startDialogue
                    }
                    NahtaTopic.Source -> runeSource()
                    NahtaTopic.Attala -> {
                        if (stage == PmoonStage.TALISMANS) {
                            enchantTalismans()
                        } else {
                            chatPlayer(neutral, "Actually, I've been sent by Attala.")
                            nahta(
                                neutral,
                                "And I already attuned your talismans. Take them to the " +
                                    "blacksmith. Tell him they've been enchanted to react to " +
                                    "naguanil magic and they need some finishing touches.",
                            )
                            aboutNaguanil()
                        }
                        return@startDialogue
                    }
                    NahtaTopic.No -> {
                        chatPlayer(neutral, "No thanks.")
                        nahta(neutral, "Then we have nothing more to discuss.")
                        return@startDialogue
                    }
                }
            }
        }
        if (trade) shops.open(player, npc)
    }

    private suspend fun Dialogue.runeSource() {
        chatPlayer(quiz, "Where do you get your runes from?")
        nahta(angry, "That is not the kind of information I intend to give to a human.")
        chatPlayer(quiz, "Why?")
        nahta(
            neutral,
            "Because before the Varlamorians decided to open their lands up to visitors, Cam " +
                "Torum was the sole supplier of runes to the entire region.",
        )
        chatPlayer(
            neutral,
            "Okay... But that's not the case anymore. With the kingdom open, they can just get " +
                "their runes from the mainland.",
        )
        nahta(
            angry,
            "A fact I'm all too aware of, especially given the impact it is having on my prices.",
        )
        nahta(
            neutral,
            "Nevertheless, there is still a benefit to having a source of runes closer to home, " +
                "and I do not intend to share any information on that source.",
        )
        chatPlayer(neutral, "Fair enough.")
        nahta(quiz, "Now, are you in need of some runes?")
    }

    private suspend fun Dialogue.enchantTalismans() {
        chatPlayer(neutral, "Actually, I've been sent by Attala.")
        nahta(quiz, "Attala? Why's that?")
        chatPlayer(
            neutral,
            "We're trying to find out more about a source of magic in the ruins beneath the " +
                "city. She asked me to bring some talismans to you.",
        )
        nahta(
            neutral,
            "If she needs me to get involved, then no doubt this is something well beyond your " +
                "people and probably highly dangerous.",
        )
        nahta(neutral, "But I suppose if she's okay with that... Go on then, let's have a look.")
        if (!access.holdsAll(PLAIN)) {
            chatPlayer(shifty, "Er... I seem to have misplaced them.")
            nahta(angry, "Then you'd better go and find them.")
            return
        }
        mesbox("You show the talismans to Nahta.")
        nahta(angry, "How in the... Where did you get these?")
        nahta(confused, "Actually, on second thoughts, I'm not sure I want to know.")
        if (!access.swap(PLAIN, ENCHANTED_SET)) return
        player.pmoonStage = PmoonStage.ENCHANTED
        mesbox("Nahta casts a spell on the talismans.")
        nahta(
            neutral,
            "There. Now take them to the blacksmith. Tell him they've been enchanted to react " +
                "to naguanil magic and they need some finishing touches.",
        )
        aboutNaguanil()
    }

    private suspend fun Dialogue.aboutNaguanil() {
        val asked = choice2("Naguanil? What's that?", true, "Okay, thanks for the help.", false)
        if (!asked) {
            chatPlayer(neutral, "Okay, thanks for the help.")
            return
        }
        chatPlayer(quiz, "Naguanil? What's that?")
        nahta(
            neutral,
            "A type of magic your people have forgotten, alongside many other things. I don't " +
                "have time to catch you up on the history of the word though, so you'll need to " +
                "learn more somewhere else.",
        )
    }

    private enum class SmithTopic {
        Wares,
        Talismans,
        Leave,
    }

    private suspend fun ProtectedAccess.talkToBlacksmith(npc: Npc, withTalisman: Boolean) {
        var trade = false
        startDialogue(npc) {
            val stage = player.pmoonStage
            blacksmith(angry, "A human? Either buy something or get out!")
            if (stage in PmoonStage.INFUSED..PmoonStage.EYATLALLI_FOUND && reclaim(INFUSED_SET)) {
                return@startDialogue
            }
            if (withTalisman && stage == PmoonStage.ENCHANTED) {
                infuseTalismans()
                return@startDialogue
            }
            val options = buildList {
                add("What do you have?" to SmithTopic.Wares)
                if (stage in PmoonStage.ENCHANTED until PmoonStage.COMPLETE) {
                    add("I need your help with these talismans." to SmithTopic.Talismans)
                }
                add("I'll get going." to SmithTopic.Leave)
            }
            when (menu(options)) {
                SmithTopic.Wares -> {
                    chatPlayer(quiz, "What do you have?")
                    blacksmith(angry, "In a blacksmith? What do you think? Weapons!")
                    trade = true
                }
                SmithTopic.Talismans ->
                    if (stage == PmoonStage.ENCHANTED) {
                        infuseTalismans()
                    } else {
                        chatPlayer(neutral, "I need your help with these talismans.")
                        blacksmith(
                            angry,
                            "I already told you to take them back to Attala. She'll tell you " +
                                "what to do with them.",
                        )
                        chatPlayer(neutral, "Okay, thanks.")
                    }
                SmithTopic.Leave -> {
                    chatPlayer(neutral, "I'll get going.")
                    blacksmith(angry, "Good.")
                }
            }
        }
        if (trade) shops.open(player, npc)
    }

    private suspend fun Dialogue.infuseTalismans() {
        chatPlayer(neutral, "I need your help with these talismans.")
        blacksmith(
            angry,
            "I'm sure you could use my help with many things, human, but I'm very busy.",
        )
        chatPlayer(neutral, "I was sent by Attala.")
        blacksmith(quiz, "Attala? What's she got to do with you?")
        chatPlayer(
            neutral,
            "We're investigating whether the ruins beneath the city pose any threat to its " +
                "citizens.",
        )
        blacksmith(
            quiz,
            "Oh... Well I suppose I can help then, as long as you make it quick. What did you " +
                "say you needed?",
        )
        chatPlayer(
            quiz,
            "The owner of the magic shop said something about these talismans detecting... " +
                "aguatil?",
        )
        blacksmith(angry, "Naguanil. Hmm...")
        val doubtful =
            choice2(
                "Can you help?",
                false,
                "What do you know about magic? Aren't you a blacksmith?",
                true,
            )
        if (doubtful) {
            chatPlayer(quiz, "What do you know about magic? Aren't you a blacksmith?")
            blacksmith(
                angry,
                "I am one of the Dwarves of Cam Torum, human! Our metalwork and our magic are " +
                    "one. I am an expert in both.",
            )
            chatPlayer(
                happy,
                "That sounds interesting. Could you teach me how to mix smithing and magic?",
            )
            blacksmith(laugh, "Hah, that's a good joke!")
            chatPlayer(sad, "Oh... But can you help with the talismans?")
        } else {
            chatPlayer(quiz, "Can you help?")
        }
        if (!access.holdsAll(ENCHANTED_SET)) {
            blacksmith(
                neutral,
                "Of course. This sort of magic is child's play. Where are the talismans?",
            )
            chatPlayer(shifty, "Er... I seem to have lost them.")
            blacksmith(angry, "Then stop wasting my time!")
            return
        }
        blacksmith(neutral, "Of course. This sort of magic is child's play. Give me a moment...")
        scenes.blacksmith(access)
        if (!access.swap(ENCHANTED_SET, INFUSED_SET)) return
        player.pmoonStage = PmoonStage.INFUSED
        mesbox("The blacksmith hands back the talismans.")
        blacksmith(
            neutral,
            "All done. Take these back to Attala. She'll tell you what to do with them.",
        )
        blacksmith(angry, "Now, get out!")
    }

    /**
     * Replaces the talismans of [set] the player no longer owns anywhere. Returns `false`, having
     * said nothing, when none are missing.
     */
    private suspend fun Dialogue.reclaim(set: TalismanSet): Boolean {
        val lost = set.talismans.filter { !player.owns(it.obj) }
        if (lost.isEmpty()) return false
        if (lost.size == 1) {
            chatPlayer(sad, "I think I've lost one of the talismans you gave me.")
        } else {
            chatPlayer(sad, set.lostBoth)
        }
        if (set === ENCHANTED_SET) {
            nahta(neutral, "Well luckily for you, I expected this might happen.")
        } else {
            blacksmith(angry, "Humans! You're lucky I prepared for this!")
        }
        if (access.inv.freeSpace() < lost.size) {
            if (set === ENCHANTED_SET) {
                nahta(neutral, "Make some room and I'll get you sorted out.")
            } else {
                blacksmith(angry, "Make some room if you want my help!")
            }
            return true
        }
        for (talisman in lost) access.invAdd(access.inv, talisman.obj)
        mesbox(if (lost.size == 1) "${set.giver} hands you a new ${lost.single().label}." else set.newPair)
        return true
    }

    private suspend fun ProtectedAccess.locate(earth: Boolean) {
        val stage = player.pmoonStage
        if (stage !in PmoonStage.INFUSED..PmoonStage.EYATLALLI_FOUND) {
            mes("Nothing interesting happens.")
            return
        }
        val target = TALISMAN_TARGET
        val dx = target.x - coords.x
        val dz = target.z - coords.z
        val atTarget = coords.level == target.level && abs(dx) <= TARGET_RANGE && abs(dz) <= TARGET_RANGE
        if (!atTarget) {
            val northSouth = if (dz > 0) "north" else "south"
            val eastWest = if (dx > 0) "east" else "west"
            val pull =
                when {
                    earth -> if (dz != 0) northSouth else eastWest
                    else -> if (dx != 0) eastWest else northSouth
                }
            mes("The talisman pulls you $pull.")
            return
        }
        val first = stage == PmoonStage.INFUSED
        if (first) {
            player.pmoonStage = PmoonStage.EYATLALLI_FOUND
            player.pmoonTalismanReturn = true
        }
        scenes.eyatlalliAppears(this, target, thrown = first)
        if (player.pmoonStage == PmoonStage.EYATLALLI_GONE) {
            for (talisman in INFUSED_SET.talismans) {
                if (inv.count(talisman.obj) > 0) invDel(inv, talisman.obj, 1)
            }
        }
    }

    private suspend fun ProtectedAccess.squeezeThrough(door: BoundLocInfo) {
        arriveDelay()
        val crossesX = coords.x < door.x || coords.x >= door.x + door.adjustedWidth
        val dest =
            when {
                crossesX && coords.x < door.x -> coords.translateX(door.adjustedWidth + 1)
                crossesX -> coords.translateX(-(door.adjustedWidth + 1))
                coords.z < door.z -> coords.translateZ(door.adjustedLength + 1)
                else -> coords.translateZ(-(door.adjustedLength + 1))
            }
        faceSquare(dest)
        delay(1)
        anim("seq.surok_player_climbing_into_tunnel")
        soundSynth("synth.climb_under")
        delay(1)
        teleport(dest)
        spam("You manage to squeeze through the tiny door.")
    }

    private suspend fun ProtectedAccess.fade(out: Boolean) {
        if (out) {
            fadeOverlay(0, 255, 0, 0, FADE_DURATION)
            delay(FADE_TICKS)
        } else {
            fadeOverlay(0, 0, 0, 255, FADE_DURATION)
            delay(FADE_TICKS)
            closeFadeOverlayNow()
        }
    }

    private fun ProtectedAccess.holdsAll(objs: List<String>): Boolean =
        objs.all { inv.count(it) > 0 }

    private fun ProtectedAccess.holdsAll(set: TalismanSet): Boolean =
        holdsAll(set.talismans.map { it.obj })

    private fun ProtectedAccess.swap(from: List<String>, into: TalismanSet): Boolean {
        if (!holdsAll(from)) return false
        for (obj in from) {
            if (invDel(inv, obj, 1).failure) return false
        }
        for (talisman in into.talismans) invAdd(inv, talisman.obj)
        return true
    }

    private fun ProtectedAccess.swap(from: TalismanSet, into: TalismanSet): Boolean =
        swap(from.talismans.map { it.obj }, into)

    private fun listed(names: List<String>): String =
        when (names.size) {
            1 -> names[0]
            2 -> "${names[0]} and ${names[1]}"
            else -> "${names.dropLast(1).joinToString(", ")} and ${names.last()}"
        }

    private class Talisman(val obj: String, val label: String)

    private class TalismanSet(
        val giver: String,
        val talismans: List<Talisman>,
        val lostBoth: String,
        val newPair: String,
    )

    private class RitualItem(
        val obj: String,
        val label: String,
        val placedAt: Int,
        val instruction: String,
        val placed: String,
    )

    private class JournalStep(val from: Int, val until: Int, val text: String)

    private companion object {
        private const val GARDEN_OF_DEATH = "quest_gardenofdeath"
        private const val ONE_SMALL_FAVOUR = "quest_onesmallfavour"
        private const val FADE_DURATION = 50
        private const val FADE_TICKS = 2
        private const val GUARD_LIFESPAN = 200
        private const val TARGET_RANGE = 2

        private val GUARD_START = CoordGrid(1436, 3128)
        private val GUARD_STOP = CoordGrid(1436, 3125)

        private val STAT_REQUIREMENTS =
            listOf(
                "stat.slayer" to 48,
                "stat.hunter" to 20,
                "stat.fishing" to 20,
                "stat.runecrafting" to 20,
                "stat.construction" to 10,
            )
        private val QUEST_REQUIREMENTS = listOf("quest_twilightspromise", "quest_childrenofthesun")
        private val XP_REWARDS =
            listOf(
                "stat.slayer" to 40_000.0,
                "stat.runecrafting" to 5_000.0,
                "stat.hunter" to 5_000.0,
                "stat.fishing" to 5_000.0,
            )

        private val TALKERS =
            listOf(
                PmoonNpc.ATTALA_GATES,
                PmoonNpc.ZUMA_GATES,
                PmoonNpc.BUILDERS_GATES,
                PmoonNpc.ATTALA_CITY,
                PmoonNpc.ZUMA_CITY,
                PmoonNpc.JESSAMINE_CITY,
                PmoonNpc.ATTALA_RUINS,
                PmoonNpc.ZUMA_RUINS,
                PmoonNpc.JESSAMINE_RUINS,
                PmoonNpc.BUILDERS_RUINS,
                PmoonNpc.EYATLALLI,
            )

        private val SUPPLY_CRATES =
            listOf(
                "loc.pmoon_camp_supplies_1",
                "loc.pmoon_camp_supplies_2",
                "loc.pmoon_camp_supplies_3",
            )

        private val PLAIN = listOf(PmoonObj.EARTH_TALISMAN, PmoonObj.WATER_TALISMAN)
        private val ENCHANTED_SET =
            TalismanSet(
                giver = "Nahta",
                talismans =
                    listOf(
                        Talisman(PmoonObj.ENCHANTED_EARTH, "enchanted earth talisman"),
                        Talisman(PmoonObj.ENCHANTED_WATER, "enchanted water talisman"),
                    ),
                lostBoth = "I think I've lost the talismans you gave me.",
                newPair = "Nahta hands you some new enchanted talismans.",
            )
        private val INFUSED_SET =
            TalismanSet(
                giver = "The blacksmith",
                talismans =
                    listOf(
                        Talisman(PmoonObj.INFUSED_EARTH, "infused earth talisman"),
                        Talisman(PmoonObj.INFUSED_WATER, "infused water talisman"),
                    ),
                lostBoth = "I've lost the talismans you gave me.",
                newPair = "The blacksmith hands you some new infused talismans.",
            )

        // UNVERIFIED: the real spot differs per player and how it is picked is unknown; this is the
        // only one ever seen, so every player gets it.
        private val TALISMAN_TARGET = CoordGrid(1484, 9693)

        private val RITUAL_ITEMS =
            listOf(
                RitualItem(
                    PmoonObj.SCALES,
                    "bream scales",
                    PmoonStage.SCALES_PLACED,
                    "Place the scales in the shape of the sun at high noon.",
                    "You place the scales in the shape of the sun.",
                ),
                RitualItem(
                    PmoonObj.LIZARD_TAIL,
                    "moss lizard tail",
                    PmoonStage.TAIL_PLACED,
                    "Place the lizard tail beneath it as the earth.",
                    "You place the lizard tail beneath the sun.",
                ),
                RitualItem(
                    PmoonObj.GRUB_PASTE,
                    "moonlight grub paste",
                    PmoonStage.PASTE_PLACED,
                    "Now carve the earth in two with the paste, as the stream carves the earth.",
                    "You pull the tail apart and draw a line through it with the paste.",
                ),
            )

        private val JOURNAL =
            listOf(
                JournalStep(
                    PmoonStage.KILL_NAGUA,
                    PmoonStage.KILL_NAGUA,
                    "<red>Attala</red> will let me into <red>Cam Torum</red> if I deal with a " +
                        "creature that escaped the ruins. It went through the mountain's " +
                        "<red>outflow spring</red>, north east of the gates.",
                ),
                JournalStep(
                    PmoonStage.NAGUA_KILLED,
                    PmoonStage.NAGUA_KILLED,
                    "I killed the creature. I should tell <red>Attala</red> at the gates of " +
                        "<red>Cam Torum</red>.",
                ),
                JournalStep(
                    PmoonStage.CITY_OPEN,
                    PmoonStage.IN_CITY,
                    "I can now enter <red>Cam Torum</red>. The builders say I should find " +
                        "<red>Jessamine</red>, the archaeologist, in the north of the city.",
                ),
                JournalStep(
                    PmoonStage.RUINS_OPEN,
                    PmoonStage.RUINS_OPEN,
                    "<red>Jessamine</red> wants help with her dig. I should meet her inside the " +
                        "<red>ruins</red>, through the entrance in the north of Cam Torum.",
                ),
                JournalStep(
                    PmoonStage.MURALS,
                    PmoonStage.MURALS,
                    "I should see what <red>Attala</red>, <red>Zuma</red> and " +
                        "<red>Jessamine</red> have found in the ruins.",
                ),
                JournalStep(
                    PmoonStage.BUILD_CAMPS,
                    PmoonStage.BUILD_CAMPS,
                    "<red>Jessamine</red> has asked me to take <red>building supplies</red> " +
                        "from the crates beside her and set up a <red>camp</red> in each of the " +
                        "three caverns around the ruins.",
                ),
                JournalStep(
                    PmoonStage.CAMPS_BUILT,
                    PmoonStage.TALISMANS_SHOWN,
                    "All three camps are built. I should tell <red>Jessamine</red>.",
                ),
                JournalStep(
                    PmoonStage.TALISMANS,
                    PmoonStage.TALISMANS,
                    "Jessamine found an <red>earth talisman</red> and a <red>water " +
                        "talisman</red>. Attala wants me to take them to <red>Nahta</red> in " +
                        "Cam Torum's magic shop.",
                ),
                JournalStep(
                    PmoonStage.ENCHANTED,
                    PmoonStage.ENCHANTED,
                    "Nahta enchanted the talismans. I should take them to the " +
                        "<red>blacksmith</red> in Cam Torum for the finishing touches.",
                ),
                JournalStep(
                    PmoonStage.INFUSED,
                    PmoonStage.INFUSED,
                    "The talismans are infused. They should <red>pull</red> me towards the " +
                        "source of the magic somewhere in the ruins. <red>Attala</red> can tell " +
                        "me more.",
                ),
                JournalStep(
                    PmoonStage.EYATLALLI_FOUND,
                    PmoonStage.ZUMA_BLAMED_AGAIN,
                    "The talismans led me to <red>Eyatlalli</red>, a nagua who guards these " +
                        "ruins. Something shook the place and she vanished. I should speak to " +
                        "<red>Jessamine</red>.",
                ),
                JournalStep(
                    PmoonStage.GATHER,
                    PmoonStage.PASTE_PLACED,
                    "Zuma has disturbed the seal on the Moons of Peril. <red>Eyatlalli</red> " +
                        "needs <red>bream scales</red>, a <red>moss lizard tail</red> and " +
                        "<red>moonlight grub paste</red> from the caverns for a ritual.",
                ),
                JournalStep(
                    PmoonStage.RITUAL_STARTED,
                    PmoonStage.RITUAL_STARTED,
                    "The ritual has begun. I should hear what <red>Eyatlalli</red> needs next.",
                ),
                JournalStep(
                    PmoonStage.FIGHT_MOONS,
                    PmoonStage.ECLIPSE_SEEN,
                    "I need to distract the <red>Blood Moon</red>, the <red>Blue Moon</red> and " +
                        "the <red>Eclipse Moon</red> in their chambers while Eyatlalli repairs " +
                        "the seal, then return to the others.",
                ),
                JournalStep(
                    PmoonStage.JESSAMINE_DONE,
                    PmoonStage.JESSAMINE_DONE,
                    "The Moons are distracted. I should speak to <red>Zuma</red>.",
                ),
                JournalStep(
                    PmoonStage.ZUMA_DONE,
                    PmoonStage.ZUMA_DONE,
                    "I should speak to <red>Eyatlalli</red>.",
                ),
            )
    }
}

class PerilousMoonsNaguaKill
@Inject
constructor(private val access: ProtectedAccessLauncher) : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        if (!context.npc.isType(PmoonNpc.NAGUA)) return
        val player = context.hero
        if (player.pmoonStage != PmoonStage.KILL_NAGUA) return
        player.pmoonStage = PmoonStage.NAGUA_KILLED
        access.launch(player) {
            startDialogue {
                chatPlayer(
                    neutral,
                    "I should head back to Attala and let her know that the aqueduct is secure.",
                )
            }
        }
    }
}
