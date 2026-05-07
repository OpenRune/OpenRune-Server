package org.rsmod.content.skills.prayer.items.ashsanctifier

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.death.NpcDeathDropContext
import org.rsmod.api.death.NpcDeathDropHook
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.obj.charges.ObjChargeManager.Companion.isFailure
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.table.prayer.SkillPrayerRow
import org.rsmod.content.skills.prayer.items.ashsanctifier.AshSanctifierScript.Companion.ashSanctifierActivityEnabled
import org.rsmod.content.skills.prayer.items.ashsanctifier.AshSanctifierScript.Companion.hasKourendKebosEliteDiaryComplete
import org.rsmod.content.skills.prayer.items.ashsanctifier.AshSanctifierScript.Companion.hasKourendKebosHardDiaryComplete
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType

@Singleton
class AshSanctifierNpcDropHook @Inject constructor(private val charges: ObjChargeManager) : NpcDeathDropHook {

    private val demonicAshXpByItem: Map<String, SkillPrayerRow> by lazy {
        SkillPrayerRow.all().filter { it.ashes }.associateBy { it.item.internalName }
    }

    override fun tryConsume(context: NpcDeathDropContext): Boolean {
        val player = context.hero

        if (!player.hasKourendKebosHardDiaryComplete()) {
            return false
        }
        if (!player.ashSanctifierActivityEnabled) {
            return false
        }
        if (context.dropType.isCert) {
            return false
        }

        val internal = context.dropType.internalName
        val row = demonicAshXpByItem[internal] ?: return false

        val slot = player.findAshSanctifierSlot() ?: return false
        if (charges.getCharges(player.inv[slot], "varbit.charges_ash_sanctifier_quantity") <= 0) {
            return false
        }

        val result = charges.reduceChargesSameItem(
            inventory = player.inv,
            slot = slot,
            remove = 1,
            internal = "varbit.charges_ash_sanctifier_quantity"
        )
        if (result.isFailure()) {
            return false
        }

        val scatterXp = row.exp.toDouble()
        val prayerXp = if (player.hasKourendKebosEliteDiaryComplete()) {
            scatterXp
        } else {
            scatterXp / 2.0
        }
        player.statAdvance("stat.prayer", prayerXp)

        return true
    }

    private fun Player.findAshSanctifierSlot(): Int? {
        for (slot in inv.indices) {
            val obj = inv[slot] ?: continue
            if (obj.isType("obj.ash_sanctifier") &&
                charges.getCharges(obj, "varbit.charges_ash_sanctifier_quantity") > 0
            ) {
                return slot
            }
        }
        return null
    }
}
