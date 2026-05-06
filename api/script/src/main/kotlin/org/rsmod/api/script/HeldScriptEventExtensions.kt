package org.rsmod.api.script

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import org.rsmod.api.player.events.interact.HeldContentEvents
import org.rsmod.api.player.events.interact.HeldDropEvents
import org.rsmod.api.player.events.interact.HeldEquipEvents
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.events.interact.HeldUContentEvents
import org.rsmod.api.player.events.interact.HeldUDefaultEvents
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.events.EventBus
import org.rsmod.plugin.scripts.ScriptContext

/* Drop functions */
public fun ScriptContext.onDropTrigger(type: String, action: HeldDropEvents.Trigger.() -> Unit) {
    onEvent(type.asRSCM(RSCMType.DROP_TRIGGER), action)
}

/* Equip functions */
public fun ScriptContext.onContentEquipObj(
    content: String,
    action: HeldEquipEvents.Equip.() -> Unit,
): Unit = onEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onContentUnequipObj(
    content: String,
    action: HeldEquipEvents.Unequip.() -> Unit,
): Unit = onEvent(content.asRSCM(RSCMType.CONTENT), action)

/* Standard obj op functions */
public fun ScriptContext.onOpHeld1(
    type: String,
    action: suspend ProtectedAccess.(HeldObjEvents.Op1) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.OBJ), action)

public fun ScriptContext.onOpHeld1(
    type: ItemServerType,
    action: suspend ProtectedAccess.(HeldObjEvents.Op1) -> Unit,
): Unit = onOpHeld1(RSCM.getReverseMapping(RSCMType.OBJ, type.id), action)

/** **Important Note:** This replaces the default wield/wear op handling for obj [type]. */
public fun ScriptContext.onOpHeld2(
    type: String,
    action: suspend ProtectedAccess.(HeldObjEvents.Op2) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.OBJ), action)

/** **Important Note:** This replaces the default wield/wear op handling for obj [type]. */
public fun ScriptContext.onOpHeld2(
    type: ItemServerType,
    action: suspend ProtectedAccess.(HeldObjEvents.Op2) -> Unit,
): Unit = onOpHeld2(RSCM.getReverseMapping(RSCMType.OBJ, type.id), action)

public fun ScriptContext.onOpHeld3(
    type: String,
    action: suspend ProtectedAccess.(HeldObjEvents.Op3) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.OBJ), action)

public fun ScriptContext.onOpHeld3(
    type: ItemServerType,
    action: suspend ProtectedAccess.(HeldObjEvents.Op3) -> Unit,
): Unit = onOpHeld3(RSCM.getReverseMapping(RSCMType.OBJ, type.id), action)

public fun ScriptContext.onOpHeld4(
    type: String,
    action: suspend ProtectedAccess.(HeldObjEvents.Op4) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.OBJ), action)

public fun ScriptContext.onOpHeld4(
    type: ItemServerType,
    action: suspend ProtectedAccess.(HeldObjEvents.Op4) -> Unit,
): Unit = onOpHeld4(RSCM.getReverseMapping(RSCMType.OBJ, type.id), action)

/** **Important Note:** This replaces the default drop op handling for obj [type]. */
public fun ScriptContext.onOpHeld5(
    type: String,
    action: suspend ProtectedAccess.(HeldObjEvents.Op5) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.OBJ), action)

/** **Important Note:** This replaces the default drop op handling for obj [type]. */
public fun ScriptContext.onOpHeld5(
    type: ItemServerType,
    action: suspend ProtectedAccess.(HeldObjEvents.Op5) -> Unit,
): Unit = onOpHeld5(RSCM.getReverseMapping(RSCMType.OBJ, type.id), action)

/* Standard content op functions */
public fun ScriptContext.onOpContentHeld1(
    content: String,
    action: suspend ProtectedAccess.(HeldContentEvents.Op1) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

/**
 * **Important Note:** This replaces the default wield/wear op handling for content group [content].
 */
public fun ScriptContext.onOpContentHeld2(
    content: String,
    action: suspend ProtectedAccess.(HeldContentEvents.Op2) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onOpContentHeld3(
    content: String,
    action: suspend ProtectedAccess.(HeldContentEvents.Op3) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onOpContentHeld4(
    content: String,
    action: suspend ProtectedAccess.(HeldContentEvents.Op4) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

/** **Important Note:** This replaces the default drop op handling for content group [content]. */
public fun ScriptContext.onOpContentHeld5(
    content: String,
    action: suspend ProtectedAccess.(HeldContentEvents.Op5) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

/* HeldU (inv obj on inv obj) functions */
/**
 * Registers a script that triggers when an inventory obj ([first]) is used on another inventory obj
 * ([second]).
 *
 * The [HeldUEvents.Type.first] and [HeldUEvents.Type.second] values passed to the script will
 * **always match** the registration order in this function. That is, [first] will always correspond
 * to `HeldUEvents.Type.first`, and [second] to `HeldUEvents.Type.second`, regardless of which obj
 * the player uses on the other in-game.
 */
public fun ScriptContext.onOpHeldU(
    first: String,
    second: String,
    action: suspend ProtectedAccess.(HeldUEvents.Type) -> Unit,
) {
    // Note: We preserve the order of `first` and `second` when registering to expose a predictable
    // and fixed order in the respective script. Because of this, we can't rely on the event bus to
    // catch duplicate registrations - we must manually check that the reversed combination has
    // not already been registered.
    val firstId = first.asRSCM(RSCMType.OBJ)
    val secondId = second.asRSCM(RSCMType.OBJ)
    val opposite = EventBus.composeLongKey(secondId, firstId)
    val registeredOpposite = eventBus.contains(HeldUEvents.Type::class.java, opposite)
    if (registeredOpposite) {
        val message = "OpHeldU for combination already registered: first=$second, second=$first"
        throw IllegalStateException(message)
    }
    onProtectedEvent(EventBus.composeLongKey(firstId, secondId), action)
}

public fun ScriptContext.onOpHeldU(
    first: ItemServerType,
    second: ItemServerType,
    action: suspend ProtectedAccess.(HeldUEvents.Type) -> Unit,
): Unit =
    onOpHeldU(
        RSCM.getReverseMapping(RSCMType.OBJ, first.id),
        RSCM.getReverseMapping(RSCMType.OBJ, second.id),
        action,
    )

public fun ScriptContext.onOpHeldU(
    first: String,
    second: ItemServerType,
    action: suspend ProtectedAccess.(HeldUEvents.Type) -> Unit,
): Unit =
    onOpHeldU(first, RSCM.getReverseMapping(RSCMType.OBJ, second.id), action)

public fun ScriptContext.onOpHeldU(
    first: ItemServerType,
    second: String,
    action: suspend ProtectedAccess.(HeldUEvents.Type) -> Unit,
): Unit =
    onOpHeldU(RSCM.getReverseMapping(RSCMType.OBJ, first.id), second, action)

/**
 * Registers a script that triggers when an inventory obj ([first]) is used on another inventory obj
 * ([second]).
 *
 * The [HeldUContentEvents.Type.first] and [HeldUContentEvents.Type.second] values passed to the
 * script will **always match** the registration order in this function. That is, [first] will
 * always correspond to `HeldUContentEvents.Type.first`, and [second] to
 * `HeldUContentEvents.Type.second`, regardless of which obj the player uses on the other in-game.
 */
public fun ScriptContext.onOpContentHeldU(
    first: String,
    second: ItemServerType,
    action: suspend ProtectedAccess.(HeldUContentEvents.Type) -> Unit,
): Unit =
    onProtectedEvent(EventBus.composeLongKey(first.asRSCM(RSCMType.CONTENT), second.id), action)

/**
 * Registers a script that triggers when an inventory obj ([first]) is used on another inventory obj
 * ([second]).
 *
 * The [HeldUContentEvents.Content.first] and [HeldUContentEvents.Content.second] values passed to
 * the script will **always match** the registration order in this function. That is, [first] will
 * always correspond to `HeldUContentEvents.Content.first`, and [second] to
 * `HeldUContentEvents.Content.second`, regardless of which obj the player uses on the other
 * in-game.
 */
public fun ScriptContext.onOpContentHeldU(
    first: String,
    second: String,
    action: suspend ProtectedAccess.(HeldUContentEvents.Content) -> Unit,
) {
    // Note: We preserve the order of `first` and `second` when registering to expose a predictable
    // and fixed order in the respective script. Because of this, we can't rely on the event bus to
    // catch duplicate registrations - we must manually check that the reversed combination has
    // not already been registered.
    val opposite =
        EventBus.composeLongKey(second.asRSCM(RSCMType.CONTENT), first.asRSCM(RSCMType.CONTENT))
    val registeredOpposite = eventBus.contains(HeldUContentEvents.Content::class.java, opposite)
    if (registeredOpposite) {
        val message = "OpHeldU for combination already registered: first=$second, second=$first"
        throw IllegalStateException(message)
    }
    onProtectedEvent(
        EventBus.composeLongKey(
            first.asRSCM(RSCMType.CONTENT),
            second.asRSCM(RSCMType.CONTENT),
        ),
        action,
    )
}

/**
 * Registers a script that triggers when an inventory obj ([first]) is used on _any other_ inventory
 * obj.
 *
 * The [HeldUDefaultEvents.Type.first] value passed to the script will **always** be [first], while
 * the target obj will be [HeldUDefaultEvents.Type.second].
 */
public fun ScriptContext.onOpHeldU(
    first: String,
    action: suspend ProtectedAccess.(HeldUDefaultEvents.Type) -> Unit,
): Unit = onProtectedEvent(first.asRSCM(RSCMType.OBJ).toLong(), action)

public fun ScriptContext.onOpHeldU(
    first: ItemServerType,
    action: suspend ProtectedAccess.(HeldUDefaultEvents.Type) -> Unit,
): Unit = onOpHeldU(RSCM.getReverseMapping(RSCMType.OBJ, first.id), action)

/**
 * Registers a script that triggers when an inventory obj ([first]) is used on _any other_ inventory
 * obj.
 *
 * The [HeldUDefaultEvents.Content.first] value passed to the script will **always** be [first],
 * while the target obj will be [HeldUDefaultEvents.Content.second].
 */
public fun ScriptContext.onOpContentHeldU(
    first: String,
    action: suspend ProtectedAccess.(HeldUDefaultEvents.Content) -> Unit,
): Unit = onProtectedEvent(first.asRSCM(RSCMType.CONTENT).toLong(), action)
