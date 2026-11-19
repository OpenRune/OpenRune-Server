package org.rsmod.content.quest.area.varrock.shieldofarrav

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.menu
import org.rsmod.game.entity.Npc
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ShieldOfArravPhoenix
@Inject
constructor(
    private val doors: ShieldOfArravDoors,
    private val npcRepo: NpcRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(STRAVEN) { startDialogue(it.npc) { straven() } }
        onOpLoc1(PHOENIX_DOOR) { phoenixDoor(it.loc) }
        onOpNpc1(JONNY) { talkToJonny(it.npc) }
        onOpNpc1(JONNY_TALK) { talkToJonny(it.npc) }
        onOpNpc1(JONNY_ATTACKABLE) { talkToJonny(it.npc) }
        onOpLoc1(CHEST) { openChest(it.loc) }
    }

    private suspend fun ProtectedAccess.phoenixDoor(door: BoundLocInfo) {
        arriveDelay()
        if (ShieldOfArravDoors.onDoorSide(door, player.coords) || player.isPhoenixMember) {
            doors.pass(this, door, ShieldOfArravBlackArm.PHOENIX_DOOR_OPEN)
            return
        }
        val straven = findStraven()
        if (straven == null) {
            mes("The door is locked.")
            return
        }
        startDialogue(straven) { straven() }
    }

    private fun ProtectedAccess.findStraven(): Npc? =
        npcRepo.findAll(ZoneKey.from(player.coords), zoneRadius = 1).firstOrNull {
            it.isType(STRAVEN) && it.coords.level == player.coords.level
        }

    private suspend fun Dialogue.straven() {
        when {
            player.isBlackArmMember -> chatNpc(angry, "Stay away from here! Black Arm dog!")
            player.isPhoenixMember -> stravenMember()
            player.phoenixGang == PHOENIX_TASK -> stravenMission()
            else -> stravenStranger()
        }
    }

    private suspend fun Dialogue.stravenStranger() {
        chatPlayer(quiz, "What's through that door?")
        chatNpc(
            neutral,
            "You can't go in there. Only authorised personnel of the VTAM Corporation are " +
                "allowed beyond this point.",
        )
        val canRecruit = player.isOnQuest && player.phoenixGang == PHOENIX_BARAEK_PAID
        val options = buildList {
            if (canRecruit) add("I know who you are!" to StrangerTopic.KnowYou)
            add("How do I get a job with the VTAM corporation?" to StrangerTopic.Job)
            add("Fair enough." to StrangerTopic.FairEnough)
        }
        when (menu(options)) {
            StrangerTopic.KnowYou -> knowWhoYouAre()
            StrangerTopic.Job -> {
                chatPlayer(quiz, "How do I get a job with the VTAM corporation?")
                chatNpc(
                    neutral,
                    "Get a copy of the Varrock Herald. If we have any positions right now, " +
                        "they'll be advertised in there.",
                )
            }
            StrangerTopic.FairEnough -> chatPlayer(neutral, "Fair enough.")
        }
    }

    private suspend fun Dialogue.knowWhoYouAre() {
        chatPlayer(neutral, "I know who you are!")
        chatNpc(quiz, "Really? Well? Who are we then?")
        chatPlayer(
            neutral,
            "This is the headquarters of one of the most dangerous crime syndicates this city " +
                "has ever seen! The Phoenix Gang!",
        )
        chatNpc(
            neutral,
            "No, this is a legitimate business run by legitimate individuals. However... " +
                "supposing we were this crime gang, what would you want with us?",
        )
        val offer =
            choice2(
                "I'd like to offer you my services.",
                true,
                "I want nothing. I was just making sure you were them.",
                false,
            )
        if (!offer) {
            chatPlayer(neutral, "I want nothing. I was just making sure you were them.")
            chatNpc(angry, "Well then get lost and stop wasting my time.")
            return
        }
        chatPlayer(neutral, "I'd like to offer you my services.")
        chatNpc(
            neutral,
            "You mean you'd like to join the Phoenix Gang? Well, obviously I can't speak for " +
                "them, but the Phoenix Gang doesn't let people join just like that. First, you'd " +
                "need to prove your loyalty.",
        )
        chatPlayer(quiz, "How would I go about doing that?")
        chatNpc(
            neutral,
            "Obviously, I would have no idea. Although, having said that, a rival gang of ours, " +
                "er, theirs, called the Black Arm Gang is supposedly meeting a contact from Port " +
                "Sarim today.",
        )
        chatNpc(
            neutral,
            "Apparently the meeting is going to take place in the Blue Moon Inn, and supposedly " +
                "the contact is none other than the notorious mass murderer turned pirate " +
                "captain, Jonny the Beard.",
        )
        player.phoenixGang = PHOENIX_TASK
        chatNpc(
            neutral,
            "Now, obviously I know nothing about the dealings of the Phoenix Gang, but I bet if " +
                "somebody were to arrange an accident for Jonny and bring back his intelligence " +
                "report, the gang would be very pleased.",
        )
        chatPlayer(
            neutral,
            "I see... I'm sure I can help sort something out for them. I'll get right on it.",
        )
    }

    private suspend fun Dialogue.stravenMission() {
        chatNpc(neutral, "How's your little mission going?")
        if (!player.owns(INTEL_REPORT)) {
            chatPlayer(sad, "I don't have it yet...")
            chatNpc(
                neutral,
                "Jonny the Beard is the person who has it. I imagine he will be in the Blue Moon " +
                    "Inn... Not that I'd know for sure, with me definitely not being a member of " +
                    "the Phoenix Gang and all.",
            )
            return
        }
        chatPlayer(happy, "I have the intelligence report!")
        chatNpc(neutral, "Let's see it then.")
        if (!player.holds(INTEL_REPORT)) {
            chatPlayer(neutral, "Let me go get it from the bank.")
            chatNpc(neutral, "I'll wait here.")
            return
        }
        objbox(INTEL_REPORT, "You show the report to Straven.")
        player.phoenixGang = PHOENIX_MEMBER
        chatNpc(
            happy,
            "Very good... Very good... Okay, I'm satisfied. Welcome to the Phoenix Gang! I'm " +
                "Straven, one of the gang leaders.",
        )
        chatPlayer(happy, "Nice to meet you. I'm ${player.displayName}.")
        chatNpc(
            neutral,
            "As a member of our gang, you now have access to our hideout and our weapons supply " +
                "depot. The hideout is just through this door and the depot is two doors down " +
                "from here. You'll need this key to get in.",
        )
        handOverKey()
        chatNpc(happy, "Once again, welcome to the Phoenix Gang!")
    }

    private suspend fun Dialogue.handOverKey() {
        if (!ShieldOfArravExchange.exchange(player, give = listOf(WEAPON_STORE_KEY to 1))) {
            mesbox("You don't have enough space in your inventory for the key.")
            return
        }
        objbox(WEAPON_STORE_KEY, "Straven hands you a key.")
    }

    private suspend fun Dialogue.stravenMember() {
        chatNpc(happy, "Greetings, fellow gang member.")
        if (!player.owns(WEAPON_STORE_KEY)) {
            chatPlayer(sad, "I'm afraid I lost that key you gave me...")
            chatNpc(
                neutral,
                "You really need to be more careful. We don't want that key falling into the " +
                    "wrong hands. Have this spare, but don't lose it this time!",
            )
            handOverKey()
            return
        }
        while (true) {
            val topic =
                menu(
                    "I've heard you've got some cool treasures in this place." to MemberTopic.Treasure,
                    "Any suggestions for where I can go thieving?" to MemberTopic.Thieving,
                    "Where's the Black Arm Gang hideout?" to MemberTopic.Hideout,
                    "I'd better get going." to MemberTopic.Leave,
                )
            when (topic) {
                MemberTopic.Treasure -> treasureChat()
                MemberTopic.Thieving -> {
                    chatPlayer(quiz, "Any suggestions for where I can go thieving?")
                    chatNpc(
                        neutral,
                        "You could always try the marketplace over in Ardougne. Lots of " +
                            "opportunity there!",
                    )
                }
                MemberTopic.Hideout -> hideoutChat()
                MemberTopic.Leave -> {
                    chatPlayer(neutral, "I'd better get going.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.treasureChat() {
        chatPlayer(quiz, "I've heard you've got some cool treasures in this place.")
        chatNpc(
            happy,
            "Oh yeah, we've all stolen some stuff in our time. Our candlesticks down here, for " +
                "example, were quite a challenge to get out of the palace.",
        )
        chatPlayer(quiz, "And the Shield of Arrav?")
        player.stravenShieldChat = 1
        if (player.isQuestComplete) {
            chatNpc(neutral, "Once one of our most prized possessions.")
            chatPlayer(quiz, "Once?")
            chatNpc(
                angry,
                "It seems the Black Arm Gang weren't content with having one half. They recently " +
                    "stole the other half of the shield from us as well!",
            )
            chatPlayer(
                neutral,
                "Oh no! I'll be sure to keep an eye out for any of their members sneaking around.",
            )
            return
        }
        chatNpc(
            shocked,
            "Woah... that's a blast from the past! We stole that years and years ago!",
        )
        chatNpc(
            neutral,
            "Truth be told, we don't even have the whole shield anymore. A few years ago there " +
                "was a big fight within the gang and the shield got broken in half as a result.",
        )
        chatNpc(
            neutral,
            "After the fight, some gang members split off and formed their own group. They took " +
                "one half of the shield with them.",
        )
        chatPlayer(quiz, "This other group? The Black Arm Gang?")
        chatNpc(neutral, "Indeed.")
    }

    private suspend fun Dialogue.hideoutChat() {
        chatPlayer(quiz, "Where's the Black Arm Gang hideout?")
        player.stravenTrampChat = 1
        chatNpc(
            neutral,
            "Interested in spying on the enemy, are you? It will be tricky. Their security is " +
                "pretty good. Not as good as ours obviously, but still good.",
        )
        chatNpc(
            neutral,
            "Still, if you really want to go there, it's down an alleyway near the southern " +
                "gate. One of our contacts, a tramp going by the name of Charlie, keeps an eye " +
                "on it. He may be able to help you.",
        )
        chatPlayer(happy, "I see. Thanks for the help!")
    }

    private suspend fun ProtectedAccess.talkToJonny(jonny: Npc) {
        startDialogue(jonny) { chatNpc(angry, "Whatever you want, I'm not interested!") }
    }

    private suspend fun ProtectedAccess.openChest(chest: BoundLocInfo) {
        doors.openContainer(this, chest, CHEST_OPEN)
        startDialogue {
            mesbox("You search the chest.")
            val canFind = player.isPhoenixMember && !player.isQuestComplete
            when {
                !canFind || player.holds(PHOENIX_SHIELD) ->
                    mesbox("You open the chest, but don't find anything of interest inside.")
                !ShieldOfArravExchange.takeFromStorage(player, PHOENIX_SHIELD) ->
                    objbox(
                        PHOENIX_SHIELD,
                        "You open the chest and find half of a shield inside, but you don't " +
                            "have enough room to take it.",
                    )
                else -> objbox(PHOENIX_SHIELD, "You open the chest and find half of a shield inside.")
            }
        }
    }

    private enum class StrangerTopic {
        KnowYou,
        Job,
        FairEnough,
    }

    private enum class MemberTopic {
        Treasure,
        Thieving,
        Hideout,
        Leave,
    }

    companion object {
        const val STRAVEN = "npc.straven"
        const val PHOENIX_DOOR = "loc.phoenixdoor"
        const val CHEST = "loc.phoenixshutchest"
        const val CHEST_OPEN = "loc.phoenixopenchest"
        const val JONNY = "npc.jonny_the_beard"
        const val JONNY_TALK = "npc.jonny_the_beard_1op"
        const val JONNY_ATTACKABLE = "npc.jonny_the_beard_2op"
    }
}
