package org.rsmod.content.events.randomevents

import dev.openrune.types.NpcMode
import dev.or2.central.account.Rights
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.Constants
import org.rsmod.api.npc.owner.assignSpawnOwner
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc5
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.content.quest.manager.chooseLampSkill
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

@Singleton
class GenieRandomEvent
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val objRepo: ObjRepository,
    private val collision: CollisionFlagMap,
    private val mapClock: MapClock,
    private val random: GameRandom,
) : PluginScript() {
    private val genies = HashMap<Player, Genie>()

    override fun ScriptContext.startup() {
        onPlayerLogin { player.softTimer(SCHEDULE_TIMER, nextEventDelay()) }
        onPlayerLogout { genies.remove(player)?.let { leave(it.npc) } }
        onPlayerSoftTimer(SCHEDULE_TIMER) {
            player.softTimer(SCHEDULE_TIMER, nextEventDelay())
            if (player !in genies) spawn(player)
        }
        onPlayerSoftTimer(GENIE_TIMER) { tick(player) }
        onOpNpc1(GENIE) { talk(it.npc) }
        onOpNpc5(GENIE) { dismiss(it.npc) }
        onOpHeld1(LAMP) { rub() }
        onCommand("randomevent") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Spawn the Genie random event beside you"
            cheat(::summon)
        }
    }

    private fun summon(cheat: Cheat) {
        val player = cheat.player
        if (player in genies) {
            player.mes("Your Genie is already here.")
            return
        }
        spawn(player)
    }

    fun spawn(player: Player): Npc {
        val npc = Npc(GENIE, besideTile(player.coords))
        npc.mode = NpcMode.None
        npcRepo.add(npc, Int.MAX_VALUE)
        npc.respawns = false
        npc.assignSpawnOwner(player, mapClock.cycle)
        npc.facePlayer(player)
        npc.say("Greetings, ${title(player)} ${player.displayName}!")
        player.softTimer(GENIE_TIMER, CALL_INTERVAL)
        genies[player] = Genie(npc, mapClock.cycle + LIFETIME_TICKS)
        return npc
    }

    fun genieOf(player: Player): Npc? = genies[player]?.npc

    private fun tick(owner: Player) {
        val genie = genies[owner]
        if (genie == null || !genie.npc.isSlotAssigned) {
            genies.remove(owner)
            return
        }
        val npc = genie.npc
        if (genie.handedOver) {
            genies.remove(owner)
            leave(npc)
            return
        }
        if (mapClock.cycle >= genie.leaveCycle) {
            npc.say("Maybe later.")
            genies.remove(owner)
            leave(npc)
            return
        }
        val besideOwner =
            npc.coords.level == owner.coords.level && npc.coords.chebyshevDistance(owner.coords) <= 1
        if (!besideOwner) {
            npc.teleport(collision, besideTile(owner.coords))
        }
        npc.facePlayer(owner)
        val name = "${title(owner)} ${owner.displayName}"
        npc.say(random.pick("Hello, $name?", "Ehem... $name?", "I'm waiting for you, $name!"))
        owner.softTimer(GENIE_TIMER, CALL_INTERVAL)
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) {
        val entry = genies.entries.firstOrNull { it.value.npc === npc }
        if (entry == null || entry.key !== player) {
            startDialogue(npc) {
                chatNpc(
                    neutral,
                    "Sorry, I'm trying to talk to someone else, but they seem to be ignoring me.",
                )
            }
            return
        }
        val genie = entry.value
        if (genie.handedOver) {
            startDialogue(npc) { chatNpc(happy, "Have a nice day, ${title(player)}.") }
            return
        }
        genie.handedOver = true
        invAddOrDrop(objRepo, LAMP, 1, inv = inv)
        mes("Your reward is: <col=ff0000>1</col> x <col=ff0000>Lamp</col>.")
        startDialogue(npc) {
            chatNpc(
                happy,
                "Ah, so you are there, ${title(player)} ${player.displayName}. I'm so glad you " +
                    "summoned me. Please take this lamp and make your wish.",
            )
        }
    }

    private fun ProtectedAccess.dismiss(npc: Npc) {
        val owner = genies.entries.firstOrNull { it.value.npc === npc }?.key
        if (owner !== player) {
            mes("He isn't interested in you.")
            return
        }
        genies.remove(player)
        leave(npc)
    }

    private suspend fun ProtectedAccess.rub() {
        val stat = chooseLampSkill("Choose the stat you wish to be advanced!") ?: return
        val xp = statBase(stat) * LAMP_XP_PER_LEVEL
        if (invDel(inv, LAMP).failure) return
        statAdvance(stat, xp.toDouble(), rate = 1.0, globalRate = 1.0)
        val skill = stat.substringAfter('.').replaceFirstChar { it.uppercase() }
        mes("Your wish has been granted! You have been awarded $xp $skill XP!")
    }

    private fun leave(npc: Npc) {
        if (npc.isSlotAssigned) npcRepo.del(npc, Int.MAX_VALUE)
    }

    private fun besideTile(centre: CoordGrid): CoordGrid {
        for ((x, z) in ADJACENT_OFFSETS) {
            val candidate = centre.translate(x, z)
            if (collision[candidate.x, candidate.z, candidate.level] and BLOCKED_FLAGS == 0) {
                return candidate
            }
        }
        return centre
    }

    private fun title(player: Player): String =
        if (player.appearance.bodyType == Constants.bodytype_a) "Master" else "Mistress"

    private fun nextEventDelay(): Int = random.of(MIN_EVENT_DELAY, MAX_EVENT_DELAY)

    private data class Genie(val npc: Npc, val leaveCycle: Int, var handedOver: Boolean = false)

    companion object {
        const val GENIE = "npc.macro_geni"
        const val LAMP = "obj.macro_genilamp"
        const val SCHEDULE_TIMER = "timer.random_event"
        const val GENIE_TIMER = "timer.random_event_genie"
        const val LIFETIME_TICKS = 100
        const val CALL_INTERVAL = 20
        const val LAMP_XP_PER_LEVEL = 10
        const val MIN_EVENT_DELAY = 6_000
        const val MAX_EVENT_DELAY = 12_000
        const val BLOCKED_FLAGS = CollisionFlag.BLOCK_WALK or CollisionFlag.BLOCK_PLAYERS
        val ADJACENT_OFFSETS =
            listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1, -1 to -1, 1 to -1, -1 to 1, 1 to 1)
    }
}
