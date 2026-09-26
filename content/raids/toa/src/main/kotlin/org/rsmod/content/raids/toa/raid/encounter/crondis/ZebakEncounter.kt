package org.rsmod.content.raids.toa.raid.encounter.crondis

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import org.rsmod.annotations.InternalApi
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.Constants
import org.rsmod.api.mechanics.toxins.impl.PlayerPoison
import org.rsmod.api.npc.heal
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit as queueNpcHit
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.hit.queueImpactHit
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.midiSong
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.content.raids.toa.raid.ToaPath
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRaidManager.currentRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.content.raids.toa.raid.encounter.ToaBossEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.game.proj.ProjAnim
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid
import org.rsmod.map.util.Bounds

/**
 * Zebak, the Crondis boss. Port of Offline_Scape ZebakEncounter + Zebak, with the corrections from
 * Jesse's capture (zebak-capture-v2: solo, Entry Mode, raid level 25) and numbers from the OSRS
 * Wiki.
 *
 * **Covered:**
 * - Zebak, his tail and the water crocodiles; stats scaled by raid level, party size and path level.
 * - Auto attacks every [attackSpeed] ticks: melee (can bleed), or a magic / ranged projectile that
 *   splits over everyone in the challenge area.
 * - The blood magic invocations (Not Just a Head, Arterial Spray, Blood Thinners).
 * - The enrage at 25%, the poison floor, death, reset.
 * - The Great Roar special (v27): acid, boulders and jugs, then the roar ([GreatRoar]).
 *
 * **Not yet:** the Tidal Waves special (waves, swimming, the water crocodiles attacking). Until it
 * exists every queued special is a Great Roar (see [startSpecial]).
 *
 * **How Zebak is driven.** He never uses the engine's combat. His attacks come from this room's
 * tick loop, and ZebakScript binds onAiOpPlayer2 for his types to a no-op so that retaliation
 * (players hitting him) doesn't start the default npc combat. Players still attack him normally.
 *
 * **Config lives in the module's pack**, not in code: `pack/.../configs/toa_zebak.toml` makes the
 * room's npcs stationary (moveRestrict NoMove), idle (defaultMode None), non-regenerating
 * (regenRate 0), sets their spawn facing, and gives the blood clouds a 1-tick AI timer. It also
 * holds the projectile timings ([[projectile]]). Synth and projanim names are declared in
 * `src/main/resources/gamevals.toml`.
 *
 * Timing notation from the capture: **T** is the tick Zebak animates an attack. A player hit queued
 * from npc/world code with delay `d` lands on T + d - 1.
 */
class ZebakEncounter(raid: ToaRaid, room: ToaRoom, region: Region, controllerId: Int) :
    ToaBossEncounter(raid, room, region, controllerId) {

    private var zebak: Npc? = null
    private var tail: Npc? = null
    private val waterCrocodiles = ArrayList<Npc>()

    // ---- Combat state (all reset by spawnZebak) ----

    /** 7, one faster every two path levels (max 2 faster). Set by [applyScaling]. */
    private var pathAttackSpeed = BASE_ATTACK_SPEED

    /** Overrides the attack speed while a special runs (Offline_Scape: 10 during the Great Roar). */
    private var specialAttackSpeed: Int? = null

    /** Ticks between attacks: the special's speed, else [pathAttackSpeed], 3 faster enraged. */
    private val attackSpeed: Int
        get() =
            specialAttackSpeed
                ?: if (enraged) max(MIN_ATTACK_SPEED, pathAttackSpeed - ENRAGE_SPEEDUP) else pathAttackSpeed

    private var attackCountdown = 0

    /** Offline_Scape damageFactor: max hits scale with raid level and path level, capped at 2.5x. */
    private var damageFactor = 1.0

    private var enraged = false

    /** Offline_Scape `usingMage`; flips a third of the time before each attack. */
    private var usingMage = false

    /** Specials whose HP threshold has been crossed but haven't run yet (see [startSpecial]). */
    private var specialsQueued = 0

    /** How many of [SPECIAL_THRESHOLDS] have been crossed. */
    private var specialsTriggered = 0

    /** Offline_Scape `jugAttack`: the specials alternate, starting with a random one. */
    private var nextSpecialIsJugs = false

    /**
     * A special is running (Offline_Scape `usingSpecial`): no other special and no blood magic
     * meanwhile. Autos carry on, at [specialAttackSpeed].
     */
    private var usingSpecial = false

    /** The Great Roar in progress, advanced once a tick by [tick]. */
    private var greatRoar: GreatRoar? = null

    /** Jugs on the floor or rolling. */
    private val jugs = HashMap<Npc, JugState>()

    /** Where a pushed/pulled jug is rolling to (one tile per tick), or 0/0 while standing. */
    private class JugState(var dx: Int = 0, var dz: Int = 0)

    /** The Great Roar's boulders and their blocking locs (instance coords). */
    private val boulders = HashMap<Npc, LocInfo>()

    // ---- Blood magic (Not Just a Head) ----

    /** Ticks to the next blood spell, or -1 when the invocation is off. */
    private var bloodCountdown = -1
    private var nextBloodIsBarrage = false
    private var cloudsFromSouth = false
    private val clouds = HashMap<Npc, CloudState>()

    private class CloudState(var switchTicks: Int, var startDelay: Int, var target: Player? = null)

    // ---- Bleed (melee) ----

    /** Map cycle each bleeding player stops bleeding. */
    private val bleeding = HashMap<Player, Int>()

    /** Where each player stood last tick, to notice movement while bleeding. */
    private val lastCoords = HashMap<Player, CoordGrid>()

    // ---- Poison floor (instance coords) ----

    private val poison = HashMap<CoordGrid, LocInfo>()

    /** Tiles that hurt. A new tile only starts hurting the tick after it lands (Offline_Scape). */
    private val activePoison = HashSet<CoordGrid>()
    private val pendingPoison = ArrayList<CoordGrid>()

    // ---- Room lifecycle ----

    override fun onBuilt() {
        spawnZebak()
        spawnWaterCrocodiles()
    }

    override fun onStart() {
        val boss = zebak ?: return
        // Offline_Scape onRoomStart: setMaxHealth, now that the party size is known.
        applyScaling(boss, teamSize)
        // Capture: size-9 blockers under Zebak and his tail, added when the challenge starts.
        addLoc(BLOCKER, ZEBAK_TILE)
        addLoc(BLOCKER, TAIL_TILE)
        for (player in players) {
            openBar(player)
            player.midiSong(MIDI_TOA_BOSS_ZEBAK)
            player.toaDamageTakenCurrent = 0
        }
        attackCountdown = FIRST_ATTACK_DELAY
        bloodCountdown = if (raid.isActive(NOT_JUST_A_HEAD)) attackSpeed * BLOOD_SPELL_EVERY - 1 else -1
        schedule(1) { tick() }
    }

    /** Offline_Scape enter(): preload Zebak's animations (client script 1846 is `seq_prefetch`). */
    override fun onEnter(player: Player) {
        for (seq in PRELOAD_SEQS) {
            player.runClientScript(SCRIPT_SEQ_PREFETCH, seq)
        }
        if (stage == ToaStage.STARTED) {
            openBar(player)
            player.midiSong(MIDI_TOA_BOSS_ZEBAK)
        }
    }

    override fun onLeave(player: Player) {
        closeBar(player)
        bleeding.remove(player)
        lastCoords.remove(player)
        for (state in clouds.values) {
            if (state.target === player) state.target = null
        }
    }

    override fun onComplete() {
        for (player in players) closeBar(player)
        clearFightState()
        removeWaterCrocodiles()
        playDeath()
        // Path completed + Osmumten.
        super.onComplete()
    }

    override fun onReset() {
        for (player in players) closeBar(player)
        clearFightState()
        // Offline_Scape onRoomReset: a fresh Zebak (full health, no queued specials).
        spawnZebak()
    }

    /** Clouds, poison, bleeds, jugs, boulders and any special: everything the fight leaves behind. */
    private fun clearFightState() {
        endSpecial()
        removeClouds()
        removeJugs()
        removeBoulders()
        clearPoison()
        bleeding.clear()
        lastCoords.clear()
    }

    // ---- Spawning ----

    private fun spawnZebak() {
        removeZebak()
        val boss = addNpc(ZEBAK, ZEBAK_TILE)
        owners[boss] = this
        zebak = boss
        tail = addNpc(ZEBAK_TAIL, TAIL_TILE)

        applyScaling(boss, raid.players.size.coerceAtLeast(1))
        enraged = false
        endSpecial()
        specialsQueued = 0
        specialsTriggered = 0
        nextSpecialIsJugs = deps.random.of(0, 1) == 0
        usingMage = deps.random.of(0, 1) == 0
        nextBloodIsBarrage = deps.random.of(0, 1) == 0
        cloudsFromSouth = deps.random.of(0, 1) == 0
    }

    private fun removeZebak() {
        zebak?.let {
            owners.remove(it)
            if (it.isSlotAssigned) deps.npcRepo.del(it, Int.MAX_VALUE)
        }
        tail?.let { if (it.isSlotAssigned) deps.npcRepo.del(it, Int.MAX_VALUE) }
        zebak = null
        tail = null
    }

    /**
     * Capture positions (low confidence, one capture; Offline_Scape's differ by 1-4 tiles, so vanilla
     * may randomise them). Idle until the Tidal Waves special (v26) puts someone in the water.
     */
    private fun spawnWaterCrocodiles() {
        removeWaterCrocodiles()
        for (tile in WATER_CROC_TILES) {
            waterCrocodiles += addNpc(WATER_CROC, tile)
        }
    }

    private fun removeWaterCrocodiles() {
        for (croc in waterCrocodiles) {
            if (croc.isSlotAssigned) deps.npcRepo.del(croc, Int.MAX_VALUE)
        }
        waterCrocodiles.clear()
    }

    /**
     * A room npc that stays until we remove it. Facing, mode and movement come from its config
     * (toa_zebak.toml). `add(npc, Int.MAX_VALUE)` marks npcs as respawning, so that's switched off
     * again: none of Zebak's npcs may come back by themselves.
     */
    private fun addNpc(type: String, static: CoordGrid): Npc = addNpcAt(type, coords(static))

    /** [addNpc] for instance coords (tiles picked at runtime, e.g. boulders and jugs). */
    private fun addNpcAt(type: String, tile: CoordGrid): Npc {
        val npc = Npc(type, tile)
        deps.npcRepo.add(npc, Int.MAX_VALUE)
        npc.respawns = false
        return npc
    }

    /**
     * Death animation, then the dead models (Offline_Scape: 3 ticks later, `id + 3`, which are
     * toa_zebak_dead / toa_zebak_tail_dead). Also covers `::toacomplete`.
     */
    private fun playDeath() {
        val boss = zebak ?: return
        if (!boss.isSlotAssigned) return
        owners.remove(boss)
        boss.noneMode()
        boss.hideAllOps()
        boss.anim(SEQ_DEATH)
        tail?.anim(SEQ_TAIL_DEATH)
        val tailNpc = tail
        // Scheduled after complete() dropped the old tasks, so this one still runs.
        schedule(DEATH_MODEL_DELAY) {
            if (boss.isSlotAssigned) boss.transmog(npcType(ZEBAK_DEAD), Int.MAX_VALUE)
            if (tailNpc != null && tailNpc.isSlotAssigned) tailNpc.transmog(npcType(ZEBAK_TAIL_DEAD), Int.MAX_VALUE)
        }
    }

    // ---- Scaling ----

    /**
     * ToA npc scaling (OSRS Wiki, Tombs of Amascut):
     * - raid level: +2% hitpoints, defence, accuracy and damage per 5 levels (0.4% per level);
     * - party size: +90% hitpoints for each of the 2nd and 3rd players, +60% for each one after.
     *   Offline_Scape used +90% for every extra player;
     * - path level: Offline_Scape levelFactor, +8% at level 1 then +5% per level (TODO: check).
     *
     * Hitpoints are rounded to the nearest 10: the capture had 640 at raid level 25 solo, where
     * Offline_Scape's floor gives 638.
     *
     * Accuracy is scaled through the attack, ranged and magic levels, because the standard accuracy
     * rolls read those (Offline_Scape had a separate accuracy multiplier).
     */
    private fun applyScaling(npc: Npc, partySize: Int) {
        val type = npc.type
        val raidFactor = 1.0 + raid.settings.raidLevel * RAID_LEVEL_FACTOR
        val pathLevel = raid.pathLevels[ToaPath.CRONDIS.ordinal]
        val levelFactor = if (pathLevel > 0) PATH_LEVEL_FIRST + (pathLevel - 1) * PATH_LEVEL_EACH else 0.0

        val hp = roundToTen(type.hitpoints * raidFactor * teamFactor(partySize) * (1.0 + levelFactor))
        npc.baseHitpointsLvl = hp
        npc.hitpoints = hp

        val defence = floor(type.defence * raidFactor).toInt()
        npc.baseDefenceLvl = defence
        npc.defenceLvl = defence
        val attack = floor(type.attack * raidFactor).toInt()
        npc.baseAttackLvl = attack
        npc.attackLvl = attack
        val ranged = floor(type.ranged * raidFactor).toInt()
        npc.baseRangedLvl = ranged
        npc.rangedLvl = ranged
        val magic = floor(type.magic * raidFactor).toInt()
        npc.baseMagicLvl = magic
        npc.magicLvl = magic

        damageFactor = min(MAX_DAMAGE_FACTOR, raidFactor + levelFactor)
        // OSRS Wiki: every two path levels make his autos faster.
        pathAttackSpeed = BASE_ATTACK_SPEED - min(2, pathLevel / 2)
    }

    private fun teamFactor(partySize: Int): Double {
        val extra = (partySize - 1).coerceAtLeast(0)
        val first = min(extra, 2)
        val rest = extra - first
        return 1.0 + first * TEAM_HP_FIRST + rest * TEAM_HP_REST
    }

    private fun roundToTen(value: Double): Int = ((value + 5.0) / 10.0).toInt() * 10

    /** Offline_Scape TOANPC.getMaxHit. */
    private fun maxHit(base: Int): Int = floor(base * damageFactor).toInt()

    // ---- The tick loop (Offline_Scape Zebak.processNPC + ZebakEncounter.process) ----

    private fun tick() {
        if (stage != ToaStage.STARTED) return
        val boss = zebak ?: return
        val targets = targets()

        activePoison += pendingPoison
        pendingPoison.clear()
        checkPoison(targets)
        checkBleeding(targets)
        greatRoar?.let { if (!it.step()) endSpecial() }

        if (targets.isNotEmpty() && boss.hitpoints > 0) {
            if (!usingSpecial && bloodCountdown != -1 && --bloodCountdown == 0) {
                bloodCountdown = attackSpeed * (if (enraged) BLOOD_SPELL_EVERY_ENRAGED else BLOOD_SPELL_EVERY) - 1
                castBloodMagic()
            }
            if (--attackCountdown <= 0) {
                attackCountdown = attackSpeed
                val special = !enraged && !usingSpecial && specialsQueued > 0 && startSpecial()
                if (!special) normalAttack(boss, targets)
            }
        }

        schedule(1) { tick() }
    }

    /** Players Zebak can hit: alive, not a ghost, inside the challenge area. */
    private fun targets(): List<Player> =
        players.filter { inChallengeArea(it) && !raid.isGhost(it) && !raid.isDying(it) }

    /**
     * Offline_Scape: the queued special runs instead of an auto, and they alternate (`jugAttack`)
     * between the Great Roar (shootJugs) and Tidal Waves (landWaves).
     *
     * TODO(v28): Tidal Waves. Until then its turn is a Great Roar too, so every threshold still
     * gives a special; [nextSpecialIsJugs] already alternates for when it exists.
     */
    private fun startSpecial(): Boolean {
        specialsQueued--
        nextSpecialIsJugs = !nextSpecialIsJugs
        val roar = GreatRoar()
        greatRoar = roar
        usingSpecial = true
        specialAttackSpeed = ROAR_ATTACK_SPEED
        if (!roar.step()) endSpecial()
        return true
    }

    /**
     * Back to normal attacks. Offline_Scape left `attackSpeed = 10` after its first Great Roar
     * (and never reset `usingSpecial` when a roar couldn't place its boulders); both are undone here.
     */
    private fun endSpecial() {
        greatRoar = null
        usingSpecial = false
        specialAttackSpeed = null
    }

    // ---- Auto attacks (Offline_Scape handleNormalCombat) ----

    private fun normalAttack(boss: Npc, targets: List<Player>) {
        // Offline_Scape Utils.random is inclusive: random(2) == 0 is a 1 in 3 chance.
        if (deps.random.of(0, 2) == 0) usingMage = !usingMage
        val meleeTargets = targets.filter { inMeleeRange(boss, it) }
        when {
            meleeTargets.isNotEmpty() && deps.random.of(0, 2) == 0 -> melee(boss, meleeTargets)
            usingMage -> projectileAttack(boss, mage = true)
            else -> projectileAttack(boss, mage = false)
        }
    }

    /**
     * Hits everyone in melee range. Capture: Zebak plays 9620 and the tail 9621 (Offline_Scape played
     * both on Zebak), and the hitsplat lands on T+1. Prayer is checked on impact.
     *
     * Bleed (Offline_Scape): a 1 in 4 chance per target, when the hit lands, for 10 ticks.
     */
    private fun melee(boss: Npc, meleeTargets: List<Player>) {
        boss.anim(if (enraged) SEQ_MELEE_ENRAGED else SEQ_MELEE)
        tail?.anim(if (enraged) SEQ_TAIL_MELEE_ENRAGED else SEQ_TAIL_MELEE)
        for (player in meleeTargets) {
            val success = deps.accuracy.rollMeleeAccuracy(boss, player, MeleeAttackType.Slash, deps.random)
            val damage = if (success) deps.random.of(0, maxHit(MELEE_MAX_HIT)) else 0
            player.queueImpactHit(boss, MELEE_HIT_DELAY, HitType.Melee, damage, deps.playerHitModifier)
        }
        schedule(1) {
            val now = targets()
            for (player in meleeTargets) {
                if (player !in now || deps.random.of(0, 3) != 0) continue
                player.mes("<col=ff3045>Zebak's fangs tear into your flesh, causing you to bleed.</col>")
                bleeding[player] = deps.mapClock.cycle + BLEED_TICKS
            }
        }
    }

    /**
     * The magic (pot, red orbs) or ranged (rock, fragments) attack. Offline_Scape shootNormalAttack
     * with the capture's timing:
     * - T: Zebak and the tail animate, the first projectile flies to the middle of the arena;
     * - T+4: it splits. Helper npc 11744 shows the break spotanim (capture: spawned at T+4, gone at
     *   T+7), and a fragment homes onto every player in the challenge area;
     * - T+8: the hitsplat (queued at T+4 with delay 5). Prayer is checked on impact, so switching
     *   after the split still blocks it.
     */
    private fun projectileAttack(boss: Npc, mage: Boolean) {
        boss.anim(if (enraged) SEQ_RANGED_ENRAGED else SEQ_RANGED)
        tail?.anim(if (enraged) SEQ_TAIL_RANGED_ENRAGED else SEQ_TAIL_RANGED)
        for (player in targets()) {
            player.soundSynth(if (mage) SYNTH_MAGE_SHOOT else SYNTH_RANGE_SHOOT)
            player.soundSynth(if (mage) SYNTH_MAGE_SPLIT else SYNTH_RANGE_SPLIT, delay = SPLIT_SOUND_DELAY)
        }
        val base = coords(PROJECTILE_BASE)
        val initial = if (mage) SPOT_MAGE_INITIAL else SPOT_RANGE_INITIAL
        deps.worldRepo.projAnim(
            ProjAnim.fromBoundsToCoord(Bounds(coords(PROJECTILE_START)), base, spotanimId(initial), PROJ_INITIAL)
        )

        schedule(SPLIT_DELAY) { split(boss, mage, base) }
    }

    private fun split(boss: Npc, mage: Boolean, base: CoordGrid) {
        if (boss.hitpoints <= 0) return
        val targets = targets()
        if (targets.isEmpty()) return

        // A lifetime of 3 ticks: the engine deletes it again at T+7.
        val helper = Npc(SPLIT_HELPER, coords(SPLIT_HELPER_TILE))
        deps.npcRepo.add(helper, SPLIT_HELPER_TICKS)
        helper.spotanim(if (mage) SPOT_MAGE_SPLIT else SPOT_RANGE_SPLIT, height = SPLIT_HEIGHT)

        for (player in targets) {
            player.soundSynth(SYNTH_PROJECTILE_IMPACT, delay = IMPACT_SOUND_DELAY)
            // Homes onto the player. Capture's angle 127 / end height 90 are in the projanim config.
            val fragment = if (mage) SPOT_MAGE_FRAGMENT else SPOT_RANGE_FRAGMENT
            deps.worldRepo.projAnim(ProjAnim.fromBoundsToPlayer(Bounds(base), player, spotanimId(fragment), PROJ_SPLIT))
            player.spotanim(if (mage) SPOT_MAGE_IMPACT else SPOT_RANGE_IMPACT, delay = 90, height = 90)
            val success =
                if (mage) {
                    deps.accuracy.rollMagicAccuracy(boss, player, deps.random)
                } else {
                    deps.accuracy.rollRangedAccuracy(boss, player, deps.random)
                }
            val damage = if (success) deps.random.of(0, maxHit(RANGED_MAGIC_MAX_HIT)) else 0
            val type = if (mage) HitType.Magic else HitType.Ranged
            player.queueImpactHit(boss, SPLIT_HIT_DELAY, type, damage, deps.playerHitModifier)
        }
    }

    /** Cardinally next to [npc]'s square (no diagonals), on the same level. */
    private fun inMeleeRange(npc: Npc, player: Player): Boolean {
        val c = player.coords
        if (c.level != npc.coords.level) return false
        val minX = npc.coords.x
        val minZ = npc.coords.z
        val maxX = minX + npc.size - 1
        val maxZ = minZ + npc.size - 1
        return (c.x in minX..maxX && (c.z == minZ - 1 || c.z == maxZ + 1)) ||
            (c.z in minZ..maxZ && (c.x == minX - 1 || c.x == maxX + 1))
    }

    // ---- Bleed ----

    /**
     * A bleeding player who moved this tick takes 5-12 (scaled), like Offline_Scape processMovement.
     * Offline_Scape hit once per step, so running hit twice; this hits once per tick moved.
     *
     * TODO(v26): the blood splats (loc bloodsplatter1/2, ground decoration, 10 ticks). Needed for
     * the bloody waves too.
     */
    private fun checkBleeding(targets: List<Player>) {
        val now = deps.mapClock.cycle
        bleeding.entries.removeIf { it.value <= now }
        for (player in targets) {
            val last = lastCoords.put(player, player.coords)
            if (last == null || last == player.coords || player !in bleeding) continue
            val base = maxHit(BLEED_BASE_DAMAGE)
            player.queueHit(
                delay = 1,
                type = HitType.Typeless,
                damage = deps.random.of(base, base + BLEED_DAMAGE_SPREAD),
                modifier = NoopPlayerHitModifier,
            )
        }
    }

    // ---- Poison floor (Offline_Scape ZebakEncounter.addPoison / process) ----

    /**
     * Standing on active poison while not already poisoned: a poison hit of 5-20 that also poisons
     * (Offline_Scape: a POISON hit plus applyToxin). PlayerPoison respects antipoison and immunity
     * gear (Serpentine helm, per the wiki).
     */
    private fun checkPoison(targets: List<Player>) {
        if (activePoison.isEmpty()) return
        for (player in targets) {
            if (player.coords !in activePoison || PlayerPoison.isPoisoned(player)) continue
            val base = deps.random.of(POISON_MIN, POISON_MAX)
            PlayerPoison.tryPoison(player, initialDamage = deps.random.of(base, base + POISON_SPREAD))
        }
    }

    /**
     * Puts a pool of poison on [tile] (instance coords). With [spread], neighbours within 1 tile (2
     * with Upset Stomach) each get one with a 1 in 3 chance, a tick later, via a small projectile;
     * [guaranteed] makes the tiles directly east and west certain. Used by the specials (v26).
     */
    fun addPoison(tile: CoordGrid, spread: Boolean, guaranteed: Boolean) {
        if (tile in poison) return
        val type = POISON_LOCS[deps.random.of(0, POISON_LOCS.lastIndex)]
        val angle = LocAngle.entries[deps.random.of(0, LocAngle.entries.lastIndex)]
        poison[tile] = deps.locRepo.add(tile, type, Int.MAX_VALUE, angle, LocShape.CentrepieceStraight)
        pendingPoison += tile
        if (!spread) return

        val range = if (raid.isActive(UPSET_STOMACH)) 2 else 1
        val spreadTiles = ArrayList<CoordGrid>()
        for (dx in -range..range) {
            for (dz in -range..range) {
                if (dx == 0 && dz == 0) continue
                if ((!guaranteed || dz != 0) && deps.random.of(0, 2) != 0) continue
                val next = tile.translate(dx, dz)
                if (next in poison || deps.collision.isWalkBlocked(next)) continue
                deps.worldRepo.projAnim(
                    ProjAnim.fromBoundsToCoord(Bounds(tile), next, spotanimId(SPOT_POISON_SPREAD), PROJ_POISON_SPREAD)
                )
                spreadTiles += next
            }
        }
        if (spreadTiles.isNotEmpty()) {
            schedule(1) { for (next in spreadTiles) addPoison(next, spread = false, guaranteed = false) }
        }
    }

    /** Removes the pool on [tile], if any (broken jugs and waves, v26). */
    fun removePoison(tile: CoordGrid) {
        val loc = poison.remove(tile) ?: return
        activePoison.remove(tile)
        pendingPoison.remove(tile)
        deps.locRepo.del(loc, Int.MAX_VALUE)
    }

    private fun clearPoison() {
        for (loc in poison.values) deps.locRepo.del(loc, Int.MAX_VALUE)
        poison.clear()
        activePoison.clear()
        pendingPoison.clear()
    }

    // ---- Blood magic (Offline_Scape sendBloodSpell, Not Just a Head) ----

    /**
     * Every 6 attack cycles (8 once enraged), two blood spotanims by Zebak, then 2 ticks later either
     * a blood barrage on everyone or blood clouds. They alternate, starting at random.
     */
    private fun castBloodMagic() {
        val spotanim = SpotanimType(SPOT_BLOOD_BARRAGE.asRSCM(RSCMType.SPOTANIM))
        for (tile in BLOOD_SPELL_TILES) deps.worldRepo.spotanimMap(spotanim, coords(tile))
        schedule(BLOOD_SPELL_DELAY) {
            val targets = targets()
            if (targets.isEmpty()) return@schedule
            if (nextBloodIsBarrage) bloodBarrage(targets) else spawnClouds()
            nextBloodIsBarrage = !nextBloodIsBarrage
        }
    }

    /**
     * 7-14 magic damage (scaled by raid level) on every target, and the same again on each other
     * player within 1 tile of them (2 with Arterial Spray). Zebak heals two thirds of every hit
     * taken without Protect from Magic. Prayer blocks the damage as usual.
     */
    private fun bloodBarrage(targets: List<Player>) {
        val boss = zebak ?: return
        val base = floor(BARRAGE_BASE_DAMAGE * raid.damageMultiplier).toInt()
        val radius = if (raid.isActive(ARTERIAL_SPRAY)) 2 else 1
        var heal = 0
        for (player in targets) {
            val damage = deps.random.of(base, base + BARRAGE_DAMAGE_SPREAD)
            heal += barrageHit(boss, player, damage)
            player.spotanim(SPOT_BLOOD_BARRAGE)
            for (other in targets) {
                if (other === player || chebyshev(player.coords, other.coords) > radius) continue
                heal += barrageHit(boss, other, damage)
            }
        }
        if (heal > 0) {
            boss.heal(heal, showHitsplat = true)
            for (player in players) updateBar(player)
        }
        for (player in targets) player.soundSynth(SYNTH_BLOOD_BARRAGE)
    }

    /** One barrage hit; returns what Zebak heals from it. */
    private fun barrageHit(boss: Npc, player: Player, damage: Int): Int {
        // Delay 1 lands this tick (Offline_Scape delayHit 0).
        player.queueImpactHit(boss, 1, HitType.Magic, damage, deps.playerHitModifier)
        return if (player.vars[PROTECT_FROM_MAGIC] > 0) 0 else (damage * BARRAGE_HEAL_RATIO).toInt()
    }

    /** One blood cloud, or three small ones with Blood Thinners; the side alternates. */
    private fun spawnClouds() {
        val base = BLOOD_CLOUD_TILES[if (cloudsFromSouth) 1 else 0]
        if (raid.isActive(BLOOD_THINNERS)) {
            for (i in 0 until 3) {
                val dz = if (cloudsFromSouth) i / 2 else -(i / 2)
                spawnCloud(BLOOD_CLOUD_SMALL, base.translate(i, dz))
            }
        } else {
            spawnCloud(BLOOD_CLOUD, base)
        }
        cloudsFromSouth = !cloudsFromSouth
    }

    private fun spawnCloud(type: String, static: CoordGrid) {
        // Its 1-tick AI timer (cloudTick) comes from the config's `timer = 1`.
        val cloud = addNpc(type, static)
        clouds[cloud] = CloudState(switchTicks = deps.random.of(10, 20), startDelay = CLOUD_START_DELAY)
        owners[cloud] = this
    }

    /**
     * Offline_Scape BloodCloud.processNPC, once a tick. The cloud follows a player (the nearest,
     * re-picked every 10-20 ticks). After 4 ticks it starts leeching: 2 damage to each player next
     * to it and 2 heal for itself; when nobody is next to it, it loses 2 instead.
     */
    private fun cloudTick(cloud: Npc) {
        if (stage != ToaStage.STARTED || (zebak?.hitpoints ?: 0) <= 0) return
        val state = clouds[cloud] ?: return
        if (state.startDelay > 0) state.startDelay--

        val targets = targets()
        state.switchTicks = max(0, state.switchTicks - 1)
        val switch = state.switchTicks == 0 || state.target.let { it == null || it !in targets }
        if (state.switchTicks == 0) state.switchTicks = deps.random.of(10, 20)
        if (switch && targets.isNotEmpty()) {
            // Offline_Scape: the nearest player other than the current target (random among ties).
            var best: Player? = state.target
            var bestDistance = Int.MAX_VALUE
            for (player in shuffled(targets)) {
                if (player === state.target) continue
                val distance = chebyshev(player.coords, cloud.coords)
                if (distance < bestDistance) {
                    bestDistance = distance
                    best = player
                }
            }
            state.target = best
        }

        val target = state.target
        if (target != null && !inMeleeRange(cloud, target)) cloud.walk(target.coords)

        if (state.startDelay > 0) return
        var leeched = false
        for (player in targets) {
            if (!inMeleeRange(cloud, player)) continue
            leeched = true
            player.queueHit(delay = 1, type = HitType.Typeless, damage = CLOUD_LEECH, modifier = NoopPlayerHitModifier)
            cloud.heal(CLOUD_LEECH, showHitsplat = true)
        }
        if (!leeched) {
            cloud.queueNpcHit(delay = 1, type = HitType.Typeless, damage = CLOUD_LEECH, modifier = NOOP_NPC_MODIFIER)
        }
    }

    /** A cloud reached 0 hitpoints (its death queue, see ZebakScript). */
    private fun removeCloud(cloud: Npc) {
        clouds.remove(cloud)
        owners.remove(cloud)
        if (cloud.isSlotAssigned) deps.npcRepo.del(cloud, Int.MAX_VALUE)
    }

    private fun removeClouds() {
        for (cloud in clouds.keys.toList()) removeCloud(cloud)
    }

    // ---- Great Roar (Offline_Scape Zebak.shootJugs) ----

    /**
     * One Great Roar, advanced a tick at a time by [step] (Offline_Scape's WorldTask, same tick
     * numbers):
     * - **0**: Zebak spits: 6 acid pools, 2-3 boulders (3 solo) each with acid 2 tiles behind them
     *   (east), and 6-8 jugs, at least one placed so it can be pushed or pulled into each boulder.
     * - **5**: they land. Boulders block the tile; the acid spreads; anyone underneath takes 2-5.
     * - **33**: the roar animation. Players have had ~4 attacks' time to break jugs into boulders,
     *   which clears the acid behind them.
     * - **36, 38, 40**: three roar waves. Everyone not in a boulder's safe strip (the boulder's row,
     *   its tile and the 3 behind it) is knocked 2 tiles east and hit hard. Boulders take 50 a
     *   wave (150 hitpoints, so the third destroys them); the first wave breaks every jug left.
     * - **49**: over.
     */
    private inner class GreatRoar {
        private var ticks = -1
        private var boulderTiles: List<CoordGrid> = emptyList()
        private var jugTiles: List<CoordGrid> = emptyList()
        private var acidTiles: List<CoordGrid> = emptyList()
        private val boulderAcidTiles = ArrayList<CoordGrid>()

        /** `false` once the special is over (or couldn't start). */
        fun step(): Boolean {
            val boss = zebak ?: return false
            if (boss.hitpoints <= 0) return false
            ticks++
            when (ticks) {
                0 -> return launch(boss)
                ROAR_LAND_TICK -> land()
                ROAR_SCREAM_TICK -> scream(boss)
                in ROAR_WAVE_TICKS -> roarWave(first = ticks == ROAR_WAVE_TICKS.first())
                ROAR_END_TICK -> {
                    // The third wave already destroyed them; this only catches leftovers.
                    removeBoulders()
                    return false
                }
            }
            return true
        }

        private fun launch(boss: Npc): Boolean {
            boss.anim(SEQ_RANGED)
            tail?.resetAnim()
            deps.worldRepo.soundArea(coords(ZEBAK_MIDDLE), SYNTH_JUGS_SHOOT, radius = ROAR_SOUND_RADIUS)

            // Offline_Scape stopped here (no boulders or no solvable jugs) and the attack was lost.
            val boulders = boulderTiles() ?: return false
            val jugPlan = jugSolveTiles(boulders) ?: return false
            boulderTiles = boulders
            jugTiles = jugPlan
            val acid = freeTiles(GROUND_MIN, GROUND_MAX, excludes = boulders).take(ROAR_ACID_POOLS).toMutableList()

            val mouth = coords(PROJECTILE_START)
            for (tile in acid) lob(SPOT_ACID, mouth, tile)
            for (boulder in boulders) {
                val behind = boulder.translate(BOULDER_ACID_DX, 0)
                if (behind in acid) continue
                acid += behind
                boulderAcidTiles += behind
                lob(SPOT_POISON_SPREAD, mouth, behind)
            }
            acidTiles = acid
            for (boulder in boulders) lob(SPOT_BOULDER, mouth, boulder)
            for (jug in jugPlan) lob(SPOT_JUG, mouth, jug)
            return true
        }

        private fun land() {
            val targets = targets()
            if (targets.isEmpty()) return
            for (tile in boulderTiles) {
                spawnBoulder(tile)
                for (player in targets) {
                    if (player.coords != tile) continue
                    hitTypeless(player, deps.random.of(LANDING_MIN, LANDING_MAX))
                    knockOffBoulder(player, tile)
                }
            }
            for (tile in boulderAcidTiles) addPoison(tile, spread = true, guaranteed = true)
            for (tile in acidTiles) {
                deps.worldRepo.soundArea(tile, SYNTH_ACID_LAND, radius = ROAR_SOUND_RADIUS)
                addPoison(tile, spread = true, guaranteed = false)
            }
            val dust = spotanim(SPOT_ROAR_DUST)
            for (tile in jugTiles) {
                spawnJug(tile)
                deps.worldRepo.spotanimMap(dust, tile)
                for (player in targets) {
                    if (player.coords == tile) hitTypeless(player, deps.random.of(LANDING_MIN, LANDING_MAX))
                }
            }
        }

        private fun scream(boss: Npc) {
            // Offline_Scape: the next auto comes 11 ticks after the scream starts.
            attackCountdown = ROAR_NEXT_ATTACK
            boss.anim(SEQ_ROAR)
            tail?.anim(SEQ_TAIL_ROAR)
            for (player in targets()) {
                for ((synth, delay) in SCREAM_SOUNDS) player.soundSynth(synth, delay = delay)
            }
        }

        private fun roarWave(first: Boolean) {
            val min = coords(GROUND_MIN)
            val max = coords(GROUND_MAX)
            val middle = coords(ZEBAK_MIDDLE)
            val dust = spotanim(SPOT_ROAR_DUST)
            for (x in min.x..max.x) {
                for (z in min.z..max.z) {
                    val tile = CoordGrid(x, z, min.level)
                    if (inSafeStrip(tile, includeBoulder = false) || !isOpenFloor(tile)) continue
                    // Offline_Scape passed 1 + distance as the spotanim's height; it reads as the
                    // intended ripple delay (client cycles) out from Zebak, so it's used as that.
                    deps.worldRepo.spotanimMap(dust, tile, delay = 1 + chebyshev(tile, middle))
                }
            }
            for (boulder in boulders.keys.toList()) {
                boulder.queueNpcHit(delay = 1, type = HitType.Typeless, damage = BOULDER_ROAR_DAMAGE, modifier = NOOP_NPC_MODIFIER)
            }
            for (player in targets()) {
                if (inSafeStrip(player.coords, includeBoulder = true)) continue
                roarPush(player)
            }
            if (first) {
                // Offline_Scape: 5 damage from Zebak to each jug, which breaks (clears acid) at 5 hp.
                for (jug in jugs.keys.toList()) breakJug(jug, jug.coords)
            }
        }

        /** Offline_Scape: same row as a boulder, from its tile (players) or the tile after it (dust), up to 3 behind. */
        private fun inSafeStrip(tile: CoordGrid, includeBoulder: Boolean): Boolean =
            boulderTiles.any { b ->
                tile.z == b.z && tile.x <= b.x + SAFE_STRIP_LENGTH &&
                    (tile.x > b.x || (includeBoulder && tile.x == b.x))
            }
    }

    /** The Offline_Scape "lob" projectile used by every Great Roar throw (lands 5 ticks later). */
    private fun lob(spot: String, from: CoordGrid, to: CoordGrid) {
        deps.worldRepo.projAnim(ProjAnim.fromBoundsToCoord(Bounds(from), to, spotanimId(spot), PROJ_LOB))
    }

    /**
     * Offline_Scape pushPlayer(scream = true): up to 2 tiles east while the way is walkable, facing
     * west (towards Zebak); damage 20-30 scaled, typeless.
     */
    @OptIn(InternalApi::class)
    private fun roarPush(player: Player) {
        var dest = player.coords
        repeat(ROAR_PUSH_TILES) {
            val next = dest.translate(1, 0)
            if (!isOpenFloor(next)) return@repeat
            dest = next
        }
        val moved = dest != player.coords
        if (!moved) player.abortRoute()
        deps.launcher.launchLenient(player) {
            if (moved) telejump(dest, TeleportType.Exempt)
            player.faceDirection(Direction.West)
            anim(SEQ_PLAYER_PUSHED)
        }
        player.soundSynth(SYNTH_PLAYER_PUSHED)
        val base = maxHit(ROAR_BASE_DAMAGE)
        hitTypeless(player, deps.random.of(base, base + ROAR_DAMAGE_SPREAD))
    }

    /** Offline_Scape movePlayer: a boulder landed on [player]; hop to the first free neighbour. */
    @OptIn(InternalApi::class)
    private fun knockOffBoulder(player: Player, boulder: CoordGrid) {
        for (dx in -1..1) {
            for (dz in -1..1) {
                if (dx == 0 && dz == 0) continue
                val next = boulder.translate(dx, dz)
                if (!isOpenFloor(next)) continue
                val start = player.coords
                deps.launcher.launchLenient(player) {
                    anim(SEQ_PLAYER_KNOCKED)
                    exactMove(start, next, delay1 = 0, delay2 = KNOCK_OFF_CYCLES, dir = faceAngle(-dx, -dz), teleportType = TeleportType.Exempt)
                }
                return
            }
        }
    }

    private fun hitTypeless(player: Player, damage: Int) {
        player.queueHit(delay = 1, type = HitType.Typeless, damage = damage, modifier = NoopPlayerHitModifier)
    }

    // -- Placement (Offline_Scape getFreeTiles / getBoulderLocations / getJugsSolveLocations) --

    /** Walkable, no centrepiece loc on it (Offline_Scape: no type-10 object, floor free). */
    private fun isOpenFloor(tile: CoordGrid): Boolean =
        !deps.collision.isWalkBlocked(tile) && deps.locRepo.findExact(tile, LocShape.CentrepieceStraight) == null

    /** Open, acid-free tiles in a static rectangle, shuffled; instance coords. */
    private fun freeTiles(min: CoordGrid, max: CoordGrid, excludes: Collection<CoordGrid>): List<CoordGrid> {
        val from = coords(min)
        val to = coords(max)
        val tiles = ArrayList<CoordGrid>()
        for (x in from.x..to.x) {
            for (z in from.z..to.z) {
                val tile = CoordGrid(x, z, from.level)
                if (tile in excludes || tile in poison || !isOpenFloor(tile)) continue
                tiles += tile
            }
        }
        return shuffled(tiles)
    }

    /**
     * Around a random tile of the boulder area, one candidate per row within 6 rows (a tile with a
     * free tile east of it, within 3 columns). 2 boulders in a team, 3 solo. `null` if none fit.
     */
    private fun boulderTiles(): List<CoordGrid>? {
        val free = freeTiles(BOULDER_MIN, BOULDER_MAX, excludes = emptyList())
        if (free.isEmpty()) return null
        val freeSet = free.toHashSet()
        val base = free[0]
        val candidates = ArrayList<CoordGrid>()
        for (dz in -BOULDER_ROWS..BOULDER_ROWS) {
            val row = ArrayList<CoordGrid>()
            for (dx in -BOULDER_COLUMNS..BOULDER_COLUMNS) {
                val tile = base.translate(dx, dz)
                if (tile in freeSet && tile.translate(1, 0) in freeSet && tile !in candidates) row += tile
            }
            if (row.isNotEmpty()) candidates += row[deps.random.of(maxExclusive = row.size)]
        }
        if (candidates.isEmpty()) return null
        val count = if (teamSize > 1) BOULDERS_TEAM else BOULDERS_SOLO
        return shuffled(candidates).take(count)
    }

    /**
     * Offline_Scape getJugsSolveLocations: first one jug per boulder on a tile that lines up with it
     * (so it can be pushed or pulled into it), then up to as many "decoy" jugs, 6-8 in total.
     * `null` if not every boulder can get a jug. Offline_Scape's edge check used the area's
     * south-west corner for both edges; the north and east edges are excluded here as intended.
     */
    private fun jugSolveTiles(boulders: List<CoordGrid>): List<CoordGrid>? {
        val min = coords(GROUND_MIN)
        val max = coords(GROUND_MAX)
        val solving = ArrayList<CoordGrid>()
        val decoys = ArrayList<CoordGrid>()
        for (tile in freeTiles(GROUND_MIN, GROUND_MAX, excludes = boulders)) {
            if (tile.x == min.x || tile.z == min.z || tile.x == max.x || tile.z == max.z) continue
            var lines = false
            for (boulder in shuffled(boulders)) {
                val dx = boulder.x - tile.x
                val dz = boulder.z - tile.z
                val adx = abs(dx)
                val adz = abs(dz)
                if ((adx == 0 || dx > 0) && adz == 0) continue
                if (adz < 2 && dx > -4 && dx < 0) continue
                if (adx > 10 || adz > 10) continue
                if (abs(adz - adx) < 2 || dx in -2..0 || adz <= 1) {
                    lines = true
                    break
                }
            }
            if (lines) solving += tile else decoys += tile
        }
        if (solving.size < boulders.size) return null
        val total = deps.random.of(JUGS_MIN, JUGS_MAX)
        val picked = ArrayList(solving.take(boulders.size))
        picked += decoys.take(min(total - picked.size, picked.size))
        return picked
    }

    // -- Boulders --

    private fun spawnBoulder(tile: CoordGrid) {
        val boulder = addNpcAt(BOULDER, tile)
        owners[boulder] = this
        boulders[boulder] = deps.locRepo.add(tile, BOULDER_BLOCKER, Int.MAX_VALUE, LocAngle.West, LocShape.CentrepieceStraight)
        deps.worldRepo.soundArea(tile, SYNTH_BOULDER_LAND, radius = BOULDER_SOUND_RADIUS)
    }

    /** Its death queue (ZebakScript), or cleanup. */
    private fun removeBoulder(boulder: Npc) {
        val loc = boulders.remove(boulder) ?: return
        owners.remove(boulder)
        deps.locRepo.del(loc, Int.MAX_VALUE)
        if (boulder.isSlotAssigned) deps.npcRepo.del(boulder, Int.MAX_VALUE)
    }

    private fun removeBoulders() {
        for (boulder in boulders.keys.toList()) removeBoulder(boulder)
    }

    // -- Jugs (Offline_Scape CrondisJug / JugPushAction) --

    private fun spawnJug(tile: CoordGrid) {
        val jug = addNpcAt(JUG, tile)
        owners[jug] = this
        jugs[jug] = JugState()
    }

    /**
     * Push (away from the player) or Pull (towards them). The jug becomes the rolling jug npc and
     * rolls a tile a tick ([jugTick]). Diagonal if the player stands diagonally to it.
     */
    private fun moveJug(player: Player, jug: Npc, push: Boolean) {
        val state = jugs[jug] ?: return
        if (state.dx != 0 || state.dz != 0) return
        var dx = Integer.signum(jug.coords.x - player.coords.x)
        var dz = Integer.signum(jug.coords.z - player.coords.z)
        if (!push) {
            dx = -dx
            dz = -dz
        }
        if (dx == 0 && dz == 0) return
        state.dx = dx
        state.dz = dz
        jug.transmog(npcType(JUG_ROLLING), Int.MAX_VALUE)
        player.anim(SEQ_PLAYER_MOVE_JUG)
    }

    /**
     * A rolling jug, once a tick (its config `timer = 1`). Offline_Scape: rolling into a boulder
     * breaks it there; rolling off the floor (into the water) is a splash and it's gone, without
     * clearing anything.
     *
     * Offline_Scape moved the jug onto the boulder's tile and broke it there; the boulder's tile
     * blocks walking here, so it breaks beside it with the splash centred on the boulder, which
     * clears the same tiles.
     *
     * TODO: the OSRS Wiki's changelog says jugs "roll two tiles further before stopping", so
     * vanilla stops them after some distance. Offline_Scape rolls until something stops them.
     */
    private fun jugTick(jug: Npc) {
        val state = jugs[jug] ?: return
        if (state.dx == 0 && state.dz == 0) return
        val next = jug.coords.translate(state.dx, state.dz)
        val boulder = boulders.values.any { it.coords == next }
        when {
            boulder -> breakJug(jug, next)
            deps.collision.isWalkBlocked(next) -> {
                deps.worldRepo.spotanimMap(spotanim(SPOT_WATER_SPLASH), next)
                removeJug(jug)
            }
            else -> jug.walk(next)
        }
    }

    /**
     * Offline_Scape CrondisJug.sendDeath: the jug bursts at [centre]; water flies to every acid
     * tile within 2 (1 with Upset Stomach, so 5x5 or 3x3), which is cleared a tick later.
     */
    private fun breakJug(jug: Npc, centre: CoordGrid) {
        if (jugs.remove(jug) == null) return
        removeJug(jug)
        deps.worldRepo.spotanimMap(spotanim(SPOT_JUG_BREAK), centre)
        val range = if (raid.isActive(UPSET_STOMACH)) 1 else 2
        val cleared = ArrayList<CoordGrid>()
        for (dx in -range..range) {
            for (dz in -range..range) {
                val tile = centre.translate(dx, dz)
                if (tile !in poison) continue
                cleared += tile
                deps.worldRepo.projAnim(ProjAnim.fromBoundsToCoord(Bounds(centre), tile, spotanimId(SPOT_JUG_SPLASH), PROJ_JUG_SPLASH))
            }
        }
        if (cleared.isEmpty()) return
        schedule(1) {
            val splash = spotanim(SPOT_ACID_CLEARED)
            for (tile in cleared) {
                removePoison(tile)
                deps.worldRepo.spotanimMap(splash, tile)
            }
        }
    }

    private fun removeJug(jug: Npc) {
        jugs.remove(jug)
        owners.remove(jug)
        if (jug.isSlotAssigned) deps.npcRepo.del(jug, Int.MAX_VALUE)
    }

    private fun removeJugs() {
        for (jug in jugs.keys.toList()) removeJug(jug)
    }

    private fun spotanim(name: String): SpotanimType = SpotanimType(spotanimId(name))

    /** An `em_face_*` angle for facing along (dx, dz). */
    private fun faceAngle(dx: Int, dz: Int): Int =
        when {
            dx == 0 && dz > 0 -> Constants.em_face_north
            dx > 0 && dz > 0 -> Constants.em_face_northeast
            dx > 0 && dz == 0 -> Constants.em_face_east
            dx > 0 -> Constants.em_face_southeast
            dx == 0 -> Constants.em_face_south
            dz < 0 -> Constants.em_face_southwest
            dz == 0 -> Constants.em_face_west
            else -> Constants.em_face_northwest
        }

    // ---- Zebak taking damage ----

    /**
     * Every hit on Zebak (ZebakScript's onNpcHit): the damage sound (capture: area sound 6590,
     * radius 10, at his centre), the bar, the special thresholds and the enrage.
     */
    private fun zebakHit(boss: Npc, hit: Hit) {
        if (stage != ToaStage.STARTED || boss !== zebak) return
        if (hit.damage > 0) {
            deps.worldRepo.soundArea(coords(ZEBAK_CENTRE), SYNTH_ZEBAK_DAMAGED, radius = DAMAGED_SOUND_RADIUS)
        }
        for (player in players) updateBar(player)
        if (enraged || boss.hitpoints <= 0) return

        // Offline_Scape setHitpoints: at most one special queued per hit.
        val max = boss.baseHitpointsLvl
        if (specialsTriggered < SPECIAL_THRESHOLDS.size &&
            boss.hitpoints <= max * SPECIAL_THRESHOLDS[specialsTriggered]
        ) {
            specialsTriggered++
            specialsQueued++
        }
        if (boss.hitpoints <= max * ENRAGE_THRESHOLD) enrage(boss)
    }

    /**
     * OSRS Wiki: at ~25% Zebak becomes enraged (npc toa_zebak_enraged, the enraged attack
     * animations) and attacks much faster. Offline_Scape: attack speed -3 (min 2), no more specials,
     * blood magic every 8 cycles, and a sound. Offline_Scape didn't change the npc.
     */
    private fun enrage(boss: Npc) {
        enraged = true
        attackCountdown = min(attackCountdown, attackSpeed)
        boss.transmog(npcType(ZEBAK_ENRAGED), Int.MAX_VALUE)
        for (player in targets()) player.soundSynth(SYNTH_FINAL_PHASE)
    }

    // ---- Boss bar ----

    private fun openBar(player: Player) {
        val npc = zebak ?: return
        deps.bossHpBar.onOpen(player, npc)
        updateBar(player)
    }

    private fun updateBar(player: Player) {
        val npc = zebak ?: return
        deps.bossHpBar.onUpdate(player, npc)
    }

    private fun closeBar(player: Player) {
        val npc = zebak ?: return
        deps.bossHpBar.onClose(player, npc, instant = true)
    }

    // ---- Helpers ----

    private fun addLoc(type: String, static: CoordGrid) {
        deps.locRepo.add(coords(static), type, Int.MAX_VALUE, LocAngle.West, LocShape.CentrepieceStraight)
    }

    private fun spotanimId(name: String): Int = name.asRSCM(RSCMType.SPOTANIM)

    private fun chebyshev(a: CoordGrid, b: CoordGrid): Int = max(abs(a.x - b.x), abs(a.z - b.z))

    /** Fisher-Yates through GameRandom (no `shuffled()`: randomness must go through deps.random). */
    private fun <T> shuffled(list: List<T>): List<T> {
        val copy = list.toMutableList()
        for (i in copy.lastIndex downTo 1) {
            val j = deps.random.of(maxExclusive = i + 1)
            val swap = copy[i]
            copy[i] = copy[j]
            copy[j] = swap
        }
        return copy
    }

    companion object {
        const val ZEBAK = "npc.toa_zebak"
        const val ZEBAK_ENRAGED = "npc.toa_zebak_enraged"
        const val BLOOD_CLOUD = "npc.toa_zebak_blood_cloud"
        const val BLOOD_CLOUD_SMALL = "npc.toa_zebak_blood_cloud_small"
        private const val ZEBAK_TAIL = "npc.toa_zebak_tail"
        private const val ZEBAK_DEAD = "npc.toa_zebak_dead"
        private const val ZEBAK_TAIL_DEAD = "npc.toa_zebak_tail_dead"
        private const val WATER_CROC = "npc.toa_zebak_watercroc"
        private const val SPLIT_HELPER = "npc.spotanim_zebak_ranged01_npc" // 11744
        const val JUG = "npc.toa_zebak_jug" // 11735: Push, Pull, Hit
        const val JUG_ROLLING = "npc.toa_zebak_jug_rolling" // 11736: Attack
        const val BOULDER = "npc.toa_zebak_safespot" // 11737
        private const val BOULDER_BLOCKER = "loc.invisible_type8_blocking_active" // 43876

        private const val BLOCKER = "loc.invisible_type8_blocking_size9" // 3192
        private val POISON_LOCS =
            listOf(
                "loc.toa_zebak_vomit01",
                "loc.toa_zebak_vomit02",
                "loc.toa_zebak_vomit03",
                "loc.toa_zebak_vomit04",
                "loc.toa_zebak_vomit05",
                "loc.toa_zebak_vomit06",
            )

        // Invocation names (cache struct param 1160).
        private const val NOT_JUST_A_HEAD = "Not Just a Head"
        private const val ARTERIAL_SPRAY = "Arterial Spray"
        private const val BLOOD_THINNERS = "Blood Thinners"
        private const val UPSET_STOMACH = "Upset Stomach"

        // -- Coordinates (static) --
        private val ZEBAK_TILE = CoordGrid(3918, 5404, 0)
        private val TAIL_TILE = CoordGrid(3909, 5403, 0)
        private val ZEBAK_CENTRE = CoordGrid(3922, 5408, 0)

        /** Offline_Scape ZEBAK_MIDDLE_LOC: where roar sounds and dust ripples come from. */
        private val ZEBAK_MIDDLE = CoordGrid(3926, 5408, 0)

        /** Offline_Scape GROUND_MIN/MAX_LOCATION: the arena floor (acid, jugs, roar dust). */
        private val GROUND_MIN = CoordGrid(3926, 5398, 0)
        private val GROUND_MAX = CoordGrid(3942, 5418, 0)

        /** Offline_Scape BOULDER_MIN/MAX_LOCATION. */
        private val BOULDER_MIN = CoordGrid(3925, 5401, 0)
        private val BOULDER_MAX = CoordGrid(3935, 5415, 0)
        private val SPLIT_HELPER_TILE = CoordGrid(3930, 5405, 0)
        private val PROJECTILE_START = CoordGrid(3925, 5408, 0)
        private val PROJECTILE_BASE = CoordGrid(3933, 5408, 0)
        private val BLOOD_SPELL_TILES = listOf(CoordGrid(3924, 5406, 0), CoordGrid(3925, 5410, 0))

        /** Offline_Scape BLOOD_CLOUD_LOCATIONS: north side, south side. */
        private val BLOOD_CLOUD_TILES = listOf(CoordGrid(3931, 5413, 0), CoordGrid(3934, 5401, 0))

        /**
         * Capture (low confidence). Offline_Scape: (3919,5403), (3921,5418), (3934,5423),
         * (3936,5394), (3936,5395), (3938,5420), (3948,5408) x2.
         */
        private val WATER_CROC_TILES =
            listOf(
                CoordGrid(3948, 5408, 0),
                CoordGrid(3948, 5408, 0),
                CoordGrid(3935, 5422, 0),
                CoordGrid(3934, 5420, 0),
                CoordGrid(3921, 5418, 0),
                CoordGrid(3937, 5396, 0),
                CoordGrid(3937, 5395, 0),
                CoordGrid(3919, 5403, 0),
            )

        // -- Scaling --
        private const val RAID_LEVEL_FACTOR = 0.004
        private const val TEAM_HP_FIRST = 0.9
        private const val TEAM_HP_REST = 0.6
        private const val PATH_LEVEL_FIRST = 0.08
        private const val PATH_LEVEL_EACH = 0.05
        private const val MAX_DAMAGE_FACTOR = 2.5

        // -- Timing --
        private const val BASE_ATTACK_SPEED = 7
        private const val MIN_ATTACK_SPEED = 2
        private const val ENRAGE_SPEEDUP = 3

        /** Capture: the first attack is 10 ticks after "Challenge started" (Offline_Scape: 7). */
        private const val FIRST_ATTACK_DELAY = 10

        /** Capture: the magic/ranged split is at T+4. */
        private const val SPLIT_DELAY = 4

        /** Queued at T+4, lands T+8. */
        private const val SPLIT_HIT_DELAY = 5

        /** Lands T+1. */
        private const val MELEE_HIT_DELAY = 2

        /** Capture: helper npc 11744 from T+4 to T+7. */
        private const val SPLIT_HELPER_TICKS = 3

        /** Offline_Scape MAGE/RANGE_BREAK_GFX height. */
        private const val SPLIT_HEIGHT = 750
        private const val DEATH_MODEL_DELAY = 3

        // -- Damage (bases, before damageFactor) --
        private const val MELEE_MAX_HIT = 38
        private const val RANGED_MAGIC_MAX_HIT = 16
        private const val BLEED_TICKS = 10
        private const val BLEED_BASE_DAMAGE = 5
        private const val BLEED_DAMAGE_SPREAD = 7
        private const val POISON_MIN = 5
        private const val POISON_MAX = 10
        private const val POISON_SPREAD = 10

        // -- Blood magic --
        private const val BLOOD_SPELL_EVERY = 6
        private const val BLOOD_SPELL_EVERY_ENRAGED = 8
        private const val BLOOD_SPELL_DELAY = 2
        private const val BARRAGE_BASE_DAMAGE = 7
        private const val BARRAGE_DAMAGE_SPREAD = 7
        private const val BARRAGE_HEAL_RATIO = 0.66
        private const val CLOUD_START_DELAY = 4
        private const val CLOUD_LEECH = 2
        private const val PROTECT_FROM_MAGIC = "varbit.prayer_protectfrommagic"

        // -- Great Roar (Offline_Scape shootJugs; tick numbers from its task) --
        private const val ROAR_LAND_TICK = 5
        private const val ROAR_SCREAM_TICK = 33
        private val ROAR_WAVE_TICKS = intArrayOf(36, 38, 40)
        private const val ROAR_END_TICK = 49

        /** Offline_Scape set attackSpeed = 10 for the roar (and, by mistake, for good). */
        private const val ROAR_ATTACK_SPEED = 10
        private const val ROAR_NEXT_ATTACK = 11
        private const val ROAR_ACID_POOLS = 6

        /** The acid 2 tiles behind (east of) each boulder. */
        private const val BOULDER_ACID_DX = 2
        private const val BOULDER_ROWS = 6
        private const val BOULDER_COLUMNS = 3
        private const val BOULDERS_TEAM = 2
        private const val BOULDERS_SOLO = 3
        private const val BOULDER_ROAR_DAMAGE = 50
        private const val JUGS_MIN = 6
        private const val JUGS_MAX = 8

        /** OSRS Wiki: safe within 3 tiles behind the stones. */
        private const val SAFE_STRIP_LENGTH = 3
        private const val ROAR_PUSH_TILES = 2
        private const val ROAR_BASE_DAMAGE = 20
        private const val ROAR_DAMAGE_SPREAD = 10
        private const val LANDING_MIN = 2
        private const val LANDING_MAX = 5
        private const val KNOCK_OFF_CYCLES = 30
        private const val ROAR_SOUND_RADIUS = 15
        private const val BOULDER_SOUND_RADIUS = 5

        /** OSRS Wiki: specials queued at 85, 70, 55 and 40%; enraged at ~25%. */
        private val SPECIAL_THRESHOLDS = doubleArrayOf(0.85, 0.70, 0.55, 0.40)
        private const val ENRAGE_THRESHOLD = 0.25

        // -- Animations (cache names; Offline_Scape ids in comments) --
        private const val SEQ_MELEE = "seq.npc_zebak01_attack_melee" // 9620
        private const val SEQ_TAIL_MELEE = "seq.npc_zebak02_attack_melee" // 9621
        private const val SEQ_MELEE_ENRAGED = "seq.npc_zebak01_attack_melee_enraged" // 9622
        private const val SEQ_TAIL_MELEE_ENRAGED = "seq.npc_zebak02_attack_melee_enraged" // 9623
        private const val SEQ_RANGED = "seq.npc_zebak01_attack_ranged" // 9624
        private const val SEQ_TAIL_RANGED = "seq.npc_zebak02_attack_ranged" // 9625
        private const val SEQ_RANGED_ENRAGED = "seq.npc_zebak01_attack_ranged_enraged" // 9626
        private const val SEQ_TAIL_RANGED_ENRAGED = "seq.npc_zebak02_attack_ranged_enraged" // 9627
        private const val SEQ_ROAR = "seq.npc_zebak01_attack_roar" // 9628
        private const val SEQ_TAIL_ROAR = "seq.npc_zebak02_attack_roar" // 9629
        private const val SEQ_PLAYER_PUSHED = "seq.warguild_parry_defend" // 4177
        private const val SEQ_PLAYER_KNOCKED = "seq.agilityarena_player_spikedback" // 1114
        private const val SEQ_PLAYER_MOVE_JUG = "seq.human_leverdown" // 834
        private const val SEQ_DEATH = "seq.npc_zebak01_death" // 9634
        private const val SEQ_TAIL_DEATH = "seq.npc_zebak02_death" // 9635

        // -- Spotanims (names from osrs-dumps config/dump.spot) --
        private const val SPOT_MAGE_INITIAL = "spotanim.zebak_mage_projanim_initial" // 2176
        private const val SPOT_RANGE_INITIAL = "spotanim.zebak_range_projanim_initial" // 2178
        private const val SPOT_MAGE_SPLIT = "spotanim.zebak_mage_split" // 2186
        private const val SPOT_RANGE_SPLIT = "spotanim.zebak_ranged_split" // 2185
        private const val SPOT_MAGE_FRAGMENT = "spotanim.zebak_mage_projanim_split" // 2181
        private const val SPOT_RANGE_FRAGMENT = "spotanim.zebak_ranged_fragment01" // 2187
        private const val SPOT_MAGE_IMPACT = "spotanim.fireblast_impact" // 131
        private const val SPOT_RANGE_IMPACT = "spotanim.darkbow_smoke_arrow_impact" // 1103
        private const val SPOT_BLOOD_BARRAGE = "spotanim.spell_blood_barrage_impact" // 377
        private const val SPOT_POISON_SPREAD = "spotanim.zebak_vomit_projectile0" // 2194
        private const val SPOT_ACID = "spotanim.tob_xarpus_acidspit" // 1555
        private const val SPOT_BOULDER = "spotanim.zebak_safespot_travel" // 2172
        private const val SPOT_JUG = "spotanim.zebak_waterjug_travel" // 2173
        private const val SPOT_ROAR_DUST = "spotanim.zebak_roar_wave_dust" // 2184
        private const val SPOT_JUG_BREAK = "spotanim.zebak_waterjug_break" // 2192
        private const val SPOT_JUG_SPLASH = "spotanim.zebak_waterjug_splash_travel" // 2193
        private const val SPOT_ACID_CLEARED = "spotanim.waterstrike_impact" // 95
        private const val SPOT_WATER_SPLASH = "spotanim.watersplash" // 68

        // -- Projectile types (pack/.../configs/toa_zebak.toml) --
        private const val PROJ_INITIAL = "projanim.toa_zebak_initial"
        private const val PROJ_SPLIT = "projanim.toa_zebak_split"
        private const val PROJ_POISON_SPREAD = "projanim.toa_zebak_poison_spread"
        private const val PROJ_LOB = "projanim.toa_zebak_lob"
        private const val PROJ_JUG_SPLASH = "projanim.toa_zebak_jug_splash"

        // -- Sounds. Names declared in the module's gamevals.toml (the cache has none); they are
        // the labels from the cache dbtable synth_zabakboss. Offline_Scape ids in comments. --
        private const val SYNTH_MAGE_SHOOT = "synth.toa_zebak_red_projectile_04" // 5823
        private const val SYNTH_RANGE_SHOOT = "synth.toa_zebak_whoosh_projectile_02" // 5819
        private const val SYNTH_MAGE_SPLIT = "synth.toa_zebak_redirected_jug_break_01" // 5878
        private const val SYNTH_RANGE_SPLIT = "synth.toa_zebak_redirected_projectile_02" // 5896
        private const val SYNTH_PROJECTILE_IMPACT = "synth.toa_zebak_projectile_impact_01" // 5884

        /** Capture: area sound when Zebak takes damage (loops 1, radius 10, at his centre). */
        private const val SYNTH_ZEBAK_DAMAGED = "synth.toa_zebak_defend_01" // 6590
        private const val DAMAGED_SOUND_RADIUS = 10

        /** Offline_Scape FINAL_PHASE_SOUND. */
        private const val SYNTH_FINAL_PHASE = "synth.fi_trollking_roar" // 3405

        /** Offline_Scape BLOOD_BARRAGE_IMPACT_SOUND; already named in .data/gamevals/synth.rscm. */
        private const val SYNTH_BLOOD_BARRAGE = "synth.blood_barrage_impact" // 102

        // Great Roar (Offline_Scape JUGS_SHOOT, BASE_POISON_LAND, BOULDER_LAND, PLAYER_PUSHED).
        private const val SYNTH_JUGS_SHOOT = "synth.toa_zebak_vomit_colours_projectile_10" // 5908
        private const val SYNTH_ACID_LAND = "synth.toa_zebak_vomit_colours_projectile_splat_03" // 5909
        private const val SYNTH_BOULDER_LAND = "synth.toa_zebak_debris_impact_01" // 5913
        private const val SYNTH_PLAYER_PUSHED = "synth.toa_zebak_roar_single_tremor_03" // 5888

        /** Offline_Scape SCREAM_SOUNDS: (synth, delay). */
        private val SCREAM_SOUNDS =
            listOf(
                "synth.toa_zebak_attack_hand_stomp_first_01" to 16, // 5860
                "synth.toa_zebak_attack_roar_05" to 20, // 5836
                "synth.toa_zebak_attack_hand_stomp_01" to 32, // 5845
                "synth.toa_zebak_attack_roar_high_02" to 69, // 5904
                "synth.toa_zebak_attack_roar_bass_02" to 70, // 5838
                "synth.toa_zebak_attack_hand_stomp_final_01" to 79, // 5851
                "synth.toa_zebak_attack_jaw_shut_01" to 260, // 5863
            )

        private const val SPLIT_SOUND_DELAY = 120
        private const val IMPACT_SOUND_DELAY = 90

        /** Capture: music track 736 from the challenge start. */
        private const val MIDI_TOA_BOSS_ZEBAK = 736

        private const val SCRIPT_SEQ_PREFETCH = 1846
        private val PRELOAD_SEQS: List<Int> = (9618..9646).toList() + listOf(9532, 9533, 9534, 9541)

        /** Blood clouds decay by themselves; no prayer or defence applies. */
        private val NOOP_NPC_MODIFIER = NpcHitModifier {}

        private fun npcType(name: String) = ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC))!!

        /** Zebak and his clouds -> their room, for ZebakScript's handlers. */
        private val owners = HashMap<Npc, ZebakEncounter>()

        private fun roomOf(npc: Npc): ZebakEncounter? {
            val room = owners[npc] ?: return null
            if (room.destroyed) {
                owners.remove(npc)
                return null
            }
            return room
        }

        /** ZebakScript's onNpcHit for Zebak (both his types). */
        fun onZebakHit(npc: Npc, hit: Hit) {
            roomOf(npc)?.zebakHit(npc, hit)
        }

        /** ZebakScript's death queue for Zebak: the room is won. */
        fun onZebakDeath(npc: Npc) {
            val room = roomOf(npc) ?: return
            if (npc === room.zebak) room.complete()
        }

        /** ZebakScript's onAiTimer for the clouds. */
        fun onCloudTick(npc: Npc) {
            roomOf(npc)?.cloudTick(npc)
        }

        /** ZebakScript's death queue for the clouds. */
        fun onCloudDeath(npc: Npc) {
            roomOf(npc)?.removeCloud(npc)
        }

        /** ZebakScript: a jug's Push (op1) or Pull (op3). */
        fun onJugMoved(player: Player, jug: Npc, push: Boolean) {
            roomOf(jug)?.moveJug(player, jug, push)
        }

        /**
         * ZebakScript: a player broke a jug, by its "Hit" op (op4), or any hit landing on it (the
         * rolling jug's "Attack" op2 uses normal combat). Offline_Scape CrondisJug.processHit: any
         * hit not from Zebak breaks it, whatever the damage.
         */
        fun onJugBroken(jug: Npc) {
            roomOf(jug)?.breakJug(jug, jug.coords)
        }

        /** ZebakScript's onAiTimer for the jugs (config `timer = 1`). */
        fun onJugTick(jug: Npc) {
            roomOf(jug)?.jugTick(jug)
        }

        /** ZebakScript's death queue for the boulders (the third roar wave). */
        fun onBoulderDeath(boulder: Npc) {
            roomOf(boulder)?.removeBoulder(boulder)
        }

        /**
         * Capture: every damaging hit updates varbits toa_damage_taken (14323) and
         * toa_damage_taken_current (14375); the second resets at the challenge start.
         *
         * Read here as damage the player takes, added up. TODO: confirm against the capture's
         * values, and whether other rooms do this too (it may belong in the raid, not this room).
         */
        fun onPlayerDamaged(player: Player, damage: Int) {
            val room = player.currentRaid?.encounterOf(player) as? ZebakEncounter ?: return
            if (room.stage != ToaStage.STARTED) return
            player.toaDamageTaken = (player.toaDamageTaken + damage).coerceAtMost(DAMAGE_TAKEN_MAX)
            player.toaDamageTakenCurrent =
                (player.toaDamageTakenCurrent + damage).coerceAtMost(DAMAGE_TAKEN_CURRENT_MAX)
        }

        /** Bits 0-15 and 15-29 of their varps (osrs-dumps config/dump.varbit). */
        private const val DAMAGE_TAKEN_MAX = 65_535
        private const val DAMAGE_TAKEN_CURRENT_MAX = 32_767
    }
}

private var Player.toaDamageTaken by intVarBit("varbit.toa_damage_taken")
private var Player.toaDamageTakenCurrent by intVarBit("varbit.toa_damage_taken_current")
