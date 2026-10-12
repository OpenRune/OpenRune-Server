package org.rsmod.content.quest.area.draynor

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestProgressState
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

class ErnestTheChicken
@Inject
constructor(
    private val worldRepo: WorldRepository,
    private val npcRepo: NpcRepository,
    private val collision: CollisionFlagMap,
) :
    QuestScript(
        "quest_ernestthechicken",
        "varp.haunted",
        rewards { item("obj.coins", ErnestProgress.CoinReward) },
        ItemRewardDisplay("obj.coins"),
    ) {

    private val logger = InlineLogger()

    override fun ScriptContext.init() {
        onOpNpc1("npc.veronica") { startVeronica(it.npc) }
        onOpNpc1("npc.professor_oddenstein") { startOddenstein(it.npc) }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Veronica</col> outside <col=800000>Draynor Manor</col>."

    override fun questLog(player: ProtectedAccess) =
        questJournal(player) {
            description(
                "<red>Veronica</red> is worried about her fiance <red>Ernest</red>, who went into " +
                    "<red>Draynor Manor</red> to ask for directions and never came out."
            )

            objective("I should speak to whoever lives in the manor.") {
                attribute(progress.foundErnest, "I found Ernest. He is a chicken.").strike()
            }

            if (progress.foundErnest.get(access.player) && !progress.hasReceivedParts(access.player)) {
                line(
                    "<red>Professor Oddenstein</red> needs three parts for his machine before he can " +
                        "turn Ernest back."
                )
                objective("I still need to find the <red>rubber tube</red>.") {
                    hasItem("rubber_tube", "I have found the <red>rubber tube</red>.")
                }
                objective("I still need to find the <red>pressure gauge</red>.") {
                    hasItem("pressure_gauge", "I have found the <red>pressure gauge</red>.")
                }
                objective("I still need to find the <red>oil can</red>.") {
                    hasItem("oil_can", "I have found the <red>oil can</red>.")
                }
            }

            objective(
                "I gave all three parts to <red>Professor Oddenstein</red>. " +
                    "I should speak to him to finish restoring Ernest."
            ) {
                visibleWhen { progress.hasReceivedParts(access.player) }
            }
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line(
                "Veronica's fiance Ernest had been turned into a chicken by Professor Oddenstein, " +
                    "who lives in Draynor Manor."
            )
            line(
                "I recovered the rubber tube, pressure gauge and oil can that the house gremlins had " +
                    "stolen, and Oddenstein turned Ernest back into a man."
            )
        }

    private val progress = ErnestProgress(quest)

    private suspend fun ProtectedAccess.startVeronica(npc: Npc) = startDialogue(npc) { veronica() }

    private suspend fun ProtectedAccess.startOddenstein(npc: Npc) =
        startDialogue(npc) { oddenstein(npc) }

    // https://oldschool.runescape.wiki/w/Transcript:Ernest_the_Chicken
    // https://oldschool.runescape.wiki/w/Transcript:Veronica
    // TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
    private suspend fun Dialogue.veronica() {
        if (quest.isQuestCompleted(player)) {
            chatNpc(happy, "Thank you for rescuing Ernest.")
            chatPlayer(quiz, "Where is he now?")
            chatNpc(
                neutral,
                "Oh he went off to talk to some green warty guy. I'm sure he'll be back soon.",
            )
            return
        }
        if (quest.questState(player) == QuestProgressState.IN_PROGRESS) {
            chatNpc(sad, "Have you found my sweetheart yet?")
            if (!progress.foundErnest.get(player)) {
                chatPlayer(sad, "No, not yet.")
                return
            }
            chatPlayer(neutral, "Yes, he's a chicken.")
            chatNpc(
                angry,
                "I know he's not exactly brave but I think you're being a bit harsh.",
            )
            chatPlayer(
                neutral,
                "No no, he's been turned into an actual chicken by a mad scientist.",
            )
            chatNpc(shocked, "Eeeeeek!")
            chatNpc(sad, "My poor darling, why must these things happen to us.")
            chatPlayer(neutral, "Well I'm doing my best to turn him back.")
            chatNpc(sad, "Well be quick, I'm sure being a chicken can't be good for him.")
            return
        }

        chatNpc(sad, "Can you please help me? I'm in a terrible spot of trouble.")

        val start =
            choice2(
                "Aha, sounds like a quest. I'll help.",
                true,
                "No, I'm looking for something to kill.",
                false,
            )
        if (!start) {
            chatPlayer(neutral, "No, I'm looking for something to kill.")
            chatNpc(shocked, "Oooh, you violent person you.")
            return
        }

        chatPlayer(happy, "Aha, sounds like a quest. I'll help.")
        chatNpc(
            neutral,
            "Yes yes, I suppose it is a quest. My fiance Ernest and I came upon this house.",
        )
        chatNpc(
            neutral,
            "Seeing as we were a little lost Ernest decided to go in and ask for directions.",
        )
        chatNpc(
            sad,
            "That was an hour ago. That house looks spooky, can you go and see if you can find " +
                "him for me?",
        )
        quest.advanceQuestStage(access)
        chatPlayer(neutral, "Ok, I'll see what I can do.")
        chatNpc(happy, "Thank you, thank you. I'm very grateful.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Professor_Oddenstein
    private suspend fun Dialogue.oddenstein(npc: Npc) {
        if (quest.isQuestCompleted(player)) {
            afterQuest()
            return
        }

        val inProgress = quest.questState(player) == QuestProgressState.IN_PROGRESS
        if (inProgress && progress.foundErnest.get(player)) {
            if (progress.hasReceivedParts(player) || progress.hasAllParts(player)) {
                handIn(npc)
                return
            }
            progressReport()
            return
        }

        chatNpc(neutral, "Be careful in here, there's lots of dangerous equipment.")

        if (!inProgress) {
            when (choice2("What does this machine do?", 1, "Is this your house?", 2)) {
                1 -> whatDoesThisMachineDo()
                2 -> isThisYourHouse()
            }
            return
        }

        when (
            choice3(
                "I'm looking for a guy called Ernest.",
                0,
                "What does this machine do?",
                1,
                "Is this your house?",
                2,
            )
        ) {
            0 -> introduceErnest()
            1 -> whatDoesThisMachineDo()
            2 -> isThisYourHouse()
        }
    }

    private suspend fun Dialogue.whatDoesThisMachineDo() {
        chatPlayer(quiz, "What does this machine do?")
        chatNpc(
            neutral,
            "Nothing at the moment... It's broken. It's meant to be a transmutation machine.",
        )
        chatNpc(
            neutral,
            "It has also spent time as a time travel machine, and a dramatic lightning " +
                "generator, and a thing for generating monsters.",
        )
    }

    private suspend fun Dialogue.isThisYourHouse() {
        chatPlayer(quiz, "Is this your house?")
        chatNpc(
            neutral,
            "No, I'm just one of the tenants. It belongs to the count who lives in the basement.",
        )
    }

    private suspend fun Dialogue.progressReport() {
        val held = PARTS.filter { player.inv.count(it) > 0 }
        chatNpc(neutral, "Have you found anything yet?")

        if (held.isEmpty()) {
            chatPlayer(sad, "I'm afraid I don't have any of them yet!")
            chatNpc(
                neutral,
                "I need a rubber tube, a pressure gauge and a can of oil. Then your friend " +
                    "can stop being a chicken.",
            )
            return
        }
        if (held.size == 1) {
            val (part, missing) =
                when (held[0]) {
                    RubberTube -> "rubber tube" to "pressure gauge and oil can"
                    PressureGauge -> "pressure gauge" to "oil can and rubber tube"
                    else -> "oil can" to "pressure gauge and rubber tube"
                }
            chatPlayer(neutral, "I've only got the $part so far.")
            chatNpc(neutral, "It's a good start, but I'll need my $missing too.")
            chatPlayer(neutral, "Ok, I'll go and look for them.")
            return
        }
        val (parts, missing) =
            when {
                OilCan !in held -> "rubber tube and the pressure gauge" to "oil can"
                PressureGauge !in held -> "oil can and the rubber tube" to "pressure gauge"
                else -> "oil can and the pressure gauge" to "rubber tube"
            }
        chatPlayer(neutral, "I've got the $parts.")
        chatNpc(neutral, "That's good, but I still need my $missing.")
        chatPlayer(neutral, "Ok, I'll go and look for it.")
    }

    private suspend fun Dialogue.introduceErnest() {
        chatPlayer(quiz, "I'm looking for a guy called Ernest.")
        chatNpc(happy, "Ah Ernest, top notch bloke. He's helping me with my experiments.")
        chatPlayer(quiz, "So you know where he is then?")
        chatNpc(neutral, "He's that chicken over there.")
        chatPlayer(quiz, "Ernest is a chicken...? Are you sure?")
        chatNpc(
            neutral,
            "Oh, he isn't normally a chicken, or at least he wasn't until he helped me test my " +
                "pouletmorph machine.",
        )
        chatNpc(
            neutral,
            "It was originally going to be called a transmutation machine. But after testing " +
                "pouletmorph seems more appropriate.",
        )
        val veronica =
            choice2(
                "I'm glad Veronica didn't actually get engaged to a chicken.",
                true,
                "Change him back this instant!",
                false,
            )
        if (veronica) {
            chatPlayer(neutral, "I'm glad Veronica didn't actually get engaged to a chicken.")
            chatNpc(quiz, "Who's Veronica?")
            chatPlayer(neutral, "Ernest's fiancee. She probably doesn't want to marry a chicken.")
            chatNpc(happy, "Ooh I dunno. She could have free eggs for breakfast.")
            chatPlayer(angry, "I think you'd better change him back.")
        } else {
            chatPlayer(angry, "Change him back this instant!")
        }
        chatNpc(sad, "Umm... It's not so easy...")
        chatNpc(
            sad,
            "My machine is broken, and the house gremlins have run off with some vital bits.",
        )
        chatPlayer(neutral, "Well I can look for them.")
        chatNpc(
            happy,
            "That would be a help. They'll be somewhere in the manor house or its grounds, the " +
                "gremlins never get further than the entrance gate.",
        )
        chatNpc(
            neutral,
            "I'm missing the pressure gauge and a rubber tube. They've also taken my oil can, " +
                "which I'm going to need to get this thing started again.",
        )
        progress.foundErnest.set(player, true)
    }

    private suspend fun Dialogue.handIn(npc: Npc) {
        if (!progress.hasReceivedParts(player)) {
            chatNpc(neutral, "Have you found anything yet?")
            chatPlayer(happy, "I have everything!")
            chatNpc(happy, "Give 'em here then.")

            if (!progress.receiveParts(access)) {
                return
            }

            access.mes("You give the rubber tube, a pressure gauge, ")
            access.mes("and a can of oil to the professor.")
            chatNpc(neutral, "Let's get this fixed then.")
        }

        if (!progress.canReceiveReward(access)) {
            chatNpc(neutral, "Make some room for your reward, then speak to me again.")
            return
        }

        PathingEntityCommon.telejump(npc, collision, OddensteinMachineTile)
        npc.faceSquare(MachineCoord)
        npc.anim(MachineSeq)
        access.mes("Oddenstein starts up the machine.")
        access.delay(2)

        access.mes("The machine hums and shakes.")
        access.soundSynth(RumbleSound)
        access.delay(3)

        val chicken = findChicken()
        access.spotanimMap(worldRepo, CastSpotanim, MachineCoord, height = MachineSpotanimHeight)
        if (chicken != null) {
            worldRepo.projAnim(
                ProjAnim(
                    spotanim = TravelSpotanim.asRSCM(RSCMType.SPOTANIM),
                    startHeight = MachineSpotanimHeight,
                    endHeight = 0,
                    startTime = 15,
                    endTime = 45,
                    angle = 0,
                    progress = 0,
                    sourceIndex = 0,
                    targetIndex = chicken.slotId + 1,
                    startCoord = MachineCoord,
                    endCoord = chicken.coords,
                )
            )
            chicken.spotanim(ImpactSpotanim, delay = 45, height = 0)
        }
        access.soundSynth(StunSound, loops = 2)
        access.delay(2)

        progress.restoreErnest(access) {
            spawnHumanErnest(findChicken()?.coords ?: ChickenSpawn)
            access.soundSynth(TransformSound)
            access.mes("The machine turns Ernest back into human form.")
            val thanks = if (access.isBodyTypeA()) "Thank you sir." else "Thank you m'lady."
            chatNpcSpecific(
                "Ernest",
                "npc.ernest",
                happy,
                "$thanks It was dreadfully irritating being a chicken. How can I ever thank you?",
            )
            chatPlayer(happy, "Well a cash reward is always nice...")
            chatNpcSpecific("Ernest", "npc.ernest", happy, "Of course, of course.")
        }
    }

    // TODO(content): [verify:in_game] Whether the greeting comes before the post-quest options.
    private suspend fun Dialogue.afterQuest() {
        val portal = choice2("What does the portal machine do?", true, "Is this your house?", false)
        if (!portal) {
            isThisYourHouse()
            return
        }
        chatPlayer(quiz, "What does the portal machine do?")
        chatNpc(
            happy,
            "Ah, an interesting question! I believe it should create a rift in the very fabric " +
                "of space! Opening a portal to some, hitherto undiscovered plane that exists " +
                "parallel to our own.",
        )
        chatPlayer(quiz, "Huh?")
        chatNpc(neutral, "It's a teleport machine.")
    }

    private fun findChicken(): Npc? =
        npcRepo.findAll(ZoneKey.from(MachineCoord), zoneRadius = 1).firstOrNull {
            it.isType(ChickenNpc)
        }

    private fun spawnHumanErnest(coords: CoordGrid) {
        val type = ServerCacheManager.getNpc(HumanErnest.asRSCM(RSCMType.NPC))
        if (type == null) {
            logger.warn {
                "No npc type '$HumanErnest'; Ernest will vanish instead of turning human."
            }
            return
        }
        npcRepo.add(Npc(type, coords), duration = HumanErnestLifespan)
    }

    private companion object {
        private const val RubberTube = "obj.rubber_tube"
        private const val PressureGauge = "obj.pressure_gauge"
        private const val OilCan = "obj.oil_can"
        private val PARTS = listOf(RubberTube, PressureGauge, OilCan)

        private const val RumbleSound = "synth.swan_rumble"
        private const val StunSound = "synth.stun_all"
        private const val TransformSound = "synth.chicken_into_human"

        private const val MachineSeq = "seq.human_pickuptable"

        private const val CastSpotanim = "spotanim.stun_casting"
        private const val TravelSpotanim = "spotanim.stun_travel"
        private const val ImpactSpotanim = "spotanim.stunned"
        private const val MachineSpotanimHeight = 50

        private const val ChickenNpc = "npc.ernest_multichicken"

        private const val HumanErnest = "npc.ernest_multiernest"

        private const val HumanErnestLifespan = 25

        private val ChickenSpawn = CoordGrid(3110, 3364, 2)

        private val MachineCoord = CoordGrid(3111, 3366, 2)

        private val OddensteinMachineTile = CoordGrid(3111, 3367, 2)
    }
}
