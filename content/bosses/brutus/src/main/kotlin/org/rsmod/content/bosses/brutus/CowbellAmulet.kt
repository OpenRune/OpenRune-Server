package org.rsmod.content.bosses.brutus

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.obj.charges.ObjChargeManager.Companion.isFailure
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.clearMapFlag
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeld5
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.script.onOpWorn3
import org.rsmod.api.script.onOpWorn4
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.utils.bits.withBits

/**
 * https://oldschool.runescape.wiki/w/Cowbell_amulet and its transcript. Animations, sounds,
 * dialogue types and timings: live capture, rev 241, 20261012T200152-df49f48@30-263.
 *
 * The charges live on the amulet itself, so each amulet keeps its own and charged ones don't stack.
 */
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
        onOpHeld4(CHARGED) { check(inv[it.slot]) }
        onOpHeld4(EMPTY) { charge(it.slot) }
        onOpHeld5(CHARGED) { uncharge(it.slot) }
        onOpHeldU(CHARGED, AIR_RUNE) { charge(it.firstSlot) }
        onOpHeldU(EMPTY, AIR_RUNE) { charge(it.firstSlot) }

        onOpWorn2(CHARGED) { ring() }
        onOpWorn2(EMPTY) { ring() }
        onOpWorn3(CHARGED) { teleport(player.worn, Wearpos.Front.slot) }
        onOpWorn4(CHARGED) { check(player.worn[Wearpos.Front.slot]) }
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

    /**
     * Whether Brutus is waiting to respawn in the player's field, or null outside it. Ringing
     * while he is dead brings him back [RING_RESPAWN_DELAY] ticks later, even when his natural
     * respawn was due sooner (https://oldschool.runescape.wiki/w/Cowbell_amulet).
     */
    private fun ProtectedAccess.fieldBrutusDead(): Boolean? {
        val session = manager.sessionForPlayer(player) ?: return null
        if (session.key != BRUTUS_FIELD) {
            return null
        }
        val brutus =
            manager.npcsForInstance(session.id).firstOrNull { it.isType(Brutus.BRUTUS) }
                ?: return false
        if (!brutus.respawns || brutus.lifecycleRespawnCycle <= clock.cycle) {
            return false
        }
        brutus.lifecycleRespawnCycle = clock.cycle + RING_RESPAWN_DELAY
        return true
    }

    private fun ProtectedAccess.check(obj: InvObj?) {
        val count = charges.getCharges(obj, CHARGES)
        mes("The amulet has ${plural(count)}.")
    }

    private suspend fun ProtectedAccess.charge(slot: Int) {
        val runes = inv.count(AIR_RUNE)
        if (runes == 0) {
            mesbox("You don't have any Air Runes to charge the amulet with.")
            return
        }
        val current = charges.getCharges(inv[slot], CHARGES)
        val room = MAX_CHARGES - current
        if (room <= 0) {
            // UNVERIFIED: dialogue type (wiki transcript gives only the text).
            mesbox("The amulet is already fully charged.")
            return
        }
        val max = min(runes, room)
        val requested = min(countDialog("How many charges do you want to apply? (Up to $max)"), max)
        if (requested == 0) {
            return
        }
        val amulet = inv[slot] ?: return
        if (!amulet.isType(EMPTY) && !amulet.isType(CHARGED)) {
            return
        }
        if (invDel(inv, AIR_RUNE, requested).failure) {
            return
        }
        val added = charges.addCharges(inv, slot, requested, CHARGES, MAX_CHARGES)
        if (added.isFailure()) {
            invAdd(inv, AIR_RUNE, requested)
            return
        }
        val total = charges.getCharges(inv[slot], CHARGES)
        soundSynth(CHARGE_SYNTH)
        objbox(
            CHARGED,
            zoom = 400,
            "You add $requested air ${runeWord(requested)} to your amulet. It now has " +
                "${plural(total)}.",
        )
    }

    private suspend fun ProtectedAccess.uncharge(slot: Int) {
        val count = charges.getCharges(inv[slot], CHARGES)
        if (count == 0) {
            return
        }
        if (invAdd(inv, AIR_RUNE, count).failure) {
            // UNVERIFIED: full inventory message.
            mes("You don't have enough inventory space.")
            return
        }
        charges.removeAllCharges(inv, slot, CHARGES)
        // UNVERIFIED: dialogue type (wiki transcript gives only the text).
        mesbox("You uncharge your amulet by removing $count air ${runeWord(count)} from it.")
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
        container[slot] =
            if (left == 0) {
                InvObj(EMPTY)
            } else {
                amulet.copy(vars = amulet.vars.withBits(chargeBits, left))
            }
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
        when (left) {
            0 -> mes("@mes_hl_red@Your amulet has run out of charges.</col>")
            // UNVERIFIED: the capture only saw warnings at 5 and 1 charges left.
            5, 1 -> mes("@mes_hl_blu@Your amulet has ${plural(left)} left.</col>")
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

    // live capture, rev 241, 20261012T200152-df49f48@196-261: ten arrivals, all within one tile of
    // (3259, 3276). UNVERIFIED: the wiki gives (3259, 3277) with a radius of 2.
    private fun destination(): CoordGrid =
        DESTINATION.translate(
            random.of(-ARRIVAL_RADIUS, ARRIVAL_RADIUS),
            random.of(-ARRIVAL_RADIUS, ARRIVAL_RADIUS),
        )

    // UNVERIFIED: which cow answers and how far away it can be (the capture's were within 3 tiles).
    private fun ProtectedAccess.mooAtArrival() {
        val cow =
            npcRepo.findAll(ZoneKey.from(player.coords), zoneRadius = 1).filter {
                (it.isAnyOf(COWS) || it.isAnyOf(CALVES)) &&
                    it.coords.chebyshevDistance(player.coords) <= MOO_RANGE
            }.toList().randomOrNull() ?: return
        cow.say(if (cow.isAnyOf(CALVES)) "M-Moo?" else "Moo?")
    }

    private fun Npc.isAnyOf(types: List<String>): Boolean = types.any { isType(it) }

    private val chargeBits by lazy {
        checkNotNull(ServerCacheManager.getVarObj(CHARGES.asRSCM(RSCMType.VAROBJ))).bits
    }

    private fun plural(count: Int): String = if (count == 1) "1 charge" else "$count charges"

    private fun runeWord(count: Int): String = if (count == 1) "rune" else "runes"

    private companion object {
        const val CHARGED = "obj.cowbell_amulet"
        const val EMPTY = "obj.cowbell_amulet_empty"
        const val AIR_RUNE = "obj.airrune"
        const val CHARGES = "varobj.charges_16383"
        const val MAX_CHARGES = 1_000
        const val BRUTUS_FIELD = "cowboss"

        const val RING_SYNTH = "synth.cowbell_amulet_ring"
        const val RING_SOUNDS = 3
        const val RING_SOUND_GAP = 8
        const val RING_RESPAWN_DELAY = 13
        const val CHARGE_SYNTH = "synth.cowbell_amulet_charge"

        const val TELEPORT_SPOTANIM = "spotanim.vfx_human_cowboss_teleport01"
        const val TELEPORT_SYNTH = "synth.cowbell_amulet_teleport"
        const val TELEPORT_SYNTH_DELAY = 50
        const val ARRIVE_SYNTH = "synth.cowbell_amulet_arrive"
        const val ARRIVE_SYNTH_DELAY = 57
        const val TELEPORT_DELAY = 3
        const val ARRIVE_DELAY = 2
        const val ARRIVAL_RADIUS = 1
        const val MOO_RANGE = 3

        val DESTINATION = CoordGrid(3259, 3276)
        val COWS = listOf("npc.cow", "npc.cow2", "npc.cow3")
        val CALVES = listOf("npc.cow2_calf", "npc.cow3_calf")
    }
}
