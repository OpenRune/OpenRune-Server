package org.rsmod.content.quest.area.portsarim.piratestreasure

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.content.quest.area.portsarim.piratestreasure.PiratesTreasureSmuggling.Companion.CRATE_CAPACITY
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.menu
import org.rsmod.content.quest.manager.rewards
import org.rsmod.content.quest.manager.startQuestPrompt
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.ScriptContext

class PiratesTreasureQuest @Inject constructor() :
    QuestScript(
        "quest_piratestreasure",
        "varp.hunt",
        rewards {
            xp("stat.cooking", COOKING_XP)
            item(CASKET, label = "One-Eyed Hector's Treasure")
        },
        ItemRewardDisplay(CASKET, zoom = 250),
        questVarbit = "varbit.pirates_treasure_progress",
    ) {

    private var Player.chestOpened by intVarBit("varbit.pirates_treasure_chest_opened")

    override fun ScriptContext.init() {
        check(quest.maxSteps == STAGE_COMPLETE) {
            "Pirate's Treasure end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the script completes at $STAGE_COMPLETE."
        }
        onOpNpc1(FRANK) { startDialogue(it.npc) { redbeardFrank() } }
        onOpNpcU(FRANK) { startDialogue(it.npc) { itemOnFrank(it.objType.internalName) } }
        onOpLoc1(CHEST) { openChest() }
        onOpLocU(CHEST, CHEST_KEY) { unlockChest() }
        onOpHeld1(MESSAGE) { readMessage() }
        onOpHeld1(CASKET) { openCasket() }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Redbeard Frank<col=000080> in <col=800000>Port Sarim<col=000080>."

    override fun questLog(player: ProtectedAccess) =
        questJournal(player) {
            val p = player.player
            val stage = stage(p)
            if (stage == STAGE_STARTED) {
                line(
                    "I have spoken to <red>Redbeard Frank</red>. He has agreed to tell me the " +
                        "location of some <red>treasure</red> for some <red>Karamjan rum</red>."
                )
                line("")
                rumProgress(p).forEach(::line)
                return@questJournal
            }
            strike("I have spoken to Redbeard Frank. He has agreed to tell me")
            strike("the location of some treasure for some Karamjan rum.")
            strike("I smuggled some rum off Karamja, and gave it to him.")
            if (stage == STAGE_KEY) {
                line("He gave me a <red>key</red> to <red>One-Eyed Hector's</red> chest, which is in")
                line("Hector's old room upstairs in the <red>Blue Moon Inn</red> in <red>Varrock</red>.")
                return@questJournal
            }
            strike("He gave me a key to One-Eyed Hector's chest in the")
            strike("Blue Moon Inn in Varrock, and I found a message inside.")
            line("The message says the treasure is buried in the <red>park</red> in")
            line("<red>Falador</red>, where <red>Saradomin</red> points to the <red>X</red> which marks the spot.")
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            strike("I have spoken to Redbeard Frank. He has agreed to tell me")
            strike("the location of some treasure for some Karamjan rum.")
            strike("I smuggled some rum off Karamja, and gave it to him.")
            strike("He gave me a key to One-Eyed Hector's chest in the")
            strike("Blue Moon Inn in Varrock, and I found a message inside.")
            strike("I followed the message to Falador Park, fought off an")
            strike("angry gardener and dug up One-Eyed Hector's treasure.")
        }

    private fun rumProgress(player: Player): List<String> =
        when {
            player.inv.count(RUM) > 0 ->
                listOf(
                    "I have the <red>Karamjan rum</red>. I should take it to " +
                        "<red>Redbeard Frank</red>."
                )
            player.vars[WYDIN_RUM] == 1 && player.vars[WYDIN_JOB] == 1 ->
                listOf(
                    "I have hidden my <red>rum</red> in a crate of <red>bananas</red>, and",
                    "the crate has been shipped to <red>Wydin's</red> food store in " +
                        "<red>Port Sarim</red>.",
                    "",
                    "I have taken a job at <red>Wydin's</red> food store, so I can get into " +
                        "the back room where the crate is kept.",
                )
            player.vars[WYDIN_RUM] == 1 ->
                listOf(
                    "I have hidden my <red>rum</red> in a crate of <red>bananas</red>, and",
                    "the crate has been shipped to <red>Wydin's</red> food store in " +
                        "<red>Port Sarim</red>. I need to get into his back room to fetch it.",
                )
            player.vars[CRATE_RUM] == 1 && player.vars[CRATE_BANANAS] >= CRATE_CAPACITY ->
                listOf(
                    "I have hidden my <red>rum</red> in the crate and filled it with " +
                        "<red>bananas</red>. I should ask <red>Luthas</red> to pay me for my work."
                )
            player.vars[CRATE_RUM] == 1 ->
                listOf(
                    "I have taken employment on the <red>banana plantation</red>.",
                    "",
                    "I have hidden my <red>rum</red> in the crate, and now I need to fill it " +
                        "with <red>bananas</red>.",
                )
            player.vars[LUTHAS_JOB] == 1 ->
                listOf(
                    "I have taken employment on the <red>banana plantation</red>, as the " +
                        "<red>Customs Officers</red> might not notice the <red>rum</red> if it " +
                        "is covered in <red>bananas</red>.",
                    "",
                    "Now all I need is some <red>rum</red> to hide in the next crate destined " +
                        "for <red>Wydin's</red> store.",
                )
            player.vars[RUM_RETRIEVED] == 1 ->
                listOf(
                    "I smuggled some <red>Karamjan rum</red> off the island, but",
                    "I seem to have lost it. I need to go to <red>Karamja</red> and get some more.",
                )
            else ->
                listOf(
                    "I need to go to <red>Karamja</red> and get some <red>Karamjan rum</red>. " +
                        "The <red>Customs Officers</red> confiscate any rum they find, so I " +
                        "will need to smuggle it off the island."
                )
        }

    fun stage(player: Player): Int = quest.getQuestStage(player)

    fun setStage(access: ProtectedAccess, stage: Int) {
        quest.setQuestStage(access, stage)
    }

    private suspend fun Dialogue.redbeardFrank() {
        chatNpc(laugh, "Arr, Matey!")
        when (stage(player)) {
            0 -> frankMenu(offerTreasure = true)
            STAGE_STARTED -> frankAsksForRum()
            STAGE_KEY -> {
                if (needsReplacementKey(player)) {
                    replaceKey()
                }
                frankMenu(offerTreasure = false)
            }
            else -> frankMenu(offerTreasure = false)
        }
    }

    private suspend fun Dialogue.frankMenu(offerTreasure: Boolean) {
        while (true) {
            val options = buildList {
                if (offerTreasure) add("I'm in search of treasure." to FrankTopic.Treasure)
                add("Arr!" to FrankTopic.Arr)
                add("Do you have anything for trade?" to FrankTopic.Trade)
            }
            when (menu(options)) {
                FrankTopic.Treasure -> return searchForTreasure()
                FrankTopic.Arr -> {
                    chatPlayer(laugh, "Arr!")
                    chatNpc(laugh, "Arr!")
                }
                FrankTopic.Trade -> {
                    chatPlayer(quiz, "Do you have anything for trade?")
                    chatNpc(neutral, CUSTOMS_REPLY)
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.searchForTreasure() {
        chatPlayer(happy, "I'm in search of treasure.")
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
        chatNpc(happy, "I'd like some rum made on Karamja Island. There's no rum like Karamja Rum!")
        if (QuestRequirements.hasCompleted(player, RUM_DEAL)) {
            chatPlayer(quiz, "Would some Braindeath 'Rum' do?")
            chatNpc(angry, "Gadzooks, that swill! Not a chance!")
            chatNpc(neutral, "It's Karamja Rum or no deal.")
        }
        if (!startQuestPrompt(quest)) {
            chatPlayer(neutral, "Not right now.")
            chatNpc(
                neutral,
                "Fair enough. I'll still be here and thirsty whenever you feel like helpin' out.",
            )
            return
        }
        if (stage(player) == 0) {
            setStage(access, STAGE_STARTED)
        }
        chatPlayer(happy, "Ok, I will bring you some rum.")
        chatNpc(happy, "Yer a saint, although it'll take a miracle to get it off Karamja.")
        chatPlayer(quiz, "What do you mean?")
        chatNpc(neutral, customsWarning())
        chatPlayer(neutral, "Well I'll give it a shot.")
        chatNpc(laugh, "Arr, that's the spirit!")
    }

    private suspend fun Dialogue.frankAsksForRum() {
        chatNpc(quiz, "Have ye brought some rum for yer ol' mate Frank?")
        val inv = access.inv
        when {
            inv.count(RUM) > 0 -> {
                chatPlayer(happy, "Yes, I've got some.")
                tellOfTreasure()
            }
            inv.count(BANANA_RUM) > 0 -> {
                chatPlayer(happy, "Yes, I've got some.")
                chatNpc(angry, "Arr - this here rum's got a banana stuck in it!")
            }
            inv.count(SLICED_BANANA_RUM) > 0 -> {
                chatPlayer(happy, "Yes, I've got some.")
                chatNpc(angry, "Arr - I don't likes banana in me rum!")
            }
            else -> {
                chatPlayer(sad, "No, not yet.")
                chatNpc(neutral, "Not surprising, tis no easy task to get it off Karamja.")
                chatPlayer(quiz, "What do you mean?")
                chatNpc(neutral, customsWarning())
                chatPlayer(neutral, "Well I'll give it another shot.")
            }
        }
    }

    private suspend fun Dialogue.itemOnFrank(obj: String) {
        val stage = stage(player)
        when (obj) {
            RUM ->
                if (stage == STAGE_STARTED) {
                    chatPlayer(happy, "Frank, I have some Karamja Rum.")
                    tellOfTreasure()
                } else if (stage > STAGE_STARTED) {
                    chatNpc(happy, "Thanks fer the thought, but I still have to finish this bottle.")
                } else {
                    access.mes(NOTHING_INTERESTING)
                }
            BANANA_RUM ->
                if (stage == STAGE_STARTED) {
                    chatNpc(angry, "Arr - this here rum's got a banana stuck in it!")
                } else {
                    access.mes(NOTHING_INTERESTING)
                }
            SLICED_BANANA_RUM ->
                if (stage == STAGE_STARTED) {
                    chatNpc(angry, "Arr - I don't likes banana in me rum!")
                } else {
                    access.mes(NOTHING_INTERESTING)
                }
            CASKET -> {
                chatPlayer(happy, "I have the treasure, would you like a share?")
                chatNpc(happy, "No ${ladOrLass()}, you got it fair and square.")
                chatNpc(happy, "You enjoy it. It's what Hector would have wanted.")
            }
            else -> access.mes(NOTHING_INTERESTING)
        }
    }

    private suspend fun Dialogue.tellOfTreasure() {
        chatNpc(
            neutral,
            "Now a deal's a deal, I'll tell ye about the treasure. I used to serve under a " +
                "pirate captain called One-Eyed Hector.",
        )
        chatNpc(
            neutral,
            "Hector were very successful and became very rich. But about a year ago we were " +
                "boarded by the Customs and Excise Agents.",
        )
        chatNpc(
            sad,
            "Hector were killed along with many of the crew, I were one of the few to escape " +
                "and I escaped with this.",
        )
        if (!tradeRumForKey(access)) {
            return
        }
        objbox(CHEST_KEY, "Frank happily takes the rum... and hands you a key.")
        chatNpc(
            neutral,
            "This be Hector's key. I believe it opens his chest in his old room in the Blue " +
                "Moon Inn in Varrock.",
        )
        chatNpc(happy, "With any luck his treasure will be in there.")
        val why =
            choice2("Ok thanks, I'll go and get it.", false, "So why didn't you ever get it?", true)
        if (!why) {
            chatPlayer(happy, "Ok thanks, I'll go and get it.")
            return
        }
        chatPlayer(quiz, "So why didn't you ever get it?")
        chatNpc(sad, "I'm not allowed in the Blue Moon Inn. Apparently I'm a drunken trouble maker.")
    }

    private suspend fun Dialogue.replaceKey() {
        chatPlayer(sad, "I seem to have lost my chest key...")
        chatNpc(
            laugh,
            "Arr, silly you. Fortunately I took the precaution to have another one made.",
        )
        if (giveReplacementKey(access)) {
            objbox(CHEST_KEY, "Frank hands you a chest key.")
        }
    }

    /** Takes one Karamjan rum for Hector's key and moves the quest on; false changes nothing. */
    fun tradeRumForKey(access: ProtectedAccess): Boolean {
        val player = access.player
        if (stage(player) != STAGE_STARTED) {
            return false
        }
        val swapped =
            player.invTransaction(player.inv) {
                val inv = select(player.inv)
                delete {
                    from = inv
                    obj = RUM.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
                insert {
                    into = inv
                    obj = CHEST_KEY.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
            }
        if (swapped.failure) {
            return false
        }
        setStage(access, STAGE_KEY)
        return true
    }

    fun needsReplacementKey(player: Player): Boolean =
        stage(player) == STAGE_KEY && !player.ownsAnywhere(CHEST_KEY) &&
            !player.ownsAnywhere(MESSAGE)

    fun giveReplacementKey(access: ProtectedAccess): Boolean {
        val player = access.player
        if (!needsReplacementKey(player)) {
            return false
        }
        return access.invAdd(player.inv, CHEST_KEY).success
    }

    private suspend fun ProtectedAccess.openChest() {
        arriveDelay()
        if (stage(player) == STAGE_KEY && player.chestOpened == 0 && inv.count(CHEST_KEY) > 0) {
            takeMessage()
            return
        }
        mes("The chest is locked.")
        soundSynth(LOCKED_SOUND)
    }

    private suspend fun ProtectedAccess.unlockChest() {
        arriveDelay()
        when {
            stage(player) == STAGE_KEY -> takeMessage()
            stage(player) > STAGE_KEY -> {
                if (invDel(inv, CHEST_KEY).success) {
                    mes("The key snaps in the lock.")
                    mes("Maybe you need to get the key for yourself...")
                }
            }
            else -> {
                mes("The chest is locked.")
                soundSynth(LOCKED_SOUND)
            }
        }
    }

    private fun ProtectedAccess.takeMessage() {
        if (!swapKeyForMessage(this)) {
            return
        }
        mes("You unlock the chest.")
        mes("All that's in the chest is a message...")
        mes("You take the message from the chest.")
    }

    /** Turns the chest key into the pirate message; false leaves the key where it was. */
    fun swapKeyForMessage(access: ProtectedAccess): Boolean {
        val player = access.player
        if (stage(player) != STAGE_KEY) {
            return false
        }
        val swapped =
            player.invTransaction(player.inv) {
                val inv = select(player.inv)
                delete {
                    from = inv
                    obj = CHEST_KEY.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
                insert {
                    into = inv
                    obj = MESSAGE.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
            }
        if (swapped.failure) {
            return false
        }
        player.chestOpened = 1
        return true
    }

    private fun ProtectedAccess.readMessage() {
        ifOpenMainModal(MESSAGE_SCROLL)
        for ((index, component) in MESSAGE_LINES.withIndex()) {
            ifSetText(component, MESSAGE_TEXT.getOrElse(index - MESSAGE_FIRST_LINE) { "" })
        }
        if (stage(player) == STAGE_KEY) {
            setStage(this, STAGE_MESSAGE_READ)
        }
    }

    private suspend fun ProtectedAccess.openCasket() {
        if (!openCasket(this)) {
            mes("You don't have enough inventory space to do that.")
            return
        }
        mesbox("You open the casket, and find One-Eyed Hector's treasure.")
    }

    /** Replaces the casket with Hector's treasure in one transaction. */
    fun openCasket(access: ProtectedAccess): Boolean {
        val player = access.player
        val opened =
            player.invTransaction(player.inv) {
                val inv = select(player.inv)
                delete {
                    from = inv
                    obj = CASKET.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
                for ((obj, count) in TREASURE) {
                    insert {
                        into = inv
                        this.obj = obj.asRSCM(RSCMType.OBJ)
                        strictCount = count
                    }
                }
            }
        return opened.success
    }

    /** Digs up the casket and completes the quest; false when there is no room for it. */
    fun unearthTreasure(access: ProtectedAccess): Boolean {
        if (stage(access.player) != STAGE_MESSAGE_READ || !access.inv.hasFreeSpace()) {
            return false
        }
        access.mes("You dig a hole in the ground...")
        access.mes("and find a little chest of treasure.")
        return quest.completeQuest(access)
    }

    private fun Dialogue.customsWarning(): String =
        "The Customs office has been clampin' down on the export of spirits. You seem like a " +
            "resourceful young ${ladOrLass()}, I'm sure ye'll be able to find a way to slip the " +
            "stuff past them."

    private fun Dialogue.ladOrLass(): String =
        if (player.appearance.bodyType == Constants.bodytype_a) "lad" else "lass"

    private enum class FrankTopic {
        Treasure,
        Arr,
        Trade,
    }

    companion object {
        const val STAGE_STARTED = 1
        const val STAGE_KEY = 2
        const val STAGE_MESSAGE_READ = 3
        const val STAGE_COMPLETE = 4

        const val FRANK = "npc.redbeard_frank"
        const val CHEST = "loc.piratechest"
        const val RUM = "obj.karamja_rum"
        const val BANANA_RUM = "obj.tbwt_banana_in_karamja_rum"
        const val SLICED_BANANA_RUM = "obj.tbwt_sliced_banana_in_karamja_rum"
        const val CHEST_KEY = "obj.chest_key"
        const val MESSAGE = "obj.piratemessage"
        const val CASKET = "obj.pirate_casket"
        const val COINS = "obj.coins"

        const val LUTHAS_JOB = "varbit.pirates_treasure_luthas_job"
        const val CRATE_BANANAS = "varbit.pirates_treasure_crate_bananas"
        const val CRATE_RUM = "varbit.pirates_treasure_crate_rum"
        const val WYDIN_RUM = "varbit.pirates_treasure_wydin_rum"
        const val WYDIN_JOB = "varbit.pirates_treasure_wydin_job"
        const val RUM_RETRIEVED = "varbit.pirates_treasure_rum_retrieved"

        const val COOKING_XP = 800.0
        const val NOTHING_INTERESTING = "Nothing interesting happens."

        val TREASURE = listOf(COINS to 450, "obj.gold_ring" to 1, "obj.emerald" to 1)

        private const val RUM_DEAL = "quest_rumdeal"
        private const val LOCKED_SOUND = "synth.locked"
        private const val CUSTOMS_REPLY =
            "Nothin' at the moment, but then again the Customs Agents are on the warpath right now."

        private const val MESSAGE_SCROLL = "interface.messagescroll_handwriting"
        private const val MESSAGE_FIRST_LINE = 4
        private val MESSAGE_LINES =
            listOf(
                "component.messagescroll_handwriting:messagescroll1_hw",
                "component.messagescroll_handwriting:messagescroll2_hw",
                "component.messagescroll_handwriting:messagescroll3_hw",
                "component.messagescroll_handwriting:messagescroll4_hw",
                "component.messagescroll_handwriting:messagescroll5_hw",
                "component.messagescroll_handwriting:messagescroll6_hw",
                "component.messagescroll_handwriting:messagescroll7_hw",
                "component.messagescroll_handwriting:messagescroll8_hw",
                "component.messagescroll_handwriting:messagescroll9_hw",
                "component.messagescroll_handwriting:messagescroll10_hw",
                "component.messagescroll_handwriting:messagescroll11_hw",
            )
        private val MESSAGE_TEXT =
            listOf(
                "Visit the city of the White Knights. In the park,",
                "Saradomin points to the X which marks the spot.",
            )
    }
}

internal fun Player.ownsAnywhere(obj: String): Boolean =
    inv.contains(obj) || worn.contains(obj) || invMap.getOrPut("inv.bank").contains(obj)
