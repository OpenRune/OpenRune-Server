package org.rsmod.content.quest.area.varrock.shieldofarrav

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onAiTimer
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.menu
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Single-player stand-in for the partner from the other gang. Hidden by multivar on the gang varps
 * except where the player needs him: by the south gate for the Black Arm key, and in the museum
 * for the certificate swap.
 */
class NotArrav
@Inject
constructor(private val players: PlayerList, private val random: GameRandom) : PluginScript() {
    private var Player.callWait by intVarBit("varbit.notarrav_call_wait")

    override fun ScriptContext.startup() {
        onAiTimer(NOTARRAV_BLACKARM) { callOut(npc) { it.blackArmGang == BLACKARM_TASK } }
        onAiTimer(NOTARRAV_BLACKARM_MUSEUM) { callOut(npc) { it.isBlackArmMember } }
        onAiTimer(NOTARRAV_PHOENIX) { callOut(npc) { it.isPhoenixMember } }
        onOpNpc1(NOTARRAV) { startDialogue(it.npc) { notArrav() } }
        onOpNpc1(NOTARRAV_BLACKARM) { startDialogue(it.npc) { notArrav() } }
        onOpNpc1(NOTARRAV_BLACKARM_MUSEUM) { startDialogue(it.npc) { notArrav() } }
        onOpNpc1(NOTARRAV_PHOENIX) { startDialogue(it.npc) { notArrav() } }
    }

    /**
     * Hails every nearby player who can see this spawn and still needs him: straight away when
     * they come within [CALL_DISTANCE], then every [CALL_INTERVAL] timer fires while they stay.
     */
    private fun callOut(notArrav: Npc, seesHim: (Player) -> Boolean) {
        for (player in players) {
            if (!seesHim(player) || offerFor(player) == null) continue
            val near =
                player.coords.level == notArrav.coords.level &&
                    player.coords.chebyshevDistance(notArrav.coords) <= CALL_DISTANCE
            if (!near) {
                player.callWait = 0
                continue
            }
            if (player.callWait > 0) {
                player.callWait--
                continue
            }
            player.callWait = CALL_INTERVAL
            val line = random.pick(CALLS).replace(NAME, player.displayName)
            notArrav.say(line)
            player.mes("NotArrav|$line", ChatType.NpcSay)
        }
    }

    private suspend fun Dialogue.notArrav() {
        chatNpc(
            shifty,
            "Psst! Don't mind me. I'm definitely not Arrav. Just a humble trader with a very real " +
                "moustache.",
        )
        val offer = offerFor(player)
        if (offer == null) {
            chatNpc(
                neutral,
                if (waitingOnCurator(player)) {
                    "Show your half of the shield to the curator first. Then come and see me " +
                        "about the other half of the certificate."
                } else {
                    "I've got nothing you need right now. Off you go!"
                },
            )
            return
        }
        val choice =
            menu(
                offer.request to 0,
                "Who are you really?" to 1,
                "No thanks." to 2,
            )
        when (choice) {
            0 -> sell(offer)
            1 -> {
                chatPlayer(quiz, "Who are you really?")
                chatNpc(
                    shifty,
                    "Nobody important! A friend of a friend in the ${offer.gang}. They couldn't " +
                        "make it, so I'm doing the swap for them.",
                )
            }
            else -> chatPlayer(neutral, "No thanks.")
        }
    }

    private suspend fun Dialogue.sell(offer: Offer) {
        chatPlayer(neutral, offer.request)
        if (offerFor(player) != offer) {
            return
        }
        val spare = offer.swaps
        if (spare != null && player.inv.count(spare) < 2) {
            chatNpc(
                neutral,
                "It's a swap, friend. Bring me your spare half-certificate and keep the other " +
                    "one for yourself. The curator will write you more if you need them.",
            )
            return
        }
        val take = listOfNotNull(COINS to offer.price, spare?.let { it to 1 })
        val sold =
            ShieldOfArravExchange.exchange(player, take = take, give = listOf(offer.item to 1))
        if (!sold) {
            val reason =
                if (player.inv.count(COINS) < offer.price) "You don't have enough coins."
                else "You don't have enough space in your inventory."
            mesbox(reason)
            return
        }
        objbox(offer.item, "NotArrav hands you the ${offer.label}.")
        chatNpc(happy, "Pleasure doing business. You never saw me.")
    }

    private data class Offer(
        val item: String,
        val price: Int,
        val label: String,
        val gang: String,
        val swaps: String? = null,
    ) {
        val request: String
            get() =
                if (swaps == null) "I'll buy the $label for ${coins(price)} coins."
                else "I'll swap my spare half-certificate and ${coins(price)} coins for yours."
    }

    private companion object {
        fun coins(amount: Int): String = "%,d".format(java.util.Locale.US, amount)

        const val NOTARRAV = "npc.notarrav"
        const val NOTARRAV_BLACKARM = "npc.notarrav_blackarm"
        const val NOTARRAV_BLACKARM_MUSEUM = "npc.notarrav_blackarm_museum"
        const val NOTARRAV_PHOENIX = "npc.notarrav_phoenix"
        const val BLACKARM_PRICE = 500
        const val PHOENIX_PRICE = 1_000
        const val CALL_DISTANCE = 6
        const val CALL_INTERVAL = 10
        const val NAME = "[name]"

        val CALLS =
            listOf(
                "Psst! Over here, [name]!",
                "You look like you need some help, [name].",
                "Psst, [name]! I've got something you're after.",
            )

        val KEY_OFFER = Offer(WEAPON_STORE_KEY, BLACKARM_PRICE, "weapon store key", "Phoenix Gang")
        val PHOENIX_HALF_OFFER =
            Offer(PHOENIX_HALF, BLACKARM_PRICE, "half certificate", "Phoenix Gang", BLACKARM_HALF)
        val BLACKARM_HALF_OFFER =
            Offer(BLACKARM_HALF, PHOENIX_PRICE, "half certificate", "Black Arm Gang", PHOENIX_HALF)

        fun offerFor(player: Player): Offer? =
            when {
                !player.isOnQuest -> null
                player.blackArmGang == BLACKARM_TASK && !player.owns(WEAPON_STORE_KEY) -> KEY_OFFER
                player.isBlackArmMember && needsHalf(player, PHOENIX_HALF) -> PHOENIX_HALF_OFFER
                player.isPhoenixMember && needsHalf(player, BLACKARM_HALF) -> BLACKARM_HALF_OFFER
                else -> null
            }

        fun needsHalf(player: Player, half: String): Boolean =
            player.shieldGiven != 0 && !player.owns(half) && !player.owns(CERTIFICATE)

        fun waitingOnCurator(player: Player): Boolean =
            player.isOnQuest &&
                (player.isBlackArmMember || player.isPhoenixMember) &&
                player.shieldGiven == 0
    }
}
