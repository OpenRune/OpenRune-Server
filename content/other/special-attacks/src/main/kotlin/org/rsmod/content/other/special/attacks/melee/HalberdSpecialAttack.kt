package org.rsmod.content.other.special.attacks.melee

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.map.CardinalDirection
import org.rsmod.game.map.Direction
import org.rsmod.game.map.translate
import org.rsmod.map.CoordGrid

class HalberdSpecialAttack
@Inject
constructor(private val worldRepo: WorldRepository, private val npcList: NpcList) :
    SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val dragon = Sweep(manager, worldRepo, npcList, "red")
        registerMelee("obj.dragon_halberd", dragon)
        registerMelee("obj.bh_dragon_halberd_corrupted", dragon)

        val crystal = Sweep(manager, worldRepo, npcList, "white")
        for (obj in CRYSTAL_HALBERDS) {
            registerMelee(obj, crystal)
        }
    }

    private class Sweep(
        private val manager: SpecialAttackManager,
        private val worldRepo: WorldRepository,
        private val npcList: NpcList,
        private val gfxColour: String,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            val anchor = playSweep(target)
            val totalDamage = hitTarget(target, attack)
            manager.giveCombatXp(this, target, attack, totalDamage)
            if (target.size == 1) {
                hitNeighbours(target, anchor, attack)
            }
            manager.continueCombat(this, target)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            playSweep(target)
            val totalDamage = hitTarget(target, attack)
            manager.giveCombatXp(this, target, attack, totalDamage)
            manager.continueCombat(this, target)
            return true
        }

        private fun ProtectedAccess.hitTarget(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Int {
            var totalDamage = roll(target, attack, accuracy = 1.0)
            if (target.size > 1) {
                totalDamage += roll(target, attack, accuracy = 0.75)
            }
            return totalDamage
        }

        private fun ProtectedAccess.roll(
            target: PathingEntity,
            attack: CombatAttack.Melee,
            accuracy: Double,
        ): Int {
            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = accuracy,
                    maxHitMultiplier = 1.1,
                    blockType = MeleeAttackType.Slash,
                )
            manager.queueMeleeHit(this, target, damage)
            return damage
        }

        private fun ProtectedAccess.hitNeighbours(
            target: Npc,
            anchor: CoordGrid,
            attack: CombatAttack.Melee,
        ) {
            if (!mapMultiway()) {
                return
            }
            for (npc in npcList) {
                if (npc === target || npc.size != 1 || !npc.isValidTarget()) {
                    continue
                }
                if (!npc.visType.hasOp(InteractionOp.Op2.slot)) {
                    continue
                }
                if (npc.coords.level != anchor.level || npc.coords.chebyshevDistance(anchor) > 1) {
                    continue
                }
                val damage = roll(npc, attack, accuracy = 1.0)
                manager.giveCombatXp(this, npc, attack, damage)
            }
        }

        private fun ProtectedAccess.playSweep(target: PathingEntity): CoordGrid {
            anim("seq.dragon_halberd_special_attack")
            val direction = Direction.cardinalBetween(player.bounds(), target.bounds())
            val anchor =
                if (target.size == 1) {
                    target.coords
                } else {
                    val centre = player.coords.translate(player.size / 2, player.size / 2)
                    centre.translate(direction.toDirection())
                }
            spotanimMap(
                repo = worldRepo,
                internal = "spotanim.dragon_halberd_special_${direction.suffix()}_$gfxColour",
                coord = anchor,
                height = 96,
            )
            return anchor
        }
    }

    private companion object {
        val CRYSTAL_HALBERDS = listOf("obj.crystal_halberd", "obj.crystal_halberd_2500")
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
