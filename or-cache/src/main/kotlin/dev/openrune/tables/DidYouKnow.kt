package dev.openrune.tables

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType


object DidYouKnow {

    const val COL_IS_TIP = 0
    const val COL_TIP = 1
    const val COL_MEMBERS_ONLY = 2
    const val COL_ON_MOBILE = 3
    const val COL_MOBILE_ONLY = 4
    const val COL_COL_EXTRA_REQ = 5

    fun didYouknow() = dbTable("dbtable.didyouknow", serverOnly = true) {

        column("istip", COL_IS_TIP, VarType.BOOLEAN)
        column("tip", COL_TIP, VarType.STRING)
        column("membersonly", COL_MEMBERS_ONLY, VarType.BOOLEAN)
        column("onmobile", COL_ON_MOBILE, VarType.BOOLEAN)
        column("mobileonly", COL_MOBILE_ONLY, VarType.BOOLEAN)
        column("extrareq", COL_COL_EXTRA_REQ, VarType.INT)

        row("dbrow.didyouknow_bankhelp") {
            column(COL_TIP, "You can hide the help button in the Bank interface via the Bank settings.")
        }

        row("dbrow.didyouknow_certproduce") {
            column(COL_TIP, "You can note produce by using it on the Tool Leprechaun.")
        }

        row("dbrow.didyouknow_fairyring_desktop") {
            column(COL_TIP, "You can right-click any fairy ring to travel to any other fairy ring without passing through Zanaris.")
        }

        row("dbrow.didyouknow_fairyring_mobile") {
            column(COL_TIP, "You can long press any fairy ring to travel to any other fairy ring without passing through Zanaris.")
            column(COL_MOBILE_ONLY, true)
        }

        row("dbrow.didyouknow_phials_f2p") {
            column(COL_TIP, "There's a man called Phials in Rimmington that will unnote items for a small fee. Perfect for Ultimate Ironmen.")
        }

        row("dbrow.didyouknow_phials") {
            column(COL_TIP, "There's a man called Phials in Rimmington that will unnote items for a small fee, right next to a house portal. Perfect for any Construction needs.")
        }

        row("dbrow.didyouknow_roofs") {
            column(COL_TIP, "If you find entering buildings tricky because the roof is in the way, you can hide roofs at all time by toggling the hide roofs setting in the Settings menu.")
        }

        row("dbrow.didyouknow_lockslot") {
            column(COL_TIP, "There is a setting in the Bank settings and on the Bank deposit interface that lets you lock slots in your inventory so their content doesn't get deposited.")
        }

        row("dbrow.didyouknow_tickeat") {
            column(COL_TIP, "You can eat a food item, drink a potion and eat a cooked karambwan in the same tick, as long as you do it in that order.")
        }

        row("dbrow.didyouknow_viyeldi_cave_barrels") {
            column(COL_TIP, "You can smash barrels in the Viyeldi caves for a chance at a random treasure, including noted cooked sharks.")
        }

        row("dbrow.didyouknow_virtual_levelling") {
            column(COL_TIP, "You can set 'virtual' level targets above 99 in the XP tracker, with the maximum level being 126.")
        }

        row("dbrow.didyouknow_pizzahalves") {
            column(COL_TIP, "You can put two halves of a pizza together to make a whole pizza!")
        }

        row("dbrow.didyouknow_kebabcompost") {
            column(COL_TIP, "Kebabs are one of the items you can use on a compost bin to make regular compost.")
        }

        row("dbrow.didyouknow_poisoncure2_desktop") {
            column(COL_TIP, "If you hold any anti-poison or anti-venom potions, clicking on the health orb while poisoned will cure you without cancelling your actions.")
        }

        row("dbrow.didyouknow_poisoncure2_mobile") {
            column(COL_TIP, "If you hold any anti-poison or anti-venom potions, tapping on the health orb while poisoned will cure you without cancelling your actions.")
            column(COL_MOBILE_ONLY, true)
        }

        row("dbrow.didyouknow_combine_jewellery") {
            column(COL_TIP, "You can combine enchanted jewellery items, as you would decant potions, by taking them to Murky Matt at the Grand Exchange.")
        }

        row("dbrow.didyouknow_combine_potions") {
            column(COL_TIP, "You can decant potions into any number of doses by taking them to Bob Barter at the Grand Exchange or Zahur in Nardah.")
        }

        row("dbrow.didyouknow_humidify") {
            column(COL_TIP, "You can use the Humidify Lunar spell to water all of your saplings at once.")
        }

        row("dbrow.didyouknow_tempoross_leave_desktop") {
            column(COL_TIP, "You can right-click any of the Spirit Anglers to leave a Tempoross fight faster once it has ended.")
        }

        row("dbrow.didyouknow_tempoross_leave_mobile") {
            column(COL_TIP, "You can long press any of the Spirit Anglers to leave a Tempoross fight faster once it has ended.")
            column(COL_MOBILE_ONLY, true)
        }

        row("dbrow.didyouknow_glory_gems") {
            column(COL_TIP, "Wearing a charged Amulet of Glory while mining will increase the rate at which you find gems.")
        }

        row("dbrow.didyouknow_snowflake_basalt") {
            column(COL_TIP, "You can use basalt on Snowflake in Weiss to note it.")
        }

        row("dbrow.didyouknow_farminggear") {
            column(COL_TIP, "You can buy basic farming gear, including plant cure, by talking to farmers near farming patches.")
        }

        row("dbrow.didyouknow_minimap_hide") {
            column(COL_TIP, "There is a setting in the Settings menu to hide the Minimap.")
        }

        row("dbrow.didyouknow_unkahfire") {
            column(COL_TIP, "The fire in the Ruins of Unkah is actually considered a range.")
        }

        row("dbrow.didyouknow_dorgesh_kalphite") {
            column(COL_TIP, "You can enter the Kalphite Lair from the caves south of Dorgesh-Kaan. Bring a light source!")
        }

        row("dbrow.didyouknow_snelms") {
            column(COL_TIP, "Not all snelms give the same damage protection against snail acid attacks. Red snelms and pointed blue snelms are the most effective.")
        }

        row("dbrow.didyouknow_strange_fruit") {
            column(COL_TIP, "Eating a strange fruit cures poison and venom as well as restoring run energy.")
        }

        row("dbrow.didyouknow_twinflame_staff") {
            column(COL_TIP, "The Twinflame staff from Royal Titans will automatically cast the correct elemental spell for a foe's weakness.")
        }

        row("dbrow.didyouknow_boxtrap") {
            column(COL_TIP, "If you stand on the same tile as your box trap, it will fail to catch 100% of the time.")
        }

        row("dbrow.didyouknow_hunter_fail") {
            column(COL_TIP, "You can attempt to catch most Hunter creatures you don't have the Hunter level to catch. You will just fail 100% of the time.")
        }

        row("dbrow.didyouknow_bolt_pouch") {
            column(COL_TIP, "You can buy a bolt pouch from Hirko in Keldagrim that lets you carry up to 5 different bolt types at once, including a mithril grapple.")
        }

        row("dbrow.didyouknow_supercompost") {
            column(COL_TIP, "You can upgrade regular compost in buckets and bins to supercompost by using a compost potion on them.")
        }

        row("dbrow.didyouknow_runsetting") {
            column(COL_TIP, "There is a setting in the Settings menu to automatically re-enable running once you have regained enough energy. This will however interrupt any action in progress.")
        }

        row("dbrow.didyouknow_house_viewer") {
            column(COL_TIP, "Thanks to the House Viewer in the House options, you can move and rotate rooms in your house without changing their content.")
        }

        row("dbrow.didyouknow_hitsplat_threshold") {
            column(COL_TIP, "By default, the smallest max hitsplat shown is 10, but there is a setting to change that in the Settings menu.")
        }

        row("dbrow.didyouknow_ii_strength_xp") {
            column(COL_TIP, "There is a setting to allow you to get Strength XP when pushing through the wheat in Puro-Puro.")
        }

        row("dbrow.didyouknow_smash_vials") {
            column(COL_TIP, "There is an option in the Settings menu to automatically smash empty vials once you have finished emptying them.")
        }

        row("dbrow.didyouknow_smash_pots") {
            column(COL_TIP, "There is an option in the Settings menu to automatically smash empty pots once you have finished planting them.")
        }

        row("dbrow.didyouknow_zammy_grapes") {
            column(COL_TIP, "You can pick Zamorak grapes from the Vinery with Bologa's blessing, a Tithe Farm reward that can be unlocked for a fee by talking to Bologa while wearing a Zamorak item.")
        }

        row("dbrow.didyouknow_makeover") {
            column(COL_TIP, "You can change any aspect of your appearance at any makeover provider by using the button in the top left corner of any makeover interface.")
        }

        row("dbrow.didyouknow_peek_desktop") {
            column(COL_TIP, "Most bosses which are not instanced have a right-click 'peek' option on or near their entrance that lets you see how many people are currently fighting it.")
        }

        row("dbrow.didyouknow_peek_mobile") {
            column(COL_TIP, "Most bosses which are not instanced have a long press 'peek' option on or near their entrance that lets you see how many people are currently fighting it.")
            column(COL_MOBILE_ONLY, true)
        }

        row("dbrow.didyouknow_sailing_boat_bottles") {
            column(COL_TIP, "Boats can be stored in boat bottles, letting you carry them from one port to another.")
        }

        row("dbrow.didyouknow_sailing_boat_escaping") {
            column(COL_TIP, "Boats have an escape option on their helm, allowing you to return to dry land if you're ever in trouble.")
        }

        row("dbrow.didyouknow_sailing_shipwrights") {
            column(COL_TIP, "As well as selling you new boats and helping you customize them, shipwrights can also help you recover a lost boat or destroy a boat you no longer need.")
        }

        row("dbrow.didyouknow_sailing_with_friends") {
            column(COL_TIP, "You can invite friends onto your boat and even allow them to navigate it for you. Set a friend as a navigator by using the Sailing sidepanel, or by using your Captain's log on them.")
        }

        row("dbrow.didyouknow_sailing_repair_kits") {
            column(COL_TIP, "Boat repair kits can be used to fix up a damaged boat. You can make boat repair kits at a shipwrights' workbench, which are found near all shipwrights.")
        }
    }
}
