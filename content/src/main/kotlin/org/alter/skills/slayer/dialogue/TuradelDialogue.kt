package org.alter.skills.slayer.dialogue

import org.alter.api.ext.chatNpc
import org.alter.api.ext.chatPlayer
import org.alter.api.ext.getVarp
import org.alter.api.ext.options
import org.alter.api.ext.setVarp
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.attr.FIRST_TIME_TURAEL
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.game.model.queue.QueueTask
import org.alter.rscm.RSCM.asRSCM
import org.alter.skills.slayer.SlayerTaskManager
import org.generated.tables.slayer.SlayerMasterTaskRow
import org.generated.tables.slayer.SlayerTaskRow

object TuradelDialogue {

    val npc = "npcs.slayer_master_1_tureal".asRSCM()

    fun start(player: Player) {
        player.queue {
            if (SlayerTaskManager.getCurrentSlayerTask(player) == null && player.attr.getOrDefault(FIRST_TIME_TURAEL,true)) {
                firstTime(player,this)
            } else {
                GenericDialogue.slayerGenericDialogue(player,this,npc)
            }
        }
    }

    suspend fun firstTime(player: Player, task : QueueTask) {
        task.chatPlayer(player,"Who are you?")
        task.chatNpc(player,"I'm one of the elite Slayer Masters.",npc)
        when(task.options(player,"What's a slayer?","Never heard of you...")) {
            1 -> {
                task.chatPlayer(player,"What's a slayer?")
                task.chatNpc(player,"Oh dear, what do they teach you in school?",npc)
                task.chatPlayer(player,"Well... er...")
                task.chatNpc(player,"I suppose I'll have to educate you then. A slayer is someone who is trained to fight specific creatures. They know these creatures' every weakness and strength. As you can guess it makes killing them a lot easier.")
                startLearning(player,task)
                player.attr[FIRST_TIME_TURAEL] = true
            }
            2 -> startLearning(player,task)
        }
    }

    suspend fun startLearning(player: Player, task : QueueTask) {
        task.chatPlayer(player,"Never heard of you...")
        task.chatNpc(player,"That's because my foe never lives to tell of me. We slayers are a dangerous bunch.",npc)
        when(task.options(player,"Wow, can you teach me?","Sounds useless to me.")) {
            1 -> {
                task.chatPlayer(player,"Wow, can you teach me?")
                task.chatNpc(player,"Hmmm well I'm not so sure...",npc)
                task.chatPlayer(player,"Pleeeaasssse!")
                task.chatNpc(player,"Oh okay then, you twisted my arm. You'll have to train against specific groups of creatures.",npc)
                task.chatPlayer(player,"Okay, what's first?")
                SlayerTaskManager.assignTask(player,npc)

                val slayerTask = SlayerTaskManager.getCurrentSlayerTask(player)
                if (slayerTask != null) {
                    task.chatNpc(player,"We'll start you off hunting ${slayerTask.nameUppercase}, you'll need to kill ${player.getVarp("varp.slayer_count")} of them.",npc)
                    task.chatNpc(player,"You'll also need this enchanted gem, it allows Slayer Masters like myself to contact you and update you on your progress. Don't worry if you lose it, you can buy another from any Slayer Master.",npc)
                    if (player.inventory.add(Item("items.slayer_gem", 1)).hasSucceeded()) {
                        task.chatNpc(player,"You'll also need this enchanted gem, it allows Slayer Masters like myself to contact you and update you on your progress. Don't worry if you lose it, you can buy another from any Slayer Master.",npc)
                    } else {
                        task.chatNpc(player,"I tried to give you this enchanted gem but you had no room, it allows Slayer Masters like myself to contact you and update you on your progress. Don't worry you can buy another from any Slayer Master.",npc)
                    }
                    when(task.options(player,"Got any tips for me?","Okay, great!")) {
                        1 ->  GenericDialogue.slayerTip(player,task)
                        2 -> {
                            task.chatPlayer(player,"Okay, great!")
                            task.chatNpc(player,"Good luck! Don't forget to come back when you need a new assignment.",npc)
                        }
                    }
                }
            }
            2 -> {
                task.chatPlayer(player,"Sounds useless to me.")
                task.chatNpc(player,"Suit yourself.",npc)
            }
        }
    }

}