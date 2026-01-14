package org.alter.interfaces.bank

import org.alter.api.CommonClientScripts
import org.alter.combat.WornBonuses
import org.alter.api.ext.runClientScript
import org.alter.combat.WeaponSpeeds
import org.alter.game.model.entity.Player
import org.alter.interfaces.ifOpenMainSidePair
import org.alter.interfaces.bank.configs.bank_components
import org.alter.interfaces.ifSetText
import org.alter.rscm.RSCM.asRSCM
import org.alter.statGroupTooltip

fun Player.openBank() {
    ifOpenMainSidePair("interfaces.bankmain", "interfaces.bankside", -1, -2)
}

/** Opens bank but does not send any events such as `if_setevent`s */
fun Player.openBankWithoutEvents() {
    disableIfEvents = true
    ifOpenMainSidePair("interfaces.bankmain", "interfaces.bankside", -1, -2)
    disableIfEvents = false
}

internal fun Player.highlightNoClickClear() {
    runClientScript(CommonClientScripts.HIGHLIGHT_NO_CLICK_CLEAR, bank_components.bankside_highlight.asRSCM())
}

fun Player.setBankWornBonuses() {
    val comps = bank_components
    val stats = WornBonuses.calculate(this)
    val speedBase = WeaponSpeeds.base(this)
    val speedActual = WeaponSpeeds.actual(this)
    val magicDmg = stats.finalMagicDmg
    val magicDmgSuffix = stats.magicDmgSuffix
    val undeadSuffix = stats.undeadSuffix
    val slayerSuffix = stats.slayerSuffix
    ifSetText(comps.worn_off_stab, "Stab: ${stats.offStab.signed}")
    ifSetText(comps.worn_off_slash, "Slash: ${stats.offSlash.signed}")
    ifSetText(comps.worn_off_crush, "Crush: ${stats.offCrush.signed}")
    ifSetText(comps.worn_off_magic, "Magic: ${stats.offMagic.signed}")
    ifSetText(comps.worn_off_range, "Range: ${stats.offRange.signed}")
    ifSetText(comps.worn_speed_base, "Base: ${speedBase.tickToSecs}")
    ifSetText(comps.worn_speed, "Actual: ${speedActual.tickToSecs}")
    ifSetText(comps.worn_def_stab, "Stab: ${stats.defStab.signed}")
    ifSetText(comps.worn_def_slash, "Slash: ${stats.defSlash.signed}")
    ifSetText(comps.worn_def_crush, "Crush: ${stats.defCrush.signed}")
    ifSetText(comps.worn_def_range, "Range: ${stats.defRange.signed}")
    ifSetText(comps.worn_def_magic, "Magic: ${stats.defMagic.signed}")
    ifSetText(comps.worn_melee_str, "Melee STR: ${stats.meleeStr.signed}")
    ifSetText(comps.worn_ranged_str, "Ranged STR: ${stats.rangedStr.signed}")
    ifSetText(comps.worn_magic_dmg, "Magic DMG: $magicDmg$magicDmgSuffix")
    ifSetText(comps.worn_prayer, "Prayer: ${stats.prayer.signed}")
    ifSetText(comps.worn_undead, "Undead: ${stats.undead.formatWholePercent}$undeadSuffix")
    statGroupTooltip(
        this,
        comps.tooltip,
        comps.worn_undead,
        "Increases your effective accuracy and damage against undead creatures. " +
                "For multi-target Ranged and Magic attacks, this applies only to the " +
                "primary target. It does not stack with the Slayer multiplier.",
    )
    ifSetText(comps.worn_slayer, "Slayer: ${stats.slayer.formatWholePercent}$slayerSuffix")
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


