package gg.rsmod.game.service.cache

import dev.openrune.cache.tools.DumpTypeId
import java.io.*
import java.nio.file.Path

object TypeDumper {

   fun int(rev : Int) {
       val dumper = DumpTypeId(
           cache = File("./data/cache/").toPath(),
           rev = rev,
           outputPath = Path.of("./game/plugins/src/main/kotlin/gg/rsmod/plugins/api/cfg"),
           packageName = "gg.rsmod.plugins.api.cfg"
       )
       dumper.init()
   }

}