package org.alter.impl

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object PrayerTable {

    fun skillTable() = dbTable("tables.skill_prayer") {
        column("exp", 0, arrayOf(VarType.INT))
        column("ashes", 1, arrayOf(VarType.BOOLEAN))
        
        row("dbrows.bones") {
            column(0, arrayOf(5))
            column(1, arrayOf(false))
        }

        row("dbrows.wolfbones") {
            column(0, arrayOf(5))
            column(1, arrayOf(false))
        }

        row("dbrows.burntbones") {
            column(0, arrayOf(5))
            column(1, arrayOf(false))
        }

        row("dbrows.monkeybones") {
            column(0, arrayOf(5))
            column(1, arrayOf(false))
        }

        row("dbrows.batbones") {
            column(0, arrayOf(5))
            column(1, arrayOf(false))
        }

        row("dbrows.bigbones") {
            column(0, arrayOf(15))
            column(1, arrayOf(false))
        }

        row("dbrows.jogrebones") {
            column(0, arrayOf(15))
            column(1, arrayOf(false))
        }

        row("dbrows.wyrmlingbones") {
            column(0, arrayOf(21))
            column(1, arrayOf(false))
        }

        row("dbrows.zogrebones") {
            column(0, arrayOf(23))
            column(1, arrayOf(false))
        }

        row("dbrows.shaikahanbones") {
            column(0, arrayOf(25))
            column(1, arrayOf(false))
        }

        row("dbrows.babydragonbones") {
            column(0, arrayOf(30))
            column(1, arrayOf(false))
        }

        row("dbrows.wyrmbones") {
            column(0, arrayOf(50))
            column(1, arrayOf(false))
        }

        row("dbrows.wyvernbones") {
            column(0, arrayOf(72))
            column(1, arrayOf(false))
        }

        row("dbrows.dragonbones") {
            column(0, arrayOf(72))
            column(1, arrayOf(false))
        }

        row("dbrows.drakebones") {
            column(0, arrayOf(80))
            column(1, arrayOf(false))
        }

        row("dbrows.fayrgbones") {
            column(0, arrayOf(84))
            column(1, arrayOf(false))
        }

        row("dbrows.lavadragonbones") {
            column(0, arrayOf(85))
            column(1, arrayOf(false))
        }

        row("dbrows.raurgbones") {
            column(0, arrayOf(96))
            column(1, arrayOf(false))
        }

        row("dbrows.hydrabones") {
            column(0, arrayOf(110))
            column(1, arrayOf(false))
        }

        row("dbrows.dagannothbones") {
            column(0, arrayOf(125))
            column(1, arrayOf(false))
        }

        row("dbrows.ourgbones") {
            column(0, arrayOf(140))
            column(1, arrayOf(false))
        }

        row("dbrows.superiordragonbones") {
            column(0, arrayOf(150))
            column(1, arrayOf(false))
        }

        row("dbrows.alansbones") {
            column(0, arrayOf(3))
            column(1, arrayOf(false))
        }

        row("dbrows.bonesapeatoll") {
            column(0, arrayOf(3))
            column(1, arrayOf(false))
        }

        row("dbrows.bleachedbones") {
            column(0, arrayOf(5))
            column(1, arrayOf(false))
        }

        row("dbrows.smallzombiemonkeybones") {
            column(0, arrayOf(5))
            column(1, arrayOf(false))
        }

        row("dbrows.largezombiemonkeybones") {
            column(0, arrayOf(5))
            column(1, arrayOf(false))
        }

        row("dbrows.smallninjamonkeybones") {
            column(0, arrayOf(16))
            column(1, arrayOf(false))
        }

        row("dbrows.mediumninjamonkeybones") {
            column(0, arrayOf(18))
            column(1, arrayOf(false))
        }

        row("dbrows.gorillabones") {
            column(0, arrayOf(18))
            column(1, arrayOf(false))
        }

        row("dbrows.beardedgorillabones") {
            column(0, arrayOf(18))
            column(1, arrayOf(true))
        }

        row("dbrows.fiendishashes") {
            column(0, arrayOf(10))
            column(1, arrayOf(true))
        }

        row("dbrows.vileashes") {
            column(0, arrayOf(25))
            column(1, arrayOf(true))
        }

        row("dbrows.maliciousashes") {
            column(0, arrayOf(65))
            column(1, arrayOf(true))
        }

        row("dbrows.abyssalashes") {
            column(0, arrayOf(85))
            column(1, arrayOf(true))
        }

        row("dbrows.infernalashes") {
            column(0, arrayOf(110))
            column(1, arrayOf(true))
        }

    }

}