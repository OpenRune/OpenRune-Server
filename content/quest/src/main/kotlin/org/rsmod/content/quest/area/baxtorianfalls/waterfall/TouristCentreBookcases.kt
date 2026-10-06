package org.rsmod.content.quest.area.baxtorianfalls.waterfall

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.generic.locs.bookcases.BookcaseSearchHook
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo

/** The ordinary bookcases upstairs in the tourist centre, beside the one hiding the book. */
class TouristCentreBookcases : BookcaseSearchHook {
    override fun claims(player: Player, bookcase: BoundLocInfo): Boolean =
        bookcase.coords.level == UpstairsLevel &&
            bookcase.coords.x in UpstairsX &&
            bookcase.coords.z in UpstairsZ

    override suspend fun ProtectedAccess.search(bookcase: BoundLocInfo) {
        searchUninterestingBooks()
    }

    private companion object {
        const val UpstairsLevel = 1
        val UpstairsX = 2514..2523
        val UpstairsZ = 3421..3433
    }
}

internal suspend fun ProtectedAccess.searchUninterestingBooks() {
    anim(SearchSeq)
    mes("You search the books...")
    delay(1)
    mes(
        random.pick(
            "You don't find anything that you'd ever want to read.",
            "You find nothing to interest you.",
            "None of them look very interesting",
        )
    )
}

private const val SearchSeq = "seq.human_pickuptable"
