package org.rsmod.api.registry.zone

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.rsprot.protocol.game.outgoing.zone.payload.ObjCustomise
import net.rsprot.protocol.game.outgoing.zone.payload.ObjEnabledOps
import net.rsprot.protocol.game.outgoing.zone.payload.ObjUncustomise
import net.rsprot.protocol.game.outgoing.zone.payload.ScriptedProjAdd
import net.rsprot.protocol.game.outgoing.zone.payload.ScriptedProjChange
import net.rsprot.protocol.message.ZoneProt
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.obj.Obj
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneGrid
import org.rsmod.map.zone.ZoneKey

public class ZoneUpdateMap {
    public val updatedZones: Int2ObjectOpenHashMap<ZoneUpdateList> = Int2ObjectOpenHashMap()

    public fun clear() {
        updatedZones.clear()
    }

    public fun locAdd(loc: LocInfo) {
        val updates = loc.coords.getOrPutUpdateList()
        val prot = ZoneUpdateTransformer.toLocAddChangeProt(loc)
        updates += prot
    }

    public fun locDel(loc: LocInfo) {
        val updates = loc.coords.getOrPutUpdateList()
        val prot = ZoneUpdateTransformer.toLocDelProt(loc)
        updates += prot
    }

    public fun locAnim(loc: LocInfo, seq: Int) {
        val updates = loc.coords.getOrPutUpdateList()
        val prot = ZoneUpdateTransformer.toLocAnim(loc, seq)
        updates += prot
    }

    public fun objAdd(obj: Obj) {
        val updates = obj.coords.getOrPutUpdateList()
        val prot = obj.resolveObjAddProt()
        updates += prot
    }

    private fun Obj.resolveObjAddProt(): ZoneProt =
        if (isPublic) {
            ZoneUpdateTransformer.toPublicObjAddProt(this)
        } else {
            ZoneUpdateTransformer.toPrivateObjAddProt(this)
        }

    public fun objDel(obj: Obj) {
        val updates = obj.coords.getOrPutUpdateList()
        val prot = obj.resolveObjDelProt()
        updates += prot
    }

    private fun Obj.resolveObjDelProt(): ZoneProt =
        if (isPublic) {
            ZoneUpdateTransformer.toPublicObjDelProt(this)
        } else {
            ZoneUpdateTransformer.toPrivateObjDelProt(this)
        }

    public fun objCount(obj: Obj, newCount: Int, oldCount: Int) {
        val updates = obj.coords.getOrPutUpdateList()
        val prot =
            ZoneUpdateTransformer.toObjCountProt(obj, oldCount = oldCount, newCount = newCount)
        updates += prot
    }

    public fun objReveal(obj: Obj, cycle: Int) {
        val updates = obj.coords.getOrPutUpdateList()
        val prot = ZoneUpdateTransformer.toObjRevealProt(obj, cycle)
        updates += prot
    }

    /**
     * Queues a model, colour, or texture override for a public ground item.
     *
     * The enclosed zone update identifies [obj] by its tile, item type, and quantity. It
     * changes the appearance seen by clients in that zone while leaving the item stack intact.
     *
     * @param model Replacement model ID, or `-1` to omit the model.
     * @param recolIndex Index of the colour to replace with [recolour], or `-1` to omit it.
     * @param retexIndex Index of the texture to replace with [retexture], or `-1` to omit it.
     */
    public fun objCustomise(
        obj: Obj,
        model: Int = -1,
        recolIndex: Int = -1,
        recolour: Int = 0,
        retexIndex: Int = -1,
        retexture: Int = 0,
    ) {
        require(obj.isPublic) { "Only public items can be customised through a shared zone update." }
        require(model in -1..65534 && recolIndex in -1..32767 && retexIndex in -1..32767)
        require(recolour in 0..65535 && retexture in 0..65535)
        val zone = ZoneGrid.from(obj.coords)
        queue(
            obj.coords,
            ObjCustomise(obj.type, obj.count, model, recolIndex, recolour, retexIndex, retexture, zone.x, zone.z),
        )
    }

    /**
     * Queues a reset of the public ground item's appearance for clients viewing its zone.
     *
     * The client uses [obj]'s tile, type, and current quantity to identify the stack whose
     * model, colours, and textures should return to their cache defaults.
     */
    public fun objUncustomise(obj: Obj) {
        require(obj.isPublic) { "Only public items can be uncustomised through a shared zone update." }
        val zone = ZoneGrid.from(obj.coords)
        queue(obj.coords, ObjUncustomise(obj.type, obj.count, zone.x, zone.z))
    }

    /**
     * Queues a right-click option update for a public ground item.
     *
     * [opFlags] controls which options the client displays for the first matching item type
     * at [obj]'s tile. Server-side interaction checks still apply.
     */
    public fun objEnabledOps(obj: Obj, opFlags: Byte) {
        require(obj.isPublic) { "Only public items can be updated through a shared zone update." }
        val zone = ZoneGrid.from(obj.coords)
        queue(obj.coords, ObjEnabledOps(obj.type, opFlags, zone.x, zone.z))
    }

    public fun soundArea(
        source: CoordGrid,
        synth: Int,
        delay: Int,
        loops: Int,
        radius: Int,
        size: Int,
    ) {
        val updates = source.getOrPutUpdateList()
        val prot = ZoneUpdateTransformer.toSoundAreaProt(source, synth, delay, loops, radius, size)
        updates += prot
    }

    public fun mapAnim(spotanim: Int, coord: CoordGrid, height: Int, delay: Int) {
        val updates = coord.getOrPutUpdateList()
        val prot = ZoneUpdateTransformer.toMapAnimProt(spotanim, coord, height, delay)
        updates += prot
    }

    public fun mapProjAnim(projAnim: ProjAnim) {
        val updates = projAnim.startCoord.getOrPutUpdateList()
        val prot = ZoneUpdateTransformer.toMapProjAnimProt(projAnim)
        updates += prot
    }

    /**
     * Queues [update] in the enclosed zone update for [source].
     *
     * [source] is the absolute start tile. The packet's in-zone X and Z must match that tile.
     * Keep the projectile slot from [update] for subsequent [scriptedProjChange] calls.
     */
    public fun scriptedProjAdd(source: CoordGrid, update: ScriptedProjAdd) {
        val inZone = ZoneGrid.from(source)
        require(update.xInZone == inZone.x && update.zInZone == inZone.z) {
            "Scripted projectile start does not match source tile $source."
        }
        queue(source, update)
    }

    /**
     * Queues a target, freeze, or removal change for an existing scripted projectile.
     *
     * Pass the original [source] tile and use the same projectile slot in [update] so the
     * change reaches clients viewing the source zone.
     */
    public fun scriptedProjChange(source: CoordGrid, update: ScriptedProjChange) {
        queue(source, update)
    }

    private fun queue(coords: CoordGrid, update: ZoneProt) {
        val updates = coords.getOrPutUpdateList()
        updates += update
    }

    public operator fun get(zone: Int): ZoneUpdateList? = updatedZones.getOrDefault(zone, null)

    public operator fun get(zone: ZoneKey): ZoneUpdateList? =
        updatedZones.getOrDefault(zone.packed, null)

    private fun CoordGrid.getOrPutUpdateList(): ZoneUpdateList =
        updatedZones.computeIfAbsent(ZoneKey.from(this).packed) { ZoneUpdateList() }
}
