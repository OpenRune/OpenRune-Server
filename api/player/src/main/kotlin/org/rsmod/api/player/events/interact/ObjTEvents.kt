package org.rsmod.api.player.events.interact

import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.types.ItemServerType
import org.rsmod.game.obj.Obj

public class ObjTEvents {
    public class Op(
        public val obj: Obj,
        public val comsub: Int,
        public val objType: ItemServerType?,
        component: ComponentType,
    ) : OpEvent(component.packed.toLong())

    public class Ap(
        public val obj: Obj,
        public val comsub: Int,
        public val objType: ItemServerType?,
        component: ComponentType,
    ) : ApEvent(component.packed.toLong())
}
