package org.rsmod.content.quest.mudfoot

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onAiTimer
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.content.quest.manager.startQuestPrompt
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.hit.Hitmark
import org.rsmod.plugin.scripts.ScriptContext

class MudfootsBigAdventure
@Inject
constructor(
    private val follower: MudfootFollower,
    private val encounters: MudfootEncounters,
    private val storeCutscene: MudfootStoreCutscene,
    private val weapons: HarlanWeapons,
) :
    QuestScript(
        "quest_mudfootsbigadventure",
        "varp.mba_primary",
        rewards {
            item("obj.mba_lamp", LAMP_COUNT, label = "4 Antique Lamps")
            item("obj.coins", BOUNTY_SHARE, label = "500 Coins")
        },
        ItemRewardDisplay("obj.iron_scimitar", zoom = 400, x = 78, y = 128),
        questVarbit = "varbit.mba",
    ) {

    override fun ScriptContext.init() {
        check(quest.maxSteps == MudfootStage.COMPLETE) {
            "Mudfoot's Big Adventure ends at ${quest.maxSteps} in the cache, not ${MudfootStage.COMPLETE}."
        }
        follower.register()
        encounters.quest = quest
        storeCutscene.quest = quest
        weapons.quest = quest

        onEvent<SessionStateEvent.PrepareLogin> { restoreMudfootLogin(player) }
        onPlayerLogout { encounters.onLogout(player) }

        onOpNpc1(HARLAN) { startDialogue(it.npc) { harlan(it.npc) } }
        onOpNpc3(HARLAN) { startDialogue(it.npc) { with(weapons) { claimMenu() } } }

        onOpNpc1(MUDFOOT_VIS) { talkToMudfoot(it.npc) }
        onOpNpc1(MUDFOOT_SPEAR_VIS) { talkToMudfoot(it.npc) }

        onOpNpc1("npc.mba_outlaw_ranged_vis") { confrontStoreOutlaw() }
        onOpNpc1("npc.mba_outlaw_magic_vis") {
            confront("Look! There she is!", MudfootFight.Mage, MudfootStage.RANGER_DEAD)
        }
        onOpNpc1("npc.mba_outlaw_melee_vis") {
            confront("Look! That him!", MudfootFight.Warrior, MudfootStage.MAGE_DEAD)
        }

        onAiTimer("npc.mba_mudfoot_spear_combat") { encounters.tick(npc) }
        onAiTimer("npc.mba_mudfoot_rocks_combat") { encounters.tick(npc) }

        onOpHeld1("obj.mba_lamp") { rubLamp() }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Captain Harlan</col> north of " +
            "<col=800000>Lumbridge Castle</col>."

    override fun questLog(player: ProtectedAccess): String =
        questJournal(player) {
            val stage = quest.getQuestStage(player.player)
            val current = MudfootJournal.currentEntry(stage)
            for (index in MudfootJournal.ENTRIES.indices) {
                when {
                    index < current -> strike(MudfootJournal.ENTRIES[index])
                    index == current -> line(MudfootJournal.highlighted(index))
                }
            }
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) { MudfootJournal.ENTRIES.forEach(::line) }

    private fun stage(player: Player): Int = quest.getQuestStage(player)

    // Captain Harlan

    private suspend fun Dialogue.harlan(npc: Npc) {
        if (!player.harlanIntroduced) {
            player.harlanIntroduced = true
            chatNpc(
                quiz,
                "Greetings, adventurer. Captain Harlan, leader of the Lumbridge Guard, at your " +
                    "service. Is there anything I can do for you?",
            )
        } else {
            chatNpc(quiz, "Hello again, adventurer, Is there anything I can do for you?")
        }
        harlanMenu(npc)
    }

    private suspend fun Dialogue.harlanMenu(npc: Npc) {
        val stage = stage(player)
        val options = ArrayList<Pair<String, suspend Dialogue.() -> Unit>>()
        when {
            stage == MudfootStage.NOT_STARTED ->
                options += "Do you need help with anything?" to { offerQuest() }
            stage == MudfootStage.RETURN_TO_HARLAN ->
                options += THIEVES_DEALT_WITH to { thievesDealtWith() }
            stage == MudfootStage.MUDFOOT_LEFT ->
                options += THIEVES_DEALT_WITH to {
                    chatPlayer(happy, THIEVES_DEALT_WITH)
                    bountyPaid()
                }
            stage < MudfootStage.COMPLETE ->
                options += "So, about those thieves..." to { aboutThieves() }
        }
        options += GUARDS_OPTION to { otherGuards() }
        capeOption()?.let(options::add)
        if (stage >= MudfootStage.WINDMILL) {
            options += "Could you give me some combat advice?" to { combatAdvice() }
            options += "Do you have any weapons I could have?" to { with(weapons) { weaponsOffer() } }
        }
        options += "I'd better be off." to { betterBeOff() }
        val picked = chatMenu(options.map { it.first })
        options.getOrNull(picked - 1)?.second?.invoke(this)
    }

    private suspend fun Dialogue.offerQuest() {
        chatPlayer(quiz, "Do you need help with anything?")
        chatNpc(
            neutral,
            "Well, I do actually have a bounty available at the moment that you might be " +
                "interested in. 1,000 coins to whoever can bring justice to some thieves that " +
                "robbed the General Store.",
        )
        if (!startQuestPrompt(quest)) {
            chatPlayer(neutral, "Sorry, but I don't think I can help with that right now.")
            chatNpc(neutral, "Fair enough. Just let me know if you change your mind.")
            return
        }
        quest.setQuestStage(access, MudfootStage.STARTED)
        chatPlayer(quiz, "Sounds like a job for me. What do you know about these thieves?")
        chatNpc(
            neutral,
            "It happened in the night, so no one got a clear view, but a traveller passing " +
                "through reckons he saw some of those goblins on the other side of the river " +
                "playing with the stolen goods.",
        )
        chatPlayer(neutral, "I guess I'll start over there then.")
        quest.setQuestStage(access, MudfootStage.FIND_GOBLINS)
        player.mudfootVis = MudfootVis.GOBLIN_HOUSE
        chatNpc(neutral, "Good luck, adventurer.")
    }

    private suspend fun Dialogue.aboutThieves() {
        chatPlayer(quiz, "So, about those thieves...")
        val stage = stage(player)
        when {
            stage == MudfootStage.MET_HARLAN -> harlanDebrief()
            stage < MudfootStage.WINDMILL && follower.isFollowing(player) -> {
                if (!player.harlanMetMudfootEarly) {
                    player.harlanMetMudfootEarly = true
                    chatNpc(confused, "You seem to have a goblin with you. Is he one of them?")
                    chatPlayer(
                        neutral,
                        "No. It seems things might not be as simple as we first thought.",
                    )
                } else {
                    chatNpc(
                        confused,
                        "You still have that goblin with you. Have you worked out what's going " +
                            "on yet.",
                    )
                    chatPlayer(neutral, "No, but I'll let you know once I've learnt more.")
                }
            }
            stage < MudfootStage.WINDMILL -> {
                chatNpc(quiz, "Ah, yes. How goes your search?")
                chatPlayer(neutral, "I'm still looking into it.")
            }
            stage < MudfootStage.RANGER_DEAD ->
                chatNpc(
                    neutral,
                    "It seems you know what you need to do next. Good luck, adventurer.",
                )
            stage < MudfootStage.MAGE_DEAD -> {
                chatNpc(quiz, "Ah, yes. How goes your hunt?")
                chatPlayer(
                    neutral,
                    "The first one has been dealt with. We're now moving on to the second.",
                )
                chatNpc(neutral, "Ah, well you'll want to make sure you have a ranged weapon then.")
            }
            else -> {
                chatNpc(quiz, "Ah, yes. How goes your hunt?")
                chatPlayer(neutral, "The first two have been dealt with.")
                chatNpc(
                    neutral,
                    "Ah, well against a warrior, magic will work best, but you'll likely also " +
                        "need a second attack style to overcome his prayers.",
                )
            }
        }
    }

    private suspend fun Dialogue.otherGuards() {
        chatPlayer(quiz, GUARDS_OPTION)
        chatNpc(
            neutral,
            "Lumbridge is usually a peaceful place, so the King requested the Duke send all able " +
                "bodies up to help in Varrock instead.",
        )
        chatPlayer(quiz, "Doesn't that cause any problems?")
        chatNpc(neutral, "One or two come up now and then, but nothing I can't deal with.")
    }

    private suspend fun Dialogue.combatAdvice(label: String = "Could you give me some combat advice?") {
        chatPlayer(quiz, label)
        harlan(
            neutral,
            "There are three main types of combat: melee, ranged and magic. Melee and ranged need " +
                "a weapon, while magic can be cast without one, though a staff will help.",
        )
        chatPlayer(quiz, "And the three together make up the combat triangle?")
        harlan(
            neutral,
            "Exactly. Melee is strong against ranged, ranged against magic, and magic against " +
                "melee. It's not an absolute rule though, so don't rely on it completely.",
        )
        chatPlayer(quiz, "I see. And what about different weapon types?")
        harlan(
            neutral,
            "Melee weapons crush, slash or stab, and most opponents are weaker to one of those " +
                "than the others. Each weapon also has attack options that train different skills.",
        )
        harlan(
            neutral,
            "Whatever you're fighting, bring some food. Practice is the best teacher, and your " +
                "Skills panel can guide you as well.",
        )
        chatPlayer(neutral, "I see. Thanks for the information.")
        harlan(neutral, "Anyway, I think that covers the basics.")
    }

    private suspend fun Dialogue.betterBeOff() {
        chatPlayer(neutral, "I'd better be off.")
        chatNpc(neutral, "Until next time, adventurer.")
    }

    private suspend fun Dialogue.harlanDebrief() {
        harlan(
            neutral,
            "Right, regarding these thieves... You said that your goblin friend wasn't able to " +
                "kill them?",
        )
        mudfoot(confused, "Me try hit them with big hammer, but it no work.")
        harlan(quiz, "I see. Did they have armour?")
        chatPlayer(neutral, "Yes, the armour of a ranger.")
        mudfoot(
            confused,
            "Yes, ranger. Other goblins tell me rangers die best when you hit them up close. That " +
                "what me do. Go close and big smack.",
        )
        harlan(
            quiz,
            "Ah... Tell me... Mudfoot... have you heard of something called the combat triangle?",
        )
        mudfoot(quiz, "Triangle? Like roof?")
        harlan(confused, "Yes, sometimes...")
        harlan(
            confused,
            "Anyway, when those goblin friends of yours told you that melee works best against a " +
                "ranger, that's what they were referring to.",
        )
        mudfoot(quiz, "But why triangle?")
        harlan(
            neutral,
            "Well, melee often works best against an opponent using ranged, ranged typically " +
                "works best when fighting an opponent using magic, and magic is normally the best " +
                "option when fighting someone using melee.",
        )
        mudfoot(confused, "But that what me do. Me do the close up hit. Why no work?")
        harlan(
            neutral,
            "The combat triangle isn't the only thing to consider when taking on a foe. The way " +
                "that you're attacking is also important. Take melee for example. You have " +
                "crushing attacks, slashing attacks and stabbing attacks.",
        )
        mudfoot(happy, "Oh, me like to do big crush best. That why me have big hammer.")
        harlan(
            neutral,
            "Indeed, but the armour typically worn by a ranger is quite resilient to crushing " +
                "attacks. You'll likely find you have better luck with a stabbing attack instead.",
        )
        harlan(
            neutral,
            "Of course, for that, you'll need the right weapon. As you've already noted, " +
                "something like a warhammer works well for crushing, but if your aim is to stab, a " +
                "weapon like a dagger or shortsword will work best.",
        )
        mudfoot(quiz, "So me go get big poker, then nasty bow man die?")
        harlan(
            neutral,
            "Well, there are no guarantees of course, but it will definitely improve your odds.",
        )
        mudfoot(
            happy,
            "Yes... Me know all this already of course. Me just pretended not to so trusty human " +
                "sidekick can do learning. Me know human afraid to ask for help.",
        )
        chatPlayer(disgruntled, "That's not-")
        mudfoot(happy, "It okay though. This why human sidekick and me in charge.")
        chatPlayer(disgruntled, "Hmm... Shall we go and find that outlaw and try again?")
        mudfoot(
            happy,
            "Yes! They go north towards that windmill. Me go grab poker and meet sidekick there. " +
                "You make sure you grab poker as well.",
        )
        harlan(
            neutral,
            "I can sort you out with a suitable weapon if needed, adventurer. Alternatively, the " +
                "local blacksmith should be able to sell you one, or give you some advice on " +
                "making your own.",
        )
        mudfoot(
            happy,
            "Weapon good. No steal one though. Stealing bad, as Mudfoot always say, apart from " +
                "when it okay, which isn't now.",
        )
        chatPlayer(confused, "Words to live by... Anything else we should grab before we do this?")
        harlan(neutral, "Given you're going into a fight, some food would be wise.")
        mudfoot(
            happy,
            "This true. Food make you feel better if you get hit, but me never get hit, so me " +
                "never need food.",
        )
        chatPlayer(confused, "If you say so.")
        mudfoot(happy, "Okay, time to go now.")
        mudfootDeparts {
            player.mudfootVis = MudfootVis.GOBLIN_HOUSE
            quest.setQuestStage(access, MudfootStage.WINDMILL)
            player.mudfootVis = MudfootVis.WINDMILL
            follower.stop(player)
        }
        harlan(neutral, "It seems you know what you need to do next. Good luck, adventurer.")
        val picked =
            chatMenu(
                listOf(
                    "Could you run me through combat basics again?",
                    "Could I have a weapon for stabbing?",
                    "Thanks. I'd best get going.",
                )
            )
        when (picked) {
            1 -> combatAdvice("Could you run me through combat basics again?")
            2 -> with(weapons) { stabbingWeapon() }
            3 -> chatPlayer(neutral, "Thanks. I'd best get going.")
        }
    }

    private suspend fun Dialogue.thievesDealtWith() {
        chatPlayer(happy, THIEVES_DEALT_WITH)
        if (!follower.isFollowing(player)) {
            chatNpc(
                happy,
                "Excellent! Why don't you go and fetch your goblin friend, and then we can " +
                    "discuss your reward.",
            )
            return
        }
        chatNpc(happy, "Excellent! Did you manage to work out why they did it?")
        chatPlayer(
            neutral,
            "It seems they were trying to stir up trouble and frame the goblins for it. At least " +
                "one of them was a H.A.M. supporter, or maybe a full member.",
        )
        chatNpc(
            suspecting,
            "I see... I can't say I agree with the kind of things H.A.M. believe, but they've " +
                "never done anything like this before. Let us hope it was a one-off rather than " +
                "anything more serious...",
        )
        chatNpc(
            neutral,
            "Anyway, that's one for me to worry about. Shall we get you sorted out with your " +
                "reward?",
        )
        mudfoot(
            happy,
            "Yes, reward good. Me also give back stolen items. Me take good care of them. Nothing " +
                "missing at all. Me check two times.",
        )
        doubleobjbox(
            "obj.tinderbox",
            "obj.shears",
            "Mudfoot gives Captain Harlan the items from the General Store.",
        )
        chatNpc(happy, "Many thanks. Now, the bounty was for 1,000 coins, so that's 500 coins each.")
        mudfoot(
            confused,
            "500? But me the boss. Human just trusty sidekick. Me should get more money. Bigger " +
                "money too. Boss always bigger than sidekick, so money be bigger to match.",
        )
        chatPlayer(disgruntled, "Mudfoot...")
        mudfoot(
            confused,
            "Hmm... But me suppose you do good job. Me actually quite proud of sidekick. Not as " +
                "proud as me am of me, but still close. One day you maybe be as good at quests as me.",
        )
        mudfoot(happy, "Okay, just this once, sidekick can have full 500.")
        objbox("obj.coins_250", 400, "Captain Harlan gives Mudfoot his half of the bounty.")
        mudfoot(
            confused,
            "Okay, me go now. Time to find new quest. Maybe me solve big mystery this time.",
        )
        chatPlayer(quiz, "What kind of mystery?")
        mudfoot(confused, "Me no know. That why it mystery. Bye now.")
        chatPlayer(happy, "See you around, Mudfoot.")
        mudfootDeparts {
            player.mudfootVis = MudfootVis.GRAVEYARD
            quest.setQuestStage(access, MudfootStage.MUDFOOT_LEFT)
            player.mudfootVis = MudfootVis.HIDDEN
            follower.stop(player)
        }
        bountyPaid()
    }

    private suspend fun Dialogue.bountyPaid() {
        if (player.inv.freeSpace() < REWARD_SLOTS) {
            chatNpc(
                neutral,
                "Here's your half of the bounty, along with some lamps, but you'll need to make " +
                    "some space in your inventory first, adventurer.",
            )
            return
        }
        chatNpc(
            happy,
            "Right then... Thanks for all of your help with those thieves, adventurer. Here's " +
                "your half of the bounty, along with some lamps that I think you'll find useful. " +
                "All the best!",
        )
        quest.completeQuest(access)
    }

    private suspend fun Dialogue.mudfootDeparts(change: () -> Unit) {
        mesboxNp(DEPARTS)
        access.fadeToBlack()
        delay(1)
        access.minimapHideMap()
        delay(2)
        change()
        delay(1)
        access.minimapReset()
        access.fadeOverlay(
            startColour = 0,
            startTransparency = 0,
            endColour = 0,
            endTransparency = 255,
            clientDuration = DEPARTS_FADE,
        )
        mesbox(DEPARTS)
        access.closeFadeOverlayNow()
    }

    // Mudfoot

    private suspend fun ProtectedAccess.talkToMudfoot(npc: Npc) {
        val stage = stage(player)
        when {
            npc.isType("npc.mba_mudfoot_ruined_building") -> goblinHouseMudfoot(npc, stage)
            npc.isType("npc.mba_mudfoot_windmill") -> windmillMudfoot(npc)
            npc.isType("npc.mba_mudfoot_woods") || npc.isType("npc.mba_mudfoot_graveyard") ->
                waitingMudfoot(npc)
            npc.isType("npc.mba_mudfoot_follower") || npc.isType("npc.mba_mudfoot_follower_spear") ->
                followingMudfoot(npc, stage)
        }
    }

    private suspend fun ProtectedAccess.goblinHouseMudfoot(npc: Npc, stage: Int) {
        if (stage >= MudfootStage.SIDEKICK) {
            waitingMudfoot(npc)
            return
        }
        startDialogue(npc) {
            swing(npc)
            chatNpc(angry, "Die, human, die!")
            chatPlayer(confused, "Er... What are you doing?")
            swing(npc)
            chatNpc(angry, "Me killing you.")
            chatPlayer(confused, "You're not doing a very good job...")
            chatNpc(angry, "Wrong. You dead now.")
            chatPlayer(confused, "Okay... And why are you killing me?")
            chatNpc(angry, "Me on quest.")
            chatPlayer(confused, "A quest?")
            chatNpc(
                angry,
                "Yes. Me going to be great goblin adventurer. Other goblins will call me Mudfoot " +
                    "the... good at quests.",
            )
            chatPlayer(confused, "And what is this quest you're on?")
            chatNpc(
                angry,
                "Me kill the nasty humans. Nasty humans rob other humans, but then they dump the " +
                    "stuff they rob here. Now we get blamed.",
            )
            chatPlayer(
                confused,
                "Wait, so it wasn't you goblins who robbed the General Store?",
            )
            chatNpc(
                neutral,
                "No. We goblins innocent. We never rob, apart from all the other times when we " +
                    "did, but this not those times. This time we no do it.",
            )
            chatNpc(neutral, "Look, me have proof.")
            doubleobjbox(
                "obj.tinderbox",
                "obj.shears",
                "Mudfoot shows you a collection of items from the General Store.",
            )
            chatNpc(
                neutral,
                "See. We keep things safe. No touch them at all, apart from when Damptoes try to " +
                    "eat chisel, but he no do that again now. Eating chisel make him throw up and " +
                    "die.",
            )
            chatPlayer(
                confused,
                "I see... Well, perhaps I can help you with your quest. I'm actually on a quest " +
                    "myself to find whoever robbed the General Store. Maybe we can work together?",
            )
            chatNpc(confused, "Hmm... me not really need help. Me very good at quest...")
            chatNpc(
                confused,
                "But me generous also. Maybe me let human join as sidekick. Human can learn " +
                    "thing or two from Mudfoot.",
            )
            quest.setQuestStage(access, MudfootStage.SIDEKICK)
            chatPlayer(confused, "That sounds... wonderful. Where do we begin?")
            player.mudfootFirstThiefDialogue = true
            chatNpc(
                neutral,
                "Other goblins see three humans who dump robbed stuff. We find all humans and " +
                    "make them dead. First one was man with bow. Other goblins see him go back " +
                    "across river. We find him there.",
            )
            chatPlayer(confused, "Alright, let's get going then.")
            joinPlayer()
        }
    }

    /** Mudfoot's harmless swing: the 0 splat is shown, not dealt, so the dialogue stays open. */
    private fun Dialogue.swing(npc: Npc) {
        npc.anim(HAMMER_ANIM)
        access.soundSynth(HAMMER_SOUND)
        val lit = ZERO_HIT_LIT.asRSCM(RSCMType.HITMARK)
        val tint = ZERO_HIT_TINT.asRSCM(RSCMType.HITMARK)
        player.showHitmark(
            Hitmark.fromNpcSource(lit, lit, tint, damage = 0, delay = SWING_HIT_DELAY, npc.slotId)
        )
    }

    private suspend fun ProtectedAccess.waitingMudfoot(npc: Npc) {
        startDialogue(npc) {
            chatPlayer(neutral, "Ready to continue, Mudfoot?")
            joinPlayer()
        }
    }

    private suspend fun Dialogue.joinPlayer() {
        if (follower.hasOtherFollower(player)) {
            mudfoot(neutral, "You pick up follower first. Me no follow you otherwise.")
            return
        }
        follower.start(player, follower.spear(stage(player)))
        chatNpcSpecific(
            MUDFOOT_TITLE,
            if (follower.spear(stage(player))) "npc.mba_mudfoot_follower_spear" else "npc.mba_mudfoot_follower",
            neutral,
            "Okay. Me behind you.",
        )
    }

    private suspend fun ProtectedAccess.windmillMudfoot(npc: Npc) {
        val stage = stage(player)
        if (stage > MudfootStage.RANGER_FIGHT) {
            waitingMudfoot(npc)
            return
        }
        startDialogue(npc) { chatNpc(neutral, "Look. Nasty human over there. Kill time now.") }
        with(encounters) { startFight(MudfootFight.Ranger) }
    }

    private suspend fun ProtectedAccess.followingMudfoot(npc: Npc, stage: Int) {
        if (!follower.isFollowerOf(npc, player)) {
            return
        }
        startDialogue(npc) {
            when (stage) {
                MudfootStage.RANGER_DEAD -> {
                    mageBriefing(quest)
                    return@startDialogue
                }
                MudfootStage.MAGE_DEAD -> {
                    warriorBriefing(quest)
                    return@startDialogue
                }
                MudfootStage.WARRIOR_DEAD -> {
                    returnToHarlan(quest)
                    return@startDialogue
                }
            }
            val whereTo =
                choice2("Where to, Mudfoot?", true, "I need to do something else.", false)
            if (whereTo) {
                chatPlayer(quiz, "Where to, Mudfoot?")
                chatNpc(neutral, whereToLine(stage))
                return@startDialogue
            }
            chatPlayer(neutral, "I need to do something else.")
            chatNpc(
                neutral,
                "Okay. Me meet you back ${MudfootFollower.waitingSpotName(stage)} when you done.",
            )
            follower.stop(player)
            player.mudfootVis = MudfootFollower.waitingSpot(stage)
        }
    }

    private fun whereToLine(stage: Int): String =
        when {
            stage < MudfootStage.WINDMILL ->
                "Other goblins see man with bow go back across river from goblin house. We find " +
                    "him there."
            stage < MudfootStage.MAGE_DEAD ->
                "Other goblins say they see her by ruined building, west of shop."
            stage < MudfootStage.WARRIOR_DEAD -> "Other goblins see him down in graveyard."
            else -> "Guard man will want to know about good job."
        }

    // Outlaws

    private suspend fun ProtectedAccess.confrontStoreOutlaw() {
        if (stage(player) != MudfootStage.SIDEKICK) {
            return
        }
        if (!follower.isFollowing(player)) {
            startDialogue { chatPlayer(confused, "Hmm... I should go and get Mudfoot first.") }
            return
        }
        val played = with(storeCutscene) { play() }
        if (played) {
            startDialogue { harlanDebrief() }
        }
    }

    private suspend fun ProtectedAccess.confront(shout: String, fight: MudfootFight, from: Int) {
        val stage = stage(player)
        if (stage < from || stage > fight.fightStage) {
            return
        }
        val mudfoot = follower.follower(player)
        if (mudfoot == null) {
            startDialogue { chatPlayer(confused, "Hmm... I should go and get Mudfoot first.") }
            return
        }
        mudfoot.say(shout)
        player.mes("$MUDFOOT_TITLE|$shout", ChatType.NpcSay)
        with(encounters) { startFight(fight) }
    }

    // Lamps

    private suspend fun ProtectedAccess.rubLamp() {
        val stat = chooseLampStat() ?: return
        if (invDel(inv, LAMP, 1).failure) {
            return
        }
        player.mudfootLampsUsed = (player.mudfootLampsUsed + 1).coerceAtMost(MAX_LAMPS_COUNTED)
        statAdvance(stat.first, LAMP_XP)
        mesbox(
            "<col=0000ff>Your wish has been granted!</col><br>You have been awarded " +
                "${LAMP_XP.toInt()} ${stat.second} experience!"
        )
    }

    private suspend fun ProtectedAccess.chooseLampStat(): Pair<String, String>? {
        while (true) {
            val first =
                choice5(
                    "Attack.",
                    "stat.attack" to "Attack",
                    "Strength.",
                    "stat.strength" to "Strength",
                    "Defence.",
                    "stat.defence" to "Defence",
                    "Ranged.",
                    "stat.ranged" to "Ranged",
                    "More options...",
                    null,
                    title = LAMP_TITLE,
                )
            if (first != null) {
                return first
            }
            val second =
                choice5(
                    "Magic.",
                    "stat.magic" to "Magic",
                    "Hitpoints.",
                    "stat.hitpoints" to "Hitpoints",
                    "Prayer.",
                    "stat.prayer" to "Prayer",
                    "Cancel.",
                    CANCEL,
                    "Previous options...",
                    null,
                    title = LAMP_TITLE,
                )
            if (second == CANCEL) {
                return null
            }
            if (second != null) {
                return second
            }
        }
    }

    private companion object {
        const val LAMP_COUNT = 4
        const val MAX_LAMPS_COUNTED = 7
        const val BOUNTY_SHARE = 500
        const val REWARD_SLOTS = 5
        const val LAMP = "obj.mba_lamp"
        const val LAMP_XP = 500.0
        const val LAMP_TITLE = "Choose the skill you wish to gain XP in."
        val CANCEL = "" to ""

        const val THIEVES_DEALT_WITH = "All of those thieves have been dealt with."
        const val GUARDS_OPTION = "If you lead the Lumbridge Guard, where are the other guards?"
        const val DEPARTS = "Mudfoot departs."
        const val DEPARTS_FADE = 50

        const val ZERO_HIT_LIT = "hitmark.zero_damage_lit"
        const val ZERO_HIT_TINT = "hitmark.zero_damage_tint"
        const val SWING_HIT_DELAY = 30
        const val HAMMER_ANIM = "seq.slice_surface_goblin_armed_attack"
        const val HAMMER_SOUND = "synth.goblin_warhammer_attack"
    }
}
