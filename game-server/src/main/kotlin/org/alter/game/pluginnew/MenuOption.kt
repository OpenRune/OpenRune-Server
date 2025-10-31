package org.alter.game.pluginnew

enum class MenuOption(val id: Int) {
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
    OP11(11),
    OP12(12),
    OP13(13),
    OP14(14),
    OP15(15),
    OP16(16),
    OP17(17),
    OP18(18),
    OP19(19),
    OP20(20),
    OP21(21),
    OP22(22),
    OP23(23),
    OP24(24),
    OP25(25),
    OP26(26),
    OP27(27),
    OP28(28),
    OP29(29),
    OP30(30),
    OP31(31),
    OP32(32);

    companion object {
        private val lookup = entries.associateBy(MenuOption::id)
        fun fromId(id: Int): MenuOption =
            lookup[id] ?: error("Invalid object option id: $id (expected 1–32)")
    }
}