package org.rsmod.content.bosses.brutus

import jakarta.inject.Inject
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcDeathDropContext
import org.rsmod.api.death.NpcDeathDropHook
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.death.PlayerDeathContext
import org.rsmod.api.death.PlayerDeathHandling
import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.death.UntradeableHandling
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.quest.area.lumbridge.idesofmilk.IdesOfMilkQuest
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.module.PluginModule

class BrutusModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerDeathHook>(BrutusSafeDeath::class.java)
        addSetBinding<NpcDeathDropHook>(BrutusQuestDrops::class.java)
        addSetBinding<NpcDeathKillHook>(BrutusQuestKill::class.java)
        addSetBinding<NpcDeathKillHook>(BrutusKillTally::class.java)
        addSetBinding<NpcAttackValidateHook>(BrutusPenAttack::class.java)
    }
}

private const val INSTANCE_KEY = "cowboss"

private fun InstanceManager.inBrutusField(player: Player): Boolean =
    sessionForPlayer(player)?.key == INSTANCE_KEY

internal fun InstanceManager.fieldToWorld(session: InstanceSession, coords: CoordGrid): CoordGrid {
    val copy = resolveCoord(session, FIELD_REFERENCE) ?: return session.placement.exitCoord
    return coords.translate(FIELD_REFERENCE.x - copy.x, FIELD_REFERENCE.z - copy.z)
}

private val FIELD_REFERENCE = CoordGrid(3261, 3287)

private fun IdesOfMilkQuest.onQuest(player: Player): Boolean =
    stage(player) < IdesOfMilkQuest.STAGE_COMPLETE

// TODO(content): [system:graves] Move unprotected items to a grave outside the field.
class BrutusSafeDeath
@Inject
constructor(
    private val manager: InstanceManager,
    private val idesOfMilk: IdesOfMilkQuest,
    private val worldStats: BrutusWorldStats,
) : PlayerDeathHook {
    override val priority: Int = 10

    override fun handleDeath(context: PlayerDeathContext): PlayerDeathHandling? {
        val player = context.player
        if (!context.inInstance || !manager.inBrutusField(player)) {
            return null
        }
        // UNVERIFIED: whether deaths in the quest fight count on the scoreboard (kills don't).
        if (!idesOfMilk.onQuest(player)) {
            VarPlayerIntMapSetter.set(player, BrutusScoreboard.DEATHS, player.vars[BrutusScoreboard.DEATHS] + 1)
            worldStats.addDeath()
        }
        return PlayerDeathHandling(
            keepCount = Int.MAX_VALUE,
            dropReceiver = null,
            dropDuration = 0,
            revealDelay = 0,
            supplyPile = false,
            untradeableHandling = UntradeableHandling.KEEP,
            isSafe = true,
        )
    }
}

class BrutusQuestDrops
@Inject
constructor(private val manager: InstanceManager, private val idesOfMilk: IdesOfMilkQuest) :
    NpcDeathDropHook {
    override fun tryConsume(context: NpcDeathDropContext): Boolean =
        questKill(context.hero)

    override fun suppressesDropTable(context: NpcDeathKillContext): Boolean =
        context.npc.isType(Brutus.BRUTUS) && questKill(context.hero)

    private fun questKill(hero: Player): Boolean =
        manager.inBrutusField(hero) && idesOfMilk.onQuest(hero)
}

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
            val outside = manager.fieldToWorld(session, player.coords)
            manager.leave(player, session, clock.cycle)
            telejump(outside, TeleportType.Exempt)
        }
    }

    private companion object {
        private const val TAG_TEXT =
            "You notice a strange tag float to the ground as the bull falls. It reads 'Brutus'."
    }
}
