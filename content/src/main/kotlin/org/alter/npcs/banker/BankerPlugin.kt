package org.alter.npcs.banker

import org.alter.api.ext.chatNpc
import org.alter.api.ext.chatPlayer
import org.alter.api.ext.options
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onNpcOption
import org.alter.interfaces.bank.openBank
import org.alter.interfaces.ifOpenMainModal


class BankerPlugin() : PluginEvent() {

    private val bankers = listOf(
        "npcs.mourning_elf_bankerm",
        "npcs.mourning_elf_bankerf",
        "npcs.banker1_new",
    )

    override fun init() {
        bankers.forEach { banker ->
            onNpcOption(banker, "talk-to") {
                player.queue {
                    dialog(player, this)
                }
            }
            onNpcOption(banker, "bank") {
                player.openBank()
            }
            onNpcOption(banker, "collect") {
                openCollect(player)
            }
        }
    }

    suspend fun dialog(player: Player, it: QueueTask) {
        it.chatNpc(player, "Good day, how may I help you?")
        when (options(player, it)) {
            1 -> player.openBank()
            2 -> openPin(player)
            3 -> openCollect(player)
            4 -> whatIsThisPlace(player, it)
        }
    }

    suspend fun options(player: Player, it: QueueTask): Int =
        it.options(
            player,
            "I'd like to access my bank account, please.",
            "I'd like to check my PIN settings.",
            "I'd like to collect items.",
            "What is this place?",
        )

    suspend fun whatIsThisPlace(player: Player, it: QueueTask) {
        it.chatNpc(
            player,
            "This is a branch of the Bank of Gielinor. We have<br>branches in many towns.",
            animation = "sequences.chathap2"
        )
        it.chatPlayer(player, "And what do you do?", animation = "sequences.chathap1")
        it.chatNpc(
            player,
            "We will look after your items and money for you.<br>Leave your valuables with us if you want to keep them<br>safe.",
            animation = "sequences.chathap3",
        )
    }

    private fun openCollect(p: Player) {
        p.ifOpenMainModal("interfaces.ge_collect",-1,-1)
    }

    private fun openPin(p: Player) {
        p.ifOpenMainModal("interfaces.bankpin_settings",-1,-1)
    }

}
