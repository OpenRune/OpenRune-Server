package org.rsmod.content.skills.prayer.items.ashsanctifier

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.death.NpcDeathDropContext
import org.rsmod.api.death.NpcDeathDropHook
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.table.prayer.SkillPrayerRow
import org.rsmod.content.skills.prayer.items.ashsanctifier.AshSanctifierScript.Companion.ashSanctifierActivityEnabled
import org.rsmod.content.skills.prayer.items.ashsanctifier.AshSanctifierScript.Companion.hasKourendKebosEliteDiaryComplete
import org.rsmod.content.skills.prayer.items.ashsanctifier.AshSanctifierScript.Companion.hasKourendKebosHardDiaryComplete
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType

@Singleton
class AshSanctifierNpcDropHook @Inject constructor() : NpcDeathDropHook {

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
        if (player.ashSanctifierCharges <= 0) {
            return false
        }

        player.ashSanctifierCharges -= 1

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
                ashSanctifierCharges > 0
            ) {
                return slot
            }
        }
        return null
    }
}

private var Player.ashSanctifierCharges by intVarBit("varbit.charges_ash_sanctifier_quantity")
