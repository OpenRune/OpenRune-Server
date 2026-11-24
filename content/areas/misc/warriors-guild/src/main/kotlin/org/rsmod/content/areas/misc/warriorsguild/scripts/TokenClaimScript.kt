package org.rsmod.content.areas.misc.warriorsguild.scripts

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.areas.misc.warriorsguild.TokenLedger
import org.rsmod.content.areas.misc.warriorsguild.TokenLedger.Claim
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class TokenClaimScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (npc in STAFF) {
            onOpNpc3(npc) { startDialogue(it.npc) { claimTokens() } }
        }
        onOpNpc3("npc.warguild_shanomi") { startDialogue(it.npc) { shanomiClaimTokens() } }
    }

    companion object {
        private val STAFF =
            listOf(
                "npc.warguild_gamfred",
                "npc.warguild_ajjat",
                "npc.warguild_kamfreena",
                "npc.warguild_lorelai",
                "npc.warguild_sloane_npc",
                "npc.strength_referree1",
                "npc.strength_referree2",
            )

        private fun tokens(amount: Int): String = if (amount == 1) "1 token" else "$amount tokens"

        suspend fun Dialogue.claimTokens() {
            chatPlayer(quiz, "May I claim my tokens please?")
            when (val claim = TokenLedger.claim(player)) {
                Claim.NothingOwed -> {
                    chatNpc(
                        neutral,
                        "I'm afraid you have not earned any tokens yet. Try some of the " +
                            "activities around the guild to earn some.",
                    )
                    chatPlayer(neutral, "Ok, I'll go see what I can find.")
                }
                Claim.NoSpace -> chatNpc(neutral, "You don't have space to carry them.")
                is Claim.Paid -> {
                    chatNpc(happy, "Of course! Here you go, you've earned ${tokens(claim.amount)}!")
                    chatPlayer(happy, "Thanks!")
                }
            }
        }

        suspend fun Dialogue.shanomiClaimTokens() {
            chatPlayer(quiz, "May I claim my tokens please?")
            when (val claim = TokenLedger.claim(player)) {
                Claim.NothingOwed -> {
                    chatNpc(
                        neutral,
                        "No tokens earned have you. In training activities participate you must.",
                    )
                    chatPlayer(
                        neutral,
                        "Okay, I'll go see what I can find to do around here to earn some tokens.",
                    )
                }
                Claim.NoSpace -> {
                    chatNpc(neutral, "No space you have carry them for.")
                    chatPlayer(neutral, "Right, I need space in my pack for them. I'll be back!")
                }
                is Claim.Paid -> {
                    chatNpc(happy, "Yes yes! Earned you have ${tokens(claim.amount)}!")
                    chatPlayer(happy, "Thanks!")
                }
            }
        }
    }
}
