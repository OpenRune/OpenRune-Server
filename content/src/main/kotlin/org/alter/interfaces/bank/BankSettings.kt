package org.alter.interfaces.bank

enum class QuantityMode(val varValue: Int) {
    One(0),
    Five(1),
    Ten(2),
    X(3),
    All(4),
}

enum class TabDisplayMode(val varValue: Int) {
    Obj(0),
    Digit(1),
    Roman(2),
}

enum class BankFillerMode(val varValue: Int) {
    All(0),
    One(1),
    Ten(2),
    Fifty(3),
    X(4),
}
