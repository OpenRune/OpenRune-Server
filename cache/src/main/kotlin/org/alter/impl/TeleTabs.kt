package org.alter.impl

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object TeleTabs {

    fun teleTabs() = dbTable("tables.teleport_tablets") {
        column("item", 0, arrayOf(VarType.OBJ))
        column("magic_level", 1, arrayOf(VarType.INT))
        column("location", 2, arrayOf(VarType.INT))

        // Standard Teleport Tablets
        row("dbrows.varrock_teleport_tablet") {
            columnRSCM(0, arrayOf("items.poh_tablet_varrockteleport"))
            column(1, arrayOf(25))
            column(2, arrayOf(1))
        }
        row("dbrows.lumbridge_teleport_tablet") {
            columnRSCM(0, arrayOf("items.poh_tablet_lumbridgeteleport"))
            column(1, arrayOf(31))
            column(2, arrayOf(1))
        }
        row("dbrows.falador_teleport_tablet") {
            columnRSCM(0, arrayOf("items.poh_tablet_faladorteleport"))
            column(1, arrayOf(37))
            column(2, arrayOf(1))
        }
        row("dbrows.camelot_teleport_tablet") {
            columnRSCM(0, arrayOf("items.poh_tablet_camelotteleport"))
            column(1, arrayOf(45))
            column(2, arrayOf(1))
        }
        row("dbrows.kourend_castle_teleport_tablet") {
            columnRSCM(0, arrayOf("items.poh_tablet_kourendteleport"))
            column(1, arrayOf(48))
            column(2, arrayOf(1))
        }
        row("dbrows.ardougne_teleport_tablet") {
            columnRSCM(0, arrayOf("items.poh_tablet_ardougneteleport"))
            column(1, arrayOf(51))
            column(2, arrayOf(1))
        }
        row("dbrows.civitas_illa_fortis_teleport_tablet") {
            columnRSCM(0, arrayOf("items.poh_tablet_fortisteleport"))
            column(1, arrayOf(54))
            column(2, arrayOf(1))
        }
        row("dbrows.watchtower_teleport_tablet") {
            columnRSCM(0, arrayOf("items.poh_tablet_watchtowerteleport"))
            column(1, arrayOf(58))
            column(2, arrayOf(1))
        }

        // Arceuus Teleport Tablets
        row("dbrows.arceuus_library_teleport_tablet") {
            columnRSCM(0, arrayOf("items.teletab_lumbridge"))
            column(1, arrayOf(6))
            column(2, arrayOf(1))
        }
        row("dbrows.draynor_manor_teleport_tablet") {
            columnRSCM(0, arrayOf("items.teletab_draynor"))
            column(1, arrayOf(17))
            column(2, arrayOf(1))
        }
        row("dbrows.battlefront_teleport_tablet") {
            columnRSCM(0, arrayOf("items.teletab_battlefront"))
            column(1, arrayOf(23))
            column(2, arrayOf(1))
        }
        row("dbrows.mind_altar_teleport_tablet") {
            columnRSCM(0, arrayOf("items.teletab_mind_altar"))
            column(1, arrayOf(28))
            column(2, arrayOf(1))
        }
        row("dbrows.salve_graveyard_teleport_tablet") {
            columnRSCM(0, arrayOf("items.teletab_salve"))
            column(1, arrayOf(40))
            column(2, arrayOf(1))
        }
        row("dbrows.fenkenstrains_castle_teleport_tablet") {
            columnRSCM(0, arrayOf("items.teletab_fenk"))
            column(1, arrayOf(48))
            column(2, arrayOf(1))
        }
        row("dbrows.west_ardougne_teleport_tablet") {
            columnRSCM(0, arrayOf("items.teletab_westardy"))
            column(1, arrayOf(61))
            column(2, arrayOf(1))
        }
        row("dbrows.harmony_island_teleport_tablet") {
            columnRSCM(0, arrayOf("items.teletab_harmony"))
            column(1, arrayOf(65))
            column(2, arrayOf(1))
        }
        row("dbrows.cemetery_teleport_tablet") {
            columnRSCM(0, arrayOf("items.teletab_cemetery"))
            column(1, arrayOf(71))
            column(2, arrayOf(1))
        }
        row("dbrows.barrows_teleport_tablet") {
            columnRSCM(0, arrayOf("items.teletab_barrows"))
            column(1, arrayOf(83))
            column(2, arrayOf(1))
        }
        row("dbrows.ape_atoll_teleport_tablet") {
            columnRSCM(0, arrayOf("items.teletab_ape"))
            column(1, arrayOf(90))
            column(2, arrayOf(1))
        }

        // Ancient Magicks Teleport Tablets
        row("dbrows.paddewwa_teleport_tablet") {
            columnRSCM(0, arrayOf("items.tablet_paddewa"))
            column(1, arrayOf(54))
            column(2, arrayOf(1))
        }
        row("dbrows.senntisten_teleport_tablet") {
            columnRSCM(0, arrayOf("items.tablet_senntisten"))
            column(1, arrayOf(60))
            column(2, arrayOf(1))
        }
        row("dbrows.kharyrll_teleport_tablet") {
            columnRSCM(0, arrayOf("items.tablet_kharyll"))
            column(1, arrayOf(66))
            column(2, arrayOf(1))
        }
        row("dbrows.lassar_teleport_tablet") {
            columnRSCM(0, arrayOf("items.tablet_lassar"))
            column(1, arrayOf(72))
            column(2, arrayOf(1))
        }
        row("dbrows.dareeyak_teleport_tablet") {
            columnRSCM(0, arrayOf("items.tablet_dareeyak"))
            column(1, arrayOf(78))
            column(2, arrayOf(1))
        }
        row("dbrows.carrallanger_teleport_tablet") {
            columnRSCM(0, arrayOf("items.tablet_carrallangar"))
            column(1, arrayOf(84))
            column(2, arrayOf(1))
        }
        row("dbrows.annakarl_teleport_tablet") {
            columnRSCM(0, arrayOf("items.tablet_annakarl"))
            column(1, arrayOf(90))
            column(2, arrayOf(1))
        }
        row("dbrows.ghorrock_teleport_tablet") {
            columnRSCM(0, arrayOf("items.tablet_ghorrock"))
            column(1, arrayOf(96))
            column(2, arrayOf(1))
        }

        // Lunar Teleport Tablets
        row("dbrows.moonclan_teleport_tablet") {
            columnRSCM(0, arrayOf("items.lunar_tablet_moonclan_teleport"))
            column(1, arrayOf(69))
            column(2, arrayOf(1))
        }
        row("dbrows.ourania_teleport_tablet") {
            columnRSCM(0, arrayOf("items.lunar_tablet_ourania_teleport"))
            column(1, arrayOf(71))
            column(2, arrayOf(1))
        }
        row("dbrows.waterbirth_teleport_tablet") {
            columnRSCM(0, arrayOf("items.lunar_tablet_waterbirth_teleport"))
            column(1, arrayOf(72))
            column(2, arrayOf(1))
        }
        row("dbrows.barbarian_teleport_tablet") {
            columnRSCM(0, arrayOf("items.lunar_tablet_barbarian_teleport"))
            column(1, arrayOf(75))
            column(2, arrayOf(1))
        }
        row("dbrows.khazard_teleport_tablet") {
            columnRSCM(0, arrayOf("items.lunar_tablet_khazard_teleport"))
            column(1, arrayOf(78))
            column(2, arrayOf(1))
        }
        row("dbrows.fishing_guild_teleport_tablet") {
            columnRSCM(0, arrayOf("items.lunar_tablet_fishing_guild_teleport"))
            column(1, arrayOf(85))
            column(2, arrayOf(1))
        }
        row("dbrows.catherby_teleport_tablet") {
            columnRSCM(0, arrayOf("items.lunar_tablet_catherby_teleport"))
            column(1, arrayOf(87))
            column(2, arrayOf(1))
        }
        row("dbrows.ice_plateau_teleport_tablet") {
            columnRSCM(0, arrayOf("items.lunar_tablet_ice_plateau_teleport"))
            column(1, arrayOf(89))
            column(2, arrayOf(1))
        }
    }
}