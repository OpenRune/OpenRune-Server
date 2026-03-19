package org.rsmod.api.npc.events.interact

import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.types.ItemServerType
import dev.openrune.types.ObjectServerType
import org.rsmod.events.EventBus
import org.rsmod.game.loc.BoundLocInfo

public sealed class AiLocEvents {
    public sealed class Op(public val loc: BoundLocInfo, public val type: ObjectServerType) :
        OpEvent(type.id.toLong())

    public class Op1(loc: BoundLocInfo, type: ObjectServerType) : Op(loc, type)

    public class Op2(loc: BoundLocInfo, type: ObjectServerType) : Op(loc, type)

    public class Op3(loc: BoundLocInfo, type: ObjectServerType) : Op(loc, type)

    public class Op4(loc: BoundLocInfo, type: ObjectServerType) : Op(loc, type)

    public class Op5(loc: BoundLocInfo, type: ObjectServerType) : Op(loc, type)

    public sealed class Ap(public val loc: BoundLocInfo, public val type: ObjectServerType) :
        ApEvent(type.id.toLong())

    public class Ap1(loc: BoundLocInfo, type: ObjectServerType) : Ap(loc, type)

    public class Ap2(loc: BoundLocInfo, type: ObjectServerType) : Ap(loc, type)

    public class Ap3(loc: BoundLocInfo, type: ObjectServerType) : Ap(loc, type)

    public class Ap4(loc: BoundLocInfo, type: ObjectServerType) : Ap(loc, type)

    public class Ap5(loc: BoundLocInfo, type: ObjectServerType) : Ap(loc, type)
}

public sealed class AiLocContentEvents {
    public sealed class Op(
        public val loc: BoundLocInfo,
        public val type: ObjectServerType,
        content: Int,
    ) : OpEvent(content.toLong())

    public class Op1(loc: BoundLocInfo, type: ObjectServerType, content: Int) :
        Op(loc, type, content)

    public class Op2(loc: BoundLocInfo, type: ObjectServerType, content: Int) :
        Op(loc, type, content)

    public class Op3(loc: BoundLocInfo, type: ObjectServerType, content: Int) :
        Op(loc, type, content)

    public class Op4(loc: BoundLocInfo, type: ObjectServerType, content: Int) :
        Op(loc, type, content)

    public class Op5(loc: BoundLocInfo, type: ObjectServerType, content: Int) :
        Op(loc, type, content)

    public sealed class Ap(
        public val loc: BoundLocInfo,
        public val type: ObjectServerType,
        content: Int,
    ) : ApEvent(content.toLong())

    public class Ap1(loc: BoundLocInfo, type: ObjectServerType, content: Int) :
        Ap(loc, type, content)

    public class Ap2(loc: BoundLocInfo, type: ObjectServerType, content: Int) :
        Ap(loc, type, content)

    public class Ap3(loc: BoundLocInfo, type: ObjectServerType, content: Int) :
        Ap(loc, type, content)

    public class Ap4(loc: BoundLocInfo, type: ObjectServerType, content: Int) :
        Ap(loc, type, content)

    public class Ap5(loc: BoundLocInfo, type: ObjectServerType, content: Int) :
        Ap(loc, type, content)
}

public sealed class AiLocDefaultEvents {
    public sealed class Op(public val loc: BoundLocInfo, public val type: ObjectServerType) :
        OpDefaultEvent()

    public class Op1(loc: BoundLocInfo, type: ObjectServerType) : Op(loc, type)

    public class Op2(loc: BoundLocInfo, type: ObjectServerType) : Op(loc, type)

    public class Op3(loc: BoundLocInfo, type: ObjectServerType) : Op(loc, type)

    public class Op4(loc: BoundLocInfo, type: ObjectServerType) : Op(loc, type)

    public class Op5(loc: BoundLocInfo, type: ObjectServerType) : Op(loc, type)

    public sealed class Ap(public val loc: BoundLocInfo, public val type: ObjectServerType) :
        ApEvent(type.id.toLong())

    public class Ap1(loc: BoundLocInfo, type: ObjectServerType) : Ap(loc, type)

    public class Ap2(loc: BoundLocInfo, type: ObjectServerType) : Ap(loc, type)

    public class Ap3(loc: BoundLocInfo, type: ObjectServerType) : Ap(loc, type)

    public class Ap4(loc: BoundLocInfo, type: ObjectServerType) : Ap(loc, type)

    public class Ap5(loc: BoundLocInfo, type: ObjectServerType) : Ap(loc, type)
}

public sealed class AiLocUnimplementedEvents {
    public sealed class Op(public val loc: BoundLocInfo, public val type: ObjectServerType) :
        OpEvent(type.id.toLong())

    public class Op1(loc: BoundLocInfo, type: ObjectServerType) : Op(loc, type)

    public class Op2(loc: BoundLocInfo, type: ObjectServerType) : Op(loc, type)

    public class Op3(loc: BoundLocInfo, type: ObjectServerType) : Op(loc, type)

    public class Op4(loc: BoundLocInfo, type: ObjectServerType) : Op(loc, type)

    public class Op5(loc: BoundLocInfo, type: ObjectServerType) : Op(loc, type)
}

public class AiLocTEvents {
    public class Op(
        public val loc: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType?,
        public val comsub: Int,
        component: ComponentType,
    ) : OpEvent(EventBus.composeLongKey(type.id, component.packed))

    public class Ap(
        public val loc: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType?,
        public val comsub: Int,
        component: ComponentType,
    ) : ApEvent(EventBus.composeLongKey(type.id, component.packed))
}

public class AiLocTContentEvents {
    public class Op(
        public val loc: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType?,
        public val comsub: Int,
        component: ComponentType,
        locContent: Int = type.contentGroup,
    ) : OpEvent(EventBus.composeLongKey(locContent, component.packed))

    public class Ap(
        public val loc: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType?,
        public val comsub: Int,
        component: ComponentType,
        locContent: Int = type.contentGroup,
    ) : ApEvent(EventBus.composeLongKey(locContent, component.packed))
}

public class AiLocTDefaultEvents {
    public class Op(
        public val loc: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType?,
        public val comsub: Int,
        component: ComponentType,
    ) : OpEvent(component.packed.toLong())

    public class Ap(
        public val loc: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType?,
        public val comsub: Int,
        component: ComponentType,
    ) : ApEvent(component.packed.toLong())
}

public class AiLocUEvents {
    public class Op(
        public val loc: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType,
        public val invSlot: Int,
    ) : OpEvent(EventBus.composeLongKey(type.id, objType.id))

    public class Ap(
        public val loc: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType,
        public val invSlot: Int,
    ) : ApEvent(EventBus.composeLongKey(type.id, objType.id))
}

public class AiLocUContentEvents {
    public class OpType(
        public val loc: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType,
        public val invSlot: Int,
        locContent: Int = type.contentGroup,
    ) : OpEvent(EventBus.composeLongKey(locContent, objType.id))

    public class ApType(
        public val loc: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType,
        public val invSlot: Int,
        locContent: Int = type.contentGroup,
    ) : ApEvent(EventBus.composeLongKey(locContent, objType.id))

    public class OpContent(
        public val loc: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType,
        public val invSlot: Int,
        objContent: Int = objType.contentGroup,
        locContent: Int = type.contentGroup,
    ) : OpEvent(EventBus.composeLongKey(locContent, objContent))

    public class ApContent(
        public val loc: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType,
        public val invSlot: Int,
        objContent: Int = objType.contentGroup,
        locContent: Int = type.contentGroup,
    ) : ApEvent(EventBus.composeLongKey(locContent, objContent))
}

public class AiLocUDefaultEvents {
    public class OpType(
        public val loc: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType,
        public val invSlot: Int,
    ) : OpEvent(type.id.toLong())

    public class ApType(
        public val loc: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType,
        public val invSlot: Int,
    ) : ApEvent(type.id.toLong())

    public class OpContent(
        public val loc: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType,
        public val invSlot: Int,
        locContent: Int = type.contentGroup,
    ) : OpEvent(locContent.toLong())

    public class ApContent(
        public val loc: BoundLocInfo,
        public val type: ObjectServerType,
        public val objType: ItemServerType,
        public val invSlot: Int,
        locContent: Int = type.contentGroup,
    ) : ApEvent(locContent.toLong())
}
