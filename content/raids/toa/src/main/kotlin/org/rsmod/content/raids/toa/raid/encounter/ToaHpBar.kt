package org.rsmod.content.raids.toa.raid.encounter

import java.awt.Color
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.ui.setColour
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

internal class ToaHpBar(
    private val room: ToaEncounter,
    private val completion: Completion = Completion.Instant,
    private val remainingOnClose: Color? = null,
    private val currentHp: (Npc) -> Int = { it.hitpoints },
    private val maxHp: (Npc) -> Int = { it.baseHitpointsLvl },
    private val subject: () -> Npc?,
) {
    private val bossHpBar
        get() = room.raid.deps.bossHpBar

    sealed interface Completion {
        data object Instant : Completion

        data class Fade(val fadeTicks: Int, val clearTicks: Int) : Completion
    }

    fun open(player: Player) {
        val npc = subject() ?: return
        bossHpBar.onOpen(player, npc)
        push(player, npc)
    }

    fun update() {
        val npc = subject() ?: return
        for (player in room.players) push(player, npc)
    }

    fun point() {
        val npc = subject() ?: return
        for (player in room.players) player.barNpc = npc.visType.id
    }

    fun close(player: Player) {
        val npc = subject() ?: return
        bossHpBar.onClose(player, npc, instant = true)
        remainingOnClose?.let { player.setColour(REMAINING, it) }
    }

    fun complete() {
        when (completion) {
            Completion.Instant -> {
                for (player in room.players) close(player)
            }
            is Completion.Fade -> {
                room.schedule(completion.fadeTicks) {
                    val args: List<Any> = bossHpBar.commonComponents.toList() + 0
                    for (player in room.players) player.runClientScript(FADE_OUT_SCRIPT, args)
                }
                room.schedule(completion.clearTicks) {
                    for (player in room.players) {
                        close(player)
                        player.clearBar()
                    }
                }
            }
        }
    }

    private fun push(player: Player, npc: Npc) {
        bossHpBar.onUpdate(player, npc, currentHp = currentHp(npc), maxHp = maxHp(npc))
    }

    private fun Player.clearBar() {
        barNpc = NO_NPC
        barHp = 0
        barBaseHp = 0
        barBoss = 0
    }

    private companion object {
        const val REMAINING = "component.hpbar_hud:health_bar_remaining"
        const val FADE_OUT_SCRIPT = 2889
        const val NO_NPC = -1
    }
}

private var Player.barNpc by intVarp("varp.hpbar_hud_npc")
private var Player.barHp by intVarBit("varbit.hpbar_hud_hp")
private var Player.barBaseHp by intVarBit("varbit.hpbar_hud_basehp")
private var Player.barBoss by intVarBit("varbit.hpbar_hud_boss")
