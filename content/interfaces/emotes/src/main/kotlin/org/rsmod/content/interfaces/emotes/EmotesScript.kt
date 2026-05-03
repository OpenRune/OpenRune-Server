package org.rsmod.content.interfaces.emotes

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.VarBitType
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import dev.openrune.types.SequenceServerType
import dev.openrune.types.StatType
import dev.openrune.types.aconverted.SpotanimType
import dev.openrune.types.aconverted.interf.IfButtonOp
import dev.openrune.types.enums.EnumTypeMap
import jakarta.inject.Inject
import org.rsmod.api.config.refs.params
import org.rsmod.api.enums.NamedEnums.emote_names
import org.rsmod.api.enums.SkillCapeEnums.skill_cape_anims
import org.rsmod.api.enums.SkillCapeEnums.skill_cape_spots
import org.rsmod.api.player.back
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerWalkTrigger
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.map.Direction
import org.rsmod.game.map.translate
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class EmotesScript
@Inject
private constructor(
    private val eventBus: EventBus,
    private val protectedAccess: ProtectedAccessLauncher,
    private val npcRepo: NpcRepository,
    private val skillCapeEmotes: SkillCapeEmoteResolver,
) : PluginScript() {
    private lateinit var emoteSlotRange: IntRange

    override fun ScriptContext.startup() {
        loadEmotesEnum()
        loadSkillCapeEmotes()
        onIfOpen("interface.emote") { player.onTabOpen() }
        onIfOverlayButton("component.emote:contents") { player.selectEmote(it.comsub, it.op) }
        onPlayerWalkTrigger("walktrigger.emote_cancelanim") { player.resetAnim() }
    }

    private fun loadEmotesEnum() {
        val comsubSlots = emote_names.keys
        emoteSlotRange = comsubSlots.min()..comsubSlots.max()
    }

    private fun loadSkillCapeEmotes() {
        skillCapeEmotes.startup()
    }

    private fun Player.selectEmote(emoteSlot: Int, op: IfButtonOp) {
        val emote = emote_names.getValue(emoteSlot)
        ifClose(eventBus)
        protectedAccess.launch(this) { selectEmote(emote, op) }
    }

    private suspend fun ProtectedAccess.selectEmote(emote: String, op: IfButtonOp) {
        when (emote) {
            "Yes" -> loopAnim("seq.emote_yes", "seq.emote_yes_loop", op)
            "No" -> loopAnim("seq.emote_no", "seq.emote_no_loop", op)
            "Bow" -> loopAnim("seq.emote_bow", "seq.emote_bow_loop", op)
            "Angry" -> loopAnim("seq.emote_angry", "seq.emote_angry_loop", op)
            "Think" -> loopAnim("seq.emote_think", "seq.emote_think_loop", op)
            "Wave" -> loopAnim("seq.emote_wave", "seq.emote_wave_loop", op)
            "Shrug" -> loopAnim("seq.emote_shrug", "seq.emote_shrug_loop", op)
            "Cheer" -> loopAnim("seq.emote_cheer", "seq.emote_cheer_loop", op)
            "Beckon" -> loopAnim("seq.emote_beckon", "seq.emote_beckon_loop", op)
            "Laugh" -> loopAnim("seq.emote_laugh", "seq.emote_laugh_loop", op)
            "Jump for Joy" -> loopAnim("seq.emote_jump_with_joy", "seq.emote_jump_with_joy_loop", op)
            "Yawn" -> loopAnim("seq.emote_yawn", "seq.emote_yawn_loop", op)
            "Dance" -> loopAnim("seq.emote_dance", "seq.emote_dance_loop", op)
            "Jig" -> loopAnim("seq.emote_dance_scottish", "seq.emote_dance_scottish_loop", op)
            "Spin" -> loopAnim("seq.emote_dance_spin", "seq.emote_dance_spin_loop", op)
            "Headbang" -> loopAnim("seq.emote_dance_headbang", "seq.emote_dance_headbang_loop", op)
            "Cry" -> loopAnim("seq.emote_cry", "seq.emote_cry_loop", op)
            "Blow Kiss" -> loopAnim("seq.emote_blow_kiss", "seq.emote_blow_kiss_loop", op)
            "Panic" -> loopAnim("seq.emote_panic", "seq.emote_panic_loop", op)
            "Raspberry" -> loopAnim("seq.emote_ya_boo_sucks", "seq.emote_ya_boo_sucks_loop", op)
            "Clap" -> loopAnim("seq.emote_clap", "seq.emote_clap_loop", op)
            "Salute" -> loopAnim("seq.emote_fremmenik_salute", "seq.emote_fremmenik_salute_loop", op)
            "Goblin Bow" ->
                lockedLoopAnimDialog(
                    "seq.human_cave_goblin_bow",
                    "seq.human_cave_goblin_bow_loop",
                    "varbit.lost_tribe_quest",
                    "This emote can be unlocked during the Lost Tribe quest.",
                    op = op,
                    varbitStateReq = 7,
                )
            "Goblin Salute" ->
                lockedLoopAnimDialog(
                    "seq.human_cave_goblin_dance",
                    "seq.human_cave_goblin_dance_loop",
                    "varbit.lost_tribe_quest",
                    "This emote can be unlocked during the Lost Tribe quest.",
                    op = op,
                    varbitStateReq = 7,
                )
            "Glass Box" ->
                lockedLoopAnimDialog(
                    "seq.emote_glass_box",
                    "seq.emote_glass_box_loop",
                    "varbit.emote_glassbox",
                    "This emote can be unlocked during the mime random event.",
                    op = op,
                )
            "Climb Rope" ->
                lockedLoopAnimDialog(
                    "seq.emote_climbing_rope",
                    "seq.emote_climbing_rope_loop",
                    "varbit.emote_climbrope",
                    "This emote can be unlocked during the mime random event.",
                    op = op,
                )
            "Lean" ->
                lockedLoopAnimDialog(
                    "seq.emote_mime_lean",
                    "seq.emote_mime_lean_loop",
                    "varbit.emote_lean",
                    "This emote can be unlocked during the mime random event.",
                    op = op,
                )
            "Glass Wall" ->
                lockedLoopAnimDialog(
                    "seq.emote_glass_wall",
                    "seq.emote_glass_wall_loop",
                    "varbit.emote_glasswall",
                    "This emote can be unlocked during the mime random event.",
                    op = op,
                )
            "Idea" ->
                lockedLoopAnimDialog(
                    "seq.emote_lightbulb",
                    "seq.emote_lightbulb_loop",
                    "varbit.sos_emote_idea",
                    "You can't use that emote yet - visit the Stronghold of Security to unlock it.",
                    spot = "spotanim.emote_lightbulb_spot",
                    op = op,
                )
            "Stamp" ->
                lockedLoopAnimDialog(
                    "seq.emote_stampfeet",
                    "seq.emote_stampfeet_loop",
                    "varbit.sos_emote_stamp",
                    "You can't use that emote yet - visit the Stronghold of Security to unlock it.",
                    spot = "spotanim.emote_duststamp_spot",
                    op = op,
                )
            "Flap" ->
                lockedAnimDialog(
                    flapEmoteSelector(op),
                    "varbit.sos_emote_flap",
                    "You can't use that emote yet - visit the Stronghold of Security to unlock it.",
                )
            "Slap Head" ->
                lockedLoopAnimDialog(
                    "seq.emote_slap_head",
                    "seq.emote_slap_head_loop",
                    "varbit.sos_emote_idea",
                    "You can't use that emote yet - visit the Stronghold of Security to unlock it.",
                    op = op,
                )
            "Zombie Walk" ->
                lockedLoopAnimDialog(
                    "seq.zombie_walk_emote",
                    "seq.zombie_walk_emote_loop",
                    "varbit.emote_zombie_walk",
                    "This emote can be unlocked during the gravedigger random event.",
                    op = op,
                )
            "Zombie Dance" ->
                lockedLoopAnimDialog(
                    "seq.zombie_dance",
                    "seq.zombie_dance_loop",
                    "varbit.emote_zombie_dance",
                    "This emote can be unlocked during the gravedigger random event.",
                    op = op,
                )
            "Scared" ->
                lockedLoopAnimDialog(
                    "seq.terrified_emote",
                    "seq.terrified_emote_loop",
                    "varbit.emote_terrified",
                    "This emote can be unlocked by doing a Halloween seasonal event.",
                    op = op,
                )
            "Rabbit Hop" ->
                lockedLoopAnimDialog(
                    "seq.rabbit_emote",
                    "seq.rabbit_emote_loop",
                    "varbit.emote_bunny_hop",
                    "This emote can be unlocked by doing an Easter seasonal event.",
                    op = op,
                )
            "Sit up" ->
                lockedLoopAnimDialog(
                    "seq.emote_situps_5",
                    "seq.emote_situps_5_loop",
                    "varbit.emote_drilldemon",
                    "You can't use that emote yet - complete the Drill Demon event to unlock them.",
                    op = op,
                )
            "Push up" ->
                lockedLoopAnimDialog(
                    "seq.emote_pushups_5",
                    "seq.emote_pushups_5_loop",
                    "varbit.emote_drilldemon",
                    "You can't use that emote yet - complete the Drill Demon event to unlock them.",
                    op = op,
                )
            "Star jump" ->
                lockedLoopAnimDialog(
                    "seq.emote_starjump_5",
                    "seq.emote_starjump_5_loop",
                    "varbit.emote_drilldemon",
                    "You can't use that emote yet - complete the Drill Demon event to unlock them.",
                    op = op,
                )
            "Jog" ->
                lockedLoopAnimDialog(
                    "seq.emote_run_on_spot",
                    "seq.emote_run_on_spot_loop",
                    "varbit.emote_drilldemon",
                    "You can't use that emote yet - complete the Drill Demon event to unlock them.",
                    op = op,
                )
            "Flex" ->
                lockedLoopAnimDialog(
                    "seq.emote_flex",
                    "seq.emote_flex_loop",
                    "varbit.emote_flex",
                    "You can unlock this emote by completing Checkal's task in Below Ice Mountain.",
                    op = op,
                )
            "Zombie Hand" ->
                lockedAnimDialog(
                    "seq.hw07_arm_from_the_ground_emote",
                    "varbit.emote_zombie_hand",
                    "This emote can be unlocked by doing a Halloween seasonal event.",
                )
            "Hypermobile Drinker" ->
                lockedLoopAnimDialog(
                    "seq.ash_emote",
                    "seq.ash_emote_loop",
                    "varbit.emote_ash",
                    "This emote can be unlocked by doing a Halloween seasonal event.",
                    op = op,
                )
            "Skill Cape" -> skillCapeEmote()
            "Air Guitar" -> airGuitarEmote()
            "Uri transform" -> uriTransformEmote()
            "Smooth dance" ->
                lockedAnimDialog(
                    "seq.bday17_bling",
                    "varbit.emote_hotline_bling",
                    "This emote can be unlocked by doing a birthday event.",
                )
            "Crazy dance" ->
                lockedAnimDialog(
                    crazyDanceEmoteSelector(op),
                    "varbit.emote_gangnam",
                    "This emote can be unlocked by doing a birthday event.",
                )
            "Premier Shield" -> premierShieldEmote()
            "Explore" ->
                lockedLoopAnimDialog(
                    "seq.emote_explore",
                    "seq.emote_explore_loop",
                    "varbit.emote_explore",
                    "This emote can be unlocked by completing at least 600 beginner clue scrolls.",
                    op = op,
                )
            "Relic unlock" -> relicUnlockEmote()
            "Party" ->
                lockedLoopAnimDialog(
                    "seq.emote_party",
                    "seq.emote_party_loop",
                    "varbit.emote_party",
                    "This emote can be unlocked by doing a birthday event.",
                    spot = "spotanim.fx_emote_party01_active",
                    op = op,
                )
            "Trick" ->
                lockedAnimDialog(
                    "seq.emote_trick",
                    "varbit.emote_trick",
                    "This emote can be unlocked by doing a Halloween event.",
                    spot = "spotanim.hw23_emote_bat_spotanim",
                )
            "Fortis Salute" -> fortisSaluteEmote(loop = op == IfButtonOp.Op2)
            "Crab dance" -> {
                /* Emote is not available in the tab. */
            }
            "Sit down" -> loopAnim("seq.emote_sit_loop", "seq.emote_sit", op)
            else -> throw NotImplementedError("Emote not implemented: $emote")
        }
    }

    private fun ProtectedAccess.playAnim(seq: String, spot: String?) {
        anim(seq)
        spot?.let(::spotanim)
        publishEmoteEvent(seq)
    }

    private fun ProtectedAccess.simpleAnim(internal: String, spot: String? = null) {

        val seq = ServerCacheManager.getAnim(internal.asRSCM(RSCMType.SEQ))?: error("Invalid sequence: $internal")
        if (seq.requiresWalkTrigger() && !trySetWalkTrigger("walktrigger.emote_cancelanim")) {
            return
        }
        stopAction()
        playAnim(internal, spot)
    }

    private fun ProtectedAccess.loopAnim(
        seqOp1: String,
        seqOp2: String,
        op: IfButtonOp,
        spot: String? = null,
    ) {
        val seq = if (op == IfButtonOp.Op2) seqOp2 else seqOp1
        simpleAnim(seq, spot)
    }

    private suspend fun ProtectedAccess.lockedAnimDialog(
        seq: String,
        varbit: String,
        text: String,
        varbitStateReq: Int = 1,
        spot: String? = null,
    ) {
        val state = vars[varbit]
        if (state < varbitStateReq) {
            mesbox(text)
            return
        }
        simpleAnim(seq, spot)
    }

    private suspend fun ProtectedAccess.lockedLoopAnimDialog(
        seqOp1: String,
        seqOp2: String,
        varbit: String,
        text: String,
        spot: String? = null,
        op: IfButtonOp,
        varbitStateReq: Int = 1,
    ) {
        val state = vars[varbit]
        if (state < varbitStateReq) {
            mesbox(text)
            return
        }
        loopAnim(seqOp1, seqOp2, op, spot)
    }

    private suspend fun ProtectedAccess.skillCapeEmote() {
        stopAction()

        if (ocIsType(player.back, "obj.music_cape_trimmed", "obj.music_cape_trimmed")) {
            masteryCapeEmote("seq.emote_air_guitar", "spotanim.air_guitar_spotanim")
            return
        }

        if (ocIsType(player.back, "obj.skillcape_qp", "obj.skillcape_qp_trimmed")) {
            val seq = "seq.skillcapes_player_quest_cape"
            val spot = "spotanim.skillcapes_quest_cape_spotanim"
            masteryCapeEmote(seq, spot)
            return
        }

        if (ocIsType(player.back, "obj.skillcape_ad", "obj.skillcape_ad_trimmed")) {
            achievementDiaryCapeEmote()
            return
        }

        if (ocIsContentType(player.back, "content.max_cape")) {
            masteryCapeEmote("seq.max_cape_player_anim", "spotanim.max_cape")
            return
        }

        val skillCape = ocIsContentType(player.back, "content.skill_cape")
        if (!skillCape) {
            mes("You need to be wearing a skillcape in order to perform that emote.")
            return
        }

        val stat = ocParamOrNull(player.back, params.statreq1_skill)
        if (stat == null || stat !in skillCapeEmotes) {
            mes("You need to be wearing a skillcape in order to perform that emote.")
            return
        }

        val (anim, spotanim) = skillCapeEmotes[stat]

        masteryCapeEmote(
            RSCM.getReverseMapping(RSCMType.SEQ, anim.id),
            RSCM.getReverseMapping(RSCMType.SPOTANIM, spotanim.id),
        )
    }

    private suspend fun ProtectedAccess.masteryCapeEmote(
        internal: String,
        spotanim: String,
    ) {
        if (isInCombat()) {
            mes("You can't perform that emote now.")
            return
        }
        playAnim(internal, spotanim)

        val seq = ServerCacheManager.getAnim(internal.asRSCM(RSCMType.SEQ))?: error("Invalid sequence: $internal")

        delay(seq)
        rebuildAppearance()
    }

    private suspend fun ProtectedAccess.achievementDiaryCapeEmote() {
        if (isInCombat()) {
            mes("You can't perform that emote now.")
            return
        }
        val southWest = coords.translate(Direction.SouthWest)
        val npc =
            Npc("npc.diary_emote_npc", southWest).apply {
                respawnDir = Direction.South
                mode = NpcMode.None
            }

        val validLineOfWalk = lineOfWalk(coords, npc.bounds())
        if (!validLineOfWalk) {
            mes("You can't do this emote here.")
            return
        }

        anim("seq.diary_emote_playeranim")
        faceEntitySquare(npc)
        npcRepo.add(npc, duration = 30)
        npc.anim("seq.diary_emote_spotanim")
        delay(31)
        rebuildAppearance()
    }

    private fun ProtectedAccess.airGuitarEmote() {
        stopAction()
        val unlocked = vars["varbit.emote_musiccape"] != 0
        if (!unlocked) {
            mes(
                "You need to have bought a music cape and have all music tracks " +
                    "unlocked (apart from holiday events) in order to perform that emote."
            )
            return
        }
        if (isInCombat()) {
            mes("You can't perform that emote now.")
            return
        }
        midiJingle("synth.emote_air_guitar")
        playAnim("seq.emote_air_guitar", "spotanim.air_guitar_spotanim")
    }

    private suspend fun ProtectedAccess.uriTransformEmote() {
        stopAction()
        val unlocked = vars["varbit.emote_uri_transform"] != 0
        if (!unlocked) {
            mesbox("This emote can be unlocked by completing at least 300 hard clue scrolls.")
            return
        }
        if (isInCombat()) {
            mes("You can't perform that emote now.")
            return
        }
        spotanim("spotanim.smokepuff", height = 92)
        transmog("npc.trail_master_uri")
        delay(1)
        spotanim("spotanim.briefcase_spotanim")
        anim("seq.emote_uri_briefcase")
        transmog("npc.uri_emote")
        delay(9)
        anim("seq.poh_smash_magic_tablet")
        delay(1)
        spotanim("spotanim.poh_absorb_tablet_magic")
        anim("seq.poh_absorb_tablet_teleport")
        delay(1)
        spotanim("spotanim.smokepuff", height = 92)
        resetAnim()
        resetTransmog()
        publishEmoteEvent("seq.emote_uri_briefcase")
    }

    private suspend fun ProtectedAccess.premierShieldEmote() {
        stopAction()
        if (vars["varbit.emote_premier_club_2018"] < 1) {
            mesbox("This emote is unlocked upon creating an account.")
            return
        }

        if (mapClock - player.premierShieldClock < 4) {
            mes("You're already doing that.")
            return
        }

        val spot =
            when (player.premierShieldCount) {
                1 -> "spotanim.premier_club_emote_spotanim_silver"
                2 -> "spotanim.premier_club_emote_spotanim_gold"
                else -> "spotanim.premier_club_emote_spotanim_bronze"
            }

        player.premierShieldCount = (player.premierShieldCount + 1) % 3
        player.premierShieldClock = mapClock
        playAnim("seq.premier_club_emote", spot)
    }

    private suspend fun ProtectedAccess.relicUnlockEmote() {
        stopAction()
        val unlocked = vars["varbit.poh_leaguehall_outfitstand_relichunter_type"] != 0
        if (!unlocked) {
            mesbox(
                "You can't use that emote unless you have stored a " +
                    "tier 3 relichunter outfit on the outfitstand in your " +
                    "player owned house League Hall."
            )
            return
        }
        if (isInCombat()) {
            mes("You can't perform that emote now.")
            return
        }
        val seq = if (isBodyTypeB()) "seq.human_relic_unlock_female" else "seq.human_relic_unlock"
        anim(seq)
        if (vars["varbit.poh_leaguehall_outfitstand_relichunter_type"] == 3) {
            spotanim("spotanim.league_twisted_relic_unlock_spot", height = 92)
        } else {
            spotanim("spotanim.league_trailblazer_relic_unlock_spot", height = 92)
        }
        delay(4)
        publishEmoteEvent(seq)
    }

    private suspend fun ProtectedAccess.fortisSaluteEmote(loop: Boolean) {
        val unlocked = vars["varp.colosseum_glory"] >= 20_000
        if (!unlocked) {
            mesbox(
                "This emote is unlocked by reaching <col=ff0000>" +
                    "Grand Champion</col> status in the Fortis Colosseum."
            )
            return
        }
        val seq = if (loop) "seq.emote_varlamore_salute_loop" else "seq.emote_varlamore_salute"
        simpleAnim(seq)
    }

    private fun ProtectedAccess.publishEmoteEvent(seq: String) {
        val event = PlayEmote(player, ServerCacheManager.getAnim(seq.asRSCM(RSCMType.SEQ))!!)
        eventBus.publish(event)
    }

    private fun Player.onTabOpen() {
        ifSetEvents(
            "component.emote:contents",
            emoteSlotRange,
            IfEvent.Op1,
            IfEvent.Op2,
            IfEvent.Op3,
        )
    }

    private fun ProtectedAccess.flapEmoteSelector(op: IfButtonOp): String {
        val chickenPieces = invContentTotal(worn, "content.chicken_outfit")
        return if (chickenPieces >= 4) {
            "seq.vm_natural_historian_monkey_hop"
        } else {
            if (op == IfButtonOp.Op2) "seq.emote_panic_flap_loop" else "seq.emote_panic_flap"
        }
    }

    private fun ProtectedAccess.crazyDanceEmoteSelector(op: IfButtonOp): String {
        player.crazyDanceCount = (player.crazyDanceCount + 1) % 2
        return if (player.crazyDanceCount == 0) {
            if (op == IfButtonOp.Op2) "seq.bday17_style_loop" else "seq.bday17_style"
        } else {
            if (op == IfButtonOp.Op2) "seq.bday17_lasso_loop" else "seq.bday17_lasso"
        }
    }

    private fun SequenceServerType.requiresWalkTrigger(): Boolean = this.maxLoops == 255
}

private class SkillCapeEmoteResolver {
    private lateinit var skillCapeAnims: EnumTypeMap<StatType, SequenceServerType>
    private lateinit var skillCapeSpots: EnumTypeMap<StatType, SpotanimType>

    fun startup() {
        skillCapeAnims = skill_cape_anims
        skillCapeSpots = skill_cape_spots
    }

    operator fun contains(stat: StatType): Boolean =
        stat in skillCapeAnims && stat in skillCapeAnims

    operator fun get(stat: StatType): Pair<SequenceServerType, SpotanimType> {
        val anim = checkNotNull(skillCapeAnims[stat]) { "Skill cape anim not defined for: $stat" }
        val spot = checkNotNull(skillCapeSpots[stat]) { "Skill cape spot not defined for: $stat" }
        return anim to spot
    }
}

private var Player.premierShieldClock by intVarp("varp.emote_clock_premier_shield")
private var Player.premierShieldCount by intVarBit("varbit.emote_counters_premier_shield")
private var Player.crazyDanceCount by intVarBit("varbit.emote_counters_crazy_dance")
