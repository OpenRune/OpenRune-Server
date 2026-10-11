package org.rsmod.content.slayer.requirements

import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.script.onEvent
import org.rsmod.api.table.slayer.SlayerMonstersRow
import org.rsmod.api.table.slayer.SlayerWeaponRulesRow
import org.rsmod.game.hit.HitType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object SlayerWeaponRules {
    fun damages(
        rule: SlayerWeaponRulesRow,
        type: HitType,
        weaponId: Int?,
        secondaryId: Int?,
    ): Boolean =
        when (type) {
            HitType.Melee -> rule.weapons.any { it.id == weaponId }
            HitType.Ranged -> rule.ammo.any { it.id == secondaryId }
            HitType.Magic -> rule.spells.any { it.id == secondaryId }
            HitType.Typeless -> true
        }
}

class SlayerWeaponScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (monster in SlayerMonstersRow.all()) {
            val rule = monster.weaponRule ?: continue
            for (npc in monster.npcs) {
                onEvent<NpcHitEvents.Modify>(npc.id) { limitToRule(rule) }
            }
        }
    }

    private fun NpcHitEvents.Modify.limitToRule(rule: SlayerWeaponRulesRow) {
        if (!hit.isFromPlayer) {
            return
        }
        val weapon = hit.righthandType()?.id
        val secondary = hit.secondaryType()?.id
        if (!SlayerWeaponRules.damages(rule, hit.type, weapon, secondary)) {
            hit.damage = 0
        }
    }
}
