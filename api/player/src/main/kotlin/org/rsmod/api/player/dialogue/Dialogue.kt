package org.rsmod.api.player.dialogue

import dev.openrune.types.ItemServerType
import dev.openrune.types.MesAnimType
import dev.openrune.types.NpcServerType
import dev.openrune.types.aconverted.ContentGroupType
import org.rsmod.api.config.Constants
import org.rsmod.api.config.refs.mesanims
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapDelegate
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory

public class Dialogue(
    public val access: ProtectedAccess,
    public val npc: Npc?,
    public val faceFar: Boolean,
) {
    public val player: Player by access::player
    public val vars: VarPlayerIntMapDelegate by access::vars

    public val quiz: MesAnimType
        get() = mesanims.quiz

    public val bored: MesAnimType
        get() = mesanims.bored

    public val short: MesAnimType
        get() = mesanims.short

    public val happy: MesAnimType
        get() = mesanims.happy

    public val shocked: MesAnimType
        get() = mesanims.shocked

    public val confused: MesAnimType
        get() = mesanims.confused

    public val silent: MesAnimType
        get() = mesanims.silent

    public val neutral: MesAnimType
        get() = mesanims.neutral

    public val shifty: MesAnimType
        get() = mesanims.shifty

    public val worried: MesAnimType
        get() = mesanims.worried

    public val drunk: MesAnimType
        get() = mesanims.drunk

    public val verymad: MesAnimType
        get() = mesanims.very_mad

    public val laugh: MesAnimType
        get() = mesanims.laugh

    public val madlaugh: MesAnimType
        get() = mesanims.mad_laugh

    public val sad: MesAnimType
        get() = mesanims.sad

    public val angry: MesAnimType
        get() = mesanims.angry

    public val npcVisType: NpcServerType
        get() = access.npcVisType(npcOrThrow())

    /** @see [ProtectedAccess.mesbox] */
    public suspend fun mesbox(text: String) {
        access.mesbox(text)
    }

    /** @see [ProtectedAccess.mesboxNp] */
    public fun mesboxNp(text: String) {
        access.mesboxNp(text)
    }

    /** @see [ProtectedAccess.objbox] */
    public suspend fun objbox(obj: ItemServerType, text: String) {
        access.objbox(obj, text)
    }

    /** @see [ProtectedAccess.objboxNp] */
    public fun objboxNp(obj: ItemServerType, text: String) {
        access.objboxNp(obj, text)
    }

    /** @see [ProtectedAccess.objbox] */
    public suspend fun objbox(obj: ItemServerType, zoom: Int, text: String) {
        access.objbox(obj, zoom, text)
    }

    /** @see [ProtectedAccess.objboxNp] */
    public fun objboxNp(obj: ItemServerType, zoom: Int, text: String) {
        access.objboxNp(obj, zoom, text)
    }

    /** @see [ProtectedAccess.objbox] */
    public suspend fun objbox(obj: InvObj, text: String) {
        access.objbox(obj, text)
    }

    /** @see [ProtectedAccess.objboxNp] */
    public fun objboxNp(obj: InvObj, text: String) {
        access.objboxNp(obj, text)
    }

    /** @see [ProtectedAccess.objbox] */
    public suspend fun objbox(obj: InvObj, zoom: Int, text: String) {
        access.objbox(obj, zoom, text)
    }

    /** @see [ProtectedAccess.objboxNp] */
    public fun objboxNp(obj: InvObj, zoom: Int, text: String) {
        access.objboxNp(obj, zoom, text)
    }

    /** @see [ProtectedAccess.doubleobjbox] */
    public suspend fun doubleobjbox(obj1: ItemServerType, obj2: ItemServerType, text: String) {
        access.doubleobjbox(obj1, obj2, text)
    }

    /** @see [ProtectedAccess.doubleobjboxNp] */
    public fun doubleobjboxNp(obj1: ItemServerType, obj2: ItemServerType, text: String) {
        access.doubleobjboxNp(obj1, obj2, text)
    }

    /** @see [ProtectedAccess.doubleobjbox] */
    public suspend fun doubleobjbox(
        obj1: ItemServerType,
        zoom1: Int,
        obj2: ItemServerType,
        zoom2: Int,
        text: String,
    ) {
        access.doubleobjbox(obj1, zoom1, obj2, zoom2, text)
    }

    /** @see [ProtectedAccess.doubleobjboxNp] */
    public fun doubleobjboxNp(
        obj1: ItemServerType,
        zoom1: Int,
        obj2: ItemServerType,
        zoom2: Int,
        text: String,
    ) {
        access.doubleobjboxNp(obj1, zoom1, obj2, zoom2, text)
    }

    /** @see [ProtectedAccess.doubleobjbox] */
    public suspend fun doubleobjbox(obj1: InvObj, obj2: InvObj, text: String) {
        access.doubleobjbox(obj1, obj2, text)
    }

    /** @see [ProtectedAccess.doubleobjboxNp] */
    public fun doubleobjboxNp(obj1: InvObj, obj2: InvObj, text: String) {
        access.doubleobjboxNp(obj1, obj2, text)
    }

    /** @see [ProtectedAccess.doubleobjbox] */
    public suspend fun doubleobjbox(
        obj1: InvObj,
        zoom1: Int,
        obj2: InvObj,
        zoom2: Int,
        text: String,
    ) {
        access.doubleobjbox(obj1, zoom1, obj2, zoom2, text)
    }

    /** @see [ProtectedAccess.doubleobjboxNp] */
    public fun doubleobjboxNp(obj1: InvObj, zoom1: Int, obj2: InvObj, zoom2: Int, text: String) {
        access.doubleobjboxNp(obj1, zoom1, obj2, zoom2, text)
    }

    /** @see [ProtectedAccess.chatPlayer] */
    public suspend fun chatPlayerNoAnim(text: String) {
        access.chatPlayer(null, text)
    }

    /** @see [ProtectedAccess.chatPlayer] */
    public suspend fun chatPlayer(mesanim: MesAnimType, text: String) {
        access.chatPlayer(mesanim, text)
    }

    /** @see [ProtectedAccess.chatNpc] */
    public suspend fun chatNpc(mesanim: MesAnimType, text: String) {
        access.chatNpc(npcOrThrow(), mesanim, text, faceFar = faceFar)
    }

    /** @see [ProtectedAccess.chatNpcNoTurn] */
    public suspend fun chatNpcNoTurn(mesanim: MesAnimType, text: String) {
        access.chatNpcNoTurn(npcOrThrow(), mesanim, text)
    }

    /** @see [ProtectedAccess.chatNpcNoAnim] */
    public suspend fun chatNpcNoAnim(text: String) {
        access.chatNpcNoAnim(npcOrThrow(), text, faceFar = faceFar)
    }

    /** @see [ProtectedAccess.chatNpcSpecific] */
    public suspend fun chatNpcSpecific(
        title: String,
        type: NpcServerType,
        mesanim: MesAnimType,
        text: String,
    ) {
        access.chatNpcSpecific(title, type, mesanim, text)
    }

    /** @see [ProtectedAccess.chatNpcSpecificNp] */
    public fun chatNpcSpecificNp(
        title: String,
        type: NpcServerType,
        mesanim: MesAnimType,
        text: String,
    ) {
        access.chatNpcSpecificNp(title, type, mesanim, text)
    }

    /** @see [ProtectedAccess.choice2] */
    public suspend fun <T> choice2(
        choice1: String,
        result1: T,
        choice2: String,
        result2: T,
        title: String = Constants.cm_options,
    ): T =
        access.choice2(
            choice1 = choice1,
            result1 = result1,
            choice2 = choice2,
            result2 = result2,
            title = title,
        )

    /** @see [ProtectedAccess.choice3] */
    public suspend fun <T> choice3(
        choice1: String,
        result1: T,
        choice2: String,
        result2: T,
        choice3: String,
        result3: T,
        title: String = Constants.cm_options,
    ): T =
        access.choice3(
            choice1 = choice1,
            result1 = result1,
            choice2 = choice2,
            result2 = result2,
            choice3 = choice3,
            result3 = result3,
            title = title,
        )

    /** @see [ProtectedAccess.choice4] */
    public suspend fun <T> choice4(
        choice1: String,
        result1: T,
        choice2: String,
        result2: T,
        choice3: String,
        result3: T,
        choice4: String,
        result4: T,
        title: String = Constants.cm_options,
    ): T =
        access.choice4(
            choice1 = choice1,
            result1 = result1,
            choice2 = choice2,
            result2 = result2,
            choice3 = choice3,
            result3 = result3,
            choice4 = choice4,
            result4 = result4,
            title = title,
        )

    /** @see [ProtectedAccess.choice5] */
    public suspend fun <T> choice5(
        choice1: String,
        result1: T,
        choice2: String,
        result2: T,
        choice3: String,
        result3: T,
        choice4: String,
        result4: T,
        choice5: String,
        result5: T,
        title: String = Constants.cm_options,
    ): T =
        access.choice5(
            choice1 = choice1,
            result1 = result1,
            choice2 = choice2,
            result2 = result2,
            choice3 = choice3,
            result3 = result3,
            choice4 = choice4,
            result4 = result4,
            choice5 = choice5,
            result5 = result5,
            title = title,
        )

    /** @see [ProtectedAccess.confirmDestroy] */
    public suspend fun confirmDestroy(
        obj: ItemServerType,
        count: Int,
        header: String,
        text: String,
    ): Boolean = access.confirmDestroy(obj, count, header, text)

    /** @see [ProtectedAccess.delay] */
    public suspend fun delay(cycles: Int = 1): Unit = access.delay(cycles)

    /** @see [ProtectedAccess.invTotal] */
    public fun invTotal(inv: Inventory, content: ContentGroupType): Int =
        access.invTotal(inv, content)

    /** @see [ProtectedAccess.invContains] */
    public operator fun Inventory.contains(content: ContentGroupType): Boolean =
        access.invContains(this, content)

    /** @see [ProtectedAccess.ocCert] */
    public fun ocCert(type: ItemServerType): ItemServerType = access.ocCert(type)

    /** @see [ProtectedAccess.ocUncert] */
    public fun ocUncert(type: ItemServerType): ItemServerType = access.ocUncert(type)

    private fun npcOrThrow(): Npc {
        return npc ?: error("`npc` must be set. Use `startDialogue(npc) { ... }` instead.")
    }
}
