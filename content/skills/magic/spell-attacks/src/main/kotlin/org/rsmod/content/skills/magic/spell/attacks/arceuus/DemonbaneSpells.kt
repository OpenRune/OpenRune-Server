package org.rsmod.content.skills.magic.spell.attacks.arceuus

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.spells.attack.SpellAttack
import org.rsmod.api.spells.attack.SpellAttackManager
import org.rsmod.api.spells.attack.SpellAttackMap
import org.rsmod.api.spells.attack.SpellAttackRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.proj.ProjAnim

internal const val DEMONBANE_CAST_ANIM = "seq.human_spellcast_demonbane"

class DemonbaneSpells : SpellAttackMap {
    override fun SpellAttackRepository.register(manager: SpellAttackManager) {
        for (spell in DemonbaneSpell.entries) {
            register(spell.obj, DemonbaneSpellAttack(manager, spell))
        }
    }

    internal enum class DemonbaneSpell(
        val obj: String,
        val maxHit: Int,
        val cast: String,
        val impact: String,
    ) {
        Inferior(
            obj = "obj.br_mithril_scimitar",
            maxHit = 16,
            cast = "spotanim.inferior_demonbane_cast_spotanim",
            impact = "spotanim.inferior_demonbane_hit_spotanim",
        ),
        Superior(
            obj = "obj.br_willow_bow",
            maxHit = 23,
            cast = "spotanim.superior_demonbane_cast_spotanim",
            impact = "spotanim.superior_demonbane_hit_spotanim",
        ),
        Dark(
            obj = "obj.br_adamant_scimitar",
            maxHit = 30,
            cast = "spotanim.dark_demonbane_cast_spotanim",
            impact = "spotanim.dark_demonbane_hit_spotanim",
        ),
    }

    private class DemonbaneSpellAttack(
        private val manager: SpellAttackManager,
        private val spell: DemonbaneSpell,
    ) : SpellAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Spell) {
            if (target.visType.param(params.demon) == 0) {
                rejectTarget()
                return
            }

            val castResult = manager.attemptCast(this, attack)
            if (castResult.isFailure()) {
                return
            }

            player.anim(DEMONBANE_CAST_ANIM, priority = 6)
            spotanim(spell.cast)

            val proj = ProjAnim.fromBoundsToNpc(player.bounds(), target, -1, "projanim.magic_spell")
            val (serverDelay, clientDelay) = proj.durations

            if (manager.rollSplash(this, target, attack, castResult)) {
                manager.playSplashFx(this, target, clientDelay, castSound = null, soundRadius = 8)
                manager.queueSplashHit(this, target, attack.spell.obj, clientDelay, serverDelay)
                manager.continueCombatIfAutocast(this, target)
                return
            }

            val damage = manager.rollMaxHit(this, target, attack, castResult, spell.maxHit)
            manager.playHitFx(
                source = this,
                target = target,
                clientDelay = clientDelay,
                castSound = null,
                soundRadius = 8,
                hitSpot = spell.impact,
                hitSpotHeight = 0,
                hitSound = null,
            )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMagicHit(this, target, attack.spell.obj, damage, clientDelay, serverDelay)
            manager.continueCombatIfAutocast(this, target)
        }

        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Spell) {
            rejectTarget()
        }

        private fun ProtectedAccess.rejectTarget() {
            manager.stopCombat(this)
            mes("This spell only affects demons.")
        }
    }
}
