package org.rsmod.api.spells.autocast

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.util.WeaponCategory
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.refs.BaseParams
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.spells.autocast.configs.autocast_params
import org.rsmod.game.entity.Player

@Singleton
public class AutocastWeapons @Inject constructor(private val spells: AutocastSpells) {
    /**
     * Returns `true` if [weapon] is a valid staff that can be used to cast the spell associated
     * with [autocastId]. Otherwise, sends the appropriate missing-requirement message to the player
     * and returns `false`.
     */
    public fun canStaffAutocast(player: Player, weapon: ItemServerType, autocastId: Int): Boolean {
        val spell = spells[autocastId]
        if (spell != null) {
            val spellbook = spell.spellbook()
            if (!canAutocastSpellbook(weapon, spellbook)) {
                player.mes(unsupportedSpellbookMessage(spellbook))
                return false
            }
        }

        val isValidStaff = canStaffAutocast(weapon, autocastId)
        if (!isValidStaff) {
            player.mes("You can't autocast that spell with this staff.")
            return false
        }
        return true
    }

    public fun canStaffAutocast(weapon: ItemServerType, autocastId: Int): Boolean {
        val spell = spells[autocastId]

        // This can occur if a player was auto-casting a spell that is now removed (or at least
        // its ability to be autocast). We return false so it can be cleared by the caller.
        if (spell == null) {
            return false
        }

        if (!canAutocastSpellbook(weapon, spell.spellbook())) {
            return false
        }

        // We return early if the spell is a standard autocast spell. (Can be cast by any staff)
        if (!spells.isRestrictedSpell(spell)) {
            return true
        }

        // Certain spells can only be autocast by specific weapons, such as Iban blast only being
        // available with Iban's staff.
        val staffAdditionalSpells = weapon.additionalAutocastSpells()
        val isValidAdditionalSpell = staffAdditionalSpells.any { it.isType(spell) }
        return isValidAdditionalSpell
    }

    public fun canAutocastSpellbook(weapon: ItemServerType, spellbook: Spellbook?): Boolean {
        val category = WeaponCategory.getOrUnarmed(weapon.weaponCategory.id)
        if (getVarBits(category) == null) {
            return false
        }
        return when (spellbook) {
            Spellbook.Standard -> true
            Spellbook.Ancients -> weapon.id in AncientAutocastWeapons
            Spellbook.Arceuus -> weapon.id in ArceuusAutocastWeapons
            else -> false
        }
    }

    public fun unsupportedSpellbookMessage(spellbook: Spellbook?): String =
        when (spellbook) {
            Spellbook.Ancients -> "You cannot autocast Ancient Magicks with that."
            Spellbook.Arceuus -> "You cannot autocast Arceuus spells with that."
            Spellbook.Lunars -> "You cannot autocast Lunar spells with that."
            else -> "You can't autocast that spell with this staff."
        }

    private fun ItemServerType.spellbook(): Spellbook? = Spellbook[param(BaseParams.spell_spellbook)]

    private fun ItemServerType.additionalAutocastSpells(): Set<ItemServerType> {
        val additional1 = paramOrNull(autocast_params.additional_spell_autocast1)
        val additional2 = paramOrNull(autocast_params.additional_spell_autocast2)
        val additional3 = paramOrNull(autocast_params.additional_spell_autocast3)
        return setOfNotNull(additional1, additional2, additional3)
    }

    public fun set(player: Player, varbits: StaffVarBits, autocastId: Int, defensiveCast: Boolean) {
        val (autocastVarBit, defensiveCastVarBit) = varbits
        VarPlayerIntMapSetter.set(player, autocastVarBit, autocastId)
        VarPlayerIntMapSetter.set(player, defensiveCastVarBit, if (defensiveCast) 1 else 0)
    }

    public fun reset(player: Player, varbits: StaffVarBits) {
        set(player, varbits, autocastId = 0, defensiveCast = false)
    }

    public fun reset(player: Player, weapon: ItemServerType) {
        val category = WeaponCategory.getOrUnarmed(weapon.weaponCategory.id)
        val varbits = getVarBits(category) ?: return
        reset(player, varbits)
    }

    // Note: If more autocast weapon categories are added in the future, we should consider storing
    // them in an enum config instead.
    public fun getVarBits(category: WeaponCategory): StaffVarBits? =
        when (category) {
            WeaponCategory.Staff ->
                StaffVarBits(
                    "varbit.saved_autocast_spell_staff",
                    "varbit.saved_defensive_casting_staff",
                )

            WeaponCategory.BladedStaff ->
                StaffVarBits(
                    "varbit.saved_autocast_spell_bladed_staff",
                    "varbit.saved_defensive_casting_bladed_staff",
                )

            else -> null
        }

    public data class StaffVarBits(
        public val autocastId: String,
        public val defensiveCast: String,
    )

    private companion object {
        val AncientAutocastWeapons =
            objSet(
                "obj.staff_of_zaros",
                "obj.beta_item_6",
                "obj.beta_item_7",
                "obj.beta_item_8",
                "obj.beta_item_9",
                "obj.ancient_sceptre",
                "obj.ancient_sceptre_trouver",
                "obj.ancient_sceptre_blood",
                "obj.ancient_sceptre_ice",
                "obj.ancient_sceptre_smoke",
                "obj.ancient_sceptre_shadow",
                "obj.ancient_sceptre_blood_trouver",
                "obj.ancient_sceptre_ice_trouver",
                "obj.ancient_sceptre_smoke_trouver",
                "obj.ancient_sceptre_shadow_trouver",
                "obj.wild_cave_sceptre_charged",
                "obj.wild_cave_sceptre_charged_recol",
                "obj.wild_cave_accursed_charged",
                "obj.wild_cave_accursed_charged_recol",
                "obj.barrows_ahrim_weapon",
                "obj.barrows_ahrim_weapon_100",
                "obj.barrows_ahrim_weapon_75",
                "obj.barrows_ahrim_weapon_50",
                "obj.barrows_ahrim_weapon_25",
                "obj.barrows_ahrim_weapon_ornament",
                "obj.barrows_ahrim_weapon_ornament_100",
                "obj.barrows_ahrim_weapon_ornament_75",
                "obj.barrows_ahrim_weapon_ornament_50",
                "obj.barrows_ahrim_weapon_ornament_25",
                "obj.br_barrows_ahrim_weapon",
                "obj.frostmoon_spear",
                "obj.br_frostmoon_spear",
                "obj.magictraining_wand_master",
                "obj.br_master_wand",
                "obj.dragonhunter_wand",
                "obj.kodai_wand",
                "obj.br_kodai_wand",
                "obj.nightmare_staff",
                "obj.nightmare_staff_volatile",
                "obj.nightmare_staff_eldritch",
                "obj.br_nightmare_staff_volatile",
                "obj.deadman_blighted_volatile_staff",
                "obj.deadman_nightmare_staff_volatile",
            )

        val ArceuusAutocastWeapons =
            objSet(
                "obj.sos_skull_sceptre_imbued",
                "obj.slayer_staff",
                "obj.slayer_staff_enchanted",
                "obj.barrows_ahrim_weapon",
                "obj.barrows_ahrim_weapon_100",
                "obj.barrows_ahrim_weapon_75",
                "obj.barrows_ahrim_weapon_50",
                "obj.barrows_ahrim_weapon_25",
                "obj.barrows_ahrim_weapon_ornament",
                "obj.barrows_ahrim_weapon_ornament_100",
                "obj.barrows_ahrim_weapon_ornament_75",
                "obj.barrows_ahrim_weapon_ornament_50",
                "obj.barrows_ahrim_weapon_ornament_25",
                "obj.br_barrows_ahrim_weapon",
                "obj.frostmoon_spear",
                "obj.br_frostmoon_spear",
                "obj.sotd",
                "obj.br_sotd",
                "obj.toxic_sotd",
                "obj.toxic_sotd_charged",
                "obj.toxic_sotd_deadman",
                "obj.toxic_sotd_charged_deadman",
                "obj.purging_staff",
                "obj.br_purging_staff",
                "obj.magictraining_wand_master",
                "obj.br_master_wand",
                "obj.kodai_wand",
                "obj.br_kodai_wand",
            )

        private fun objSet(vararg refs: String): Set<Int> =
            refs.mapTo(HashSet(refs.size)) { obj(it) }

        private fun obj(ref: String): Int = ref.asRSCM(RSCMType.OBJ)
    }
}
