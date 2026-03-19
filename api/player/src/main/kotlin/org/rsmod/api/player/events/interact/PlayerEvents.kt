package org.rsmod.api.player.events.interact

import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.types.ItemServerType
import org.rsmod.game.entity.Player

public class PlayerEvents {
    public sealed class Op(public val target: Player) : OpDefaultEvent()

    public class Op1(target: Player) : Op(target)

    public class Op2(target: Player) : Op(target)

    public class Op3(target: Player) : Op(target)

    public class Op4(target: Player) : Op(target)

    public class Op5(target: Player) : Op(target)

    public sealed class Ap(public val target: Player) : ApDefaultEvent()

    public class Ap1(target: Player) : Ap(target)

    public class Ap2(target: Player) : Ap(target)

    public class Ap3(target: Player) : Ap(target)

    public class Ap4(target: Player) : Ap(target)

    public class Ap5(target: Player) : Ap(target)
}

public class PlayerTEvents {
    public class Op(
        public val target: Player,
        public val comsub: Int,
        public val objType: ItemServerType?,
        component: ComponentType,
    ) : OpEvent(component.packed.toLong())

    public class Ap(
        public val target: Player,
        public val comsub: Int,
        public val objType: ItemServerType?,
        component: ComponentType,
    ) : ApEvent(component.packed.toLong())
}

public class PlayerUEvents {
    public class Op(
        public val target: Player,
        public val invSlot: Int,
        public val objType: ItemServerType,
    ) : OpEvent(objType.id.toLong())

    public class Ap(
        public val target: Player,
        public val invSlot: Int,
        public val objType: ItemServerType,
    ) : ApEvent(objType.id.toLong())
}

public class PlayerUContentEvents {
    public class Op(
        public val target: Player,
        public val invSlot: Int,
        public val objType: ItemServerType,
    ) : OpEvent(objType.contentGroup.toLong())

    public class Ap(
        public val target: Player,
        public val invSlot: Int,
        public val objType: ItemServerType,
    ) : ApEvent(objType.contentGroup.toLong())
}
