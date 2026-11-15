package org.rsmod.content.travel.teleports

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Kharedst's memoirs and the Book of the Dead: torn pages from the Great Kourend quests unlock
 * one Reminisce destination each and add [CHARGES_PER_PAGE] charges, and the Old Memorial
 * recharges the book for a law, body, mind and soul rune per charge.
 */
class MemoirsScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (page in Memoirs.PAGES) {
            for (book in Memoirs.BOOKS) {
                onOpHeldU(page.obj, book) { event ->
                    val pageSlot =
                        if (event.first.isType(page.obj)) event.firstSlot else event.secondSlot
                    with(Memoirs) { addPage(page, book, pageSlot) }
                }
            }
        }
        onOpLoc1(MEMORIAL) { inspect() }
        onOpLocU(MEMORIAL) { event ->
            val book = RSCM.getReverseMapping(RSCMType.OBJ, event.objType.id)
            if (book in Memoirs.BOOKS) {
                inspect()
            } else {
                mes("Nothing interesting happens.")
            }
        }
    }

    private suspend fun ProtectedAccess.inspect() {
        mesbox(QUOTE)
        val book = Memoirs.BOOKS.firstOrNull { inv.contains(it) || worn.contains(it) } ?: return
        if (with(Memoirs) { pagesAdded() } == 0) {
            return
        }
        val added = with(Memoirs) { recharge(book) }
        when {
            added > 0 -> {
                statAdvance(STAT_MAGIC, XP_PER_CHARGE * added)
                // UNVERIFIED: recharge message wording
                mes("You recharge the book with $added charges.")
            }
            added == 0 ->
                mes("You need a law, body, mind and soul rune for each charge you want to add.")
        }
    }

    private companion object {
        private const val MEMORIAL = "loc.kourendwoodland_statue"
        private const val STAT_MAGIC = "stat.magic"
        private const val XP_PER_CHARGE = 10.0
        private const val QUOTE =
            "<col=800080>'Obey the Laws of your Body, Mind and Soul'</col> - King Kharedst IV"
    }
}

/** The item and charge changes behind [MemoirsScript], kept apart so they can be tested. */
object Memoirs {
    const val CHARGES_PER_PAGE = 20
    private const val BOOK_OF_THE_DEAD_CAP = 250
    private const val CHARGES = "varbit.veos_memoir_charges"

    const val MEMOIRS = "obj.veos_kharedsts_memoirs"
    const val BOOK_OF_THE_DEAD = "obj.book_of_the_dead"
    val BOOKS = listOf(MEMOIRS, BOOK_OF_THE_DEAD)

    val RUNES = listOf("obj.lawrune", "obj.bodyrune", "obj.mindrune", "obj.soulrune")

    data class Page(val obj: String, val varbit: String)

    val PAGES =
        listOf(
            Page("obj.veos_memoirs_hos_page", "varbit.hosidius_page"),
            Page("obj.veos_memoirs_pisc_page", "varbit.piscarilius_page"),
            Page("obj.veos_memoirs_shay_page", "varbit.shayzien_page"),
            Page("obj.veos_memoirs_lova_page", "varbit.lovakengj_page"),
            Page("obj.veos_memoirs_arc_page", "varbit.arceuus_page"),
        )

    fun ProtectedAccess.pagesAdded(): Int = PAGES.count { vars[it.varbit] != 0 }

    /** The most charges [book] can hold: [CHARGES_PER_PAGE] per page, or the Book of the Dead's. */
    fun ProtectedAccess.cap(book: String): Int =
        if (book == BOOK_OF_THE_DEAD) BOOK_OF_THE_DEAD_CAP else CHARGES_PER_PAGE * pagesAdded()

    /**
     * Takes the page in [pageSlot] and adds it to the book: its varbit is set and the book gains
     * [CHARGES_PER_PAGE] charges up to the new cap. Returns `false`, changing nothing, when the
     * page is already in the book or no longer in that slot.
     */
    fun ProtectedAccess.addPage(page: Page, book: String, pageSlot: Int): Boolean {
        if (vars[page.varbit] != 0) {
            mes("You have already added that page to the book.")
            return false
        }
        if (!invDel(inv, page.obj, count = 1, slot = pageSlot).success) {
            return false
        }
        vars[page.varbit] = 1
        vars[CHARGES] = minOf(vars[CHARGES] + CHARGES_PER_PAGE, cap(book))
        // UNVERIFIED: add-page message wording
        mes("You add the page to the book.")
        return true
    }

    /**
     * Fills [book] as far as the runes in the inventory and its cap allow, taking one of each of
     * [RUNES] per charge in a single transaction. Returns the charges added, `0` when no charge
     * fits or could be paid for, or `-1` when the book is already full.
     */
    fun ProtectedAccess.recharge(book: String): Int {
        val room = cap(book) - vars[CHARGES]
        if (room <= 0) {
            mes("Your book is already fully charged.")
            return -1
        }
        val count = minOf(room, RUNES.minOf { inv.count(it) })
        if (count <= 0) {
            return 0
        }
        val result =
            player.invTransaction(inv) {
                val from = select(inv)
                for (rune in RUNES) {
                    delete(from, RSCM.getRSCM(rune), count)
                }
            }
        if (!result.success) {
            return 0
        }
        vars[CHARGES] = vars[CHARGES] + count
        return count
    }
}
