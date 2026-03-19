package org.rsmod.api.combat.commons.magic

import dev.openrune.types.ItemServerType
import org.rsmod.api.config.refs.objs

public object MagicSpellChecks {
    public fun isBoltSpell(spell: ItemServerType): Boolean =
        spell.isAnyType(
            objs.spell_wind_bolt,
            objs.spell_water_bolt,
            objs.spell_earth_bolt,
            objs.spell_fire_bolt,
        )

    public fun isGodSpell(spell: ItemServerType): Boolean =
        spell.isAnyType(
            objs.spell_claws_of_guthix,
            objs.spell_flames_of_zamorak,
            objs.spell_saradomin_strike,
        )

    public fun isDemonbaneSpell(spell: ItemServerType): Boolean =
        spell.isAnyType(
            objs.spell_inferior_demonbane,
            objs.spell_superior_demonbane,
            objs.spell_dark_demonbane,
        )

    public fun isBindSpell(spell: ItemServerType): Boolean =
        spell.isAnyType(objs.spell_bind, objs.spell_snare, objs.spell_entangle)

    public fun isWindSpell(spell: ItemServerType): Boolean =
        spell.isAnyType(
            objs.spell_wind_strike,
            objs.spell_wind_bolt,
            objs.spell_wind_blast,
            objs.spell_wind_wave,
            objs.spell_wind_surge,
        )

    public fun isWaterSpell(spell: ItemServerType): Boolean =
        spell.isAnyType(
            objs.spell_water_strike,
            objs.spell_water_bolt,
            objs.spell_water_blast,
            objs.spell_water_wave,
            objs.spell_water_surge,
        )

    public fun isEarthSpell(spell: ItemServerType): Boolean =
        spell.isAnyType(
            objs.spell_earth_strike,
            objs.spell_earth_bolt,
            objs.spell_earth_blast,
            objs.spell_earth_wave,
            objs.spell_earth_surge,
        )

    public fun isFireSpell(spell: ItemServerType): Boolean =
        spell.isAnyType(
            objs.spell_fire_strike,
            objs.spell_fire_bolt,
            objs.spell_fire_blast,
            objs.spell_fire_wave,
            objs.spell_fire_surge,
        )
}
