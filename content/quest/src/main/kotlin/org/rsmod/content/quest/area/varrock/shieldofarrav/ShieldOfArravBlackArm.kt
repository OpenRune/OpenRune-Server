package org.rsmod.content.quest.area.varrock.shieldofarrav

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpObj3
import org.rsmod.content.quest.manager.menu
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.obj.Obj
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ShieldOfArravBlackArm
@Inject
constructor(
    private val doors: ShieldOfArravDoors,
    private val npcRepo: NpcRepository,
    private val objRepo: ObjRepository,
    private val aiPlayerInteractions: AiPlayerInteractions,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(CHARLIE) { startDialogue(it.npc) { charlie() } }
        onOpNpc1(KATRINE) { startDialogue(it.npc) { katrine() } }
        onOpLoc1(BLACKARM_DOOR) { blackArmDoor(it.loc) }
        onOpLoc1(DEPOT_DOOR) { depotDoor(it.loc) }
        onOpLoc1(CUPBOARD) { openCupboard(it.loc) }
        onOpNpc1(WEAPONSMASTER) { talkToWeaponsmaster(it.npc) }
        onOpNpc1(WEAPONSMASTER_VIS) { talkToWeaponsmaster(it.npc) }
        val crossbow = checkNotNull(ServerCacheManager.getItem(CROSSBOW.asRSCM())) { CROSSBOW }
        onOpObj3(crossbow) { takeCrossbow(it.obj) }
    }

    private suspend fun Dialogue.charlie() {
        chatNpc(neutral, "Spare some change, guv?")
        while (true) {
            val options = charlieOptions()
            when (menu(options)) {
                CharlieTopic.WhoAreYou -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(
                        neutral,
                        "Charles. Charles E. Trampin' at your service. Now, about that change you " +
                            "were going to give me...",
                    )
                }
                CharlieTopic.BlackArm -> return charlieBlackArm()
                CharlieTopic.Phoenix -> return charliePhoenix()
                CharlieTopic.GiveCoin -> return charlieCoin()
                CharlieTopic.NoChange -> {
                    chatPlayer(neutral, "Sorry, I haven't got any.")
                    chatNpc(neutral, "Thanks anyway.")
                    return
                }
                CharlieTopic.GetAJob -> {
                    chatPlayer(angry, "Go get a job!")
                    chatNpc(angry, "You startin? I hope your nose falls off!")
                    return
                }
            }
        }
    }

    private fun Dialogue.charlieOptions(): List<Pair<String, CharlieTopic>> {
        val who = "Who are you?" to CharlieTopic.WhoAreYou
        val give = "Okay, here you go." to CharlieTopic.GiveCoin
        val none = "Sorry, I haven't got any." to CharlieTopic.NoChange
        val job = "Go get a job!" to CharlieTopic.GetAJob
        val blackArm = "I'm looking for the Black Arm Gang." to CharlieTopic.BlackArm
        return when {
            !player.isOnQuest || player.isBlackArmMember -> listOf(who, none, job, give)
            player.blackArmGang == BLACKARM_TASK ->
                listOf(who, "Any idea where the Phoenix Gang are based?" to CharlieTopic.Phoenix, give, none)
            player.isPhoenixMember -> listOf(who, blackArm, job, none)
            player.blackArmGang >= BLACKARM_KNOWN -> listOf(who, blackArm, none, job)
            else -> listOf(who, none, job, give)
        }
    }

    private suspend fun Dialogue.charlieBlackArm() {
        chatPlayer(neutral, "I'm looking for the Black Arm Gang.")
        if (player.charlieMet == 0 && !player.isPhoenixMember) {
            chatNpc(
                neutral,
                "Funny you should mention that... Rumour has it that they have their " +
                    "headquarters right down this alleyway. Though if I were you, I'd be careful " +
                    "about messing with them...",
            )
        } else {
            chatNpc(
                neutral,
                "Like I told you, the rumour is that they have their headquarters right down this " +
                    "alleyway. Though if I were you, I'd be careful about messing with them...",
            )
        }
        chatPlayer(quiz, "Do you think they would let me join?")
        if (!player.isPhoenixMember) {
            player.charlieMet = 1
            chatNpc(
                neutral,
                "You never know. You'll find a lady down there called Katrine. Speak to her. But " +
                    "whatever you do, don't upset her.",
            )
            return
        }
        chatNpc(
            neutral,
            "No. You're a collaborator with the Phoenix Gang. There's no way they'll let you join.",
        )
        val asked =
            choice2(
                "How did you know I was in the Phoenix Gang?",
                true,
                "Any ideas how I could get in there then?",
                false,
            )
        if (asked) {
            chatPlayer(quiz, "How did you know I was in the Phoenix Gang?")
            chatNpc(
                neutral,
                "I've been known to have dealings with the Phoenix Gang. I wouldn't be very good " +
                    "at my job if I didn't know their members.",
            )
            chatPlayer(quiz, "I see. So, any ideas how I could get in there?")
        } else {
            chatPlayer(quiz, "Any ideas how I could get in there then?")
        }
        chatNpc(
            neutral,
            "Your best bet would probably be to find someone who isn't a member of the Phoenix " +
                "Gang and get them to infiltrate the ranks of the Black Arm Gang for you.",
        )
        fellowAdventurer()
    }

    private suspend fun Dialogue.charliePhoenix() {
        chatPlayer(quiz, "Any idea where the Phoenix Gang are based?")
        if (player.charliePaid == 1) {
            chatNpc(
                neutral,
                "Well, given you paid up, I guess I can remind you. The Phoenix Gang can be found " +
                    "east of here. Take the southern alley past the Blue Moon Inn and look for a " +
                    "building owned by the VTAM corporation.",
            )
            phoenixLocation()
            return
        }
        chatNpc(
            happy,
            "Sure, I know everything there is to know about this city. I'd be happy to tell you " +
                "for ten coins.",
        )
        val choice =
            menu(
                "Okay, that sounds fair." to 0,
                "Ten coins? That's too much! I'll give you five." to 1,
                "Never mind. I'll find it myself." to 2,
            )
        when (choice) {
            0 -> {
                chatPlayer(neutral, "Okay, that sounds fair.")
                if (!payCharlie(CHARLIE_FEE)) return
                objbox(InvObj(COINS, CHARLIE_FEE), "You give ten coins to Charlie.")
                charlieDirections()
            }
            1 -> {
                chatPlayer(neutral, "Ten coins? That's too much! I'll give you five.")
                chatNpc(sad, "I guess that'll have to do then.")
                if (!payCharlie(CHARLIE_HALF_FEE)) return
                objbox(InvObj(COINS, CHARLIE_HALF_FEE), "You give five coins to Charlie.")
                chatNpc(happy, "Great, thanks.")
                chatPlayer(quiz, "So where can I find the Phoenix Gang then?")
                chatNpc(shifty, "If you pay me five more coins, I'll tell you.")
                val pay =
                    choice2(
                        "That's not fair, but I guess I don't have a choice.",
                        true,
                        "You thieving gutter-scum! You'd better watch your back!",
                        false,
                    )
                if (!pay) {
                    chatPlayer(angry, "You thieving gutter-scum! You'd better watch your back!")
                    chatNpc(neutral, "Yeah, sure, whatever. You don't frighten me, pal.")
                    return
                }
                chatPlayer(sad, "That's not fair, but I guess I don't have a choice.")
                if (!payCharlie(CHARLIE_HALF_FEE)) return
                objbox(InvObj(COINS, CHARLIE_HALF_FEE), "You give five coins to Charlie.")
                charlieDirections()
            }
            else -> {
                chatPlayer(neutral, "Never mind. I'll find it myself.")
                chatNpc(neutral, "Well, you know where to find me if you get lost!")
            }
        }
    }

    private suspend fun Dialogue.payCharlie(coins: Int): Boolean {
        if (ShieldOfArravExchange.exchange(player, take = listOf(COINS to coins))) {
            return true
        }
        chatPlayer(sad, "Oh wait. I haven't got enough, sorry.")
        chatNpc(neutral, "Thanks anyway.")
        return false
    }

    private suspend fun Dialogue.charlieDirections() {
        player.charliePaid = 1
        chatNpc(
            happy,
            "Perfect! Now listen closely. The Phoenix Gang can be found east of here. Take the " +
                "southern alley past the Blue Moon Inn and look for a building owned by the VTAM " +
                "corporation.",
        )
        phoenixLocation()
    }

    private suspend fun Dialogue.phoenixLocation() {
        chatNpc(
            neutral,
            "Their main hideout is in that building, with their weapon stash being two buildings " +
                "east of it. Good luck getting into either without being a member of their gang " +
                "though.",
        )
        chatPlayer(worried, "Without being a member of their gang? That might be problematic.")
        chatNpc(
            neutral,
            "Well, your best bet would probably be to find someone who isn't a member of the " +
                "Black Arm Gang and get them to infiltrate the ranks of the Phoenix Gang for you.",
        )
        fellowAdventurer()
    }

    private suspend fun Dialogue.fellowAdventurer() {
        chatPlayer(quiz, "Someone? Like who?")
        chatNpc(
            neutral,
            "There's plenty of other adventurers about besides yourself. I'm sure if you asked " +
                "one of them nicely they would help you.",
        )
        chatPlayer(neutral, "I see...")
    }

    private suspend fun Dialogue.charlieCoin() {
        chatPlayer(neutral, "Okay, here you go.")
        if (!ShieldOfArravExchange.exchange(player, take = listOf(COINS to 1))) {
            chatPlayer(sad, "Oh wait. I haven't got any, sorry.")
            chatNpc(neutral, "Thanks anyway.")
            return
        }
        chatNpc(happy, "Hey, thanks a lot!")
        val hint =
            choice2(
                "No problem.",
                false,
                "Don't I get some sort of quest hint or something now?",
                true,
            )
        if (!hint) {
            chatPlayer(neutral, "No problem.")
            return
        }
        chatPlayer(quiz, "So...don't I get some sort of quest hint or something now?")
        chatNpc(confused, "Huh? What do you mean? That wasn't why I asked you for money.")
        chatNpc(sad, "I just need to eat...")
    }

    private suspend fun Dialogue.katrine() {
        when {
            player.isPhoenixMember ->
                chatNpc(
                    angry,
                    "You've got some guts coming here, Phoenix scum! Get lost, or I'll make sure " +
                        "you haven't got those guts anymore!",
                )
            player.isBlackArmMember -> katrineMember()
            player.blackArmGang == BLACKARM_TASK -> katrineCrossbows()
            else -> katrineStranger()
        }
    }

    private suspend fun Dialogue.katrineStranger() {
        chatPlayer(quiz, "What is this place?")
        chatNpc(neutral, "It's a private business. Can I help you at all?")
        val knowsGang = player.isOnQuest && player.blackArmGang >= BLACKARM_KNOWN
        val options = buildList {
            if (knowsGang) add("I've heard you're the Black Arm Gang." to 0)
            add("What sort of business?" to 1)
            add("I'm looking for fame and riches." to 2)
        }
        when (menu(options)) {
            0 -> heardYoureTheGang()
            1 -> {
                chatPlayer(quiz, "What sort of business?")
                chatNpc(
                    neutral,
                    "A small, family business. We give financial advice to other companies. Now, " +
                        "if you'll excuse me.",
                )
            }
            else -> {
                chatPlayer(happy, "I'm looking for fame and riches.")
                chatNpc(
                    neutral,
                    "And you expect to find that up the back streets of Varrock? Keep dreaming.",
                )
            }
        }
    }

    private suspend fun Dialogue.heardYoureTheGang() {
        chatPlayer(neutral, "I've heard you're the Black Arm Gang.")
        chatNpc(angry, "Who told you that?")
        val source =
            menu(
                "I'd rather not reveal my sources." to 0,
                "It was Charlie, the tramp outside." to 1,
                "Everyone knows. It's no great secret." to 2,
            )
        when (source) {
            0 -> {
                chatPlayer(neutral, "I'd rather not reveal my sources.")
                chatNpc(
                    neutral,
                    "Hmm... I suppose I can understand that. So, say we were the Black Arm Gang, " +
                        "what would you want with us?",
                )
            }
            1 -> {
                chatPlayer(neutral, "It was Charlie, the tramp outside.")
                chatNpc(
                    angry,
                    "Is that guy still out there? He's getting to be a nuisance. Might be time " +
                        "for him to have a little accident...",
                )
                chatNpc(
                    neutral,
                    "Anyway, say we were the Black Arm Gang, what would you want with us?",
                )
            }
            else -> {
                chatPlayer(neutral, "Everyone knows. It's no great secret.")
                chatNpc(shocked, "What?! You must be joking!")
                chatPlayer(
                    neutral,
                    "Oh no, not at all... It's so obvious! Even the town guard have caught on...",
                )
                chatNpc(
                    worried,
                    "I see... Well... say we were the Black Arm Gang, what would you want with us?",
                )
            }
        }
        val want =
            menu(
                "I want to become a member of your gang." to 0,
                "I want some hints on becoming a thief." to 1,
                "I'm looking for the door out of here." to 2,
            )
        when (want) {
            0 -> chatPlayer(neutral, "I want to become a member of your gang.")
            1 -> {
                chatPlayer(neutral, "I want some hints on becoming a thief.")
                chatNpc(
                    neutral,
                    "Do you now? And what makes you think I'll provide anything like that?",
                )
                chatPlayer(neutral, "Well, how about I become a member of your gang in return?")
            }
            else -> {
                chatPlayer(neutral, "I'm looking for the door out of here.")
                chatNpc(neutral, "Try... the one you just came through?")
                return
            }
        }
        katrineRecruitment()
    }

    private suspend fun Dialogue.katrineRecruitment() {
        chatNpc(
            neutral,
            "How unusual... Okay, let's say we are who you believe we are. Do you think we'd be " +
                "recruiting people who just waltz in here saying 'hello, I'd like to play'?",
        )
        chatPlayer(quiz, "I don't know. What would be the alternative?")
        chatNpc(
            neutral,
            "Watching local thugs and thieves in action, perhaps? How do I know I can just trust " +
                "someone who's wandered in here.",
        )
        val tryMe =
            choice2(
                "Well, you can give me a try can't you?",
                true,
                "Well, people tell me I have an honest face.",
                false,
            )
        if (tryMe) {
            chatPlayer(neutral, "Well, you can give me a try can't you?")
            chatNpc(neutral, "I'm not so sure...")
        } else {
            chatPlayer(happy, "Well, people tell me I have an honest face.")
            chatNpc(
                neutral,
                "Someone honest wanting to join a gang of thieves? Excuse me if I remain " +
                    "unconvinced...",
            )
        }
        chatNpc(
            neutral,
            "Thinking about it... I may have a solution actually. You may have heard of the " +
                "Phoenix Gang. I hear they have a very nice weapons stash not too far from here.",
        )
        chatNpc(
            neutral,
            "We're fresh out of crossbows, but apparently they have plenty. If someone were to " +
                "aquire a couple for us, it would be very much appreciated.",
        )
        chatPlayer(quiz, "Sounds simple enough. Any particular reason you need them?")
        chatNpc(
            shifty,
            "Let's just say there's a local merchant who is refusing to pay some very reasonable " +
                "'keep-your-life-pleasant' insurance rates.",
        )
        chatPlayer(shocked, "So you're planning to murder him?")
        chatNpc(
            shifty,
            "Him? No. However, I hear that the local law might soon be finding a crossbow " +
                "belonging to the Phoenix Gang in his home...",
        )
        chatNpc(
            shifty,
            "I also hear that a similar type of crossbow might soon be used in a murder on the " +
                "other side of the city...",
        )
        chatNpc(
            neutral,
            "Now, are you going to keep asking questions, or are you going to go get those " +
                "crossbows?",
        )
        val accept =
            choice2("Okay, no problem.", true, "Sounds a little tricky. Got anything easier?", false)
        if (accept) {
            chatPlayer(neutral, "Okay, no problem.")
        } else {
            chatPlayer(worried, "Sounds a little tricky. Got anything easier?")
            chatNpc(
                angry,
                "If you're not up for a little bit of danger, I don't think you've got anything " +
                    "to offer us.",
            )
            if (!choice2("Okay, I'll do it.", true, "It's a no from me then.", false)) {
                chatPlayer(neutral, "It's a no from me then.")
                chatNpc(angry, "Then get lost!")
                return
            }
            chatPlayer(neutral, "Okay, I'll do it.")
        }
        player.blackArmGang = BLACKARM_TASK
        chatNpc(
            happy,
            "Great! Apparently the Phoenix Gang's weapons stash is somewhere to the east. Ask that " +
                "tramp outside. I'm sure he'll know more.",
        )
    }

    private suspend fun Dialogue.katrineCrossbows() {
        chatNpc(quiz, "Have you got those crossbows for me yet?")
        when (player.inv.count(CROSSBOW)) {
            0 -> {
                chatPlayer(
                    sad,
                    "No, I haven't found them yet. Do you know where I should be looking.",
                )
                chatNpc(
                    neutral,
                    "Apparently the Phoenix Gang's weapons stash is somewhere to the east. Ask " +
                        "that tramp outside. I'm sure he'll know more.",
                )
            }
            1 -> {
                chatPlayer(neutral, "I have one...")
                chatNpc(neutral, "Well, I need two. Come back when you have them.")
            }
            else -> {
                chatPlayer(happy, "Yes, I have.")
                if (!ShieldOfArravExchange.exchange(player, take = listOf(CROSSBOW to 2))) {
                    return
                }
                player.blackArmGang = BLACKARM_MEMBER
                player.weaponsmasterDead = 0
                objbox(CROSSBOW, "You give the crossbows to Katrine.")
                chatNpc(
                    happy,
                    "It seems you're more skilled than I originally gave you credit for. Well " +
                        "done. Consider yourself a member of the Black Arm Gang. I'd suggest you " +
                        "now go and familiarise yourself with the rest of the ganghouse.",
                )
                chatPlayer(happy, "Will do! Thanks!")
            }
        }
    }

    private suspend fun Dialogue.katrineMember() {
        chatPlayer(neutral, "Hey.")
        chatNpc(neutral, "Hey.")
        val topic =
            menu(
                "So I'm a part of the gang now?" to 0,
                "Who are all those people in there?" to 1,
                "Teach me to be a top class criminal!" to 2,
            )
        when (topic) {
            0 -> {
                chatPlayer(quiz, "So I'm part of the gang now?")
                if (player.isQuestComplete) {
                    chatNpc(
                        neutral,
                        "You are, but don't make me regret it. In case you didn't hear, someone " +
                            "recently stole something very important from us. We need to be more " +
                            "careful about who we let in.",
                    )
                    chatPlayer(shifty, "Imagine doing something like that!")
                } else {
                    chatNpc(
                        neutral,
                        "You are. Feel free to familiarise yourself with the rest of the ganghouse.",
                    )
                    chatPlayer(happy, "Will do! Thanks!")
                }
            }
            1 -> {
                chatPlayer(quiz, "Who are all those people in there?")
                chatNpc(neutral, "They're just various rogues and thieves.")
                chatPlayer(neutral, "They don't say a lot...")
                chatNpc(neutral, "Nope.")
            }
            else -> {
                chatPlayer(happy, "Teach me to be a top class criminal.")
                chatNpc(neutral, "Teach yourself.")
            }
        }
    }

    private suspend fun ProtectedAccess.blackArmDoor(door: BoundLocInfo) {
        arriveDelay()
        when {
            ShieldOfArravDoors.onDoorSide(door, player.coords) || player.isBlackArmMember ->
                doors.pass(this, door, BLACKARM_DOOR_OPEN)
            player.isPhoenixMember -> {
                soundSynth(ShieldOfArravDoors.LOCKED_SOUND)
                startDialogue { mesbox("The door is securely locked.") }
            }
            else -> mes("This door seems to be locked from the inside.")
        }
    }

    private suspend fun ProtectedAccess.depotDoor(door: BoundLocInfo) {
        arriveDelay()
        if (!ShieldOfArravDoors.onDoorSide(door, player.coords)) {
            doors.pass(this, door, PHOENIX_DOOR_OPEN)
            return
        }
        if (WEAPON_STORE_KEY !in inv) {
            soundSynth(ShieldOfArravDoors.LOCKED_SOUND)
            startDialogue { mesbox("The door is securely locked.") }
            return
        }
        mes("You unlock the door using the key you're carrying.")
        doors.pass(this, door, PHOENIX_DOOR_OPEN)
    }

    private suspend fun ProtectedAccess.openCupboard(cupboard: BoundLocInfo) {
        doors.openContainer(this, cupboard, CUPBOARD_OPEN)
        val canFind = player.isBlackArmMember && !player.isQuestComplete
        startDialogue {
            when {
                !canFind || player.holds(BLACKARM_SHIELD) ->
                    mesbox("You open the cupboard, but don't find anything of interest inside.")
                !ShieldOfArravExchange.takeFromStorage(player, BLACKARM_SHIELD) ->
                    objbox(
                        BLACKARM_SHIELD,
                        "You open the cupboard and find half of a shield inside, but you don't " +
                            "have enough room to take it.",
                    )
                else -> {
                    objbox(
                        BLACKARM_SHIELD,
                        "You open the cupboard and find half of a shield inside.",
                    )
                    chatPlayer(
                        happy,
                        "This must be the Shield of Arrav! Well... half of it at least. I wonder " +
                            "where the other half is...",
                    )
                }
            }
        }
    }

    private suspend fun ProtectedAccess.talkToWeaponsmaster(weaponsmaster: Npc) {
        startDialogue(weaponsmaster) {
            chatPlayer(neutral, "Hello.")
            if (player.isPhoenixMember) {
                chatNpc(happy, "Hello fellow Phoenix! What are you after?")
                val weapons =
                    choice2("I'm after a weapon or two.", true, "I'm looking for treasure.", false)
                if (weapons) {
                    chatPlayer(neutral, "I'm after a weapon or two.")
                    chatNpc(neutral, "No problem. Feel free to look around.")
                } else {
                    chatPlayer(neutral, "I'm looking for treasure.")
                    chatNpc(
                        neutral,
                        "Aren't we all? We've not got any up here. Go mug someone somewhere if you " +
                            "want some treasure.",
                    )
                }
                return@startDialogue
            }
            chatNpc(
                angry,
                "Hey! Who are you? I'm gonna teach you not to stick your nose where it don't " +
                    "belong!",
            )
        }
        if (!player.isPhoenixMember) {
            weaponsmaster.opPlayer2(player, aiPlayerInteractions)
        }
    }

    private suspend fun ProtectedAccess.takeCrossbow(crossbow: Obj) {
        val guard = if (crossbow.coords.level == DEPOT_LEVEL) findWeaponsmaster() else null
        if (guard != null && player.isPhoenixMember) {
            startDialogue(guard) {
                chatNpc(angry, "Hey! That's Straven's! He won't like you messing with that!")
            }
            return
        }
        if (guard != null && player.weaponsmasterDead == 0) {
            guard.say("Stop! Thief!")
            guard.opPlayer2(player, aiPlayerInteractions)
            return
        }
        take(crossbow)
    }

    private fun ProtectedAccess.findWeaponsmaster(): Npc? =
        npcRepo.findAll(ZoneKey.from(player.coords), zoneRadius = 1).firstOrNull {
            it.isType(WEAPONSMASTER) && it.coords.level == player.coords.level
        }

    private suspend fun ProtectedAccess.take(obj: Obj) {
        if (player.coords != obj.coords) {
            delay(1)
            anim("seq.human_pickuptable")
        }
        val added = player.invAdd(inv, obj.type, obj.count, autoCommit = false)
        if (added.failure) {
            mes(Constants.dm_take_invspace)
            return
        }
        if (!objRepo.del(obj)) {
            mes(Constants.dm_take_taken)
            return
        }
        soundSynth("synth.pick2")
        added.commitAll()
    }

    private enum class CharlieTopic {
        WhoAreYou,
        BlackArm,
        Phoenix,
        GiveCoin,
        NoChange,
        GetAJob,
    }

    companion object {
        const val CHARLIE = "npc.tramppg"
        const val KATRINE = "npc.katrine"
        const val WEAPONSMASTER = "npc.weaponsmaster"
        const val WEAPONSMASTER_VIS = "npc.weaponsmaster_vis"
        const val BLACKARM_DOOR = "loc.blackarmdoor"
        const val BLACKARM_DOOR_OPEN = "loc.blackarmdoor_open"
        const val DEPOT_DOOR = "loc.phoenixdoor2"
        const val PHOENIX_DOOR_OPEN = "loc.phoenixdoor_open"
        const val CUPBOARD = "loc.blackarmcupboardshut"
        const val CUPBOARD_OPEN = "loc.blackarmcupboardopen"
        const val CHARLIE_FEE = 10
        const val CHARLIE_HALF_FEE = 5
        const val DEPOT_LEVEL = 1
    }
}
