package org.alter.plugins.content.combat.combat_options

import org.alter.api.*
import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.model.attr.NEW_ACCOUNT_ATTR
import org.alter.game.model.entity.Player
import org.alter.game.plugin.*
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onButton
import org.alter.game.pluginnew.event.impl.onEquipToSlot
import org.alter.game.pluginnew.event.impl.onLogin
import org.alter.game.pluginnew.event.impl.onLogout
import org.alter.game.pluginnew.event.impl.onTimer
import org.alter.plugins.content.interfaces.attack.AttackTab
import org.alter.plugins.content.interfaces.attack.AttackTab.ATTACK_STYLE_VARP
import org.alter.plugins.content.interfaces.attack.AttackTab.DISABLE_AUTO_RETALIATE_VARP
import org.alter.plugins.content.interfaces.attack.AttackTab.SPECIAL_ATTACK_VARP
import org.alter.plugins.content.interfaces.attack.AttackTab.setEnergy
import org.alter.plugins.content.combat.specialattack.SpecialAttacks

class AttackTabPlugin() : PluginEvent() {

    override fun init() {
        /**
         * First log-in logic (when accounts have just been made).
         */
        onLogin {
            if (player.attr.getOrDefault(NEW_ACCOUNT_ATTR, false)) {
                setEnergy(player, 100)
            }
            AttackTab.resetRestorationTimer(player)
        }

        onTimer(AttackTab.SPEC_RESTORE) {
            val p = player as Player
            AttackTab.restoreEnergy(p)
            AttackTab.resetRestorationTimer(p)
        }

        /**
         * Attack style buttons
         */

        onButton("components.combat_interface:0") {
            player.setVarp(ATTACK_STYLE_VARP, 0)
        }

        onButton("components.combat_interface:1") {
            player.setVarp(ATTACK_STYLE_VARP, 1)
        }

        onButton("components.combat_interface:2") {
            player.setVarp(ATTACK_STYLE_VARP, 2)
        }

        onButton("components.combat_interface:3") {
            player.setVarp(ATTACK_STYLE_VARP, 3)
        }

        /**
         * Toggle auto-retaliate button.
         */
        onButton("components.combat_interface:retaliate") {
            player.toggleVarp(DISABLE_AUTO_RETALIATE_VARP)
        }


        /**
         * Toggle special attack.
         */
        onButton("components.combat_interface:special_attack") {
            val weaponId = player.equipment[EquipmentType.WEAPON.id]!!.id
            if (SpecialAttacks.executeOnEnable(weaponId)) {
                if (!SpecialAttacks.execute(player, null, world)) {
                    player.message("You don't have enough power left.")
                }
            } else {
                player.toggleVarp(SPECIAL_ATTACK_VARP)
            }
        }

        /**
         * Disable special attack when switching weapons.
         */
        onEquipToSlot(EquipmentType.WEAPON.id) {
            player.setVarp(SPECIAL_ATTACK_VARP, 0)
        }

        /**
         * Disable special attack on log-out.
         */
        onLogout {
            player.setVarp(SPECIAL_ATTACK_VARP, 0)
        }
    }
}
