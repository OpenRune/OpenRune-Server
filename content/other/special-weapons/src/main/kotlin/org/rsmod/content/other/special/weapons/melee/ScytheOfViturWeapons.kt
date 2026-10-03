package org.rsmod.content.other.special.weapons.melee

import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.weapons.MeleeWeapon
import org.rsmod.api.weapons.WeaponAttackManager
import org.rsmod.api.weapons.WeaponMap
import org.rsmod.api.weapons.WeaponRepository
import org.rsmod.content.other.special.weapons.scripts.charge.ScytheCharges
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.map.CardinalDirection
import org.rsmod.game.map.Direction
import org.rsmod.game.map.translate

class ScytheOfViturWeapons @Inject constructor(private val worldRepo: WorldRepository) :
    WeaponMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        for ((charged, empty) in ScytheCharges.variants) {
            register(charged, ScytheOfVitur(manager, worldRepo))
            register(empty, ScytheOfVitur(manager, worldRepo))
        }
    }

    private class ScytheOfVitur(
        private val manager: WeaponAttackManager,
        private val worldRepo: WorldRepository,
    ) : MeleeWeapon {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            val current = normalize(attack) ?: return true
            manager.playWeaponFx(this, current)
            playSpecGfx(target)

            val extraHits = (target.size - 1).coerceIn(0, 2)
            val multipliers = listOf(1.0, 0.5, 0.25).take(1 + extraHits)

            var totalDamage = 0
            for (multiplier in multipliers) {
                val damage =
                    manager.rollMeleeDamage(
                        source = this,
                        target = target,
                        attack = current,
                        accuracyMultiplier = 1.0,
                        maxHitMultiplier = multiplier,
                    )
                totalDamage += damage
                manager.queueMeleeHit(this, target, damage)
            }

            manager.giveCombatXp(this, target, current, totalDamage)
            consume(current, totalDamage)
            manager.continueCombat(this, target)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            val current = normalize(attack) ?: return true
            manager.playWeaponFx(this, current)
            playSpecGfx(target)
            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = current,
                    accuracyMultiplier = 1.0,
                    maxHitMultiplier = 1.0,
                )
            manager.giveCombatXp(this, target, current, damage)
            manager.queueMeleeHit(this, target, damage)
            consume(current, damage)
            manager.continueCombat(this, target)
            return true
        }

        private fun ProtectedAccess.normalize(attack: CombatAttack.Melee): CombatAttack.Melee? {
            val item = player.worn[Wearpos.RightHand.slot]
            if (item == null || item != attack.weapon || ScytheCharges.variant(item) == null) {
                manager.stopCombat(this)
                return null
            }
            val normalized = ScytheCharges.withCharges(item, ScytheCharges.count(item))
            player.worn[Wearpos.RightHand.slot] = normalized
            return attack.copy(weapon = normalized)
        }

        private fun ProtectedAccess.consume(attack: CombatAttack.Melee, damage: Int) {
            val item = player.worn[Wearpos.RightHand.slot] ?: return
            if (item != attack.weapon || damage <= 0) return
            val before = ScytheCharges.count(item)
            if (before == 0) return
            player.worn[Wearpos.RightHand.slot] = ScytheCharges.withCharges(item, before - 1)
            if (before == 1) mes("Your scythe has run out of charges.")
        }

        private fun ProtectedAccess.playSpecGfx(target: PathingEntity) {
            val direction = Direction.cardinalBetween(player.bounds(), target.bounds())
            val selfCentre = player.coords.translate(player.size / 2, player.size / 2)
            val anchor = selfCentre.translate(direction.toDirection())
            spotanimMap(
                repo = worldRepo,
                internal = "spotanim.dragon_halberd_special_${direction.suffix()}_red",
                coord = anchor,
                height = 96,
                delay = 20,
            )
        }
    }
}

private fun CardinalDirection.toDirection(): Direction =
    when (this) {
        CardinalDirection.North -> Direction.North
        CardinalDirection.South -> Direction.South
        CardinalDirection.East -> Direction.East
        CardinalDirection.West -> Direction.West
    }

private fun CardinalDirection.suffix(): String =
    when (this) {
        CardinalDirection.North -> "north"
        CardinalDirection.South -> "south"
        CardinalDirection.East -> "east"
        CardinalDirection.West -> "west"
    }
