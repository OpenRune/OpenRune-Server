package org.rsmod.content.areas.city.portsarim.npcs

import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onOpNpc5
import org.rsmod.api.shops.Shops
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:pandemonium] Sailing, port tasks, boats and the after-quest dialogue
class PortSarimDocksScript @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.klarense") { startDialogue(it.npc) { klarense() } }
        onOpNpc1(PORT_MASTER) { startDialogue(it.npc) { portMaster() } }
        onOpNpc3(PORT_MASTER) { startDialogue(it.npc) { comeBackLater() } }
        onOpNpc4(PORT_MASTER) { startDialogue(it.npc) { comeBackLater() } }
        onOpNpc1("npc.vc_face") { startDialogue(it.npc) { theFace() } }

        onTalkAcross(SHIPWRIGHT) { startDialogue(it) { shipwrightSam() } }
        onOpNpc3(SHIPWRIGHT) { startDialogue(it.npc) { comeBackLater() } }
        onOpNpc4(SHIPWRIGHT) { startDialogue(it.npc) { comeBackLater() } }
        onOpNpc5(SHIPWRIGHT) { startDialogue(it.npc) { comeBackLater() } }

        onOpNpc1("npc.sailing_intro_anne_sarim") { startDialogue(it.npc) { sailingRecruiters() } }
        onOpNpc1("npc.sailing_intro_will_sarim") { startDialogue(it.npc) { sailingRecruiters() } }

        for (crew in TRADER_CREW + TRADER_STAN) {
            val isStan = crew == TRADER_STAN
            onOpNpc1(crew) { startDialogue(it.npc) { traderCrewmember(isStan) } }
            onOpNpc3(crew) { player.openTraderStan() }
            onOpNpc4(crew) { mes(CHARTER_UNAVAILABLE) }
        }

        for (crate in CRATES) {
            onOpLoc1(crate) { mes("You search the crate, but find nothing of note.") }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Klarense
    // TODO(content): [quest:dragon_slayer_i] "I'd like to buy her." during the quest
    // TODO(content): [verify:in_game] the wiki has no dialogue before Dragon Slayer I; this uses
    // the in-quest greeting without the purchase option
    private suspend fun Dialogue.klarense() {
        if (!QuestRequirements.hasCompleted(player, DRAGON_SLAYER)) {
            return klarenseBeforeQuest()
        }
        chatPlayer(quiz, "Hey, that's my ship!")
        chatNpc(
            neutral,
            "No, it's not. It may, to the untrained eye, at first appear to be the Lady Lumbridge, " +
                "but it is definitely not. It's my new ship. It just happens to look slightly " +
                "similar, is all.",
        )
        chatPlayer(
            angry,
            "It has Lady Lumbridge painted out and 'Klarense's Cruiser' painted over it!",
        )
        chatNpc(neutral, "Nope, you're mistaken. It's my new boat.")
        chatPlayer(quiz, "Well will you at least take me to Crandor in it?")
        chatNpc(neutral, "Nope. I don't take passengers on my ship.")
        chatPlayer(angry, "Argh!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Klarense (Dragon Slayer I)
    private suspend fun Dialogue.klarenseBeforeQuest() {
        chatNpc(
            happy,
            "Hello there! Interested in buying this fine ship? I'm sure I can offer her to you " +
                "for a much better price than any of those shipwrights ask for!",
        )
        chatPlayer(quiz, "A much better price? What's the catch?")
        chatNpc(shifty, "Well, she's not quite seaworthy right now...")
        chatPlayer(confused, "Not seaworthy? Why would I want to buy her then?")
        chatNpc(
            happy,
            "Because this isn't just any ship! This is the Lady Lumbridge, one of the last " +
                "Crandorian ships! Despite her size, she's one of the most agile ship around!",
        )
        chatPlayer(neutral, "Except she's not seaworthy...")
        chatNpc(happy, "No, but a bit of work will quickly sort that out!")
        while (true) {
            if (!choice2("Why is she damaged?", true, "I think I'll pass.", false)) {
                chatPlayer(neutral, "I think I'll pass.")
                return
            }
            chatPlayer(quiz, "Why is she damaged?")
            chatNpc(
                neutral,
                "Oh, there was no particular accident, just years of wear and tear. It's been a " +
                    "long time since Crandor was destroyed and she's had several owners since " +
                    "then. Unfortunately, not all of them looked after her.",
            )
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Port_master
    private suspend fun Dialogue.portMaster() {
        chatPlayer(neutral, "Hello.")
        chatNpc(neutral, "Hi there. How can I help you?")
        chatPlayer(quiz, "What do you do around here?")
        chatNpc(
            neutral,
            "I'm the port master. It's my job to keep everything around here running nice and " +
                "smoothly for all the vessels coming and going.",
        )
        val askAboutVessel =
            choice2(
                "So could you tell me how I could get my own vessel?",
                true,
                "Sounds like quite a challenging job. I'd best not distract you.",
                false,
            )
        if (!askAboutVessel) {
            chatPlayer(quiz, "Sounds like quite a challenging job. I'd best not distract you.")
            return
        }
        chatPlayer(quiz, "So could you tell me how I could get my own vessel?")
        chatNpc(
            confused,
            "I'm afraid not, but maybe someone else around here could help. I saw a couple of " +
                "people further north on the docks. I think I heard them saying they were looking " +
                "for help on their boat.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:The_Face
    // TODO(content): [quest:ratcatchers] quest stages
    private suspend fun Dialogue.theFace() {
        chatPlayer(neutral, "Hello.")
        if (!QuestRequirements.hasCompleted(player, RATCATCHERS)) {
            chatNpc(neutral, "...")
            chatPlayer(neutral, "Never mind!")
            return
        }
        chatNpc(neutral, "Oh. It's you again.")
        chatNpc(
            neutral,
            "That was an amazing performance! I've only seen one like it... as you know.",
        )
        chatPlayer(happy, "Don't worry, Felkrash has no idea how I figured it out.")
        chatNpc(angry, "And it had better stay that way!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Shipwright_Sam
    private suspend fun Dialogue.shipwrightSam() {
        chatNpc(happy, "Greetings! What can I do for you?")
        chatPlayer(quiz, "Hello there. What do you do around here?")
        chatNpc(
            neutral,
            "I'm a shipwright. I supply boats of all kinds to anyone with the money to afford " +
                "them.",
        )
        chatPlayer(happy, "Interesting... Would I be able to buy one?")
        chatNpc(
            neutral,
            "Sorry, but I don't have any ready for sale right now. I'm sure I will soon, so keep " +
                "checking back.",
        )
        chatPlayer(happy, "Okay, fair enough.")
        mesbox(SAILING_LOCKED)
    }

    private suspend fun Dialogue.comeBackLater() {
        chatNpc(
            neutral,
            "I'm afraid I can't help you with that at the moment. You'll have to come back later.",
        )
        mesbox(SAILING_LOCKED)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Pandemonium
    private suspend fun Dialogue.sailingRecruiters() {
        chatNpcSpecific(
            ANNE_NAME,
            ANNE,
            happy,
            "Ah, look what we have here, Will! This looks like someone who needs a good job!",
        )
        chatPlayer(confused, "What?")
        chatNpcSpecific(
            WILL_NAME,
            WILL,
            happy,
            "Goodness, Anne, I think you're right! This one's clearly never worked an honest day " +
                "in their life, and it's about time someone changed that!",
        )
        chatPlayer(confused, "But I don't need a...")
        chatNpcSpecific(
            ANNE_NAME,
            ANNE,
            happy,
            "Well, let's not waste any more time! Stranger, are you ready for your interview?",
        )
        if (choice2("Yes.", true, "No.", false, title = "Start the Pandemonium quest?")) {
            return
        }
        chatPlayer(
            confused,
            "Sorry, but I'm not looking for a job right now. I'm just going to go...",
        )
        chatNpcSpecific(
            WILL_NAME,
            WILL,
            happy,
            "Oh, you'll be back, friend! You already know you can't turn down an opportunity " +
                "like this!",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Trader_Crewmember
    // https://oldschool.runescape.wiki/w/Transcript:Trader_Stan
    // TODO(content): [skill:sailing] Trader Stan's "Do you have any paints for sale?"
    // TODO(content): [system:charter_ships] charter destinations, fares and the bedsheet and rum
    // checks
    private suspend fun Dialogue.traderCrewmember(isStan: Boolean) {
        chatNpc(quiz, "Can I help you?")
        val topic =
            choice3(
                "Yes, who are you?",
                CrewTopic.Who,
                "Yes, I would like to charter a ship.",
                CrewTopic.Charter,
                "No thanks.",
                CrewTopic.Leave,
            )
        crewTopic(topic, isStan)
    }

    private suspend fun Dialogue.crewTopic(topic: CrewTopic, isStan: Boolean) {
        when (topic) {
            CrewTopic.Who -> {
                chatPlayer(quiz, "Yes, who are you?")
                if (isStan) {
                    chatNpc(
                        happy,
                        "Why, I'm Trader Stan, owner and operator of the largest fleet of trading " +
                            "ships and chartered vessels to ever sail the seas!",
                    )
                } else {
                    chatNpc(
                        happy,
                        "I'm one of Trader Stan's crew. We are all part of one of the largest " +
                            "fleet of trading and sailing vessels to ever sail the seas.",
                    )
                }
                val whose = if (isStan) "my" else "our"
                chatNpc(
                    neutral,
                    "If you want to get to a port in a hurry, then you can charter one of " +
                        "$whose ships to take you there, if the price is right...",
                )
                chatPlayer(quiz, "So, where exactly can I go with your ships?")
                chatNpc(
                    neutral,
                    "We run ships all over the place. Port Sarim, Catherby, Brimhaven, Musa Point " +
                        "and Port Khazard are just a few of our many destinations!",
                )
                chatPlayer(
                    quiz,
                    "If you visit a lot of ports, I take it you have some exotic stuff to trade?",
                )
                val who = if (isStan) "My crew and I have" else "We have"
                chatNpc(
                    happy,
                    "We certainly do! $who access to items bought and sold from around the " +
                        "world. Would you like to take a look? Or would you like to charter a ship?",
                )
                crewTopic(crewFollowUp(isStan), isStan)
            }
            CrewTopic.Clothes -> {
                chatPlayer(quiz, "Isn't it tricky to sail about in those clothes?")
                chatNpc(angry, "Tricky? Tricky!")
                chatNpc(
                    neutral,
                    "With all due credit, Trader Stan is a great employer, but he insists we wear " +
                        "the latest in high fashion even when sailing.",
                )
                chatNpc(
                    angry,
                    "Do you have even the slightest idea how tricky it is to sail in this stuff?",
                )
                chatNpc(
                    neutral,
                    "Some of us tried tearing it and arguing that it was too fragile to wear when " +
                        "on a boat, but he just had it enchanted to re-stitch itself.",
                )
                chatNpc(
                    sad,
                    "It's hard to hate him when we know how much he shells out on this gear, but " +
                        "if I fall overboard because of this getup one more time, I'm going to quit.",
                )
                chatPlayer(neutral, "Wow, that's kind of harsh.")
                chatNpc(
                    neutral,
                    "Yes... Anyway, would you like to take a look at our exotic wares from around " +
                        "the world? Or would you like to charter a ship?",
                )
                crewTopic(crewFollowUp(isStan), isStan)
            }
            CrewTopic.Charter -> {
                chatPlayer(neutral, "Yes, I would like to charter a ship.")
                chatNpc(neutral, "Certainly, ${sirOrMadam()}. Where would you like to go?")
                access.mes(CHARTER_UNAVAILABLE)
            }
            CrewTopic.Trade -> {
                chatPlayer(neutral, "Yes, let's see what you're trading.")
                player.openTraderStan()
            }
            CrewTopic.Leave -> chatPlayer(sad, "No thanks.")
        }
    }

    private suspend fun Dialogue.crewFollowUp(isStan: Boolean): CrewTopic =
        menu(
            buildList {
                add("Yes, let's see what you're trading." to CrewTopic.Trade)
                add("Yes, I would like to charter a ship." to CrewTopic.Charter)
                if (!isStan) {
                    add("Isn't it tricky to sail about in those clothes?" to CrewTopic.Clothes)
                }
                add("No thanks." to CrewTopic.Leave)
            }
        )

    private fun Dialogue.sirOrMadam(): String =
        if (player.appearance.bodyType == Constants.bodytype_a) "sir" else "madam"

    private fun Player.openTraderStan() {
        shops.open(
            player = this,
            title = "Trader Stan's Trading Post",
            shopInv = "inv.trader_stan_shop",
            buyPercentage = 15.0,
            sellPercentage = 250.0,
            changePercentage = 2.0,
        )
    }

    private enum class CrewTopic {
        Who,
        Charter,
        Trade,
        Clothes,
        Leave,
    }

    private companion object {
        const val SHIPWRIGHT = "npc.sailing_shipwright_port_sarim"
        const val TRADER_STAN = "npc.sailing_transport_trader_stan"
        const val ANNE = "npc.sailing_intro_anne_sarim"
        const val ANNE_NAME = "Anne"
        const val WILL = "npc.sailing_intro_will_sarim"
        const val WILL_NAME = "Will"
        const val PORT_MASTER = "npc.port_task_master_port_sarim"
        const val DRAGON_SLAYER = "quest_dragonslayer1"
        const val RATCATCHERS = "quest_ratcatchers"
        const val SAILING_LOCKED =
            "You need to complete the Pandemonium quest to fully access the Sailing skill."
        const val CHARTER_UNAVAILABLE = "Charter ships aren't available yet."

        val TRADER_CREW =
            listOf(
                "npc.sailing_transport_trader_stan_crew_woman3",
                "npc.sailing_transport_trader_stan_crew_man3",
            )
        val CRATES = listOf("loc.sarim_crate", "loc.sarim_crate2")
    }
}
