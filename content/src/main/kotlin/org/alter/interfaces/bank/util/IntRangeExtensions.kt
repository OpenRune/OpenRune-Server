package org.alter.interfaces.bank.util

fun IntRange.offset(by: Int): IntRange = (by + start)..(last + by)
