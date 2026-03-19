package org.rsmod.api.player.events.interact

import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.types.ItemServerType
import dev.openrune.types.NpcServerType
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc

public sealed class NpcEvents {
    public sealed class Op(public val npc: Npc) : OpEvent(npc.id.toLong())

    public class Op1(npc: Npc) : Op(npc)

    public class Op2(npc: Npc) : Op(npc)

    public class Op3(npc: Npc) : Op(npc)

    public class Op4(npc: Npc) : Op(npc)

    public class Op5(npc: Npc) : Op(npc)

    public sealed class Ap(public val npc: Npc) : ApEvent(npc.id.toLong())

    public class Ap1(npc: Npc) : Ap(npc)

    public class Ap2(npc: Npc) : Ap(npc)

    public class Ap3(npc: Npc) : Ap(npc)

    public class Ap4(npc: Npc) : Ap(npc)

    public class Ap5(npc: Npc) : Ap(npc)
}

public sealed class NpcContentEvents {
    public sealed class Op(public val npc: Npc, contentGroup: Int) : OpEvent(contentGroup.toLong())

    public class Op1(npc: Npc, category: Int) : Op(npc, category)

    public class Op2(npc: Npc, category: Int) : Op(npc, category)

    public class Op3(npc: Npc, category: Int) : Op(npc, category)

    public class Op4(npc: Npc, category: Int) : Op(npc, category)

    public class Op5(npc: Npc, category: Int) : Op(npc, category)

    public sealed class Ap(public val npc: Npc, contentGroup: Int) : ApEvent(contentGroup.toLong())

    public class Ap1(npc: Npc, category: Int) : Ap(npc, category)

    public class Ap2(npc: Npc, category: Int) : Ap(npc, category)

    public class Ap3(npc: Npc, category: Int) : Ap(npc, category)

    public class Ap4(npc: Npc, category: Int) : Ap(npc, category)

    public class Ap5(npc: Npc, category: Int) : Ap(npc, category)
}

public sealed class NpcDefaultEvents {
    public sealed class Op(public val npc: Npc) : OpDefaultEvent()

    public class Op1(npc: Npc) : Op(npc)

    public class Op2(npc: Npc) : Op(npc)

    public class Op3(npc: Npc) : Op(npc)

    public class Op4(npc: Npc) : Op(npc)

    public class Op5(npc: Npc) : Op(npc)

    public sealed class Ap(public val npc: Npc) : ApDefaultEvent()

    public class Ap1(npc: Npc) : Ap(npc)

    public class Ap2(npc: Npc) : Ap(npc)

    public class Ap3(npc: Npc) : Ap(npc)

    public class Ap4(npc: Npc) : Ap(npc)

    public class Ap5(npc: Npc) : Ap(npc)
}

public sealed class NpcUnimplementedEvents {
    public sealed class Op(public val npc: Npc) : OpEvent(npc.id.toLong())

    public class Op1(npc: Npc) : Op(npc)

    public class Op2(npc: Npc) : Op(npc)

    public class Op3(npc: Npc) : Op(npc)

    public class Op4(npc: Npc) : Op(npc)

    public class Op5(npc: Npc) : Op(npc)
}

public class NpcTEvents {
    public class Op(
        public val npc: Npc,
        public val comsub: Int,
        public val objType: ItemServerType?,
        npcType: NpcServerType,
        component: ComponentType,
    ) : OpEvent(EventBus.composeLongKey(npcType.id, component.packed))

    public class Ap(
        public val npc: Npc,
        public val comsub: Int,
        public val objType: ItemServerType?,
        npcType: NpcServerType,
        component: ComponentType,
    ) : ApEvent(EventBus.composeLongKey(npcType.id, component.packed))
}

public class NpcTContentEvents {
    public class Op(
        public val npc: Npc,
        public val comsub: Int,
        public val objType: ItemServerType?,
        component: ComponentType,
        content: Int,
    ) : OpEvent(EventBus.composeLongKey(content, component.packed))

    public class Ap(
        public val npc: Npc,
        public val comsub: Int,
        public val objType: ItemServerType?,
        component: ComponentType,
        content: Int,
    ) : ApEvent(EventBus.composeLongKey(content, component.packed))
}

public class NpcTDefaultEvents {
    public class Op(
        public val npc: Npc,
        public val comsub: Int,
        public val objType: ItemServerType?,
        public val npcType: NpcServerType,
        component: ComponentType,
    ) : OpEvent(component.packed.toLong())

    public class Ap(
        public val npc: Npc,
        public val comsub: Int,
        public val objType: ItemServerType?,
        public val npcType: NpcServerType,
        component: ComponentType,
    ) : ApEvent(component.packed.toLong())
}

public class NpcUEvents {
    public class Op(
        public val npc: Npc,
        public val invSlot: Int,
        public val objType: ItemServerType,
        npcType: NpcServerType,
    ) : OpEvent(EventBus.composeLongKey(npcType.id, objType.id))

    public class Ap(
        public val npc: Npc,
        public val invSlot: Int,
        public val objType: ItemServerType,
        npcType: NpcServerType,
    ) : ApEvent(EventBus.composeLongKey(npcType.id, objType.id))
}

public class NpcUContentEvents {
    public class Op(
        public val npc: Npc,
        public val invSlot: Int,
        public val objType: ItemServerType,
        content: Int,
    ) : OpEvent(EventBus.composeLongKey(content, objType.id))

    public class Ap(
        public val npc: Npc,
        public val invSlot: Int,
        public val objType: ItemServerType,
        content: Int,
    ) : ApEvent(EventBus.composeLongKey(content, objType.id))
}

public class NpcUDefaultEvents {
    public class OpType(
        public val npc: Npc,
        public val invSlot: Int,
        public val objType: ItemServerType,
        npcType: NpcServerType,
    ) : OpEvent(npcType.id.toLong())

    public class ApType(
        public val npc: Npc,
        public val invSlot: Int,
        public val objType: ItemServerType,
        npcType: NpcServerType,
    ) : ApEvent(npcType.id.toLong())

    public class OpContent(
        public val npc: Npc,
        public val invSlot: Int,
        public val objType: ItemServerType,
        content: Int,
    ) : OpEvent(content.toLong())

    public class ApContent(
        public val npc: Npc,
        public val invSlot: Int,
        public val objType: ItemServerType,
        content: Int,
    ) : ApEvent(content.toLong())
}
