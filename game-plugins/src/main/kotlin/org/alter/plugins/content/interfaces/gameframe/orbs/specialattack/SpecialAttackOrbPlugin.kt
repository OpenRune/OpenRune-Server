package org.alter.plugins.content.interfaces.gameframe.orbs.specialattack


import org.alter.api.*
import org.alter.api.cfg.*
import org.alter.api.ext.*
import org.alter.api.ext.player
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.plugin.*
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.interfaces.attack.AttackTab.SPECIAL_ATTACK_VARP
import org.alter.plugins.content.interfaces.gameframe.config.Orbs
import org.alter.plugins.content.interfaces.xpdrops.XpSettings
import kotlin.collections.get

class SpecialAttackOrbPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {

        onButton(interfaceId = Orbs.ORBS_UNIVERSE, component = Orbs.SPECBUTTON) {
            val weaponId = player.equipment[EquipmentType.WEAPON.id]!!.id
            if (SpecialAttacks.executeOnEnable(weaponId)) {
                if (!SpecialAttacks.execute(player, null, world)) {
                    player.message("You don't have enough power left.")
                }
            } else {
                player.toggleVarp(Orbs.SPECATTVARP)
            }
        }
    }
}
