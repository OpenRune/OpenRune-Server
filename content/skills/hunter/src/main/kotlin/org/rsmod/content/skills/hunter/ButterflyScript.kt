package org.rsmod.content.skills.hunter

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.config.constants
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.mapMultiway
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.statBoost
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpPlayerU
import org.rsmod.api.table.hunter.HunterButterfliesRow
import org.rsmod.content.skills.hunter.HunterTrapScript.Companion.catchChance256
import org.rsmod.content.skills.hunter.HunterTrapScript.Companion.distance
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ButterflyScript
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val playerRepo: PlayerRepository,
    private val spawns: ImplingSpawns,
    private val areas: AreaChecker,
    val rumours: HunterRumours,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val rows = HunterButterfliesRow.all()
        for (row in rows) onOpNpc1(row.npc) { catch(it.npc, row) }
        for (row in rows.distinctBy { it.jar.id }) {
            onOpHeld4(row.jar) { release(row) }
            onOpPlayerU(row.jar) { share(it.target, row) }
            for (meat in row.mixMeats.distinctBy { it.id }) {
                onOpHeldU(row.jar.internalName, meat.internalName) { mix(row, meat.internalName) }
            }
            onOpHeld1(row.mixes[0]) { drink(row, row.mixes[0].internalName, row.mixes[1].internalName) }
            onOpHeld1(row.mixes[1]) { drink(row, row.mixes[1].internalName, null) }
        }
    }

    fun ProtectedAccess.catch(npc: Npc, row: HunterButterfliesRow) {
        if (!npc.isCatchable()) return
        faceEntitySquare(npc)
        anim(CATCH_SEQ)
        if (!rollCatch(row)) return
        caught += npc
        spawns.after(REMOVE_TICKS) {
            caught -= npc
            if (npc.respawns) npcRepo.despawn(npc, RESPAWN_TICKS) else npcRepo.del(npc, Int.MAX_VALUE)
        }
        statAdvance(HUNTER, row.xp.toDouble())
        rumours.onCatch(player, row.creature.internalName)
        if (BUTTERFLY_JAR in inv && invReplace(inv, BUTTERFLY_JAR, 1, row.jar.internalName).success) {
            mes("You manage to catch the ${row.name}.", ChatType.Spam)
            return
        }
        player.applyEffect(row)
        if (row.creature.internalName == SUNLIGHT_MOTH) restoreRunEnergy(SUNLIGHT_RUN_PERCENT)
        if (row.kind == MOTH) {
            val spot = if (row.creature.internalName == SUNLIGHT_MOTH) SUNLIGHT_OUT else MOONLIGHT_OUT
            spotanim(spot, RELEASE_DELAY, RELEASE_HEIGHT)
            soundSynth(RELEASE_SYNTH, delay = RELEASE_DELAY)
        }
        mes("You catch and release the ${row.name}.", ChatType.Spam)
    }

    private fun ProtectedAccess.rollCatch(row: HunterButterfliesRow): Boolean {
        val net = wieldedNet()
        val required = if (net == null) row.level + BAREHANDED_EXTRA_LEVELS else row.level
        if (stat(HUNTER) < required) return false
        val (low, high) =
            when (net) {
                NET -> row.netLow to row.netHigh
                MAGIC_NET -> row.magicLow to row.magicHigh
                else -> row.bareLow to row.bareHigh
            }
        return random.of(256) < HunterOutfit.applyTo(catchChance256(low, high, stat(HUNTER)), player)
    }

    suspend fun ProtectedAccess.release(row: HunterButterfliesRow) {
        if (invReplace(inv, row.jar.internalName, 1, BUTTERFLY_JAR).failure) return
        anim(RELEASE_SEQ)
        player.applyEffect(row)
        mes("You release the ${row.name}.")
    }

    private fun ProtectedAccess.share(target: Player, row: HunterButterfliesRow) {
        if (!player.mapMultiway(areas) || !target.acceptsAid()) {
            mes("Nothing interesting happens.")
            return
        }
        if (invReplace(inv, row.jar.internalName, 1, BUTTERFLY_JAR).failure) return
        anim(RELEASE_SEQ)
        val others =
            playerRepo
                .findAll(ZoneKey.from(target.coords), zoneRadius = 1)
                .filter { it !== target && it !== player && it.acceptsAid() && distance(it.coords, target.coords) <= JAR_SHARE_RANGE }
                .take(MAX_SHARED)
                .toList()
        for (receiver in listOf(target) + others) receiver.applyEffect(row)
        mes("You release the ${row.name}.")
    }

    private suspend fun ProtectedAccess.mix(row: HunterButterfliesRow, meat: String) {
        if (PESTLE !in inv) {
            mes("You need a pestle and mortar to make a hunter's mix.")
            return
        }
        anim(MIX_SEQ)
        delay(MIX_TICKS)
        if (row.jar.internalName !in inv || meat !in inv) return
        val mixed =
            player.invTransaction(inv) {
                val target = select(inv)
                delete(target, row.jar.id, 1)
                delete(target, meat.asRSCM(RSCMType.OBJ), 1)
                add(target, row.mixes[0].id, 1)
            }
        if (mixed.failure) return
        mes("You mix the meat into the butterfly jar... Don't think too hard about it.")
    }

    private fun ProtectedAccess.drink(row: HunterButterfliesRow, dose: String, next: String?) {
        if (next != null) {
            if (invReplace(inv, dose, 1, next).failure) return
        } else if (invReplace(inv, dose, 1, EMPTY_VIAL).failure) {
            return
        }
        anim(DRINK_SEQ)
        player.applyEffect(row, row.mixEffectConstant)
        if (player.mapMultiway(areas)) {
            playerRepo
                .findAll(ZoneKey.from(player.coords), zoneRadius = 1)
                .filter { it !== player && it.acceptsAid() && distance(it.coords, player.coords) <= MIX_SHARE_RANGE }
                .take(MAX_SHARED)
                .forEach { it.applyEffect(row, row.mixEffectConstant) }
        }
        mes("You drink the ${row.name} mix.", ChatType.Spam)
    }

    private fun Player.acceptsAid(): Boolean = vars[ACCEPT_AID] == 1

    private fun Player.applyEffect(row: HunterButterfliesRow, constant: Int = row.effectConstant) {
        val stat = row.effectStat.internalName
        when (row.effect) {
            EFFECT_BOOST -> statBoost(stat, constant, row.effectPercent)
            EFFECT_HEAL -> statHeal(stat, constant, row.effectPercent)
            EFFECT_RESTORE -> for (restored in RESTORED_STATS) statHeal(restored, constant, row.effectPercent)
        }
        if (row.hitpointsHeal > 0) statHeal(HITPOINTS, row.hitpointsHeal, 0)
    }

    private fun ProtectedAccess.restoreRunEnergy(percent: Int) {
        val restored = min(constants.run_max_energy, player.runEnergy + percent * constants.run_max_energy / 100)
        if (restored == player.runEnergy) return
        player.runEnergy = restored
        UpdateRun.energy(player, restored)
    }

    private val caught = HashSet<Npc>()

    private fun Npc.isCatchable(): Boolean = isSlotAssigned && isVisible && this !in caught

    private fun ProtectedAccess.wieldedNet(): String? {
        val held = player.righthand ?: return null
        return listOf(NET, MAGIC_NET).firstOrNull { it.asRSCM(RSCMType.OBJ) == held.id }
    }

    companion object {
        const val HUNTER = "stat.hunter"
        const val HITPOINTS = "stat.hitpoints"
        const val NET = "obj.hunting_butterfly_net"
        const val MAGIC_NET = "obj.ii_magic_butterfly_net"
        const val BUTTERFLY_JAR = "obj.butterfly_jar"
        const val PESTLE = "obj.pestle_and_mortar"
        const val EMPTY_VIAL = "obj.vial_empty"
        const val ACCEPT_AID = "varbit.option_acceptaid"
        const val CATCH_SEQ = "seq.hunting_impling_catch"
        const val RELEASE_SEQ = "seq.hunting_butterfly_out_jar"
        const val MIX_SEQ = "seq.human_herbing_grind"
        const val DRINK_SEQ = "seq.human_eat"
        const val SUNLIGHT_OUT = "spotanim.hunting_moth_sunlight_out_spot_anim"
        const val MOONLIGHT_OUT = "spotanim.hunting_moth_moonlight_out_spot_anim"
        const val RELEASE_SYNTH = "synth.hunting_moth_release"
        const val MOTH = "moth"
        const val RELEASE_DELAY = 45
        const val RELEASE_HEIGHT = 190
        const val BAREHANDED_EXTRA_LEVELS = 10
        const val RESPAWN_TICKS = 7
        const val REMOVE_TICKS = 1
        const val MIX_TICKS = 3
        const val MAX_SHARED = 3
        const val JAR_SHARE_RANGE = 2
        const val MIX_SHARE_RANGE = 2
        const val SUNLIGHT_MOTH = "npc.moth_sunlight"
        const val SUNLIGHT_RUN_PERCENT = 5

        const val EFFECT_BOOST = 0
        const val EFFECT_HEAL = 1
        const val EFFECT_RESTORE = 2

        private val RESTORED_STATS =
            listOf(
                "attack", "defence", "strength", "ranged", "magic", "cooking", "woodcutting", "fletching",
                "fishing", "firemaking", "crafting", "smithing", "mining", "herblore", "agility", "thieving",
                "slayer", "farming", "runecrafting", "hunter", "construction",
            ).map { "stat.$it" }
    }
}
