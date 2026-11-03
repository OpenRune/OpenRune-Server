package org.rsmod.api.script

import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.or2.central.account.Rights
import org.rsmod.api.player.events.NpcKilledEvent
import org.rsmod.api.invtx.PlayerInvChangedEvent
import org.rsmod.api.player.events.PlayerAreaEvents
import org.rsmod.api.player.events.PlayerAttackEvents
import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.player.events.PlayerLevelEvents
import org.rsmod.api.player.events.PlayerHitpointsChangedEvent
import org.rsmod.api.player.events.PlayerMovementEvent
import org.rsmod.api.player.events.PlayerQueueEvents
import org.rsmod.api.player.events.PlayerShopEvents
import org.rsmod.api.player.events.PlayerSkillRollEvents
import org.rsmod.api.player.events.PlayerWornEvents
import org.rsmod.api.player.events.PlayerXpEvents
import org.rsmod.api.player.events.interact.PlayerInteractionEvent
import org.rsmod.api.player.events.interact.PlayerInteractionFinishedEvent
import org.rsmod.api.player.events.PlayerTimerEvent
import org.rsmod.api.player.events.interact.PlayerTEvents
import org.rsmod.api.player.events.interact.PlayerUContentEvents
import org.rsmod.api.player.events.interact.PlayerUEvents
import org.rsmod.api.player.input.DialogInput
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.PlayerButtonEvent
import org.rsmod.api.player.ui.WorldMapClick
import org.rsmod.api.player.vars.PlayerVarChangedEvent
import org.rsmod.api.player.vars.PlayerVarWatch
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.entity.player.WorldTypeChangedEvent
import org.rsmod.plugin.scripts.ScriptContext

public fun ScriptContext.onPlayerInit(action: SessionStateEvent.Initialize.() -> Unit): Unit =
    onEvent(action)

public fun ScriptContext.onPlayerLogin(action: SessionStateEvent.Login.() -> Unit): Unit =
    onEvent(action)

public fun ScriptContext.onDialogInput(action: DialogInput.() -> Unit): Unit = onEvent(action)

public fun ScriptContext.onPlayerLogout(action: SessionStateEvent.Logout.() -> Unit): Unit =
    onEvent(action)

public fun ScriptContext.onWorldTypeChanged(action: WorldTypeChangedEvent.() -> Unit): Unit =
    onEvent(action)

/* Op functions */
public fun ScriptContext.onOpPlayerT(
    component: String,
    action: suspend ProtectedAccess.(PlayerTEvents.Op) -> Unit,
): Unit = onProtectedEvent(component.asRSCM(RSCMType.COMPONENT), action)

public fun ScriptContext.onOpPlayerU(
    content: String,
    action: suspend ProtectedAccess.(PlayerUContentEvents.Op) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onOpPlayerU(
    type: ItemServerType,
    action: suspend ProtectedAccess.(PlayerUEvents.Op) -> Unit,
): Unit = onProtectedEvent(type.id, action)

/* Ap functions */
public fun ScriptContext.onApPlayerT(
    component: ComponentType,
    action: suspend ProtectedAccess.(PlayerTEvents.Ap) -> Unit,
): Unit = onProtectedEvent(component.packed, action)

public fun ScriptContext.onApPlayerU(
    content: String,
    action: suspend ProtectedAccess.(PlayerUContentEvents.Ap) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onApPlayerU(
    type: ItemServerType,
    action: suspend ProtectedAccess.(PlayerUEvents.Ap) -> Unit,
): Unit = onProtectedEvent(type.id, action)

/* Timer functions */
public fun ScriptContext.onPlayerTimer(
    timer: String,
    action: suspend ProtectedAccess.(PlayerTimerEvent.Normal) -> Unit,
): Unit = onProtectedEvent(timer.asRSCM(RSCMType.TIMER), action)

public fun ScriptContext.onPlayerSoftTimer(
    timer: String,
    action: PlayerTimerEvent.Soft.() -> Unit,
): Unit = onEvent(timer.asRSCM(RSCMType.TIMER), action)

/* Queue functions */
public fun ScriptContext.onPlayerQueue(
    queue: String,
    action: suspend ProtectedAccess.(PlayerQueueEvents.Protected<Nothing>) -> Unit,
): Unit = onProtectedEvent(queue.asRSCM(RSCMType.QUEUE), action)

public fun <T> ScriptContext.onPlayerQueueWithArgs(
    queue: String,
    action: suspend ProtectedAccess.(PlayerQueueEvents.Protected<T>) -> Unit,
): Unit = onProtectedEvent(queue.asRSCM(RSCMType.QUEUE), action)

public fun ScriptContext.onPlayerSoftQueue(
    queue: String,
    action: PlayerQueueEvents.Soft<Nothing>.() -> Unit,
): Unit = onEvent(queue.asRSCM(RSCMType.QUEUE), action)

public fun <T> ScriptContext.onPlayerSoftQueueWithArgs(
    queue: String,
    action: PlayerQueueEvents.Soft<T>.() -> Unit,
): Unit = onEvent(queue.asRSCM(RSCMType.QUEUE), action)

/* Walk trigger functions */
public fun ScriptContext.onPlayerWalkTrigger(
    trigger: String,
    action: PlayerMovementEvent.WalkTrigger.() -> Unit,
): Unit = onEvent(trigger.asRSCM(RSCMType.WALKTRIGGER), action)

public fun ScriptContext.onPlayerCoordsChanged(
    action: PlayerMovementEvent.CoordsMovedEvent.() -> Unit
): Unit = onEvent(action)

public fun ScriptContext.onPlayerHitpointsChanged(
    action: PlayerHitpointsChangedEvent.() -> Unit
): Unit = onEvent(action)

public fun ScriptContext.onPlayerButton(action: PlayerButtonEvent.() -> Unit): Unit =
    onEvent(action)

public fun ScriptContext.onPlayerInteraction(action: PlayerInteractionEvent.() -> Unit): Unit =
    onEvent(action)

public fun ScriptContext.onPlayerInteractionFinished(
    action: PlayerInteractionFinishedEvent.() -> Unit
): Unit = onEvent(action)

public fun ScriptContext.onNpcKilled(action: NpcKilledEvent.() -> Unit): Unit = onEvent(action)

public fun ScriptContext.onPlayerInvChanged(action: PlayerInvChangedEvent.() -> Unit): Unit =
    onEvent(action)

/** [name] is a varp or varbit symbol, such as `"varbit.prayer_smite"`. */
public fun ScriptContext.onVarChanged(name: String, action: PlayerVarChangedEvent.() -> Unit) {
    val watch = PlayerVarWatch.watch(name)
    onEvent<PlayerVarChangedEvent> { watch.matching(this)?.action() }
}

public fun ScriptContext.onPlayerSkillRollModify(
    action: PlayerSkillRollEvents.Modify.() -> Unit
): Unit = onEvent(action)

public fun ScriptContext.onPlayerSkillRoll(action: PlayerSkillRollEvents.Result.() -> Unit): Unit =
    onEvent(action)

public fun ScriptContext.onPlayerLevelUp(action: PlayerLevelEvents.Advanced.() -> Unit): Unit =
    onEvent(action)

public fun ScriptContext.onPlayerCombatLevelUp(
    action: PlayerLevelEvents.CombatAdvanced.() -> Unit
): Unit = onEvent(action)

public fun ScriptContext.onPlayerStatChanged(
    action: PlayerLevelEvents.StatChanged.() -> Unit
): Unit = onEvent(action)

/** Observes [area] being entered. Unlike `onArea`, any number of scripts can subscribe. */
public fun ScriptContext.onPlayerAreaEntered(
    area: String,
    action: PlayerAreaEvents.Entered.() -> Unit,
) {
    val id = area.asRSCM(RSCMType.AREA)
    onEvent<PlayerAreaEvents.Entered> { if (this.area == id) action() }
}

/** Observes [area] being left. Unlike `onAreaExit`, any number of scripts can subscribe. */
public fun ScriptContext.onPlayerAreaExited(
    area: String,
    action: PlayerAreaEvents.Exited.() -> Unit,
) {
    val id = area.asRSCM(RSCMType.AREA)
    onEvent<PlayerAreaEvents.Exited> { if (this.area == id) action() }
}

public fun ScriptContext.onPlayerShopBuyPrepare(
    action: PlayerShopEvents.BuyPrepare.() -> Unit
): Unit = onEvent(action)

public fun ScriptContext.onPlayerShopSellPrepare(
    action: PlayerShopEvents.SellPrepare.() -> Unit
): Unit = onEvent(action)

public fun ScriptContext.onPlayerShopBought(action: PlayerShopEvents.Bought.() -> Unit): Unit =
    onEvent(action)

public fun ScriptContext.onPlayerShopSold(action: PlayerShopEvents.Sold.() -> Unit): Unit =
    onEvent(action)

public fun ScriptContext.onPlayerEquip(action: PlayerWornEvents.Equip.() -> Unit): Unit =
    onEvent(action)

public fun ScriptContext.onPlayerUnequip(action: PlayerWornEvents.Unequip.() -> Unit): Unit =
    onEvent(action)

public fun ScriptContext.onPlayerXpModify(action: PlayerXpEvents.Modify.() -> Unit): Unit =
    onEvent(action)

public fun ScriptContext.onPlayerXpGain(action: PlayerXpEvents.Gain.() -> Unit): Unit =
    onEvent(action)

public fun ScriptContext.onPlayerHit(action: PlayerHitEvents.Impact.() -> Unit): Unit =
    onEvent(action)

public fun ScriptContext.onPlayerAttackImpact(
    action: PlayerAttackEvents.HitImpact.() -> Unit
): Unit = onEvent(action)

public fun ScriptContext.onPlayerDamageRoll(
    action: PlayerAttackEvents.DamageRoll.() -> Unit
): Unit = onEvent(action)

public fun ScriptContext.onPlayerAttackResult(
    action: PlayerAttackEvents.AttackResult.() -> Unit
): Unit = onEvent(action)

public fun ScriptContext.onWorldMapClick(
    requiredRights: Rights,
    action: suspend ProtectedAccess.(WorldMapClick) -> Unit,
): Unit =
    onProtectedEvent(WorldMapClick.BUS_ID) { event: WorldMapClick ->
        if (!player.modLevel.isAtLeast(requiredRights)) {
            return@onProtectedEvent
        }
        action(event)
    }
