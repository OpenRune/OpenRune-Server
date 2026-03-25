package org.alter.plugins.content.npcs.bosses

import org.alter.api.*
import org.alter.api.cfg.*
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
import org.alter.game.plugin.*
import org.alter.plugins.content.combat.*
import org.alter.plugins.content.combat.formula.DragonfireFormula
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.strategy.RangedCombatStrategy
import org.alter.plugins.content.mechanics.poison.poison

/**
 * Corrupted King Black Dragon — a 3-phase boss encounter.
 *
 * Phase 1 (HP > 50%): Enhanced KBD attacks — stronger melee, guaranteed poison.
 * Phase 2 (HP 50%-20%): TODO — corruption tiles, splash dragonfire.
 * Phase 3 (HP < 20%): TODO — shadow burst, minions, attack speed increase.
 */
class CorruptedKbdPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    companion object {
        private val PHASE_ATTR = AttributeKey<Int>()

        private const val PHASE_1 = 1
        private const val PHASE_2 = 2
        private const val PHASE_3 = 3
    }

    init {
        setMultiCombatRegion(region = 9033)

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
                // Phase 1 -> 2 transition
                forceChat("The dragon's corruption intensifies!")
                animate("sequences.dragon_head_attack") // howl animation
            }
            PHASE_3 -> {
                // Phase 2 -> 3 transition
                forceChat("The dragon enters a frenzy!")
                animate("sequences.dragon_head_attack") // howl animation
                // TODO Phase 3: increase attack speed to 2
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

                when (getCurrentPhase()) {
                    PHASE_1 -> phase1Attack(task, target)
                    PHASE_2 -> phase2Attack(task, target)
                    PHASE_3 -> phase3Attack(task, target)
                }

                postAttackLogic(target)
            }
            task.wait(1)
            target = getCombatTarget() ?: break
        }

        resetFacePawn()
        removeCombatTarget()
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
    // Phase 2 — Stub
    // ---------------------------------------------------------------

    /**
     * TODO Phase 2 attacks:
     * - Corruption tiles: spawn damaging ground tiles around target
     * - Splash dragonfire: AoE dragonfire hitting all players in range
     * - Retains all Phase 1 attacks with increased accuracy
     */
    private suspend fun Npc.phase2Attack(task: QueueTask, target: Pawn) {
        // For now, fall back to Phase 1 attacks
        phase1Attack(task, target)
    }

    // ---------------------------------------------------------------
    // Phase 3 — Stub
    // ---------------------------------------------------------------

    /**
     * TODO Phase 3 attacks:
     * - Shadow burst: large AoE damage around the boss
     * - Minion summon: spawn corrupted dragon minions
     * - Attack speed increases to 2 ticks
     * - Retains all Phase 1 + Phase 2 attacks
     */
    private suspend fun Npc.phase3Attack(task: QueueTask, target: Pawn) {
        // For now, fall back to Phase 1 attacks
        phase1Attack(task, target)
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
