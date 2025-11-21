package org.alter.plugins.content.combat.specialattack.weapons.dragonlongsword

import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.AreaSound
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.dealHit
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttacks

class DragonLongswordPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {
        val SPECIAL_REQUIREMENT = 25

        SpecialAttacks.register("items.dragon_longsword", SPECIAL_REQUIREMENT) {
            player.animate(id = "sequences.longsword_special")
            player.graphic(id = "spotanims.sp_attack_longsword_spotanim", height = 92)
            world.spawn(AreaSound(tile = player.tile, id = 2537, radius = 10, volume = 1))

            val maxHit = MeleeCombatFormula.getMaxHit(player, target, specialAttackMultiplier = 1.25)
            val accuracy = MeleeCombatFormula.getAccuracy(player, target, specialAttackMultiplier = 1.0)
            val landHit = accuracy >= world.randomDouble()
            player.dealHit(target = target, maxHit = maxHit, landHit = landHit, delay = 1)
        }
    }
}

