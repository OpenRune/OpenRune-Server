import java.nio.*;
import java.nio.file.*;
import java.util.*;
import dev.openrune.filesystem.Cache;
import dev.openrune.ServerCacheManager;
import dev.openrune.rscm.RSCM;
import com.displee.cache.CacheLibrary;
import dev.openrune.map.area.*;
import it.unimi.dsi.fastutil.bytes.*;
import it.unimi.dsi.fastutil.shorts.*;
import io.netty.buffer.*;

/** Applies a bounded server-only delta over the accepted runtime, preserving native config IDs. */
public final class PatchRevenants {
  private static Set<Integer> ids(int[] values) { var result=new TreeSet<Integer>();for(int id:values)result.add(id);return result; }
  private static List<Integer> npcs(byte[] data) {
    if(data==null)return List.of();var in=ByteBuffer.wrap(data);int count=Short.toUnsignedInt(in.getShort());var result=new ArrayList<Integer>();
    for(int n=0;n<count;n++)result.add(in.getInt());if(in.hasRemaining())throw new IllegalStateException("Unexpected NPC data");return result;
  }
  private static byte[] npcBytes(List<Integer> list) { var out=ByteBuffer.allocate(2+4*list.size());out.putShort((short)list.size());for(int packed:list)out.putInt(packed);return out.array(); }
  private static final Set<Integer> ordinary = new TreeSet<>();
  private static boolean revenant(int packed) { return ordinary.contains(packed & 65535); }
  private static ShortSet without(ShortSet source, short id) { var result=new ShortArraySet(source);result.remove(id);return result; }
  private static MapAreaDefinition singles(MapAreaDefinition original, short multi) {
    var zones=new Byte2ObjectOpenHashMap<ShortSet>();original.getZoneAreas().byte2ObjectEntrySet().forEach(e->zones.put(e.getByteKey(),without(e.getValue(),multi)));
    var coords=new Short2ObjectOpenHashMap<ShortSet>();original.getCoordAreas().short2ObjectEntrySet().forEach(e->coords.put(e.getShortKey(),without(e.getValue(),multi)));
    return new MapAreaDefinition(without(original.getMapSquareAreas(),multi),zones,coords,original.getIncludes(),original.getExcludes());
  }
  public static void main(String[] args)throws Exception {
    Path compiled=Path.of(args[0]).toRealPath(), original=Path.of(args[1]).toRealPath(), stage=Path.of(args[2]).toRealPath();
    if(!stage.getFileName().toString().equals("revenant-runtime-stage")||stage.equals(original)||stage.equals(compiled)||!Files.readString(stage.resolve("ISOLATED-CANDIDATE")).equals(stage.toString()))throw new IllegalStateException("Unsafe stage");
    ServerCacheManager.INSTANCE.init(240).close();
    var groups=new TreeMap<Integer,Set<Integer>>();
    for(String name:List.of("imp","goblin","pyrefiend","hobgoblin","cyclops","hellhound","demon","ork","darkbeast","knight","dragon"))ordinary.add(RSCM.INSTANCE.getRSCM("npc.wild_cave_"+name));
    var monsters=new TreeSet<Integer>(ordinary);monsters.add(RSCM.INSTANCE.getRSCM("npc.wild_cave_superior"));groups.put(58,monsters);
    var items=new TreeSet<Integer>(List.of(21816,21817));for(int value:List.of(500,1000,2000,4000,8000,16000)) {
      items.add(RSCM.INSTANCE.getRSCM("obj.wild_cave_artifact_"+value));items.add(RSCM.INSTANCE.getRSCM("obj.cert_wild_cave_artifact_"+value));
    }groups.put(59,items);
    groups.put(67,new TreeSet<>(List.of(65465,65466,65467,65468)));groups.put(86,new TreeSet<>(List.of(65522,65523)));groups.put(87,new TreeSet<>(List.of(9)));
    Path beforePath=original.resolve(".data/cache/SERVER"), sourcePath=compiled.resolve(".data/cache/SERVER"), targetPath=stage.resolve(".data/cache/SERVER");
    var before=Cache.Companion.load(beforePath);var source=Cache.Companion.load(sourcePath);
    var replacementRefs=new TreeMap<Integer,byte[]>();
    for(int group:List.of(67,86,87)) {
      var wanted=ids(before.files(2,group));for(int id:groups.get(group))if(!wanted.add(id))throw new IllegalStateException("Config allocation collides: "+group+"/"+id);
      if(!wanted.equals(ids(source.files(2,group))))throw new IllegalStateException("Unexpected source IDs: "+group);
      for(int id:before.files(2,group))if(!Arrays.equals(before.data(2,group,id,null),source.data(2,group,id,null)))throw new IllegalStateException("Changed unrelated config: "+group+"/"+id);
      replacementRefs.put(group,PreserveConfigReference.reference(sourcePath));
    }
    var maps=new TreeSet<Integer>();for(int x=49;x<=50;x++)for(int z=157;z<=159;z++)maps.add((x<<8)|z);
    var areaDecode=MapAreaDecoder.class.getMethod("decode-YfwNhVc",byte[].class);
    short multi=(short)RSCM.INSTANCE.getRSCM("area.multiway");int totalSpawns=0;
    try(var target=new CacheLibrary(targetPath,false,null)) {
      for(int group:List.of(58,59))for(int id:groups.get(group))target.put(2,group,id,Objects.requireNonNull(source.data(2,group,id,null)),null);
      for(int map:maps) {
        var old=npcs(before.data(5,map,5,null));var fresh=npcs(source.data(5,map,5,null));var merged=new ArrayList<Integer>();old.stream().filter(n->!revenant(n)).forEach(merged::add);
        var revs=fresh.stream().filter(PatchRevenants::revenant).toList();merged.addAll(revs);totalSpawns+=revs.size();target.put(5,map,5,npcBytes(merged),null);
        byte[] area=Objects.requireNonNull(before.data(5,map,7,null));var expected=singles((MapAreaDefinition)areaDecode.invoke(MapAreaDecoder.INSTANCE,(Object)area),multi);
        var out=Unpooled.buffer();try{MapAreaEncoder.INSTANCE.encode(expected,out);byte[] bytes=new byte[out.readableBytes()];out.readBytes(bytes);target.put(5,map,7,bytes,null);}finally{out.release();}
      }
      if(totalSpawns!=30)throw new IllegalStateException("Expected 30 ordinary spawns, got "+totalSpawns);
      target.update();for(int group:List.of(67,86,87))if(!target.index(2).writeArchiveSector(group,source.sector(2,group)))throw new IllegalStateException("Config sector write failed");
    }
    PreserveConfigReference.write(targetPath,PreserveConfigReference.mergeGroups(PreserveConfigReference.reference(beforePath),PreserveConfigReference.reference(targetPath),replacementRefs));
    source.close();before.close();
    for(String flavour:List.of("SERVER","LIVE")) {
      before=Cache.Companion.load(original.resolve(".data/cache/"+flavour));var after=Cache.Companion.load(stage.resolve(".data/cache/"+flavour));int archives=0,files=0;
      if(!Arrays.equals(before.indices(),after.indices()))throw new IllegalStateException("Changed indices");
      for(int index:before.indices()) {
        if(!Arrays.equals(before.archives(index),after.archives(index)))throw new IllegalStateException("Changed archive IDs: "+index);
        for(int archive:before.archives(index)) {
          boolean config=flavour.equals("SERVER")&&index==2&&groups.containsKey(archive);
          boolean map=flavour.equals("SERVER")&&index==5&&maps.contains(archive);
          if(!config&&!map) {if(!Arrays.equals(before.sector(index,archive),after.sector(index,archive)))throw new IllegalStateException("Changed unrelated archive: "+index+"/"+archive);archives++;continue;}
          var wanted=ids(before.files(index,archive));if(config)wanted.addAll(groups.get(archive));else wanted.addAll(List.of(5,7));
          if(!wanted.equals(ids(after.files(index,archive))))throw new IllegalStateException("Changed file IDs: "+index+"/"+archive);
          for(int file:before.files(index,archive)) {
            if(config&&groups.get(archive).contains(file))continue;
            if(map&&(file==5||file==7))continue;
            if(!Arrays.equals(before.data(index,archive,file,null),after.data(index,archive,file,null)))throw new IllegalStateException("Changed unrelated file: "+index+"/"+archive+"/"+file);files++;
          }
          if(map) {
            if(!npcs(before.data(5,archive,5,null)).stream().filter(n->!revenant(n)).toList().equals(npcs(after.data(5,archive,5,null)).stream().filter(n->!revenant(n)).toList()))throw new IllegalStateException("Unrelated NPC spawns changed");
            var expected=singles((MapAreaDefinition)areaDecode.invoke(MapAreaDecoder.INSTANCE,(Object)before.data(5,archive,7,null)),multi);
            if(!expected.equals(areaDecode.invoke(MapAreaDecoder.INSTANCE,(Object)after.data(5,archive,7,null))))throw new IllegalStateException("Changed unrelated areas");
          }
        }
      }
      System.out.println("PRESERVED "+flavour+" archives="+archives+" files="+files+"; revenant spawns="+totalSpawns);
      before.close();after.close();
    }
  }
}
