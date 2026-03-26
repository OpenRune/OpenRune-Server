package org.alter.combat.special

import org.alter.api.CombatAttributes
import org.alter.api.EquipmentType
import org.alter.api.ext.getVarp
import org.alter.api.ext.hasEquipped
import org.alter.api.ext.message
import org.alter.api.ext.setVarp
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.PostAttackEvent
import org.alter.game.pluginnew.event.impl.PreAttackEvent

class SpecialAttackDispatcher : PluginEvent() {

    private data class SpecialDef(val energyCost: Int, val executeOnSpecBar: Boolean = false)

    private val specials = mapOf(
        // Abyssal Bludgeon - 50%
        "items.abyssal_bludgeon" to SpecialDef(energyCost = 50),

        // Abyssal Dagger - 50% (all variants including poisoned)
        "items.abyssal_dagger" to SpecialDef(energyCost = 50),
        "items.abyssal_dagger_p" to SpecialDef(energyCost = 50),
        "items.abyssal_dagger_p+" to SpecialDef(energyCost = 50),
        "items.abyssal_dagger_p++" to SpecialDef(energyCost = 50),

        // Armadyl Godsword - 50% (normal and gilded)
        "items.ags" to SpecialDef(energyCost = 50),
        "items.agsg" to SpecialDef(energyCost = 50),

        // Bandos Godsword - 50%
        "items.bgs" to SpecialDef(energyCost = 50),

        // Dragon Battleaxe - 100%, fires on spec bar activation (no attack)
        "items.dragon_battleaxe" to SpecialDef(energyCost = 100, executeOnSpecBar = true),

        // Dragon Dagger - 25%
        "items.dragon_dagger" to SpecialDef(energyCost = 25),

        // Dragon Longsword - 25%
        "items.dragon_longsword" to SpecialDef(energyCost = 25),

        // Dragon Mace - 50%
        "items.dragon_mace" to SpecialDef(energyCost = 50),

        // Dragon Warhammer - 50%
        "items.dragon_warhammer" to SpecialDef(energyCost = 50),

        // Saradomin Godsword - 50%
        "items.sgs" to SpecialDef(energyCost = 50),

        // Zamorak Godsword - 50%
        "items.zgs" to SpecialDef(energyCost = 50),
    )

    override fun init() {
        on<PreAttackEvent> {
            where { attacker is Player }
            priority(-20)
            then {
                val player = attacker as Player
                if (player.getVarp("varp.sa_attack") != 1) return@then

                val specDef = findSpecialDef(player) ?: run {
                    player.setVarp("varp.sa_attack", 0)
                    return@then
                }

                if (specDef.executeOnSpecBar) {
                    player.setVarp("varp.sa_attack", 0)
                    return@then
                }

                val currentEnergy = player.getVarp("varp.sa_energy") / 10
                if (currentEnergy < specDef.energyCost) {
                    player.message("You don't have enough special attack energy.")
                    player.setVarp("varp.sa_attack", 0)
                    cancelled = true
                    cancelReason = "Insufficient special attack energy"
                    return@then
                }

                player.setVarp("varp.sa_energy", (currentEnergy - specDef.energyCost) * 10)
                player.setVarp("varp.sa_attack", 0)
                player.attr[CombatAttributes.SPECIAL_ATTACK_ACTIVE] = true
            }
        }

        on<PostAttackEvent> {
            where { attacker is Player }
            priority(100)
            then {
                val player = attacker as Player
                player.attr.remove(CombatAttributes.SPECIAL_ATTACK_ACTIVE)
            }
        }
    }

    private fun findSpecialDef(player: Player): SpecialDef? {
        return specials.entries.firstOrNull { (itemName, _) ->
            player.hasEquipped(EquipmentType.WEAPON, itemName)
        }?.value
    }
}
