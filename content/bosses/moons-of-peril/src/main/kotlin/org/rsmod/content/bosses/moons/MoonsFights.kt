package org.rsmod.content.bosses.moons

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.EnumMap
import java.util.IdentityHashMap
import org.rsmod.api.bossbar.plugin.BossHpBarScript
import org.rsmod.api.bosses.runtime.Angles
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.PersonalHpTracker
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.combat.commons.player.finishNpcHit
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.output.spam
import org.rsmod.api.player.output.projAnimSpecific
import org.rsmod.api.player.output.spotanimMapPrivate
import org.rsmod.game.hit.Hitmark
import org.rsmod.game.proj.ProjAnim
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.EntityExactMove
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid

interface BlueHooks {
    fun onColdGain(fight: MoonsFight, before: Int, after: Int)

    fun left(player: Player)
}

class MoonsFight(val player: Player, val moon: Moon) {
    var burn: Int = BURN_START
    var subdued: Boolean = false
    var chainUntil: Int = -1
    var parryUntil: Int = -1
    var coldLocked: Boolean = false
    var relightSide: Int = -1
    var freezeTarget: Int = -1

    companion object {
        const val BURN_START = 1
        const val BURN_MAX = 16
    }
}

/**
 * Who is fighting which Moon. The Moon is one shared npc per arena whose hp is locked and whose
 * ambient spec cycles whether or not anyone is inside; each player's progress is their pool in
 * the encounter's [PersonalHpTracker] plus a [MoonsFight].
 */
@Singleton
class MoonsFights
@Inject
constructor(
    private val deps: BossDeps,
    private val eventBus: EventBus,
    private val bossBar: BossHpBarScript,
) {
    private val npcs = EnumMap<Moon, Npc>(Moon::class.java)
    private val focus = EnumMap<Moon, Int>(Moon::class.java)
    private val debugging = java.util.Collections.newSetFromMap(IdentityHashMap<Player, Boolean>())
    private val focusSince = EnumMap<Moon, Int>(Moon::class.java)
    private val fights = IdentityHashMap<Player, MoonsFight>()
    private val jaguars = EnumMap<Moon, MutableList<Npc>>(Moon::class.java)

    fun register(moon: Moon, npc: Npc) {
        npcs[moon] = npc
        focus[moon] = Moon.NEW_MOON
    }

    fun moonOf(npc: Npc): Moon? = npcs.entries.firstOrNull { it.value === npc }?.key

    fun npc(moon: Moon): Npc? = npcs[moon]

    fun of(player: Player): MoonsFight? = fights[player]

    fun focus(moon: Moon): Int = focus[moon] ?: Moon.NEW_MOON

    fun playersIn(moon: Moon): List<Player> = fights.values.filter { it.moon == moon }.map { it.player }

    fun tracker(moon: Moon): PersonalHpTracker? = npcs[moon]?.let { deps.encounter(it).personalHp }

    var blueHooks: BlueHooks? = null

    fun begin(player: Player, moon: Moon) {
        if (fights[player]?.moon == moon) return
        end(player)
        blueHooks?.left(player)
        val npc = npcs[moon] ?: return
        val tracker = tracker(moon) ?: return
        fights[player] = MoonsFight(player, moon)
        debugging.add(player)
        tracker.add(player)

        val current = focus(moon)
        player.set("varbit.pmoon_boss_in_combat", 1)
        player.set("varbit.pmoon_boss_current_focus", current)
        player.set("varbit.pmoon_boss_last_focus", current)
        player.set("varbit.pmoon_boss_condition", 0)
        player.set("varbit.pmoon_boss_hide_npc", 0)
        player.set("varbit.pmoon_boss_blue_wearprevent", 0)
        player.set("varbit.pmoon_boss_ad_hp", 0)
        player.set("varbit.pmoon_boss_icicle_state", 0)
        bossBar.onOpen(player, npc)
        bossBar.onUpdate(player, npc, Moon.MAX_HITPOINTS, Moon.MAX_HITPOINTS)
        player.set("varp.pmoon_inside", 1)
        player.ifOpenOverlay(HUD, HUD_TARGET, eventBus)
    }

    /** [quiet] skips everything sent to the client, for a player who is logging out. */
    fun end(player: Player, quiet: Boolean = false) {
        val fight = fights.remove(player) ?: return
        if (fight.moon == Moon.Blue) blueHooks?.left(player)
        debugging.remove(player)
        player.bound = false
        fight.parryUntil = -1
        val moon = fight.moon
        tracker(moon)?.remove(player)
        if (!quiet) {
            player.ifCloseOverlay(HUD, eventBus)
            player.set("varbit.pmoon_boss_in_combat", 0)
            player.set("varbit.pmoon_boss_condition", 0)
            player.set("varbit.pmoon_boss_hide_npc", 0)
            npcs[moon]?.let { bossBar.onClose(player, it) }
            player.set("varp.pmoon_inside", 0)
        }
    }

    fun setPlayerFocus(player: Player, value: Int) {
        player.set("varbit.pmoon_boss_last_focus", player.vars[CURRENT_FOCUS])
        player.set(CURRENT_FOCUS, value)
    }

    fun setFocus(moon: Moon, next: Int) {
        val previous = focus(moon)
        focus[moon] = next
        focusSince[moon] = deps.mapClock.cycle
        npcs[moon]?.faceSquare(moon.glyph(next))
        for (player in playersIn(moon)) {
            player.set("varbit.pmoon_boss_last_focus", previous)
            player.set("varbit.pmoon_boss_current_focus", next)
        }
    }

    fun addCondition(fight: MoonsFight, amount: Int) {
        val player = fight.player
        val before = player.vars[CONDITION]
        val after = (before + amount).coerceIn(0, MAX_CONDITION)
        player.set(CONDITION, after)
        if (fight.moon == Moon.Blue && amount > 0) blueHooks?.onColdGain(fight, before, after)
    }

    /**
     * Starts the Moon's three-hit chain for [player]. Each hit is resolved on its own tick against
     * where the player stands then, so stepping onto the glyph mid-chain takes the remaining hits;
     * once a hit misses, the rest of the chain does.
     */
    fun standardAttack(npc: Npc, player: Player) {
        val fight = fights[player]
        if (fight == null || fight.subdued) return
        fight.chainUntil = deps.mapClock.cycle + CHAIN_MAX_HITS.size - 1
        val chain = HitChain()
        debug(player, "attack started: chain hits at +0,+1,+2")
        chainHit(fight, npc, 0, chain)
        for (index in 1 until CHAIN_MAX_HITS.size) {
            deps.worldQueues.add(index) { chainHit(fight, npc, index, chain) }
        }
    }

    fun toggleDebug(player: Player): Boolean = if (debugging.add(player)) true else !debugging.remove(player)

    private fun debug(player: Player, text: String) {
        if (player in debugging) player.spam("[moon t=${deps.mapClock.cycle}] $text")
    }

    private class HitChain(
        var missed: Boolean = false,
        var attempted: Boolean = false,
        var messaged: Boolean = false,
    )

    private fun chainHit(fight: MoonsFight, npc: Npc, index: Int, chain: HitChain) {
        val player = fight.player
        val moon = fight.moon
        if (fights[player] !== fight || fight.subdued) return
        val last = index == CHAIN_MAX_HITS.lastIndex
        val onGlyph = moon.onGlyph(focus(moon), player.coords)
        if (!onGlyph || chain.missed) {
            debug(player, "chain hit ${index + 1}: skipped (${if (onGlyph) "earlier miss" else "off glyph"})")
            player.finishNpcHit(npc, 1, HitType.Typeless, 0, deps.playerHitModifier)
        } else {
            chain.attempted = true
            val landed = deps.accuracy.rollMeleeAccuracy(npc, player, MeleeAttackType.Slash, deps.random)
            val damage = if (landed) deps.random.of(0, CHAIN_MAX_HITS[index]) else 0
            val hit = player.finishNpcHit(npc, 1, HitType.Typeless, damage, deps.playerHitModifier)
            debug(player, "chain hit ${index + 1}: ${if (landed) "landed" else "missed"} rolled=$damage dealt=${hit.damage}")
            if (landed) {
                addCondition(fight, moon.chainCurse(index, hit.damage))
                if (!chain.messaged) {
                    chain.messaged = true
                    moon.attackMessage?.let { player.mes(it) }
                }
                if (moon == Moon.Blood) {
                    val heal =
                        if (last) deps.random.of(BLOOD_FINAL_HEAL.first, BLOOD_FINAL_HEAL.last)
                        else hit.damage * (index + 1)
                    deps.worldQueues.add(1) {
                        if (fights[player] === fight) bloodHeal(fight, npc, heal, last)
                    }
                }
            } else {
                chain.missed = true
            }
        }
        val tracker = tracker(moon) ?: return
        val hitpoints = tracker.of(player)?.hitpoints ?: return
        when (moon.selfDamage) {
            Moon.SelfDamage.OnePerSwing ->
                if (onGlyph) tracker.set(player, (hitpoints - 1).coerceAtLeast(1))
            Moon.SelfDamage.PerChainHit ->
                if (onGlyph) {
                    val cost = Math.round(hitpoints / SELF_DAMAGE_DIVISOR.toDouble()).toInt()
                    tracker.set(player, (hitpoints - cost.coerceAtLeast(1)).coerceAtLeast(1))
                }
            Moon.SelfDamage.PerAttack ->
                if (last && chain.attempted) {
                    val cost = (hitpoints / SELF_DAMAGE_DIVISOR).coerceAtLeast(1)
                    tracker.set(player, (hitpoints - cost).coerceAtLeast(1))
                }
        }
    }

    private fun bloodHeal(fight: MoonsFight, npc: Npc, amount: Int, scream: Boolean) {
        if (fight.subdued || amount <= 0) return
        val player = fight.player
        tracker(fight.moon)?.heal(player, amount)
        player.soundSynth(BLOOD_HEAL_SYNTH)
        debug(player, "blood heal +$amount")
        player.projAnimSpecific(healProjectile(player, npc, BLOOD_HEAL_SPOTANIM, 60, 350, 60, 135, 0))
        if (scream) {
            player.projAnimSpecific(healProjectile(player, npc, BLOOD_SCREAM_SPOTANIM, 12, 12, 40, 0, 50))
        }
        showHeal(fight, npc, amount)
    }

    fun showHeal(fight: MoonsFight, npc: Npc, amount: Int) {
        val player = fight.player
        val hitmark = HEAL_HITMARK.asRSCM(RSCMType.HITMARK)
        deps.worldQueues.add(HEAL_HITSPLAT_DELAY) {
            if (fights[player] !== fight || !npc.isValidTarget()) return@add
            npc.showHitmark(
                Hitmark.fromPlayerSource(hitmark, hitmark, null, amount, 0, player.slotId)
            )
        }
    }

    private fun healProjectile(
        player: Player,
        npc: Npc,
        spotanim: String,
        startHeight: Int,
        endHeight: Int,
        endTime: Int,
        angle: Int,
        progress: Int,
    ) =
        ProjAnim(
            spotanim = spotanim.asRSCM(RSCMType.SPOTANIM),
            startHeight = startHeight,
            endHeight = endHeight,
            startTime = 0,
            endTime = endTime,
            angle = angle,
            progress = progress,
            sourceIndex = 0,
            targetIndex = if (player.vars["varbit.pmoon_boss_hide_npc"] == 1) 0 else npc.slotId + 1,
            startCoord = player.coords,
            endCoord = moonOf(npc)?.centre ?: npc.coords,
        )

    /** Burns [player] for standing off the active glyph: 1, doubling each tick up to 16. */
    fun burnOffGlyph(npc: Npc, player: Player) {
        val fight = fights[player] ?: return
        if (fight.subdued) return
        val moon = fight.moon
        if (moon.onGlyph(focus(moon), player.coords)) {
            fight.burn = MoonsFight.BURN_START
            return
        }
        if (deps.mapClock.cycle <= fight.chainUntil) {
            debug(player, "burn: suppressed (attack chain in progress)")
            return
        }
        if (deps.mapClock.cycle - focusChangedAt(moon) <= BURN_GRACE) {
            debug(player, "burn: grace after glyph moved")
            return
        }
        val hit = player.queueHit(npc, 1, HitType.Typeless, fight.burn, deps.playerHitModifier)
        debug(player, "burn: hit ${hit.damage} (stage ${fight.burn}), off glyph")
        fight.burn = (fight.burn * 2).coerceAtMost(MoonsFight.BURN_MAX)
        addCondition(fight, 1)
        if (moon == Moon.Blood) {
            val dealt = hit.damage
            deps.worldQueues.add(1) { if (fights[player] === fight) bloodHeal(fight, npc, dealt, false) }
        }
    }

    private fun focusChangedAt(moon: Moon): Int = focusSince[moon] ?: 0

    private fun bloodStrike(fight: MoonsFight, npc: Npc, maxHit: Int, healMultiplier: Int) {
        val player = fight.player
        val dealt =
            player
                .finishNpcHit(npc, 1, HitType.Typeless, deps.random.of(0, maxHit), deps.playerHitModifier)
                .damage
        debug(player, "blood strike: dealt $dealt of max $maxHit")
        if (dealt <= 0 || healMultiplier <= 0) return
        deps.worldQueues.add(1) {
            if (fights[player] === fight) bloodHeal(fight, npc, dealt * healMultiplier, false)
        }
    }

    fun poolHit(npc: Npc, player: Player, maxHit: Int) {
        val fight = fights[player] ?: return
        if (fight.subdued) return
        bloodStrike(fight, npc, maxHit, POOL_HEAL_MULTIPLIER)
    }

    fun sap(npc: Npc) {
        val moon = moonOf(npc) ?: return
        for (fight in fights.values.filter { it.moon == moon && !it.subdued }) {
            bloodStrike(fight, npc, SAP_MAX_HIT, 1)
        }
    }

    fun jaguarSpawned(moon: Moon, jaguar: Npc) {
        jaguars.getOrPut(moon) { mutableListOf() } += jaguar
        jaguar.ignoreCombatInteractions = true
        jaguar.anim(JAGUAR_SPAWN_SEQ)
        jaguar.faceSquare(moon.centre)
    }

    fun jaguarAttack(npc: Npc) {
        val moon = moonOf(npc) ?: return
        val live = jaguars[moon]?.filter { it.isValidTarget() }.orEmpty()
        jaguars[moon] = live.toMutableList()
        live.forEach { it.anim(JAGUAR_MELEE_SEQ) }
        for (fight in fights.values.filter { it.moon == moon && !it.subdued }) {
            if (struckByJaguar(moon, fight.player)) bloodStrike(fight, npc, JAGUAR_MAX_HIT, JAGUAR_HEAL_MULTIPLIER)
        }
    }

    /**
     * Jaguars are hitpoints-locked, so a hit only shows its splat; the damage comes off [player]'s
     * Moon pool instead, and hitting the safe jaguar also returns hitpoints sapped from them.
     */
    fun jaguarHit(jaguar: Npc, player: Player, damage: Int): Int {
        val fight = fights[player] ?: return damage
        val moon = fight.moon
        if (damage > 0 && !fight.subdued && tracker(moon)?.damage(player, damage) == true) subdue(player)
        val index =
            Moon.JAGUAR_OFFSETS.indexOfFirst { (dx, dz) ->
                moon.southWest.translate(dx, dz) == jaguar.coords
            }
        val sapped = player.vars[AD_HP]
        if (index != player.vars[CURRENT_FOCUS] || sapped <= 0) {
            player.mes(NO_BLOOD_MESSAGE)
            return damage
        }
        val back = minOf(damage, sapped)
        if (back <= 0) return damage
        player.set(AD_HP, sapped - back)
        player.statHeal("stat.hitpoints", back, 0)
        return damage
    }

    private fun struckByJaguar(moon: Moon, player: Player): Boolean {
        val safe = player.vars[CURRENT_FOCUS]
        return Moon.JAGUAR_OFFSETS.indices.any { index ->
            if (index == safe) return@any false
            val (dx, dz) = Moon.JAGUAR_OFFSETS[index]
            val (fx, fz) = Moon.JAGUAR_FACING[index]
            val origin = moon.southWest.translate(dx, dz)
            (1..JAGUAR_REACH).any { step -> origin.translate(fx * step, fz * step) == player.coords }
        }
    }

    fun rainThrow(npc: Npc) {
        val moon = moonOf(npc) ?: return
        for (fight in fights.values.filter { it.moon == moon && !it.subdued }) {
            val player = fight.player
            val from = player.coords
            val dest =
                (1..THROW_ATTEMPTS)
                    .map {
                        from.translate(
                            deps.random.of(-THROW_RANGE, THROW_RANGE),
                            deps.random.of(-THROW_RANGE, THROW_RANGE),
                        )
                    }
                    .firstOrNull { it != from && moon.inArena(it) && !deps.collision.isWalkBlocked(it) }
                    ?: continue
            throwTo(player, dest)
        }
    }

    fun throwTo(player: Player, dest: CoordGrid) {
        val from = player.coords
        player.anim(THROW_SEQ)
        player.spotanim(THROW_SPOTANIM, height = THROW_SPOTANIM_HEIGHT)
        player.soundSynth(THROW_SYNTH)
        PathingEntityCommon.teleport(player, deps.collision, dest)
        player.pendingExactMove =
            EntityExactMove(
                deltaX1 = from.x - dest.x,
                deltaZ1 = from.z - dest.z,
                deltaX2 = 0,
                deltaZ2 = 0,
                clientDelay1 = 0,
                clientDelay2 = THROW_CLIENT_CYCLES,
                direction = Angles.bearing(dest, from),
            )
    }

    fun throwToOffset(moon: Moon, offset: Pair<Int, Int>) {
        val dest = moon.southWest.translate(offset.first, offset.second)
        for (fight in fights.values.filter { it.moon == moon && !it.subdued }) {
            throwTo(fight.player, dest)
        }
    }

    fun arrival(moon: Moon): CoordGrid = moon.landing(focus(moon))

    fun damagePool(player: Player, damage: Int) {
        val fight = fights[player] ?: return
        if (damage > 0 && !fight.subdued && tracker(fight.moon)?.damage(player, damage) == true) subdue(player)
    }


    /** Runs once for the player whose pool reached 0; the shared npc itself never dies. */
    fun subdue(player: Player) {
        val fight = fights[player] ?: return
        if (fight.subdued) return
        fight.subdued = true
        val moon = fight.moon
        val camp = campAfter(player, moon)
        player.set(moon.deadVarbit, 1)
        player.set(moon.killsVarp, player.vars[moon.killsVarp] + 1)
        player.set("varbit.pmoon_boss_hide_npc", 1)
        player.spotanimMapPrivate(moon.deathSpotanim.asRSCM(RSCMType.SPOTANIM), moon.centre, height = 0)

        deps.worldQueues.add(LEAVE_SOUND_DELAY) {
            if (fights[player] === fight) player.soundSynth(PORTAL_SYNTH)
        }
        deps.worldQueues.add(LEAVE_DELAY) {
            if (fights[player] !== fight) return@add
            end(player)
            PathingEntityCommon.telejump(player, deps.collision, camp)
            sendFarewell(player, moon)
        }
    }

    private fun sendFarewell(player: Player, moon: Moon) {
        player.spam("Eyatlalli's voice echoes through the surrounding earth...")
        val allSubdued = Moon.entries.all { player.isSubdued(it) }
        val line =
            if (allSubdued) "Thank you, iknil. The Moons are sufficiently distracted. You should now return to me."
            else {
                "Thank you, iknil. The ${moon.label} of Peril is sufficiently distracted. " +
                    "You should now proceed on to the next one."
            }
        player.mes("Eyatlalli|$line", ChatType.NpcSay)
    }

    private fun campAfter(player: Player, moon: Moon): CoordGrid {
        val bloodDead = player.isSubdued(Moon.Blood)
        val blueDead = player.isSubdued(Moon.Blue)
        return when (moon) {
            Moon.Eclipse -> if (bloodDead) Moon.STREAMBOUND_CAMP else Moon.EARTHBOUND_CAMP
            Moon.Blue -> if (bloodDead) Moon.STREAMBOUND_CAMP else Moon.ANCIENT_PRISON_CAMP
            Moon.Blood -> if (blueDead) Moon.ANCIENT_PRISON_CAMP else Moon.EARTHBOUND_CAMP
        }
    }

    companion object {
        const val HUD = "interface.pmoon_hud"
        const val HUD_TARGET = "component.toplevel_osrs_stretch:overlay_hud"
        const val PORTAL_SYNTH = "synth.varlamore_pm_passageway_teleport_08"
        const val CONDITION = "varbit.pmoon_boss_condition"
        const val CURRENT_FOCUS = "varbit.pmoon_boss_current_focus"
        const val JAGUAR_NPC = "npc.pmoon_boss_jaguar"
        const val AD_HP = "varbit.pmoon_boss_ad_hp"
        const val NO_BLOOD_MESSAGE = "This jaguar doesn't possess any of your blood!"
        const val SAP_MAX_HIT = 15
        const val JAGUAR_MAX_HIT = 9
        const val JAGUAR_REACH = 2
        const val JAGUAR_HEAL_MULTIPLIER = 3
        const val POOL_HEAL_MULTIPLIER = 2
        const val JAGUAR_SPAWN_SEQ = "seq.npc_lynx_combat_spawn"
        const val JAGUAR_MELEE_SEQ = "seq.blood_jaguar_combat_melee"
        const val THROW_SEQ = "seq.human_troll_flyback_merge"
        const val THROW_SPOTANIM = "spotanim.stunned_thieving"
        const val THROW_SPOTANIM_HEIGHT = 124
        const val THROW_SYNTH = "synth.myarm_flyback"
        const val THROW_RANGE = 3
        const val THROW_ATTEMPTS = 12
        const val THROW_CLIENT_CYCLES = 60
        const val MAX_CONDITION = 127
        const val SELF_DAMAGE_DIVISOR = 125
        const val BLOOD_HEAL_SYNTH = "synth.varl_pm_blood_healing_projectiles_05"
        const val BLOOD_HEAL_SPOTANIM = "spotanim.fx_wardens_charging01"
        const val BLOOD_SCREAM_SPOTANIM = "spotanim.banshee_scream"
        const val HEAL_HITMARK = "hitmark.heal"
        const val HEAL_HITSPLAT_DELAY = 3
        const val BURN_GRACE = 3
        const val LEAVE_SOUND_DELAY = 5
        const val LEAVE_DELAY = 7
        val CHAIN_MAX_HITS = listOf(4, 8, 20)
        val CHAIN_CONDITION = listOf(2, 4, 10)
        val BLOOD_FINAL_HEAL = 30..50

        fun Player.isSubdued(moon: Moon): Boolean = vars[moon.deadVarbit] != 0

        fun Player.set(variable: String, value: Int) = VarPlayerIntMapSetter.set(this, variable, value)
    }
}
