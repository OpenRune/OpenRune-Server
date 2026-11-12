package org.rsmod.content.areas.misc.warriorsguild.kegs

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.hands
import org.rsmod.api.player.hat
import org.rsmod.api.player.lefthand
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpWorn1
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerTimer
import org.rsmod.content.areas.misc.warriorsguild.TokenLedger
import org.rsmod.content.areas.misc.warriorsguild.kegTicks
import org.rsmod.content.areas.misc.warriorsguild.kegTokensOwed
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Jimmy's keg balancing. The kegs on the player's head are the worn barrel obj, so the count is
 * read back from the hat slot; each keg loc the player has lifted is hidden by its own varbit.
 */
class KegScript
@Inject
constructor(private val npcs: NpcList, private val random: GameRandom) : PluginScript() {
    override fun ScriptContext.startup() {
        for (keg in 1..MAX_KEGS) {
            onOpLoc1("loc.war_strength_keg$keg") { pickUp(keg) }
            onOpWorn1(barrel(keg)) { loseBalance() }
        }
        onPlayerTimer(KEG_TIMER) { balance() }
        onPlayerLogout { clearKegs(player) }
        onOpNpc1(JIMMY) { startDialogue(it.npc) { jimmy() } }
    }

    private suspend fun ProtectedAccess.pickUp(keg: Int) {
        val held = player.kegCount()
        val handsFull = player.righthand != null || player.lefthand != null || player.hands != null
        if (handsFull || (held == 0 && player.hat != null)) {
            mes("You must have both your hands as well as your head completely free to balance kegs.")
            return
        }
        if (vars[kegVar(keg)] == 1 || held >= MAX_KEGS) return
        anim("seq.balance_pickup")
        delay(PICKUP_TICKS)
        if (!inRoom(coords) || vars[kegVar(keg)] == 1 || player.kegCount() != held) return
        vars[kegVar(keg)] = 1
        setHead(player, held + 1)
        if (held == 0) {
            player.kegTicks = 0
            timer(KEG_TIMER, 1)
        }
        val name = player.displayName
        jimmyNearby(coords)?.say(
            if (held + 1 <= 3) {
                "Ya got no chance o' beatin' me $name!"
            } else {
                "No one's stronger than me, 'speshly not $name!"
            }
        )
    }

    private fun ProtectedAccess.balance() {
        for (keg in 1..MAX_KEGS) {
            val stray = inv.count(barrel(keg))
            if (stray > 0) invDel(inv, barrel(keg), stray)
        }
        val kegs = player.kegCount()
        if (kegs == 0 || !inRoom(coords)) {
            clearKegs(player)
            return
        }
        if (player.righthand != null || player.lefthand != null || player.hands != null) {
            loseBalance()
            return
        }
        val ticks = player.kegTicks
        if (ticks % DRAIN_EVERY == 0) {
            player.runEnergy = (player.runEnergy - DRAIN).coerceAtLeast(0)
            UpdateRun.energy(player, player.runEnergy)
        }
        player.kegTicks = minOf(ticks + 1, MAX_TICKS)
        val energyPercent = player.runEnergy / ENERGY_PER_PERCENT
        val steadiness = energyPercent * random.of(0, FALL_PRECISION) / FALL_PRECISION
        if (steadiness <= random.of(0, FALL_THRESHOLD)) {
            loseBalance()
            return
        }
        timer(KEG_TIMER, 1)
    }

    private fun ProtectedAccess.loseBalance() {
        val kegs = player.kegCount()
        if (kegs == 0) return
        val ticks = player.kegTicks
        clearKegs(player)
        anim("seq.barrel_fall_play")
        spotanim("spotanim.barrel${kegs}_fall_spotanim")
        queueHit(1, HitType.Typeless, random.of(MIN_FALL_DAMAGE, MAX_FALL_DAMAGE))
        mes("You lose balance and the kegs fall onto your head.")
        player.say("Ouch!")
        if (kegs > 1) {
            statAdvance("stat.strength", (STRENGTH_XP_PER_KEG * kegs).toDouble())
            TokenLedger.creditKegs(player, TOKENS_PER_KEG * kegs + ticks / 2)
        }
        if (kegs >= MAX_KEGS) {
            jimmyNearby(coords)?.say(
                "Wow! That'sh bery impr....imp...impresh.... good ${player.displayName}! Equalsh " +
                    "my record!"
            )
        }
    }

    private suspend fun Dialogue.jimmy() {
        chatNpc(happy, "'Ello there.")
        jimmyOptions()
    }

    private suspend fun Dialogue.jimmyOptions() {
        when (
            choice4(
                "Tell me about this room.",
                Topic.Room,
                "Tell me how to balance kegs.",
                Topic.Kegs,
                "May I claim my tokens please?",
                Topic.Tokens,
                "Bye!",
                Topic.Bye,
            )
        ) {
            Topic.Room -> {
                chatPlayer(quiz, "Tell me about this room?")
                chatNpc(neutral, "Well... s'like thish...")
                chatNpc(neutral, "Thish here'sh a shtore room right?")
                chatPlayer(quiz, "A store room you mean?")
                chatNpc(
                    neutral,
                    "That'sh what I shaid! *HIC* A shtore room.... Now technic'ly shpeaking, I " +
                        "should be outshide guarding it...",
                )
                chatPlayer(quiz, "But you just nipped in to have a quick drink?")
                chatNpc(happy, "Yep... and to practish.")
                chatPlayer(confused, "Practish? I mean.. practise what?")
                chatNpc(happy, "Keg balancin. I'm the besht.")
                jimmyOptions()
            }
            Topic.Kegs -> {
                chatPlayer(quiz, "Tell me how to balance kegs?")
                chatNpc(
                    neutral,
                    "Yer very very shtrange. But.... you pick the keg up, and balance it on yer " +
                        "head, then you pick another keg up and put that on top. S'really very " +
                        "eashy.",
                )
                chatPlayer(confused, "Eashy?")
                chatNpc(neutral, "Yesh. Eashy.")
                chatNpc(happy, "But you couldn't ever balansh ash many ash meee!")
                chatPlayer(happy, "That sounds like a challenge, I'll show you!")
                jimmyOptions()
            }
            Topic.Tokens -> {
                chatPlayer(quiz, "May I claim my tokens please?")
                if (player.kegTokensOwed <= 0) {
                    chatNpc(neutral, "Wha'? You ain't earnt any mate!")
                    return
                }
                chatNpc(
                    neutral,
                    "Well... err.. ish not offishal or anyfin... but I got the ledger of tokensh " +
                        "'ere. I'll jus' err.. write it in!",
                )
                TokenLedger.transferKegs(player)
                chatPlayer(quiz, "Won't they know?")
                chatNpc(
                    happy,
                    "Nah... hic.... I'm a wizsh at copyin' signaturesh! Jus' ashk an offishal " +
                        "mem'er of shtaff like Shloane fer yer tokensh.",
                )
                mesbox(
                    "The rather drunk Jimmy scribbles the tokens you've earned from Keg Balancing " +
                        "in the Ledger so that you can claim them from an official member of " +
                        "training staff.",
                )
            }
            Topic.Bye -> {
                chatPlayer(neutral, "Bye!")
                chatNpc(happy, "Shure you wouldn't like an ickle drinkie fore yer go?")
                chatPlayer(
                    neutral,
                    "No thanks, got things to do, people to see, tokens to earn...",
                )
            }
        }
    }

    private fun jimmyNearby(coords: CoordGrid): Npc? =
        npcs.firstOrNull { it.id == JIMMY_ID && it.coords.chebyshevDistance(coords) <= SPEECH_RANGE }

    private enum class Topic {
        Room,
        Kegs,
        Tokens,
        Bye,
    }

    companion object {
        private const val MAX_KEGS = 5
        private const val PICKUP_TICKS = 2
        private const val KEG_TIMER = "timer.warguild_kegs"
        private const val JIMMY = "npc.war_strength_drunken_warrior"
        private const val DRAIN_EVERY = 10
        private const val DRAIN = 900
        private const val ENERGY_PER_PERCENT = 100
        private const val FALL_PRECISION = 1000
        private const val FALL_THRESHOLD = 15
        private const val MIN_FALL_DAMAGE = 2
        private const val MAX_FALL_DAMAGE = 4
        private const val STRENGTH_XP_PER_KEG = 10
        private const val TOKENS_PER_KEG = 10
        private const val MAX_TICKS = 16383
        private const val SPEECH_RANGE = 15

        private val JIMMY_ID = JIMMY.asRSCM(RSCMType.NPC)
        private val ROOM_MIN = CoordGrid(2861, 3535, 1)
        private val ROOM_MAX = CoordGrid(2878, 3543, 1)

        private fun barrel(count: Int): String = "obj.war_strength_barrel$count"

        private fun kegVar(keg: Int): String = "varbit.war_strength_keg$keg"

        private fun inRoom(coords: CoordGrid): Boolean =
            coords.level == ROOM_MIN.level &&
                coords.x in ROOM_MIN.x..ROOM_MAX.x &&
                coords.z in ROOM_MIN.z..ROOM_MAX.z

        fun Player.kegCount(): Int {
            val head = hat ?: return 0
            return (1..MAX_KEGS).firstOrNull { head.id == barrel(it).asRSCM(RSCMType.OBJ) } ?: 0
        }

        private fun setHead(player: Player, count: Int) {
            player.hat = InvObj(checkNotNull(ServerCacheManager.getItem(barrel(count).asRSCM())), 1)
            player.bas = ServerCacheManager.getBas(BALANCE_BAS.asRSCM(RSCMType.BAS))
            player.rebuildAppearance()
        }

        fun clearKegs(player: Player) {
            if (player.kegCount() > 0) player.hat = null
            for (keg in 1..MAX_KEGS) VarPlayerIntMapSetter.set(player, kegVar(keg), 0)
            player.bas = null
            player.kegTicks = 0
            player.clearTimer(KEG_TIMER)
            player.rebuildAppearance()
        }

        private const val BALANCE_BAS = "bas.warguild_balance"
    }
}
