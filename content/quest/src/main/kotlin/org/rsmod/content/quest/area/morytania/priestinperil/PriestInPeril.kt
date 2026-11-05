package org.rsmod.content.quest.area.morytania.priestinperil

import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Singleton
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onApNpc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.content.quest.manager.startQuestPrompt
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@Singleton
class PriestInPeril :
    QuestScript(
        "quest_priestinperil",
        "varp.priestperil",
        rewards {
            xp("stat.prayer", 1406.0)
            item(WOLFBANE)
            extra("Access to Morytania after Drezel's blessing")
        },
        ItemRewardDisplay(WOLFBANE),
    ) {
    override fun ScriptContext.init() {
        check(quest.maxSteps == COMPLETE)
        onOpNpc1("npc.king_roald") { startDialogue(it.npc) { roald() } }
        onOpNpc1("npc.priestperiltrappedmonk") { startDialogue(it.npc) { imprisonedDrezel() } }
        onApNpc1("npc.priestperiltrappedmonk") {
            if (isWithinApRange(it.npc, 3)) startDialogue(it.npc) { imprisonedDrezel() }
        }
        onOpNpc1("npc.priestperiltrappedmonk2") { startDialogue(it.npc) { undergroundDrezel() } }
    }

    internal fun stage(player: Player) = quest.getQuestStage(player)

    internal fun canPass(player: Player) =
        quest.isQuestCompleted(player) && player.vars[SALVE_BLESSING] != 0

    internal fun canEnterMausoleum(player: Player) =
        stage(player) >= RETURNED_TO_ROALD &&
            (player.vars[MAUSOLEUM_UNLOCKED] != 0 || GOLD_KEY in player.inv)

    internal fun canMonkAggress(player: Player, hooded: Boolean): Boolean =
        if (hooded) player.vars["varbit.priestperilevilmonk_dead"] == 0
        else stage(player) >= CELL_UNLOCKED

    internal fun keepMonkHidden(player: Player, ownedGroundKey: Boolean): Boolean {
        val bank = player.invMap.getOrPut("inv.bank")
        return stage(player) >= CELL_UNLOCKED ||
            ownedGroundKey ||
            GOLD_KEY in player.inv ||
            IRON_KEY in player.inv ||
            GOLD_KEY in bank ||
            IRON_KEY in bank
    }

    internal fun dropsGoldenKey(player: Player, npc: Npc): Boolean =
        (npc.isType("npc.priestperilevilmonk3") || npc.isType("npc.priestperilevilmonk3_vis")) &&
            npc.spawnCoords == CoordGrid(3411, 3489, 0) &&
            stage(player) in RETURNED_TO_ROALD..COFFIN_SEALED &&
            !keepMonkHidden(player, false)

    private suspend fun Dialogue.roald() {
        when (stage(player)) {
            0 -> {
                chatNpc(
                    sad,
                    "I've heard nothing from Drezel at Paterdomus, east of Varrock. Will you find out what has happened?",
                )
                if (startQuestPrompt(quest)) {
                    quest.setQuestStage(access, STARTED)
                    chatNpc(
                        neutral,
                        "Follow the road east, past the lumber yard, to the temple beside the River Salve.",
                    )
                }
            }
            GUARDIAN_DEAD -> {
                chatPlayer(
                    neutral,
                    "The priests asked me to kill a dog in the mausoleum. It's dead now.",
                )
                chatNpc(
                    angry,
                    "That guardian was protecting Misthalin! Those men cannot be Drezel's priests. Return and put this right!",
                )
                quest.setQuestStage(access, RETURNED_TO_ROALD)
                VarPlayerIntMapSetter.set(player, "varbit.priestperil_returned_to_roald", 1)
            }
            in STARTED..GUARDIAN_REQUESTED ->
                chatNpc(neutral, "Please investigate Paterdomus and report back to me.")
            in RETURNED_TO_ROALD until COMPLETE ->
                chatNpc(sad, "Find Drezel and help him restore the temple's protection.")
            else -> chatNpc(happy, "Thank you for helping Drezel protect our kingdom.")
        }
    }

    private suspend fun Dialogue.imprisonedDrezel() {
        when (stage(player)) {
            RETURNED_TO_ROALD -> {
                chatNpc(
                    sad,
                    "The Zamorakians have imprisoned me. They are trying to break the blessings that protect the River Salve.",
                )
                chatNpc(
                    neutral,
                    "Find the key to my cell. A vampyre lies in the coffin outside; I cannot leave while it can awaken.",
                )
                quest.setQuestStage(access, MET_DREZEL)
            }
            MET_DREZEL ->
                chatNpc(
                    neutral,
                    "The monks' golden key doesn't fit this cell. Look among the monuments beneath the temple.",
                )
            CELL_UNLOCKED -> {
                if (MURKY_WATER in player.inv) {
                    chatNpc(
                        neutral,
                        "The well's water carries the Salve's blessing. Let me strengthen it for you.",
                    )
                    if (exchange(access, MURKY_WATER, BLESSED_WATER))
                        chatNpc(neutral, "Pour this over the coffin to keep the vampyre asleep.")
                } else
                    chatNpc(
                        neutral,
                        "Bring water from the well below. Once blessed, it will seal the vampyre's coffin.",
                    )
            }
            COFFIN_SEALED -> {
                chatNpc(
                    happy,
                    "The vampyre is contained. Meet me beyond the monument room; the river still needs our help.",
                )
                quest.setQuestStage(access, MOVED_UNDERGROUND)
            }
            else -> chatNpc(neutral, "We should speak in the mausoleum.")
        }
    }

    private suspend fun Dialogue.undergroundDrezel() {
        val current = stage(player)
        if (current >= COMPLETE) {
            if (!canPass(player)) {
                chatNpc(
                    neutral,
                    "The eastern passage leads to Morytania. Do you wish to pass? The lands beyond the Salve are dangerous.",
                )
                if (choice2("Yes, please bless me so I can pass.", 1, "Not yet.", 2) == 1) {
                    chatNpc(neutral, "May Saradomin's blessing protect you beyond the river.")
                    VarPlayerIntMapSetter.set(player, SALVE_BLESSING, 1)
                }
            } else {
                chatNpc(happy, "The passage is open to you. Take care in Morytania.")
            }
            return
        }
        if (current < MOVED_UNDERGROUND) {
            chatNpc(neutral, "Please help me at the temple first.")
            return
        }
        if (current < ESSENCE_START) quest.setQuestStage(access, ESSENCE_START)
        val remaining = ESSENCE_REQUIRED - donatedEssence(player)
        chatNpc(
            neutral,
            "The river has been polluted by dark magic. Rune essence will absorb it. I still need $remaining pieces of unnoted rune or pure essence.",
        )
        val delivered = donateEssence(access)
        if (delivered == 0 && remaining > 0) {
            chatNpc(neutral, "You can bring the essence in several trips. I cannot use banknotes.")
            return
        }
        if (donatedEssence(player) < ESSENCE_REQUIRED) {
            chatNpc(
                neutral,
                "Thank you. I still need ${ESSENCE_REQUIRED - donatedEssence(player)} pieces.",
            )
            return
        }
        chatNpc(
            happy,
            "The river is cleansed. Please accept Wolfbane, my family's dagger, with my thanks.",
        )
        if (!finish(access))
            chatNpc(neutral, "Please make room in your pack for Wolfbane, then speak to me again.")
    }

    /**
     * Keep native 10..59 journal/helper stages; fifty donated pieces unlock the once-only reward.
     */
    internal fun donatedEssence(player: Player) = player.vars[ESSENCE_COUNT]

    internal fun donateEssence(access: ProtectedAccess): Int {
        val current = stage(access.player)
        if (current !in ESSENCE_START..ESSENCE_END) return 0
        val remaining = ESSENCE_REQUIRED - donatedEssence(access.player)
        if (remaining <= 0) return 0
        val rune = minOf(access.inv.count(RUNE_ESSENCE), remaining)
        val pure = minOf(access.inv.count(PURE_ESSENCE), remaining - rune)
        if (rune + pure == 0) return 0
        val tx =
            access.player.invTransaction(access.inv, autoCommit = false) {
                val pack = select(access.inv)
                if (rune > 0) delete(pack, RUNE_ESSENCE.asRSCM(), rune)
                if (pure > 0) delete(pack, PURE_ESSENCE.asRSCM(), pure)
            }
        if (tx.failure) return 0
        tx.commitAll()
        val total = donatedEssence(access.player) + rune + pure
        VarPlayerIntMapSetter.set(access.player, ESSENCE_COUNT, total)
        quest.setQuestStage(access, minOf(ESSENCE_END, ESSENCE_START + total))
        return rune + pure
    }

    internal fun exchange(access: ProtectedAccess, from: String, to: String): Boolean {
        val tx =
            access.player.invTransaction(access.inv, autoCommit = false) {
                val pack = select(access.inv)
                delete(pack, from.asRSCM(), 1)
                add(pack, to.asRSCM(), 1)
            }
        if (tx.failure) return false
        tx.commitAll()
        return true
    }

    internal fun finish(access: ProtectedAccess): Boolean =
        stage(access.player) == ESSENCE_END &&
            donatedEssence(access.player) == ESSENCE_REQUIRED &&
            quest.completeQuest(access)

    override fun subTitle() =
        "talking to <col=800000>King Roald</col> in <col=800000>Varrock Palace</col>."

    override fun questLog(player: ProtectedAccess) =
        questJournal(player) {
            when (val current = stage(player.player)) {
                STARTED ->
                    objective(
                        "King Roald has asked me to investigate Paterdomus, east of Varrock."
                    ) {}
                GUARDIAN_REQUESTED ->
                    objective(
                        "The men behind the temple door asked me to kill the guardian below the northern trapdoor."
                    ) {}
                GUARDIAN_DEAD ->
                    objective("I killed the guardian. I should report to King Roald.") {}
                RETURNED_TO_ROALD ->
                    objective("I must find the real Drezel in the temple's upper floor.") {}
                MET_DREZEL ->
                    objective(
                        "Drezel is imprisoned. The monuments below the temple may hold the correct key."
                    ) {}
                CELL_UNLOCKED ->
                    objective(
                        "Drezel needs water from the well to bless. I must pour it over the coffin."
                    ) {}
                COFFIN_SEALED ->
                    objective(
                        "The coffin is sealed. I should tell Drezel he can leave his cell."
                    ) {}
                in MOVED_UNDERGROUND..ESSENCE_END ->
                    objective(
                        "I must bring Drezel ${ESSENCE_REQUIRED - donatedEssence(player.player)} more unnoted rune or pure essence to cleanse the Salve."
                    ) {}
            }
        }

    override fun completedLog(player: ProtectedAccess) =
        completionJournal(player) {
            line("I freed Drezel, sealed the vampyre's coffin and helped cleanse the River Salve.")
            line(
                if (canPass(player.player)) "Drezel has blessed me to enter Morytania."
                else "I should ask Drezel for his blessing before passing into Morytania."
            )
        }

    companion object {
        internal const val STARTED = 1
        internal const val GUARDIAN_REQUESTED = 2
        internal const val GUARDIAN_DEAD = 3
        internal const val RETURNED_TO_ROALD = 4
        internal const val MET_DREZEL = 5
        internal const val CELL_UNLOCKED = 6
        internal const val COFFIN_SEALED = 7
        internal const val MOVED_UNDERGROUND = 8
        internal const val ESSENCE_START = 10
        internal const val ESSENCE_END = 59
        internal const val ESSENCE_REQUIRED = 50
        internal const val ESSENCE_COUNT = "varbit.priest_peril_essence_count"
        internal const val COMPLETE = 60
        internal const val GOLD_KEY = "obj.pipkey_gold"
        internal const val IRON_KEY = "obj.pipkey_iron"
        internal const val MURKY_WATER = "obj.bucket_murkywater"
        internal const val BLESSED_WATER = "obj.bucket_blessedwater"
        internal const val WOLFBANE = "obj.dagger_wolfbane"
        internal const val RUNE_ESSENCE = "obj.blankrune"
        internal const val PURE_ESSENCE = "obj.blankrune_high"
        internal const val SALVE_BLESSING = "varbit.priestperil_salve_dialogue"
        internal const val MAUSOLEUM_UNLOCKED = "varbit.priest_peril_mausoleum_unlocked"
    }
}
