package org.rsmod.content.bosses.brutus

import jakarta.inject.Inject
import org.rsmod.api.death.NpcDeathDropContext
import org.rsmod.api.death.NpcDeathDropHook
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.death.PlayerDeathContext
import org.rsmod.api.death.PlayerDeathHandling
import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.death.UntradeableHandling
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.content.quest.area.lumbridge.idesofmilk.IdesOfMilkQuest
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.plugin.module.PluginModule

class BrutusModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerDeathHook>(BrutusSafeDeath::class.java)
        addSetBinding<NpcDeathDropHook>(BrutusQuestDrops::class.java)
        addSetBinding<NpcDeathKillHook>(BrutusQuestKill::class.java)
    }
}

private const val INSTANCE_KEY = "cowboss"

private fun InstanceManager.inBrutusField(player: Player): Boolean =
    sessionForPlayer(player)?.key == INSTANCE_KEY

private fun IdesOfMilkQuest.onQuest(player: Player): Boolean =
    stage(player) < IdesOfMilkQuest.STAGE_COMPLETE

/**
 * Keeps every item on a death in the field.
 *
 * TODO(content): [system:graves] Live moves unprotected items to a grave outside the field
 * (https://oldschool.runescape.wiki/w/Brutus); this keeps them until graves exist.
 */
class BrutusSafeDeath @Inject constructor(private val manager: InstanceManager) : PlayerDeathHook {
    override val priority: Int = 10

    override fun handleDeath(context: PlayerDeathContext): PlayerDeathHandling? {
        if (!context.inInstance || !manager.inBrutusField(context.player)) {
            return null
        }
        return PlayerDeathHandling(
            keepCount = Int.MAX_VALUE,
            dropReceiver = null,
            dropDuration = 0,
            revealDelay = 0,
            supplyPile = false,
            untradeableHandling = UntradeableHandling.KEEP,
        )
    }
}

/** The quest kill drops nothing (https://oldschool.runescape.wiki/w/Brutus). */
class BrutusQuestDrops
@Inject
constructor(private val manager: InstanceManager, private val idesOfMilk: IdesOfMilkQuest) :
    NpcDeathDropHook {
    override fun tryConsume(context: NpcDeathDropContext): Boolean =
        manager.inBrutusField(context.hero) && idesOfMilk.onQuest(context.hero)
}

/**
 * The quest kill reveals the bull's name and takes the player back out.
 * Live capture, rev 241, 20261012T180256-df49f48@1236-1243.
 */
class BrutusQuestKill
@Inject
constructor(
    private val manager: InstanceManager,
    private val idesOfMilk: IdesOfMilkQuest,
    private val launcher: ProtectedAccessLauncher,
    private val clock: MapClock,
) : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        if (!context.npc.isType(Brutus.BRUTUS)) {
            return
        }
        val hero = context.hero
        if (idesOfMilk.stage(hero) !in IdesOfMilkQuest.STAGE_BULL_TASK until
                IdesOfMilkQuest.STAGE_BRUTUS_KILLED
        ) {
            return
        }
        launcher.launch(hero) {
            idesOfMilk.quest.setQuestStage(this, IdesOfMilkQuest.STAGE_BRUTUS_KILLED)
            mes(TAG_TEXT)
            startDialogue { mesbox(TAG_TEXT) }
            val session = manager.sessionForPlayer(player) ?: return@launch
            val exit = manager.leave(player, session, clock.cycle)
            telejump(exit, TeleportType.Exempt)
        }
    }

    private companion object {
        private const val TAG_TEXT =
            "You notice a strange tag float to the ground as the bull falls. It reads 'Brutus'."
    }
}
