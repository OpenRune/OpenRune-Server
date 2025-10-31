package org.alter.plugins.content.npcs.barrows

import org.alter.api.*
import org.alter.api.cfg.*
import org.alter.api.dsl.*
import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.model.attr.*
import org.alter.game.model.container.*
import org.alter.game.model.container.key.*
import org.alter.game.model.entity.*
import org.alter.game.model.item.*
import org.alter.game.model.queue.*
import org.alter.game.model.shop.*
import org.alter.game.model.timer.*
import org.alter.game.plugin.*

class DharokPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
        
    init {
        spawnNpc("npcs.barrows_dharok", 3576, 3298, 0, 2)
        spawnNpc("npcs.barrows_dharok", 3576, 3300, 0, 2)
        spawnNpc("npcs.barrows_dharok", 3573, 3299, 0, 2)
        spawnNpc("npcs.barrows_dharok", 3578, 3296, 0, 2)
        spawnNpc("npcs.barrows_dharok", 3574, 3295, 0, 2)

        setCombatDef("npcs.barrows_dharok") {
            configs {
                attackSpeed = 7
                respawnDelay = 50
            }

            stats {
                hitpoints = 100
                attack = 100
                strength = 100
                defence = 100
            }

            anims {

attack = "sequences.barrow_dharok_crush"
block = "sequences.human_unarmedblock"
death = "sequences.champions_zombie_death"

}
        }
    }
}
