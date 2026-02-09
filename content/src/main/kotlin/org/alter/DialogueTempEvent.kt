package org.alter

import dev.openrune.util.TextAlignment
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.*
import org.alter.interfaces.*
import org.alter.rscm.RSCM
import org.alter.rscm.RSCMType

//todo THIS IS TEMP TILL EVEYTHING MOVED TO NEW SYSTEM

class DialogueTempEvent : PluginEvent() {

    override fun init() {

        on<DialogCloseAll> {
            then {
                player.ifCloseModals()
            }
        }

        on<DialogPlayerOpen> {
            then {
                val pages = TextAlignment.generateChatPageList(message)
                for (page in pages) {
                    val (pgText, lineCount) = page
                    val lineHeight = TextAlignment.chatLineHeight(lineCount)
                    player.ifChatPlayer(title, pgText, animation, constants.cm_pausebutton, lineHeight)
                }
            }
        }

        on<DialogNpcOpen> {
            then {
                val pages = TextAlignment.generateChatPageList(message)
                for (page in pages) {
                    val (pgText, lineCount) = page
                    val lineHeight = TextAlignment.chatLineHeight(lineCount)
                    player.ifChatNpcSpecific(title, RSCM.getReverseMapping(RSCMType.NPCTYPES,npc),pgText,animation, constants.cm_pausebutton, lineHeight)
                }
            }
        }

        on<DialogNpcOpen> {
            then {
                val pages = TextAlignment.generateChatPageList(message)
                for (page in pages) {
                    val (pgText, lineCount) = page
                    val lineHeight = TextAlignment.chatLineHeight(lineCount)
                    player.ifChatNpcSpecific(title, RSCM.getReverseMapping(RSCMType.NPCTYPES,npc),pgText,animation, constants.cm_pausebutton, lineHeight)
                }
            }
        }

        on<DialogMessageOpen> {
            then {
                val pages = TextAlignment.generateMesPageList(message)
                for (page in pages) {
                    val (pgText, lineCount) = page
                    val lineHeight = TextAlignment.mesLineHeight(lineCount)
                    player.ifMesbox(pgText, if (continues) constants.cm_pausebutton else "", lineHeight)
                }
            }
        }

        on<DialogMessageOption> {
            then {
                player.ifChoice(title, options, options.length)
            }
        }

        on<DialogItem> {
            then {
                val pages = TextAlignment.generateChatPageList(message)
                for (page in pages) {
                    player.ifObjbox(page.text, item, zoom, if (continues) constants.cm_pausebutton else "")
                }
            }
        }

        on<DialogItemDouble> {
            then {
                val pages = TextAlignment.generateChatPageList(message)
                for (page in pages) {
                    player.ifDoubleobjbox(page.text,item1, zoom1, item2, zoom2, constants.cm_pausebutton)
                }
            }
        }

        on<DialogSkillMulti> {
            then {
                player.skillMulti()
            }
        }

    }



}