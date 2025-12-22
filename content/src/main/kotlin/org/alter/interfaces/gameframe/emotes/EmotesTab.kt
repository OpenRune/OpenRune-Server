package org.alter.plugins.content.interfaces.emotes

import org.alter.api.EquipmentType
import org.alter.api.ext.*
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.TaskPriority
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM

/**
 * @author Tom <rspsmods@gmail.com>
 */
object EmotesTab {
    const val COMPONENT_ID = 216

    fun unlockAll(p: Player) {
        p.setVarbit("varbits.lost_tribe_quest", 7)
        p.setVarbit("varbits.emote_glassbox", 1)
        p.setVarbit("varbits.emote_climbrope", 1)
        p.setVarbit("varbits.emote_lean", 1)
        p.setVarbit("varbits.emote_glasswall", 1)
        p.setVarbit("varbits.sos_emote_idea", 1)
        p.setVarbit("varbits.sos_emote_stamp", 1)
        p.setVarbit("varbits.sos_emote_flap", 1)
        p.setVarbit("varbits.sos_emote_doh", 1)
        p.setVarbit("varbits.emote_zombie_walk", 1)
        p.setVarbit("varbits.emote_zombie_dance", 1)
        p.setVarbit("varbits.emote_terrified", 1)
        p.setVarbit("varbits.emote_bunny_hop", 1)
        p.setVarbit("varbits.emote_drilldemon", 1)
        p.setVarbit("varbits.emote_zombie_hand", 1)
        p.setVarbit("varbits.emote_ash", 1)
        p.setVarbit("varbits.emote_maxcape", 1)
        p.setVarbit("varbits.emote_musiccape", 1)
        p.setVarbit("varbits.emote_uri_transform", 1)
        p.setVarbit("varbits.emote_hotline_bling", 1)
        p.setVarbit("varbits.emote_gangnam", 1)
        p.setVarbit("varbits.emote_premier_club_2018", 1)
        p.setVarbit("varbits.emote_explore", 1)
        p.setVarbit("varbits.emote_flex", 1)
        p.setVarbit("varbits.poh_leaguehall_outfitstand_relichunter_type", 9)
        p.setVarbit("varbits.emote_party", 1)
    }

    fun performEmote(
        p: Player,
        emote: Emote,
    ) {
        if (emote.varbit != RSCM.NONE && p.getVarbit(emote.varbit) != emote.requiredVarbitValue) {
            val description = emote.unlockDescription ?: "You have not unlocked this emote yet."
            p.queue { messageBox(p, description) }
            return
        }

        /**
         * @author Jarafi
         * If you move you get locked into uris form
         */
        if (emote == Emote.URI_TRANSFORM) {
            p.queue {
                p.lock()
                p.graphic("spotanims.smokepuff")
                p.setTransmogId(7311)
                wait(1)
                p.setTransmogId(7313)
                p.graphic("spotanims.briefcase_spotanim")
                p.animate("sequences.emote_uri_briefcase")
                wait(10)
                p.animate("sequences.poh_smash_magic_tablet")
                wait(1)
                p.graphic("spotanims.poh_absorb_tablet_magic")
                p.animate("sequences.poh_absorb_tablet_teleport")
                wait(2)
                p.graphic("spotanims.smokepuff")
                p.setTransmogId(-1)
                p.unlock()
            }
        }
        /**
         * Thanks to @ClaroJack for the skill animation/gfx id's
         */
        if (emote == Emote.SKILLCAPE) {
            when (p.equipment[EquipmentType.CAPE.id]?.id) {
                getRSCM("items.skillcape_max_worn") -> {
                    p.animate("sequences.max_cape_player_anim", 4)
                    p.graphic("spotanims.max_cape", delay = 4)
                }
                getRSCM("items.skillcape_attack"), getRSCM("items.skillcape_attack_trimmed") -> {
                    p.animate("sequences.skill_cape_attack")
                    p.graphic("spotanims.skillcape_attack_spotanim")
                }
                getRSCM("items.skillcape_strength"), getRSCM("items.skillcape_strength_trimmed") -> {
                    p.animate("sequences.skillcapes_human_strength")
                    p.graphic("spotanims.skillcapes_strength")
                }
                getRSCM("items.skillcape_defence"), getRSCM("items.skillcape_defence_trimmed") -> {
                    p.animate("sequences.skill_cape_defend")
                    p.graphic("spotanims.skillcape_defend_spotanim")
                }
                getRSCM("items.skillcape_ranging"), getRSCM("items.skillcape_ranging_trimmed") -> {
                    p.animate("sequences.skillcapes_human_range")
                    p.graphic("spotanims.skillcapes_range")
                }
                getRSCM("items.skillcape_prayer"), getRSCM("items.skillcape_prayer_trimmed") -> {
                    p.animate("sequences.skillcapes_human_prayer")
                    p.graphic("spotanims.skillcapes_prayer")
                }
                getRSCM("items.skillcape_magic"), getRSCM("items.skillcape_magic_trimmed") -> {
                    p.animate("sequences.skillcapes_player_magic")
                    p.graphic("spotanims.skillcapes_magic_spotanim")
                }
                getRSCM("items.skillcape_runecrafting"), getRSCM("items.skillcape_runecrafting_trimmed") -> {
                    p.animate("sequences.skillcapes_player_runecrafting")
                    p.graphic("spotanims.skillcapes_runecrafting_spotanim")
                }
                getRSCM("items.skillcape_hitpoints"), getRSCM("items.skillcape_hitpoints_trimmed") -> {
                    p.animate("sequences.skillcapes_human_hitpoints")
                    p.graphic("spotanims.skillcapes_hitpoints")
                }
                getRSCM("items.skillcape_agility"), getRSCM("items.skillcape_agility_trimmed") -> {
                    p.animate("sequences.skillcapes_human_agility")
                    p.graphic("spotanims.skillcapes_agility")
                }
                getRSCM("items.skillcape_herblore"), getRSCM("items.skillcape_herblore_trimmed") -> {
                    p.animate("sequences.skillcapes_human_herblore")
                    p.graphic("spotanims.skillcapes_herblore")
                }
                getRSCM("items.skillcape_thieving"), getRSCM("items.skillcape_thieving_trimmed") -> {
                    p.animate("sequences.skill_cape_thieving")
                    p.graphic("spotanims.skillcape_thieving_spotanim")
                }
                getRSCM("items.skillcape_crafting"), getRSCM("items.skillcape_crafting_trimmed") -> {
                    p.animate("sequences.skillcapes_crafting_player_anim")
                    p.graphic("spotanims.skillcapes_crafting_spotanim")
                }
                getRSCM("items.skillcape_fletching"), getRSCM("items.skillcape_fletching_trimmed") -> {
                    p.animate("sequences.skillcapes_player_fletching_bow")
                    p.graphic("spotanims.skillcapes_fletching_bow_spotanim")
                }
                getRSCM("items.skillcape_slayer"), getRSCM("items.skillcape_slayer_trimmed") -> {
                    p.animate("sequences.skill_cape_slayer")
                    p.graphic("spotanims.skillcape_slayer_spotanim")
                }
                getRSCM("items.skillcape_construction"), getRSCM("items.skillcape_construction_trimmed") -> {
                    p.animate("sequences.skillcapes_construction_player_anim")
                    p.graphic("spotanims.skillcapes_construction_spotanim")
                }
                getRSCM("items.skillcape_mining"), getRSCM("items.skillcape_mining_trimmed") -> {
                    p.animate("sequences.skillcapes_player_mining")
                    p.graphic("spotanims.skillcapes_mining_spotanim")
                }
                getRSCM("items.skillcape_smithing"), getRSCM("items.skillcape_smithing_trimmed") -> {
                    p.animate("sequences.skillcapes_player_smithing")
                    p.graphic("spotanims.skillcapes_smithing_spotanim")
                }
                getRSCM("items.skillcape_fishing"), getRSCM("items.skillcape_fishing_trimmed") -> {
                    p.animate("sequences.skillcapes_fishing_player_anim")
                    p.graphic("spotanims.skillcapes_fishing_spotanim")
                }
                getRSCM("items.skillcape_cooking"), getRSCM("items.skillcape_cooking_trimmed") -> {
                    p.animate("sequences.skillcapes_cooking_player_anim")
                    p.graphic("spotanims.skillcapes_cooking_spotanim")
                }
                getRSCM("items.skillcape_firemaking"), getRSCM("items.skillcape_firemaking_trimmed") -> {
                    p.animate("sequences.skillcapes_human_firemaking")
                    p.graphic("spotanims.skillcapes_firemaking")
                }
                getRSCM("items.skillcape_woodcutting"), getRSCM("items.skillcape_woodcutting_trimmed") -> {
                    p.animate("sequences.skillcapes_woodcutting_player_anim")
                    p.graphic("spotanims.skillcapes_woodcutting_spotanim")
                }
                getRSCM("items.skillcape_farming"), getRSCM("items.skillcape_farming_trimmed") -> {
                    p.animate("sequences.skillcape_farming")
                    p.graphic("spotanims.skillcape_farming_spotanim")
                }
                getRSCM("items.skillcape_hunting"), getRSCM("items.skillcape_hunting_trimmed") -> {
                    p.animate("sequences.skillcapes_human_hunting")
                    p.graphic("spotanims.skillcapes_hunting")
                }
                getRSCM("items.skillcape_cabbage") -> {
                    p.animate("sequences.eoc_pick_strength_success")
                }
                getRSCM("items.skillcape_qp"), getRSCM("items.skillcape_qp_trimmed") -> {
                    p.animate("sequences.skillcapes_player_quest_cape")
                    p.graphic("spotanims.skillcapes_quest_cape_spotanim")
                }
                getRSCM("items.skillcape_ad"), getRSCM("items.skillcape_ad_trimmed") -> {
                    p.animate("sequences.diary_emote_playeranim")
                    p.graphic("spotanims.skillcape_diary_spotanim")
                }
                getRSCM("items.music_cape"), getRSCM("items.music_cape_trimmed") -> {
                    p.animate("sequences.emote_air_guitar")
                    p.graphic("spotanims.air_guitar_spotanim")
                }
            }
        }

        if (emote == Emote.RELIC_UNLOCKED) {
            p.queue(TaskPriority.STANDARD) {
                p.graphic(RSCM.NONE)
                p.graphic(emote.gfx, 100)
                p.unlock()
            }
        }
        if (emote.anim != RSCM.NONE) {
            p.queue(TaskPriority.STANDARD) {
                p.animate(RSCM.NONE)
                p.animate(emote.anim, 1)
                p.unlock()
            }
        }
        if (emote.gfx != RSCM.NONE && emote != Emote.RELIC_UNLOCKED) {
            p.queue(TaskPriority.STANDARD) {
                p.graphic(RSCM.NONE)
                p.graphic(emote.gfx)
                p.unlock()
            }
        }
    }
}
