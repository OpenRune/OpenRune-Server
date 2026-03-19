package org.rsmod.api.player.events.interact

import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.types.ItemServerType
import dev.openrune.types.ObjectServerType
import org.rsmod.events.EventBus
import org.rsmod.game.loc.BoundLocInfo

public sealed class LocEvents {
    /**
     * @param loc The _base_ loc target of the event.
     * @param vis The current _visual_ representation of [loc], based on the player's varps. If
     *   [loc] has a [ObjectServerType.multiLoc], [vis] reflects the resolved variant. Otherwise,
     *   [vis] is equal to [loc].
     * @param type The [ObjectServerType] of the [vis] loc.
     */
    public sealed class Op(
        public val loc: BoundLocInfo,
        public val vis: BoundLocInfo,
        public val type: ObjectServerType,
    ) : OpEvent(type.id.toLong())

    public class Op1(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType) :
        Op(loc, vis, type)

    public class Op2(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType) :
        Op(loc, vis, type)

    public class Op3(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType) :
        Op(loc, vis, type)

    public class Op4(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType) :
        Op(loc, vis, type)

    public class Op5(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType) :
        Op(loc, vis, type)

    /**
     * @param loc The _base_ loc target of the event.
     * @param vis The current _visual_ representation of [loc], based on the player's varps. If
     *   [loc] has a [ObjectServerType.multiLoc], [vis] reflects the resolved variant. Otherwise,
     *   [vis] is equal to [loc].
     * @param type The [ObjectServerType] of the [vis] loc.
     */
    public sealed class Ap(
        public val loc: BoundLocInfo,
        public val vis: BoundLocInfo,
        public val type: ObjectServerType,
    ) : ApEvent(type.id.toLong())

    public class Ap1(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType) :
        Ap(loc, vis, type)

    public class Ap2(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType) :
        Ap(loc, vis, type)

    public class Ap3(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType) :
        Ap(loc, vis, type)

    public class Ap4(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType) :
        Ap(loc, vis, type)

    public class Ap5(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType) :
        Ap(loc, vis, type)
}

public sealed class LocContentEvents {
    /**
     * @param loc The _base_ loc target of the event.
     * @param vis The current _visual_ representation of [loc], based on the player's varps. If
     *   [loc] has a [ObjectServerType.multiLoc], [vis] reflects the resolved variant. Otherwise,
     *   [vis] is equal to [loc].
     * @param type The [ObjectServerType] of the [vis] loc.
     */
    public sealed class Op(
        public val loc: BoundLocInfo,
        public val vis: BoundLocInfo,
        public val type: ObjectServerType,
        content: Int,
    ) : OpEvent(content.toLong())

    public class Op1(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType, content: Int) :
        Op(loc, vis, type, content)

    public class Op2(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType, content: Int) :
        Op(loc, vis, type, content)

    public class Op3(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType, content: Int) :
        Op(loc, vis, type, content)

    public class Op4(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType, content: Int) :
        Op(loc, vis, type, content)

    public class Op5(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType, content: Int) :
        Op(loc, vis, type, content)

    /**
     * @param loc The _base_ loc target of the event.
     * @param vis The current _visual_ representation of [loc], based on the player's varps. If
     *   [loc] has a [ObjectServerType.multiLoc], [vis] reflects the resolved variant. Otherwise,
     *   [vis] is equal to [loc].
     * @param type The [ObjectServerType] of the [vis] loc.
     */
    public sealed class Ap(
        public val loc: BoundLocInfo,
        public val vis: BoundLocInfo,
        public val type: ObjectServerType,
        content: Int,
    ) : ApEvent(content.toLong())

    public class Ap1(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType, content: Int) :
        Ap(loc, vis, type, content)

    public class Ap2(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType, content: Int) :
        Ap(loc, vis, type, content)

    public class Ap3(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType, content: Int) :
        Ap(loc, vis, type, content)

    public class Ap4(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType, content: Int) :
        Ap(loc, vis, type, content)

    public class Ap5(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType, content: Int) :
        Ap(loc, vis, type, content)
}

public sealed class LocDefaultEvents {
    /**
     * @param loc The _base_ loc target of the event.
     * @param vis The current _visual_ representation of [loc], based on the player's varps. If
     *   [loc] has a [ObjectServerType.multiLoc], [vis] reflects the resolved variant. Otherwise,
     *   [vis] is equal to [loc].
     * @param type The [ObjectServerType] of the [vis] loc.
     */
    public sealed class Op(
        public val loc: BoundLocInfo,
        public val vis: BoundLocInfo,
        public val type: ObjectServerType,
    ) : OpDefaultEvent()

    public class Op1(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType) :
        Op(loc, vis, type)

    public class Op2(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType) :
        Op(loc, vis, type)

    public class Op3(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType) :
        Op(loc, vis, type)

    public class Op4(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType) :
        Op(loc, vis, type)

    public class Op5(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType) :
        Op(loc, vis, type)

    /**
     * @param loc The _base_ loc target of the event.
     * @param vis The current _visual_ representation of [loc], based on the player's varps. If
     *   [loc] has a [ObjectServerType.multiLoc], [vis] reflects the resolved variant. Otherwise,
     *   [vis] is equal to [loc].
     * @param type The [ObjectServerType] of the [vis] loc.
     */
    public sealed class Ap(
        public val loc: BoundLocInfo,
        public val vis: BoundLocInfo,
        public val type: ObjectServerType,
    ) : ApEvent(type.id.toLong())

    public class Ap1(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType) :
        Ap(loc, vis, type)

    public class Ap2(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType) :
        Ap(loc, vis, type)

    public class Ap3(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType) :
        Ap(loc, vis, type)

    public class Ap4(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType) :
        Ap(loc, vis, type)

    public class Ap5(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType) :
        Ap(loc, vis, type)
}

public sealed class LocUnimplementedEvents {
    /**
     * @param loc The _base_ loc target of the event.
     * @param vis The current _visual_ representation of [loc], based on the player's varps. If
     *   [loc] has a [ObjectServerType.multiLoc], [vis] reflects the resolved variant. Otherwise,
     *   [vis] is equal to [loc].
     * @param type The [ObjectServerType] of the [vis] loc.
     */
    public sealed class Op(
        public val loc: BoundLocInfo,
        public val vis: BoundLocInfo,
        public val type: ObjectServerType,
    ) : OpEvent(type.id.toLong())

    public class Op1(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType) :
        Op(loc, vis, type)

    public class Op2(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType) :
        Op(loc, vis, type)

    public class Op3(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType) :
        Op(loc, vis, type)

    public class Op4(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType) :
        Op(loc, vis, type)

    public class Op5(loc: BoundLocInfo, vis: BoundLocInfo, type: ObjectServerType) :
        Op(loc, vis, type)
}

public class LocTEvents {
    /**
     * @param loc The _base_ loc target of the event.
     * @param vis The current _visual_ representation of [loc], based on the player's varps. If
     *   [loc] has a [ObjectServerType.multiLoc], [vis] reflects the resolved variant. Otherwise,
     *   [vis] is equal to [loc].
     * @param type The [ObjectServerType] of the [vis] loc.
     */
    public class Op(
        public val loc: BoundLocInfo,
        public val vis: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType?,
        public val comsub: Int,
        component: ComponentType,
    ) : OpEvent(EventBus.composeLongKey(type.id, component.packed))

    /**
     * @param loc The _base_ loc target of the event.
     * @param vis The current _visual_ representation of [loc], based on the player's varps. If
     *   [loc] has a [ObjectServerType.multiLoc], [vis] reflects the resolved variant. Otherwise,
     *   [vis] is equal to [loc].
     * @param type The [ObjectServerType] of the [vis] loc.
     */
    public class Ap(
        public val loc: BoundLocInfo,
        public val vis: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType?,
        public val comsub: Int,
        component: ComponentType,
    ) : ApEvent(EventBus.composeLongKey(type.id, component.packed))
}

public class LocTContentEvents {
    /**
     * @param loc The _base_ loc target of the event.
     * @param vis The current _visual_ representation of [loc], based on the player's varps. If
     *   [loc] has a [ObjectServerType.multiLoc], [vis] reflects the resolved variant. Otherwise,
     *   [vis] is equal to [loc].
     * @param type The [ObjectServerType] of the [vis] loc.
     */
    public class Op(
        public val loc: BoundLocInfo,
        public val vis: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType?,
        public val comsub: Int,
        component: ComponentType,
        locContent: Int = type.contentGroup,
    ) : OpEvent(EventBus.composeLongKey(locContent, component.packed))

    /**
     * @param loc The _base_ loc target of the event.
     * @param vis The current _visual_ representation of [loc], based on the player's varps. If
     *   [loc] has a [ObjectServerType.multiLoc], [vis] reflects the resolved variant. Otherwise,
     *   [vis] is equal to [loc].
     * @param type The [ObjectServerType] of the [vis] loc.
     */
    public class Ap(
        public val loc: BoundLocInfo,
        public val vis: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType?,
        public val comsub: Int,
        component: ComponentType,
        locContent: Int = type.contentGroup,
    ) : ApEvent(EventBus.composeLongKey(locContent, component.packed))
}

public class LocTDefaultEvents {
    /**
     * @param loc The _base_ loc target of the event.
     * @param vis The current _visual_ representation of [loc], based on the player's varps. If
     *   [loc] has a [ObjectServerType.multiLoc], [vis] reflects the resolved variant. Otherwise,
     *   [vis] is equal to [loc].
     * @param type The [ObjectServerType] of the [vis] loc.
     */
    public class Op(
        public val loc: BoundLocInfo,
        public val vis: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType?,
        public val comsub: Int,
        component: ComponentType,
    ) : OpEvent(component.packed.toLong())

    /**
     * @param loc The _base_ loc target of the event.
     * @param vis The current _visual_ representation of [loc], based on the player's varps. If
     *   [loc] has a [ObjectServerType.multiLoc], [vis] reflects the resolved variant. Otherwise,
     *   [vis] is equal to [loc].
     * @param type The [ObjectServerType] of the [vis] loc.
     */
    public class Ap(
        public val loc: BoundLocInfo,
        public val vis: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType?,
        public val comsub: Int,
        component: ComponentType,
    ) : ApEvent(component.packed.toLong())
}

public class LocUEvents {
    /**
     * @param loc The _base_ loc target of the event.
     * @param vis The current _visual_ representation of [loc], based on the player's varps. If
     *   [loc] has a [ObjectServerType.multiLoc], [vis] reflects the resolved variant. Otherwise,
     *   [vis] is equal to [loc].
     * @param type The [ObjectServerType] of the [vis] loc.
     */
    public class Op(
        public val loc: BoundLocInfo,
        public val vis: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType,
        public val invSlot: Int,
    ) : OpEvent(EventBus.composeLongKey(type.id, objType.id))

    /**
     * @param loc The _base_ loc target of the event.
     * @param vis The current _visual_ representation of [loc], based on the player's varps. If
     *   [loc] has a [ObjectServerType.multiLoc], [vis] reflects the resolved variant. Otherwise,
     *   [vis] is equal to [loc].
     * @param type The [ObjectServerType] of the [vis] loc.
     */
    public class Ap(
        public val loc: BoundLocInfo,
        public val vis: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType,
        public val invSlot: Int,
    ) : ApEvent(EventBus.composeLongKey(type.id, objType.id))
}

public class LocUContentEvents {
    /**
     * @param loc The _base_ loc target of the event.
     * @param vis The current _visual_ representation of [loc], based on the player's varps. If
     *   [loc] has a [ObjectServerType.multiLoc], [vis] reflects the resolved variant. Otherwise,
     *   [vis] is equal to [loc].
     * @param type The [ObjectServerType] of the [vis] loc.
     */
    public class OpType(
        public val loc: BoundLocInfo,
        public val vis: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType,
        public val invSlot: Int,
        locContent: Int = type.contentGroup,
    ) : OpEvent(EventBus.composeLongKey(locContent, objType.id))

    /**
     * @param loc The _base_ loc target of the event.
     * @param vis The current _visual_ representation of [loc], based on the player's varps. If
     *   [loc] has a [ObjectServerType.multiLoc], [vis] reflects the resolved variant. Otherwise,
     *   [vis] is equal to [loc].
     * @param type The [ObjectServerType] of the [vis] loc.
     */
    public class ApType(
        public val loc: BoundLocInfo,
        public val vis: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType,
        public val invSlot: Int,
        locContent: Int = type.contentGroup,
    ) : ApEvent(EventBus.composeLongKey(locContent, objType.id))

    /**
     * @param loc The _base_ loc target of the event.
     * @param vis The current _visual_ representation of [loc], based on the player's varps. If
     *   [loc] has a [ObjectServerType.multiLoc], [vis] reflects the resolved variant. Otherwise,
     *   [vis] is equal to [loc].
     * @param type The [ObjectServerType] of the [vis] loc.
     */
    public class OpContent(
        public val loc: BoundLocInfo,
        public val vis: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType,
        public val invSlot: Int,
        objContent: Int = objType.contentGroup,
        locContent: Int = type.contentGroup,
    ) : OpEvent(EventBus.composeLongKey(locContent, objContent))

    /**
     * @param loc The _base_ loc target of the event.
     * @param vis The current _visual_ representation of [loc], based on the player's varps. If
     *   [loc] has a [ObjectServerType.multiLoc], [vis] reflects the resolved variant. Otherwise,
     *   [vis] is equal to [loc].
     * @param type The [ObjectServerType] of the [vis] loc.
     */
    public class ApContent(
        public val loc: BoundLocInfo,
        public val vis: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType,
        public val invSlot: Int,
        objContent: Int = objType.contentGroup,
        locContent: Int = type.contentGroup,
    ) : ApEvent(EventBus.composeLongKey(locContent, objContent))
}

public class LocUDefaultEvents {
    /**
     * @param loc The _base_ loc target of the event.
     * @param vis The current _visual_ representation of [loc], based on the player's varps. If
     *   [loc] has a [ObjectServerType.multiLoc], [vis] reflects the resolved variant. Otherwise,
     *   [vis] is equal to [loc].
     * @param type The [ObjectServerType] of the [vis] loc.
     */
    public class OpType(
        public val loc: BoundLocInfo,
        public val vis: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType,
        public val invSlot: Int,
    ) : OpEvent(type.id.toLong())

    /**
     * @param loc The _base_ loc target of the event.
     * @param vis The current _visual_ representation of [loc], based on the player's varps. If
     *   [loc] has a [ObjectServerType.multiLoc], [vis] reflects the resolved variant. Otherwise,
     *   [vis] is equal to [loc].
     * @param type The [ObjectServerType] of the [vis] loc.
     */
    public class ApType(
        public val loc: BoundLocInfo,
        public val vis: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType,
        public val invSlot: Int,
    ) : ApEvent(type.id.toLong())

    /**
     * @param loc The _base_ loc target of the event.
     * @param vis The current _visual_ representation of [loc], based on the player's varps. If
     *   [loc] has a [ObjectServerType.multiLoc], [vis] reflects the resolved variant. Otherwise,
     *   [vis] is equal to [loc].
     * @param type The [ObjectServerType] of the [vis] loc.
     */
    public class OpContent(
        public val loc: BoundLocInfo,
        public val vis: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType,
        public val invSlot: Int,
        locContent: Int = type.contentGroup,
    ) : OpEvent(locContent.toLong())

    /**
     * @param loc The _base_ loc target of the event.
     * @param vis The current _visual_ representation of [loc], based on the player's varps. If
     *   [loc] has a [ObjectServerType.multiLoc], [vis] reflects the resolved variant. Otherwise,
     *   [vis] is equal to [loc].
     * @param type The [ObjectServerType] of the [vis] loc.
     */
    public class ApContent(
        public val loc: BoundLocInfo,
        public val vis: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType,
        public val invSlot: Int,
        locContent: Int = type.contentGroup,
    ) : ApEvent(locContent.toLong())
}
