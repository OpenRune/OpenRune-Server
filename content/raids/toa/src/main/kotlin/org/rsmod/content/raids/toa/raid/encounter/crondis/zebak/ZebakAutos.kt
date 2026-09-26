package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.player.hit.queueImpactHit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

/**
 * Zebak's auto attacks and the melee bleed (Offline_Scape handleNormalCombat). Capture timing, with
 * T the tick Zebak animates: melee lands T+1; magic/ranged split at T+4 and land T+8. Prayer is
 * checked on impact (queueImpactHit with the standard modifier).
 */
internal class ZebakAutos(private val room: ZebakEncounter) {
    private val deps = room.raid.deps
    private var usingMage = false

    /** Map cycle each bleeding player stops bleeding. */
    private val bleeding = HashMap<Player, Int>()
    private val lastCoords = HashMap<Player, CoordGrid>()

    fun attack(boss: Npc, targets: List<Player>) {
        // Offline_Scape Utils.random is inclusive: random(2) == 0 is 1 in 3.
        if (deps.random.of(0, 2) == 0) usingMage = !usingMage
        val meleeTargets = targets.filter { boss.isBeside(it) }
        when {
            meleeTargets.isNotEmpty() && deps.random.of(0, 2) == 0 -> melee(boss, meleeTargets)
            else -> projectile(boss, usingMage)
        }
    }

    /** Capture: the tail plays its own anim (Offline_Scape played both on Zebak). */
    private fun melee(boss: Npc, targets: List<Player>) {
        boss.anim(if (room.enraged) ZebakSeqs.MELEE_ENRAGED else ZebakSeqs.MELEE)
        room.tail?.anim(if (room.enraged) ZebakSeqs.TAIL_MELEE_ENRAGED else ZebakSeqs.TAIL_MELEE)
        for (player in targets) {
            val slash = MeleeAttackType.Slash
            val success = deps.accuracy.rollMeleeAccuracy(boss, player, slash, deps.random)
            val damage = if (success) deps.random.of(0, room.maxHit(MELEE_MAX_HIT)) else 0
            val modifier = deps.playerHitModifier
            player.queueImpactHit(boss, MELEE_HIT_DELAY, HitType.Melee, damage, modifier)
        }
        room.schedule(1) {
            val now = room.targets()
            for (player in targets) {
                if (player !in now || deps.random.of(0, 3) != 0) continue
                player.mes("<col=ff3045>Zebak's fangs tear into your flesh, causing you to bleed.</col>")
                bleeding[player] = deps.mapClock.cycle + BLEED_TICKS
            }
        }
    }

    /**
     * Capture: the normal ranged anims even when enraged. The *_enraged ranged anims are 60 client
     * cycles long, so the projectile (released at cycle 60) would come out after they end.
     */
    private fun projectile(boss: Npc, mage: Boolean) {
        boss.anim(ZebakSeqs.RANGED)
        room.tail?.anim(ZebakSeqs.TAIL_RANGED)
        for (player in room.targets()) {
            player.soundSynth(if (mage) ZebakSynths.MAGE_SHOOT else ZebakSynths.RANGE_SHOOT)
            val split = if (mage) ZebakSynths.MAGE_SPLIT else ZebakSynths.RANGE_SPLIT
            player.soundSynth(split, delay = SPLIT_SOUND_DELAY)
        }
        val base = room.coords(ZebakCoords.PROJECTILE_BASE)
        val initial = if (mage) ZebakSpots.MAGE_INITIAL else ZebakSpots.RANGE_INITIAL
        val start = room.coords(ZebakCoords.PROJECTILE_START)
        deps.worldRepo.projectile(initial, start, base, ZebakProjs.INITIAL)
        room.schedule(SPLIT_DELAY) { split(boss, mage, base) }
    }

    /** Capture: helper npc 11744 shows the break from T+4 to T+7 (a 3-tick lifetime). */
    private fun split(boss: Npc, mage: Boolean, base: CoordGrid) {
        if (boss.hitpoints <= 0) return
        val targets = room.targets()
        if (targets.isEmpty()) return

        val helper = Npc(ZebakNpcs.SPLIT_HELPER, room.coords(ZebakCoords.SPLIT_HELPER))
        deps.npcRepo.add(helper, SPLIT_HELPER_TICKS)
        val burst = if (mage) ZebakSpots.MAGE_SPLIT else ZebakSpots.RANGE_SPLIT
        helper.spotanim(burst, height = SPLIT_HEIGHT)

        val fragment = if (mage) ZebakSpots.MAGE_FRAGMENT else ZebakSpots.RANGE_FRAGMENT
        val impact = if (mage) ZebakSpots.MAGE_IMPACT else ZebakSpots.RANGE_IMPACT
        val type = if (mage) HitType.Magic else HitType.Ranged
        for (player in targets) {
            player.soundSynth(ZebakSynths.PROJECTILE_IMPACT, delay = IMPACT_DELAY)
            deps.worldRepo.projectile(fragment, base, player, ZebakProjs.SPLIT)
            player.spotanim(impact, delay = IMPACT_DELAY, height = IMPACT_HEIGHT)
            val success =
                if (mage) {
                    deps.accuracy.rollMagicAccuracy(boss, player, deps.random)
                } else {
                    deps.accuracy.rollRangedAccuracy(boss, player, deps.random)
                }
            val damage = if (success) deps.random.of(0, room.maxHit(RANGED_MAGIC_MAX_HIT)) else 0
            player.queueImpactHit(boss, SPLIT_HIT_DELAY, type, damage, deps.playerHitModifier)
        }
    }

    /**
     * A bleeding player who moved this tick takes 5-12 (scaled) and leaves a blood splat for 10
     * ticks where they stand, unless the tile already has ground decoration (Offline_Scape; OSRS
     * Wiki: a wave that crosses one turns bloody). Offline_Scape hit per step (twice when running);
     * this is once per tick moved.
     */
    fun tickBleeding(targets: List<Player>) {
        val now = deps.mapClock.cycle
        bleeding.entries.removeIf { it.value <= now }
        for (player in targets) {
            val last = lastCoords.put(player, player.coords)
            if (last == null || last == player.coords || player !in bleeding) continue
            val base = room.maxHit(BLEED_BASE_DAMAGE)
            player.hitTypeless(deps.random.of(base, base + BLEED_DAMAGE_SPREAD))
            splat(player.coords)
        }
    }

    private fun splat(tile: CoordGrid) {
        if (deps.locRepo.findExact(tile, LocShape.GroundDecor) != null) return
        val type = ZebakLocs.BLOOD_SPLATS[deps.random.of(0, ZebakLocs.BLOOD_SPLATS.lastIndex)]
        deps.locRepo.add(tile, type, SPLAT_TICKS, LocAngle.West, LocShape.GroundDecor)
    }

    fun forget(player: Player) {
        bleeding.remove(player)
        lastCoords.remove(player)
    }

    fun clear() {
        bleeding.clear()
        lastCoords.clear()
        usingMage = deps.random.of(0, 1) == 0
    }

    private companion object {
        const val MELEE_MAX_HIT = 38
        const val RANGED_MAGIC_MAX_HIT = 16
        const val MELEE_HIT_DELAY = 2
        const val SPLIT_DELAY = 4
        const val SPLIT_HIT_DELAY = 5
        const val SPLIT_HELPER_TICKS = 3
        const val SPLIT_HEIGHT = 750
        const val SPLIT_SOUND_DELAY = 120
        const val IMPACT_DELAY = 90
        const val IMPACT_HEIGHT = 90
        const val BLEED_TICKS = 10
        const val BLEED_BASE_DAMAGE = 5
        const val BLEED_DAMAGE_SPREAD = 7
        const val SPLAT_TICKS = 10
    }
}
