package org.rsmod.game.type

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import kotlin.contracts.contract
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.obj.Obj


public fun ItemServerType.hasOp(interactionOp: InteractionOp): Boolean {
    return hasOp(interactionOp.slot)
}

public fun ItemServerType.hasInvOp(invOp: HeldOp): Boolean {
    return hasInvOp(invOp.slot)
}

public fun String.isAssociatedWith(obj: InvObj?): Boolean {
    contract { returns(true) implies (obj != null) }
    return obj != null && obj.id == this.asRSCM(RSCMType.OBJ)
}

public fun ItemServerType.isAssociatedWith(obj: InvObj?): Boolean {
    contract { returns(true) implies (obj != null) }
    return obj != null && obj.id == id
}

public fun getObj(obj: Obj): ItemServerType =
    ServerCacheManager.getItem(obj.type)
        ?: throw NoSuchElementException("Type is missing in the map: $obj.")

public fun getInvObj(obj: InvObj): ItemServerType =
    ServerCacheManager.getItem(obj.id)
        ?: throw NoSuchElementException("Type is missing in the map: $obj.")

public fun getOrNull(obj: InvObj?): ItemServerType? = if (obj == null) null else getInvObj(obj)

public fun cert(obj: InvObj): InvObj {
    require(obj.vars == 0) { "Cannot cert obj with vars: $obj" }
    val type =
        ServerCacheManager.getItem(obj.id)
            ?: throw NoSuchElementException("Type is missing in the map: $${obj.id}.")
    if (!type.canCert) {
        return obj
    }
    val link = type.certlink
    val certType = RSCM.getReverseMapping(RSCMType.OBJ,link)
    return InvObj(certType, obj.count)
}

public fun uncert(obj: InvObj): InvObj {
    if (obj.vars != 0) {
        return obj
    }

    val type =
        ServerCacheManager.getItem(obj.id)
            ?: throw NoSuchElementException("Type is missing in the map: $${obj.id}.")
    if (!type.isCert) {
        return obj
    }

    val link = type.certlink
    val uncertType = RSCM.getReverseMapping(RSCMType.OBJ,link)
    return InvObj(uncertType, obj.count)
}

public fun cert(internal: String): ItemServerType {
    val type = ServerCacheManager.getItem(internal.asRSCM(RSCMType.OBJ))?: error("Could not find type: $internal")
    if (!type.canCert) {
        return type
    }
    val link = type.certlink
    return ServerCacheManager.getItem(link)
        ?: throw NoSuchElementException("Type is missing in the map: $link.")
}

public fun uncert(type: ItemServerType): ItemServerType {
    if (!type.isCert) {
        return type
    }
    val link = type.certlink
    return ServerCacheManager.getItem(link)
        ?: throw NoSuchElementException("Type is missing in the map: $link.")
}


public fun placeholder(type: ItemServerType): ItemServerType {
    if (!type.hasPlaceholder) {
        return type
    }
    val link = type.placeholderLink
    return ServerCacheManager.getItem(link)
        ?: throw NoSuchElementException("Type is missing in the map: $link.")
}

public fun unplacehold(type: ItemServerType): ItemServerType {
    if (!type.isPlaceholder) {
        return type
    }
    val link = type.placeholderLink
    return ServerCacheManager.getItem(link)
        ?: throw NoSuchElementException("Type is missing in the map: $link.")
}

public fun untransform(type: ItemServerType): ItemServerType {
    if (!type.isTransformation) {
        return type
    }
    val link = type.transformlink
    return ServerCacheManager.getItem(link)
        ?: throw NoSuchElementException("Type is missing in the map: $link.")
}

public fun normalize(type: ItemServerType): ItemServerType {
    val uncert = uncert(type)
    return unplacehold(uncert)
}
