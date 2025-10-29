package org.alter.game.pluginnew

enum class MenuOption(val id: Int) {
    OP0(0),
    OP1(1),
    OP2(2),
    OP3(3),
    OP4(4),
    OP5(5),
    OP6(6),
    OP7(7),
    OP8(8),
    OP9(9),
    OP10(10),
    OP11(11);

    companion object {
        fun fromId(id: Int): MenuOption = entries.find { it.id == id } ?: error("Invalid object option id: $id")
    }
}
