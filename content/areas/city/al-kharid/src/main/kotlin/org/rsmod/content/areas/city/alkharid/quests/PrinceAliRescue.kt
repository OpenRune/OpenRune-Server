@file:Suppress("SpellCheckingInspection")

package org.rsmod.content.areas.city.alkharid.quests

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Prince Ali Rescue — F2P novice quest (M7.F5).
 *
 * Re-expressed on the rev240 quest engine: identity (id, display name, quest points, endstate) is
 * read from the `dbrow.quest_princealirescue` cache row, and progress is written to the
 * OSRS-canonical `varp.princequest` varp. The cache row's `endstate` is **110**, and the canonical
 * varp progression runs in steps of ten, so the stage constants below are raw varp values rather
 * than a collapsed 0..n counter.
 *
 * ## Stages
 * - `0` — Chancellor Hassan has not been asked for the quest.
 * - [STAGE_TALKED_TO_HASSAN] (`10`) — Hassan accepted the quest and pointed the player at Osman,
 *   the royal spymaster. Hassan sits in the south room of the Al-Kharid palace.
 * - [STAGE_OSMAN_BRIEFED] (`20`) — Osman handed the player the shopping list of disguise
 *   ingredients (a blond wig, skin-paste, a pink skirt, a key impression, and a soft clay lump).
 *   Osman's daughter Leela waits in Draynor with the operation plan.
 * - [STAGE_LEELA_BRIEFED] (`30`) — Leela explained the full plan: distract Joe with beer, take an
 *   impression of Lady Keli's key, smith a copy from bronze, free the prince.
 * - [STAGE_KEY_IMPRESSED] (`50`) — The player pressed Lady Keli's key into a soft clay lump and is
 *   carrying a [KEYPRINT].
 * - [STAGE_KEY_FORGED] (`70`) — Leela inspected the impression and the bronze bar and minted a
 *   [PRINCESKEY] for the player to take to the prison.
 * - [STAGE_COMPLETE] (`110`) — Prince Ali is freed. Completion pays the cache-row 3 quest points
 *   plus 700 coins, and unlocks free passage through the Al-Kharid toll gate (read by
 *   [org.rsmod.content.areas.city.alkharid.AlKharidTollGate]).
 *
 * Dialogue text follows [https://oldschool.runescape.wiki/w/Transcript:Prince_Ali_Rescue]. The
 * cutscene moments (Lady Keli's key demonstration, Joe's beer nap, the prison escape) are mesbox
 * lines rather than choreographed cutscenes.
 *
 * The script lives in the al-kharid module because the start NPC (Chancellor Hassan) is in the
 * Al-Kharid palace. Leela's Talk-to handler lives here too — she is a Draynor NPC, but she is a
 * pure quest actor, so this module owns her binding and there is no Draynor → al-kharid edge.
 */
class PrinceAliRescue @Inject constructor(private val objRepo: ObjRepository) :
    QuestScript(
        "quest_princealirescue",
        "varp.princequest",
        rewards { item(COINS, COIN_REWARD) },
        ItemRewardDisplay(PRINCESKEY),
    ) {

    override fun ScriptContext.init() {
        onOpNpc1(HASSAN) { handleHassan(it.npc) }
        onOpNpc1(OSMAN) { handleOsman(it.npc) }
        onOpNpc1(LADY_KELI) { handleLadyKeli(it.npc) }
        onOpNpc1(JOE) { handleJoe(it.npc) }
        onOpNpc1(PRINCE_ALI) { handlePrinceAli(it.npc) }
        onOpNpc1(LEELA) { handleLeela(it.npc) }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Chancellor Hassan — Al-Kharid palace, ground floor southern room.
    // ────────────────────────────────────────────────────────────────────────

    private suspend fun ProtectedAccess.handleHassan(npc: Npc) {
        when {
            quest.isQuestNotStarted(player) -> startDialogue(npc) { hassanOffer() }
            quest.isQuestCompleted(player) -> startDialogue(npc) { hassanComplete() }
            else -> startDialogue(npc) { hassanInProgress() }
        }
    }

    private suspend fun Dialogue.hassanOffer() {
        chatNpc(sad, "Help us, please, brave adventurer!")
        chatPlayer(quiz, "What's the matter?")
        chatNpc(
            sad,
            "Our young Prince Ali has been kidnapped. We fear he is being held by Lady Keli " +
                "north of Draynor Village. The Lumbridge guards refuse to help — we need an " +
                "outsider to slip in and free him.",
        )
        val accepted =
            choice2(
                "Yes, I will help.",
                true,
                "Sorry, I'm too busy.",
                false,
                title = "Start the Prince Ali Rescue quest?",
            )
        if (!accepted) {
            chatPlayer(sad, "Sorry, I'm too busy.")
            chatNpc(sad, "I understand. Please come back if you change your mind.")
            return
        }
        chatPlayer(happy, "Yes, I will help.")
        chatNpc(
            happy,
            "Thank you, friend. Go and speak to my spymaster Osman — he stands by the fountain " +
                "just outside the palace. He has been planning the rescue and will tell you " +
                "what is needed.",
        )
        access.advanceTo(STAGE_TALKED_TO_HASSAN)
    }

    private suspend fun Dialogue.hassanInProgress() {
        chatNpc(
            quiz,
            "How fares the rescue, my friend? Osman should be able to help you more than I can.",
        )
    }

    private suspend fun Dialogue.hassanComplete() {
        chatNpc(
            happy,
            "Thank you again, brave adventurer. Al-Kharid owes you a great debt for returning " +
                "the prince to us.",
        )
    }

    // ────────────────────────────────────────────────────────────────────────
    // Osman — outside the palace, by the fountain.
    // ────────────────────────────────────────────────────────────────────────

    private suspend fun ProtectedAccess.handleOsman(npc: Npc) {
        when {
            quest.isQuestNotStarted(player) -> startDialogue(npc) { osmanNotStarted() }
            quest.isQuestCompleted(player) -> startDialogue(npc) { osmanComplete() }
            else -> startDialogue(npc) { osmanInProgress(quest.getQuestStage(player)) }
        }
    }

    private suspend fun Dialogue.osmanNotStarted() {
        chatNpc(
            neutral,
            "Greetings. I am Osman, spymaster to Chancellor Hassan. If you wish to help us " +
                "with our troubles, speak to the chancellor first.",
        )
    }

    private suspend fun Dialogue.osmanInProgress(stage: Int) {
        if (stage >= STAGE_OSMAN_BRIEFED) {
            chatNpc(
                neutral,
                "My daughter Leela is waiting for you south of the Draynor wheat fields. She " +
                    "has the operation plan.",
            )
            return
        }
        chatPlayer(quiz, "Hassan sent me. How do we get Prince Ali out?")
        chatNpc(
            neutral,
            "Listen carefully. Lady Keli wears a key on her belt — get an impression of it " +
                "with a lump of soft clay. We will smith a copy from a bronze bar.",
        )
        chatNpc(
            neutral,
            "The guard, Joe, can be put to sleep with poison brewed by Aggie in Draynor. With " +
                "him snoring, you can slip past to the prince's cell.",
        )
        chatNpc(
            neutral,
            "Ali must walk out disguised. You will need a blond wig (Ned in Draynor makes wigs " +
                "from wool — Aggie dyes them yellow), skin-coloured paste (Aggie again), and a " +
                "pink skirt (Ranael in Al-Kharid sells them).",
        )
        chatNpc(
            neutral,
            "My daughter Leela will help you in Draynor. She is waiting south of the wheat " +
                "fields. Go and speak with her once you have the disguise pieces.",
        )
        access.advanceTo(STAGE_OSMAN_BRIEFED)
    }

    private suspend fun Dialogue.osmanComplete() {
        chatNpc(happy, "You have served Al-Kharid well, friend. The prince is safe again.")
    }

    // ────────────────────────────────────────────────────────────────────────
    // Leela — south of the Draynor wheat fields. Drives the key impression + forge.
    // ────────────────────────────────────────────────────────────────────────

    private suspend fun ProtectedAccess.handleLeela(npc: Npc) {
        when {
            quest.isQuestNotStarted(player) -> startDialogue(npc) { leelaNotStarted() }
            quest.isQuestCompleted(player) -> startDialogue(npc) { leelaComplete() }
            else -> startDialogue(npc) { leelaInProgress(quest.getQuestStage(player)) }
        }
    }

    private suspend fun Dialogue.leelaNotStarted() {
        chatNpc(
            neutral,
            "I'm sorry, adventurer, this is family business. Speak to my father Osman in " +
                "Al-Kharid if you wish to help.",
        )
    }

    private suspend fun Dialogue.leelaInProgress(stage: Int) {
        when {
            stage < STAGE_OSMAN_BRIEFED ->
                chatNpc(neutral, "Talk to my father first. He has the briefing for the rescue.")
            stage == STAGE_OSMAN_BRIEFED -> leelaBriefing()
            stage == STAGE_LEELA_BRIEFED -> leelaAwaitingImpression()
            stage == STAGE_KEY_IMPRESSED -> leelaForgeKey()
            else -> leelaAwaitingRescue()
        }
    }

    private suspend fun Dialogue.leelaBriefing() {
        chatPlayer(quiz, "Osman sent me. What do you need me to do?")
        chatNpc(
            neutral,
            "Listen — Lady Keli loves to brag about her keys. Ask her about them and she will " +
                "show you the one to the cell. While she is showing off, press a lump of soft " +
                "clay against it to take the impression.",
        )
        chatNpc(
            neutral,
            "Bring me the impression and a bronze bar; I will smith you a working copy. While " +
                "I do that, distract Joe with a beer.",
        )
        chatNpc(
            neutral,
            "Don't forget Prince Ali's disguise: blond wig, skin-paste, and a pink skirt. He " +
                "must walk out as a noblewoman.",
        )
        access.advanceTo(STAGE_LEELA_BRIEFED)
    }

    private suspend fun Dialogue.leelaAwaitingImpression() {
        if (access.invTotal(access.inv, SOFTCLAY) < 1) {
            chatNpc(
                quiz,
                "Do you have a lump of soft clay yet? Press it against Lady Keli's key while " +
                    "she shows it off.",
            )
            return
        }
        chatPlayer(quiz, "I have a lump of soft clay. How do I get the impression?")
        chatNpc(
            neutral,
            "Go to Lady Keli's hideout and ask to see her key — she loves to brag. Press the " +
                "clay against it while she's holding it up. Come back to me with the print.",
        )
    }

    private suspend fun Dialogue.leelaForgeKey() {
        if (access.invTotal(access.inv, KEYPRINT) < 1) {
            chatNpc(
                quiz,
                "You need to bring me the key impression first. Go and press a lump of soft " +
                    "clay against Lady Keli's key.",
            )
            return
        }
        if (access.invTotal(access.inv, BRONZE_BAR) < 1) {
            chatNpc(
                quiz,
                "I need a bronze bar to smith the copy. Bring one from the Al-Kharid furnace " +
                    "or the Grand Exchange.",
            )
            return
        }
        chatPlayer(happy, "I have the impression and a bronze bar.")
        access.invDel(access.inv, KEYPRINT, 1)
        access.invDel(access.inv, BRONZE_BAR, 1)
        mesbox(
            "Leela presses the bronze bar against the clay impression and works it on a small " +
                "furnace. The bronze cools into a working key."
        )
        access.invAddOrDrop(objRepo, PRINCESKEY)
        chatNpc(
            happy,
            "There you go. Take this princes' key to Prince Ali's cell. Don't forget the " +
                "disguise pieces — and a beer for Joe.",
        )
        access.advanceTo(STAGE_KEY_FORGED)
    }

    private suspend fun Dialogue.leelaAwaitingRescue() {
        chatNpc(
            quiz,
            "You have the key, friend. Go and free Prince Ali. Be wary of Joe — a beer should " +
                "put him out for long enough.",
        )
    }

    private suspend fun Dialogue.leelaComplete() {
        chatNpc(happy, "Well done, friend. My father will be pleased.")
    }

    // ────────────────────────────────────────────────────────────────────────
    // Lady Keli — her hideout. Bragging about her key drops the impression.
    // ────────────────────────────────────────────────────────────────────────

    private suspend fun ProtectedAccess.handleLadyKeli(npc: Npc) {
        when {
            quest.isQuestNotStarted(player) -> startDialogue(npc) { keliFlavour() }
            quest.isQuestCompleted(player) -> startDialogue(npc) { keliPostQuest() }
            else -> startDialogue(npc) { keliInProgress(quest.getQuestStage(player)) }
        }
    }

    private suspend fun Dialogue.keliFlavour() {
        chatNpc(
            angry,
            "What do you want, peasant? I am Lady Keli, the greatest jailer in the land!",
        )
    }

    private suspend fun Dialogue.keliInProgress(stage: Int) {
        if (stage < STAGE_LEELA_BRIEFED) {
            chatNpc(
                angry,
                "Go away, peasant. I am busy guarding the most important prisoner in Al-Kharid.",
            )
            return
        }
        if (stage >= STAGE_KEY_IMPRESSED) {
            chatNpc(angry, "Why do you keep coming back here? I've nothing to say to you.")
            return
        }
        chatPlayer(
            happy,
            "I've heard you have the finest keys in all of Misthalin. Could I see one?",
        )
        chatNpc(
            happy,
            "Ha! Of course. Behold — the only key to Prince Ali's cell. Forged from solid " +
                "bronze, the work of the greatest smith in Al-Kharid!",
        )
        if (access.invTotal(access.inv, SOFTCLAY) < 1) {
            chatPlayer(sad, "Magnificent — though I don't have any soft clay to press it on.")
            chatNpc(
                neutral,
                "Then stop wasting my time, peasant. Come back when you have something useful " +
                    "to do.",
            )
            return
        }
        chatPlayer(neutral, "May I get a closer look?")
        chatNpc(happy, "But of course. Behold!")
        access.invDel(access.inv, SOFTCLAY, 1)
        mesbox(
            "While Lady Keli holds the key aloft you press the soft clay against it. A " +
                "perfect impression of the key is left in the clay."
        )
        access.invAddOrDrop(objRepo, KEYPRINT)
        chatNpc(angry, "Right, that's enough. Be off with you.")
        access.advanceTo(STAGE_KEY_IMPRESSED)
    }

    private suspend fun Dialogue.keliPostQuest() {
        chatNpc(angry, "You! You're the thief who freed the prince! I'll have your head for this!")
    }

    // ────────────────────────────────────────────────────────────────────────
    // Joe — the jail-guard slacker. Takes a beer and naps.
    // ────────────────────────────────────────────────────────────────────────

    private suspend fun ProtectedAccess.handleJoe(npc: Npc) {
        when {
            quest.isQuestNotStarted(player) -> startDialogue(npc) { joeFlavour() }
            quest.isQuestCompleted(player) -> startDialogue(npc) { joePostQuest() }
            else -> startDialogue(npc) { joeInProgress(quest.getQuestStage(player)) }
        }
    }

    private suspend fun Dialogue.joeFlavour() {
        chatNpc(neutral, "Get lost, traveller. I'm on duty.")
    }

    private suspend fun Dialogue.joeInProgress(stage: Int) {
        if (stage < STAGE_LEELA_BRIEFED) {
            chatNpc(neutral, "Get lost, traveller. I'm on duty.")
            return
        }
        chatPlayer(happy, "Hot work guarding the prince, isn't it? Care for a beer?")
        if (access.invTotal(access.inv, BEER) < 1) {
            chatNpc(sad, "Ha! Got any on you? I'm parched.")
            return
        }
        chatNpc(happy, "Why thank you, friend. Don't mind if I do.")
        access.invDel(access.inv, BEER, 1)
        mesbox("Joe drains the beer in one gulp and slumps against the wall, snoring soundly.")
    }

    private suspend fun Dialogue.joePostQuest() {
        chatNpc(angry, "I'll get you back for that beer trick someday, traveller.")
    }

    // ────────────────────────────────────────────────────────────────────────
    // Prince Ali — locked in the prison cell. Hands over disguise and completes.
    // ────────────────────────────────────────────────────────────────────────

    private suspend fun ProtectedAccess.handlePrinceAli(npc: Npc) {
        when {
            quest.isQuestNotStarted(player) -> startDialogue(npc) { aliFlavour() }
            quest.isQuestCompleted(player) -> startDialogue(npc) { aliPostQuest() }
            else -> startDialogue(npc) { aliInProgress(quest.getQuestStage(player)) }
        }
    }

    private suspend fun Dialogue.aliFlavour() {
        chatNpc(sad, "Help me, please! I am Prince Ali of Al-Kharid!")
    }

    private suspend fun Dialogue.aliInProgress(stage: Int) {
        if (stage < STAGE_KEY_FORGED) {
            chatNpc(sad, "I cannot escape without help. Please bring me a disguise!")
            return
        }
        if (access.invTotal(access.inv, PRINCESKEY) < 1) {
            chatNpc(
                sad,
                "I cannot escape without the key to my cell. Leela should be able to smith " +
                    "you one.",
            )
            return
        }
        val missing = missingDisguisePieces(access)
        if (missing.isNotEmpty()) {
            chatNpc(
                sad,
                "I cannot walk out without a complete disguise. You still need: " +
                    missing.joinToString(", ") +
                    ".",
            )
            return
        }
        chatPlayer(happy, "Quickly — put these on. I have a key to your cell.")
        mesbox(
            "Prince Ali dons the blond wig, smears the skin-paste over his face, and pulls on " +
                "the pink skirt. He turns the key and slips out of the cell."
        )
        access.invDel(access.inv, BLONDWIG, 1)
        access.invDel(access.inv, SKINPASTE, 1)
        access.invDel(access.inv, PINK_SKIRT, 1)
        access.invDel(access.inv, PRINCESKEY, 1)
        chatNpc(
            happy,
            "Thank you, friend! Tell my father and Osman that I am safe. Take this purse for " +
                "your trouble.",
        )
        access.advanceTo(STAGE_COMPLETE)
    }

    private suspend fun Dialogue.aliPostQuest() {
        chatNpc(happy, "Greetings, friend! Al-Kharid is forever in your debt.")
    }

    /** Test-visible: drives Chancellor Hassan's Talk-to branch against a spawned [npc]. */
    public suspend fun talkToHassanForTest(access: ProtectedAccess, npc: Npc) {
        access.handleHassan(npc)
    }

    /** Test-visible: drives Osman's Talk-to branch against a spawned [npc]. */
    public suspend fun talkToOsmanForTest(access: ProtectedAccess, npc: Npc) {
        access.handleOsman(npc)
    }

    /** Test-visible: drives Leela's Talk-to branch against a spawned [npc]. */
    public suspend fun talkToLeelaForTest(access: ProtectedAccess, npc: Npc) {
        access.handleLeela(npc)
    }

    /** Test-visible: drives Lady Keli's Talk-to branch against a spawned [npc]. */
    public suspend fun talkToLadyKeliForTest(access: ProtectedAccess, npc: Npc) {
        access.handleLadyKeli(npc)
    }

    /** Test-visible: drives Joe's Talk-to branch against a spawned [npc]. */
    public suspend fun talkToJoeForTest(access: ProtectedAccess, npc: Npc) {
        access.handleJoe(npc)
    }

    /** Test-visible: drives Prince Ali's Talk-to branch against a spawned [npc]. */
    public suspend fun talkToPrinceAliForTest(access: ProtectedAccess, npc: Npc) {
        access.handlePrinceAli(npc)
    }

    private fun missingDisguisePieces(access: ProtectedAccess): List<String> {
        val missing = mutableListOf<String>()
        if (access.invTotal(access.inv, BLONDWIG) < 1) missing += "a blond wig"
        if (access.invTotal(access.inv, SKINPASTE) < 1) missing += "skin-paste"
        if (access.invTotal(access.inv, PINK_SKIRT) < 1) missing += "a pink skirt"
        return missing
    }

    /**
     * Jumps the quest varp straight to [target].
     *
     * The canonical `princequest` progression runs in steps of ten, so every advance is a jump of
     * more than one stage; [org.rsmod.content.quest.manager.Quest.advanceQuestStage] takes a delta.
     */
    private fun ProtectedAccess.advanceTo(target: Int) {
        val current = quest.getQuestStage(player)
        if (current >= target) {
            return
        }
        quest.advanceQuestStage(this, target - current)
    }

    override fun subTitle(): String =
        "speaking to <col=800000>Chancellor Hassan</col> in the <col=800000>Al-Kharid palace</col>."

    override fun questLog(player: ProtectedAccess) =
        questJournal(player) {
            description(
                "Prince Ali of <red>Al-Kharid</red> has been kidnapped by <red>Lady Keli</red> " +
                    "and is being held north of <red>Draynor Village</red>. Chancellor Hassan " +
                    "has asked me to bring him home."
            ) {
                hideWhenQuestCompleted()
            }

            objective("I must speak to <red>Osman</red> by the Al-Kharid palace fountain.") {
                stageAtLeast(STAGE_OSMAN_BRIEFED, "Osman gave me the plan for the rescue.")
                    .strike()
                    .finalise()
            }

            objective("I must find <red>Leela</red>, south of the Draynor wheat fields.") {
                stageAtLeast(STAGE_LEELA_BRIEFED, "Leela briefed me on the whole operation.")
                    .strike()
                    .finalise()
            }

            objective(
                "I must press a lump of <red>soft clay</red> against Lady Keli's key while she " +
                    "brags about it."
            ) {
                stageAtLeast(STAGE_KEY_IMPRESSED, "I have an impression of Lady Keli's key.")
                    .strike()
                    .finalise()
            }

            objective(
                "I must take the key impression and a <red>bronze bar</red> back to Leela so she " +
                    "can smith a copy."
            ) {
                stageAtLeast(STAGE_KEY_FORGED, "Leela smithed me a working princes' key.")
                    .strike()
                    .finalise()
            }

            objective(
                "I must gather Prince Ali's disguise — a <red>blond wig</red>, " +
                    "<red>skin-paste</red> and a <red>pink skirt</red> — distract <red>Joe</red> " +
                    "with a beer, and unlock the cell."
            ) {
                visibleWhen { quest.getQuestStage(access.player) >= STAGE_KEY_FORGED }
            }
        }

    override fun completedLog(player: ProtectedAccess) =
        completionJournal(player) {
            line(
                "Chancellor Hassan and his spymaster Osman asked me to free Prince Ali from Lady Keli's hideout north of Draynor Village."
            )
            line(
                "I took an impression of Lady Keli's cell key in soft clay, and Osman's daughter Leela smithed me a copy from a bronze bar."
            )
            line(
                "With Joe the guard snoring off a beer, I dressed the prince in a blond wig, skin-paste and a pink skirt and walked him out of the cell."
            )
            line("Al-Kharid now lets me through its toll gate for free.")
        }

    public companion object {
        // ── Stage values. Canonical `dbrow.quest_princealirescue` endstate = 110. ──
        public const val STAGE_TALKED_TO_HASSAN: Int = 10
        public const val STAGE_OSMAN_BRIEFED: Int = 20
        public const val STAGE_LEELA_BRIEFED: Int = 30
        public const val STAGE_KEY_IMPRESSED: Int = 50
        public const val STAGE_KEY_FORGED: Int = 70
        public const val STAGE_COMPLETE: Int = 110

        /** OSRS-canonical coin reward on completion (3 quest points come from the cache row). */
        public const val COIN_REWARD: Int = 700

        public const val HASSAN: String = "npc.hassan"
        public const val OSMAN: String = "npc.osman"
        public const val LEELA: String = "npc.leela"
        public const val LADY_KELI: String = "npc.lady_keli"
        public const val JOE: String = "npc.joe"
        public const val PRINCE_ALI: String = "npc.prince_ali_prison"

        public const val COINS: String = "obj.coins"
        public const val SOFTCLAY: String = "obj.softclay"
        public const val KEYPRINT: String = "obj.keyprint"
        public const val BRONZE_BAR: String = "obj.bronze_bar"
        public const val PRINCESKEY: String = "obj.princeskey"
        public const val BLONDWIG: String = "obj.blondwig"
        public const val SKINPASTE: String = "obj.skinpaste"
        public const val PINK_SKIRT: String = "obj.pink_skirt"
        public const val BEER: String = "obj.beer"
    }
}
