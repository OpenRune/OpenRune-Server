package org.rsmod.content.quest.area.varrock.demonslayer

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.chooseLampSkill
import org.rsmod.content.quest.manager.menu
import org.rsmod.content.quest.manager.rewards
import org.rsmod.content.quest.manager.startQuestPrompt
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.ScriptContext

class DemonSlayerQuest
@Inject
constructor(private val objRepo: ObjRepository, private val scenes: DemonSlayerScenes) :
    QuestScript(
        "quest_demonslayer",
        "varp.demonstart",
        rewards { extra("Silverlight") },
        ItemRewardDisplay(SILVERLIGHT, zoom = 400),
        questVarbit = "varbit.demonslayer_main",
    ) {

    private var Player.traibornBones by intVarBit("varbit.demon_slayer_traiborn_bones")
    private var Player.traibornAsked by intVarBit("varbit.demon_slayer_traiborn_asked")
    private var Player.lampsGiven by intVarBit("varbit.demon_slayer_lamps_given")

    override fun ScriptContext.init() {
        check(quest.maxSteps == STAGE_COMPLETE) {
            "Demon Slayer end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the script completes at $STAGE_COMPLETE."
        }
        onOpNpc1("npc.aris") { startDialogue(it.npc) { aris() } }
        onOpNpc1("npc.sir_prysin") { startDialogue(it.npc) { prysin() } }
        onOpNpc1("npc.captain_rovin") { startDialogue(it.npc) { rovin() } }
        onOpHeld1(LAMP) { rubLamp() }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Aris<col=000080> in her tent in <col=800000>Varrock " +
            "Square<col=000080>."

    override fun questLog(player: ProtectedAccess) =
        questJournal(player) {
            val p = player.player
            val stage = stage(p)
            strike("I spoke to Aris in Varrock Square who saw my future.")
            strike("Unfortunately it involved killing a demon who nearly")
            strike("destroyed Varrock over 150 years ago.")
            if (stage < STAGE_KEYS) {
                line("To defeat the <red>demon</red> I need the magical sword <red>Silverlight.</red>")
                line("I should ask <red>Sir Prysin</red> in <red>Varrock Palace</red> where it is.")
                return@questJournal
            }
            if (hasCollectedSilverlight(p)) {
                strike("I reclaimed the magical sword Silverlight from Sir Prysin.")
                line("Now I should go to the stone circle south of the city and")
                line("destroy <red>Delrith</red> using <red>Silverlight</red>!")
                return@questJournal
            }
            if (KEYS.all { p.inv.count(it) > 0 }) {
                line("Now I have <red>all 3 keys</red> I should go and speak to <red>Sir Prysin</red>")
                line("and collect the magical sword <red>Silverlight</red> from him.")
                return@questJournal
            }
            line("<red>Sir Prysin</red> needs <red>3 keys</red> before he can give me <red>Silverlight.</red>")
            if (p.inv.count(DRAIN_KEY) > 0) {
                strike("I have the 1st Key with me.")
            } else {
                line("The <red>1st Key</red> was dropped down the palace kitchen drains.")
            }
            if (p.inv.count(ROVIN_KEY) > 0) {
                strike("I have the 2nd Key with me.")
            } else {
                line("The <red>2nd Key</red> is with Captain Rovin in Varrock Palace.")
            }
            if (p.inv.count(TRAIBORN_KEY) > 0) {
                strike("I have the 3rd key with me.")
            } else {
                line("The <red>3rd Key</red> is with Wizard Traiborn at the Wizards' Tower.")
                if (p.traibornAsked == 1) {
                    line("<red>Traiborn</red> needs <red>${BONES_NEEDED - p.traibornBones}</red> more <red>bones.</red>")
                }
            }
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            strike("I spoke to Aris in Varrock Square who saw my future.")
            strike("Unfortunately it involved killing a demon who nearly")
            strike("destroyed Varrock over 150 years ago.")
            strike("I reclaimed the magical sword Silverlight from Sir Prysin.")
            strike("Using its power I managed to destroy the demon Delrith")
            strike("like the great hero Wally did many years before.")
        }

    fun stage(player: Player): Int = quest.getQuestStage(player)

    fun hasCollectedSilverlight(player: Player): Boolean =
        player.vars[SILVERLIGHT_CASE] == 1

    fun isReadyForDelrith(player: Player): Boolean =
        stage(player) == STAGE_KEYS && hasCollectedSilverlight(player)

    private suspend fun Dialogue.aris() {
        when (stage(player)) {
            0 -> arisFortune()
            STAGE_STARTED -> arisStarted()
            STAGE_KEYS ->
                if (hasCollectedSilverlight(player)) arisWithSword() else arisFoundPrysin()
            else -> if (player.lampsGiven == 0) arisLamps() else arisPostQuest()
        }
    }

    private suspend fun Dialogue.arisFortune() {
        chatNpc(neutral, "Hello young one. Cross my palm with silver and the future will be " +
            "revealed to you.")
        if (access.inv.count(COINS) == 0) {
            chatPlayer(sad, "Oh dear. I don't have any money.")
            return
        }
        if (player.combatLevel < RECOMMENDED_COMBAT) {
            mesbox(
                "Before starting this quest, be aware that your combat level is lower than the " +
                    "recommended level of $RECOMMENDED_COMBAT."
            )
        }
        if (!startQuestPrompt(quest)) {
            chatPlayer(neutral, "No, I don't believe in that stuff.")
            chatNpc(neutral, "Ok suit yourself.")
            return
        }
        chatPlayer(happy, "Okay, here you go.")
        if (access.invDel(access.inv, COINS, 1).failure) {
            return
        }
        scenes.crystalBallStart(access, checkNotNull(npc))
        chatNpc(neutral, "Come closer, and listen carefully to what the future holds for you, " +
            "as I peer into the swirling mists of the crystal ball.")
        scenes.crystalBallGaze(checkNotNull(npc))
        chatNpc(neutral, "I can see images forming. I can see you.")
        scenes.crystalBallGaze(checkNotNull(npc))
        chatNpc(neutral, "You are holding a very impressive looking sword. I'm sure I " +
            "recognise that sword...")
        scenes.crystalBallGaze(checkNotNull(npc))
        chatNpc(worried, "There is a big dark shadow appearing now.")
        scenes.crystalBallEnd(access, checkNotNull(npc))
        chatNpc(shocked, "Aaargh!")
        chatPlayer(worried, "Are you all right?")
        chatNpc(shocked, "It's Delrith! Delrith is coming!")
        chatPlayer(quiz, "Who's Delrith?")
        chatNpc(neutral, "Delrith...")
        chatNpc(neutral, "Delrith is a powerful demon.")
        chatNpc(worried, "Oh! I really hope he didn't see me looking at him through my crystal " +
            "ball!")
        chatNpc(neutral, "He tried to destroy this city 150 years ago. He was stopped just in " +
            "time by the great hero Wally.")
        chatNpc(neutral, "Using his magic sword Silverlight, Wally managed to trap the demon in " +
            "the stone circle just south of this city.")
        chatNpc(shocked, "Ye gods! Silverlight was the sword you were holding in my vision! You " +
            "are the one destined to stop the demon this time.")
        arisLore()
    }

    private suspend fun Dialogue.arisLore() {
        var last: ArisTopic? = null
        while (true) {
            val pool = listOf(ArisTopic.Fight, ArisTopic.KillHim, ArisTopic.Wally, ArisTopic.HowDid)
            val topics = if (last == null) pool.take(3) else pool - last
            val topic = menu(topics.map { it.option to it }, title = SAY_TITLE)
            if (topic == ArisTopic.HowDid) {
                break
            }
            arisTopic(topic)
            last = topic
        }
        chatPlayer(quiz, ArisTopic.HowDid.option)
        scenes.wallyFlashback(access, this@DemonSlayerQuest)
        if (stage(player) == 0) {
            quest.setQuestStage(access, STAGE_STARTED)
        }
        chatNpc(neutral, "Delrith will come forth from the stone circle again.")
        chatNpc(neutral, "I would imagine an evil sorcerer is already starting on the rituals " +
            "to summon Delrith as we speak.")
        var next: ArisTopic? = null
        while (true) {
            val topics =
                if (next == null) {
                    listOf(ArisTopic.Fight, ArisTopic.KillHim, ArisTopic.Incantation, ArisTopic.Where)
                } else {
                    listOf(
                        ArisTopic.Fight,
                        ArisTopic.KillHim,
                        ArisTopic.Wally,
                        ArisTopic.Incantation,
                        ArisTopic.Where,
                        ArisTopic.Thanks,
                    ) - next
                }
            val topic = menu(topics.map { it.option to it }, title = SAY_TITLE)
            if (topic == ArisTopic.Thanks) {
                chatPlayer(happy, "Ok thanks. I'll do my best to stop the demon.")
                chatNpc(happy, "Good luck, and may Guthix be with you!")
                return
            }
            if (topic == ArisTopic.Incantation) {
                chatPlayer(quiz, ArisTopic.Incantation.option)
                chatNpc(neutral, "Oh yes, let me think a second...")
                chatNpc(neutral, "Alright, I think I've got it now, it goes.... " +
                    "${incantation(player, ". ")}. Have you got that?")
                chatPlayer(neutral, "I think so, yes.")
            } else {
                arisTopic(topic)
            }
            next = topic
        }
    }

    private suspend fun Dialogue.arisTopic(topic: ArisTopic) {
        when (topic) {
            ArisTopic.Fight -> {
                chatPlayer(shocked, "How am I meant to fight a demon who can destroy cities?!")
                chatNpc(neutral, "If you face Delrith while he is still weak from being " +
                    "summoned, and use the correct weapon, you will not find the task too " +
                    "arduous.")
                chatNpc(neutral, "Do not fear. If you follow the path of the great hero Wally, " +
                    "then you are sure to defeat the demon.")
            }
            ArisTopic.KillHim -> {
                chatPlayer(angry, "Okay, where is he? I'll kill him for you!")
                chatNpc(laugh, "Ah, the overconfidence of the young!")
                chatNpc(neutral, "Delrith can't be harmed by ordinary weapons. You must face " +
                    "him using the same weapon that Wally used.")
            }
            ArisTopic.Wally -> {
                chatPlayer(quiz, "Wally doesn't sound a very heroic name.")
                chatNpc(neutral, "Yes I know. Maybe that is why history doesn't remember him. " +
                    "However he was a very great hero.")
                chatNpc(neutral, "Who knows how much pain and suffering Delrith would have " +
                    "brought forth without Wally to stop him!")
                chatNpc(neutral, "It looks like you are going to need to perform similar " +
                    "heroics.")
            }
            ArisTopic.Where -> arisWhereIsSilverlight()
            else -> Unit
        }
    }

    private suspend fun Dialogue.arisWhereIsSilverlight() {
        chatPlayer(quiz, "Where can I find Silverlight?")
        chatNpc(neutral, "Silverlight has been passed down through Wally's descendants. I " +
            "believe it is currently in the care of one of the King's knights called Sir " +
            "Prysin.")
        chatNpc(neutral, "He shouldn't be too hard to find. He lives in the royal palace in " +
            "this city. Tell him Aris sent you.")
    }

    private suspend fun Dialogue.arisStarted() {
        chatNpc(neutral, "Greetings. How goes thy quest?")
        chatPlayer(neutral, "I'm still working on it.")
        chatNpc(neutral, "Well if you need any advice I'm always here, young one.")
        while (true) {
            val topic =
                menu(
                    ArisTopic.Incantation.option to ArisTopic.Incantation,
                    ArisTopic.Where.option to ArisTopic.Where,
                    "Stop calling me that!" to ArisTopic.StopCalling,
                    PRESS_ON to ArisTopic.Thanks,
                    title = SAY_TITLE,
                )
            when (topic) {
                ArisTopic.Incantation -> return arisIncantationThenLeave()
                ArisTopic.Where -> arisWhereIsSilverlight()
                ArisTopic.StopCalling -> return arisStopCalling()
                else -> return arisPressOn(PRESS_ON)
            }
        }
    }

    private suspend fun Dialogue.arisFoundPrysin() {
        chatNpc(neutral, "How goes the quest?")
        chatPlayer(neutral, "I found Sir Prysin. Unfortunately I haven't got the sword yet. " +
            "He's made it complicated for me.")
        chatNpc(neutral, "Ok, hurry, we haven't much time.")
        when (menu(ArisTopic.Incantation.option to true, PRESS_ON to false, title = SAY_TITLE)) {
            true -> arisIncantationThenLeave()
            false -> arisPressOn(PRESS_ON)
        }
    }

    private suspend fun Dialogue.arisWithSword() {
        chatNpc(neutral, "How goes the quest?")
        chatPlayer(happy, "I have the sword now. I just need to kill the demon, I think.")
        chatNpc(happy, "Yep, that's right.")
        val topic =
            menu(
                ArisTopic.Incantation.option to ArisTopic.Incantation,
                PRESS_ON_NO_STOP to ArisTopic.Thanks,
                "Where can I find the demon?" to ArisTopic.Where,
                title = SAY_TITLE,
            )
        when (topic) {
            ArisTopic.Incantation -> arisIncantationThenLeave()
            ArisTopic.Where -> {
                chatPlayer(quiz, "Where can I find the demon?")
                chatNpc(neutral, "Just head south and you should find the stone circle just " +
                    "outside the city gate.")
            }
            else -> arisPressOn(PRESS_ON_NO_STOP)
        }
    }

    private suspend fun Dialogue.arisIncantationThenLeave() {
        chatPlayer(quiz, ArisTopic.Incantation.option)
        chatNpc(neutral, "${incantation(player, ". ")}.")
        arisPressOn(PRESS_ON)
    }

    private suspend fun Dialogue.arisPressOn(line: String) {
        chatPlayer(neutral, line)
        chatNpc(neutral, "See you anon.")
    }

    private suspend fun Dialogue.arisStopCalling() {
        chatPlayer(angry, "Stop calling me that!")
        chatNpc(neutral, "In the scheme of things you are very young.")
        val askAge =
            menu(
                "Ok but how old are you?" to true,
                "Oh if it's in the scheme of things that's ok." to false,
                title = SAY_TITLE,
            )
        if (askAge) {
            chatPlayer(quiz, "Ok, but how old are you?")
            chatNpc(neutral, "Count the number of legs on the stools in the Blue Moon inn, and " +
                "multiply that number by seven.")
            chatPlayer(confused, "Er, yeah, whatever.")
        } else {
            chatPlayer(neutral, "Oh if it's in the scheme of things that's ok.")
            chatNpc(happy, "You show wisdom for one so young.")
        }
    }

    private suspend fun Dialogue.arisPostQuest() {
        chatNpc(happy, "Greetings young one.")
        chatNpc(happy, "You're a hero now. That was a good bit of demonslaying.")
        val topic =
            menu(
                "How do you know I killed it?" to 0,
                "Thanks." to 1,
                "Stop calling me that!" to 2,
                title = SAY_TITLE,
            )
        when (topic) {
            0 -> {
                chatPlayer(quiz, "How do you know I killed it?")
                chatNpc(neutral, "You forget. I'm good at knowing things.")
            }
            1 -> chatPlayer(happy, "Thanks.")
            else -> arisStopCalling()
        }
    }

    private suspend fun Dialogue.arisLamps() {
        chatNpc(happy, "I have a little something here for you, young one. Consider it a gift " +
            "for slaying that demon.")
        if (!giveLamps(access)) {
            mesbox("You don't have enough inventory space.")
            return
        }
        chatPlayer(quiz, "How do you know I killed it?")
        chatNpc(neutral, "You forget. I'm good at knowing things.")
    }

    fun giveLamps(access: ProtectedAccess): Boolean {
        val player = access.player
        if (player.lampsGiven == 1) {
            return false
        }
        val added =
            player.invTransaction(player.inv) {
                val inv = select(player.inv)
                insert {
                    into = inv
                    obj = LAMP.asRSCM(RSCMType.OBJ)
                    strictCount = LAMP_COUNT
                }
            }
        if (added.failure) {
            return false
        }
        player.lampsGiven = 1
        return true
    }

    private suspend fun ProtectedAccess.rubLamp() {
        val stat = chooseLampSkill(LAMP_TITLE, stats = LAMP_STATS) ?: return
        if (invDel(inv, LAMP).failure) {
            return
        }
        statAdvance(stat, LAMP_XP)
        mes("Your wish has been granted!")
    }

    private suspend fun Dialogue.prysin() {
        val stage = stage(player)
        when {
            stage == STAGE_STARTED -> prysinIntro(onQuest = true)
            stage == STAGE_KEYS && !hasCollectedSilverlight(player) -> prysinKeys()
            stage == STAGE_KEYS -> prysinBeforeDelrith()
            else -> prysinIntro(onQuest = false)
        }
    }

    private suspend fun Dialogue.prysinIntro(onQuest: Boolean) {
        chatNpc(neutral, "Hello, who are you?")
        val options = buildList {
            add("I am a mighty adventurer. Who are you?" to 0)
            add("I'm not sure, I was hoping you could tell me." to 1)
            if (onQuest) add("Aris said I should come and talk to you." to 2)
        }
        when (menu(options)) {
            0 -> {
                chatPlayer(neutral, "I am a mighty adventurer, who are you?")
                chatNpc(neutral, "I am Sir Prysin. A bold and famous knight of the realm.")
            }
            1 -> {
                chatPlayer(confused, "I was hoping you could tell me.")
                chatNpc(confused, "Well I've never met you before.")
            }
            else -> prysinArisSentMe()
        }
    }

    private suspend fun Dialogue.prysinArisSentMe() {
        chatPlayer(neutral, "Aris said I should come and talk to you.")
        chatNpc(quiz, "Aris? Is she still alive? I remember her from when I was pretty young. " +
            "Well what do you need to talk to me about?")
        if (!menu("I need to find Silverlight." to true, "Yes, she is still alive." to false)) {
            chatPlayer(neutral, "Yes she is still alive. She lives right outside the castle!")
            chatNpc(shocked, "Oh, is that the same Aris? I would have thought she would have " +
                "died by now. She was pretty old when I was a lad.")
            chatNpc(neutral, "Anyway, what can I do for you?")
        }
        chatPlayer(neutral, "I need to find Silverlight.")
        chatNpc(quiz, "What do you need to find that for?")
        chatPlayer(neutral, "I need it to fight Delrith.")
        chatNpc(shocked, "Delrith? I thought the world was rid of him, thanks to my " +
            "great-grandfather.")
        val crystalBall =
            menu(
                "Well, Aris' crystal ball seems to think otherwise." to true,
                "He's back and unfortunately I've got to deal with him." to false,
            )
        if (crystalBall) {
            chatPlayer(neutral, "Well Aris' crystal ball seems to think otherwise.")
            chatNpc(neutral, "Well if the ball says so, I'd better help you.")
        } else {
            chatPlayer(neutral, "He's back and unfortunately I've got to deal with him.")
            chatNpc(neutral, "You don't look up to much. I suppose Silverlight may be good " +
                "enough to carry you through though.")
        }
        chatNpc(neutral, "The problem is getting Silverlight.")
        chatPlayer(quiz, "You mean you don't have it?")
        chatNpc(neutral, "Oh I do have it, but it is so powerful that the king made me put it " +
            "in a special box which needs three different keys to open it. That way it won't " +
            "fall into the wrong hands.")
        val giveKeys =
            menu("So give me the keys!" to true, "And why is this a problem?" to false)
        if (giveKeys) {
            chatPlayer(angry, "So give me the keys!")
            chatNpc(worried, "Um, well, it's not so easy.")
        } else {
            chatPlayer(quiz, "And why is this a problem?")
        }
        prysinKeyHolders()
        if (stage(player) == STAGE_STARTED) {
            quest.setQuestStage(access, STAGE_KEYS)
        }
        prysinKeyMenu(asked = null)
    }

    private suspend fun Dialogue.prysinKeyHolders() {
        chatNpc(neutral, "I kept one of the keys. I gave the other two to other people for " +
            "safe keeping.")
        chatNpc(neutral, "One I gave to Rovin, the captain of the palace guard.")
        chatNpc(neutral, "I gave the other to the wizard Traiborn.")
    }

    private suspend fun Dialogue.prysinKeyMenu(asked: PrysinTopic?) {
        var last = asked
        while (true) {
            val options = buildList {
                if (last != PrysinTopic.YourKey) add("Can you give me your key?" to PrysinTopic.YourKey)
                if (last != PrysinTopic.Rovin) add("Where can I find Captain Rovin?" to PrysinTopic.Rovin)
                if (last != PrysinTopic.Wizard) add("Where does the wizard live?" to PrysinTopic.Wizard)
                if (last != null) add("Well I'd better go key hunting." to PrysinTopic.Leave)
            }
            val topic = menu(options)
            when (topic) {
                PrysinTopic.YourKey -> {
                    chatPlayer(quiz, "Can you give me your key?")
                    chatNpc(worried, "Um.... ah....")
                    chatNpc(worried, "Well there's a problem there as well.")
                    chatNpc(sad, "I managed to drop the key in the drain just outside the palace " +
                        "kitchen. It is just inside and I can't reach it.")
                    val drain =
                        menu(
                            "So what does the drain lead to?" to PrysinTopic.Drain,
                            "Where can I find Captain Rovin?" to PrysinTopic.Rovin,
                            "Where does the wizard live?" to PrysinTopic.Wizard,
                        )
                    if (drain == PrysinTopic.Drain) {
                        chatPlayer(quiz, "So what does the drain connect to?")
                        chatNpc(neutral, "It is the drain for the drainpipe running from the " +
                            "sink in the kitchen down to the palace sewers.")
                        last = PrysinTopic.YourKey
                        continue
                    }
                    prysinTopic(drain)
                    last = drain
                }
                PrysinTopic.Leave -> {
                    chatPlayer(neutral, "Well I'd better go key hunting.")
                    chatNpc(neutral, "Ok, goodbye.")
                    return
                }
                else -> {
                    prysinTopic(topic)
                    last = topic
                }
            }
        }
    }

    private suspend fun Dialogue.prysinTopic(topic: PrysinTopic) {
        if (topic == PrysinTopic.Rovin) {
            chatPlayer(quiz, "Where can I find Captain Rovin?")
            chatNpc(neutral, "Captain Rovin lives at the top of the guards' quarters in the " +
                "north-west wing of this palace.")
        } else {
            chatPlayer(quiz, "Where does the wizard live?")
            chatNpc(neutral, "Wizard Traiborn?")
            chatNpc(neutral, "He is one of the wizards who lives in the tower on the little " +
                "island just off the south coast. I believe his quarters are on the first floor " +
                "of the tower.")
        }
    }

    private suspend fun Dialogue.prysinKeys() {
        chatNpc(quiz, "So how are you doing with getting the keys?")
        val traiborn = access.inv.count(TRAIBORN_KEY) > 0
        val rovin = access.inv.count(ROVIN_KEY) > 0
        val drain = access.inv.count(DRAIN_KEY) > 0
        if (traiborn && rovin && drain) {
            chatPlayer(happy, "I've got all three keys!")
            chatNpc(happy, "Excellent! Now I can give you Silverlight.")
            scenes.silverlightHandover(access, checkNotNull(npc), this@DemonSlayerQuest)
            return
        }
        val line =
            when {
                traiborn && rovin -> "I've got the keys from Wizard Traiborn and Captain Rovin."
                rovin && drain ->
                    "I've got the key from Captain Rovin and the one that you dropped down the " +
                        "drain."
                traiborn && drain ->
                    "I've got the key from Wizard Traiborn and the one that you dropped down " +
                        "the drain."
                rovin -> "I've got the key from Captain Rovin."
                traiborn -> "I've got the key from Wizard Traiborn."
                drain -> "I've got the key which you dropped down the drain."
                else -> "I haven't found any of them yet."
            }
        chatPlayer(neutral, line)
        val remind =
            menu("Can you remind me where all the keys were again?" to true, "I'm still looking." to false)
        if (!remind) {
            chatPlayer(neutral, "I'm still looking.")
            chatNpc(neutral, "Ok, tell me when you've got them all.")
            return
        }
        chatPlayer(quiz, "Can you remind me where all the keys were again?")
        prysinKeyHolders()
        prysinKeyMenu(asked = null)
    }

    /** Swaps the three keys for Silverlight in one transaction; false leaves both untouched. */
    fun takeKeysForSilverlight(player: Player): Boolean {
        if (hasCollectedSilverlight(player)) {
            return false
        }
        val swapped =
            player.invTransaction(player.inv) {
                val inv = select(player.inv)
                for (key in KEYS) {
                    delete {
                        from = inv
                        obj = key.asRSCM(RSCMType.OBJ)
                        strictCount = 1
                    }
                }
                insert {
                    into = inv
                    obj = SILVERLIGHT.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
            }
        if (swapped.failure) {
            return false
        }
        VarPlayerIntMapSetter.set(player, SILVERLIGHT_CASE, 1)
        return true
    }

    private suspend fun Dialogue.prysinBeforeDelrith() {
        chatNpc(quiz, "Have you sorted that demon out yet?")
        if (ownsAnywhere(access, SILVERLIGHT)) {
            chatPlayer(neutral, "No, not yet.")
            chatNpc(neutral, "Well get on with it. He'll be pretty powerful when he gets to full " +
                "strength.")
            return
        }
        chatPlayer(sad, "Not yet. And I, um, lost Silverlight.")
        chatNpc(angry, "Yes, I know, someone returned it to me. Take better care of it this " +
            "time.")
        if (!returnSilverlight(access)) {
            mesbox("You don't have any inventory space for Silverlight.")
        }
    }

    fun returnSilverlight(access: ProtectedAccess): Boolean {
        if (!isReadyForDelrith(access.player) || ownsAnywhere(access, SILVERLIGHT)) {
            return false
        }
        return access.invAdd(access.inv, SILVERLIGHT).success
    }

    private suspend fun Dialogue.rovin() {
        chatNpc(angry, "What are you doing up here? Only the palace guards are allowed up here.")
        val topic =
            menu(
                "I am one of the palace guards." to 0,
                "What about the King?" to 1,
                "Yes I know, but this is important." to 2,
            )
        when (topic) {
            0 -> {
                chatPlayer(neutral, "I am one of the palace guards.")
                chatNpc(angry, "No, you're not! I know all the palace guards.")
                val recruit =
                    menu(
                        "I'm a new recruit." to true,
                        "I've had extensive plastic surgery." to false,
                    )
                if (recruit) {
                    chatPlayer(neutral, "I'm a new recruit.")
                    chatNpc(angry, "I interview all the new recruits. I'd know if you were one " +
                        "of them.")
                    chatPlayer(sad, "That blows that story out of the water then.")
                    chatNpc(angry, "Get out of my sight.")
                } else {
                    chatPlayer(neutral, "I've had extensive plastic surgery.")
                    chatNpc(confused, "What sort of surgery is that? I've never heard of it. " +
                        "Besides, you look reasonably healthy.")
                    chatNpc(angry, "Why is this relevant anyway? You still shouldn't be here.")
                }
            }
            1 -> {
                chatPlayer(quiz, "What about the King? Surely you'd let him up here.")
                chatNpc(neutral, "Well, yes, I suppose we'd let him up. He doesn't generally " +
                    "want to come up here, but if he did want to, he could.")
                chatNpc(angry, "Anyway, you're not the King either. So get out of my sight.")
            }
            else -> rovinImportant()
        }
    }

    private suspend fun Dialogue.rovinImportant() {
        chatPlayer(neutral, "Yes, I know, but this is important.")
        chatNpc(neutral, "Ok, I'm listening. Tell me what's so important.")
        val onQuest = stage(player) == STAGE_KEYS && !hasCollectedSilverlight(player)
        val options = buildList {
            if (onQuest) add("There's a demon who wants to invade this city." to 0)
            add("Erm I forgot." to 1)
            add("The castle has just received its ale delivery." to 2)
        }
        when (menu(options)) {
            0 -> rovinDemon()
            1 -> {
                chatPlayer(confused, "Erm I forgot.")
                chatNpc(neutral, "Well it can't be that important then.")
                chatPlayer(quiz, "How do you know?")
                chatNpc(angry, "Just go away.")
            }
            else -> {
                chatPlayer(neutral, "The castle has just received its ale delivery.")
                chatNpc(neutral, "Now that is important. However I'm the wrong person to speak " +
                    "to about it. Go talk to the kitchen staff.")
            }
        }
    }

    private suspend fun Dialogue.rovinDemon() {
        chatPlayer(neutral, "There's a demon who wants to invade the city.")
        if (access.inv.count(ROVIN_KEY) > 0 || access.bank.count(ROVIN_KEY) > 0) {
            chatNpc(neutral, "Yes, you said before, haven't you killed it yet?")
            chatPlayer(neutral, "Well I'm going to use the powerful sword Silverlight, which I " +
                "believe you have one of the keys for?")
            if (access.inv.count(ROVIN_KEY) > 0) {
                chatNpc(neutral, "I already gave you my key. Check your pockets.")
            } else {
                chatNpc(neutral, "I already gave you my key. Maybe you left it somewhere. Have " +
                    "you checked your bank account?")
            }
            return
        }
        chatNpc(quiz, "Is it a powerful demon?")
        if (!menu("Not really." to false, "Yes, very." to true)) {
            chatPlayer(neutral, "Not really.")
            chatNpc(neutral, "Well, I'm sure the palace guards can deal with it, then. Thanks " +
                "for the information.")
            return
        }
        chatPlayer(worried, "Yes, very.")
        chatNpc(worried, "As good as the palace guards are, I don't know if they're up to " +
            "taking on a very powerful demon.")
        val insult =
            menu(
                "Yeah, the palace guards are rubbish!" to true,
                "It's not them who are going to fight the demon, it's me." to false,
            )
        if (insult) {
            chatPlayer(laugh, "Yeah, the palace guards are rubbish!")
            chatNpc(laugh, "Yeah, they're--")
            chatNpc(angry, "Wait! How dare you insult the palace guards? Get out of my sight!")
            return
        }
        chatPlayer(neutral, "It's not them who are going to fight the demon, it's me.")
        chatNpc(shocked, "What, all by yourself? How are you going to do that?")
        chatPlayer(neutral, "I'm going to use the powerful sword Silverlight, which I believe " +
            "you have one of the keys for?")
        chatNpc(neutral, "Yes, I do. But why should I give it to you?")
        var askedWhy = false
        while (true) {
            val options = buildList {
                if (askedWhy.not()) {
                    add("Fortune-teller Aris said I was destined to kill the demon." to 0)
                    add("Otherwise the demon will destroy the city!" to 1)
                    add("Sir Prysin said you would give me the key." to 2)
                } else {
                    add("Why did he give you one of the keys then?" to 3)
                    add("Fortune-teller Aris said I was destined to kill the demon." to 0)
                    add("Otherwise the demon will destroy the city!" to 1)
                }
            }
            when (menu(options)) {
                0 -> {
                    chatPlayer(neutral, "Fortune-teller Aris said I was destined to kill the " +
                        "demon.")
                    chatNpc(angry, "A fortune-teller? Destiny? I don't believe in that stuff. I " +
                        "got where I am today by hard work, not by destiny! Why should I care " +
                        "what that mad old fortune-teller says?")
                }
                1 -> {
                    chatPlayer(worried, "Otherwise the demon will destroy the city!")
                    chatNpc(angry, "You can't fool me! How do I know you haven't just made that " +
                        "story up to get my key?")
                }
                2 -> {
                    chatPlayer(neutral, "Sir Prysin said you would give me the key.")
                    chatNpc(angry, "Oh, he did, did he? Well I don't report to Sir Prysin, I " +
                        "report directly to the king!")
                    chatNpc(angry, "I didn't work my way up through the ranks of the palace " +
                        "guards so I could take orders from an ill-bred moron who only has his " +
                        "job because his great- grandfather was a hero with a silly name!")
                    askedWhy = true
                }
                else -> {
                    chatPlayer(quiz, "Why did he give you one of the keys then?")
                    chatNpc(neutral, "Only because the king ordered him to! The king couldn't " +
                        "get Sir Prysin to part with his precious ancestral sword, but he made " +
                        "him lock it up so he couldn't lose it.")
                    chatNpc(neutral, "I got one key and I think some wizard got another. Now " +
                        "what happened to the third one?")
                    chatPlayer(neutral, "Sir Prysin dropped it down a drain!")
                    chatNpc(laugh, "Ha ha ha! The idiot!")
                    chatNpc(neutral, "Okay, I'll give you the key, just so that it's you that " +
                        "kills the demon and not Sir Prysin!")
                    if (giveQuestKey(access, ROVIN_KEY)) {
                        objbox(ROVIN_KEY, KEY_ZOOM, "Captain Rovin hands you a key.")
                    } else {
                        chatNpc(neutral, "Talk to me again when you have free inventory space.")
                    }
                    return
                }
            }
        }
    }

    /** Gives one Silverlight key unless the player already owns it or the case is open. */
    fun giveQuestKey(access: ProtectedAccess, key: String): Boolean {
        if (hasCollectedSilverlight(access.player) || ownsAnywhere(access, key)) {
            return false
        }
        return access.invAdd(access.inv, key).success
    }

    /** Wizard Traiborn's Demon Slayer topic; false means the topic is not on offer. */
    suspend fun Dialogue.traibornDemonSlayer(): Boolean {
        if (stage(player) != STAGE_KEYS || hasCollectedSilverlight(player)) {
            return false
        }
        val demonSlayer =
            menu("Talk about Demon Slayer." to true, "Talk about something else." to false)
        if (!demonSlayer) {
            return false
        }
        when {
            player.traibornAsked == 0 -> traibornFirstVisit()
            player.traibornBones >= BONES_NEEDED && ownsAnywhere(access, TRAIBORN_KEY) -> {
                chatNpc(neutral, "Don't you have somewhere to be, young thingummywut? You still " +
                    "have that key you asked me for.")
                chatPlayer(neutral, "You're right. I've got a demon to slay.")
            }
            player.traibornBones >= BONES_NEEDED -> {
                chatNpc(neutral, "Ello young thingummywut.")
                chatPlayer(sad, "I've lost the key you gave to me.")
                chatNpc(neutral, "Yes I know, it was returned to me. If you want it back you're " +
                    "going to have to collect another 25 sets of bones.")
                player.traibornBones = 0
            }
            else -> traibornBones()
        }
        return true
    }

    private suspend fun Dialogue.traibornFirstVisit() {
        chatPlayer(neutral, "I need to get a key given to you by Sir Prysin.")
        chatNpc(confused, "Sir Prysin? Who's that? What would I want his key for?")
        var topic =
            menu(
                "He told me you were looking after it for him." to 0,
                "He's one of the King's knights." to 1,
                "Well, have you got any keys knocking around?" to 2,
            )
        while (topic != 2) {
            topic =
                when (topic) {
                    0 -> traibornLookingAfterIt()
                    1 -> traibornKnights()
                    else -> traibornWhichKey()
                } ?: return
        }
        traibornKeysKnockingAround()
    }

    private suspend fun Dialogue.traibornLookingAfterIt(): Int? {
        chatPlayer(neutral, "He told me you were looking after it for him.")
        chatNpc(confused, "That wasn't very clever of him. I'd lose my head if it wasn't " +
            "screwed on. Go and tell him to find someone else to look after his valuables in " +
            "future.")
        val tell =
            menu(
                "Okay, I'll go and tell him that." to true,
                "Well, have you got any keys knocking around?" to false,
            )
        if (!tell) return 2
        chatPlayer(neutral, "Okay, I'll go and tell him that.")
        chatNpc(happy, "Oh that's great, if it wouldn't be too much trouble.")
        return traibornLeaveOrKeys()
    }

    private suspend fun Dialogue.traibornKnights(): Int? {
        chatPlayer(neutral, "He's one of the King's knights.")
        chatNpc(happy, "Say, I remember one of the King's knights. He had nice shoes...")
        chatNpc(happy, "...and didn't like my homemade spinach rolls. Would you like a spinach " +
            "roll?")
        val roll = menu("Yes please." to true, "Just tell me if you have the key." to false)
        if (!roll) return 3
        chatPlayer(happy, "Yes please.")
        access.invAddOrDrop(objRepo, SPINACH_ROLL)
        objbox(SPINACH_ROLL, "Traiborn digs around in the pockets of his robes. After a few " +
            "moments he triumphantly presents you with a spinach roll.")
        chatPlayer(happy, "Thank you very much.")
        return traibornLeaveOrKeys()
    }

    private suspend fun Dialogue.traibornWhichKey(): Int? {
        chatPlayer(neutral, "Just tell me if you have the key.")
        chatNpc(confused, "The key? The key to what?")
        chatNpc(confused, "There's more than one key in the world don't you know? Would be a " +
            "bit odd if there was only the one.")
        val silverlight =
            menu(
                "It's the key to get a sword called Silverlight." to true,
                "You've lost it, haven't you?" to false,
            )
        if (!silverlight) {
            chatPlayer(quiz, "You've lost it haven't you?")
            chatNpc(angry, "Me? Lose things? That's a nasty accusation.")
            return 2
        }
        chatPlayer(neutral, "It's the key to get a sword called Silverlight.")
        chatNpc(confused, "Silverlight? Never heard of that. Sounds a good name for a ship. Are " +
            "you sure it's not the name of a ship rather than a sword?")
        val sure =
            menu(
                "Yeah, pretty sure." to true,
                "Well, have you got any keys knocking around?" to false,
            )
        if (!sure) return 2
        chatPlayer(neutral, "Yeah, pretty sure.")
        chatNpc(sad, "That's a pity, waste of a name.")
        return traibornLeaveOrKeys()
    }

    private suspend fun Dialogue.traibornLeaveOrKeys(): Int? {
        val leave =
            menu(
                "Err I'd better be off really." to true,
                "Well, have you got any keys knocking around?" to false,
            )
        if (!leave) return 2
        chatPlayer(confused, "Err I'd better be off really.")
        chatNpc(happy, "Oh ok, have a good time, and watch out for sheep! They're more cunning " +
            "than they look.")
        return null
    }

    private suspend fun Dialogue.traibornKeysKnockingAround() {
        chatPlayer(neutral, "Well, have you got any keys knocking around?")
        chatNpc(neutral, "Now you come to mention it, yes I do have a key. It's in my special " +
            "closet of valuable stuff. Now how do I get into that?")
        chatNpc(neutral, "I sealed it using one of my magic rituals. So it would make sense " +
            "that another ritual would open it again.")
        chatPlayer(quiz, "So do you know what ritual to use?")
        chatNpc(neutral, "Let me think a second.")
        chatNpc(neutral, "Yes a simple drazier style ritual should suffice. Hmm, main problem " +
            "with that is I'll need 25 sets of bones. Now where am I going to get hold of " +
            "something like that?")
        val help =
            menu(
                "Hmm, that's too bad. I really need that key." to false,
                "I'll get the bones for you." to true,
            )
        if (!help) {
            chatPlayer(sad, "Hmm, that's too bad. I really need that key.")
            chatNpc(sad, "Ah well, sorry I couldn't be any more help.")
            return
        }
        chatPlayer(neutral, "I'll help get the bones for you.")
        player.traibornAsked = 1
        chatNpc(happy, "Ooh that would be very good of you.")
        chatPlayer(neutral, "Okay, I'll speak to you when I've got some bones.")
    }

    private suspend fun Dialogue.traibornBones() {
        chatNpc(quiz, "How are you doing finding bones?")
        if (access.inv.count(BONES) == 0) {
            chatPlayer(sad, "I haven't got any at the moment.")
            chatNpc(neutral, "Nevermind, keep working on it.")
            return
        }
        chatPlayer(happy, "I have some bones.")
        chatNpc(happy, "Give 'em here then.")
        val given = handInBones(player)
        if (given == 0) {
            return
        }
        mesbox("You give Traiborn $given ${if (given == 1) "set" else "sets"} of bones.")
        if (player.traibornBones < BONES_NEEDED) {
            chatPlayer(neutral, "That's all of them.")
            chatNpc(neutral, "I still need ${BONES_NEEDED - player.traibornBones} more.")
            chatPlayer(neutral, "Ok, I'll keep looking.")
            return
        }
        chatNpc(happy, "Hurrah! That's all 25 sets of bones.")
        scenes.traibornRitual(access, checkNotNull(npc))
        objbox(TRAIBORN_KEY, KEY_ZOOM, "Traiborn hands you a key.")
        chatPlayer(happy, "Thank you very much.")
        chatNpc(happy, "Not a problem for a friend of Sir What's-his-face.")
    }

    /**
     * Takes up to the bones still needed and, when that completes the 25, hands over Traiborn's key
     * in the same transaction. Returns the number of bones taken.
     */
    fun handInBones(player: Player): Int {
        val wanted = BONES_NEEDED - player.traibornBones
        val count = minOf(player.inv.count(BONES), wanted)
        if (count <= 0) {
            return 0
        }
        val completes = count == wanted
        val result =
            player.invTransaction(player.inv) {
                val inv = select(player.inv)
                delete {
                    from = inv
                    obj = BONES.asRSCM(RSCMType.OBJ)
                    strictCount = count
                }
                if (completes) {
                    insert {
                        into = inv
                        obj = TRAIBORN_KEY.asRSCM(RSCMType.OBJ)
                        strictCount = 1
                    }
                }
            }
        if (result.failure) {
            return 0
        }
        player.traibornBones += count
        return count
    }

    fun incantationWords(player: Player): List<String> {
        if (INCANTATION_VARBITS.all { player.vars[it] == 0 }) {
            val order = WORDS.indices.shuffled()
            INCANTATION_VARBITS.forEachIndexed { slot, varbit -> VarPlayerIntMapSetter.set(player, varbit, order[slot]) }
        }
        return INCANTATION_VARBITS.map { WORDS[player.vars[it]] }
    }

    fun incantation(player: Player, separator: String): String =
        incantationWords(player).joinToString("..$separator")

    fun completeBanishing(access: ProtectedAccess) {
        if (stage(access.player) == STAGE_KEYS) {
            quest.completeQuest(access)
        }
    }

    private enum class ArisTopic(val option: String) {
        Fight("How am I meant to fight a demon who can destroy cities?"),
        KillHim("Okay, where is he? I'll kill him for you!"),
        Wally("Wally doesn't sound like a very heroic name."),
        HowDid("So how did Wally kill Delrith?"),
        Incantation("What is the magical incantation?"),
        Where("Where can I find Silverlight?"),
        Thanks("Okay, thanks. I'll do my best to stop the demon."),
        StopCalling("Stop calling me that!"),
    }

    private enum class PrysinTopic {
        YourKey,
        Rovin,
        Wizard,
        Drain,
        Leave,
    }

    companion object {
        const val STAGE_STARTED = 1
        const val STAGE_KEYS = 2
        const val STAGE_COMPLETE = 3

        const val SILVERLIGHT = "obj.silverlight"
        const val TRAIBORN_KEY = "obj.silverlight_key_1"
        const val ROVIN_KEY = "obj.silverlight_key_2"
        const val DRAIN_KEY = "obj.silverlight_key_3"
        const val BONES = "obj.bones"
        const val COINS = "obj.coins"
        const val LAMP = "obj.demon_slayer_lamp"
        const val SPINACH_ROLL = "obj.spinach_roll"
        const val SILVERLIGHT_CASE = "varbit.delrith_silverlight_case"
        const val DRAIN_STATE = "varbit.delrith_drain_key"
        const val BONES_NEEDED = 25
        const val KEY_ZOOM = 400

        val KEYS = listOf(TRAIBORN_KEY, ROVIN_KEY, DRAIN_KEY)

        val WORDS = listOf("Carlem", "Aber", "Camerinthum", "Purchai", "Gabindo")

        val INCANTATION_VARBITS =
            listOf(
                "varbit.delrith_incantation_1",
                "varbit.delrith_incantation_2",
                "varbit.delrith_incantation_3",
                "varbit.delrith_incantation_4",
                "varbit.delrith_incantation_5",
            )

        fun ownsAnywhere(access: ProtectedAccess, obj: String): Boolean =
            access.inv.count(obj) > 0 || access.worn.count(obj) > 0 || access.bank.count(obj) > 0

        private const val LAMP_COUNT = 2
        private const val LAMP_XP = 1_000.0
        private const val LAMP_TITLE = "Choose the stat you wish to be advanced!"
        private val LAMP_STATS =
            listOf(
                "stat.attack",
                "stat.strength",
                "stat.defence",
                "stat.ranged",
                "stat.magic",
                "stat.hitpoints",
                "stat.prayer",
            )

        private const val RECOMMENDED_COMBAT = 15
        private const val SAY_TITLE = "What would you like to say?"
        private const val PRESS_ON = "Well I'd better press on with it."
        private const val PRESS_ON_NO_STOP = "Well I'd better press on with it"
    }
}
