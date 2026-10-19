package org.rsmod.content.slayer.requirements

import dev.openrune.ServerCacheManager
import dev.openrune.types.ItemServerType
import dev.openrune.types.enums.EnumTypeMap
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.script.onEvent
import org.rsmod.api.table.slayer.SlayerWeaponRulesRow
import org.rsmod.game.hit.HitType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object SlayerWeaponRules {
    private val ammo: Map<Int, Set<Int>> =
        SlayerWeaponRulesRow.all().associate { it.rowId to ids(it.ammo) }
    private val spells: Map<Int, Set<Int>> =
        SlayerWeaponRulesRow.all().associate { it.rowId to ids(it.spells) }

    fun damages(
        rule: SlayerWeaponRulesRow,
        type: HitType,
        weaponId: Int?,
        secondaryId: Int?,
    ): Boolean =
        when (type) {
            HitType.Melee -> weaponId != null && isRuleWeapon(rule, weaponId)
            HitType.Ranged -> secondaryId in ammo.getValue(rule.rowId)
            HitType.Magic -> secondaryId in spells.getValue(rule.rowId)
            HitType.Typeless -> true
        }

    private fun isRuleWeapon(rule: SlayerWeaponRulesRow, weaponId: Int): Boolean =
        ServerCacheManager.getItem(weaponId)?.contentGroup == rule.weapons

    private fun ids(items: EnumTypeMap<ItemServerType, Boolean>): Set<Int> =
        items.keys.mapTo(HashSet()) { it.id }
}

class SlayerWeaponScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (rule in SlayerWeaponRulesRow.all()) {
            for (npc in rule.npcs) {
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
