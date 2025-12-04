package org.alter.game.ui


@JvmInline
value class UserInterface(val id: String) {

    constructor(id: Int) : this(id.toString())

    companion object {
        val NULL = UserInterface("NULL")
    }
}