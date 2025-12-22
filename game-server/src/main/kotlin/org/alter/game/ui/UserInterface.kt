package org.alter.game.ui

import dev.openrune.cache.filestore.definition.InterfaceType


@JvmInline
public value class UserInterface(public val id: Int) {
    public constructor(type: InterfaceType) : this(type.id)

    public companion object {
        public val NULL: UserInterface = UserInterface(-1)
    }
}
