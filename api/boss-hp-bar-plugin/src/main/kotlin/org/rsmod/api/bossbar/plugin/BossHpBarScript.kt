package org.rsmod.api.bossbar.plugin

import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.awt.Color
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bossbar.BossHpBarMode
import org.rsmod.api.config.refs.params
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.events.InstancePlayerJoinUnboundEvent
import org.rsmod.api.instances.events.InstancePlayerLeaveUnboundEvent
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.ifSetHide
import org.rsmod.api.player.ui.setColour
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext


@Singleton
public class BossHpBarScript @Inject constructor(
    private val instances: InstanceManager,
    private val contributor: BossHpBarDamageContributor,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {

    internal var Player.bossHudDisabled by boolVarBit("varbit.hpbar_hud_boss_disabled")
    internal var Player.bossHudCurrentHp by intVarBit("varbit.hpbar_hud_hp")
    internal var Player.bossHudBaseHp by intVarBit("varbit.hpbar_hud_basehp")
    internal var Player.bossHudNpcID by intVarp("varp.hpbar_hud_npc")
    internal var Player.bossHudBarSize by intVarBit("varbit.hpbar_hud_boss")

    override fun ScriptContext.startup() {
        onEvent<InstancePlayerJoinUnboundEvent> {
            for (npc in instances.npcsForInstance(instanceId)) {
                if (npcBarMode(npc) == BossHpBarMode.ON_ENTER && npc.hitpoints > 0) {
                    onOpen(player, npc)
                }
            }
        }

        onEvent<InstancePlayerLeaveUnboundEvent> {
            for (npc in instances.npcsForInstance(instanceId)) {
                if (npcBarMode(npc) == BossHpBarMode.ON_ENTER) {
                    onClose(player, npc, instant = true)
                }
            }
        }

        onPlayerSoftTimer(BossHpBarDamageContributor.COMBAT_CHECK_TIMER) {
            contributor.checkAndExpirePlayer(player)
        }

        onEvent<NpcStateEvents.Delete> {
            when (npcBarMode(npc)) {
                BossHpBarMode.ON_ATTACK -> {
                    for (player in contributor.removeTrackedForNpc(npc)) {
                        onClose(player, npc)
                    }
                }
                BossHpBarMode.ON_ENTER -> closeForAllInstancePlayers(npc)
                BossHpBarMode.NEVER -> Unit
            }
        }

        onEvent<NpcStateEvents.Respawn> {
            if (npcBarMode(npc) != BossHpBarMode.ON_ENTER) return@onEvent
            val instanceId = instances.instanceForNpc(npc) ?: return@onEvent
            for (player in contributor.playersInInstance(instanceId)) {
                onOpen(player, npc)
                onUpdate(player, npc, npc.baseHitpointsLvl, npc.baseHitpointsLvl)
            }
        }
    }

    public fun onOpen(player: Player, npc: Npc) {
        if (player.bossHudDisabled) return

        player.bossHudNpcID = npc.id
        player.bossHudBaseHp = npc.baseHitpointsLvl
        player.bossHudCurrentHp = npc.hitpoints
        player.bossHudBarSize = 1

        player.setColour("component.hpbar_hud:inner",ORIGINAL_COLORS[0])
        player.setColour("component.hpbar_hud:health_bar_back",ORIGINAL_COLORS[1])
        player.setColour("component.hpbar_hud:health_bar_sliding",ORIGINAL_COLORS[2])

        player.runClientScript(2287, commonComponents, 0)
        openScripts(player)
    }


    @OptIn(InternalApi::class)
    public fun onClose(player: Player, npc: Npc, instant: Boolean = false) {
        if (instant) {
            player.ifSetHide("component.hpbar_hud:hp", true)
            return
        }
        protectedAccess.launchLenient(player) {
            player.runClientScript(2889, commonComponents, 0)
            delay(2)
            player.ifSetHide("component.hpbar_hud:hp", true)
        }
    }

    public fun onUpdate(player: Player, npc: Npc, currentHp: Int = npc.hitpoints, maxHp: Int = npc.baseHitpointsLvl) {
        if (player.bossHudDisabled) return

        player.bossHudBaseHp = maxHp
        player.bossHudCurrentHp = currentHp
    }

    private fun closeForAllInstancePlayers(npc: Npc) {
        val instanceId = instances.instanceForNpc(npc) ?: return
        for (player in contributor.playersInInstance(instanceId)) {
            onClose(player, npc)
        }
    }

    private fun npcBarMode(npc: Npc): BossHpBarMode =
        BossHpBarMode.fromId(npc.visType.paramOrNull(params.boss_hp_bar_mode) ?: 0)

    private fun openScripts(player: Player) {
        player.runClientScript(2887, commonComponents, 255)
        player.runClientScript(2102, commonComponents,1)

        player.runClientScript(
            2376,
            "component.hpbar_hud:universe".asRSCM(),
            "component.hpbar_hud:hpdodgerside".asRSCM(),
            "component.hpbar_hud:hpdodger".asRSCM(),
            "component.hpbar_hud:hp".asRSCM(),
            "component.hpbar_hud:name_area".asRSCM(),
            "component.hpbar_hud:hp_bar".asRSCM(),
            "component.hpbar_hud:hp_bar_text".asRSCM(),
            "component.hpbar_hud:health_bar_back".asRSCM(),
            "component.hpbar_hud:health_bar_sliding".asRSCM(),
            "component.hpbar_hud:health_bar_remaining".asRSCM(),
            "component.hpbar_hud:creature_name".asRSCM(),
            "component.hpbar_hud:outer_border".asRSCM(),
            "component.hpbar_hud:name_backing".asRSCM(),
            "component.hpbar_hud:inner_border".asRSCM(),
            "component.hpbar_hud:health_bar_lower_threshold".asRSCM(),
            "component.hpbar_hud:health_bar_upper_threshold".asRSCM(),
            "component.hpbar_hud:hp_bar_1".asRSCM(),
            "component.hpbar_hud:hp_bar_2".asRSCM(),
            "component.hpbar_hud:hpcontentside".asRSCM()
        )
    }

    public val commonComponents: IntArray = intArrayOf(
        "component.hpbar_hud:hp".asRSCM(),
        "component.hpbar_hud:name_area".asRSCM(),
        "component.hpbar_hud:outer_border".asRSCM(),
        "component.hpbar_hud:name_backing".asRSCM(),
        "component.hpbar_hud:creature_name".asRSCM(),
        "component.hpbar_hud:inner_border".asRSCM(),
        "component.hpbar_hud:health_bar_back".asRSCM(),
        "component.hpbar_hud:health_bar_sliding".asRSCM(),
        "component.hpbar_hud:health_bar_remaining".asRSCM(),
        "component.hpbar_hud:hp_bar_text".asRSCM(),
        "component.hpbar_hud:health_bar_lower_threshold".asRSCM(),
        "component.hpbar_hud:health_bar_upper_threshold".asRSCM(),
        "component.hpbar_hud:hp_bar_1".asRSCM(),
        "component.hpbar_hud:hp_bar_2".asRSCM(),
    )

    public companion object {
        public val ORIGINAL_COLORS: Array<Color> = arrayOf(Color(204, 0, 0), Color(149, 0, 0), Color(0, 245, 0))
    }

}
