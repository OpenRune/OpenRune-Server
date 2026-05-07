package org.rsmod.content.skills.prayer.items.bonecrusher

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.events.prayer.PrayerSkillAction
import org.rsmod.api.player.events.skilling.SkillingActionCompleteEvent
import org.rsmod.api.player.events.skilling.SkillingActionContext
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.death.NpcDeathDropContext
import org.rsmod.api.death.NpcDeathDropHook
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.obj.charges.ObjChargeManager.Companion.isFailure
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statBoost
import org.rsmod.api.table.prayer.SkillPrayerRow
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType

@Singleton
public class BonecrusherNpcDropHook
@Inject
constructor(
    private val charges: ObjChargeManager,
    private val areaChecker: AreaChecker,
    private val eventBus: EventBus,
) : NpcDeathDropHook {
    private val boneXpByItem: Map<String, SkillPrayerRow> by lazy {
        SkillPrayerRow.all().filter { !it.ashes }.associateBy { it.item.internalName }
    }

    override fun tryConsume(context: NpcDeathDropContext): Boolean {
        val player = context.hero

        if (!player.isBonecrusherActivityEnabled()) {
            return false
        }
        if (context.dropType.isCert) {
            return false
        }

        val internal = context.dropType.internalName

        val row = boneXpByItem[internal] ?: return false

        val source = player.findBonecrusherChargeSource() ?: return false
        if (!decrementBonecrusherCharge(source)) {
            return false
        }

        val prayerXp = row.exp.toDouble() / 2
        player.statAdvance("stat.prayer", prayerXp)

        eventBus.publish(SkillingActionCompleteEvent(player = player, context =
            SkillingActionContext.Prayer(PrayerSkillAction.BonecrusherCrushComplete(
                boneItemInternal = internal,
                experienceGranted = prayerXp,
                catacombsBonePrayerRestore = row.prayerRestore
            ))),
        )

        applyPrayerRestoreFromCrush(player, row, source)

        return true
    }

    private fun applyPrayerRestoreFromCrush(
        player: Player,
        row: SkillPrayerRow,
        source: CrusherSource
    ) {
        val restore = row.prayerRestore
        if (restore <= 0) {
            return
        }

        if (areaChecker.inArea("area.catacombs_of_kourend", player.coords)) {
            return
        }

        if (player.isBonecrusherNecklacePrayerSource(source)) {
            player.statBoost("stat.prayer", restore, 0)
            return
        }
        if (player.isDragonboneNecklaceInvCrushPrayer(source)) {
            player.statBoost("stat.prayer", restore, 0)
        }
    }

    private fun Player.isBonecrusherNecklacePrayerSource(source: CrusherSource): Boolean =
        source.inventory === worn && worn[source.slot]?.isType("obj.bonecrusher_necklace") == true

    private fun Player.isDragonboneNecklaceInvCrushPrayer(source: CrusherSource): Boolean {
        if (source.inventory !== inv) {
            return false
        }
        val neck = worn[Wearpos.Front.slot] ?: return false
        return neck.isType("obj.dragonbone_necklace")
    }

    private fun Player.findBonecrusherChargeSource(): CrusherSource? {
        val neckSlot = Wearpos.Front.slot
        val neck = worn[neckSlot]
        if (neck != null && neck.isCrusherItem() && charges.getCharges(
                neck,
                "varbit.charges_bonecrusher_quantity",
            ) > 0) {
            return CrusherSource(worn, neckSlot)
        }

        for (slot in inv.indices) {
            val obj = inv[slot] ?: continue
            if (!obj.isType("obj.bonecrusher")) {
                continue
            }
            if (charges.getCharges(obj, "varbit.charges_bonecrusher_quantity") > 0) {
                return CrusherSource(inv, slot)
            }
        }
        return null
    }

    private fun decrementBonecrusherCharge(source: CrusherSource): Boolean {
        val result = charges.reduceChargesSameItem(source.inventory, source.slot, remove = 1, "varbit.charges_bonecrusher_quantity",)
        return !result.isFailure()
    }

    private data class CrusherSource(val inventory: Inventory, val slot: Int)
}

private fun InvObj?.isCrusherItem(): Boolean {
    if (this == null) {
        return false
    }
    val internal = RSCM.getReverseMapping(RSCMType.OBJ, id)
    return internal == "obj.bonecrusher" ||
        internal == "obj.bonecrusher_necklace"
}

private var Player.bonecrusherActivityEnabledVar by boolVarBit("varbit.bonecrusher_activity_enabled")

internal fun Player.isBonecrusherActivityEnabled(): Boolean =
    bonecrusherActivityEnabledVar

internal fun Player.setBonecrusherActivity(enabled: Boolean) {
    bonecrusherActivityEnabledVar = enabled
}
