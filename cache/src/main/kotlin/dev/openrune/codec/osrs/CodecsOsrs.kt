package dev.openrune.codec.osrs

import dev.openrune.cache.filestore.definition.ConfigDefinitionDecoder
import dev.openrune.codec.osrs.impl.*
import dev.openrune.types.*

class ObjectDecoder : ConfigDefinitionDecoder<ObjectServerType>(ObjectServerCodec(), 55)
class HealthBarDecoder : ConfigDefinitionDecoder<HealthBarServerType>(HealthBarServerCodec(), 56)
class SequenceDecoder : ConfigDefinitionDecoder<SequenceServerType>(SequenceServerCodec(), 57)
class NpcDecoder : ConfigDefinitionDecoder<NpcServerType>(NpcServerCodec(), 58)
class ItemDecoder : ConfigDefinitionDecoder<ItemServerType>(ItemServerCodec(), 59)
class InventoryDecoder : ConfigDefinitionDecoder<InventoryServerType>(InventoryServerCodec(), 60)
