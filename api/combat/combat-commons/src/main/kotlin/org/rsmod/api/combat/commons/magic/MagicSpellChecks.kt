package org.rsmod.api.combat.commons.magic

import dev.openrune.types.ItemServerType

public object MagicSpellChecks {
    public fun isBoltSpell(spell: ItemServerType): Boolean =
        spell.isAnyType(
            "obj.17_wind_bolt",
            "obj.23_water_bolt",
            "obj.29_earth_bolt",
            "obj.35_fire_bolt",
        )

    public fun isGodSpell(spell: ItemServerType): Boolean =
        spell.isAnyType(
            "obj.60_claws_of_guthix",
            "obj.60_flames_of_zamorak",
            "obj.60_saradomin_strike",
        )

    public fun isDemonbaneSpell(spell: ItemServerType): Boolean =
        spell.isAnyType(
            "obj.br_mithril_scimitar",
            "obj.br_willow_bow",
            "obj.br_adamant_scimitar",
        )

    public fun isBindSpell(spell: ItemServerType): Boolean =
        spell.isAnyType("obj.20_bind", "obj.50_snare", "obj.79_entangle")

    public fun isWindSpell(spell: ItemServerType): Boolean =
        spell.isAnyType(
            "obj.01_wind_strike",
            "obj.17_wind_bolt",
            "obj.41_wind_blast",
            "obj.62_wind_wave",
            "obj.81_wind_surge",
        )

    public fun isWaterSpell(spell: ItemServerType): Boolean =
        spell.isAnyType(
            "obj.05_water_strike",
            "obj.23_water_bolt",
            "obj.47_water_blast",
            "obj.65_water_wave",
            "obj.85_water_surge",
        )

    public fun isEarthSpell(spell: ItemServerType): Boolean =
        spell.isAnyType(
            "obj.09_earth_strike",
            "obj.29_earth_bolt",
            "obj.53_earth_blast",
            "obj.70_earth_wave",
            "obj.90_earth_surge",
        )

    public fun isFireSpell(spell: ItemServerType): Boolean =
        spell.isAnyType(
            "obj.13_fire_strike",
            "obj.35_fire_bolt",
            "obj.59_fire_blast",
            "obj.75_fire_wave",
            "obj.95_fire_surge",
        )
}
