package org.alter.plugins.content.npcs.bosses

import org.alter.api.*
import org.alter.api.dsl.*
import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.combat.AttackStyle
import org.alter.game.model.combat.CombatClass
import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.entity.*
import org.alter.game.model.queue.*
import org.alter.game.model.timer.ATTACK_DELAY
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.plugin.*
import java.lang.ref.WeakReference
import org.alter.plugins.content.combat.*
import org.alter.plugins.content.combat.formula.DragonfireFormula
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.strategy.RangedCombatStrategy
import org.alter.plugins.content.mechanics.poison.poison
import org.alter.rscm.RSCM.asRSCM

/**
 * Corrupted King Black Dragon — a 3-phase boss encounter.
 *
 * Phase 1 (HP > 50%): Enhanced KBD attacks — stronger melee, guaranteed poison.
 * Phase 2 (HP 50%-20%): Corruption tiles + splash dragonfire AoE.
 * Phase 3 (HP < 20%): Shadow burst, minions, attack speed increase.
 */
class CorruptedKbdPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    companion object {
        private val PHASE_ATTR = AttributeKey<Int>()
        private val CORRUPTION_TILES = AttributeKey<MutableList<Pair<Tile, Int>>>()
        private val COMBAT_TICK_COUNTER = AttributeKey<Int>()
        private val MINIONS_SPAWNED = AttributeKey<Boolean>()

        private const val PHASE_1 = 1
        private const val PHASE_2 = 2
        private const val PHASE_3 = 3

        private const val CORRUPTION_SPAWN_INTERVAL = 10
        private const val CORRUPTION_TILE_LIFETIME = 8
        private const val SHADOW_BURST_INTERVAL = 15
        private const val BOSS_REGION = 9033

        /**
         * Drop table entry: RSCM name, min amount, max amount, weight.
         * For entries where min == max, only that amount is dropped.
         * A null rscmName means "Nothing" (no drop).
         */
        private data class Drop(val rscmName: String?, val min: Int, val max: Int, val weight: Int)

        /** Main drop table — total weight = 128, rolled once on death. */
        private val DROP_TABLE = listOf(
            Drop("items.rune_longsword",    1, 1, 8),
            Drop("items.rune_platelegs",    1, 1, 6),
            Drop("items.dragon_med_helm",   1, 1, 2),
            Drop("items.dragon_dagger",     1, 1, 5),
            Drop("items.firerune",        500, 500, 10),
            Drop("items.bloodrune",        50, 50, 8),
            Drop("items.deathrune",        75, 75, 8),
            Drop("items.lawrune",          50, 50, 8),
            Drop("items.cert_adamantite_bar", 10, 10, 8),
            Drop("items.cert_runite_bar",     3,  3, 5),
            Drop("items.cert_yew_logs",     200, 200, 10),
            Drop("items.cert_magic_logs",    30, 30, 5),
            Drop("items.shark",              5,  5, 10),
            Drop("items.coins",          15000, 30000, 15),
            Drop("items.cert_dragon_bones", 10, 10, 8),
            Drop(null,                       0,  0, 12),  // Nothing
        )
    }

    init {
        setMultiCombatRegion(region = BOSS_REGION)

        spawnNpc("npcs.king_dragon", x = 2274, z = 4698, walkRadius = 5)

        setCombatDef("npcs.king_dragon") {
            species {
                +NpcSpecies.DRACONIC
                +NpcSpecies.BASIC_DRAGON
            }

            configs {
                attackSpeed = 3
                respawnDelay = 50
            }

            aggro {
                radius = 16
                searchDelay = 1
            }

            stats {
                hitpoints = 600
                attack = 320
                strength = 300
                defence = 280
                magic = 300
            }

            bonuses {
                defenceStab = 70
                defenceSlash = 90
                defenceCrush = 90
                defenceMagic = 80
                defenceRanged = 70
            }

            anims {
                block = "sequences.dragon_block"
                death = "sequences.dragon_death"
            }
        }

        onNpcCombat("npcs.king_dragon") {
            npc.queue {
                npc.combat(this)
            }
        }

        // ---------------------------------------------------------------
        // Drop table
        // ---------------------------------------------------------------
        onNpcDeath("npcs.king_dragon") {
            val npc = ctx as Npc
            val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return@onNpcDeath
            val tile = npc.tile

            // Always drops
            world.spawn(GroundItem("items.dragon_bones".asRSCM(), 1, tile, killer))
            world.spawn(GroundItem("items.black_dhide".asRSCM(), 2, tile, killer))

            // Main table roll (total weight = 128)
            val roll = world.random(127)
            var cumulative = 0
            val drop = DROP_TABLE.firstOrNull { entry ->
                cumulative += entry.weight
                roll < cumulative
            }

            if (drop?.rscmName != null) {
                val amount = if (drop.min == drop.max) drop.min else world.random(drop.min..drop.max)
                world.spawn(GroundItem(drop.rscmName.asRSCM(), amount, tile, killer))
            }
        }
    }

    // ---------------------------------------------------------------
    // Phase tracking
    // ---------------------------------------------------------------

    private fun Npc.getCurrentPhase(): Int = attr[PHASE_ATTR] ?: PHASE_1

    private fun Npc.setPhase(phase: Int) {
        attr[PHASE_ATTR] = phase
    }

    /**
     * Determine the correct phase based on current HP and trigger
     * transition effects when the phase changes.
     */
    private fun Npc.updatePhase() {
        val hp = getCurrentHp()
        val maxHp = getMaxHp()
        val hpPercent = (hp.toDouble() / maxHp.toDouble()) * 100.0

        val newPhase = when {
            hpPercent > 50.0 -> PHASE_1
            hpPercent > 20.0 -> PHASE_2
            else -> PHASE_3
        }

        val oldPhase = getCurrentPhase()
        if (newPhase != oldPhase) {
            setPhase(newPhase)
            onPhaseTransition(oldPhase, newPhase)
        }
    }

    private fun Npc.onPhaseTransition(oldPhase: Int, newPhase: Int) {
        when (newPhase) {
            PHASE_2 -> {
                world.players.forEach { player ->
                    if (player.tile.regionId == BOSS_REGION && player.getCurrentHp() > 0) {
                        player.message("The dragon's corruption intensifies!")
                    }
                }
                animate("sequences.dragon_head_attack")
            }
            PHASE_3 -> {
                world.players.forEach { player ->
                    if (player.tile.regionId == BOSS_REGION && player.getCurrentHp() > 0) {
                        player.message("The dragon enters a frenzy!")
                    }
                }
                animate("sequences.dragon_head_attack")
                spawnMinions()
            }
        }
    }

    // ---------------------------------------------------------------
    // Main combat loop
    // ---------------------------------------------------------------

    private suspend fun Npc.combat(task: QueueTask) {
        var target = getCombatTarget() ?: return

        while (canEngageCombat(target)) {
            facePawn(target)
            if (moveToAttackRange(task, target, distance = 6, projectile = true) && isAttackDelayReady()) {

                updatePhase()

                val phase = getCurrentPhase()

                // Tick corruption tiles (Phase 2+)
                if (phase >= PHASE_2) {
                    tickCorruption()
                }

                // Shadow burst check (Phase 3)
                if (phase == PHASE_3) {
                    val counter = attr[COMBAT_TICK_COUNTER] ?: 0
                    if (counter % SHADOW_BURST_INTERVAL == 0 && counter > 0) {
                        shadowBurst()
                    }
                }

                when (phase) {
                    PHASE_1 -> phase1Attack(task, target)
                    PHASE_2 -> phase2Attack(task, target)
                    PHASE_3 -> phase3Attack(task, target)
                }

                postAttackLogic(target)

                // Phase 3: override attack delay to 2 ticks (faster attacks)
                if (phase == PHASE_3) {
                    timers[ATTACK_DELAY] = 2
                }
            }
            task.wait(1)
            target = getCombatTarget() ?: break
        }

        resetFacePawn()
        removeCombatTarget()
    }

    // ---------------------------------------------------------------
    // Corruption tile mechanics (Phase 2+)
    // ---------------------------------------------------------------

    /**
     * Tick corruption tiles: spawn new ones periodically, damage players
     * standing on them, and remove expired tiles.
     */
    private fun Npc.tickCorruption() {
        val counter = (attr[COMBAT_TICK_COUNTER] ?: 0) + 1
        attr[COMBAT_TICK_COUNTER] = counter

        val tiles = attr[CORRUPTION_TILES] ?: mutableListOf()

        // Spawn new corruption tiles every CORRUPTION_SPAWN_INTERVAL ticks
        if (counter % CORRUPTION_SPAWN_INTERVAL == 0) {
            repeat(world.random(3..5)) {
                val t = Tile(tile.x + world.random(-3..3), tile.z + world.random(-3..3), tile.height)
                tiles.add(Pair(t, counter))
                // Visual: purple spotanim on the tile
                try {
                    world.spawn(TileGraphic(tile = t, id = "spotanims.cerberus_special_attack_flame", height = 0, delay = 0))
                } catch (_: Exception) {
                    // If the spotanim RSCM doesn't resolve, skip visuals
                }
            }
        }

        // Damage players standing on corruption tiles
        tiles.forEach { (t, _) ->
            world.players.forEach { player ->
                if (player.tile.sameAs(t) && player.getCurrentHp() > 0 && player.tile.regionId == BOSS_REGION) {
                    player.hit(world.random(10..15), type = HitType.HIT, delay = 0)
                }
            }
        }

        // Remove expired tiles (older than CORRUPTION_TILE_LIFETIME ticks)
        tiles.removeAll { (_, spawnTick) -> counter - spawnTick >= CORRUPTION_TILE_LIFETIME }
        attr[CORRUPTION_TILES] = tiles
    }

    // ---------------------------------------------------------------
    // Shadow Burst (Phase 3)
    // ---------------------------------------------------------------

    /**
     * Shadow burst: AoE attack hitting all players in the boss region.
     * Damage is fully blocked by Protect from Magic.
     */
    private fun Npc.shadowBurst() {
        animate("sequences.dragon_head_attack")
        world.players.forEach { player ->
            if (player.tile.regionId == BOSS_REGION && player.getCurrentHp() > 0) {
                // Protect from Magic blocks shadow burst entirely
                if (player.hasPrayerIcon(PrayerIcon.PROTECT_FROM_MAGIC)) {
                    player.hit(damage = 0, type = HitType.BLOCK, delay = 1)
                } else {
                    val damage = world.random(20..30)
                    try {
                        player.graphic(id = "spotanims.cerberus_special_attack_flame", height = 92)
                    } catch (_: Exception) {
                        // Skip graphic if RSCM doesn't resolve
                    }
                    player.hit(damage, type = HitType.HIT, delay = 1)
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // Minion spawning (Phase 3)
    // ---------------------------------------------------------------

    /**
     * Spawn 2 corrupted dragon minions near the boss. Only happens once
     * per Phase 3 transition.
     */
    private fun Npc.spawnMinions() {
        if (attr[MINIONS_SPAWNED] == true) return
        attr[MINIONS_SPAWNED] = true

        repeat(2) { i ->
            val offset = if (i == 0) -2 else 2
            val minionTile = Tile(tile.x + offset, tile.z + offset, tile.height)
            try {
                val minionId = "npcs.baby_black_dragon".asRSCM()
                val minion = Npc(minionId, minionTile, world)
                minion.setActive(true)
                world.spawn(minion)

                // Despawn minions after 60 ticks (~36 seconds) if not killed
                world.queue {
                    wait(60)
                    if (minion.isSpawned()) {
                        world.remove(minion)
                    }
                }
            } catch (_: Exception) {
                // If baby_black_dragon RSCM doesn't resolve, skip minion spawn
                // TODO: find a valid small dragon NPC ID in gamevals
            }
        }
    }

    // ---------------------------------------------------------------
    // Phase 1 — Enhanced KBD attacks
    // ---------------------------------------------------------------

    private suspend fun Npc.phase1Attack(task: QueueTask, target: Pawn) {
        // 25% melee (if in range), 75% random breath
        if (this.world.chance(1, 4) && canAttackMelee(task, target, moveIfNeeded = false)) {
            meleeAttack(target)
        } else {
            when (this.world.random(3)) {
                0 -> fireAttack(target)
                1 -> poisonAttack(target)
                2 -> freezeAttack(target)
                3 -> shockAttack(target)
            }
        }
    }

    // ---------------------------------------------------------------
    // Phase 2 — Corruption Awakens
    // ---------------------------------------------------------------

    /**
     * Phase 2: All Phase 1 attacks continue, but fire attack is replaced
     * with splash fire AoE (33% chance), and corruption tiles tick in
     * the combat loop.
     */
    private suspend fun Npc.phase2Attack(task: QueueTask, target: Pawn) {
        // 25% melee, 25% splash fire AoE, 50% other breath attacks
        if (this.world.chance(1, 4) && canAttackMelee(task, target, moveIfNeeded = false)) {
            meleeAttack(target)
        } else {
            when (this.world.random(3)) {
                0 -> splashFireAttack(target)
                1 -> poisonAttack(target)
                2 -> freezeAttack(target)
                3 -> shockAttack(target)
            }
        }
    }

    // ---------------------------------------------------------------
    // Phase 3 — Enrage Mode
    // ---------------------------------------------------------------

    /**
     * Phase 3: All Phase 2 mechanics + shadow burst + faster attacks.
     * Attack speed is handled in the combat loop (timer override).
     * Shadow burst is also handled in the combat loop.
     */
    private suspend fun Npc.phase3Attack(task: QueueTask, target: Pawn) {
        // Same attack selection as Phase 2
        phase2Attack(task, target)
    }

    // ---------------------------------------------------------------
    // Individual attack implementations
    // ---------------------------------------------------------------

    private fun Npc.meleeAttack(target: Pawn) {
        if (this.world.chance(1, 2)) {
            // Headbutt
            prepareAttack(CombatClass.MELEE, CombatStyle.STAB, AttackStyle.ACCURATE)
            animate("sequences.dragon_head_attack")
        } else {
            // Claw
            prepareAttack(CombatClass.MELEE, CombatStyle.SLASH, AttackStyle.AGGRESSIVE)
            animate("sequences.dragon_attack")
        }
        if (MeleeCombatFormula.getAccuracy(this, target) >= this.world.randomDouble()) {
            target.hit(this.world.random(38), type = HitType.HIT, delay = 1)
        } else {
            target.hit(damage = 0, type = HitType.BLOCK, delay = 1)
        }
    }

    private fun Npc.fireAttack(target: Pawn) {
        val projectile = createProjectile(target, gfx = 393, startHeight = 43, endHeight = 31, delay = 51, angle = 15, steepness = 127)
        prepareAttack(CombatClass.MAGIC, CombatStyle.MAGIC, AttackStyle.ACCURATE)
        animate("sequences.dragon_firebreath_all_attack")
        world.spawn(projectile)
        dealHit(
            target = target,
            formula = DragonfireFormula(maxHit = 65),
            delay = RangedCombatStrategy.getHitDelay(getFrontFacingTile(target), target.getCentreTile()) - 1,
        )
    }

    /**
     * Splash dragonfire — 3x3 AoE version of fire attack.
     * Primary target takes full dragonfire damage. Other players within
     * 1 tile of the target take 50% damage (random 1-32).
     */
    private fun Npc.splashFireAttack(target: Pawn) {
        val projectile = createProjectile(target, gfx = 393, startHeight = 43, endHeight = 31, delay = 51, angle = 15, steepness = 127)
        prepareAttack(CombatClass.MAGIC, CombatStyle.MAGIC, AttackStyle.ACCURATE)
        animate("sequences.dragon_firebreath_all_attack")
        world.spawn(projectile)

        // Primary target: full dragonfire damage
        val hitDelay = RangedCombatStrategy.getHitDelay(getFrontFacingTile(target), target.getCentreTile()) - 1
        dealHit(
            target = target,
            formula = DragonfireFormula(maxHit = 65),
            delay = hitDelay,
        )

        // Splash damage: other players within 1 tile of target take 50% damage
        val targetTile = target.tile
        world.players.forEach { p ->
            if (p !== target &&
                p.getCurrentHp() > 0 &&
                p.tile.regionId == BOSS_REGION &&
                Math.abs(p.tile.x - targetTile.x) <= 1 &&
                Math.abs(p.tile.z - targetTile.z) <= 1 &&
                p.tile.height == targetTile.height
            ) {
                p.hit(world.random(1..32), type = HitType.HIT, delay = hitDelay)
            }
        }
    }

    /**
     * Poison breath — same as KBD but ALWAYS poisons on a landed hit
     * (the original KBD has a 1/6 chance). Initial poison damage = 8.
     */
    private fun Npc.poisonAttack(target: Pawn) {
        val projectile = createProjectile(target, gfx = 394, startHeight = 43, endHeight = 31, delay = 51, angle = 15, steepness = 127)
        prepareAttack(CombatClass.MAGIC, CombatStyle.MAGIC, AttackStyle.ACCURATE)
        animate("sequences.dragon_firebreath_left_attack")
        this.world.spawn(projectile)
        val hit = dealHit(
            target = target,
            formula = DragonfireFormula(maxHit = 65, minHit = 10),
            delay = RangedCombatStrategy.getHitDelay(getFrontFacingTile(target), target.getCentreTile()) - 1,
        ) {
            if (it.landed()) {
                // Always poison — no 1/6 chance like the regular KBD
                target.poison(initialDamage = 8) {
                    if (target is Player) {
                        target.message("You have been poisoned.")
                    }
                }
            }
        }
        if (hit.blocked()) {
            target.graphic(id = "spotanims.failedspell_impact", height = 124, delay = hit.getClientHitDelay())
        }
    }

    private fun Npc.freezeAttack(target: Pawn) {
        val projectile = createProjectile(target, gfx = 395, startHeight = 43, endHeight = 31, delay = 51, angle = 15, steepness = 127)
        prepareAttack(CombatClass.MAGIC, CombatStyle.MAGIC, AttackStyle.ACCURATE)
        animate("sequences.dragon_firebreath_right_attack")
        this.world.spawn(projectile)
        val hit = dealHit(
            target = target,
            formula = DragonfireFormula(maxHit = 65, minHit = 10),
            delay = RangedCombatStrategy.getHitDelay(getFrontFacingTile(target), target.getCentreTile()) - 1,
        ) {
            if (it.landed() && this.world.chance(1, 6)) {
                target.freeze(cycles = 6) {
                    if (target is Player) {
                        target.message("You have been frozen.")
                    }
                }
            }
        }
        if (hit.blocked()) {
            target.graphic(id = "spotanims.failedspell_impact", height = 124, delay = hit.getClientHitDelay())
        }
    }

    private fun Npc.shockAttack(target: Pawn) {
        val projectile = createProjectile(target, gfx = 396, startHeight = 43, endHeight = 31, delay = 51, angle = 15, steepness = 127)
        prepareAttack(CombatClass.MAGIC, CombatStyle.MAGIC, AttackStyle.ACCURATE)
        animate("sequences.dragon_firebreath_middle_attack")
        this.world.spawn(projectile)
        val hit = dealHit(
            target = target,
            formula = DragonfireFormula(maxHit = 65, minHit = 12),
            delay = RangedCombatStrategy.getHitDelay(getFrontFacingTile(target), target.getCentreTile()) - 1,
        ) {
            if (it.landed() && this.world.chance(1, 6)) {
                if (target is Player) {
                    arrayOf(Skills.ATTACK, Skills.STRENGTH, Skills.DEFENCE, Skills.MAGIC, Skills.RANGED).forEach { skill ->
                        target.getSkills().alterCurrentLevel(skill, -2)
                    }
                    target.message("You're shocked and weakened!")
                }
            }
        }
        if (hit.blocked()) {
            target.graphic(id = "spotanims.failedspell_impact", height = 124, delay = hit.getClientHitDelay())
        }
    }
}
