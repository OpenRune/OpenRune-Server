package org.alter.plugins.content.combat.strategy

import org.alter.api.ProjectileType
import org.alter.api.Skills
import org.alter.api.ext.getVarbit
import org.alter.api.ext.playSound
import org.alter.game.model.Graphic
import org.alter.game.model.Tile
import org.alter.game.model.combat.XpMode
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.plugins.content.combat.Combat
import org.alter.plugins.content.combat.CombatConfigs
import org.alter.plugins.content.combat.createProjectile
import org.alter.plugins.content.combat.dealHit
import org.alter.plugins.content.combat.formula.MagicCombatFormula
import org.alter.plugins.content.magic.MagicSpells

// Legacy reflection helpers for accessing CombatSpell properties without importing it.
// CombatSpell lives in content; game-plugins cannot import it directly.
private fun spellInt(spell: Any, getter: String): Int =
    spell.javaClass.getMethod(getter).invoke(spell) as Int

private fun spellString(spell: Any, getter: String): String =
    spell.javaClass.getMethod(getter).invoke(spell) as String

private fun spellDouble(spell: Any, getter: String): Double =
    spell.javaClass.getMethod(getter).invoke(spell) as Double

private fun spellGraphic(spell: Any, getter: String): Graphic? =
    spell.javaClass.getMethod(getter).invoke(spell) as? Graphic

private fun spellId(spell: Any): Int = spellInt(spell, "getId")
private fun spellProjectile(spell: Any): Int = spellInt(spell, "getProjectile")
private fun spellProjectileEndHeight(spell: Any): Int = spellInt(spell, "getProjectilEndHeight")
private fun spellCastAnimation(spell: Any): String = spellString(spell, "getCastAnimation")
private fun spellCastSound(spell: Any): Int = spellInt(spell, "getCastSound")
private fun spellCastGfx(spell: Any): Graphic? = spellGraphic(spell, "getCastGfx")
private fun spellImpactGfx(spell: Any): Graphic? = spellGraphic(spell, "getImpactGfx")
private fun spellBaseXp(spell: Any): Double = spellDouble(spell, "getBaseXp")

/**
 * @author Tom <rspsmods@gmail.com>
 */
object MagicCombatStrategy : CombatStrategy {
    override fun getAttackRange(pawn: Pawn): Int = 10

    override fun canAttack(
        pawn: Pawn,
        target: Pawn,
    ): Boolean {
        if (pawn is Player) {
            val spell = pawn.attr[Combat.CASTING_SPELL]!!
            val requirements = MagicSpells.getMetadata(spellId(spell))
            if (requirements != null && !MagicSpells.canCast(pawn, requirements.lvl, requirements.items, requirements.spellbook)) {
                return false
            }
        }
        return true
    }

    override fun attack(
        pawn: Pawn,
        target: Pawn,
    ) {
        val world = pawn.world

        val spell = pawn.attr[Combat.CASTING_SPELL]!!
        val projectile =
            pawn.createProjectile(
                target,
                gfx = spellProjectile(spell),
                type = ProjectileType.MAGIC,
                endHeight = spellProjectileEndHeight(spell),
            )

        pawn.animate(spellCastAnimation(spell))
        spellCastGfx(spell)?.let { gfx -> pawn.graphic(gfx) }
        spellImpactGfx(spell)?.let { gfx -> target.graphic(Graphic(gfx.id, gfx.height, projectile.lifespan)) }
        if (spellProjectile(spell) > 0) {
            world.spawn(projectile)
        }

        if (pawn is Player) {
            if (spellCastSound(spell) != -1) {
                pawn.playSound(id = spellCastSound(spell), volume = 1, delay = 0)
            }
            MagicSpells.getMetadata(spellId(spell))?.let { requirement -> MagicSpells.removeRunes(pawn, requirement.items) }
        }

        val formula = MagicCombatFormula
        val accuracy = formula.getAccuracy(pawn, target)
        val maxHit = formula.getMaxHit(pawn, target)
        val landHit = accuracy >= world.randomDouble()

        val hitDelay = getHitDelay(pawn.getCentreTile(), target.getCentreTile())
        val damage = pawn.dealHit(target = target, maxHit = maxHit, landHit = landHit, delay = hitDelay).hit.hitmarks.sumOf { it.damage }

        if (damage >= 0 && pawn.entityType.isPlayer) {
            addCombatXp(pawn as Player, target, damage, spell)
        }
    }

    fun getHitDelay(
        start: Tile,
        target: Tile,
    ): Int {
        val distance = start.getDistance(target)
        return 2 + Math.floor((1.0 + distance) / 3.0).toInt()
    }

    private fun addCombatXp(
        player: Player,
        target: Pawn,
        damage: Int,
        spell: Any,
    ) {
        val modDamage = if (target.entityType.isNpc) Math.min(target.getCurrentHp(), damage) else damage
        val mode = CombatConfigs.getXpMode(player)
        val multiplier = if (target is Npc) Combat.getNpcXpMultiplier(target) else 1.0
        val baseXp = spellBaseXp(spell)

        if (mode == XpMode.MAGIC) {
            val defensive =
                player.getVarbit(
                    Combat.SELECTED_AUTOCAST_VARBIT,
                ) != 0 && player.getVarbit(Combat.DEFENSIVE_MAGIC_CAST_VARBIT) != 0
            if (!defensive) {
                player.addXp(Skills.MAGIC, (modDamage * 2.0 * multiplier) + baseXp)
                player.addXp(Skills.HITPOINTS, modDamage * 1.33 * multiplier)
            } else {
                player.addXp(Skills.MAGIC, (modDamage * 1.33 * multiplier) + baseXp)
                player.addXp(Skills.DEFENCE, modDamage * multiplier)
                player.addXp(Skills.HITPOINTS, modDamage * 1.33 * multiplier)
            }
        } else if (mode == XpMode.SHARED) {
            player.addXp(Skills.MAGIC, (modDamage * 1.33 * multiplier) + baseXp)
            player.addXp(Skills.DEFENCE, modDamage * multiplier)
            player.addXp(Skills.HITPOINTS, modDamage * 1.33 * multiplier)
        } else {
            player.addXp(Skills.MAGIC, (modDamage * 2.0 * multiplier) + baseXp)
            player.addXp(Skills.HITPOINTS, modDamage * 1.33 * multiplier)
        }
    }
}
