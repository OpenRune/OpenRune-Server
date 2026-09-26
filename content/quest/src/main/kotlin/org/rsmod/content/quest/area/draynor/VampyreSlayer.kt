package org.rsmod.content.quest.area.draynor

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcServerType
import jakarta.inject.Inject
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.heal
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.menu
import org.rsmod.content.quest.manager.rewards
import org.rsmod.content.quest.manager.startQuestPrompt
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

private var Player.morganThanked by intVarBit("varbit.morgan_postquest_dialogue")
private var Player.harlowGivenBeer by intVarBit("varbit.harlow_given_beer")
private var Player.harlowGivenStake by intVarBit("varbit.harlow_given_stake")

class VampyreSlayer
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val locRepo: LocRepository,
    private val aiPlayerInteractions: AiPlayerInteractions,
) :
    QuestScript(
        "quest_vampyreslayer",
        "varp.vampire",
        rewards { xp("stat.attack", 4825.0) },
        ItemRewardDisplay(STAKE),
    ) {

    private val counts = hashMapOf<PlayerUid, Npc>()
    private val owners = hashMapOf<Npc, Player>()
    private val searchedCupboard = hashSetOf<PlayerUid>()

    override fun ScriptContext.init() {
        for (morgan in listOf("npc.morgan", "npc.morgan_pre", "npc.morgan_post")) {
            onOpNpc1(morgan) { startDialogue(it.npc) { morgan() } }
        }
        for (harlow in listOf("npc.dr_harlow", "npc.dr_harlow_vis")) {
            onOpNpc1(harlow) { startDialogue(it.npc) { harlow() } }
            onOpNpcU(harlow) {
                if (it.objType.internalName == BEER && canGiveBeer(player)) {
                    startDialogue(it.npc) { giveBeer() }
                } else {
                    mes("Nothing interesting happens.")
                }
            }
        }
        onOpLoc1("loc.garliccupboardshut") { openCupboard(it.loc) }
        onOpLoc1("loc.garliccupboardopen") { searchCupboard() }
        onOpLoc2("loc.garliccupboardopen") { closeCupboard(it.loc) }
        onOpLoc1("loc.vampcoffin") { openCoffin(it.loc) }
        onNpcQueue(npcType(COUNT), "queue.death") { countDefeated() }
        onPlayerQueue(STAKE_QUEUE) { stakeCount() }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Morgan</col> who can be found in <col=800000>Draynor Village</col>."

    override fun questLog(player: ProtectedAccess): String =
        questJournal(player) {
            line("I spoke to <red>Morgan</red> at his home in <red>Draynor Village</red>. He told me about <red>Count Draynor</red>, a vampyre living in <red>Draynor Manor</red> north of the village. At his request, I agreed to try and kill Count Draynor. Morgan suggested I begin by seeking out <red>Dr Harlow</red>, a retired vampyre hunter, at the <red>Blue Moon Inn</red> in <red>Varrock</red>.")
            if (stage(access.player) < SPOKEN_TO_HARLOW) return@questJournal
            if (access.player.harlowGivenStake == 0) {
                line("I travelled to the Blue Moon Inn in Varrock and found Dr Harlow. However, he doesn't seem willing to talk to me unless I buy him a <red>Beer</red> first.")
                return@questJournal
            }
            line("I travelled to the Blue Moon Inn in Varrock and found Dr Harlow. He told me that vampyres are hard to kill due to their ability to regenerate. However, he gave me a <red>Stake</red> and told me I could use it to prevent the regeneration. I'll now need to head to <red>Draynor Manor</red>, find <red>Count Draynor</red> and kill him. I should make sure to take the Stake with me, along with a <red>Hammer</red>. Most General Stores should stock a Hammer. Dr Harlow also suggested I carry some <red>Garlic</red> on me. Morgan might have some.")
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line("Dr Harlow gave me a stake, and with it and a hammer I killed Count Draynor in the basement of Draynor Manor.")
        }

    private fun stage(player: Player): Int = quest.getQuestStage(player)

    private fun npcType(internal: String): NpcServerType =
        ServerCacheManager.getNpc(internal.asRSCM(RSCMType.NPC)) ?: error("Missing npc: $internal")

    private suspend fun Dialogue.morgan() {
        when {
            quest.isQuestCompleted(player) -> morganThanks()
            quest.isQuestInProgress(player) -> morganProgress()
            else -> morganStart()
        }
    }

    private suspend fun Dialogue.morganStart() {
        chatNpc(shocked, "Could it be? A bold adventurer! Please, you must help us!")
        chatPlayer(quiz, "What is it? What's the problem?")
        chatNpc(
            shocked,
            "It's the evil vampyre, Count Draynor! For too long he's terrorised us from his manor to " +
                "the north. Someone finally needs to put a stop to him once and for all!",
        )
        if (access.player.combatLevel < RECOMMENDED_COMBAT) {
            val warning =
                "Before starting this quest, be aware that your combat level is lower than the " +
                    "recommended level of $RECOMMENDED_COMBAT."
            mesbox(warning)
        }
        if (!startQuestPrompt(quest)) {
            chatPlayer(
                worried,
                "An evil vampyre? I don't think this is the job for me. It sounds far too scary!",
            )
            chatNpc(
                sad,
                "I understand... Hopefully someone else will come along to finally save us from this " +
                    "evil.",
            )
            return
        }
        chatPlayer(neutral, "Sounds like a job for me. Where should I start?")
        chatNpc(
            happy,
            "Oh, thank goodness! I've been hoping this day would come for a long time, so I've made " +
                "sure to do my research. I've heard of a retired vampyre hunter called Dr Harlow who " +
                "lives in Varrock.",
        )
        quest.advanceQuestStageTo(access, STARTED)
        chatNpc(
            happy,
            "If you speak to him, I'm sure he'll be able to help. I hear he's a bit of an old soak " +
                "these days, so he spends most of his time in the Blue Moon Inn.",
        )
        morganOptions("What else can you tell me about this vampyre?")
    }

    private suspend fun Dialogue.morganProgress() {
        chatNpc(quiz, "Adventurer! Has Count Draynor been dealt with yet?")
        if (player.harlowGivenStake == 1) {
            chatPlayer(neutral, "I'm still working on it, but I could use some garlic to help me out.")
            chatNpc(
                happy,
                "Ah, yes! I heard vampyres didn't like garlic. I keep some in the cupboard upstairs, " +
                    "just in case Count Draynor comes and tries anything!",
            )
            val lore =
                menu(
                    "What else can you tell me about Count Draynor?" to true,
                    "Perfect, thanks." to false,
                )
            if (lore) {
                morganLore("What else can you tell me about Count Draynor?")
            } else {
                chatPlayer(happy, "Perfect, thanks.")
                chatNpc(happy, "Good luck in Draynor Manor, adventurer.")
            }
            return
        }
        chatPlayer(neutral, "I'm still working on it.")
        chatNpc(worried, "Please hurry! I'm sure Dr Harlow in Varrock's Blue Moon Inn can help.")
        morganOptions("What else can you tell me about Count Draynor?")
    }

    private suspend fun Dialogue.morganOptions(loreOption: String) {
        val lore = menu(loreOption to true, "Alright, I'll see about paying him a visit." to false)
        if (lore) {
            morganLore(loreOption)
        } else {
            chatPlayer(neutral, "Alright, I'll see about paying him a visit.")
            chatNpc(happy, "Thank you, brave adventurer.")
        }
    }

    private suspend fun Dialogue.morganLore(question: String) {
        chatPlayer(quiz, question)
        chatNpc(happy, "All sorts of things! I've studied the legends extensively!")
        chatPlayer(confused, "Wait... legends? What do you mean legends?")
        chatNpc(
            neutral,
            "The legends of Count Draynor! It's said that long ago, he travelled here from dark lands " +
                "in the east. He built Draynor Manor, and from there he began his campaign of " +
                "darkness against our peaceful lands!",
        )
        chatPlayer(
            quiz,
            "Long ago? If this has been going on for so long, why has no one tried to stop him?",
        )
        chatNpc(
            sad,
            "Many have tried. All have failed. Vampyres are not killed easily. Only those with the " +
                "right skills and equipment have any hope of slaying one.",
        )
        chatPlayer(
            quiz,
            "Like this Dr Harlow? Home come he's never tried to kill this Count Draynor?",
        )
        chatNpc(confused, "I'm... not sure. Perhaps he was always too busy killing other vampyres?")
        chatPlayer(
            shifty,
            "Hmm... Something seems amiss here. Are you sure you're telling me everything?",
        )
        chatNpc(
            worried,
            "Well... it is true that some claim Count Draynor hasn't left his manor for generations " +
                "now.",
        )
        chatPlayer(quiz, "So he's not actually attacked the village?")
        chatNpc(worried, "Er... no. At least, not for a long time! He used to though!")
        chatPlayer(quiz, "When?")
        chatNpc(worried, "Well... not since I've moved here at least.")
        chatPlayer(
            quiz,
            "So if there's no actual danger, why are you so keen for someone to kill this vampyre?",
        )
        chatNpc(
            angry,
            "Because what if he does start attacking again? Are we just meant to live our lives " +
                "constantly fearing the future?",
        )
        chatPlayer(
            neutral,
            "It doesn't really sound like anyone is living in fear apart from you.",
        )
        chatNpc(
            angry,
            "More fool them! Count Draynor has cast a shadow over Draynor Village for generations. " +
                "They even named the place after him for Saradomin's sake!",
        )
        chatNpc(
            angry,
            "Even if the people here don't realise it, we will never be at peace until we know he's " +
                "dead!",
        )
        chatPlayer(neutral, "If you're sure... I guess I'll go and see this Dr Harlow.")
        chatNpc(happy, "Thank you, brave adventurer.")
    }

    private suspend fun Dialogue.morganThanks() {
        if (player.morganThanked == 0) {
            chatPlayer(happy, "I have some good news. Count Draynor is no more!")
            player.morganThanked = 1
            chatNpc(
                happy,
                "He's really gone? Finally, we can live without fear! Thank you, thank you! You're a " +
                    "true hero!",
            )
            chatPlayer(happy, "I'm happy to have helped.")
            return
        }
        chatNpc(happy, "Once again, thank you for slaying that vampyre! You will always be a hero!")
        chatPlayer(happy, "Don't mention it.")
    }

    private fun canGiveBeer(player: Player): Boolean =
        stage(player) == SPOKEN_TO_HARLOW && player.harlowGivenBeer == 0

    private suspend fun Dialogue.harlow() {
        val stage = stage(player)
        when {
            stage == STARTED -> harlowFirstMeeting()
            stage == SPOKEN_TO_HARLOW && player.harlowGivenBeer == 0 -> harlowWantsBeer()
            stage == SPOKEN_TO_HARLOW -> harlowReturn()
            else -> {
                chatNpc(drunk, "Buy me a drink pleassh...")
                chatPlayer(neutral, "I think you've had enough.")
            }
        }
    }

    private suspend fun Dialogue.harlowFirstMeeting() {
        chatNpc(drunk, "Buy me a drink pleassh...")
        val help =
            menu(
                "I need your help dealing with a vampyre." to true,
                "I think you've had enough." to false,
            )
        if (!help) {
            chatPlayer(neutral, "I think you've had enough.")
            return
        }
        chatPlayer(neutral, "I need your help dealing with a vampyre.")
        chatNpc(drunk, "A vampyre you shhay...?")
        chatPlayer(neutral, "Not just any vampyre. Count Draynor.")
        chatNpc(drunk, "Draynor? Well, buy me a beer firsht...")
        chatPlayer(confused, "Are you sure you've not had enough?")
        quest.advanceQuestStageTo(access, SPOKEN_TO_HARLOW)
        chatNpc(drunk, "Huh? No, I don't think ssho. Now, buy ush a beer.")
        if (access.inv.count(BEER) == 0) return
        val give = menu("Alright, here you go." to true, "No. I think you've had enough." to false)
        if (give) {
            chatPlayer(neutral, "Alright, here you go.")
            giveBeer()
        } else {
            chatPlayer(neutral, "No. I think you've had enough.")
            chatNpc(drunk, "No help for yoush then.")
        }
    }

    private suspend fun Dialogue.harlowWantsBeer() {
        chatNpc(drunk, "Have yoush got me a beer?")
        val hasBeer = access.inv.count(BEER) > 0
        val give =
            menu(
                (if (hasBeer) "Yes, here you go." else "I'll go and get you one.") to hasBeer,
                "No. I think you've had enough." to null,
            )
        when (give) {
            true -> {
                chatPlayer(neutral, "Yes, here you go.")
                giveBeer()
            }
            false -> {
                chatPlayer(neutral, "I'll go and get you one.")
                chatNpc(drunk, "Cheersh, matey...")
            }
            null -> {
                chatPlayer(neutral, "No. I think you've had enough.")
                chatNpc(drunk, "No help for yoush then.")
            }
        }
    }

    private suspend fun Dialogue.giveBeer() {
        access.invDel(access.inv, BEER)
        player.harlowGivenBeer = 1
        objbox(BEER, "You give a beer to Dr Harlow.")
        chatNpc(drunk, "Cheersh, matey...")
        chatPlayer(quiz, "Now, about Count Draynor...")
        chatNpc(
            drunk,
            "Yesh, Count Draynor! The evil nashty vampyre that no one's ever sheen! Every nowsh and " +
                "then, some adventurer... theysh go and try to kill him, but none of 'em ever comesh " +
                "back.",
        )
        chatNpc(
            drunk,
            "You want to havesh a go? Then don't be likesh them! Be prepared! Vampyres... Theysh hard " +
                "to kill. Some... maybe imposhible.",
        )
        chatPlayer(quiz, "So how do I make sure I'm prepared?")
        chatNpc(
            drunk,
            "Most vampyres regenerate. Yoush need to stop them. I knowsh a few ways, but the easiest " +
                "ish a stake. Here, You can havesh thish one.",
        )
        giveStake()
        harlowInstructions(first = true)
        harlowQuestions(ownStakeExit = false)
    }

    private suspend fun Dialogue.giveStake() {
        player.harlowGivenStake = 1
        access.invAdd(access.inv, STAKE)
        objbox(STAKE, "Dr Harlow hands you a stake.")
    }

    private suspend fun Dialogue.harlowInstructions(first: Boolean) {
        chatNpc(
            drunk,
            (if (first) "Takesh that to Draynor Manor." else "Go to Draynor Manor.") +
                " Find the vampyre inshide and show him what for! When hesh weak, use a hammer to " +
                "drive " + (if (first) "the stake" else "that stake I gave you") + " in! Mosht " +
                "general stores have them.",
        )
        chatNpc(drunk, "Oh, and yoush should take some garlic with you as well. Vampyres don't likesh garlic.")
        chatPlayer(quiz, "Garlic? Hmm... I'll see if Morgan knows where I can get some.")
    }

    private suspend fun Dialogue.harlowReturn() {
        chatNpc(drunk, "Yoush back! Killed that vampyre yet?")
        if (!access.playerContainsObj(STAKE)) {
            chatPlayer(sad, "No. I lost that stake you gave me.")
            chatNpc(drunk, "Oh, hangsh on then...")
            access.invAdd(access.inv, STAKE)
            objbox(STAKE, "Dr Harlow hands you a stake.")
            chatNpc(
                drunk,
                "Good job I've gotsh some spares. Now, takesh that to Draynor Manor. Find the vampyre " +
                    "inshide and show him what for! When hesh weak, use a hammer to drive the stake " +
                    "in! Mosht general stores have them.",
            )
            chatNpc(drunk, "Oh, and yoush should take some garlic with you as well. Vampyres don't likesh garlic.")
            chatPlayer(quiz, "Garlic? Hmm... I'll see if Morgan knows where I can get some.")
        }
        harlowQuestions(ownStakeExit = true)
    }

    private suspend fun Dialogue.harlowQuestions(ownStakeExit: Boolean) {
        var askedAgain = ownStakeExit
        while (true) {
            val options = buildList {
                if (askedAgain) add("What do I need to do again?" to 0)
                add("You said no one's ever seen Count Draynor?" to 1)
                add("So you were once a proper vampyre hunter?" to 2)
                add((if (ownStakeExit) "Not yet. I'd best get going." else "I'd best get going.") to 3)
            }
            when (menu(options)) {
                0 -> {
                    chatPlayer(quiz, "What do I need to do again?")
                    harlowInstructions(first = false)
                }
                1 -> {
                    chatPlayer(quiz, "You said no one's ever seen Count Draynor?")
                    chatNpc(drunk, "No, butsh every now and then someonesh tried to kill him.")
                    chatPlayer(
                        quiz,
                        "But they've never come back? Meaning someone or something killed them most " +
                            "likely. Presumably Count Draynor?",
                    )
                    chatNpc(drunk, "Yesh, I'd say so.")
                    chatPlayer(quiz, "So why does he never leave his manor?")
                    chatNpc(drunk, "Who knowsh. Most likely he's grown too weak to do ssho.")
                    chatPlayer(quiz, "Weak? So he shouldn't be too hard to kill then?")
                    chatNpc(
                        drunk,
                        "Even a weak vampyre ish very dangerous. Just go ashk those other adventurers " +
                            "thatsh never came back.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "So you were once a proper vampyre hunter?")
                    chatNpc(
                        drunk,
                        "Yesh! I travelled their homeland to the easht. Morytania! I killed lotsh of " +
                            "vampyres over there!",
                    )
                    chatPlayer(quiz, "What was the most dangerous vampyre you killed?")
                    chatNpc(drunk, "Dangerous? Oh, well... Ish never killed one of the really dangerous ones.")
                    chatPlayer(quiz, "Why not?")
                    chatNpc(drunk, "Can't. Theysh too strong. Nothing cansh hurt them.")
                    chatPlayer(worried, "Oh... But what if Count Draynor's like that?")
                    chatNpc(
                        drunk,
                        "Yoush don't get many vampyres around here. Pretty much none, actshually. " +
                            "There's speshial magic that stops them coming over here. Any that are " +
                            "here... theysh weak... and trapped.",
                    )
                    chatPlayer(quiz, "So how come you've never tried to kill him yourself?")
                    chatNpc(
                        drunk,
                        "Why bother? Hesh not killed anyone in a long time, so no one really caresh " +
                            "about him that much, apart from the odd adventurer thatsh looking to " +
                            "prove themshelves.",
                    )
                    chatPlayer(neutral, "I see. Fair enough.")
                }
                else -> {
                    chatPlayer(
                        neutral,
                        if (ownStakeExit) "Not yet. I'd best get going." else "I'd best get going.",
                    )
                    chatNpc(drunk, "Good luck, matey...")
                    return
                }
            }
            askedAgain = true
        }
    }

    private suspend fun ProtectedAccess.openCupboard(loc: BoundLocInfo) {
        arriveDelay()
        anim("seq.human_openchest")
        soundSynth("synth.cupboard_open")
        delay(1)
        searchedCupboard.remove(player.uid)
        locRepo.change(loc, "loc.garliccupboardopen", CUPBOARD_OPEN_TICKS)
    }

    private suspend fun ProtectedAccess.closeCupboard(loc: BoundLocInfo) {
        arriveDelay()
        anim("seq.human_closechest")
        delay(1)
        locRepo.change(loc, "loc.garliccupboardshut", Int.MAX_VALUE)
    }

    private suspend fun ProtectedAccess.searchCupboard() {
        arriveDelay()
        if (inv.isFull()) {
            mes("You don't have enough inventory space.")
            return
        }
        invAdd(inv, GARLIC)
        if (searchedCupboard.add(player.uid)) {
            mes("The cupboard contains garlic. You take a clove.")
        } else {
            mes("You take a clove of garlic.")
        }
    }

    private suspend fun ProtectedAccess.openCoffin(coffin: BoundLocInfo) {
        arriveDelay()
        if (!quest.isQuestInProgress(player)) {
            mes("The coffin is sealed shut.")
            return
        }
        val existing = counts[player.uid]
        if (existing != null && existing.isSlotAssigned) {
            mes("You should probably deal with the vampyre!")
            return
        }
        faceSquare(coffin.coords)
        locRepo.change(coffin, "loc.vampcoffin_animated", COFFIN_ANIMATION_TICKS)
        delay(4)
        val rising = Npc("npc.count_draynor_coffin", coffin.coords)
        npcRepo.add(rising, COFFIN_ANIMATION_TICKS)
        rising.anim("seq.vampireslayer_arise")
        delay(5)
        if (rising.isSlotAssigned) npcRepo.del(rising, Int.MAX_VALUE)
        val count = Npc(COUNT, COUNT_SPAWN)
        npcRepo.add(count, COUNT_LIFESPAN)
        count.anim("seq.vampireslayer_travel")
        counts[player.uid] = count
        owners[count] = player
        if (inv.count(GARLIC) > 0) {
            mes("The vampyre seems to be weakened by the garlic you're carrying.")
            count.hitpoints = (count.hitpoints - GARLIC_DAMAGE).coerceAtLeast(1)
        }
        count.opPlayer2(player, aiPlayerInteractions)
    }

    private suspend fun StandardNpcAccess.countDefeated() {
        val owner = owners[npc]
        val staked =
            owner != null &&
                owner.inv.count(STAKE) > 0 &&
                owner.inv.count(HAMMER) > 0 &&
                quest.isQuestInProgress(owner)
        if (!staked) {
            if (owner != null && owner.inv.count(STAKE) > 0) {
                owner.mes("<col=e00a19>You're unable to push the stake far enough in!")
            }
            owner?.mes("<col=e00a19>The vampyre seems to regenerate!")
            npc.heal(npc.type.hitpoints)
            return
        }
        owner.strongQueue(STAKE_QUEUE, 1)
        npc.transmog(npcType("npc.count_draynor_done"), Int.MAX_VALUE)
        delay(1)
        anim("seq.human_death")
        delay(2)
        owners.remove(npc)
        counts.remove(owner.uid)
        if (npc.isSlotAssigned) npcRepo.del(npc, Int.MAX_VALUE)
    }

    private suspend fun ProtectedAccess.stakeCount() {
        mes("<col=e00a19>You hammer the stake into the vampyre's chest!")
        anim("seq.human_stake")
        quest.advanceQuestStageTo(this, COMPLETE)
    }

    private companion object {
        const val STARTED = 1
        const val SPOKEN_TO_HARLOW = 2
        const val COMPLETE = 3
        const val RECOMMENDED_COMBAT = 20

        const val BEER = "obj.beer"
        const val STAKE = "obj.stake"
        const val HAMMER = "obj.hammer"
        const val GARLIC = "obj.garlic"
        const val COUNT = "npc.count_draynor"
        const val STAKE_QUEUE = "queue.vampyre_slayer_stake"

        const val GARLIC_DAMAGE = 10
        const val COFFIN_ANIMATION_TICKS = 10
        const val COUNT_LIFESPAN = 500
        const val CUPBOARD_OPEN_TICKS = 300

        val COUNT_SPAWN = CoordGrid(3078, 9774, 0)
    }
}
