package org.rsmod.content.bosses.brutus

import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.clearMapFlag
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.script.onOpWorn3
import org.rsmod.content.bosses.brutus.CowbellAmuletCharging.Companion.CHARGED
import org.rsmod.content.bosses.brutus.CowbellAmuletCharging.Companion.CHARGES
import org.rsmod.content.bosses.brutus.CowbellAmuletCharging.Companion.EMPTY
import org.rsmod.content.bosses.brutus.CowbellAmuletCharging.Companion.plural
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CowbellAmulet
@Inject
constructor(
    private val charges: ObjChargeManager,
    private val manager: InstanceManager,
    private val npcRepo: NpcRepository,
    private val random: GameRandom,
    private val clock: MapClock,
    private val teleportValidator: PlayerTeleportValidator,
    private val areaChecker: AreaChecker,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(CHARGED) { teleport(inv, it.slot) }
        onOpHeld3(CHARGED) { ring() }
        onOpHeld3(EMPTY) { ring() }

        onOpWorn2(CHARGED) { ring() }
        onOpWorn2(EMPTY) { ring() }
        onEvent<NpcStateEvents.Respawn> { if (npc.isType(Brutus.BRUTUS)) npc.vars[BELL_RUNG] = 0 }
        onOpWorn3(CHARGED) { teleport(player.worn, Wearpos.Front.slot) }
    }

    private suspend fun ProtectedAccess.ring() {
        val text =
            when (fieldBrutusDead()) {
                null -> "You ring the bell."
                false -> "You ring the bell... but nothing happens."
                true -> "You ring the bell... and hear something approaching."
            }
        repeat(RING_SOUNDS) { soundSynth(RING_SYNTH, delay = it * RING_SOUND_GAP) }
        objbox(CHARGED, zoom = 400, text)
    }

    private fun ProtectedAccess.fieldBrutusDead(): Boolean? {
        val session = manager.sessionForPlayer(player) ?: return null
        if (session.key != BRUTUS_FIELD) {
            return null
        }
        val brutus =
            manager.npcsForInstance(session.id).firstOrNull { it.isType(Brutus.BRUTUS) }
                ?: return false
        val dead = brutus.respawns && brutus.lifecycleRespawnCycle > clock.cycle
        if (!dead || brutus.vars[BELL_RUNG] != 0) {
            return false
        }
        brutus.vars[BELL_RUNG] = 1
        brutus.lifecycleRespawnCycle = clock.cycle + RING_RESPAWN_DELAY
        return true
    }

    private suspend fun ProtectedAccess.teleport(container: Inventory, slot: Int) {
        val amulet = container[slot] ?: return
        if (!amulet.isType(CHARGED) || !canTeleport()) {
            return
        }
        val left = charges.getCharges(amulet, CHARGES) - 1
        if (left < 0) {
            return
        }
        container[slot] = CowbellAmuletCharging.afterTeleport(amulet, left)
        anim("seq.human_cowboss_teleport")
        spotanim(TELEPORT_SPOTANIM, slot = 1)
        soundSynth(TELEPORT_SYNTH, delay = TELEPORT_SYNTH_DELAY)
        delay(TELEPORT_DELAY)
        telejump(destination())
        spotanim(TELEPORT_SPOTANIM, slot = 1)
        soundSynth(ARRIVE_SYNTH, delay = ARRIVE_SYNTH_DELAY)
        delay(ARRIVE_DELAY)
        resetAnim()
        mooAtArrival()
        // UNVERIFIED: warnings at other counts below 50.
        when {
            left == 0 -> mes("@mes_hl_red@Your amulet has run out of charges.</col>")
            left % WARN_EVERY == 0 || left == 5 || left == 1 ->
                mes("@mes_hl_blu@Your amulet has ${plural(left)} left.</col>")
        }
    }

    private fun ProtectedAccess.canTeleport(): Boolean {
        val denial = teleportValidator.validate(player, TeleportType.Standard, areaChecker)
        if (denial == null) {
            return true
        }
        player.clearMapFlag()
        player.mes(denial, ChatType.Engine)
        return false
    }

    private fun destination(): CoordGrid =
        DESTINATION.translate(
            random.of(-ARRIVAL_RADIUS, ARRIVAL_RADIUS),
            random.of(-ARRIVAL_RADIUS, ARRIVAL_RADIUS),
        )

    private fun ProtectedAccess.mooAtArrival() {
        val cow =
            npcRepo.findAll(ZoneKey.from(player.coords), zoneRadius = 1).filter {
                (it.isAnyOf(COWS) || it.isAnyOf(CALVES)) &&
                    it.coords.chebyshevDistance(player.coords) <= MOO_RANGE
            }.toList().randomOrNull() ?: return
        cow.say(if (cow.isAnyOf(CALVES)) "M-Moo?" else "Moo?")
    }

    private fun Npc.isAnyOf(types: List<String>): Boolean = types.any { isType(it) }

    companion object {
        const val BRUTUS_FIELD = "cowboss"

        const val RING_SYNTH = "synth.cowbell_amulet_ring"
        const val RING_SOUNDS = 3
        const val RING_SOUND_GAP = 8

        const val RING_RESPAWN_DELAY = 12
        const val BELL_RUNG = "varn.brutus_bell_rung"

        const val TELEPORT_SPOTANIM = "spotanim.vfx_human_cowboss_teleport01"
        const val TELEPORT_SYNTH = "synth.cowbell_amulet_teleport"
        const val TELEPORT_SYNTH_DELAY = 50
        const val ARRIVE_SYNTH = "synth.cowbell_amulet_arrive"
        const val ARRIVE_SYNTH_DELAY = 57
        const val TELEPORT_DELAY = 3
        const val ARRIVE_DELAY = 2
        const val ARRIVAL_RADIUS = 1
        const val WARN_EVERY = 50
        const val MOO_RANGE = 3

        val DESTINATION = CoordGrid(3259, 3276)
        val COWS = listOf("npc.cow", "npc.cow2", "npc.cow3")
        val CALVES = listOf("npc.cow2_calf", "npc.cow3_calf")
    }
}
