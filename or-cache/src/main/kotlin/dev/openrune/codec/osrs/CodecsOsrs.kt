package dev.openrune.codec.osrs

import dev.openrune.cache.filestore.definition.ConfigDefinitionDecoder
import dev.openrune.codec.osrs.impl.*
import dev.openrune.types.*
import dev.openrune.types.varp.VarpServerType

class ObjectDecoder(rev : Int) : ConfigDefinitionDecoder<ObjectServerType>(ObjectServerCodec(rev), 55)

class HealthBarDecoder : ConfigDefinitionDecoder<HealthBarServerType>(HealthBarServerCodec(), 56)

class SequenceDecoder : ConfigDefinitionDecoder<SequenceServerType>(SequenceServerCodec(), 57)

class NpcDecoder(rev : Int) : ConfigDefinitionDecoder<NpcServerType>(NpcServerCodec(rev), 58)

class ItemDecoder(rev : Int) : ConfigDefinitionDecoder<ItemServerType>(ItemServerCodec(rev), 59)

class InventoryDecoder : ConfigDefinitionDecoder<InventoryServerType>(InventoryServerCodec(), 60)

class MesAnimDecoder : ConfigDefinitionDecoder<MesAnimType>(MesAnimServerCodec(), 61)

class ModLevelDecoder : ConfigDefinitionDecoder<ModLevelType>(ModLevelServerCodec(), 62)

class StatTypeDecoder : ConfigDefinitionDecoder<StatType>(StatTypeServerCodec(), 63)

class ProjectileTypeDecoder :
    ConfigDefinitionDecoder<ProjAnimType>(ProjectileTypeServerCodec(), 64)

class BasTypeDecoder : ConfigDefinitionDecoder<BasType>(BasCodec(), 65)

class VarpDecoder : ConfigDefinitionDecoder<VarpServerType>(VarpServerTypeCodec(), 67)

class WalkTriggerDecoder : ConfigDefinitionDecoder<WalkTriggerType>(WalkTriggerTypeCodec(), 68)

class VarnDecoder : ConfigDefinitionDecoder<VarnType>(VarnTypeCodec(), 82)

class VarnBitDecoder : ConfigDefinitionDecoder<VarnBitType>(VarnBitTypeCodec(), 83)

class VarConBitDecoder : ConfigDefinitionDecoder<VarConBitType>(VarConBitTypeCodec(), 84)

class VarConDecoder : ConfigDefinitionDecoder<VarConType>(VarConCodec(), 85)

class VarObjBitDecoder : ConfigDefinitionDecoder<VarObjBitType>(VarObjCodec(), 86)

class HuntModeDecoder : ConfigDefinitionDecoder<HuntModeType>(HuntCodec(), 87)
