package org.alter.items.dwarven_rock_cake

import kotlin.math.ceil
import org.alter.api.cfg.Sound
import org.alter.api.ext.*
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onItemOption

/**
 *  @author <a href="https://github.com/CloudS3c">Cl0ud</a>
 *  @author <a href="https://www.rune-server.ee/members/376238-cloudsec/">Cl0ud</a>
 */
class DwarvenRockCakePlugin : PluginEvent() {

    override fun init() {
        val rockCakeItems = listOf(
            "items.hundred_dwarf_hot_rockcake",
            "items.hundred_dwarf_cool_rockcake"
        )

        rockCakeItems.forEach { item ->

            onItemOption(item, "Eat") {
                player.queue {
                    player.filterableMessage("Ow! You nearly broke a tooth!")
                    player.filterableMessage("The rock cake resists all attempts to eat it.")

                    player.animate("sequences.human_eat")
                    player.playSound(Sound.EAT_ROCKCAKE)

                    val hp = player.getCurrentHp()
                    player.hit(if (hp > 1) 1 else 0)
                }
            }

            onItemOption(item, "Guzzle") {
                player.queue {
                    player.filterableMessage("You bite hard into the rock cake to guzzle it down.")
                    player.filterableMessage("OW! A terrible shock jars through your skull.")

                    player.animate("sequences.human_eat")
                    player.playSound(Sound.EAT_ROCKCAKE)

                    val hp = player.getCurrentHp()
                    val damage = when {
                        hp <= 1 -> 0
                        hp == 2 -> 1
                        else -> ceil(hp * 0.10).toInt()
                    }

                    player.hit(damage)
                }
            }
        }
    }
}