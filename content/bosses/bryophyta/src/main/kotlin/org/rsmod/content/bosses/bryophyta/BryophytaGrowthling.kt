package org.rsmod.content.bosses.bryophyta

import dev.openrune.types.ItemServerType
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.CombatStance
import org.rsmod.api.combat.commons.styles.MeleeAttackStyle
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.manager.PlayerAttackManager
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.combat.weapon.types.AttackTypes
import org.rsmod.api.npc.queueDeath
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.righthand
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

internal object BryophytaGrowthling {
    public const val NPC: String = "npc.gb_growthling"
    public const val PRUNE_MESSAGE: String = "You prune the Growthling."
    public const val PRUNE_ANIM: String = "seq.human_dhsword_stab"
    public const val PRUNE_SOUND: String = "synth.warhammer_crush"

    private const val SECATEURS = "obj.secateurs"
    private const val MAGIC_SECATEURS = "obj.fairy_enchanted_secateurs"
    private const val BLESSED_AXE = "obj.blessed_axe"
    private const val ZOMBIE_AXE = "obj.zombie_axe"
    private const val WOODCUTTING_AXE = "content.woodcutting_axe"

    public fun isAxe(type: ItemServerType): Boolean {
        if (type.internalName == BLESSED_AXE || type.internalName == ZOMBIE_AXE) return false
        return type.isContentType(WOODCUTTING_AXE)
    }

    public fun isAxeOrSecateurs(type: ItemServerType?): Boolean {
        if (type == null) return false
        return isAxe(type) ||
            type.internalName == SECATEURS ||
            type.internalName == MAGIC_SECATEURS
    }

    public fun isOneClick(type: ItemServerType): Boolean =
        isAxe(type) || type.internalName == MAGIC_SECATEURS

    public fun prune(
        player: Player,
        npc: Npc,
        awardXp: Boolean,
        attackStyles: AttackStyles,
        attackTypes: AttackTypes,
        attackManager: PlayerAttackManager,
    ) {
        player.anim(PRUNE_ANIM)
        player.soundSynth(PRUNE_SOUND)
        if (awardXp) {
            val attack = meleeAttack(player, attackStyles, attackTypes)
            val xpDamage = npc.baseHitpointsLvl
            npc.hitpoints = xpDamage
            attackManager.giveCombatXp(player, npc, attack, xpDamage)
        }
        npc.hitpoints = 0
        npc.queueDeath()
    }

    private fun meleeAttack(
        player: Player,
        attackStyles: AttackStyles,
        attackTypes: AttackTypes,
    ): CombatAttack.Melee =
        CombatAttack.Melee(
            weapon = player.righthand,
            type = MeleeAttackType.from(attackTypes.get(player)),
            style = MeleeAttackStyle.from(attackStyles.get(player)),
            stance = CombatStance[player.vars["varp.com_mode"]] ?: CombatStance.Stance1,
        )
}
