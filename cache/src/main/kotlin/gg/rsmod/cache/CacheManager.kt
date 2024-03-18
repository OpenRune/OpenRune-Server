package gg.rsmod.cache

import gg.rsmod.cache.definition.data.*
import gg.rsmod.cache.definition.decoder.*
import gg.rsmod.cache.util.Index
import java.nio.file.Path

object CacheManager {

    lateinit var cache : Cache
    private var npcs : Array<NPCDefinition> = emptyArray()
    private var objects : Array<ObjectDefinition> = emptyArray()
    private var items : Array<ItemDefinition> = emptyArray()
    private var varbit : Array<VarBitDefinition> = emptyArray()
    private var varps : Array<VarpDefinition> = emptyArray()
    private var anim : Array<AnimDefinition> = emptyArray()
    private var enum : Array<EnumDefinition> = emptyArray()

    fun init(cachePath : Path) {
        cache = Cache.load(cachePath,false)

        npcs = NPCDecoder().load(cache)
        objects = ObjectDecoder().load(cache)
        items = ItemDecoder().load(cache)
        varbit = VarBitDecoder().load(cache)
        varps = VarDecoder().load(cache)
        anim = AnimDecoder().load(cache)
        enum = EnumDecoder().load(cache)
    }

    fun npc(id : Int) = npcs[id]

    fun npcCount() = npcs.size

    fun objects(id : Int) = objects[id]

    fun objectCount() = objects.size

    fun item(id : Int) = items[id]

    fun itemCount() = items.size

    fun varbit(id : Int) = varbit[id]

    fun varp(id : Int) = varps[id]

    fun varpCount() = varps.size

    fun varbitCount() = varbit.size

    fun anim(id : Int) = anim[id]

    fun enum(id : Int) = enum[id]
    
}