package org.rsmod.content.bosses.giantmole

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.ui.ifCloseSub
import org.rsmod.api.player.ui.ifOpenFullOverlay
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.table.LightSourcesRow
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType

internal const val GIANT_MOLE_INSECT_WARNING_TIMER = "timer.giant_mole_insect_warning"
internal const val GIANT_MOLE_INSECT_SWARM_TIMER = "timer.giant_mole_insect_swarm"
internal const val GIANT_MOLE_INSECT_DAMAGE_TIMER = "timer.giant_mole_insect_damage"

private const val WARNING_TICKS = 16
private const val SWARM_TICKS = 14
private const val DAMAGE_TICKS = 1
private const val DARKNESS_LEVEL = 2
private const val VERY_DARKNESS_LEVEL = 3
private const val DARKNESS_OVERLAY = "interface.darkness_medium"
private const val VERY_DARKNESS_OVERLAY = "interface.darkness_dark"
private const val FIRE_OF_ETERNAL_LIGHT_VARBIT = "varbit.my2arm_fire_mole"
private const val TINDERBOX = "obj.tinderbox"

private var Player.darkness by intVarBit("varbit.darkness_level")

@Singleton
class MoleHoleEnvironment @Inject constructor(private val eventBus: EventBus) {
    private val inside = mutableSetOf<Int>()
    private val dark = mutableSetOf<Int>()

    private val lightSources: Set<String> by lazy {
        val cacheSources = LightSourcesRow.all().map { it.lit.internalName }
        val extras =
            EXTRA_LIGHT_SOURCES.filter {
                ServerCacheManager.getItem(it.asRSCM(RSCMType.OBJ)) != null
            }
        (cacheSources + extras).toSet()
    }

    fun onTick(player: Player) {
        val uid = player.uid.packed
        val inLair = GiantMoleRules.inMoleLair(player.coords)

        if (inLair && inside.add(uid)) {
            enterLair(player, forceOverlay = true)
        }
        if (!inLair) {
            if (inside.remove(uid)) {
                leaveLair(player)
            }
            if (dark.remove(uid)) {
                clearInsectTimers(player)
            }
            return
        }

        refresh(player)
    }

    fun enter(player: Player) {
        val uid = player.uid.packed
        if (inside.add(uid)) {
            enterLair(player, forceOverlay = true)
        }
        refresh(player)
    }

    fun onLogin(player: Player) {
        val uid = player.uid.packed
        val wasInside = inside.remove(uid)
        val wasDark = dark.remove(uid)
        clearInsectTimers(player)
        if (!GiantMoleRules.inMoleLair(player.coords)) {
            if (wasInside || wasDark) {
                resetDarkness(player)
            }
            return
        }
        inside.add(uid)
        enterLair(player, forceOverlay = true)
        refresh(player)
    }

    fun leave(player: Player) {
        inside.remove(player.uid.packed)
        leaveLair(player)
    }

    fun onLogout(player: Player) {
        val uid = player.uid.packed
        val wasInside = inside.remove(uid)
        val wasDark = dark.remove(uid)
        clearInsectTimers(player)
        if (wasInside || wasDark) {
            resetDarkness(player)
        }
    }

    fun refresh(player: Player) {
        if (!GiantMoleRules.inMoleLair(player.coords)) {
            return
        }
        val uid = player.uid.packed
        if (hasLightSource(player)) {
            if (dark.remove(uid)) {
                clearInsectTimers(player)
            }
            setDarkness(player, DARKNESS_LEVEL)
        } else {
            if (dark.add(uid)) {
                player.softTimer(GIANT_MOLE_INSECT_WARNING_TIMER, WARNING_TICKS)
            }
            setDarkness(player, VERY_DARKNESS_LEVEL)
        }
    }

    fun isDark(player: Player): Boolean =
        GiantMoleRules.inMoleLair(player.coords) && player.darkness > 0

    fun hasLightSource(player: Player): Boolean {
        if (GiantMoleRules.inMoleLair(player.coords) && hasPermanentLight(player)) {
            return true
        }
        return lightSources.any { player.inv.count(it) > 0 || player.worn.count(it) > 0 }
    }

    fun hasPermanentLight(player: Player): Boolean =
        player.vars[FIRE_OF_ETERNAL_LIGHT_VARBIT] != 0

    fun hasTinderbox(player: Player): Boolean = player.inv.count(TINDERBOX) > 0

    fun onWarningTimer(player: Player) {
        if (!GiantMoleRules.inMoleLair(player.coords) || hasLightSource(player)) {
            clearInsectTimers(player)
            return
        }
        player.mes("You hear tiny insects skittering over the ground...")
        player.soundSynth("synth.swamp_cave_insects")
        player.softTimer(GIANT_MOLE_INSECT_SWARM_TIMER, SWARM_TICKS)
    }

    fun onSwarmTimer(player: Player) {
        if (!GiantMoleRules.inMoleLair(player.coords) || hasLightSource(player)) {
            clearInsectTimers(player)
            return
        }
        player.mes("Tiny biting insects swarm all over you!")
        player.soundSynth("synth.swamp_cave_insects")
        player.softTimer(GIANT_MOLE_INSECT_DAMAGE_TIMER, DAMAGE_TICKS)
    }

    fun onDamageTimer(player: Player) {
        if (!GiantMoleRules.inMoleLair(player.coords) || hasLightSource(player)) {
            clearInsectTimers(player)
            return
        }
        player.queueHit(
            delay = 1,
            type = HitType.Typeless,
            damage = 1,
            modifier = NoopPlayerHitModifier,
        )
        player.soundSynth("synth.insect_bites")
        player.softTimer(GIANT_MOLE_INSECT_DAMAGE_TIMER, DAMAGE_TICKS)
    }

    private fun enterLair(player: Player, forceOverlay: Boolean = false) {
        if (hasLightSource(player)) {
            setDarkness(player, DARKNESS_LEVEL, forceOverlay)
        } else {
            dark.add(player.uid.packed)
            player.softTimer(GIANT_MOLE_INSECT_WARNING_TIMER, WARNING_TICKS)
            setDarkness(player, VERY_DARKNESS_LEVEL, forceOverlay)
        }
    }

    private fun leaveLair(player: Player) {
        resetDarkness(player)
        dark.remove(player.uid.packed)
        clearInsectTimers(player)
    }

    private fun resetDarkness(player: Player) {
        setDarkness(player, 0)
    }

    private fun setDarkness(player: Player, level: Int, forceOverlay: Boolean = false) {
        if (player.darkness == level && !forceOverlay) {
            return
        }
        player.darkness = level
        when (level) {
            DARKNESS_LEVEL -> {
                player.ifCloseSub(VERY_DARKNESS_OVERLAY, eventBus)
                player.ifOpenFullOverlay(DARKNESS_OVERLAY, eventBus)
            }
            VERY_DARKNESS_LEVEL -> {
                player.ifCloseSub(DARKNESS_OVERLAY, eventBus)
                player.ifOpenFullOverlay(VERY_DARKNESS_OVERLAY, eventBus)
            }
            else -> {
                player.ifCloseSub(DARKNESS_OVERLAY, eventBus)
                player.ifCloseSub(VERY_DARKNESS_OVERLAY, eventBus)
            }
        }
    }

    private fun clearInsectTimers(player: Player) {
        player.clearSoftTimer(GIANT_MOLE_INSECT_WARNING_TIMER)
        player.clearSoftTimer(GIANT_MOLE_INSECT_SWARM_TIMER)
        player.clearSoftTimer(GIANT_MOLE_INSECT_DAMAGE_TIMER)
    }

    private companion object {
        private val EXTRA_LIGHT_SOURCES =
            listOf(
                "obj.skillcape_firemaking",
                "obj.skillcape_firemaking_trimmed",
                "obj.skillcape_max",
                "obj.jar_of_light",
                "obj.abyssal_lantern",
            )
    }
}
