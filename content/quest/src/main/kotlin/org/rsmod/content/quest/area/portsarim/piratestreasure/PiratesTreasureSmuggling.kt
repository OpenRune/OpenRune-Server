package org.rsmod.content.quest.area.portsarim.piratestreasure

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.area.portsarim.piratestreasure.PiratesTreasureQuest.Companion.BANANA_RUM
import org.rsmod.content.quest.area.portsarim.piratestreasure.PiratesTreasureQuest.Companion.COINS
import org.rsmod.content.quest.area.portsarim.piratestreasure.PiratesTreasureQuest.Companion.NOTHING_INTERESTING
import org.rsmod.content.quest.area.portsarim.piratestreasure.PiratesTreasureQuest.Companion.RUM
import org.rsmod.content.quest.area.portsarim.piratestreasure.PiratesTreasureQuest.Companion.SLICED_BANANA_RUM
import org.rsmod.content.quest.area.portsarim.piratestreasure.PiratesTreasureQuest.Companion.STAGE_COMPLETE
import org.rsmod.content.quest.area.portsarim.piratestreasure.PiratesTreasureQuest.Companion.STAGE_STARTED
import org.rsmod.content.quest.manager.menu
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PiratesTreasureSmuggling
@Inject
constructor(
    private val quest: PiratesTreasureQuest,
    private val objRepo: ObjRepository,
) : PluginScript() {
    private var Player.luthasJob by intVarBit("varbit.pirates_treasure_luthas_job")
    private var Player.crateBananas by intVarBit("varbit.pirates_treasure_crate_bananas")
    private var Player.crateRum by intVarBit("varbit.pirates_treasure_crate_rum")
    private var Player.wydinRum by intVarBit("varbit.pirates_treasure_wydin_rum")
    private var Player.rumRetrieved by intVarBit("varbit.pirates_treasure_rum_retrieved")
    private var Player.wydinJob by intVarBit("varbit.pirates_treasure_wydin_job")

    override fun ScriptContext.startup() {
        onOpNpc1(LUTHAS) { startDialogue(it.npc) { luthas() } }
        onOpLoc1(BANANA_CRATE) { searchBananaCrate() }
        onOpLoc2(BANANA_CRATE) { fillBananaCrate() }
        onOpLocU(BANANA_CRATE) { itemOnBananaCrate(it.objType.internalName) }
        onOpLoc1(GROCERY_CRATE) { searchGroceryCrate() }
    }

    private suspend fun Dialogue.luthas() {
        if (player.luthasJob == 1 && player.crateBananas >= CRATE_CAPACITY) {
            return luthasPays()
        }
        if (player.luthasJob == 1) {
            chatNpc(quiz, "Have you completed your task yet?")
            val topic =
                menu(
                    "What did I have to do again?" to LuthasTopic.Task,
                    "No, the crate isn't full yet." to LuthasTopic.NotFull,
                    "So where are these bananas going to be delivered to?" to LuthasTopic.Where,
                    "That customs officer is annoying isn't she?" to LuthasTopic.Customs,
                )
            when (topic) {
                LuthasTopic.Task -> {
                    chatPlayer(quiz, "What did I have to do again?")
                    chatNpc(
                        neutral,
                        "There's a crate ready to be loaded onto the ship. If you could fill it " +
                            "up with bananas, I'll pay you 30 gold.",
                    )
                }
                LuthasTopic.NotFull -> {
                    chatPlayer(neutral, "No, the crate isn't full yet...")
                    chatNpc(neutral, "Well come back when it is.")
                }
                else -> luthasTopic(topic)
            }
            return
        }
        chatNpc(happy, "Hello I'm Luthas, I run the banana plantation here.")
        val topic =
            menu(
                "Could you offer me employment on your plantation?" to LuthasTopic.Employment,
                "That customs officer is annoying isn't she?" to LuthasTopic.Customs,
            )
        luthasTopic(topic)
    }

    private suspend fun Dialogue.luthasPays() {
        chatPlayer(happy, "I've filled a crate with bananas.")
        chatNpc(happy, "Well done, here's your payment.")
        payForCrate(access)
        access.mes("Luthas hands you 30 coins.")
        val topic =
            menu(
                "Will you pay me for another crate full?" to LuthasTopic.Another,
                "Thank you, I'll be on my way" to LuthasTopic.Leave,
                "So where are these bananas going to be delivered to?" to LuthasTopic.Where,
                "That customs officer is annoying isn't she?" to LuthasTopic.Customs,
            )
        luthasTopic(topic)
    }

    private suspend fun Dialogue.luthasTopic(topic: LuthasTopic) {
        when (topic) {
            LuthasTopic.Employment -> {
                chatPlayer(quiz, "Could you offer me employment on your plantation?")
                chatNpc(
                    happy,
                    "Yes, I can sort something out. There's a crate ready to be loaded onto the " +
                        "ship.",
                )
                chatNpc(
                    neutral,
                    "You wouldn't believe the demand for bananas from Wydin's shop over in Port " +
                        "Sarim. I think this is the third crate I've shipped him this month..",
                )
                player.luthasJob = 1
                chatNpc(happy, "If you could fill it up with bananas, I'll pay you 30 gold.")
            }
            LuthasTopic.Customs -> {
                chatPlayer(shifty, "That customs officer is annoying isn't she?")
                chatNpc(
                    neutral,
                    "Well I know her pretty well. She doesn't cause me any trouble any more.",
                )
                chatNpc(
                    neutral,
                    "She doesn't even search my export crates any more. She knows they only " +
                        "contain bananas.",
                )
                chatPlayer(shifty, "Really? How interesting. Whereabouts do you send those to?")
                chatNpc(
                    neutral,
                    "There is a little shop over in Port Sarim that buys them up by the crate. I " +
                        "believe it is run by a man called Wydin.",
                )
            }
            LuthasTopic.Where -> {
                chatPlayer(quiz, "So where are these bananas going to be delivered to?")
                chatNpc(neutral, "I sell them to Wydin who runs a grocery store in Port Sarim.")
            }
            LuthasTopic.Another -> {
                chatPlayer(quiz, "Will you pay me for another crate full?")
                chatNpc(happy, "Yes certainly.")
                player.luthasJob = 1
                chatNpc(
                    neutral,
                    "If you go outside you should see the old crate has been loaded on to the " +
                        "ship, and there is another empty crate in its place.",
                )
            }
            LuthasTopic.Leave -> chatPlayer(happy, "Thank you, I'll be on my way.")
            else -> Unit
        }
    }

    /**
     * Ships the full crate: Luthas pays 30 coins, the job ends and any stashed rum travels on to
     * Wydin's store. False if the crate was not full.
     */
    fun payForCrate(access: ProtectedAccess): Boolean {
        val player = access.player
        if (player.luthasJob != 1 || player.crateBananas < CRATE_CAPACITY) {
            return false
        }
        player.crateBananas = 0
        player.luthasJob = 0
        if (player.crateRum == 1) {
            player.crateRum = 0
            player.wydinRum = 1
        }
        access.invAddOrDrop(objRepo, COINS, CRATE_PAY)
        return true
    }

    private fun ProtectedAccess.searchBananaCrate() {
        val bananas = player.crateBananas
        if (bananas == 0 && player.crateRum == 1) {
            mes(
                "There is some rum in here, although with no bananas to cover it. It is a " +
                    "little obvious."
            )
            return
        }
        when {
            bananas >= CRATE_CAPACITY -> mes("The crate is full of bananas.")
            bananas == 0 -> mes("The crate is completely empty.")
            bananas == 1 -> mes("The crate has 1 banana inside.")
            else -> mes("The crate has $bananas bananas inside.")
        }
        if (player.crateRum == 1) {
            mes("There is also some rum stashed in here too.")
        }
    }

    private suspend fun ProtectedAccess.fillBananaCrate() {
        if (player.luthasJob == 0) {
            mes("I don't know what goes in there.")
            return
        }
        if (player.crateBananas >= CRATE_CAPACITY) {
            mes("The crate is already full.")
            return
        }
        val packed = packBananas(this, CRATE_CAPACITY)
        if (packed == 0) {
            mes("You don't have any bananas.")
            return
        }
        packAnimation()
        if (player.crateBananas >= CRATE_CAPACITY) {
            mesbox("You fill the crate with bananas.")
        } else {
            mesbox("You pack all your bananas into the crate.")
        }
    }

    private suspend fun ProtectedAccess.itemOnBananaCrate(obj: String) {
        when (obj) {
            BANANA -> packOneBanana()
            RUM -> stashRumInCrate()
            BANANA_RUM ->
                if (quest.stage(player) == STAGE_STARTED) {
                    mesbox("Pirates don't usually take banana with their Karamjan rum.")
                } else {
                    mes(NO_REASON)
                }
            SLICED_BANANA_RUM ->
                if (quest.stage(player) == STAGE_STARTED) {
                    mesbox("Pirates don't usually take sliced banana with their Karamjan rum.")
                } else {
                    mes(NO_REASON)
                }
            else -> mes(NOTHING_INTERESTING)
        }
    }

    private suspend fun ProtectedAccess.packOneBanana() {
        if (player.luthasJob == 0) {
            mes("Why would I want to do that?")
            return
        }
        if (player.crateBananas >= CRATE_CAPACITY) {
            mes("The crate is already full.")
            return
        }
        if (packBananas(this, 1) == 0) {
            return
        }
        packAnimation()
        mesbox("You pack a banana into the crate.")
    }

    private suspend fun ProtectedAccess.stashRumInCrate() {
        if (quest.stage(player) != STAGE_STARTED) {
            mes(NO_REASON)
            return
        }
        if (player.wydinRum == 1) {
            mes("There is already some rum in Wydin's store, I should go and get that first.")
            return
        }
        if (player.luthasJob == 0) {
            mes("Why would I want to do that?")
            return
        }
        if (player.crateRum == 1) {
            mes("There's already some rum in here...")
            return
        }
        if (!stashRum(this)) {
            return
        }
        packAnimation()
        mesbox("You stash the rum in the crate.")
    }

    /** Moves up to [max] bananas from the inventory into the crate; returns how many moved. */
    fun packBananas(access: ProtectedAccess, max: Int): Int {
        val player = access.player
        val space = CRATE_CAPACITY - player.crateBananas
        val amount = min(min(max, space), player.inv.count(BANANA))
        if (amount <= 0) {
            return 0
        }
        if (access.invDel(player.inv, BANANA, amount).failure) {
            return 0
        }
        player.crateBananas += amount
        return amount
    }

    fun stashRum(access: ProtectedAccess): Boolean {
        val player = access.player
        if (player.luthasJob == 0 || player.crateRum == 1 || player.wydinRum == 1) {
            return false
        }
        if (quest.stage(player) != STAGE_STARTED) {
            return false
        }
        if (access.invDel(player.inv, RUM).failure) {
            return false
        }
        player.crateRum = 1
        player.rumRetrieved = 0
        return true
    }

    private fun ProtectedAccess.packAnimation() {
        anim(PACK_SEQ)
        soundSynth(PACK_SOUND)
    }

    fun isHiredByWydin(player: Player): Boolean = player.wydinJob == 1

    fun canAskWydinForJob(player: Player): Boolean =
        quest.stage(player) in STAGE_STARTED until STAGE_COMPLETE && player.wydinJob == 0

    suspend fun Dialogue.wydinHired(openShop: () -> Unit) {
        chatNpc(quiz, "Is it nice and tidy round the back now?")
        val topic =
            menu(
                "Yes, can I work out front now?" to 1,
                "Yes, are you going to pay me yet?" to 2,
                "No, it's a complete mess." to 3,
                "Can I buy something please?" to 4,
            )
        when (topic) {
            1 -> {
                chatPlayer(quiz, "Yes, can I work out front now?")
                chatNpc(neutral, "No, I'm the one who works here.")
            }
            2 -> {
                chatPlayer(quiz, "Yes, are you going to pay me yet?")
                chatNpc(shifty, "Umm... No, not yet.")
            }
            3 -> {
                chatPlayer(sad, "No, it's a complete mess.")
                chatNpc(laugh, "Ah well, it'll give you something to do, won't it.")
            }
            else -> {
                chatPlayer(quiz, "Can I buy something please?")
                chatNpc(happy, "Yes, of course.")
                openShop()
            }
        }
    }

    suspend fun Dialogue.askWydinForJob() {
        chatPlayer(quiz, "Can I get a job here?")
        chatNpc(
            happy,
            "Well, you're keen, I'll give you that. Okay, I'll give you a go. Have you got your " +
                "own white apron?",
        )
        val wearing = player.worn.contains(APRON)
        if (!wearing && player.inv.count(APRON) == 0) {
            chatPlayer(sad, "No, I haven't.")
            chatNpc(
                neutral,
                "Well, you can't work here unless you have a white apron. Health and safety " +
                    "regulations, you understand.",
            )
            chatPlayer(quiz, "Where can I get one of those?")
            chatNpc(
                neutral,
                "Well, I get all of mine over at the clothing shop in Varrock. They sell them " +
                    "cheap there.",
            )
            chatNpc(
                neutral,
                "Oh, and I'm sure that I've seen a spare one over in Gerrant's fish store " +
                    "somewhere. It's the little place just north of here.",
            )
            return
        }
        chatPlayer(happy, "Yes, I have one right here.")
        player.wydinJob = 1
        chatNpc(
            happy,
            "Wow - you are well prepared! You're hired. Go through to the back and tidy up for " +
                "me, please.",
        )
        if (!wearing) {
            chatNpc(neutral, "You need to put your white apron on first though.")
        }
    }

    private suspend fun ProtectedAccess.searchGroceryCrate() {
        arriveDelay()
        mes("There are a lot of bananas in the crate.")
        if (player.wydinRum == 1) {
            if (takeRumFromGroceryCrate(this)) {
                mes("You find your bottle of rum in amongst the bananas.")
            } else {
                mes("You don't have enough inventory space to do that.")
            }
            return
        }
        var take = false
        startDialogue {
            take = choice2("Yes.", true, "No.", false, title = "Do you want to take a banana?")
        }
        if (take && invAdd(inv, BANANA).success) {
            mes("You take a banana.")
        }
    }

    fun takeRumFromGroceryCrate(access: ProtectedAccess): Boolean {
        val player = access.player
        if (player.wydinRum == 0) {
            return false
        }
        if (access.invAdd(player.inv, RUM).failure) {
            return false
        }
        player.wydinRum = 0
        player.rumRetrieved = 1
        return true
    }

    private enum class LuthasTopic {
        Employment,
        Customs,
        Task,
        NotFull,
        Where,
        Another,
        Leave,
    }

    companion object {
        const val LUTHAS = "npc.luthas"
        const val WYDIN = "npc.wydin"
        const val BANANA_CRATE = "loc.bananacrate"
        const val GROCERY_CRATE = "loc.grocerycrate"
        const val BANANA = "obj.banana"
        const val APRON = "obj.white_apron"
        const val CRATE_CAPACITY = 10
        const val CRATE_PAY = 30

        private const val NO_REASON = "I see no reason to do that."
        private const val PACK_SEQ = "seq.human_pickuptable"
        private const val PACK_SOUND = "synth.put_down"
    }
}

/** Takes every Karamjan rum from the inventory, as the Karamja customs officers do. */
fun confiscateRum(access: ProtectedAccess): Int {
    val count = access.inv.count(RUM)
    if (count == 0) {
        return 0
    }
    return if (access.invDel(access.inv, RUM, count).success) count else 0
}
