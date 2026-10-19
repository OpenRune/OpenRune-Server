package org.rsmod.content.quest.area.baxtorianfalls.waterfall

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.generic.locs.bookcases.BookcaseSearchHook
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo

class TouristCentreBookcases : BookcaseSearchHook {
    override fun claims(player: Player, bookcase: BoundLocInfo): Boolean =
        bookcase.coords.level == UPSTAIRS_LEVEL &&
            bookcase.coords.x in UPSTAIRS_X &&
            bookcase.coords.z in UPSTAIRS_Z

    override suspend fun ProtectedAccess.search(bookcase: BoundLocInfo) {
        searchUninterestingBooks()
    }

    private companion object {
        const val UPSTAIRS_LEVEL = 1
        val UPSTAIRS_X = 2514..2523
        val UPSTAIRS_Z = 3421..3433
    }
}

internal suspend fun ProtectedAccess.searchUninterestingBooks() {
    anim(SEARCH_SEQ)
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

private const val SEARCH_SEQ = "seq.human_pickuptable"
