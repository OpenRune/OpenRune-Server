package org.rsmod.content.quest.area.portsarim

import jakarta.inject.Inject
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpcU
import org.rsmod.api.shops.Shops
import org.rsmod.content.quest.manager.DigSpots
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

internal var Player.crateBananas by intVarBit("varbit.pirates_treasure_crate_bananas")
internal var Player.crateRum by boolVarBit("varbit.pirates_treasure_crate_rum")
internal var Player.rumShipped by boolVarBit("varbit.pirates_treasure_rum_shipped")
internal var Player.crateTaskAccepted by boolVarBit("varbit.pirates_treasure_luthas_task")

class PiratesTreasure
@Inject
constructor(
    private val shops: Shops,
    private val npcRepo: NpcRepository,
    private val locRepo: LocRepository,
    private val aiPlayerInteractions: AiPlayerInteractions,
) :
    QuestScript(
        "quest_piratestreasure",
        "varp.hunt",
        rewards { extra("One-Eyed Hector's Treasure") },
        ItemRewardDisplay(CASKET),
    ) {

    private val gardeners = mutableMapOf<PlayerUid, Npc>()

    override fun ScriptContext.init() {
        onOpNpc1("npc.redbeard_frank") { startDialogue(it.npc) { redbeardFrank() } }
        onOpNpcU("npc.redbeard_frank") {
            val obj = it.objType.internalName
            startDialogue(it.npc) {
                when {
                    obj == RUM && quest.isQuestCompleted(player) ->
                        chatNpc(neutral, "Thanks fer the thought, but I still have to finish this bottle.")
                    obj == RUM && stage(player) == STARTED -> {
                        chatPlayer(happy, "Frank, I have some Karamja Rum.")
                        tradeRumForKey()
                    }
                    obj == CASKET && quest.isQuestCompleted(player) -> {
                        chatPlayer(happy, "I have the treasure, would you like a share?")
                        chatNpc(neutral, "No ${lad()}, you got it fair and square.")
                        chatNpc(happy, "You enjoy it. It's what Hector would have wanted.")
                    }
                    else -> access.mes("Nothing interesting happens.")
                }
            }
        }
        onOpNpc1("npc.luthas") { startDialogue(it.npc) { luthas() } }
        onOpNpc1("npc.zembo") { startDialogue(it.npc) { zembo() } }
        onOpNpc3("npc.zembo") { openSpiritsShop() }
        onOpLoc1("loc.bananacrate") { searchBananaCrate() }
        onOpLoc2("loc.bananacrate") { fillBananaCrate() }
        onOpLocU("loc.bananacrate", BANANA) { packOneBanana() }
        onOpLocU("loc.bananacrate", RUM) { stashRum() }
        onOpLoc1("loc.piratechest") { mes("The chest is locked.") }
        onOpLocU("loc.piratechest", KEY) { unlockChest(it.loc) }
        onOpHeld1(MESSAGE) { readMessage() }
        onOpHeld1(CASKET) { openCasket() }
        DigSpots.register { coords -> digForTreasure(coords) }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Redbeard Frank</col> in <col=800000>Port Sarim</col>."

    override fun questLog(player: ProtectedAccess): String =
        questJournal(player) {
            val stage = quest.getQuestStage(access.player)
            if (stage < KEY_GIVEN) {
                line("<red>Redbeard Frank</red> in <red>Port Sarim</red> will tell me where to find some treasure if I bring him a bottle of <red>Karamja Rum</red>.")
                line("The <red>Customs officers</red> won't let rum off <red>Karamja</red>, so I'll need to find a way to smuggle it past them.")
                return@questJournal
            }
            strike("I brought Redbeard Frank some Karamja Rum.")
            if (stage < MESSAGE_READ) {
                line("He gave me a key to <red>One-Eyed Hector's</red> chest in the <red>Blue Moon Inn</red> in <red>Varrock</red>.")
                return@questJournal
            }
            strike("I found a message in One-Eyed Hector's chest in the Blue Moon Inn.")
            line("The message says the treasure is buried in the <red>park</red> in <red>Falador</red>, where <red>Saradomin</red> points to the X.")
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line("I smuggled some Karamja Rum off Karamja for Redbeard Frank, and he gave me the key to One-Eyed Hector's chest.")
            line("The chest held a message that led me to Falador park, where I dug up Hector's treasure.")
        }

    private fun stage(player: Player): Int = quest.getQuestStage(player)

    private fun Dialogue.lad(): String = if (access.isBodyTypeA()) "lad" else "lass"

    private suspend fun Dialogue.redbeardFrank() {
        chatNpc(laugh, "Arr, Matey!")
        val stage = stage(player)
        if (stage == STARTED) {
            rumCheck()
            return
        }
        while (true) {
            val options = buildList {
                if (stage == NOT_STARTED) add("I'm in search of treasure." to 1)
                if (stage in KEY_GIVEN until COMPLETE && !hasKey(player)) {
                    add("I seem to have lost my chest key..." to 4)
                }
                add("Arr!" to 2)
                add("Do you have anything for trade?" to 3)
            }
            when (menu(options)) {
                1 -> return searchOfTreasure()
                2 -> {
                    chatPlayer(laugh, "Arr!")
                    chatNpc(laugh, "Arr!")
                }
                3 -> {
                    chatPlayer(quiz, "Do you have anything for trade?")
                    chatNpc(
                        neutral,
                        "Nothin' at the moment, but then again the Customs Agents are on the " +
                            "warpath right now.",
                    )
                    return
                }
                4 -> {
                    chatPlayer(sad, "I seem to have lost my chest key...")
                    chatNpc(
                        laugh,
                        "Arr, silly you. Fortunately I took the precaution to have another one made.",
                    )
                    access.invAdd(access.inv, KEY)
                    objbox(KEY, "Frank hands you a chest key.")
                }
            }
        }
    }

    private fun hasKey(player: Player): Boolean = player.inv.count(KEY) > 0

    private suspend fun Dialogue.searchOfTreasure() {
        chatPlayer(shifty, "I'm in search of treasure.")
        chatNpc(
            shifty,
            "Arr, treasure you be after eh? Well I might be able to tell you where to find " +
                "some... For a price...",
        )
        chatPlayer(quiz, "What sort of price?")
        chatNpc(
            neutral,
            "Well for example if you can get me a bottle of rum... Not just any rum mind...",
        )
        chatNpc(
            happy,
            "I'd like some rum made on Karamja Island. There's no rum like Karamja Rum!",
        )
        if (!startQuestPrompt(quest)) {
            chatPlayer(neutral, "Not right now.")
            chatNpc(
                neutral,
                "Fair enough. I'll still be here and thirsty whenever you feel like helpin' out.",
            )
            return
        }
        quest.advanceQuestStageTo(access, STARTED)
        chatPlayer(neutral, "Ok, I will bring you some Karamja Rum.")
        chatNpc(happy, "Yer a saint, although it'll take a miracle to get it off Karamja.")
        chatPlayer(quiz, "What do you mean?")
        chatNpc(
            neutral,
            "The Customs office has been clampin' down on the export of spirits. You seem like " +
                "a resourceful young ${lad()}, I'm sure ye'll be able to find a way to slip the " +
                "stuff past them.",
        )
        chatPlayer(neutral, "Well I'll give it a shot.")
        chatNpc(happy, "Arr, that's the spirit!")
    }

    private suspend fun Dialogue.rumCheck() {
        chatNpc(quiz, "Have ye brought some rum for yer ol' mate Frank?")
        if (player.inv.count(RUM) == 0) {
            chatPlayer(neutral, "No, not yet.")
            chatNpc(neutral, "Not surprising, tis no easy task to get it off Karamja.")
            chatPlayer(quiz, "What do you mean?")
            chatNpc(
                neutral,
                "The Customs office has been clampin' down on the export of spirits. You seem " +
                    "like a resourceful young ${lad()}, I'm sure ye'll be able to find a way to " +
                    "slip the stuff past them.",
            )
            chatPlayer(neutral, "Well I'll give it another shot.")
            return
        }
        chatPlayer(happy, "Yes, I've got some.")
        tradeRumForKey()
    }

    private suspend fun Dialogue.tradeRumForKey() {
        chatNpc(
            shifty,
            "Now a deal's a deal, I'll tell ye about the treasure. I used to serve under a pirate " +
                "captain called One-Eyed Hector.",
        )
        chatNpc(
            shifty,
            "Hector were very successful and became very rich. But about a year ago we were " +
                "boarded by the Customs and Excise Agents.",
        )
        chatNpc(
            sad,
            "Hector were killed along with many of the crew, I were one of the few to escape and " +
                "I escaped with this.",
        )
        access.invDel(access.inv, RUM)
        access.invAdd(access.inv, KEY)
        quest.advanceQuestStageTo(access, KEY_GIVEN)
        objbox(KEY, "Frank happily takes the rum... and hands you a key.")
        chatNpc(
            shifty,
            "This be Hector's key. I believe it opens his chest in his old room in the Blue Moon " +
                "Inn in Varrock.",
        )
        chatNpc(neutral, "With any luck his treasure will be in there.")
        val why = menu("Ok thanks, I'll go and get it." to false, "So why didn't you ever get it?" to true)
        if (why) {
            chatPlayer(quiz, "So why didn't you ever get it?")
            chatNpc(
                sad,
                "I'm not allowed in the Blue Moon Inn. Apparently I'm a drunken trouble maker.",
            )
        } else {
            chatPlayer(neutral, "Ok thanks, I'll go and get it.")
        }
    }

    private suspend fun Dialogue.luthas() {
        if (player.crateBananas >= CRATE_CAPACITY) {
            crateFilled()
            return
        }
        if (player.crateTaskAccepted) {
            chatNpc(quiz, "Have you completed your task yet?")
            when (
                menu(
                    "What did I have to do again?" to 1,
                    "No, the crate isn't full yet." to 2,
                    "So where are these bananas going to be delivered to?" to 3,
                    "That customs officer is annoying isn't she?" to 4,
                )
            ) {
                1 -> {
                    chatPlayer(quiz, "What did I have to do again?")
                    chatNpc(
                        neutral,
                        "There's a crate ready to be loaded onto the ship. If you could fill it up " +
                            "with bananas, I'll pay you 30 gold.",
                    )
                }
                2 -> {
                    chatPlayer(neutral, "No, the crate isn't full yet...")
                    chatNpc(neutral, "Well come back when it is.")
                }
                3 -> whereAreBananasGoing()
                4 -> annoyingCustomsOfficer()
            }
            return
        }
        chatNpc(happy, "Hello I'm Luthas, I run the banana plantation here.")
        val employment =
            menu(
                "Could you offer me employment on your plantation?" to true,
                "That customs officer is annoying isn't she?" to false,
            )
        if (!employment) {
            annoyingCustomsOfficer()
            return
        }
        chatPlayer(quiz, "Could you offer me employment on your plantation?")
        chatNpc(
            happy,
            "Yes, I can sort something out. There's a crate ready to be loaded onto the ship.",
        )
        chatNpc(
            neutral,
            "You wouldn't believe the demand for bananas from Wydin's shop over in Port Sarim. I " +
                "think this is the third crate I've shipped him this month..",
        )
        player.crateTaskAccepted = true
        chatNpc(happy, "If you could fill it up with bananas, I'll pay you 30 gold.")
    }

    private suspend fun Dialogue.crateFilled() {
        chatPlayer(happy, "I've filled a crate with bananas.")
        chatNpc(happy, "Well done, here's your payment.")
        access.invAdd(access.inv, "obj.coins", CRATE_PAYMENT)
        access.mes("Luthas hands you $CRATE_PAYMENT coins.")
        if (player.crateRum) {
            player.rumShipped = true
        }
        player.crateRum = false
        player.crateBananas = 0
        player.crateTaskAccepted = false
        when (
            menu(
                "Will you pay me for another crate full?" to 1,
                "Thank you, I'll be on my way" to 2,
                "So where are these bananas going to be delivered to?" to 3,
                "That customs officer is annoying isn't she?" to 4,
            )
        ) {
            1 -> {
                chatPlayer(quiz, "Will you pay me for another crate full?")
                chatNpc(happy, "Yes certainly.")
                player.crateTaskAccepted = true
                chatNpc(
                    neutral,
                    "If you go outside you should see the old crate has been loaded on to the " +
                        "ship, and there is another empty crate in its place.",
                )
            }
            2 -> chatPlayer(happy, "Thank you, I'll be on my way.")
            3 -> whereAreBananasGoing()
            4 -> annoyingCustomsOfficer()
        }
    }

    private suspend fun Dialogue.whereAreBananasGoing() {
        chatPlayer(quiz, "So where are these bananas going to be delivered to?")
        chatNpc(neutral, "I sell them to Wydin who runs a grocery store in Port Sarim.")
    }

    private suspend fun Dialogue.annoyingCustomsOfficer() {
        chatPlayer(neutral, "That customs officer is annoying isn't she?")
        chatNpc(neutral, "Well I know her pretty well. She doesn't cause me any trouble any more.")
        chatNpc(
            neutral,
            "She doesn't even search my export crates any more. She knows they only contain " +
                "bananas.",
        )
        chatPlayer(quiz, "Really? How interesting. Whereabouts do you send those to?")
        chatNpc(
            neutral,
            "There is a little shop over in Port Sarim that buys them up by the crate. I believe " +
                "it is run by a man called Wydin.",
        )
    }

    private suspend fun ProtectedAccess.searchBananaCrate() {
        arriveDelay()
        val bananas = player.crateBananas
        when {
            bananas == 0 && player.crateRum ->
                mes(
                    "There is some rum in here, although with no bananas to cover it. It is a " +
                        "little obvious."
                )
            bananas == 0 -> mes("The crate is empty.")
            bananas >= CRATE_CAPACITY -> mesbox("The crate is full of bananas.")
            else -> mes("The crate has $bananas banana${if (bananas == 1) "" else "s"} inside.")
        }
        if (bananas > 0 && player.crateRum) {
            mesbox("There is also some rum stashed in here too.")
        }
    }

    private suspend fun ProtectedAccess.fillBananaCrate() {
        arriveDelay()
        if (player.crateBananas >= CRATE_CAPACITY) {
            mes("The crate is already full.")
            return
        }
        val held = inv.count(BANANA)
        if (held == 0) {
            mes("You don't have any bananas to pack.")
            return
        }
        val packed = minOf(held, CRATE_CAPACITY - player.crateBananas)
        anim("seq.human_pickuptable")
        invDel(inv, BANANA, packed)
        player.crateBananas += packed
        mesbox("You pack all your bananas into the crate.")
    }

    private suspend fun ProtectedAccess.packOneBanana() {
        arriveDelay()
        if (player.crateBananas >= CRATE_CAPACITY) {
            mes("The crate is already full.")
            return
        }
        anim("seq.human_pickuptable")
        invDel(inv, BANANA)
        player.crateBananas++
        mesbox("You pack a banana into the crate.")
    }

    private suspend fun ProtectedAccess.stashRum() {
        arriveDelay()
        if (player.crateRum) {
            mes("There's already some rum in here...")
            return
        }
        anim("seq.human_pickuptable")
        invDel(inv, RUM)
        player.crateRum = true
        mesbox("You stash the rum in the crate.")
    }

    private suspend fun Dialogue.zembo() {
        chatNpc(
            happy,
            "Hey, are you wanting to try some of my fine wines and spirits? All brewed locally on " +
                "Karamja island.",
        )
        if (menu("Yes please." to true, "No, thank you." to false)) {
            access.openSpiritsShop()
        } else {
            chatPlayer(neutral, "No, thank you.")
        }
    }

    private fun ProtectedAccess.openSpiritsShop() {
        shops.open(
            player = player,
            title = "Karamja Wines, Spirits, and Beers.",
            shopInv = "inv.boozeshop",
            buyPercentage = 70.0,
            sellPercentage = 100.0,
            changePercentage = 2.0,
        )
    }

    private suspend fun ProtectedAccess.unlockChest(chest: BoundLocInfo) {
        arriveDelay()
        mes("You unlock the chest.")
        soundSynth("synth.unlock")
        if (stage(player) !in KEY_GIVEN until COMPLETE || inv.count(MESSAGE) > 0) {
            mes("The chest is empty.")
            return
        }
        mes("All that's in the chest is a message...")
        locRepo.change(chest, "loc.piratechestopen", CHEST_OPEN_TICKS)
        delay(CHEST_OPEN_TICKS)
        mes("You take the message from the chest.")
        invAdd(inv, MESSAGE)
    }

    private fun ProtectedAccess.readMessage() {
        if (stage(player) == KEY_GIVEN) {
            quest.advanceQuestStageTo(this, MESSAGE_READ)
        }
        ifOpenMain("interface.messagescroll_handwriting")
        for (line in 1..MESSAGE_LINES) {
            val text =
                when (line) {
                    5 -> "Visit the city of the White Knights. In the park,"
                    6 -> "Saradomin points to the X which marks the spot."
                    else -> ""
                }
            ifSetText("component.messagescroll_handwriting:messagescroll${line}_hw", text)
        }
    }

    private suspend fun ProtectedAccess.digForTreasure(coords: CoordGrid): Boolean {
        if (coords != TREASURE_SPOT || stage(player) != MESSAGE_READ) {
            return false
        }
        val gardener = gardeners[player.uid]
        if (gardener == null) {
            spawnGardener()
            return true
        }
        if (gardener.isSlotAssigned && gardener.hitpoints > 0) {
            mes("I can't dig up anything with him attacking me!")
            return true
        }
        gardeners.remove(player.uid)
        mes("You dig a hole in the ground...")
        delay(4)
        mes("and find a little chest of treasure.")
        invAdd(inv, CASKET)
        quest.advanceQuestStageTo(this, COMPLETE)
        return true
    }

    private fun ProtectedAccess.spawnGardener() {
        val gardener = Npc("npc.pirate_irate_gardener", GARDENER_SPAWN)
        npcRepo.add(gardener, GARDENER_DURATION)
        gardener.say("First moles, now this! Take this, vandal!")
        gardener.opPlayer2(player, aiPlayerInteractions)
        gardeners[player.uid] = gardener
    }

    private fun ProtectedAccess.openCasket() {
        invDel(inv, CASKET)
        invAdd(inv, "obj.coins", 450)
        invAdd(inv, "obj.gold_ring")
        invAdd(inv, "obj.emerald")
        mes("You open the casket, and find One-Eyed Hector's treasure.")
    }

    private companion object {
        const val NOT_STARTED = 0
        const val STARTED = 1
        const val KEY_GIVEN = 2
        const val MESSAGE_READ = 3
        const val COMPLETE = 4

        const val RUM = "obj.karamja_rum"
        const val BANANA = "obj.banana"
        const val KEY = "obj.chest_key"
        const val MESSAGE = "obj.piratemessage"
        const val CASKET = "obj.pirate_casket"

        const val CRATE_CAPACITY = 10
        const val CRATE_PAYMENT = 30
        const val CHEST_OPEN_TICKS = 4
        const val MESSAGE_LINES = 11
        const val GARDENER_DURATION = 100

        val TREASURE_SPOT = CoordGrid(2999, 3383)
        val GARDENER_SPAWN = CoordGrid(2997, 3382)
    }
}
