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

class ToragPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
        
    init {
        spawnNpc("npcs.barrows_torag", 3552, 3283, 0, 2)
        spawnNpc("npcs.barrows_torag", 3551, 3280, 0, 2)
        spawnNpc("npcs.barrows_torag", 3551, 3285, 0, 2)
        spawnNpc("npcs.barrows_torag", 3554, 3280, 0, 2)
        spawnNpc("npcs.barrows_torag", 3556, 3284, 0, 2)

        setCombatDef("npcs.barrows_torag") {
            configs {
                attackSpeed = 6
                respawnDelay = 50
            }

            stats {
                hitpoints = 100
                magic = 100
                defence = 100
            }

            anims {
                attack = "sequences.human_caststun"
                block = "sequences.barrows_quarterstaff_defend"
                death = "sequences.champions_zombie_death"
            }
        }
    }
}
