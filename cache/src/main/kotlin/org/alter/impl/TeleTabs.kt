package org.alter.impl

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object TeleTabs {

    const val COL_ITEM = 0
    const val COL_MAGIC_LEVEL = 1
    const val COL_LOCATION = 2

    fun teleTabs() = dbTable("tables.teleport_tablets") {

        column("item", COL_ITEM, VarType.OBJ)
        column("magic_level", COL_MAGIC_LEVEL, VarType.INT)
        column("location", COL_LOCATION, VarType.INT)

        // Standard Teleport Tablets
        row("dbrows.varrock_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.poh_tablet_varrockteleport")
            column(COL_MAGIC_LEVEL, 25)
        }
        row("dbrows.lumbridge_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.poh_tablet_lumbridgeteleport")
            column(COL_MAGIC_LEVEL, 31)
            column(COL_LOCATION, 52776082)
        }
        row("dbrows.falador_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.poh_tablet_faladorteleport")
            column(COL_MAGIC_LEVEL, 37)
            column(COL_LOCATION, 48565554)
        }
        row("dbrows.camelot_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.poh_tablet_camelotteleport")
            column(COL_MAGIC_LEVEL, 45)
            column(COL_LOCATION, 45174167)
        }
        row("dbrows.kourend_castle_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.poh_tablet_kourendteleport")
            column(COL_MAGIC_LEVEL, 48)
            column(COL_LOCATION, 26857052)
        }
        row("dbrows.ardougne_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.poh_tablet_ardougneteleport")
            column(COL_MAGIC_LEVEL, 51)
            column(COL_LOCATION, 43617513)
        }
        row("dbrows.civitas_illa_fortis_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.poh_tablet_fortisteleport")
            column(COL_MAGIC_LEVEL, 54)
            column(COL_LOCATION, 27544637)
        }
        row("dbrows.watchtower_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.poh_tablet_watchtowerteleport")
            column(COL_MAGIC_LEVEL, 58)
        }

        // Arceuus Teleport Tablets
        row("dbrows.arceuus_library_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.teletab_lumbridge")
            column(COL_MAGIC_LEVEL, 6)
            column(COL_LOCATION, 26758910)
        }
        row("dbrows.draynor_manor_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.teletab_draynor")
            column(COL_MAGIC_LEVEL, 17)
            column(COL_LOCATION, 50924825)
        }
        row("dbrows.battlefront_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.teletab_battlefront")
            column(COL_MAGIC_LEVEL, 23)
            column(COL_LOCATION, 22072988)
        }
        row("dbrows.mind_altar_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.teletab_mind_altar")
            column(COL_MAGIC_LEVEL, 28)
            column(COL_LOCATION, 48827829)
        }
        row("dbrows.salve_graveyard_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.teletab_salve")
            column(COL_MAGIC_LEVEL, 40)
            column(COL_LOCATION, 56233349)
        }
        row("dbrows.fenkenstrains_castle_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.teletab_fenk")
            column(COL_MAGIC_LEVEL, 48)
            column(COL_LOCATION, 58133961)
        }
        row("dbrows.west_ardougne_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.teletab_westardy")
            column(COL_MAGIC_LEVEL, 61)
            column(COL_LOCATION, 40963291)
        }
        row("dbrows.harmony_island_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.teletab_harmony")
            column(COL_MAGIC_LEVEL, 65)
            column(COL_LOCATION, 62212915)
        }
        row("dbrows.cemetery_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.teletab_cemetery")
            column(COL_MAGIC_LEVEL, 71)
            column(COL_LOCATION, 48828083)
        }
        row("dbrows.barrows_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.teletab_barrows")
            column(COL_MAGIC_LEVEL, 83)
            column(COL_LOCATION, 58412274)
        }
        row("dbrows.ape_atoll_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.teletab_ape")
            column(COL_MAGIC_LEVEL, 90)
            column(COL_LOCATION, 45409166)
        }

        // Ancient Magicks Teleport Tablets
        row("dbrows.paddewwa_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.tablet_paddewa")
            column(COL_MAGIC_LEVEL, 54)
            column(COL_LOCATION, 50800283)
        }
        row("dbrows.senntisten_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.tablet_senntisten")
            column(COL_MAGIC_LEVEL, 60)
            column(COL_LOCATION, 54398217)
        }
        row("dbrows.kharyrll_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.tablet_kharyll")
            column(COL_MAGIC_LEVEL, 66)
            column(COL_LOCATION, 57249169)
        }
        row("dbrows.lassar_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.tablet_lassar")
            column(COL_MAGIC_LEVEL, 72)
            column(COL_LOCATION, 49188238)
        }
        row("dbrows.dareeyak_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.tablet_dareeyak")
            column(COL_MAGIC_LEVEL, 78)
            column(COL_LOCATION, 48631408)
        }
        row("dbrows.carrallanger_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.tablet_carrallangar")
            column(COL_MAGIC_LEVEL, 84)
            column(COL_LOCATION, 51744338)
        }
        row("dbrows.annakarl_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.tablet_annakarl")
            column(COL_MAGIC_LEVEL, 90)
            column(COL_LOCATION, 53858096)
        }
        row("dbrows.ghorrock_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.tablet_ghorrock")
            column(COL_MAGIC_LEVEL, 96)
            column(COL_LOCATION, 48729889)
        }

        // Lunar Teleport Tablets
        row("dbrows.moonclan_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.lunar_tablet_moonclan_teleport")
            column(COL_MAGIC_LEVEL, 69)
            column(COL_LOCATION, 34623307)
        }
        row("dbrows.ourania_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.lunar_tablet_ourania_teleport")
            column(COL_MAGIC_LEVEL, 71)
            column(COL_LOCATION, 40438958)
        }
        row("dbrows.waterbirth_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.lunar_tablet_waterbirth_teleport")
            column(COL_MAGIC_LEVEL, 72)
            column(COL_LOCATION, 41717420)
        }
        row("dbrows.barbarian_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.lunar_tablet_barbarian_teleport")
            column(COL_MAGIC_LEVEL, 75)
            column(COL_LOCATION, 41668081)
        }
        row("dbrows.khazard_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.lunar_tablet_khazard_teleport")
            column(COL_MAGIC_LEVEL, 78)
            column(COL_LOCATION, 43191391)
        }
        row("dbrows.fishing_guild_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.lunar_tablet_fishing_guild_teleport")
            column(COL_MAGIC_LEVEL, 85)
            column(COL_LOCATION, 42814783)
        }
        row("dbrows.catherby_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.lunar_tablet_catherby_teleport")
            column(COL_MAGIC_LEVEL, 87)
            column(COL_LOCATION, 45895033)
        }
        row("dbrows.ice_plateau_teleport_tablet") {
            columnRSCM(COL_ITEM, "items.lunar_tablet_ice_plateau_teleport")
            column(COL_MAGIC_LEVEL, 89)
            column(COL_LOCATION, 48746338)
        }
    }
}