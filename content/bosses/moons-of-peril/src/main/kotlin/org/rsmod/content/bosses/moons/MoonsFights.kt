package org.rsmod.content.bosses.moons

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.EnumMap
import java.util.IdentityHashMap
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.PersonalHpTracker
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.combat.commons.player.finishNpcHit
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.hit.queueHit
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
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid

class MoonsFight(val player: Player, val moon: Moon) {
    var condition: Int = 0
    var burn: Int = BURN_START
    var subdued: Boolean = false

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
class MoonsFights @Inject constructor(private val deps: BossDeps, private val eventBus: EventBus) {
    private val npcs = EnumMap<Moon, Npc>(Moon::class.java)
    private val focus = EnumMap<Moon, Int>(Moon::class.java)
    private val focusSince = EnumMap<Moon, Int>(Moon::class.java)
    private val fights = IdentityHashMap<Player, MoonsFight>()

    fun register(moon: Moon, npc: Npc) {
        npcs[moon] = npc
        focus[moon] = Moon.NEW_MOON
    }

    fun moonOf(npc: Npc): Moon? = npcs.entries.firstOrNull { it.value === npc }?.key

    fun npc(moon: Moon): Npc? = npcs[moon]

    fun of(player: Player): MoonsFight? = fights[player]

    fun focus(moon: Moon): Int = focus[moon] ?: Moon.NEW_MOON

    fun playersIn(moon: Moon): List<Player> = fights.values.filter { it.moon == moon }.map { it.player }

    private fun tracker(moon: Moon): PersonalHpTracker? = npcs[moon]?.let { deps.encounter(it).personalHp }

    fun begin(player: Player, moon: Moon) {
        if (fights[player]?.moon == moon) return
        end(player)
        val npc = npcs[moon] ?: return
        val tracker = tracker(moon) ?: return
        fights[player] = MoonsFight(player, moon)
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
        player.set("varp.hpbar_hud_npc", npc.id)
        player.set("varp.pmoon_inside", 1)
        player.ifOpenOverlay(HUD, HUD_TARGET, eventBus)
    }

    /** [quiet] skips everything sent to the client, for a player who is logging out. */
    fun end(player: Player, quiet: Boolean = false) {
        val fight = fights.remove(player) ?: return
        val moon = fight.moon
        tracker(moon)?.remove(player)
        if (!quiet) {
            player.ifCloseOverlay(HUD, eventBus)
            player.set("varbit.pmoon_boss_in_combat", 0)
            player.set("varbit.pmoon_boss_condition", 0)
            player.set("varbit.pmoon_boss_hide_npc", 0)
            player.set("varp.hpbar_hud_npc", -1)
            player.set("varp.pmoon_inside", 0)
        }
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
        fight.condition = (fight.condition + amount).coerceIn(0, MAX_CONDITION)
        fight.player.set("varbit.pmoon_boss_condition", fight.condition)
    }

    /** The Moon's three-hit chain against [player]: once a hit misses, the rest of the chain does. */
    fun standardAttack(npc: Npc, player: Player) {
        val fight = fights[player]
        if (fight == null || fight.subdued) return
        val moon = fight.moon
        val tracker = tracker(moon) ?: return
        var landed = true
        for ((index, maxHit) in CHAIN_MAX_HITS.withIndex()) {
            landed = landed && deps.accuracy.rollMeleeAccuracy(npc, player, MeleeAttackType.Slash, deps.random)
            val damage = if (landed) deps.random.of(0, maxHit) else 0
            val hit = player.finishNpcHit(npc, index + 1, HitType.Typeless, damage, deps.playerHitModifier)
            if (!landed) continue
            addCondition(fight, CHAIN_CONDITION[index])
            if (moon != Moon.Blood) continue
            val dealt = hit.damage
            val heal = if (index == CHAIN_MAX_HITS.lastIndex) deps.random.of(BLOOD_FINAL_HEAL.first, BLOOD_FINAL_HEAL.last) else dealt * (index + 1)
            deps.worldQueues.add(index + 1) { if (fights[player] === fight) bloodHeal(fight, npc, heal, index == CHAIN_MAX_HITS.lastIndex) }
        }
        val hitpoints = tracker.of(player)?.hitpoints ?: return
        tracker.set(player, (hitpoints - (hitpoints / SELF_DAMAGE_DIVISOR).coerceAtLeast(1)).coerceAtLeast(1))
    }

    private fun bloodHeal(fight: MoonsFight, npc: Npc, amount: Int, scream: Boolean) {
        if (fight.subdued || amount <= 0) return
        val player = fight.player
        tracker(fight.moon)?.heal(player, amount)
        player.soundSynth(BLOOD_HEAL_SYNTH)
        player.projAnimSpecific(healProjectile(player, npc, BLOOD_HEAL_SPOTANIM, 15, 87, 60, 135, 0))
        if (scream) {
            player.projAnimSpecific(healProjectile(player, npc, BLOOD_SCREAM_SPOTANIM, 3, 3, 40, 0, 50))
        }
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
            targetIndex = npc.slotId + 1,
            startCoord = player.coords,
            endCoord = npc.coords,
        )

    fun decayCondition(player: Player) {
        val fight = fights[player] ?: return
        if (fight.condition > 0) addCondition(fight, -CONDITION_DECAY)
    }

    /** Burns [player] for standing off the active glyph: 1, doubling each tick up to 16. */
    fun burnOffGlyph(npc: Npc, player: Player) {
        val fight = fights[player] ?: return
        if (fight.subdued) return
        val moon = fight.moon
        if (moon.onGlyph(focus(moon), player.coords)) {
            fight.burn = MoonsFight.BURN_START
            return
        }
        if (deps.mapClock.cycle - focusChangedAt(moon) <= BURN_GRACE) return
        val hit = player.queueHit(npc, 1, HitType.Typeless, fight.burn, deps.playerHitModifier)
        fight.burn = (fight.burn * 2).coerceAtMost(MoonsFight.BURN_MAX)
        addCondition(fight, 1)
        if (moon == Moon.Blood) {
            val dealt = hit.damage
            deps.worldQueues.add(1) { if (fights[player] === fight) bloodHeal(fight, npc, dealt, false) }
        }
    }

    private fun focusChangedAt(moon: Moon): Int = focusSince[moon] ?: 0

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
        const val MAX_CONDITION = 127
        const val SELF_DAMAGE_DIVISOR = 125
        const val BLOOD_HEAL_SYNTH = "synth.varl_pm_blood_healing_projectiles_05"
        const val BLOOD_HEAL_SPOTANIM = "spotanim.fx_wardens_charging01"
        const val BLOOD_SCREAM_SPOTANIM = "spotanim.banshee_scream"
        const val HEAL_HITMARK = "hitmark.heal"
        const val HEAL_HITSPLAT_DELAY = 3
        const val BURN_GRACE = 3
        const val CONDITION_DECAY = 2
        const val LEAVE_SOUND_DELAY = 5
        const val LEAVE_DELAY = 7
        val CHAIN_MAX_HITS = listOf(4, 8, 20)
        val CHAIN_CONDITION = listOf(2, 4, 10)
        val BLOOD_FINAL_HEAL = 30..50

        fun Player.isSubdued(moon: Moon): Boolean = vars[moon.deadVarbit] != 0

        fun Player.set(variable: String, value: Int) = VarPlayerIntMapSetter.set(this, variable, value)
    }
}
