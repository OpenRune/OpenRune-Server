package org.rsmod.content.events.randomevents

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import dev.openrune.types.aconverted.SpotanimType
import dev.or2.central.account.Rights
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.Constants
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.npc.owner.assignSpawnOwner
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc5
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.server.config.ServerConfig
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

// Epoch minutes of the next random event; a real-time clock that also runs while offline.
private var Player.eventDue by intVarp("varp.random_event_due")

@Singleton
class GenieRandomEvent
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val objRepo: ObjRepository,
    private val worldRepo: WorldRepository,
    private val collision: CollisionFlagMap,
    private val mapClock: MapClock,
    private val random: GameRandom,
    private val config: ServerConfig,
) : PluginScript() {
    private val genies = HashMap<Player, Genie>()

    override fun ScriptContext.startup() {
        if (config.gameplay.randomEvents) {
            onPlayerLogin { login(player) }
            onPlayerSoftTimer(SCHEDULE_TIMER) { check(player) }
        }
        onPlayerLogout { genies.remove(player)?.let { leave(it.npc) } }
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

    private fun login(player: Player) {
        val now = nowMinutes()
        var due = if (player.eventDue == 0) now + eventDelay() else player.eventDue
        while (due <= now) {
            due += offlineExtension()
        }
        player.eventDue = due
        player.softTimer(SCHEDULE_TIMER, CHECK_INTERVAL)
    }

    private fun check(player: Player) {
        if (nowMinutes() < player.eventDue) {
            player.softTimer(SCHEDULE_TIMER, CHECK_INTERVAL)
            return
        }
        // Make-X repeats run as weak queues.
        if (player.ui.modals.isNotEmpty() || player.weakQueueList.isNotEmpty) {
            player.softTimer(SCHEDULE_TIMER, 1)
            return
        }
        player.eventDue = nowMinutes() + eventDelay()
        player.softTimer(SCHEDULE_TIMER, CHECK_INTERVAL)
        if (player !in genies && player.transmog == null) spawn(player)
    }

    private fun summon(cheat: Cheat) {
        val player = cheat.player
        if (player in genies) {
            player.mes("Your Genie is already here.")
            return
        }
        spawn(player)
    }

    private fun spawn(player: Player): Npc {
        val npc = Npc(GENIE, besideTile(player.coords))
        npc.mode = NpcMode.None
        npcRepo.add(npc, Int.MAX_VALUE)
        npc.respawns = false
        npc.assignSpawnOwner(player, mapClock.cycle)
        player.softTimer(GENIE_TIMER, 1)
        genies[player] = Genie(npc)
        return npc
    }

    private fun greet(owner: Player, npc: Npc) {
        npc.facePlayer(owner)
        npc.anim(WAVE)
        npc.say("Greetings, ${title(owner)} ${owner.displayName}!")
        smokePuff(npc.coords)
        worldRepo.soundArea(npc.coords, ARRIVE_SOUND, radius = SOUND_RADIUS)
        smokePuff(npc.coords)
        worldRepo.soundArea(npc.coords, PUFF_SOUND, radius = SOUND_RADIUS)
        owner.softTimer(GENIE_TIMER, CALL_INTERVAL - 1)
    }

    private fun tick(owner: Player) {
        val genie = genies[owner]
        if (genie == null || !genie.npc.isSlotAssigned) {
            genies.remove(owner)
            return
        }
        val npc = genie.npc
        if (genie.handedOver || genie.ignored || owner.transmog != null) {
            genies.remove(owner)
            leave(npc)
            return
        }
        if (genie.calls == 0) {
            genie.calls = 1
            greet(owner, npc)
            return
        }
        if (genie.calls == LIFETIME_TICKS / CALL_INTERVAL) {
            npc.say("Maybe later.")
            genie.ignored = true
            owner.softTimer(GENIE_TIMER, IGNORED_LEAVE_TICKS)
            return
        }
        val besideOwner =
            npc.coords.level == owner.coords.level && npc.coords.chebyshevDistance(owner.coords) <= 1
        if (!besideOwner) {
            npc.teleport(collision, besideTile(owner.coords))
        }
        npc.facePlayer(owner)
        npc.anim(WAVE)
        val name = "${title(owner)} ${owner.displayName}"
        npc.say(random.pick("Hello, $name?", "Ehem... $name?", "I'm waiting for you, $name!"))
        genie.calls++
        owner.softTimer(GENIE_TIMER, CALL_INTERVAL)
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) {
        val entry = genies.entries.firstOrNull { it.value.npc === npc }
        if (entry == null || entry.key !== player) {
            val pronoun = if (entry != null && isMale(entry.key)) "he" else "she"
            startDialogue(npc) {
                chatNpc(
                    neutral,
                    "Sorry, I'm trying to talk to someone else, but $pronoun seems to be ignoring me.",
                )
            }
            return
        }
        val genie = entry.value
        if (genie.handedOver || genie.ignored) {
            startDialogue(npc) { chatNpc(happy, "Have a nice day, ${title(player)}.") }
            return
        }
        genie.handedOver = true
        player.softTimer(GENIE_TIMER, HAND_OVER_TICKS)
        player.invAddOrDrop(objRepo, LAMP, duration = FULL_INV_DROP_TICKS)
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
        val xp = stat(stat) * LAMP_XP_PER_LEVEL
        if (invDel(inv, LAMP).failure) return
        statAdvance(stat, xp.toDouble(), rate = 1.0, globalRate = 1.0)
        soundSynth(WISH_SOUND)
        val skill =
            ServerCacheManager.getStats(stat.asRSCM(RSCMType.STAT))
                ?.displayName
                ?.replaceFirstChar(Char::uppercaseChar)
        mesbox("Your wish has been granted!<br>You have been awarded $xp $skill XP!")
    }

    private fun leave(npc: Npc) {
        if (!npc.isSlotAssigned) return
        val coords = npc.coords
        npcRepo.del(npc, Int.MAX_VALUE)
        smokePuff(coords)
        worldRepo.soundArea(coords, PUFF_SOUND, radius = SOUND_RADIUS)
    }

    private fun smokePuff(coords: CoordGrid) {
        worldRepo.spotanimMap(SpotanimType(SMOKE_PUFF.asRSCM(RSCMType.SPOTANIM)), coords, SMOKE_HEIGHT)
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

    private fun isMale(player: Player): Boolean =
        player.appearance.bodyType == Constants.bodytype_a

    private fun title(player: Player): String = if (isMale(player)) "Master" else "Mistress"

    private fun nowMinutes(): Int = (System.currentTimeMillis() / 60_000).toInt()

    private fun eventDelay(): Int = random.of(MIN_EVENT_DELAY, MAX_EVENT_DELAY)

    private fun offlineExtension(): Int = random.of(MIN_OFFLINE_EXTENSION, MAX_OFFLINE_EXTENSION)

    private class Genie(
        val npc: Npc,
        var calls: Int = 0,
        var handedOver: Boolean = false,
        var ignored: Boolean = false,
    )

    companion object {
        const val GENIE = "npc.macro_geni"
        const val LAMP = "obj.macro_genilamp"
        const val SCHEDULE_TIMER = "timer.random_event"
        const val GENIE_TIMER = "timer.random_event_genie"
        const val WAVE = "seq.emote_wave"
        const val SMOKE_PUFF = "spotanim.smokepuff"
        const val ARRIVE_SOUND = "synth.macro_genie_appear"
        const val PUFF_SOUND = "synth.smokepuff"
        const val WISH_SOUND = "synth.found_gem"
        private const val SMOKE_HEIGHT = 124
        private const val SOUND_RADIUS = 7
        private const val LIFETIME_TICKS = 100
        private const val CALL_INTERVAL = 10
        private const val HAND_OVER_TICKS = 10
        private const val IGNORED_LEAVE_TICKS = 3
        private const val CHECK_INTERVAL = 500
        private const val FULL_INV_DROP_TICKS = 18_000
        const val LAMP_XP_PER_LEVEL = 10
        const val MIN_EVENT_DELAY = 60
        const val MAX_EVENT_DELAY = 120
        const val MIN_OFFLINE_EXTENSION = 10
        const val MAX_OFFLINE_EXTENSION = 70
        const val BLOCKED_FLAGS = CollisionFlag.BLOCK_WALK or CollisionFlag.BLOCK_PLAYERS
        val ADJACENT_OFFSETS =
            listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1, -1 to -1, 1 to -1, -1 to 1, 1 to 1)
    }
}
