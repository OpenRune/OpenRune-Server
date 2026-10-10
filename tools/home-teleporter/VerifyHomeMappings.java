import java.nio.file.*;
import java.util.*;
import dev.openrune.gamevals.GameValProvider;

public final class VerifyHomeMappings {
 public static void main(String[] args) throws Exception {
  var before=GameValProvider.Companion.loadIsolated(Path.of(args[0]).toRealPath()+"/").getMappings();
  var after=GameValProvider.Companion.loadIsolated(Path.of(args[1]).toRealPath()+"/").getMappings();
  var expected=Map.of("npc.home_teleport_wizard",65526,"interface.home_teleports",1105,"clientscript.home_teleports_init",45546,"queue.home_teleport",61);
  int preserved=0,added=0,components=0;
  for(var table:before.entrySet())for(var entry:table.getValue().entrySet()) {
   if(!Objects.equals(after.get(table.getKey()).get(entry.getKey()),entry.getValue()))throw new AssertionError("Changed mapping "+entry);
   preserved++;
  }
  for(var table:after.entrySet())for(var entry:table.getValue().entrySet())if(!before.getOrDefault(table.getKey(),new HashMap<>()).containsKey(entry.getKey())) {
   if(entry.getKey().startsWith("component.home_teleports:") && entry.getValue()>>>16==1105)components++;
   else if(!Objects.equals(expected.get(entry.getKey()),entry.getValue()))throw new AssertionError("Unexpected mapping "+entry);
   added++;
  }
  if(components<40 || added!=components+expected.size())throw new AssertionError("Missing home mappings");
  String report="{\"passed\":true,\"preservedMappings\":"+preserved+",\"addedMappings\":"+added+",\"components\":"+components+"}";
  Files.writeString(Path.of(args[2]),report);System.out.println(report);
 }
}
