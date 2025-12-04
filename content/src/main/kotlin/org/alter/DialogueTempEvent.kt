package org.alter

import dev.openrune.util.TextAlignment
import net.rsprot.protocol.game.outgoing.camera.CamReset
import net.rsprot.protocol.game.outgoing.misc.client.HideLocOps
import net.rsprot.protocol.game.outgoing.misc.client.HideNpcOps
import net.rsprot.protocol.game.outgoing.misc.client.HideObjOps
import net.rsprot.protocol.game.outgoing.misc.client.MinimapToggle
import net.rsprot.protocol.game.outgoing.misc.client.ResetAnims
import net.rsprot.protocol.game.outgoing.misc.player.ChatFilterSettings
import net.rsprot.protocol.game.outgoing.misc.player.SetPlayerOp
import net.rsprot.protocol.game.outgoing.misc.player.UpdateRunEnergy
import net.rsprot.protocol.game.outgoing.varp.VarpReset
import org.alter.api.ChatMessageType
import org.alter.api.ClientScript
import org.alter.api.CommonClientScripts
import org.alter.api.ext.boolVarBit
import org.alter.api.ext.message
import org.alter.api.ext.runClientScript
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.DialogItem
import org.alter.game.pluginnew.event.impl.DialogItemDouble
import org.alter.game.pluginnew.event.impl.DialogMessageOpen
import org.alter.game.pluginnew.event.impl.DialogMessageOption
import org.alter.game.pluginnew.event.impl.DialogNpcOpen
import org.alter.game.pluginnew.event.impl.DialogPlayerOpen
import org.alter.game.pluginnew.event.impl.DialogSkillMulti
import org.alter.interfaces.ifChatNpcSpecific
import org.alter.interfaces.ifChatPlayer
import org.alter.interfaces.ifChoice
import org.alter.interfaces.ifDoubleobjbox
import org.alter.interfaces.ifMesbox
import org.alter.interfaces.ifObjbox
import org.alter.interfaces.skillMulti
import org.alter.rscm.RSCM
import org.alter.rscm.RSCMType

//todo THIS IS TEMP TILL EVEYTHING MOVED TO NEW SYSTEM

class DialogueTempEvent : PluginEvent() {

    override fun init() {
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
                    player.ifObjbox(page.text, item, zoom, constants.cm_pausebutton)
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