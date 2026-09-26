package org.rsmod.content.raids.toa.raid.encounter.crondis

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.mechanics.toxins.impl.PlayerPoison
import org.rsmod.api.npc.heal
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit as queueNpcHit
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.hit.queueImpactHit
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

/**
 * Zebak, the Crondis boss. Port of Offline_Scape ZebakEncounter + Zebak, with the corrections from
 * Jesse's capture (zebak-capture-v2: solo, Entry Mode, raid level 25) and numbers from the OSRS
 * Wiki.
 *
 * **v25 covers the core fight:**
 * - Zebak, his tail and the water crocodiles; stats scaled by raid level, party size and path level.
 * - Auto attacks every [attackSpeed] ticks: melee (can bleed), or a magic / ranged projectile that
 *   splits over everyone in the challenge area.
 * - The blood magic invocations (Not Just a Head, Arterial Spray, Blood Thinners).
 * - The enrage at 25%, the poison floor (used by the specials), death, reset.
 *
 * **Not yet (v26):** the two specials (Great Roar with jugs and boulders; Tidal Waves with swimming
 * and the water crocodiles attacking). Their HP thresholds are already tracked ([specialsQueued]);
 * [startSpecial] is the hook they'll go into. Until then Zebak just keeps auto-attacking.
 *
 * **How Zebak is driven.** He never uses the engine's combat. He's stationary (movementLocked), his
 * attacks come from this room's tick loop, and ZebakScript binds onAiOpPlayer2 for his types to a
 * no-op so that retaliation (players hitting him) doesn't start the default npc combat. Players
 * still attack him normally.
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

    /** Ticks between attacks: 7, one faster every two path levels (max 2 faster), 3 faster enraged. */
    private var attackSpeed = BASE_ATTACK_SPEED
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

    /** A special is running; no autos or blood magic meanwhile (Offline_Scape `usingSpecial`). */
    private var usingSpecial = false

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

    /** Clouds, poison and bleeds: everything the fight leaves behind. */
    private fun clearFightState() {
        removeClouds()
        clearPoison()
        bleeding.clear()
        lastCoords.clear()
    }

    // ---- Spawning ----

    private fun spawnZebak() {
        removeZebak()
        val boss = addNpc(ZEBAK, ZEBAK_TILE, Direction.East)
        boss.movementLocked = true
        owners[boss] = this
        zebak = boss
        tail = addNpc(ZEBAK_TAIL, TAIL_TILE, Direction.East).also { it.movementLocked = true }

        applyScaling(boss, raid.players.size.coerceAtLeast(1))
        enraged = false
        usingSpecial = false
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
            waterCrocodiles += addNpc(WATER_CROC, tile, Direction.South)
        }
    }

    private fun removeWaterCrocodiles() {
        for (croc in waterCrocodiles) {
            if (croc.isSlotAssigned) deps.npcRepo.del(croc, Int.MAX_VALUE)
        }
        waterCrocodiles.clear()
    }

    /**
     * A room npc that stays until we remove it. `add(npc, Int.MAX_VALUE)` marks npcs as respawning,
     * so that's switched off again: none of Zebak's npcs may come back by themselves.
     */
    private fun addNpc(type: String, static: CoordGrid, facing: Direction): Npc {
        val npc = Npc(type, coords(static))
        npc.respawnDir = facing
        deps.npcRepo.add(npc, Int.MAX_VALUE)
        npc.respawns = false
        npc.noneMode()
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
        attackSpeed = BASE_ATTACK_SPEED - min(2, pathLevel / 2)
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
     * Where the specials go (v26). Offline_Scape: the queued special runs instead of an auto, and
     * they alternate (`jugAttack`) between the Great Roar (shootJugs) and Tidal Waves (landWaves).
     * Returns `false` until they exist, so Zebak auto-attacks instead and the special stays queued.
     */
    private fun startSpecial(): Boolean = false

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
        projectile(
            spotanim = if (mage) SPOT_MAGE_INITIAL else SPOT_RANGE_INITIAL,
            from = coords(PROJECTILE_START),
            to = base,
            startHeight = 200,
            endHeight = 700,
            startTime = 60,
            endTime = 120,
            angle = clientAngle(30),
        )

        schedule(SPLIT_DELAY) { split(boss, mage, base) }
    }

    private fun split(boss: Npc, mage: Boolean, base: CoordGrid) {
        if (boss.hitpoints <= 0) return
        val targets = targets()
        if (targets.isEmpty()) return

        val helper = Npc(SPLIT_HELPER, coords(SPLIT_HELPER_TILE))
        helper.respawnDir = Direction.South
        deps.npcRepo.add(helper, SPLIT_HELPER_TICKS)
        helper.noneMode()
        helper.spotanim(if (mage) SPOT_MAGE_SPLIT else SPOT_RANGE_SPLIT, height = SPLIT_HEIGHT)

        for (player in targets) {
            player.soundSynth(SYNTH_PROJECTILE_IMPACT, delay = IMPACT_SOUND_DELAY)
            projectile(
                spotanim = if (mage) SPOT_MAGE_FRAGMENT else SPOT_RANGE_FRAGMENT,
                from = base,
                to = player.coords,
                startHeight = SPLIT_HEIGHT,
                // Capture: end height 90 and angle 127 (Offline_Scape: 88 and 1).
                endHeight = 90,
                startTime = 0,
                endTime = 90,
                angle = 127,
                homing = player,
            )
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
                projectile(
                    spotanim = SPOT_POISON_SPREAD,
                    from = tile,
                    to = next,
                    startHeight = 0,
                    endHeight = 0,
                    startTime = 0,
                    endTime = 30,
                    angle = clientAngle(20),
                    progress = 10,
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
        val cloud = addNpc(type, static, Direction.West)
        cloud.aiTimer(1)
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

    // ---- Zebak taking damage ----

    /**
     * Every hit on Zebak (ZebakScript's onNpcHit): the damage sound (capture: area sound 6590,
     * radius 10, at his centre), the bar, the special thresholds and the enrage.
     */
    private fun zebakHit(boss: Npc, hit: Hit) {
        if (stage != ToaStage.STARTED || boss !== zebak) return
        if (hit.damage > 0) {
            deps.zoneUpdates.soundArea(coords(ZEBAK_CENTRE), SYNTH_ZEBAK_DAMAGED, 0, 1, DAMAGED_SOUND_RADIUS, 0)
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
        attackSpeed = max(MIN_ATTACK_SPEED, attackSpeed - ENRAGE_SPEEDUP)
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

    /**
     * A coord-to-coord projectile (like BossFx.bossProjectile). The numbers are Offline_Scape's
     * Projectile fields: `startTime` is its delay and `endTime` is delay + duration + distance *
     * multiplier (every Zebak projectile has multiplier 0). [homing] makes it follow a player.
     */
    private fun projectile(
        spotanim: String,
        from: CoordGrid,
        to: CoordGrid,
        startHeight: Int,
        endHeight: Int,
        startTime: Int,
        endTime: Int,
        angle: Int,
        progress: Int = 0,
        homing: Player? = null,
    ) {
        deps.worldRepo.projAnim(
            ProjAnim(
                spotanim = spotanim.asRSCM(RSCMType.SPOTANIM),
                startHeight = startHeight,
                endHeight = endHeight,
                startTime = startTime,
                endTime = endTime,
                angle = angle,
                progress = progress,
                sourceIndex = 0,
                targetIndex = homing?.let { -(it.slotId + 1) } ?: 0,
                startCoord = from,
                endCoord = to,
            )
        )
    }

    /** Offline_Scape Projectile: degrees (0-90) to the client's units (0-64). */
    private fun clientAngle(degrees: Int): Int = Math.round(degrees * 64f / 90f)

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
        private const val SPLIT_HEIGHT = 700
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

        // -- Sounds. No gameval names; labels from the cache dbtable synth_zabakboss. --
        /** toa_zebak_red_projectile_04 */
        private const val SYNTH_MAGE_SHOOT = 5823

        /** toa_zebak_whoosh_projectile_02 */
        private const val SYNTH_RANGE_SHOOT = 5819

        /** toa_zebak_redirected_jug_break_01 */
        private const val SYNTH_MAGE_SPLIT = 5878

        /** toa_zebak_redirected_projectile_02 */
        private const val SYNTH_RANGE_SPLIT = 5896

        /** toa_zebak_projectile_impact_01 */
        private const val SYNTH_PROJECTILE_IMPACT = 5884

        /** toa_zebak_defend_01. Capture: area sound when Zebak takes damage. */
        private const val SYNTH_ZEBAK_DAMAGED = 6590
        private const val DAMAGED_SOUND_RADIUS = 10

        /** Offline_Scape FINAL_PHASE_SOUND (not in synth_zabakboss). */
        private const val SYNTH_FINAL_PHASE = 3405

        /** Offline_Scape BLOOD_BARRAGE_IMPACT_SOUND. */
        private const val SYNTH_BLOOD_BARRAGE = 102

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
