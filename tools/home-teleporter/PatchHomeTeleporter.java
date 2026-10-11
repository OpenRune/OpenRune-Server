import java.nio.file.*;
import java.util.*;
import dev.openrune.filesystem.Cache;
import com.displee.cache.CacheLibrary;
import dev.openrune.definition.GameValGroupTypes;

/** Add only the native wizard definition, teleport interface and its init script. */
public final class PatchHomeTeleporter {
 private static Set<Integer> ids(int[] values) { var set=new TreeSet<Integer>();for(int id:values)set.add(id);return set; }
 public static void main(String[] args) throws Exception {
  Path sourceRoot=Path.of(args[0]).toRealPath(),beforeRoot=Path.of(args[1]).toRealPath(),stage=Path.of(args[2]).toRealPath();
  if(!stage.getFileName().toString().equals("home-runtime-stage") || stage.equals(beforeRoot) || stage.equals(sourceRoot) || !Files.readString(stage.resolve("ISOLATED-CANDIDATE")).equals(stage.toString()))throw new IllegalStateException("Unsafe stage");
  for(String flavour:List.of("LIVE","SERVER")) {
   Path beforePath=beforeRoot.resolve(".data/cache/"+flavour),sourcePath=sourceRoot.resolve(".data/cache/"+flavour),targetPath=stage.resolve(".data/cache/"+flavour);
   var before=Cache.Companion.load(beforePath);var source=Cache.Companion.load(sourcePath);
   int npcGroup=flavour.equals("LIVE")?9:58;
   var metadata=Map.of(GameValGroupTypes.NPCTYPES.getId(),65526,GameValGroupTypes.IFTYPES_V2.getId(),1105);
   if(before.data(2,npcGroup,65526,null)!=null || source.data(2,npcGroup,65526,null)==null)throw new IllegalStateException("NPC allocation missing or occupied");
   for(int[] asset:List.of(new int[]{3,1105},new int[]{12,45546})) {
    if(ids(before.archives(asset[0])).contains(asset[1]) || !ids(source.archives(asset[0])).contains(asset[1]))throw new IllegalStateException("Asset allocation missing or occupied: "+flavour+" "+asset[0]+"/"+asset[1]+" before="+ids(before.archives(asset[0])).contains(asset[1])+" source="+ids(source.archives(asset[0])).contains(asset[1]));
   }
   var wanted=ids(before.files(2,npcGroup));wanted.add(65526);
   if(!wanted.equals(ids(source.files(2,npcGroup))))throw new IllegalStateException("Unexpected NPC file IDs in compiled source");
   try(var target=new CacheLibrary(targetPath,false,null)) {
    target.put(2,npcGroup,65526,Objects.requireNonNull(source.data(2,npcGroup,65526,null)),null);
    for(int[] asset:List.of(new int[]{3,1105},new int[]{12,45546}))for(int file:source.files(asset[0],asset[1]))target.put(asset[0],asset[1],file,Objects.requireNonNull(source.data(asset[0],asset[1],file,null)),null);
    for(var entry:metadata.entrySet()) {
     if(before.data(24,entry.getKey(),entry.getValue(),null)!=null)throw new IllegalStateException("Metadata allocation occupied");
     target.put(24,entry.getKey(),entry.getValue(),Objects.requireNonNull(source.data(24,entry.getKey(),entry.getValue(),null)),null);
    }
    target.update();
   }
   byte[] candidateReference=PreserveConfigReference.reference(targetPath);
   PreserveConfigReference.write(targetPath,PreserveConfigReference.mergeGroups(PreserveConfigReference.reference(beforePath),candidateReference,Map.of(npcGroup,candidateReference),npcGroup));
   source.close();before.close();
   before=Cache.Companion.load(beforePath);var after=Cache.Companion.load(targetPath);int preservedArchives=0,preservedFiles=0;
   if(!Arrays.equals(before.indices(),after.indices()))throw new IllegalStateException("Changed indices");
   for(int index:before.indices()) {
    var archives=ids(before.archives(index));if(index==3)archives.add(1105);if(index==12)archives.add(45546);
    if(!archives.equals(ids(after.archives(index))))throw new IllegalStateException("Unexpected archive IDs: "+index);
    for(int archive:before.archives(index)) {
     if((index==2 && archive==npcGroup) || (index==24 && metadata.containsKey(archive))) {
      var expected=ids(before.files(index,archive));expected.add(index==2?65526:metadata.get(archive));
      if(!expected.equals(ids(after.files(index,archive))))throw new IllegalStateException("Changed definition IDs");
      for(int file:before.files(index,archive)) {
       if(!Arrays.equals(before.data(index,archive,file,null),after.data(index,archive,file,null)))throw new IllegalStateException("Changed existing NPC "+file);
       preservedFiles++;
      }
     } else {
      if(!ids(before.files(index,archive)).equals(ids(after.files(index,archive))))throw new IllegalStateException("Changed unrelated file IDs "+index+"/"+archive);
      if(!Arrays.equals(before.sector(index,archive),after.sector(index,archive)))throw new IllegalStateException("Changed unrelated archive "+index+"/"+archive);
      preservedArchives++;
     }
    }
   }
   for(int[] asset:List.of(new int[]{3,1105},new int[]{12,45546})) {
    if(!ids(source.files(asset[0],asset[1])).equals(ids(after.files(asset[0],asset[1]))))throw new IllegalStateException("Missing asset files");
    for(int file:source.files(asset[0],asset[1]))if(!Arrays.equals(source.data(asset[0],asset[1],file,null),after.data(asset[0],asset[1],file,null)))throw new IllegalStateException("Changed new asset data");
   }
   before.close();after.close();
   System.out.println("PRESERVED "+flavour+" archives="+preservedArchives+" definitionFiles="+preservedFiles+"; added wizard=65526 interface=1105 script=45546 with native metadata");
  }
 }
}
