package org.rsmod.content.quest.area.varrock.childrenofthesun

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.route.RayCastValidator
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerTimer
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.menu
import org.rsmod.content.quest.manager.rewards
import org.rsmod.content.quest.manager.startQuestPrompt
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

class ChildrenOfTheSun
internal constructor(private val scenes: CotsScenes, private val tails: CotsTails) :
    QuestScript(
        "quest_childrenofthesun",
        "varp.vmq1_primary",
        rewards { extra("Access to Varlamore") },
        ItemRewardDisplay(RewardModel),
        questVarbit = "varbit.vmq1",
    ) {

    @Inject
    constructor(
        npcRepo: NpcRepository,
        locRepo: LocRepository,
        collision: CollisionFlagMap,
        sight: RayCastValidator,
    ) : this(WorldScenes(npcRepo, locRepo, collision), GuardTails(npcRepo, sight))

    private var Player.metAlina by boolVarBit("varbit.vmq1_met_alina")

    override fun ScriptContext.init() {
        check(quest.maxSteps == CotsStage.Complete) {
            "Children of the Sun end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the script completes at ${CotsStage.Complete}."
        }

        onOpNpc1(CotsNpc.AlinaBase) { talkToAlina(it.npc) }
        onOpNpc1(CotsNpc.NoahBase) { talkToAlina(it.npc) }
        onOpNpc1(CotsNpc.TobynBase) { startDialogue(it.npc) { tobynConversation() } }
        onOpNpc1(CotsNpc.TobynRoofBase) { startDialogue(it.npc) { tobynConversation() } }
        onOpNpc1(CotsNpc.ItzlaBase) { startDialogue(it.npc) { itzlaConversation() } }
        for (guard in 1..CotsNpc.GuardCount) {
            onOpNpc1(CotsNpc.guardBase(guard)) {
                when (player.vars[CotsNpc.guardVarbit(guard)]) {
                    GuardUnmarked -> markGuard(guard)
                    GuardMarked -> unmarkGuard(guard)
                }
            }
        }

        onPlayerTimer(TailTimer) { tailTick() }
        onPlayerLogin {
            tails.stop(player)
            syncGuards(player)
        }
        onPlayerLogout { tails.stop(player) }
    }

    override fun subTitle(): String =
        "speaking to <col=800000>Alina</col> or <col=800000>Noah</col> in " +
            "<col=800000>Varrock Square</col>."

    override fun questLog(player: ProtectedAccess) =
        questJournal(player) {
            description(
                "A delegation from the far-off kingdom of <red>Varlamore</red> has come to " +
                    "<red>Varrock</red> to sign a treaty with the rulers of " +
                    "<red>Misthalin</red>. Something about the visit doesn't feel right."
            )
            for (step in JournalSteps) {
                objective(step.text) {
                    visibleWhen { stage(access.player) in step.from..step.until }
                }
            }
            objective(
                "I have marked <red>${markedCount(access.player)}</red> of the " +
                    "<red>$BanditCount</red> bandits so far."
            ) {
                visibleWhen { stage(access.player) == CotsStage.Marking }
            }
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line(
                "A delegation from Varlamore arrived in Varrock. Following a guard who looked " +
                    "suspicious, I overheard four bandits plotting to attack them."
            )
            line(
                "I warned Sergeant Tobyn and picked the disguised bandits out from among the " +
                    "guards. They were arrested."
            )
            line(
                "Prince Itzla Arkan questioned one of the prisoners with me. A Varlamorian had " +
                    "paid them to kill Servius, the Teokan of Ralos."
            )
            line(
                "The prince has invited me to join his investigation in Varlamore, and the road " +
                    "there is now open to me."
            )
        }

    private fun stage(player: Player): Int = quest.getQuestStage(player)

    private fun ProtectedAccess.setStage(stage: Int) {
        quest.setQuestStage(this, stage)
        syncGuards(player)
    }

    fun syncGuards(player: Player) {
        val marking = stage(player) == CotsStage.Marking
        for (guard in 1..CotsNpc.GuardCount) {
            val varbit = CotsNpc.guardVarbit(guard)
            val current = player.vars[varbit]
            val wanted =
                when {
                    !marking -> GuardHidden
                    current == GuardHidden -> GuardUnmarked
                    else -> current
                }
            if (current != wanted) {
                VarPlayerIntMapSetter.set(player, varbit, wanted)
            }
        }
    }

    internal fun markedCount(player: Player): Int =
        (1..CotsNpc.GuardCount).count { player.vars[CotsNpc.guardVarbit(it)] == GuardMarked }

    private fun markedCorrectly(player: Player): Boolean =
        (1..CotsNpc.GuardCount).all {
            val marked = player.vars[CotsNpc.guardVarbit(it)] == GuardMarked
            marked == (it <= CotsNpc.BanditGuards)
        }

    private suspend fun ProtectedAccess.markGuard(guard: Int) {
        if (stage(player) != CotsStage.Marking) {
            return
        }
        if (markedCount(player) >= CotsNpc.BanditGuards) {
            startDialogue { mesbox("You've already marked enough guards.") }
            return
        }
        VarPlayerIntMapSetter.set(player, CotsNpc.guardVarbit(guard), GuardMarked)
        mes("You mark the guard.")
    }

    private fun ProtectedAccess.unmarkGuard(guard: Int) {
        if (stage(player) != CotsStage.Marking) {
            return
        }
        VarPlayerIntMapSetter.set(player, CotsNpc.guardVarbit(guard), GuardUnmarked)
        mes("You unmark the guard.")
    }

    internal suspend fun ProtectedAccess.tailTick() {
        if (stage(player) != CotsStage.Tailing) {
            tails.stop(player)
            return
        }
        when (tails.tick(player)) {
            TailResult.Continue -> return
            TailResult.Spotted -> {
                delay(SpottedTicks)
                tails.stop(player)
                startDialogue { mesbox("You failed to stay hidden from the guard.") }
            }
            TailResult.TooFar -> {
                tails.stop(player)
                startDialogue { mesbox("You failed to stay close enough to the guard.") }
            }
            TailResult.Arrived -> {
                tails.stop(player)
                finishTail()
            }
        }
    }

    internal suspend fun ProtectedAccess.finishTail() {
        setStage(CotsStage.Eavesdropping)
        scenes.eavesdrop(this)
        setStage(CotsStage.ReportToTobyn)
    }

    private suspend fun ProtectedAccess.talkToAlina(npc: Npc) {
        startDialogue(npc) { alinaConversation() }
    }

    internal suspend fun Dialogue.alinaConversation() {
        when (stage(player)) {
            CotsStage.NotStarted -> beforeQuest()
            CotsStage.Started -> {
                alina(
                    happy,
                    "Hello again! So what do you think? Reckon you'll be one of the first to " +
                        "head over to Varlamore?",
                )
                delegationTopics()
            }
            CotsStage.Tailing -> {
                noah(
                    quiz,
                    "That was quite a large bag that guard was carrying. I wonder where he's " +
                        "taking it...",
                )
                tails.start(access)
            }
        }
    }

    private suspend fun Dialogue.beforeQuest() {
        if (!player.metAlina) {
            noah(quiz, "So they're from the west?")
            alina(
                happy,
                "Yes! They call it Varlamore, the Shining Kingdom! It's meant to be one of the " +
                    "most beautiful places in the world!",
            )
            chatPlayer(quiz, "What are you talking about?")
            player.metAlina = true
            alina(happy, "Oh, hello! Are you here to see the delegation?")
            chatPlayer(quiz, "Delegation?")
            noah(confused, "See! It's not just me who doesn't know what's happening.")
            alina(
                happy,
                "The delegation from Varlamore, of course! After all these years, they're " +
                    "finally opening up the kingdom for travel and trade! Isn't it exciting?",
            )
            noah(bored, "Why would I be excited about a place I never even knew existed?")
        }
        alina(
            happy,
            "A whole land locked off for hundreds of years! Do you not want to be one of the " +
                "first outsiders to step foot there? Think of the adventure!",
        )
        noah(
            worried,
            "Adventure? Sounds dangerous. I'll be staying right here, thank you very much!",
        )
        alina(angry, "Bah! Boring! What about you, stranger?")
        if (!startQuestPrompt(quest)) {
            chatPlayer(neutral, "I'm not sure. Doesn't really sound like my kind of thing.")
            alina(neutral, "Well... suit yourself, I guess.")
            return
        }
        chatPlayer(happy, "Well I'm actually something of an adventurer myself.")
        alina(
            laugh,
            "That's the spirit! I bet you'll want to be one of the first to head over to " +
                "Varlamore!",
        )
        quest.setQuestStage(access, CotsStage.Started)
        delegationTopics()
    }

    private enum class Topic {
        Varlamore,
        Source,
        Arrival,
        Leave,
    }

    private suspend fun Dialogue.delegationTopics() {
        while (true) {
            val topic =
                menu(
                    "What can you tell me about Varlamore?" to Topic.Varlamore,
                    "How come you know so much about Varlamore?" to Topic.Source,
                    "When will this delegation arrive?" to Topic.Arrival,
                    "Sorry, but I need to head off." to Topic.Leave,
                )
            when (topic) {
                Topic.Varlamore -> aboutVarlamore()
                Topic.Source -> aboutSource()
                Topic.Arrival -> {
                    delegationArrival()
                    return
                }
                Topic.Leave -> {
                    chatPlayer(neutral, "Sorry, but I need to head off.")
                    alina(neutral, "Fair enough.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.aboutVarlamore() {
        chatPlayer(quiz, "What can you tell me about Varlamore?")
        alina(
            neutral,
            "It's far across the western sea, over the mountains south of Great Kourend. " +
                "Between the sea and the mountains, it's totally isolated! No one is allowed to " +
                "come or go without good reason.",
        )
        noah(quiz, "Sounds like a prison.")
        alina(
            happy,
            "Quite the opposite! It's a beautiful and prosperous kingdom, ruled over by the Sun " +
                "Queen!",
        )
        chatPlayer(quiz, "The Sun Queen?")
        alina(
            happy,
            "Yes! She rules from her palace in their capital, Civitas illa Fortis. The sun " +
                "plays an important role in Varlamorian life. In fact, they worship it!",
        )
        noah(confused, "What... the sun?")
        alina(
            happy,
            "That's right! They have their own religion over there dedicated to the sun and the " +
                "moon. Isn't that fascinating?",
        )
        noah(worried, "I don't know... It all sounds a bit odd to me.")
    }

    private suspend fun Dialogue.aboutSource() {
        chatPlayer(quiz, "How come you know so much about Varlamore?")
        alina(
            neutral,
            "A while back, a group of travellers came through here, on their way to the dark " +
                "lands of Morytania. They said they were from the College of Bards!",
        )
        noah(quiz, "College of Bards?")
        alina(
            happy,
            "Yes! It's found in the floating city of Tempestus! Members of the College are some " +
                "of the only people allowed to travel to and from Varlamore.",
        )
        alina(
            happy,
            "They stayed the night in the Blue Moon Inn and spent hours telling us all about " +
                "their homeland. I've been dying to visit ever since!",
        )
    }

    private suspend fun Dialogue.delegationArrival() {
        chatPlayer(quiz, "When will this delegation arrive?")
        alina(happy, "Well it should be any time now!")
        noah(confused, "Hang on... what's going on over there?")
        alina(shocked, "Wait! That's them! They're here!")
        scenes.delegation(access)
        quest.setQuestStage(access, CotsStage.Tailing)
        tails.start(access)
    }

    internal suspend fun Dialogue.tobynConversation() {
        when (stage(player)) {
            CotsStage.Tailing,
            CotsStage.Eavesdropping -> tobyn(neutral, "Move along, citizen.")
            CotsStage.ReportToTobyn -> reportBandits()
            CotsStage.Marking -> markingStage()
            CotsStage.OnRoof,
            CotsStage.Interrogating,
            CotsStage.Interrogated,
            CotsStage.ItzlaLeft -> roofConversation()
            CotsStage.Complete -> afterQuest()
        }
    }

    internal suspend fun Dialogue.itzlaConversation() {
        when (stage(player)) {
            CotsStage.OnRoof,
            CotsStage.Interrogating,
            CotsStage.Interrogated -> roofConversation()
        }
    }

    private suspend fun Dialogue.reportBandits() {
        tobyn(neutral, "Move along, citizen.")
        chatPlayer(shocked, "Wait! I have some information that you need to hear.")
        tobyn(quiz, "What is it?")
        chatPlayer(
            worried,
            "There are some bandits disguising themselves as guards! I think they're planning " +
                "to attack the delegation from Varlamore!",
        )
        tobyn(confused, "Surely you can't be serious?")
        chatPlayer(
            worried,
            "It's true! One of them infiltrated your guards and stole some uniforms. He came " +
                "right through here earlier with them.",
        )
        tobyn(
            quiz,
            "The one with the large bag? I thought I didn't recognise him, but then it's so " +
                "hard to keep up. We get through guards so quickly around here for some reason...",
        )
        chatPlayer(quiz, "So what do we do? Should we warn the delegation?")
        tobyn(
            neutral,
            "And risk a panic? No. Better we deal with this now while they're safe in the " +
                "palace. Do you know how many bandits there are?",
        )
        chatPlayer(neutral, "Four in total.")
        tobyn(
            neutral,
            "Right, we can work with that. You get out there and point them out. With them " +
                "being disguised, you'll need to keep a close eye out for any guard who looks " +
                "or acts differently to the others.",
        )
        tobyn(
            neutral,
            "Don't go too far. If the bandits intend to attack the delegation, they'll either " +
                "be here in the square or on the eastern road between the square and the bank.",
        )
        tobyn(
            neutral,
            "Return to me once you've found all four and we'll take them all at the same time.",
        )
        chatPlayer(neutral, "Alright, I'll get to it.")
        access.setStage(CotsStage.Marking)
    }

    private suspend fun Dialogue.markingStage() {
        if (markedCount(player) < CotsNpc.BanditGuards) {
            tobyn(
                neutral,
                "Get out there and point out the four bandits. They're in disguise, so be sure " +
                    "to keep a close eye out for any guard who looks or acts differently to the " +
                    "others.",
            )
            tobyn(
                neutral,
                "Remember, if they intend to attack the delegation, they'll either be here in " +
                    "the square or on the eastern road between the square and the bank.",
            )
            chatPlayer(neutral, "Alright, I'll be back soon.")
            return
        }
        chatPlayer(happy, "Alright, I've pointed out all the bandits.")
        if (!markedCorrectly(player)) {
            tobyn(
                confused,
                "Are you sure? I definitely recognise at least one of them as one of my " +
                    "guards. Maybe you should try again.",
            )
            return
        }
        tobyn(happy, "Good work. I'll signal my guards to bring them in...")
        mesbox("Sergeant Tobyn signals to his guards and the bandits are quickly arrested.")
        tobyn(
            happy,
            "That could have ended up quite nasty if not for you. Once again, good work.",
        )
        chatPlayer(quiz, "So what happens now?")
        tobyn(
            neutral,
            "The bandits will be interrogated. If all goes well, we'll get some useful " +
                "information out of them.",
        )
        tobyn(
            quiz,
            "Speaking of which... given it was you who found them in the first place, you might " +
                "be able to offer some support. Would you be willing to help?",
        )
        chatPlayer(happy, "Of course! Where do you need me?")
        tobyn(
            neutral,
            "Come with me. We'll use the cell on the palace roof for the interrogations. It's " +
                "nice and out of the way.",
        )
        mesbox("Sergeant Tobyn escorts you to the palace roof.")
        access.setStage(CotsStage.OnRoof)
        scenes.toRoof(access)
    }

    private suspend fun Dialogue.roofConversation() {
        val name = player.displayName
        when (stage(player)) {
            CotsStage.OnRoof -> {
                itzla(neutral, "Ah, nilsal, sergeant.")
                tobyn(
                    shocked,
                    "Prince Itzla! I... I'm sorry, but I wasn't expecting you here.",
                )
                itzla(
                    happy,
                    "Well all that politics downstairs was getting a bit dry, but then I heard " +
                        "there'd been a bit of a commotion. I thought I'd come see what the fuss " +
                        "was all about. You don't mind, do you?",
                )
                tobyn(worried, "Well no, it's just...")
                itzla(happy, "Wonderful! And you! Who are you?")
                chatPlayer(neutral, "Hello there. I'm $name.")
                itzla(
                    happy,
                    "$name? Kuaini! Pleased to meet you. Crown Prince Itzla Arkan, heir to the " +
                        "Sun Throne, at your service.",
                )
                itzla(quiz, "Tell me, $name, are you the one who caught these would-be assassins?")
                chatPlayer(neutral, "That's right.")
                itzla(
                    happy,
                    "Well then, why don't you and I ask this one a few little questions. You " +
                        "don't mind, do you, sergeant?",
                )
                tobyn(worried, "Not at all, but...")
                itzla(laugh, "Excellent! Let's get started then.")
                access.setStage(CotsStage.Interrogating)
                interrogate()
            }
            CotsStage.Interrogating -> interrogate()
            CotsStage.Interrogated -> afterInterrogation()
            CotsStage.ItzlaLeft -> finale()
        }
    }

    private suspend fun Dialogue.interrogate() {
        scenes.interrogation(access)
        access.setStage(CotsStage.Interrogated)
        afterInterrogation()
    }

    private suspend fun Dialogue.afterInterrogation() {
        val name = player.displayName
        itzla(happy, "I think we got what we needed there.")
        tobyn(quiz, "You're sure he told you everything?")
        itzla(
            laugh,
            "Oh yes. The fear of being cooked will do that to a person. All total rubbish of " +
                "course! Imagine burning someone alive! We're much more civilised over in " +
                "Varlamore.",
        )
        itzla(happy, "We just cut out their heart while it's still beating instead.")
        chatPlayer(confused, "I can't tell if you're joking or not...")
        itzla(
            neutral,
            "Anyway, sounds like there'll be some work to do when we get back home. An " +
                "assassination attempt on the Teokan of Ralos... Concerning stuff. First up " +
                "though, I'd better get back to the politics!",
        )
        itzla(
            happy,
            "$name, once we've got these papers signed, you lot will be free to travel to " +
                "Varlamore for the first time! No more needing explicit permission. You can " +
                "come and go as you please!",
        )
        itzla(
            quiz,
            "Now, it's entirely up to you of course, but given we have you to thank for " +
                "stopping this plot, I'd be very keen if you'd join me in Varlamore to continue " +
                "the investigation.",
        )
        itzla(
            neutral,
            "No need to decide now, but if you do wish to accept my offer, just speak to " +
                "Regulus Cento outside the eastern gates of this city.",
        )
        itzla(
            happy,
            "Now, they'll be missing me downstairs. Timoiva, and may the sun light your way!",
        )
        access.setStage(CotsStage.ItzlaLeft)
        mesbox("Itzla departs.")
        finale()
    }

    private suspend fun Dialogue.finale() {
        tobyn(neutral, "Well, that was interesting...")
        chatPlayer(
            quiz,
            "Not every day the heir to a far-off land comes and interrogates one of your " +
                "prisoners?",
        )
        tobyn(
            laugh,
            "Oddly enough, no. This will be an interesting report to write. And on that note, " +
                "I'd best get to it. All the best, adventurer. Thank you for your help.",
        )
        quest.completeQuest(access)
    }

    private suspend fun Dialogue.afterQuest() {
        tobyn(
            happy,
            "Good to see you, adventurer. I hope everything's going well for you out there.",
        )
        chatPlayer(happy, "Not bad, thanks.")
        tobyn(neutral, "Glad to hear it. Did you end up travelling to Varlamore?")
        chatPlayer(neutral, "Not yet.")
        tobyn(
            neutral,
            "Prince Itzla said one of his people would meet you just outside the eastern gates. " +
                "It might be worth heading over there.",
        )
        chatPlayer(happy, "I might just do that. Thanks!")
    }

    private class JournalStep(val from: Int, val until: Int, val text: String)

    private companion object {
        private const val RewardModel = "obj.sunfirerune"

        private const val BanditCount = CotsNpc.BanditGuards

        private const val SpottedTicks = 2

        private const val GuardHidden = 0
        private const val GuardUnmarked = 1
        private const val GuardMarked = 2

        private val JournalSteps =
            listOf(
                JournalStep(
                    CotsStage.Started,
                    CotsStage.Started,
                    "I should talk to <red>Alina</red> or <red>Noah</red> in <red>Varrock " +
                        "Square</red> and ask when the <red>delegation</red> arrives.",
                ),
                JournalStep(
                    CotsStage.Tailing,
                    CotsStage.Tailing,
                    "A <red>guard</red> carrying a very large bag has slipped out of the palace. " +
                        "I should follow him to south-east Varrock without being noticed. If he " +
                        "turns around I need to be <red>out of his sight</red>, but I mustn't " +
                        "fall too far behind. If I fail, <red>Noah</red> can help me try again.",
                ),
                JournalStep(
                    CotsStage.Eavesdropping,
                    CotsStage.ReportToTobyn,
                    "I overheard four <red>bandits</red> planning to attack the delegation, " +
                        "disguised as <red>Varrock guards</red>. I should tell <red>Sergeant " +
                        "Tobyn</red> in <red>Varrock Square</red>.",
                ),
                JournalStep(
                    CotsStage.Marking,
                    CotsStage.Marking,
                    "<red>Sergeant Tobyn</red> wants me to <red>Mark</red> the four disguised " +
                        "bandits among the guards in the <red>square</red> and on the " +
                        "<red>eastern road</red> to the bank. Once I've marked all four, I " +
                        "should report back to him.",
                ),
                JournalStep(
                    CotsStage.OnRoof,
                    CotsStage.Interrogated,
                    "The bandits have been arrested. <red>Sergeant Tobyn</red> has taken me to " +
                        "the cell on the <red>palace roof</red>, where <red>Prince Itzla " +
                        "Arkan</red> is helping to question them.",
                ),
                JournalStep(
                    CotsStage.ItzlaLeft,
                    CotsStage.ItzlaLeft,
                    "The prince has returned to the treaty talks. I should have a final word " +
                        "with <red>Sergeant Tobyn</red> on the palace roof.",
                ),
            )
    }
}
