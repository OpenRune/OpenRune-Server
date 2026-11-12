package org.rsmod.content.bosses.moons.eclipse

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.EnumMap
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.combat.commons.player.finishNpcHit
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.CombatStance
import org.rsmod.api.combat.commons.styles.MeleeAttackStyle
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.manager.PlayerAttackManager
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.combat.weapon.types.AttackTypes
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.weapons.WeaponRegistry
import org.rsmod.api.weapons.attack
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.projAnimSpecific
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.output.spam
import org.rsmod.api.player.righthand
import org.rsmod.content.bosses.moons.Moon
import org.rsmod.content.bosses.moons.MoonsFight
import org.rsmod.content.bosses.moons.MoonsFights
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.game.entity.util.EntityFaceAngle
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.hit.HitType
import org.rsmod.game.proj.ProjAnim
import org.rsmod.game.type.getOrNull
import org.rsmod.map.CoordGrid

@Singleton
class EclipseFights
@Inject
constructor(
    private val deps: BossDeps,
    private val fights: MoonsFights,
    private val npcHitModifier: NpcHitModifier,
    private val protectedAccess: ProtectedAccessLauncher,
    private val weapons: WeaponRegistry,
    private val attackManager: PlayerAttackManager,
    private val attackTypes: AttackTypes,
    private val attackStyles: AttackStyles,
) {
    private var shield: Npc? = null
    private var shieldStep = 0
    private var facingShield = false
    private var clonesActive = false
    private val clones = EnumMap<Side, Npc>(Side::class.java)

    private val moon = Moon.Eclipse

    private fun fighters(): List<Player> =
        fights.playersIn(moon).filter { fights.of(it)?.subdued == false }

    internal fun glance(event: NpcHitEvents.Modify) {
        val hit = event.hit
        if (hit.damage <= 0 || !hit.isFromPlayer) return
        val player = hit.sourceUid?.let { PlayerUid(it).resolve(deps.playerList) } ?: return
        hit.damage = glanced(player, hit.damage, GLANCE_REDUCTION)
    }

    private fun glanced(player: Player, damage: Int, reduction: Int): Int {
        if (damage <= 0) return damage
        if (deps.random.of(0, 99) >= player.vars[MoonsFights.CONDITION]) return damage
        player.mes(GLANCE_MESSAGE)
        return (damage - reduction).coerceAtLeast(0)
    }

    fun joined(player: Player) {
        if (fights.of(player)?.moon != moon || !clonesActive) return
        player.bound = true
        player.boundFacing = CoordGrid.NULL
    }

    fun shieldSpawned(spawned: Npc) {
        shield = spawned
        shieldStep = 0
        spawned.ignoreCombatInteractions = true
        spawned.anim(SHIELD_RISE_SEQ)
        spawned.faceSquare(moon.southWest.translate(-2, -2))
        facingShield = true
        fights.npc(moon)?.facePathingEntitySquare(spawned)
    }

    fun shieldUnface() {
        facingShield = false
        fights.npc(moon)?.let {
            it.resetFaceEntity()
            it.resetPendingFaceSquare()
        }
    }

    fun shieldStep() {
        val current = shield?.takeIf { it.isValidTarget() } ?: return
        val step = shieldStep++
        val (x, z) = EclipseGeometry.shieldPosition(step)
        val dest = moon.southWest.translate(x, z)
        if (dest != current.coords) PathingEntityCommon.teleport(current, deps.collision, dest)
        if (facingShield) fights.npc(moon)?.facePathingEntitySquare(current)
        EclipseGeometry.turnAt(step)?.let { (dx, dz) ->
            current.faceSquare(dest.translate(dx * FACE_REACH, dz * FACE_REACH))
        }
    }

    fun shieldSink() {
        val current = shield?.takeIf { it.isValidTarget() } ?: return
        current.anim(SHIELD_SINK_SEQ)
        for (player in fighters()) player.soundSynth(SHIELD_SINK_SYNTH)
    }

    fun rayFire(player: Player) {
        val fight = fights.of(player) ?: return
        val tracker = fights.tracker(moon) ?: return
        val hitpoints = tracker.of(player)?.hitpoints ?: return
        tracker.set(player, (hitpoints - (hitpoints / RAY_SELF_DIVISOR).coerceAtLeast(1)).coerceAtLeast(1))

        val npc = fights.npc(moon) ?: return
        val sw = moon.southWest
        val current = shield?.takeIf { it.isValidTarget() }
        val blocked =
            current != null &&
                EclipseGeometry.behindShield(
                    current.coords.x - sw.x,
                    current.coords.z - sw.z,
                    player.coords.x - sw.x,
                    player.coords.z - sw.z,
                )
        if (blocked && current != null) {
            player.projAnimSpecific(
                rayProjectile(moon.centre, current.coords, BLOCKED_END_TIME, BLOCKED_END_HEIGHT, current.slotId + 1)
            )
            deps.worldQueues.add(SHIELD_STRUCK_DELAY) {
                if (fights.of(player) === fight) player.soundSynth(SHIELD_STRUCK_SYNTH)
            }
            return
        }
        deps.worldRepo.projAnim(
            rayProjectile(moon.centre, player.coords, EXPOSED_END_TIME, EXPOSED_END_HEIGHT, -(player.slotId + 1))
        )
        player.soundSynth(RAY_HIT_SYNTH)
        moon.attackMessage?.let(player::spam)
        val damage = deps.random.of(RAY_DAMAGE.first, RAY_DAMAGE.last)
        fights.addCondition(fight, damage)
        player.finishNpcHit(npc, RAY_HIT_DELAY, HitType.Typeless, damage, deps.playerHitModifier)
    }

    private fun rayProjectile(from: CoordGrid, to: CoordGrid, endTime: Int, endHeight: Int, targetIndex: Int) =
        ProjAnim(
            spotanim = RAY_SPOTANIM.asRSCM(RSCMType.SPOTANIM),
            startHeight = RAY_START_HEIGHT,
            endHeight = endHeight,
            startTime = RAY_START_TIME,
            endTime = endTime,
            angle = RAY_ANGLE,
            progress = RAY_PROGRESS,
            sourceIndex = 0,
            targetIndex = targetIndex,
            startCoord = from,
            endCoord = to,
        )

    fun bindAll() {
        clonesActive = true
        clones.clear()
        val centre = moon.centre
        for (player in fighters()) {
            val from = player.coords
            if (from != centre) fights.throwTo(player, centre)
            player.abortRoute()
            player.boundFacing = if (from != centre) from else CoordGrid.NULL
            player.bound = true
        }
    }

    fun unbindAll() {
        clonesActive = false
        for (player in fights.playersIn(moon)) {
            player.bound = false
            fights.of(player)?.clearParry()
        }
    }

    fun cloneSpawned(clone: Npc, side: Side) {
        clones[side] = clone
        clone.ignoreCombatInteractions = true
        clone.vars["varn.flat_armour"] = CLONE_FLAT_ARMOUR
        clone.anim(CLONE_RISE_SEQ)
        clone.hideAllOps()
        clone.pendingFaceAngle = EntityFaceAngle(side.faceAngle)
        clone.pendingFaceInstant = true
        if (fighters().isEmpty()) hideClone(clone)
    }

    fun subdued(player: Player) {
        player.bound = false
        fights.of(player)?.clearParry()
        if (fighters().isEmpty()) clones.values.forEach(::hideClone)
    }

    private fun hideClone(clone: Npc) {
        if (!clone.isValidTarget()) return
        val type = ServerCacheManager.getNpc(INVISIBLE_MOON.asRSCM(RSCMType.NPC)) ?: return
        clone.transmog(type, Int.MAX_VALUE)
    }

    fun cloneLeave(side: Side) {
        clones[side]?.takeIf { it.isValidTarget() }?.anim(CLONE_LEAVE_SEQ, delay = 1)
    }

    fun cloneStrike(side: Side) {
        val clone = clones[side]?.takeIf { it.isValidTarget() } ?: return
        val npc = fights.npc(moon) ?: return
        clone.anim(CLONE_STRIKE_SEQ)
        val centre = moon.centre
        val tracker = fights.tracker(moon)
        for (player in fighters()) {
            if (!player.bound) continue
            val fight = fights.of(player) ?: continue
            tracker?.of(player)?.hitpoints?.let { hp -> tracker.set(player, (hp - 1).coerceAtLeast(1)) }
            if (facing(player, centre, side)) {
                parry(player, fight, clone)
            } else {
                val damage = deps.random.of(0, UNPARRIED_MAX_HIT)
                player.finishNpcHit(npc, 1, HitType.Typeless, damage, deps.playerHitModifier)
            }
        }
    }

    private fun facing(player: Player, centre: CoordGrid, side: Side): Boolean {
        val facing = player.boundFacing
        if (facing == CoordGrid.NULL || facing == centre) return false
        return EclipseGeometry.octant(facing.x - centre.x, facing.z - centre.z) == side.octant
    }

    private fun parry(player: Player, fight: MoonsFight, clone: Npc) {
        player.queueHit(clone, 1, HitType.Typeless, 0, deps.playerHitModifier)
        fight.startParry(PARRY_WINDOW)
        val launched = protectedAccess.launch(player) { weaponParry(clone) }
        if (launched) return
        player.anim(attackAnim(player))
        val max = deps.maxHit.getMeleeMaxHit(player, clone, null, null, 1.0)
        clone.queueHit(player, 1, HitType.Melee, deps.random.of(0, max), npcHitModifier)
    }

    private suspend fun ProtectedAccess.weaponParry(clone: Npc) {
        val attack =
            CombatAttack.Melee(
                player.righthand,
                MeleeAttackType.from(attackTypes.get(player)),
                MeleeAttackStyle.from(attackStyles.get(player)),
                CombatStance.entries.firstOrNull { it.varValue == vars["varp.com_mode"] } ?: CombatStance.Stance1,
            )
        val handled = weapons.getMelee(attack.weapon)?.attack(this, clone, attack) == true
        if (!handled) {
            val damage = attackManager.rollMeleeDamage(player, clone, attack)
            attackManager.giveCombatXp(player, clone, attack, damage)
            attackManager.playWeaponFx(player, attack)
            attackManager.queueMeleeHit(player, clone, damage)
        }
        player.clearInteraction()
    }

    private fun attackAnim(player: Player): String {
        val weapon = getOrNull(player.righthand)
        val anim = weapon?.paramOrNull(params.attack_anim_stance1) ?: return UNARMED_ATTACK_SEQ
        return RSCM.getReverseMapping(RSCMType.SEQ, anim.id)
    }

    /** A hit on a clone: only hits landing inside a parry window count, and they come off the attacker's Moon pool. */
    fun cloneHit(player: Player, damage: Int): Int {
        val fight = fights.of(player) ?: return 0
        if (!fight.parryActive) return 0
        val dealt = glanced(player, damage, CLONES_GLANCE_REDUCTION)
        moon.hitSynth?.let(player::soundSynth)
        fights.damagePool(player, dealt)
        return dealt
    }

    fun setMoonOps(npc: Npc, enabled: Boolean) {
        if (enabled) npc.showAllOps() else npc.hideAllOps()
    }

    fun hideMoon(npc: Npc) {
        val type = ServerCacheManager.getNpc(INVISIBLE_MOON.asRSCM(RSCMType.NPC)) ?: return
        npc.transmog(type, Int.MAX_VALUE)
    }

    private companion object {
        private const val GLANCE_MESSAGE =
            "<col=ff8e32>Blinded by the Eclipse Moon's light, your attack glances off its shield!</col>"
        private const val GLANCE_REDUCTION = 12
        private const val CLONES_GLANCE_REDUCTION = 8
        private const val FACE_REACH = 5
        private const val SHIELD_RISE_SEQ = "seq.vfx_djinn_eclipse_moon_spawn_01"
        private const val SHIELD_SINK_SEQ = "seq.vfx_djinn_eclipse_moon_despawn_01"
        private const val SHIELD_SINK_SYNTH = "synth.varl_pm_eclipse_sphere_despawn_11"
        private const val SHIELD_STRUCK_SYNTH = "synth.varl_pm_eclipse_sphere_impact_03"
        private const val SHIELD_STRUCK_DELAY = 2
        private const val RAY_HIT_SYNTH = "synth.varl_pm_eclipse_projectile_player_impact_03"
        private const val RAY_SPOTANIM = "spotanim.vfx_djinn_eclipse_moon_projanim_01"
        private const val RAY_SELF_DIVISOR = 60
        private const val RAY_HIT_DELAY = 2
        private const val RAY_START_HEIGHT = 610
        private const val RAY_START_TIME = 20
        private const val RAY_ANGLE = 127
        private const val RAY_PROGRESS = 200
        private const val BLOCKED_END_TIME = 50
        private const val BLOCKED_END_HEIGHT = 200
        private const val EXPOSED_END_TIME = 60
        private const val EXPOSED_END_HEIGHT = 70
        private val RAY_DAMAGE = 1..10
        private const val CLONE_FLAT_ARMOUR = 4
        private const val PARRY_WINDOW = 1
        private const val CLONE_RISE_SEQ = "seq.npc_djinn_02_spawn"
        private const val CLONE_STRIKE_SEQ = "seq.npc_djinn_02_melee"
        private const val CLONE_LEAVE_SEQ = "seq.npc_djinn_02_teleport_idle_disappear"
        private const val UNPARRIED_MAX_HIT = 15
        private const val UNARMED_ATTACK_SEQ = "seq.human_unarmedpunch"
        private const val INVISIBLE_MOON = "npc.pmoon_eclipse_boss_invis"
    }
}
