package org.rsmod.content.interfaces.bank

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.combat.weapon.WeaponSpeeds
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.player.output.ClientScripts.statGroupTooltip
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.ui.ifOpenMainSidePair
import org.rsmod.api.player.ui.ifSetText
import org.rsmod.api.player.vars.intVarp
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getInvObj

private var Player.extraOpsSpecialBits by intVarp("varp.if1")
private var Player.extraOpsWearBits by intVarp("varp.if2")
private var Player.extraOpsConsumableBits by intVarp("varp.if3")

fun Player.openBank(eventBus: EventBus) {
    ifOpenMainSidePair("interface.bankmain", "interface.bankside", -1, -2, eventBus)
}

/** Opens bank but does not send any events such as `if_setevent`s */
fun Player.openBankWithoutEvents(eventBus: EventBus) {
    disableIfEvents = true
    ifOpenMainSidePair("interface.bankmain", "interface.bankside", -1, -2, eventBus)
    disableIfEvents = false
}

internal fun Player.highlightNoClickClear() {
    runClientScript(3407, "component.bankside:bankside_highlight".asRSCM(RSCMType.COMPONENT))
}

internal fun Player.setBanksideExtraOps() {
    var specialBits = 0
    var wearBits = 0
    var consumableBits = 0

    for (slot in inv.indices) {
        val obj = inv[slot] ?: continue
        val type = getInvObj(obj)

        val specialBit = type.paramOrNull(params.bankside_extraop_bit)
        if (specialBit != null) {
            val varbit = type.param(params.bankside_extraop_varbit)
            val flipReq = type.param(params.bankside_extraop_flip)
            val enabled = flipReq && vars[varbit] == 0 || !flipReq && vars[varbit] > 0
            if (enabled) {
                specialBits = specialBits or (1 shl specialBit)
                continue
            }
        }

        val extraOpText = type.param(params.bankside_extraop)
        if (extraOpText.isNotBlank()) {
            continue
        }

        val wearBit = type.wearpos1 != -1
        if (wearBit) {
            val wearOpIndex = type.param(params.wear_op_index)
            val hasWearOp = type.hasInvOp(wearOpIndex)
            if (hasWearOp) {
                wearBits = wearBits or (1 shl slot)
                continue
            }
        }

        val foodBit = type.isContentType("content.food")
        val potionBit = type.isContentType("content.potion")

        val consumableBit = foodBit || potionBit
        if (consumableBit) {
            consumableBits = consumableBits or (1 shl slot)
            continue
        }
    }

    extraOpsSpecialBits = specialBits
    extraOpsWearBits = wearBits
    extraOpsConsumableBits = consumableBits
}

internal fun Player.setBankWornBonuses(wornBonuses: WornBonuses, weaponSpeeds: WeaponSpeeds) {
    val stats = wornBonuses.calculate(this)
    val speedBase = weaponSpeeds.base(this)
    val speedActual = weaponSpeeds.actual(this)
    val magicDmg = stats.finalMagicDmg
    val magicDmgSuffix = stats.magicDmgSuffix
    val undeadSuffix = stats.undeadSuffix
    val slayerSuffix = stats.slayerSuffix
    ifSetText("component.bankmain:stabatt", "Stab: ${stats.offStab.signed}")
    ifSetText("component.bankmain:slashatt", "Slash: ${stats.offSlash.signed}")
    ifSetText("component.bankmain:crushatt", "Crush: ${stats.offCrush.signed}")
    ifSetText("component.bankmain:magicatt", "Magic: ${stats.offMagic.signed}")
    ifSetText("component.bankmain:rangeatt", "Range: ${stats.offRange.signed}")
    ifSetText("component.bankmain:attackspeedbase", "Base: ${speedBase.tickToSecs}")
    ifSetText("component.bankmain:attackspeedactual", "Actual: ${speedActual.tickToSecs}")
    ifSetText("component.bankmain:stabdef", "Stab: ${stats.defStab.signed}")
    ifSetText("component.bankmain:slashdef", "Slash: ${stats.defSlash.signed}")
    ifSetText("component.bankmain:crushdef", "Crush: ${stats.defCrush.signed}")
    ifSetText("component.bankmain:rangedef", "Range: ${stats.defRange.signed}")
    ifSetText("component.bankmain:magicdef", "Magic: ${stats.defMagic.signed}")
    ifSetText("component.bankmain:meleestrength", "Melee STR: ${stats.meleeStr.signed}")
    ifSetText("component.bankmain:rangestrength", "Ranged STR: ${stats.rangedStr.signed}")
    ifSetText("component.bankmain:magicdamage", "Magic DMG: $magicDmg$magicDmgSuffix")
    ifSetText("component.bankmain:prayer", "Prayer: ${stats.prayer.signed}")
    ifSetText("component.bankmain:typemultiplier", "Undead: ${stats.undead.formatWholePercent}$undeadSuffix")
    statGroupTooltip(
        this,
        "component.bankmain:tooltip",
        "component.bankmain:typemultiplier",
        "Increases your effective accuracy and damage against undead creatures. " +
            "For multi-target Ranged and Magic attacks, this applies only to the " +
            "primary target. It does not stack with the Slayer multiplier.",
    )
    ifSetText("component.bankmain:slayermultiplier", "Slayer: ${stats.slayer.formatWholePercent}$slayerSuffix")
}

private val Int.signed: String
    get() = if (this < 0) "$this" else "+$this"

private val Int.formatPercent: String
    get() = "+${this / 10.0}%"

private val Int.formatWholePercent: String
    get() = "+${this / 10}%"

private val Int.tickToSecs: String
    get() = "${(this * 600) / 1000.0}s"

private val WornBonuses.Bonuses.finalMagicDmg: String
    get() = multipliedMagicDmg.formatPercent

private val WornBonuses.Bonuses.magicDmgSuffix: String
    get() = if (magicDmgAdditive == 0) "" else "<col=be66f4> ($magicDmgAdditive%)</col>"

// Undead bonus has a trailing whitespace when bonus is at 0.
private val WornBonuses.Bonuses.undeadSuffix: String
    get() = if (undead == 0) " " else if (undeadMeleeOnly) " (melee)" else " (all styles)"

private val WornBonuses.Bonuses.slayerSuffix: String
    get() = if (slayer == 0) "" else if (slayerMeleeOnly) " (melee)" else " (all styles)"
