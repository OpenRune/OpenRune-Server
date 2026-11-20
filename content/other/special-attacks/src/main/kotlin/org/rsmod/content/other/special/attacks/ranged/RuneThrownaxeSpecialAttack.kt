package org.rsmod.content.other.special.attacks.ranged

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.config.constants
import org.rsmod.api.npc.queueDeath
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.RangedSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

class RuneThrownaxeSpecialAttack @Inject constructor(private val npcRepo: NpcRepository) :
    SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerRanged("obj.rune_thrownaxe", RuneThrownaxe(manager, npcRepo))
    }

    private class RuneThrownaxe(
        private val manager: SpecialAttackManager,
        private val npcRepo: NpcRepository,
    ) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean {
            if (target.visType.id in WEAK_DOOR_IDS) {
                knockDownSupports(target)
                return true
            }
            return shoot(target, attack)
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean = shoot(target, attack)

        private fun ProtectedAccess.shoot(
            target: org.rsmod.game.entity.PathingEntity,
            attack: CombatAttack.Ranged,
        ): Boolean {
            anim("seq.human_throw")
            spotanim(
                spot = "spotanim.rune_taxe_launch",
                slot = constants.spotanim_slot_combat,
                height = 96,
            )
            val proj = manager.spawnProjectile(this, target, "spotanim.rune_taxe_travel", "projanim.thrown")
            val damage = manager.rollRangedDamage(this, target, attack)
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueRangedHit(
                this,
                target,
                null,
                damage,
                proj.clientCycles,
                proj.serverCycles,
            )
            manager.continueCombat(this, target)
            return true
        }

        private suspend fun ProtectedAccess.knockDownSupports(target: Npc) {
            anim("seq.human_throw")
            spotanim(
                spot = "spotanim.rune_taxe_launch",
                slot = constants.spotanim_slot_combat,
                height = 96,
            )
            val props =
                npcRepo
                    .findAll(ZoneKey.from(CENTRE), zoneRadius = 1)
                    .filter { it.visType.id in WEAK_DOOR_IDS }
                    .toList()
            for (prop in props) {
                manager.spawnProjectile(this, prop, "spotanim.rune_taxe_travel", "projanim.thrown")
            }
            if (props.isEmpty()) {
                manager.spawnProjectile(this, target, "spotanim.rune_taxe_travel", "projanim.thrown")
            }
            delay(2)
            for (prop in props) {
                prop.hitpoints = 0
                prop.queueDeath()
            }
            mes("The rune thrownaxe ricochets between the door supports, knocking them all down!")
            manager.stopCombat(this)
        }

        private companion object {
            val CENTRE = CoordGrid(2545, 10143, 0)
            val WEAK_DOOR_IDS =
                setOf(
                    "npc.dagannoth_weak_door_north".asRSCM(RSCMType.NPC),
                    "npc.dagannoth_weak_door_south".asRSCM(RSCMType.NPC),
                    "npc.dagannoth_weak_door_west".asRSCM(RSCMType.NPC),
                    "npc.dagannoth_weak_door_falling_north".asRSCM(RSCMType.NPC),
                    "npc.dagannoth_weak_door_falling_south".asRSCM(RSCMType.NPC),
                    "npc.dagannoth_weak_door_falling_west".asRSCM(RSCMType.NPC),
                    "npc.dagannoth_weak_door_fallen_north".asRSCM(RSCMType.NPC),
                    "npc.dagannoth_weak_door_fallen_south".asRSCM(RSCMType.NPC),
                    "npc.dagannoth_weak_door_fallen_west".asRSCM(RSCMType.NPC),
                )
        }
    }
}
