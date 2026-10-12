package org.rsmod.content.areas.city.burthorpe.npcs.warriorsguild

import org.rsmod.api.player.dialogue.Dialogue

internal const val CLAIM_TOKENS = "May I claim my tokens please?"

// TODO(content): [system:warriors_guild_tokens] token ledger; staff hand out earned tokens
//  ("Of course! Here you go, you've earned [number] tokens!") and refuse with a full pack
internal suspend fun Dialogue.claimTokens(
    refusal: String = "I'm afraid you have not earned any tokens yet. Try some of the " +
        "activities around the guild to earn some.",
    reply: String? = "Ok, I'll go see what I can find.",
) {
    chatPlayer(quiz, CLAIM_TOKENS)
    chatNpc(neutral, refusal)
    if (reply != null) {
        chatPlayer(neutral, reply)
    }
}

internal fun Dialogue.isMale(): Boolean = access.isBodyTypeA()
