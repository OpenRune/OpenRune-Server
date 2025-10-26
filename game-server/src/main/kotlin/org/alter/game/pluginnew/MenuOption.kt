package org.alter.game.pluginnew

enum class MenuOption(val id: Int) {
    OP1(0),
    OP2(1),
    OP3(2),
    OP4(3),
    OP5(4),
    OP6(5),
    OP7(7),
    OP8(8),
    OP9(9),
    OP10(10),
    OP11(11);

    companion object {
        fun fromId(id: Int): MenuOption = entries.find { it.id == id } ?: error("Invalid object option id: $id")
    }
}
