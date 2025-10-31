package org.alter.plugins.content.interfaces.spellfilter

/**
 * @author Tom <rspsmods@gmail.com>
 */
object SpellFilters {
    const val SPELL_FILTER_INTERFACE_ID = 218
    const val SPELL_FILTER_COMPONENT_ID = 196

    const val DISABLE_FILTERS_VARBIT = "varbits.magic_spellbook_hidefilterbutton"

    const val FILTER_COMBAT_VARBIT = "varbits.magic_filter_blockcombat"
    const val FILTER_TELEPORTS_VARBIT = "varbits.magic_filter_blockteleport"
    const val FILTER_UTILITY_VARBIT = "varbits.magic_filter_blockutility"
    const val FILTER_BY_LEVEL_VARBIT = "varbits.magic_filter_blocklacklevel"
    const val FILTER_BY_RUNES_VARBIT = "varbits.magic_filter_blocklackrunes"
}
