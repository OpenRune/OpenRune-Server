import java.nio.file.*;
import java.util.*;
import dev.openrune.gamevals.GameValProvider;

/** Read-only proof that every accepted symbol retains its exact native ID. */
public final class VerifyRevenantMappings {
 public static void main(String[] args)throws Exception {
  var before=GameValProvider.Companion.loadIsolated(Path.of(args[0]).toRealPath()+"/").getMappings();
  var after=GameValProvider.Companion.loadIsolated(Path.of(args[1]).toRealPath()+"/").getMappings();
  var expected=Map.of("varp.revenant_caves_admission",65468,"varp.revenant_caves_killcount",65467,"varp.revenant_maledictus_killcount",65466,"varp.revenant_scroll_nowarn",65465,"varobj.revenant_ether",65523,"varobj.revenant_absorption",65522);
  int preserved=0,added=0;
  for(var table:before.entrySet())for(var entry:table.getValue().entrySet()) {
   if(!Objects.equals(after.get(table.getKey()).get(entry.getKey()),entry.getValue()))throw new AssertionError("Changed mapping "+entry);
   preserved++;
  }
  for(var table:after.entrySet())for(var entry:table.getValue().entrySet())if(!before.getOrDefault(table.getKey(),new HashMap<>()).containsKey(entry.getKey())) {
   if(!Objects.equals(expected.get(entry.getKey()),entry.getValue()))throw new AssertionError("Unexpected mapping "+entry);
   added++;
  }
  if(added!=expected.size())throw new AssertionError("Missing Revenant mappings");
  String report="{\"passed\":true,\"preservedMappings\":"+preserved+",\"addedMappings\":"+added+"}";
  Files.writeString(Path.of(args[2]),report);System.out.println(report);
 }
}
